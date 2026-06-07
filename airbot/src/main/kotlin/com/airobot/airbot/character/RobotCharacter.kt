package com.airobot.airbot.character

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.airbot.character.canvas.AetherCharacter
import com.airobot.airbot.character.rive.RiveCharacter
import com.airobot.airbot.character.rive.RiveCharacterConfigManager
import com.airobot.airbot.state.RobotVisualState

/**
 * 机器人角色主组件 - 支持多引擎切换调度
 * 作为统一的动画入口容器
 */
@Composable
fun RobotCharacter(
    state: RobotVisualState,
    characterType: CharacterType = CharacterType.ANDROID_CANVAS,
    roleName: String? = null,
    ttsProgressNormalized: Float = 0f,
    audioLevel: () -> Float = { 0f },
    headSize: Dp = 320.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (characterType) {
            CharacterType.ANDROID_CANVAS -> {
                // 默认原生 Canvas 形象：Aether
                AetherCharacter(
                    state = state,
                    ttsProgressNormalized = ttsProgressNormalized,
                    audioLevel = audioLevel,
                    headSize = headSize,
                    modifier = Modifier.fillMaxSize()
                )
            }
            CharacterType.RIVE_IP -> {
                val context = LocalContext.current
                val config = RiveCharacterConfigManager.getCharacterConfig(context, roleName)
                val density = LocalDensity.current
                val translationX = with(density) { config.offsetX.dp.toPx() }
                val translationY = with(density) { config.offsetY.dp.toPx() }

                // Create a cutout container that crops the margins.
                // It's smaller than the full canvas so it acts like a clipping mask.
                // 保持底层view大小，位置不变，rive缩小确保rive动画完整显示
                Box(
                    modifier = Modifier
                        .size(headSize * 1.2f)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    RiveCharacter(
                        state = state,
                        roleName = roleName,
                        audioLevel = audioLevel,
                        // 使用 fillMaxSize 代替 requiredSize，确保 Rive 完整显示而不被过度裁剪
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = config.scale
                                scaleY = config.scale
                                this.translationX = translationX
                                this.translationY = translationY
                            }
                    )
                }
            }
        }
    }
}
