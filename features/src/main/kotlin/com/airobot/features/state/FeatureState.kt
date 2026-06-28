package com.airobot.features.state

/**
 * Main function module categories — drives SkeuomorphicDial outer ring
 * Prototype ref: types.ts → MainCategory
 */
enum class MainCategory {
    CLOCK,      // AI Clock
    PODCAST,    // AI Podcast
    SCHEDULE    // AI Schedule
}

/**
 * Sub-category pages within each main module — drives SkeuomorphicDial inner knob
 * Prototype ref: types.ts → SubCategory
 */
enum class SubCategory {
    // Clock sub-pages
    CLOCK_HOME,         // Clock Home (Skeuomorphic Dial)
    CLOCK_ALARM,        // Alarm Management
    CLOCK_TIMER,        // Timer

    // Schedule sub-pages
    SCHEDULE_HOME,      // Schedule Home (Today Overview)
    SCHEDULE_BOARD,     // Schedule Board (Timeline)
    SCHEDULE_LIST,      // Todo List

    // Podcast sub-pages
    PODCAST_HOME,       // Podcast Home (Featured)
    PODCAST_LIBRARY,    // Podcast Library
    PODCAST_SUBSCRIBE   // Subscription Management
}

/**
 * Module navigation state — single source of truth for current view
 */
data class ModuleNavigationState(
    val mainCategory: MainCategory = MainCategory.CLOCK,
    val subCategory: SubCategory = SubCategory.CLOCK_HOME
)

/**
 * Global Overlay Type for full-screen feature views
 */
enum class OverlayType {
    NONE,
    ALARM,
    TIMER,
    PODCAST,
    LOGBOOK,
    SCHEDULE_PLANNER,
    HOURLY_CHIME,
    DIY_PODCAST
}

/**
 * Modes for the Timer feature
 */
enum class TimerMode {
    COUNTDOWN,
    FOCUS
}

// --- Helper Extensions for Navigation ---

/**
 * Get default sub-category for a main category
 */
fun MainCategory.defaultSubCategory(): SubCategory = when (this) {
    MainCategory.CLOCK -> SubCategory.CLOCK_HOME
    MainCategory.SCHEDULE -> SubCategory.SCHEDULE_HOME
    MainCategory.PODCAST -> SubCategory.PODCAST_HOME
}

/**
 * Get ordered sub-categories for cycling via dial center-click
 */
fun MainCategory.subCategories(): List<SubCategory> = when (this) {
    MainCategory.CLOCK -> listOf(
        SubCategory.CLOCK_HOME,
        SubCategory.CLOCK_ALARM,
        SubCategory.CLOCK_TIMER
    )
    MainCategory.SCHEDULE -> listOf(
        SubCategory.SCHEDULE_HOME,
        SubCategory.SCHEDULE_BOARD,
        SubCategory.SCHEDULE_LIST
    )
    MainCategory.PODCAST -> listOf(
        SubCategory.PODCAST_HOME,
        SubCategory.PODCAST_LIBRARY,
        SubCategory.PODCAST_SUBSCRIBE
    )
}

/**
 * Cycle to next sub-category within the same main category
 */
fun ModuleNavigationState.cycleSubCategory(): ModuleNavigationState {
    val subs = mainCategory.subCategories()
    val currentIndex = subs.indexOf(subCategory)
    val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % subs.size
    return copy(subCategory = subs[nextIndex])
}

/**
 * Get sub-category index within its main category (for dial inner knob rotation)
 */
fun SubCategory.indexInParent(): Int {
    val parent = this.parentCategory()
    return parent.subCategories().indexOf(this).coerceAtLeast(0)
}

/**
 * Get parent main category for a sub-category
 */
fun SubCategory.parentCategory(): MainCategory = when (this) {
    SubCategory.CLOCK_HOME, SubCategory.CLOCK_ALARM,
    SubCategory.CLOCK_TIMER -> MainCategory.CLOCK

    SubCategory.SCHEDULE_HOME, SubCategory.SCHEDULE_BOARD,
    SubCategory.SCHEDULE_LIST -> MainCategory.SCHEDULE

    SubCategory.PODCAST_HOME, SubCategory.PODCAST_LIBRARY,
    SubCategory.PODCAST_SUBSCRIBE -> MainCategory.PODCAST
}

/**
 * Display label for main category
 */
fun MainCategory.displayLabel(): String = when (this) {
    MainCategory.CLOCK -> "AI Clock"
    MainCategory.SCHEDULE -> "AI Schedule"
    MainCategory.PODCAST -> "AI Podcast"
}
