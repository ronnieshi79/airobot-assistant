package com.airobot.assistant.settings

import androidx.compose.foundation.background
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
import com.airobot.core.system.model.SystemInfo
import com.airobot.framework.comp.ConfigTextField
import com.airobot.framework.theme.RobotTheme

/**
 * 角色管理配置页面
 * 展示当前角色的切换下拉框，以及对应的角色引擎、语音模型、唤醒词等配置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleConfig(
    systemInfo: SystemInfo,
    onRoleSelected: (Int) -> Unit
) {
    val roles = systemInfo.aiRobotArray.filterNotNull()
    val activeIndex = systemInfo.activeRoleIndex
    val activeRole = systemInfo.aiRobotArray.getOrNull(activeIndex) ?: roles.firstOrNull()
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // —— 当前角色 ——
        Text(
            "当前角色",
            color = RobotTheme.colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = activeRole?.roleName ?: "无",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedTextColor = RobotTheme.colors.textPrimary,
                        unfocusedTextColor = RobotTheme.colors.textPrimary,
                        focusedBorderColor = RobotTheme.colors.accent,
                        unfocusedBorderColor = RobotTheme.colors.cardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    systemInfo.aiRobotArray.forEachIndexed { index, robot ->
                        if (robot != null) {
                            DropdownMenuItem(
                                text = { Text(robot.roleName) },
                                onClick = {
                                    onRoleSelected(index)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // —— 角色详情 ——
        Text(
            "角色信息",
            color = RobotTheme.colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        val engineDisplay = when (activeRole?.characterType) {
            "ANDROID_CANVAS" -> "原生 Canvas 动画 (Aether)"
            "RIVE_IP" -> "Rive 引擎动画 (心小苗)"
            else -> activeRole?.characterType ?: "未知引擎"
        }

        ConfigTextField(
            label = "形象模型引擎",
            value = engineDisplay,
            onValueChange = {},
            readOnly = true
        )

        ConfigTextField(
            label = "性格特征",
            value = activeRole?.personality ?: "无",
            onValueChange = {},
            readOnly = true
        )

        ConfigTextField(
            label = "语音模型",
            value = activeRole?.voiceModel ?: "火山模型",
            onValueChange = {},
            readOnly = true
        )

        ConfigTextField(
            label = "唤醒词",
            value = activeRole?.wakeWords ?: "无",
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
