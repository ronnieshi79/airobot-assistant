package com.airobot.assistant.assembly

import androidx.compose.runtime.Composable
import com.airobot.features.aiserv.guidance.components.CardRecommendationCarousel
import com.airobot.features.aiserv.guidance.data.RecommendCard
import com.airobot.features.FeatureCards
import com.airobot.features.aiserv.routes.AiNotepadOverlayRoute
import com.airobot.features.podcast.routes.PodcastOverlayRoute
import com.airobot.features.podcast.routes.PodcastDiyOverlayRoute

@Composable
fun FeatureScreens(
    isCardMode: Boolean,
    serviceCards: List<RecommendCard>,
    currentCardIndex: Int,
    onPageChanged: (Int) -> Unit,
    statusTip: String,
    onCardClick: (RecommendCard) -> Unit,
    activeOverlay: String,
    onCloseOverlay: () -> Unit,
    onWakeupAirobot: () -> Unit
) {
    if (!isCardMode) {
        // 展示右侧推荐卡片
        CardRecommendationCarousel(
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
                FeatureCards.PODCAST -> {
                    PodcastOverlayRoute(
                        onHideOverlay = onCloseOverlay,
                        onWakeupAirobot = onWakeupAirobot
                    )
                }
                FeatureCards.DIY_PODCAST -> {
                    PodcastDiyOverlayRoute(
                        onHideOverlay = onCloseOverlay
                    )
                }
                FeatureCards.LOGBOOK -> {
                    AiNotepadOverlayRoute(
                        onHideOverlay = onCloseOverlay
                    )
                }
            }
        }
    }
}
