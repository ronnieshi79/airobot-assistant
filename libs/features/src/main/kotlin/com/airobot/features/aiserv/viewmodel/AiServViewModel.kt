package com.airobot.features.aiserv.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airobot.features.aiserv.data.AiNotepadRepository
import com.airobot.features.aiserv.data.model.AlarmRecord
import com.airobot.features.aiserv.data.model.TimerRecord
import com.airobot.features.aiserv.data.model.FocusRecord
import com.airobot.features.aiserv.data.model.PodcastActivityRecord
import com.airobot.features.aiserv.event.AiEvent
import com.airobot.features.aiserv.event.AiEventDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AiServViewModel — Architecture orchestrator for all AI Services, specifically owning
 * the skeuomorphic AI Notepad (AiNotepad) closed-loop activity logging and persistence logic.
 */
@HiltViewModel
class AiServViewModel @Inject constructor(
    private val aiNotepadRepository: AiNotepadRepository,
    private val aiEventDispatcher: AiEventDispatcher
) : ViewModel() {

    companion object {
        private const val TAG = "AiServViewModel"
    }

    // --- Notepad Persistent History States ---
    private val _alarmHistory = MutableStateFlow<List<AlarmRecord>>(emptyList())
    val alarmHistory: StateFlow<List<AlarmRecord>> = _alarmHistory.asStateFlow()

    private val _timerHistory = MutableStateFlow<List<TimerRecord>>(emptyList())
    val timerHistory: StateFlow<List<TimerRecord>> = _timerHistory.asStateFlow()

    private val _focusHistory = MutableStateFlow<List<FocusRecord>>(emptyList())
    val focusHistory: StateFlow<List<FocusRecord>> = _focusHistory.asStateFlow()

    private val _podcastHistory = MutableStateFlow<List<PodcastActivityRecord>>(emptyList())
    val podcastHistory: StateFlow<List<PodcastActivityRecord>> = _podcastHistory.asStateFlow()

    init {
        Log.d(TAG, "Initializing AiServViewModel closed-loop AI Notepad service")
        
        // 1. Load existing persisted Notepad history
        viewModelScope.launch {
            try {
                _alarmHistory.value = aiNotepadRepository.loadAlarmHistory()
                _timerHistory.value = aiNotepadRepository.loadTimerHistory()
                _focusHistory.value = aiNotepadRepository.loadFocusHistory()
                _podcastHistory.value = aiNotepadRepository.loadPodcastHistory()
                Log.d(TAG, "Loaded Notepad history: alarm=${_alarmHistory.value.size}, timer=${_timerHistory.value.size}, focus=${_focusHistory.value.size}, podcast=${_podcastHistory.value.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load AiNotepad history", e)
            }
        }

        // 2. Reactively subscribe to the features AI event dispatcher stream
        viewModelScope.launch {
            aiEventDispatcher.events.collect { event ->
                Log.d(TAG, "Reactive Event Received: $event")
                saveEventToNotepad(event)
            }
        }
    }

    /**
     * Persists incoming AI events straight to local Notepad preferences and updates StateFlow.
     */
    private fun saveEventToNotepad(event: AiEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is AiEvent.AlarmTriggered -> {
                        val record = AlarmRecord(
                            id = event.id,
                            label = event.label,
                            time = event.time,
                            triggerTime = event.triggerTime,
                            insight = if (event.label == "准点报时") "新的一小时开始了，祝您心情愉快！" else "您在 ${event.time} 准时被唤醒，美好的一天从现在开始！"
                        )
                        val newList = listOf(record) + _alarmHistory.value
                        _alarmHistory.value = newList
                        aiNotepadRepository.saveAlarmHistory(newList)
                        Log.d(TAG, "Persisted AlarmRecord to Notepad")
                    }
                    is AiEvent.TimerStarted -> {
                        Log.d(TAG, "AiServ audited TimerStarted: id=${event.id}, label=${event.label}, mode=${event.mode}, duration=${event.duration}s")
                        // Start events are audited in real-time but do not require saving as individual history rows in the Notepad UI list
                    }
                    is AiEvent.TimerReminderTriggered -> {
                        Log.d(TAG, "AiServ audited TimerReminderTriggered: label=${event.label}, elapsed=${event.elapsedSeconds}s, mode=${event.mode}")
                        // Reminder ticks are logged in real-time and counted dynamically for the final insight summary
                    }
                    is AiEvent.TimerFinished -> {
                        if (event.mode == com.airobot.features.state.TimerMode.COUNTDOWN) {
                            val computedInsight = when (event.closeReason) {
                                "completed" -> "您成功完成了「${event.label}」倒计时，共用时 ${event.duration} 秒。中途触发了 ${event.remindersCount} 次提醒，完美达成目标！"
                                "user_interrupted" -> "您中止了「${event.label}」倒计时，实际用时 ${event.duration} 秒（原定 ${event.targetDuration} 秒）。中途触发了 ${event.remindersCount} 次提醒，吸取经验，下一次定能坚持到底！"
                                else -> "您提前结束了「${event.label}」倒计时，实际用时 ${event.duration} 秒。再接再厉！"
                            }
                            val record = TimerRecord(
                                id = event.id,
                                label = event.label,
                                duration = event.duration,
                                timestamp = event.timestamp,
                                insight = computedInsight,
                                startTime = if (event.startTime > 0L) event.startTime else event.timestamp - (event.duration * 1000L),
                                remindersCount = event.remindersCount,
                                closeReason = event.closeReason
                            )
                            val newList = listOf(record) + _timerHistory.value
                            _timerHistory.value = newList
                            aiNotepadRepository.saveTimerHistory(newList)
                            Log.d(TAG, "Persisted TimerRecord to Notepad: reason=${event.closeReason}, reminders=${event.remindersCount}")
                        } else {
                            val computedInsight = when (event.closeReason) {
                                "completed" -> "心流聚焦「${event.label}」完成！在 ${event.duration / 60} 分钟的深度专注中，您保持了极高的脑力负荷，中途触发了 ${event.remindersCount} 次里程碑提醒。效率超乎以往，继续保持！"
                                "emergency_stopped" -> "心流聚焦「${event.label}」中途被强制中断。本次实际专注 ${event.duration / 60} 分钟（原定 ${event.targetDuration / 60} 分钟），中途触发了 ${event.remindersCount} 次专注提醒。别气馁，适当调整后再次出发吧！"
                                else -> "「${event.label}」专注被中止，实际专注时长为 ${event.duration / 60} 分钟。保持脑力，期待你的下一次进入心流状态！"
                            }
                            val actualStartTime = if (event.startTime > 0L) event.startTime else event.timestamp - (event.duration * 1000L)
                            val record = FocusRecord(
                                id = event.id,
                                task = event.label,
                                duration = event.duration,
                                targetDuration = event.targetDuration,
                                startTime = actualStartTime,
                                insight = computedInsight,
                                remindersCount = event.remindersCount,
                                closeReason = event.closeReason
                            )
                            val newList = listOf(record) + _focusHistory.value
                            _focusHistory.value = newList
                            aiNotepadRepository.saveFocusHistory(newList)
                        }
                    }
                    
                    // --- Podcast Events Handling ---
                    is AiEvent.PodcastEpisodeCreated -> {
                        val record = PodcastActivityRecord.CreationRecord(
                            id = "pod_act_${event.timestamp}_${event.episodeId}",
                            episodeId = event.episodeId,
                            title = event.title,
                            type = event.type,
                            channelName = event.channelName,
                            isDiy = event.isDiy,
                            timestamp = event.timestamp,
                            insight = "您成功创作了新节目《${event.title}》（属于 ${event.channelName} 栏目），您的收听库又丰富了！"
                        )
                        val newList = listOf(record) + _podcastHistory.value
                        _podcastHistory.value = newList
                        aiNotepadRepository.savePodcastHistory(newList)
                        Log.d(TAG, "Persisted CreationRecord to Notepad")
                    }
                    
                    is AiEvent.PodcastPlaybackStarted -> {
                        // Consolidation: check if the most recent record is an active PlaybackRecord for the same episode
                        val existingList = _podcastHistory.value
                        val lastIndex = existingList.indexOfFirst { it is PodcastActivityRecord.PlaybackRecord && it.episodeId == event.episodeId }
                        
                        val updatedList = if (lastIndex != -1 && ! (existingList[lastIndex] as PodcastActivityRecord.PlaybackRecord).isCompleted) {
                            // Already has an active (uncompleted) record, no need to prepend. Just update timestamp to show active
                            val oldRecord = existingList[lastIndex] as PodcastActivityRecord.PlaybackRecord
                            val updatedRecord = oldRecord.copy(timestamp = event.timestamp)
                            existingList.toMutableList().apply { set(lastIndex, updatedRecord) }
                        } else {
                            // Create a new PlaybackRecord
                            val newRecord = PodcastActivityRecord.PlaybackRecord(
                                id = "pod_play_${event.timestamp}_${event.episodeId}",
                                episodeId = event.episodeId,
                                title = event.title,
                                type = event.type,
                                channelName = event.channelName,
                                startTime = event.timestamp,
                                totalListenedMs = 0,
                                currentProgressPercent = 0f,
                                isCompleted = false,
                                timestamp = event.timestamp,
                                insight = "您开始收听节目《${event.title}》。"
                            )
                            listOf(newRecord) + existingList
                        }
                        
                        _podcastHistory.value = updatedList
                        aiNotepadRepository.savePodcastHistory(updatedList)
                        Log.d(TAG, "Handled PlaybackStarted: consolidated=${lastIndex != -1}")
                    }
                    
                    is AiEvent.PodcastPlaybackStopped -> {
                        val existingList = _podcastHistory.value
                        val index = existingList.indexOfFirst { it is PodcastActivityRecord.PlaybackRecord && it.episodeId == event.episodeId }
                        
                        if (index != -1) {
                            val oldRecord = existingList[index] as PodcastActivityRecord.PlaybackRecord
                            val totalListened = oldRecord.totalListenedMs + event.listenedMs
                            val seconds = totalListened / 1000
                            val minutes = seconds / 60
                            val timeStr = if (minutes > 0) "${minutes}分${seconds % 60}秒" else "${seconds}秒"
                            
                            val updatedRecord = oldRecord.copy(
                                totalListenedMs = totalListened,
                                currentProgressPercent = event.progressPercent,
                                timestamp = event.timestamp,
                                insight = "您今天收听了节目《${event.title}》共计 $timeStr，当前进度为 ${event.progressPercent.toInt()}%。建议您找个完整的时间一次听完它。"
                            )
                            
                            val newList = existingList.toMutableList().apply { set(index, updatedRecord) }
                            _podcastHistory.value = newList
                            aiNotepadRepository.savePodcastHistory(newList)
                            Log.d(TAG, "Handled PlaybackStopped: updated totalListened=$totalListened ms")
                        }
                    }
                    
                    is AiEvent.PodcastPlaybackCompleted -> {
                        val existingList = _podcastHistory.value
                        val index = existingList.indexOfFirst { it is PodcastActivityRecord.PlaybackRecord && it.episodeId == event.episodeId }
                        
                        val updatedList = if (index != -1) {
                            val oldRecord = existingList[index] as PodcastActivityRecord.PlaybackRecord
                            val totalListened = oldRecord.totalListenedMs + event.listenedMs
                            val seconds = totalListened / 1000
                            val minutes = seconds / 60
                            val timeStr = if (minutes > 0) "${minutes}分${seconds % 60}秒" else "${seconds}秒"
                            
                            val updatedRecord = oldRecord.copy(
                                totalListenedMs = totalListened,
                                currentProgressPercent = 100f,
                                isCompleted = true,
                                timestamp = event.timestamp,
                                insight = "恭喜！您完整收听了节目《${event.title}》，累计用时 $timeStr。这是一个非常棒的知识获取过程！"
                            )
                            existingList.toMutableList().apply { set(index, updatedRecord) }
                        } else {
                            val seconds = event.listenedMs / 1000
                            val minutes = seconds / 60
                            val timeStr = if (minutes > 0) "${minutes}分${seconds % 60}秒" else "${seconds}秒"
                            val newRecord = PodcastActivityRecord.PlaybackRecord(
                                id = "pod_play_${event.timestamp}_${event.episodeId}",
                                episodeId = event.episodeId,
                                title = event.title,
                                type = event.type,
                                channelName = event.channelName,
                                startTime = event.timestamp - event.listenedMs,
                                totalListenedMs = event.listenedMs,
                                currentProgressPercent = 100f,
                                isCompleted = true,
                                timestamp = event.timestamp,
                                insight = "恭喜！您完整收听了节目《${event.title}》，累计用时 $timeStr。"
                            )
                            listOf(newRecord) + existingList
                        }
                        
                        _podcastHistory.value = updatedList
                        aiNotepadRepository.savePodcastHistory(updatedList)
                        Log.d(TAG, "Handled PlaybackCompleted")
                    }
                    
                    is AiEvent.PodcastEpisodeFavorited -> {
                        val actionStr = if (event.favorite) "收藏" else "取消收藏"
                        val record = PodcastActivityRecord.FavoriteRecord(
                            id = "pod_fav_${event.timestamp}_${event.episodeId}",
                            episodeId = event.episodeId,
                            title = event.title,
                            favorite = event.favorite,
                            timestamp = event.timestamp,
                            insight = "您 $actionStr 了节目《${event.title}》。${if (event.favorite) "该节目已归档至您的个人收藏，方便随时回顾。" else "该节目已从您的个人收藏中移除。"}"
                        )
                        val newList = listOf(record) + _podcastHistory.value
                        _podcastHistory.value = newList
                        aiNotepadRepository.savePodcastHistory(newList)
                        Log.d(TAG, "Handled EpisodeFavorited: fav=${event.favorite}")
                    }
                    
                    is AiEvent.PodcastSubscriptionChanged -> {
                        val actionStr = if (event.isSubscribed) "订阅" else "退订"
                        val record = PodcastActivityRecord.SubscriptionRecord(
                            id = "pod_sub_${event.timestamp}_${event.channelId}",
                            channelId = event.channelId,
                            channelTitle = event.channelTitle,
                            isSubscribed = event.isSubscribed,
                            timestamp = event.timestamp,
                            insight = "您 $actionStr 了播客栏目《${event.channelTitle}》。${if (event.isSubscribed) "当该栏目有新内容时，AETHER 将在推荐面板第一时间为您更新。" else "您后续将不再接收该栏目的内容推荐。"}"
                        )
                        val newList = listOf(record) + _podcastHistory.value
                        _podcastHistory.value = newList
                        aiNotepadRepository.savePodcastHistory(newList)
                        Log.d(TAG, "Handled SubscriptionChanged: sub=${event.isSubscribed}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist event to notepad", e)
            }
        }
    }
}

