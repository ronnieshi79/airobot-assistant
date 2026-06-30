package com.airobot.assistant.assembly

import androidx.compose.runtime.Composable
import com.airobot.assistant.assembly.ServiceCard
import com.airobot.assistant.ui.comp.services.ServiceCardCarousel
import com.airobot.features.aiserv.popup.OverlayTags
import com.airobot.features.aiserv.routes.AiNotepadOverlayRoute
import com.airobot.features.podcast.routes.PodcastOverlayRoute
import com.airobot.features.podcast.routes.PodcastDiyOverlayRoute

@Composable
fun FeatureScreens(
    isCardMode: Boolean,
    serviceCards: List<ServiceCard>,
    currentCardIndex: Int,
    onPageChanged: (Int) -> Unit,
    statusTip: String,
    onCardClick: (ServiceCard) -> Unit,
    activeOverlay: String,
    onCloseOverlay: () -> Unit,
    onWakeupAirobot: () -> Unit
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
        if (activeOverlay.isNotEmpty()) {
            when (activeOverlay) {
                OverlayTags.PODCAST -> {
                    PodcastOverlayRoute(
                        onHideOverlay = onCloseOverlay,
                        onWakeupAirobot = onWakeupAirobot
                    )
                }
                OverlayTags.DIY_PODCAST -> {
                    PodcastDiyOverlayRoute(
                        onHideOverlay = onCloseOverlay
                    )
                }
                OverlayTags.LOGBOOK -> {
                    AiNotepadOverlayRoute(
                        onHideOverlay = onCloseOverlay
                    )
                }
            }
        }
    }
}
