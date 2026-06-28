package com.airobot.features.state

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PopupQueueViewModel @Inject constructor(
    private val popupQueueService: PopupQueueService
) : ViewModel() {
    val topQueueItem: StateFlow<PopupServiceItem?> = popupQueueService.topQueueItem
    val queue: StateFlow<List<PopupServiceItem>> = popupQueueService.queue
}
