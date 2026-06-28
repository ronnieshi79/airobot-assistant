package com.airobot.features

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.aiserv.cards.AiNotepadOverlay
import com.airobot.features.clock.cards.AlarmCard
import com.airobot.features.clock.cards.AlarmOverlay
import com.airobot.features.clock.cards.ClockHomeCard
import com.airobot.features.clock.cards.HourlyChimeOverlay
import com.airobot.features.clock.cards.TimerCard
import com.airobot.features.clock.cards.TimerOverlay
import com.airobot.features.clock.viewmodel.ClockViewModel
import com.airobot.features.clock.viewmodel.TimerStartResult
import com.airobot.features.podcast.cards.PodcastDiyOverlay
import com.airobot.features.podcast.cards.PodcastHomeCard
import com.airobot.features.podcast.cards.PodcastLibraryCard
import com.airobot.features.podcast.cards.PodcastPlayerOverlay
import com.airobot.features.podcast.cards.PodcastSubscribeCard
import com.airobot.features.podcast.viewmodel.PodcastViewModel
import com.airobot.features.schedule.cards.ScheduleBoardCard
import com.airobot.features.schedule.cards.ScheduleHomeCard
import com.airobot.features.schedule.cards.ScheduleListCard
import com.airobot.features.schedule.cards.SchedulePlannerOverlay
import com.airobot.features.schedule.viewmodel.ScheduleViewModel
import com.airobot.features.state.OverlayType
import com.airobot.features.state.OverlayViewModel
import com.airobot.features.state.PopupServiceType
import com.airobot.features.state.SubCategory

/**
 * FeatureScreens — Single facade offering composable entry points for all cards and overlays.
 *
 * Encapsulates viewmodel flow collections and internal feature routing, enabling the top-level
 * AppMainScreen shell to remain lightweight and completely decoupled from feature submodules.
 */
object FeatureScreens {

