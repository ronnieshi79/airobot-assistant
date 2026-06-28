package com.airobot.airbot

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.airbot.components.AirobotDialogueFlow
import com.airobot.airbot.components.AirobotIdleFlow
import com.airobot.airbot.viewmodel.AirobotViewModel
import com.airobot.airbot.viewmodel.ConversationViewModel
import kotlinx.coroutines.flow.SharedFlow

/**
 * AirobotScreen - A unified presentation screen for the robot character.
 * Resolves character animations, dialogue box streams, and voice interactions in the `:airbot` UI layer.
 */
@Composable
fun AirobotScreen(
    permissionsGranted: Boolean,
    wakeupEvent: SharedFlow<Unit>,
    modifier: Modifier = Modifier,
    screenViewModel: AirobotViewModel = hiltViewModel(),
    conversationViewModel: ConversationViewModel = hiltViewModel()
) {
    val uiState by screenViewModel.uiState.collectAsState()
    val messages by conversationViewModel.messages.collectAsState()

    // React to system wake event to trigger voice conversation
    LaunchedEffect(wakeupEvent) {
        wakeupEvent.collect {
            if (permissionsGranted) {
                conversationViewModel.startConversation()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        val scaleRatio = (maxWidth / 320.dp).coerceIn(0.6f, 1.2f)
        val showDialogueFlow = uiState.visualState.isDialogueFamily

        // Dialogue Flow container (Listening, Thinking, Speaking)
        AirobotDialogueFlow(
            isVisible = showDialogueFlow && uiState.isCharacterLoaded,
            visualState = uiState.visualState,
            messages = messages,
            characterType = uiState.characterType,
            roleName = uiState.roleName,
            audioLevel = uiState.audioLevel,
            isConnected = uiState.isConnected,
            scaleRatio = scaleRatio,
            onClose = {
                conversationViewModel.interrupt()
            },
            onStopListening = {
                conversationViewModel.stopAutoConversation()
            },
            onInterruptSpeak = {
                conversationViewModel.interruptSpeak("ui")
            },
            onCommandClick = { command ->
                if (permissionsGranted) {
                    conversationViewModel.startConversation()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        val characterSize = min(maxWidth, 240.dp) * 0.72f

        // Idle State Flow (Sleeping, Thinking, Ready, etc.)
        AirobotIdleFlow(
            isVisible = !showDialogueFlow && uiState.isCharacterLoaded,
            visualState = uiState.visualState,
            characterType = uiState.characterType,
            roleName = uiState.roleName,
            audioLevel = uiState.audioLevel,
            isConnected = uiState.isConnected,
            characterSize = characterSize,
            scaleRatio = scaleRatio,
            onRobotClick = {
                if (permissionsGranted) {
                    conversationViewModel.startConversation()
                }
            },
            onCommandClick = { command ->
                if (permissionsGranted) {
                    conversationViewModel.startConversation()
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxHeight * 0.5f)
        )
    }
}
