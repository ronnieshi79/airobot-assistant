package com.airobot.agent.audio.recorder

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.airobot.agent.AudioConfig
import com.airobot.agent.AudioEvent

/**
 * Audio recorder interface.
 */
interface AudioRecorder {
    /**
     * Initializes the recorder with configuration and event flow.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun initialize(config: AudioConfig, events: MutableSharedFlow<AudioEvent>): Boolean

    /**
     * Starts recording.
     */
    fun startRecording()

    /**
     * Stops recording.
     */
    fun stopRecording()

    /**
     * Checks if recording is active.
     */
    fun isRecording(): Boolean
    
    /**
     * Sets the audio processing work state.
     */
    fun setWorkState(state: com.airobot.agent.AudioWorkState)

    /**
     * Cleans up and releases resources.
     */
    fun cleanup()

    /**
     * Flow emitting changes to the recording state.
     */
    val onRecordingStateChanged: SharedFlow<Boolean>
}
