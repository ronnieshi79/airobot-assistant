package com.airobot.features

import com.airobot.features.aiserv.notepad.AiNotepadProcessor
import com.airobot.features.aiserv.guidance.CardRegistry
import com.airobot.features.aiserv.guidance.models.RecommendedCard
import com.airobot.features.aiserv.popup.OverlayTags
import com.airobot.features.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FeaturesInitializer — Unified entry point for eagerly initializing background
 * and application-scoped components inside the features module.
 */
@Singleton
class FeaturesInitializer @Inject constructor(
    private val aiNotepadProcessor: AiNotepadProcessor,
    private val cardRegistry: CardRegistry
) {
    /**
     * Initializes all background processors and engines.
     * Called by the application module at boot.
     */
    fun initialize() {
        aiNotepadProcessor.start()
        registerDefaultCards()
    }

    private fun registerDefaultCards() {
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
    }
}

