package com.airobot.assistant.ui.comp.services

/**
 * 服务卡片子状态
 */
enum class ServiceSubState {
    IDLE,       // 空闲
    RUNNING,    // 运行中
    PAUSED,     // 已暂停
    COMPLETED,  // 已完成
    CANCELLED   // 已取消
}

/**
 * 服务卡片类型
 */
enum class ServiceCardType {
    PODCAST,        // AI播客播放器
    PODCAST_DIY,    // AI播客DIY新节目
    NOTEPAD         // AI记事本
}

/**
 * 服务卡片具体数据接口
 */
sealed interface ServiceCardData

/**
 * 服务卡片数据
 */
data class ServiceCard(
    val id: String,
    val type: ServiceCardType,
    val title: String,
    val content: String,
    val statusTip: String,
    val iconResId: Int,
    val demoContent: String? = null
)
