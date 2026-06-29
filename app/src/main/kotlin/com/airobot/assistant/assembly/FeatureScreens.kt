package com.airobot.assistant.assembly

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobot.assistant.ui.comp.services.ServiceCard
import com.airobot.assistant.ui.comp.services.ServiceCardCarousel
import com.airobot.assistant.ui.comp.services.ServiceCardType
import com.airobot.features.aiserv.cards.AiNotepadOverlay
import com.airobot.features.podcast.cards.PodcastDiyOverlay
import com.airobot.features.podcast.cards.PodcastPlayerOverlay
import com.airobot.features.podcast.viewmodel.PodcastViewModel

@Composable
fun FeatureScreens(
    isCardMode: Boolean,
    serviceCards: List<ServiceCard>,
    currentCardIndex: Int,
    onPageChanged: (Int) -> Unit,
    statusTip: String,
    onCardClick: (ServiceCard) -> Unit,
    activeCard: ServiceCard?,
    onCloseOverlay: () -> Unit,
    onWakeupAirobot: () -> Unit,
    podcastViewModel: PodcastViewModel = hiltViewModel()
) {
    if (!isCardMode) {
        // 展示右侧推荐卡片
        ServiceCardCarousel(
            cards = serviceCards,
            currentIndex = currentCardIndex,
            onPageChanged = onPageChanged,
            statusTip = statusTip,
            onCardClick = onCardClick
        )
    } else {
        // 展示功能详情Overlay
        activeCard?.let { card ->
            when (card.type) {
                ServiceCardType.PODCAST -> {
                    PodcastPlayerOverlay(
                        onClose = onCloseOverlay,
                        onWakeupAirobot = onWakeupAirobot,
                        podcastViewModel = podcastViewModel
                    )
                }
                ServiceCardType.PODCAST_DIY -> {
                    PodcastDiyOverlay(
                        podcastViewModel = podcastViewModel,
                        onClose = onCloseOverlay
                    )
                }
                ServiceCardType.NOTEPAD -> {
                    AiNotepadOverlay(
                        alarmHistory = emptyList(),
                        timerHistory = emptyList(),
                        focusHistory = emptyList(),
                        podcastHistory = emptyList(),
                        onClose = onCloseOverlay
                    )
                }
            }
        }
    }
}
