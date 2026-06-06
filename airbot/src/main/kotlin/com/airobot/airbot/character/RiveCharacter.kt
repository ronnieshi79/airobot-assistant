package com.airobot.airbot.character

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Alignment
import com.airobot.airbot.state.RobotVisualState
import com.airobot.airbot.R

private const val TAG = "RiveCharacter"

/**
 * Rive character component. Renders the Rive animation for "心小苗" / Xin Xiao Miao.
 * Maps [RobotVisualState] and audio levels to Rive state machine inputs.
 * Uses a try-catch block to gracefully fallback if the state machine/inputs do not exist in the placeholder.
 */
@Composable
fun RiveCharacter(
    state: RobotVisualState,
    audioLevel: () -> Float,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Log.d(TAG, "Initializing RiveAnimationView for Xin Xiao Miao")
            RiveAnimationView(context).apply {
                try {
                    // Load Rive file from raw resources using the default artboard and state machine
                    setRiveResource(
                        resId = R.raw.xin_xiao_miao,
                        fit = Fit.CONTAIN,
                        alignment = Alignment.CENTER,
                        autoplay = true
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading Rive resource: ${e.message}", e)
                }
            }
        },
        update = { view ->
            val level = audioLevel()
            val stateNum = when (state) {
                RobotVisualState.IDLE -> 0f
                RobotVisualState.LISTENING -> 1f
                RobotVisualState.THINKING -> 2f
                RobotVisualState.SPEAKING -> 3f
                RobotVisualState.FOCUS -> 4f
                RobotVisualState.HAPPY -> 5f
                RobotVisualState.SLEEPING -> 6f
            }

            // Dynamically resolve the active state machine name from the loaded view
            val stateMachine = view.stateMachines.firstOrNull()
            val activeStateMachineName = stateMachine?.name
            if (stateMachine != null && activeStateMachineName != null) {
                try {
                    val inputNames = stateMachine.inputs.map { it.name }
                    Log.d(TAG, "Active state machine '$activeStateMachineName' has inputs: $inputNames")

                    if (inputNames.contains("state")) {
                        view.setNumberState(activeStateMachineName, "state", stateNum)
                    }
                    if (inputNames.contains("audioLevel")) {
                        view.setNumberState(activeStateMachineName, "audioLevel", level * 100f)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update Rive state machine inputs on '$activeStateMachineName': ${e.message}")
                }
            } else {
                Log.v(TAG, "No active Rive state machine found yet to update inputs")
            }
        }
    )
}
