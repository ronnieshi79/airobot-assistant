package com.airobot.agent.brain

import kotlinx.coroutines.flow.StateFlow

/**
 * Unified AI brain state enumeration.
 * Drives robot visual state transitions and conversation lifecycle.
 */
enum class BrainState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

/**
 * Core AI brain interface — the single abstraction for all AI backends
 * (cloud proxy, local agent, etc.). Upper layers observe brainState
 * to react to AI lifecycle changes without knowing the backend details.
 */
interface AiBrain {
    val brainState: StateFlow<BrainState>

    fun wakeUp()
    fun sleep()

    fun setSpeechInterruptionEnabled(enabled: Boolean)
    fun isSpeechInterruptionEnabled(): Boolean
}
