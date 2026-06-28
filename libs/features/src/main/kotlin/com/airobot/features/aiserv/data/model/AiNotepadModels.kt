package com.airobot.features.aiserv.data.model

import kotlinx.serialization.Serializable

/**
 * Data class representing a historical record of an alarm trigger event in Notepad.
 */
@Serializable
data class AlarmRecord(
    val id: String,
    val label: String,
    val time: String,
    val triggerTime: Long,
    val insight: String = ""   // Placeholder for AI insights/analysis
)

/**
 * Data class representing a historical record of a timer session in Notepad.
 */
@Serializable
data class TimerRecord(
    val id: String,
    val label: String,
    val duration: Int,         // Session duration in seconds
    val timestamp: Long,
    val insight: String = "",  // Placeholder for AI insights
    val startTime: Long = 0L,
    val remindersCount: Int = 0,
    val closeReason: String = "completed" // "completed", "user_interrupted", "emergency_stopped"
)

/**
 * Data class representing a historical record of a focus session in Notepad.
 */
@Serializable
data class FocusRecord(
    val id: String,
    val task: String,
    val duration: Int,         // Actual focus time in seconds
    val targetDuration: Int,   // Configured target focus time in seconds
    val startTime: Long,
    val insight: String = "",  // Placeholder for AI insights
    val remindersCount: Int = 0,
    val closeReason: String = "completed" // "completed", "user_interrupted", "emergency_stopped"
)

/**
 * Sealed class representing various podcast activity records saved in Notepad.
 */
@Serializable
sealed class PodcastActivityRecord {
    abstract val id: String
    abstract val timestamp: Long
    abstract val insight: String

    @Serializable
    data class PlaybackRecord(
        override val id: String,
        val episodeId: String,
        val title: String,
        val type: String,
        val channelName: String,
        val startTime: Long,
        val totalListenedMs: Long,
        val currentProgressPercent: Float,
        val isCompleted: Boolean = false,
        override val timestamp: Long, // Last updated time
        override val insight: String = ""
    ) : PodcastActivityRecord()

    @Serializable
    data class CreationRecord(
        override val id: String,
        val episodeId: String,
        val title: String,
        val type: String,
        val channelName: String,
        val isDiy: Boolean,
        override val timestamp: Long,
        override val insight: String = ""
    ) : PodcastActivityRecord()

    @Serializable
    data class FavoriteRecord(
        override val id: String,
        val episodeId: String,
        val title: String,
        val favorite: Boolean,
        override val timestamp: Long,
        override val insight: String = ""
    ) : PodcastActivityRecord()

    @Serializable
    data class SubscriptionRecord(
        override val id: String,
        val channelId: String,
        val channelTitle: String,
        val isSubscribed: Boolean,
        override val timestamp: Long,
        override val insight: String = ""
    ) : PodcastActivityRecord()
}

