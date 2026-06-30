package com.airobot.features

import com.airobot.features.aiserv.notepad.AiNotepadProcessor
import com.airobot.features.aiserv.guidance.CardRegistry
import com.airobot.features.aiserv.guidance.RecommendationEngine
import com.airobot.features.aiserv.guidance.data.RecommendedCard
import com.airobot.features.aiserv.popup.OverlayTags
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FeaturesModule — Unified entry point for eagerly initializing background
 * and application-scoped components inside the features module.
 */
@Singleton
class FeaturesModule @Inject constructor(
    private val aiNotepadProcessor: AiNotepadProcessor,
    private val cardRegistry: CardRegistry,
    private val recommendationEngine: RecommendationEngine
) {
    /**
     * Backward-compatible initialize method.
     */
    fun initialize() {
        initialize(emptyList())
    }

    /**
     * Initializes all background processors and engines with the host application's supported cards.
     */
    fun initialize(supportedCardTags: List<String>) {
        aiNotepadProcessor.start()
        registerDefaultCards()
        recommendationEngine.setSupportedTags(supportedCardTags)
    }

    private fun registerDefaultCards() {
        // Podcast Overlays
        cardRegistry.register(
            RecommendedCard(
                overlayTag = OverlayTags.PODCAST,
                titleResId = R.string.card_podcast_title,
                contentResId = R.string.card_podcast_content,
                statusTipResId = R.string.card_podcast_tip,
                iconResId = com.airobot.framework.R.drawable.music,
                basePriority = 60
            )
        )
        cardRegistry.register(
            RecommendedCard(
                overlayTag = OverlayTags.DIY_PODCAST,
                titleResId = R.string.card_podcast_diy_title,
                contentResId = R.string.card_podcast_diy_content,
                statusTipResId = R.string.card_podcast_diy_tip,
                iconResId = com.airobot.framework.R.drawable.palette,
                basePriority = 50
            )
        )

        // AiServ / Notepad Overlays
        cardRegistry.register(
            RecommendedCard(
                overlayTag = OverlayTags.LOGBOOK,
                titleResId = R.string.card_notepad_title,
                contentResId = R.string.card_notepad_content,
                statusTipResId = R.string.card_notepad_tip,
                iconResId = com.airobot.framework.R.drawable.book,
                basePriority = 40
            )
        )

        // Clock Overlays
        cardRegistry.register(
            RecommendedCard(
                overlayTag = OverlayTags.ALARM,
                titleResId = R.string.card_alarm_title,
                contentResId = R.string.card_alarm_content,
                statusTipResId = R.string.card_alarm_tip,
                iconResId = com.airobot.framework.R.drawable.alarm,
                basePriority = 70
            )
        )
        cardRegistry.register(
            RecommendedCard(
                overlayTag = OverlayTags.TIMER,
                titleResId = R.string.card_timer_title,
                contentResId = R.string.card_timer_content,
                statusTipResId = R.string.card_timer_tip,
                iconResId = com.airobot.framework.R.drawable.timer,
                basePriority = 55
            )
        )
        cardRegistry.register(
            RecommendedCard(
                overlayTag = OverlayTags.FOCUS,
                titleResId = R.string.card_focus_title_rec,
                contentResId = R.string.card_focus_content_rec,
                statusTipResId = R.string.card_focus_tip_rec,
                iconResId = com.airobot.framework.R.drawable.timer, // uses timer icon
                basePriority = 65
            )
        )

        // Schedule Overlays
        cardRegistry.register(
            RecommendedCard(
                overlayTag = OverlayTags.SCHEDULE_PLANNER,
                titleResId = R.string.card_schedule_title,
                contentResId = R.string.card_schedule_content,
                statusTipResId = R.string.card_schedule_tip,
                iconResId = com.airobot.framework.R.drawable.book, // uses book icon
                basePriority = 45
            )
        )
    }
}
