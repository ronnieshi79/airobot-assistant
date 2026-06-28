package com.airobot.features.aiserv.cards.notepad

sealed class NotepadItem {
    abstract val id: String
    abstract val timestamp: Long

    data class AiSummary(
        override val id: String,
        override val timestamp: Long,
        val title: String,
        val content: String
    ) : NotepadItem()

    data class FocusCard(
        override val id: String,
        override val timestamp: Long,
        val task: String,
        val duration: Int,
        val targetDuration: Int,
        val insight: String
    ) : NotepadItem()

    data class TimerCard(
        override val id: String,
        override val timestamp: Long,
        val label: String,
        val duration: Int,
        val insight: String
    ) : NotepadItem()

    data class AlarmCard(
        override val id: String,
        override val timestamp: Long,
        val label: String,
        val time: String,
        val insight: String
    ) : NotepadItem()

    data class PodcastCard(
        override val id: String,
        override val timestamp: Long,
        val record: com.airobot.features.aiserv.data.model.PodcastActivityRecord
    ) : NotepadItem()
}
