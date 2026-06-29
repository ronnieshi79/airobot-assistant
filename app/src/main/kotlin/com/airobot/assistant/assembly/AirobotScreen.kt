package com.airobot.assistant.assembly

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.airobot.airbot.character.RobotCharacter
import com.airobot.airbot.components.dialogue.BubbleAiDialogue
import com.airobot.airbot.components.interaction.VoiceInputPanel
import com.airobot.airbot.domain.model.CharacterType
import com.airobot.airbot.viewmodel.RobotVisualState

@Composable
fun AirobotScreen(
    robotHorizontalBias: Float,
    robotVisualState: RobotVisualState,
    characterType: CharacterType,
    roleName: String,
    audioLevel: Float,
    isConnected: Boolean,
    isTimerActive: Boolean,
    isTimerPaused: Boolean,
    currentRoundAiText: String?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onInterruptSpeak: () -> Unit,
    onTimerControl: (String) -> Unit,
    onCommandClick: (String) -> Unit,
    onBubbleClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier.fillMaxSize()) {
        val (robotRef, voicePanelRef, aiBubbleRef) = createRefs()

        // 1. 机器人角色
        Box(
            modifier = Modifier
                .constrainAs(robotRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(voicePanelRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    horizontalBias = robotHorizontalBias
                    verticalBias = 0.5f
                    height = Dimension.fillToConstraints
                },
            contentAlignment = Alignment.Center
        ) {
            RobotCharacter(
                state = robotVisualState,
                characterType = characterType,
                roleName = roleName,
                audioLevel = { audioLevel },
                headSize = 400.dp
            )
        }

        // 2. 语音输入面板
        Box(
            modifier = Modifier
                .constrainAs(voicePanelRef) {
                    bottom.linkTo(parent.bottom, margin = 40.dp)
                    start.linkTo(robotRef.start)
                    end.linkTo(robotRef.end)
                }
        ) {
            VoiceInputPanel(
                robotState = robotVisualState,
                isConnected = isConnected,
                isTimerActive = isTimerActive,
                isTimerPaused = isTimerPaused,
                audioLevel = audioLevel,
                onStopListening = onStopListening,
                onInterruptSpeak = onInterruptSpeak,
                onTimerControl = onTimerControl,
                onCommandClick = { command ->
                    if (command == "WAKE_UP") {
                        onStartListening()
                    } else {
                        onCommandClick(command)
                    }
                }
            )
        }

        // 3. AI 对话气泡
        Box(
            modifier = Modifier
                .constrainAs(aiBubbleRef) {
                    start.linkTo(robotRef.end, margin = (-180).dp)
                    top.linkTo(robotRef.top, margin = 180.dp)
                }
        ) {
            BubbleAiDialogue(
                robotState = robotVisualState,
                aiMsg = currentRoundAiText,
                onAiSpeechComplete = {},
                onClose = onBubbleClose
            )
        }
    }
}
