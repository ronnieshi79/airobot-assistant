package com.airobot.airbot.character

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Alignment
import com.airobot.airbot.state.RobotVisualState
import com.airobot.airbot.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

private const val TAG = "RiveCharacter"

/**
 * Rive character component. Renders the Rive animation for selected IP characters.
 * Maps [RobotVisualState] and audio levels to Rive state machine inputs.
 * Uses a try-catch block to gracefully fallback if the state machine/inputs do not exist in the placeholder.
 */
@Composable
fun RiveCharacter(
    state: RobotVisualState,
    roleName: String?,
    audioLevel: () -> Float,
    modifier: Modifier = Modifier
) {
    key(roleName) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                Log.d(TAG, "Initializing RiveAnimationView for $roleName")
                RiveAnimationView(context).apply {
                    try {
                        val resId = RiveCharacterConfigManager.getResourceForRole(context, roleName)
                        // Load Rive file from raw resources using the default artboard and state machine
                        setRiveResource(
                            resId = resId,
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
}

/**
 * Data class representing a Rive character config entry.
 */
data class RiveCharacterEntry(
    val name: String,
    val resourceName: String
)

/**
 * Configuration manager to dynamically load Rive characters mapping from rive_config.json.
 */
object RiveCharacterConfigManager {
    private var cachedCharacters: List<Pair<String, Int>>? = null

    fun getRiveCharacters(context: Context): List<Pair<String, Int>> {
        if (cachedCharacters != null) return cachedCharacters!!

        synchronized(this) {
            if (cachedCharacters != null) return cachedCharacters!!

            val list = mutableListOf<Pair<String, Int>>()
            try {
                context.assets.open("rive_config.json").use { inputStream ->
                    InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                        val type = object : TypeToken<List<RiveCharacterEntry>>() {}.type
                        val entries = Gson().fromJson<List<RiveCharacterEntry>>(reader, type)
                        
                        if (entries != null) {
                            for (entry in entries) {
                                // Dynamically look up the resource identifier
                                val resId = context.resources.getIdentifier(
                                    entry.resourceName,
                                    "raw",
                                    context.packageName
                                )
                                // Filter out invalid configurations (resource not found)
                                if (resId != 0) {
                                    list.add(Pair(entry.name, resId))
                                }
                                // Limit to maximum 3 characters
                                if (list.size >= 3) break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RiveConfig", "Error loading rive_config.json: ${e.message}", e)
            }

            // Fallback default in case config is empty or completely invalid
            if (list.isEmpty()) {
                list.add(Pair("心小苗", R.raw.xin_xiao_miao))
            }

            cachedCharacters = list
            return list
        }
    }

    fun getResourceForRole(context: Context, roleName: String?): Int {
        val characters = getRiveCharacters(context)
        if (roleName == null) return characters.first().second
        
        // Match by role name (case-insensitive)
        val match = characters.find { 
            it.first.equals(roleName, ignoreCase = true)
        }
        return match?.second ?: characters.first().second
    }
}
