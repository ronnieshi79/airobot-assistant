package com.airobot.assistant.assembly

import com.airobot.features.aiserv.popup.OverlayTags
import com.airobot.framework.R

/**
 * App overlay card state definition
 */
data class ServiceCard(
    val id: String,
    val overlayTag: String,
    val title: String,
    val content: String,
    val statusTip: String,
    val iconResId: Int,
    val demoContent: String? = null
)

/**
 * Pre-defined service cards
 */
val DEFAULT_SERVICE_CARDS = listOf(
    ServiceCard(
        id = "card-podcast",
        overlayTag = OverlayTags.PODCAST,
        title = "AI播客",
        content = "你的专属智能播客",
        statusTip = "听点有意思的",
        iconResId = R.drawable.music,
        demoContent = "AI播客需要后端支持"
    ),
    ServiceCard(
        id = "card-podcast-diy",
        overlayTag = OverlayTags.DIY_PODCAST,
        title = "播客DIY",
        content = "创作你的播客节目",
        statusTip = "来点灵感",
        iconResId = R.drawable.palette,
        demoContent = "DIY功能需要后端支持"
    ),
    ServiceCard(
        id = "card-notepad",
        overlayTag = OverlayTags.LOGBOOK,
        title = "AI记事本",
        content = "智能记录灵感",
        statusTip = "记下你的想法",
        iconResId = R.drawable.book,
        demoContent = "记事本功能需要后端支持"
    )
)
