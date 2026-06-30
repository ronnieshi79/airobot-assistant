package com.airobot.features.aiserv.popup

/**
 * OverlayTags — Single source of truth for all global overlay identifiers.
 * Replaces magic strings and guarantees type safety when triggering full-screen overlays.
 */
object OverlayTags {
    // Clock Feature Overlays
    const val ALARM = "ALARM"
    const val TIMER = "TIMER"
    const val FOCUS = "FOCUS"
    const val HOURLY_CHIME = "HOURLY_CHIME"
    const val CHIME = "CHIME" // Used by HourlyChimeManager

    // Podcast Feature Overlays
    const val PODCAST = "PODCAST"
    const val DIY_PODCAST = "DIY_PODCAST"

    // Schedule Feature Overlays
    const val SCHEDULE_PLANNER = "SCHEDULE_PLANNER"

    // AiServ / Global Overlays
    const val LOGBOOK = "LOGBOOK"
}
