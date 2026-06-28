package com.airobot.framework.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.framework.theme.RobotTheme

@Composable
fun OverlayBackdrop(
    onClose: () -> Unit,
    enabled: Boolean = true,
    clickThrough: Boolean = false,
    backdropAlpha: Float = 0.65f,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = RobotTheme.isDark
    val baseColor = if (isDark) Color(0xFF090D16) else Color(0xFFF1F5F9)
    val backdropColor = baseColor.copy(alpha = backdropAlpha)

    val baseModifier = Modifier
        .fillMaxSize()
        .background(backdropColor)

    val finalModifier = if (!clickThrough) {
        baseModifier.clickable(
            enabled = enabled,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClose
        )
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun OverlayServiceCard(
    onClose: () -> Unit,
    width: Dp = 360.dp,
    height: Dp = 480.dp,
    shape: Shape = RoundedCornerShape(32.dp),
    showCloseButton: Boolean = true,
    containerBackground: Brush = Brush.verticalGradient(
        if (RobotTheme.isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        else listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
    ),
    containerBorder: Color = if (RobotTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = RobotTheme.isDark

    OverlayBackdrop(
        onClose = onClose,
        enabled = true
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(shape)
                .background(containerBackground)
                .border(2.dp, containerBorder, shape)
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            content()

            if (showCloseButton) {
                // Close button (Top Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
