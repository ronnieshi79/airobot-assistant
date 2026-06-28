package com.airobot.assistant.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.airbot.domain.model.Character
import com.airobot.framework.comp.ConfigTextField
import com.airobot.framework.theme.RobotTheme

@Composable
fun RoleConfig(
    characters: List<Character>,
    activeCharacter: Character?,
    onRoleSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // —— 当前角色 ——
        Text(
            "当前角色",
            color = RobotTheme.colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // Pill-segmented Tab Row for switching roles (maximum 4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RobotTheme.colors.cardBg)
                .border(1.dp, RobotTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            characters.forEach { robot ->
                val isSelected = activeCharacter?.roleName == robot.roleName
                val bgSelectedColor = RobotTheme.colors.accent.copy(alpha = 0.15f)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) bgSelectedColor else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onRoleSelected(robot.roleName) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = robot.roleName,
                        color = if (isSelected) RobotTheme.colors.accent else RobotTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // —— 角色详情配置 ——
        val engineDisplay = when (activeCharacter?.characterType) {
            "ANDROID_CANVAS" -> "原生 Canvas 动画 (Aether)"
            "RIVE_IP" -> "Rive 引擎动画 (${activeCharacter.roleName})"
            else -> activeCharacter?.characterType ?: "未知引擎"
        }

        ConfigTextField(
            label = "动画引擎",
            value = engineDisplay,
            onValueChange = {},
            readOnly = true
        )

        ConfigTextField(
            label = "性格特征",
            value = activeCharacter?.personality ?: "无",
            onValueChange = {},
            readOnly = true
        )

        ConfigTextField(
            label = "语音模型",
            value = activeCharacter?.voiceModel ?: "火山模型",
            onValueChange = {},
            readOnly = true
        )

        ConfigTextField(
            label = "唤醒词",
            value = activeCharacter?.wakeWord ?: "无",
            onValueChange = {},
            readOnly = true
        )

        // —— 底部提示 ——
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RobotTheme.colors.accent.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "提示",
                    tint = RobotTheme.colors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "切换角色后，系统形象与交互风格将自动切换。",
                    color = RobotTheme.colors.textPrimary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
