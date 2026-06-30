package com.airobot.features.aiserv.guidance.models

/**
 * RecommendedCard — Data model for feature overlay recommendations
 * Decoupled from App modules and localized via string resource IDs.
 */
data class RecommendedCard(
    val overlayTag: String,        // OverlayTags constant
    val titleResId: Int,           // string resource id
    val contentResId: Int,         // string resource id
    val statusTipResId: Int,       // string resource id
    val iconResId: Int,            // drawable resource id
    val basePriority: Int = 50     // Default sorting weight
)
