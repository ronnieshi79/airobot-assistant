package com.airobot.airbot.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airobot.agent.audio.AudioEvent
import com.airobot.agent.audio.AudioService
import com.airobot.agent.brain.AiBrain
import com.airobot.airbot.R
import com.airobot.airbot.api.AirbotEngineApi
import com.airobot.airbot.domain.model.ConversationSubState
import com.airobot.airbot.domain.model.Message
import com.airobot.airbot.domain.model.MessageRole
import com.airobot.airbot.domain.model.RobotState
import com.airobot.core.comm.NetCommEvent
import com.airobot.core.comm.NetCommService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * AI Conversation ViewModel.
 * Manages conversation lifecycle: STT/TTS handling, audio event processing,
 * and state synchronization with the robot state engine.
 *
 * Note: aiBrain is injected to ensure XiaozhiCloudBrain is eagerly initialized
 * and listening for MCP events. Future work: migrate to observe aiBrain.brainState
 * instead of parsing TextMessage JSON directly.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    application: Application,
    private val netCommService: NetCommService,
    private val audioService: AudioService,
    private val robotStateEngine: AirbotEngineApi,
    @Suppress("unused") private val aiBrain: AiBrain,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConversationViewModel"
    }

    private var isAutoMode = false    // Continuous Conversation Mode
    private var isActive = false
    private var isTransitioningAfterTtsStop = false
    private val _subState = MutableStateFlow(ConversationSubState.LISTENING)
    private val _isMuted = MutableStateFlow(false)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var inactivityJob: kotlinx.coroutines.Job? = null
    private var thinkingTimeoutJob: kotlinx.coroutines.Job? = null

    private var isBuffering = false
    private val audioBuffer = mutableListOf<ByteArray>()
    private var bufferJob: kotlinx.coroutines.Job? = null

    private val _currentRoundUserText = MutableStateFlow<String?>(null)
    val currentRoundUserText: StateFlow<String?> = _currentRoundUserText.asStateFlow()

    private val _currentRoundAiText = MutableStateFlow<String?>(null)
    val currentRoundAiText: StateFlow<String?> = _currentRoundAiText.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    init {
        viewModelScope.launch {
            netCommService.events.collect { event ->
                if (isActive) {
                    handleAiRobotCommEvent(event)
                }
            }
        }

        viewModelScope.launch {
            audioService.events.collect { event ->
                if (isActive) {
                    handleAudioEvent(event)
                }
            }
        }
    }

    private fun handleAiRobotCommEvent(event: NetCommEvent) {
        when (event) {
            is NetCommEvent.TextMessage -> handleTextMessage(event.json)
            is NetCommEvent.AudioFrame -> handleTtsAudioFrame(event.data)
            else -> {}
        }
    }

    private fun handleTextMessage(jsonStr: String) {
        try {
            val json = org.json.JSONObject(jsonStr)
            val type = json.optString("type")
            when (type) {
                "stt" -> {
                    val text = json.optString("text", "")
                    if (text.isNotBlank()) handleSttResult(text)
                }

                "tts" -> {
                    val state = json.optString("state")
                    if (state == "start") {
                        handleTtsStart()
                    } else if (state == "stop") {
                        handleTtsStop()
                    } else if (state == "sentence_start") {
                        val text = json.optString("text", "")
                        if (text.isNotBlank()) handleTtsSentence(text)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing TextMessage", e)
        }
    }

    private fun handleAudioEvent(event: AudioEvent) {
        when (event) {
            is AudioEvent.SpeechData -> {
                // Only send captured audio to ASR server when in LISTENING state
                if (_subState.value == ConversationSubState.LISTENING) {
                    if (isBuffering) {
                        audioBuffer.add(event.data)
                    } else {
                        netCommService.sendAudio(event.data)
                    }
                }
            }

            is AudioEvent.VoiceLevel -> _audioLevel.value = event.level
            is AudioEvent.SpeechStart -> {
                Log.d(TAG, "SpeechStart received, subState=${_subState.value}")
                if (isTransitioningAfterTtsStop) {
                    Log.d(TAG, "SpeechStart ignored: transitioning after TTS stop (preventing echo leakage)")
                    return
                }
                // If the user starts speaking while the robot is speaking, treat it as an interruption
                if (_subState.value == ConversationSubState.SPEAKING) {
                    Log.d(TAG, "User started speaking while AI is speaking, interrupting...")
                    interruptSpeak(source = "vad_interrupt")
                }
                // Transition to manual mode on ASR
                if (_subState.value == ConversationSubState.LISTENING) {
                    Log.d(TAG, "Starting manual listening session on ASR server")
                    netCommService.startListening("manual")

                    isBuffering = true
                    audioBuffer.clear()
                    bufferJob?.cancel()
                    bufferJob = viewModelScope.launch {
                        delay(300) // Delay to let server initialize ASR after listen(start) and abort
                        isBuffering = false
                        val combined = audioBuffer.toList()
                        audioBuffer.clear()
                        combined.forEach {
                            netCommService.sendAudio(it)
                        }
                    }
                }
            }

            is AudioEvent.SpeechEnd -> {
                Log.d(TAG, "SpeechEnd received, subState=${_subState.value}")
                if (_subState.value == ConversationSubState.LISTENING) {
                    Log.d(
                        TAG,
                        "User finished speaking. Stopping listening session and transitioning to THINKING"
                    )
                    netCommService.stopListening()
                    _subState.value = ConversationSubState.THINKING
                    syncSubState()
                    startThinkingTimeout()
                }
            }

            else -> return
        }
    }

    private fun syncSubState() {
        val current = robotStateEngine.robotState.value
        Log.d(TAG, "syncSubState: current=$current, target subState=${_subState.value}")
        if (current is RobotState.Ready || current is RobotState.Conversation
            || current is RobotState.Connecting
        ) {
            robotStateEngine.updateEngineState(
                RobotState.Conversation(_subState.value)
            )
        } else {
            Log.w(TAG, "syncSubState ignored because current state is $current")
        }
    }

    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    private fun handleSttResult(text: String) {
        cancelThinkingTimeout()
        if (text.isNotBlank()) {
            resetInactivityTimer()
            _currentRoundUserText.value = text
            addMessage(Message(role = MessageRole.USER, content = text))

            if (_subState.value == ConversationSubState.LISTENING) {
                Log.d(TAG, "STT received, transitioning LISTENING -> THINKING")
                _subState.value = ConversationSubState.THINKING
                syncSubState()
            } else {
                Log.d(
                    TAG,
                    "STT received during ${_subState.value}, text displayed but state unchanged"
                )
            }
        }
    }

    private fun handleTtsStart() {
        Log.d(TAG, "TTS Start received, transitioning to SPEAKING")
        cancelThinkingTimeout()
        resetInactivityTimer()
        _subState.value = ConversationSubState.SPEAKING
        syncSubState()
    }

    private fun handleTtsStop() {
        audioService.stopPlaying()

        // If we are no longer SPEAKING (e.g., due to user interruption), ignore the natural TtsStop transition.
        if (_subState.value != ConversationSubState.SPEAKING) {
            Log.d(
                TAG,
                "Ignoring TtsStop because subState is ${_subState.value} (likely interrupted)"
            )
            return
        }

        isTransitioningAfterTtsStop = true

        viewModelScope.launch {
            delay(200)
            // Double check state after delay just in case an interruption happened during the delay
            if (_subState.value != ConversationSubState.SPEAKING) {
                Log.d(TAG, "Ignoring TtsStop transition because subState changed during delay")
                isTransitioningAfterTtsStop = false
                return@launch
            }

            if (isAutoMode) {
                startNextRound()
            } else {
                cleanConversation()
            }
            isTransitioningAfterTtsStop = false
        }
    }

    private fun handleTtsSentence(text: String) {
        resetInactivityTimer()
        _currentRoundAiText.value = text
        addMessage(Message(role = MessageRole.AGENT, content = text))

        if (_subState.value != ConversationSubState.SPEAKING) {
            Log.d(TAG, "TtsSentence received, ensuring state is SPEAKING")
            _subState.value = ConversationSubState.SPEAKING
            syncSubState()
        }
    }

    private fun handleTtsAudioFrame(data: ByteArray) {
        if (!_isMuted.value && _subState.value == ConversationSubState.SPEAKING)
            audioService.play(data)
    }

    private fun handleDialogueEnd() {
        Log.d(TAG, "DialogueEnd received, deactivating audio")
        cancelThinkingTimeout()
        cleanConversation()
    }

    fun startConversation(contextData: ByteArray? = null) {
        if (!netCommService.isConnected) return
        isActive = true
        isAutoMode = true
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncSubState()

        audioService.activate()

        if (contextData != null) {
            Log.d(TAG, "startConversation with contextData: starting manual listening session")
            netCommService.startListening("manual")
            netCommService.sendAudio(contextData)
        }
    }

    fun interrupt() {
        netCommService.abort("user_interrupt")
        cleanConversation()
    }

    fun stopAutoConversation() {
        netCommService.abort("stop_auto_mode")
        cleanConversation()
    }

    private fun resetRoundText() {
        _currentRoundUserText.value = null
        _currentRoundAiText.value = null
        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        if (isActive) {
            inactivityJob = viewModelScope.launch {
                // Set airbot inactivity timeout to 90s to prevent missing messages.
                delay(90000)
                Log.d(TAG, "Inactivity timeout reached, stopping conversation")
                stopAutoConversation()
            }
        }
    }

    private fun startThinkingTimeout() {
        thinkingTimeoutJob?.cancel()
        thinkingTimeoutJob = viewModelScope.launch {
            delay(10000) // 10s thinking timeout
            if (_subState.value == ConversationSubState.THINKING) {
                Log.w(TAG, "Thinking timeout reached! Reverting to LISTENING.")
                startNextRound()
            }
        }
    }

    private fun cancelThinkingTimeout() {
        thinkingTimeoutJob?.cancel()
    }

    private fun cleanConversation() {
        Log.d(TAG, "cleanConversation: Deactivating audio and resetting state")
        isActive = false
        isAutoMode = false

        cancelThinkingTimeout()
        bufferJob?.cancel()
        isBuffering = false
        audioBuffer.clear()

        audioService.deactivate()
        audioService.stopPlaying()

        resetRoundText()

        robotStateEngine.updateEngineState(RobotState.Ready)
    }

    private fun startNextRound() {
        cancelThinkingTimeout()
        bufferJob?.cancel()
        isBuffering = false
        audioBuffer.clear()

        if (!isAutoMode || !netCommService.isConnected) {
            isActive = false
            audioService.deactivate()
            return
        }
        isActive = true
        resetRoundText()
        _subState.value = ConversationSubState.LISTENING
        syncSubState()

        audioService.activate()
    }

    /**
     * Interrupts the current speaking state and reverts to listening state.
     * Centralized domain logic for handling interruptions from various sources (UI, Audio VAD, etc.).
     */
    fun interruptSpeak(source: String = "ui") {
        if (_subState.value != ConversationSubState.SPEAKING) return

        Log.d(
            TAG,
            "interruptSpeak triggered by source: $source, stopping playback and resuming listening"
        )

        // Add system message indicating user interrupted AI speaking
        val interruptMsg =
            getApplication<Application>().getString(R.string.dialogue_system_interrupted)
        addMessage(Message(role = MessageRole.SYSTEM, content = interruptMsg))

        // 1. Notify underlying protocol to stop TTS
        netCommService.abort("user_interrupt")

        // 2. Stop local audio playing
        audioService.stopPlaying()

        // 3. Immediately transition back to LISTENING state for the next round
        startNextRound()
    }
}