    @Composable
    fun FunctionalCard(
        subCategory: SubCategory,
        modifier: Modifier = Modifier,
        clockViewModel: ClockViewModel = hiltViewModel(),
        overlayViewModel: OverlayViewModel = hiltViewModel(),
        scheduleViewModel: ScheduleViewModel = hiltViewModel(),
        onNavigateToSubCategory: (SubCategory) -> Unit = {}
    ) {
        // Collect states internally from ClockViewModel
        val alarms by clockViewModel.alarms.collectAsState()
        val timerPresets by clockViewModel.timerPresets.collectAsState()
        val timerSeconds by clockViewModel.timerSeconds.collectAsState()

        val context = androidx.compose.ui.platform.LocalContext.current
        AnimatedContent(
            targetState = subCategory,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically { it / 4 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically { -it / 4 })
            },
            label = "cardSwitch",
            modifier = modifier
        ) { targetSub ->
            when (targetSub) {
                // Clock sub-pages
                SubCategory.CLOCK_HOME -> ClockHomeCard(
                    onAlarmClick = {
                        val targetAlarm = alarms.firstOrNull { it.enabled } ?: alarms.firstOrNull()
                        clockViewModel.setSelectedAlarmId(targetAlarm?.id)
                        clockViewModel.clearAlarmBackgrounded()
                        overlayViewModel.showOverlay(OverlayType.ALARM)
                    },
                    onFocusClick = {
                        overlayViewModel.showOverlay(OverlayType.TIMER)
                    },
                    onTimerClick = {
                        overlayViewModel.showOverlay(OverlayType.TIMER)
                    },
                    onChimeClick = {
                        overlayViewModel.showOverlay(OverlayType.HOURLY_CHIME)
                    },
                    onRemindClick = { action ->
                        when (action) {
                            "focus" -> overlayViewModel.showOverlay(OverlayType.TIMER)
                            "logbook" -> overlayViewModel.showOverlay(OverlayType.LOGBOOK)
                            "timer" -> overlayViewModel.showOverlay(OverlayType.TIMER)
                        }
                    }
                )

                SubCategory.CLOCK_ALARM -> AlarmCard(
                    alarms = alarms,
                    onToggleAlarm = { id -> clockViewModel.toggleAlarm(id) },
                    onUpdateAlarm = { id, updated -> clockViewModel.updateAlarm(id, updated) },
                    onItemClick = { alarm ->
                        clockViewModel.setSelectedAlarmId(alarm.id)
                        clockViewModel.clearAlarmBackgrounded()
                        overlayViewModel.showOverlay(OverlayType.ALARM)
                    }
                )

                SubCategory.CLOCK_TIMER -> TimerCard(
                    presets = timerPresets,
                    onUpdatePreset = { id, preset -> clockViewModel.updateTimerPreset(id, preset) },
                    onPresetClick = { preset ->
                        val startResult = clockViewModel.startTimerSession(preset)
                        when (startResult) {
                            TimerStartResult.STARTED -> {
                                overlayViewModel.showOverlay(OverlayType.TIMER)
                            }

                            TimerStartResult.DUPLICATE_PRESET -> {
                                clockViewModel.bringTimerToForeground(preset.id)
                                overlayViewModel.showTopAlert(
                                    context.resources.getString(R.string.timer_blocked_duplicate_preset)
                                )
                                overlayViewModel.showOverlay(OverlayType.TIMER)
                            }

                            TimerStartResult.FOCUS_ACTIVE -> {
                                overlayViewModel.showTopAlert(
                                    context.resources.getString(R.string.timer_blocked_focus_active)
                                )
                            }

                            TimerStartResult.COUNTDOWN_BLOCKS_FOCUS -> {
                                overlayViewModel.showTopAlert(
                                    context.resources.getString(R.string.timer_blocked_concurrent_focus)
                                )
                            }
                        }
                    }
                )

                // Podcast sub-pages
                SubCategory.PODCAST_HOME -> PodcastHomeCard(
                    onPlayClick = {
                        overlayViewModel.showOverlay(OverlayType.PODCAST)
                    },
                    onRemindClick = { action ->
                        when (action) {
                            "podcast" -> overlayViewModel.showOverlay(OverlayType.PODCAST)
                            "logbook" -> overlayViewModel.showOverlay(OverlayType.LOGBOOK)
                            "diy" -> overlayViewModel.showOverlay(OverlayType.DIY_PODCAST)
                        }
                    },
                    onNavigateToLibrary = {
                        onNavigateToSubCategory(SubCategory.PODCAST_LIBRARY)
                    }
                )

                SubCategory.PODCAST_LIBRARY -> PodcastLibraryCard(
                    onPlayClick = {
                        overlayViewModel.showOverlay(OverlayType.PODCAST)
                    }
                )

                SubCategory.PODCAST_SUBSCRIBE -> PodcastSubscribeCard()

                // Schedule sub-pages
                SubCategory.SCHEDULE_HOME -> ScheduleHomeCard(
                    scheduleViewModel = scheduleViewModel,
                    onNavigateToSubCategory = onNavigateToSubCategory,
                    onRemindClick = { action ->
                        when (action) {
                            "schedule" -> overlayViewModel.showOverlay(OverlayType.SCHEDULE_PLANNER)
                            "logbook" -> overlayViewModel.showOverlay(OverlayType.LOGBOOK)
                            "focus" -> overlayViewModel.showOverlay(OverlayType.TIMER)
                        }
                    }
                )

                SubCategory.SCHEDULE_BOARD -> ScheduleBoardCard(
                    scheduleViewModel = scheduleViewModel,
                    onRemindClick = { action ->
                        when (action) {
                            "schedule" -> overlayViewModel.showOverlay(OverlayType.SCHEDULE_PLANNER)
                            "logbook" -> overlayViewModel.showOverlay(OverlayType.LOGBOOK)
                            "focus" -> overlayViewModel.showOverlay(OverlayType.TIMER)
                        }
                    }
                )

                SubCategory.SCHEDULE_LIST -> ScheduleListCard(
                    scheduleViewModel = scheduleViewModel,
                    onRemindClick = { action ->
                        when (action) {
                            "schedule" -> overlayViewModel.showOverlay(OverlayType.SCHEDULE_PLANNER)
                            "logbook" -> overlayViewModel.showOverlay(OverlayType.LOGBOOK)
                            "focus" -> overlayViewModel.showOverlay(OverlayType.TIMER)
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun ActiveOverlay(
        modifier: Modifier = Modifier,
        clockViewModel: ClockViewModel = hiltViewModel(),
        overlayViewModel: OverlayViewModel = hiltViewModel(),
        scheduleViewModel: ScheduleViewModel = hiltViewModel(),
        aiServViewModel: com.airobot.features.aiserv.viewmodel.AiServViewModel = hiltViewModel(),
        popupQueueViewModel: com.airobot.features.state.PopupQueueViewModel = hiltViewModel(),
        onWakeupAirobot: () -> Unit = {}
    ) {
        val activeOverlay by overlayViewModel.activeOverlay.collectAsState()
        val alarms by clockViewModel.alarms.collectAsState()
        val selectedAlarmId by clockViewModel.selectedAlarmId.collectAsState()
        val ringingAlarmId by clockViewModel.ringingAlarmId.collectAsState()
        val timerSeconds by clockViewModel.timerSeconds.collectAsState()
        val totalTimerSeconds by clockViewModel.totalTimerSeconds.collectAsState()
        val isTimerRunning by clockViewModel.isTimerRunning.collectAsState()
        val activeTimerPreset by clockViewModel.activeTimerPreset.collectAsState()
        val activeTimerMode by clockViewModel.activeTimerMode.collectAsState()
        val pendingAlarms by clockViewModel.pendingAlarms.collectAsState()
        val chimeConfig by clockViewModel.chimeConfig.collectAsState()
        val isChiming by clockViewModel.isChiming.collectAsState()
        val isTimerBackgrounded by clockViewModel.isTimerBackgrounded.collectAsState()
        val isAlarmBackgrounded by clockViewModel.isAlarmBackgrounded.collectAsState()

        val alarmHistory by aiServViewModel.alarmHistory.collectAsState()
        val timerHistory by aiServViewModel.timerHistory.collectAsState()
        val focusHistory by aiServViewModel.focusHistory.collectAsState()
        val podcastHistory by aiServViewModel.podcastHistory.collectAsState()

        val topQueueItem by popupQueueViewModel.topQueueItem.collectAsState()

        LaunchedEffect(isTimerBackgrounded, activeTimerPreset) {
            if (isTimerBackgrounded) {
                overlayViewModel.hideOverlay()
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = activeOverlay != OverlayType.NONE,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (activeOverlay) {
                        OverlayType.ALARM -> {
                            val activeAlarm = if (ringingAlarmId != null) {
                                alarms.find { it.id == ringingAlarmId }
                            } else {
                                alarms.find { it.id == selectedAlarmId }
                                    ?: alarms.firstOrNull { it.enabled } ?: alarms.firstOrNull()
                            }

                            AlarmOverlay(
                                alarm = activeAlarm,
                                ringingAlarmId = ringingAlarmId,
                                alarms = alarms,
                                onToggleAlarm = { activeAlarm?.let { clockViewModel.toggleAlarm(it.id) } },
                                onDismissAlarm = {
                                    clockViewModel.dismissAlarm()
                                    overlayViewModel.hideOverlay()
                                },
                                onMinimizeAlarm = { silence ->
                                    clockViewModel.minimizeAlarm(silence)
                                    overlayViewModel.hideOverlay()
                                },
                                onClose = { overlayViewModel.hideOverlay() }
                            )
                        }

                        OverlayType.HOURLY_CHIME -> {
                            HourlyChimeOverlay(
                                config = chimeConfig,
                                isChiming = isChiming,
                                onConfigChange = { clockViewModel.updateChimeConfig(it) },
                                onDismissChime = {
                                    clockViewModel.dismissChime()
                                    overlayViewModel.hideOverlay()
                                },
                                onClose = { overlayViewModel.hideOverlay() }
                            )
                        }

                        OverlayType.TIMER -> {
                            TimerOverlay(
                                timerSeconds = timerSeconds,
                                totalSeconds = totalTimerSeconds,
                                isRunning = isTimerRunning,
                                preset = activeTimerPreset,
                                mode = activeTimerMode,
                                pendingAlarms = pendingAlarms,
                                onToggle = { clockViewModel.toggleTimerRunning() },
                                onReset = { clockViewModel.resetTimerSession() },
                                onSendToBackground = {
                                    clockViewModel.sendTimerToBackground()
                                    overlayViewModel.hideOverlay()
                                },
                                onEmergencyStop = {
                                    clockViewModel.emergencyStopTimer()
                                    clockViewModel.clearPendingAlarms()
                                    overlayViewModel.hideOverlay()
                                },
                                onClearPendingAlarms = { clockViewModel.clearPendingAlarms() },
                                onShowConstraintAlert = { overlayViewModel.showTopAlert(it) },
                                onClose = {
                                    clockViewModel.clearPendingAlarms()
                                    overlayViewModel.hideOverlay()
                                }
                            )
                        }

                        OverlayType.PODCAST -> PodcastPlayerOverlay(
                            onClose = { overlayViewModel.hideOverlay() },
                            onWakeupAirobot = onWakeupAirobot
                        )

                        OverlayType.DIY_PODCAST -> {
                            val podcastViewModel: PodcastViewModel = hiltViewModel()
                            PodcastDiyOverlay(
                                podcastViewModel = podcastViewModel,
                                onClose = { overlayViewModel.hideOverlay() }
                            )
                        }

                        OverlayType.LOGBOOK -> AiNotepadOverlay(
                            alarmHistory = alarmHistory,
                            timerHistory = timerHistory,
                            focusHistory = focusHistory,
                            podcastHistory = podcastHistory,
                            onClose = { overlayViewModel.hideOverlay() }
                        )

                        OverlayType.SCHEDULE_PLANNER -> SchedulePlannerOverlay(
                            scheduleViewModel = scheduleViewModel,
                            onClose = { overlayViewModel.hideOverlay() }
                        )

                        OverlayType.NONE -> {}
                    }
                }
            }

            // Dynamic Queue Overlays
            val isQueueOverlayVisible = topQueueItem != null && topQueueItem!!.isFullScreen &&
                !(activeOverlay == OverlayType.TIMER &&
                    (topQueueItem!!.serviceType == PopupServiceType.TIMER || topQueueItem!!.serviceType == PopupServiceType.FOCUS))
            AnimatedVisibility(
                visible = isQueueOverlayVisible,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    topQueueItem?.Content()
                }
            }
        }
    }
}
