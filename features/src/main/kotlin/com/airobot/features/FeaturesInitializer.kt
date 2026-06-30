package com.airobot.features

import com.airobot.features.aiserv.notepad.AiNotepadProcessor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FeaturesInitializer — Unified entry point for eagerly initializing background
 * and application-scoped components inside the features module.
 */
@Singleton
class FeaturesInitializer @Inject constructor(
    private val aiNotepadProcessor: AiNotepadProcessor
) {
    /**
     * Initializes all background processors and engines.
     * Called by the application module at boot.
     */
    fun initialize() {
        aiNotepadProcessor.start()
    }
}
