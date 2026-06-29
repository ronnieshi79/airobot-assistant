package com.airobot.assistant.ui.comp.services

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.R
import com.airobot.assistant.ui.comp.services.ServiceCard
import com.airobot.assistant.ui.comp.services.ServiceCardType
import com.airobot.framework.theme.RobotTheme

/**
 * 服务卡片组件
 *
 * Web原型对应: ProactiveServiceKit.tsx
 *
 * 功能:
 * - 显示服务卡片信息
 * - 点击交互
 * - 进度条动画
 */
@Composable
fun ServiceCardItem(
    card: ServiceCard,
    onClick: () -> Unit,
    showProgress: Boolean = true,
    progressDuration: Int = 10000,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }

    val offsetX by animateFloatAsState(
        targetValue = if (isHovered) 8f else 0f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "cardOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (RobotTheme.isDark) {
                        listOf(
                            RobotTheme.colors.cardBg.copy(alpha = if (isHovered) 0.95f else 0.85f),
                            RobotTheme.colors.surfaceOverlay.copy(alpha = if (isHovered) 0.1f else 0.05f)
                        )
                    } else {
                        listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.8f)
                        )
                    }
                )
            )
            .clickable {
                isHovered = true
                onClick()
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                RobotTheme.colors.accent,
                                RobotTheme.colors.accentBg
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = getServiceCardIcon(card.type)),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            // 内容
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.title,
                        color = RobotTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 闪烁图标（hover时显示）
                    AnimatedVisibility(
                        visible = isHovered,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.star),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = RobotTheme.colors.accent
                        )
                    }
                }
                Text(
                    text = card.content,
                    color = RobotTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // 箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = if (isHovered) 4.dp else 0.dp),
                tint = RobotTheme.colors.accent.copy(alpha = if (isHovered) 1f else 0.3f)
            )
        }

        // 进度条
        if (showProgress) {
            CardProgressBar(
                duration = progressDuration,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}

/**
 * 卡片进度条
 */
@Composable
private fun CardProgressBar(
    duration: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = duration, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(1.dp))
            .background(RobotTheme.colors.accent.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.value)
                .fillMaxHeight()
                .background(RobotTheme.colors.accent)
        )
    }
}

/**
 * 获取服务卡片图标
 */
fun getServiceCardIcon(type: ServiceCardType): Int {
    return when (type) {
        ServiceCardType.PODCAST -> R.drawable.music
        ServiceCardType.PODCAST_DIY -> R.drawable.palette
        ServiceCardType.NOTEPAD -> R.drawable.book
    }
}

/**
 * 预定义的服务卡片池
 */
val DEFAULT_SERVICE_CARDS = listOf(
    ServiceCard(
        id = "card-podcast",
        type = ServiceCardType.PODCAST,
        title = "AI播客",
        content = "你的专属智能播客",
        statusTip = "听点有意思的",
        iconResId = R.drawable.music,
        demoContent = "AI播客需要后端支持"
    ),
    ServiceCard(
        id = "card-podcast-diy",
        type = ServiceCardType.PODCAST_DIY,
        title = "播客DIY",
        content = "创作你的播客节目",
        statusTip = "来点灵感",
        iconResId = R.drawable.palette,
        demoContent = "DIY功能需要后端支持"
    ),
    ServiceCard(
        id = "card-notepad",
        type = ServiceCardType.NOTEPAD,
        title = "AI记事本",
        content = "智能记录灵感",
        statusTip = "记下你的想法",
        iconResId = R.drawable.book,
        demoContent = "记事本功能需要后端支持"
    )
)


