package com.airobot.features.podcast.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.airobot.features.aiserv.popup.OverlayTags
import com.airobot.features.podcast.cards.PodcastHomeCard
import com.airobot.features.podcast.cards.PodcastLibraryCard
import com.airobot.features.podcast.cards.PodcastSubscribeCard

@Composable
fun PodcastHomeRoute(
    modifier: Modifier = Modifier,
    onShowOverlay: (String) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    PodcastHomeCard(
        modifier = modifier,
        onPlayClick = { onShowOverlay(OverlayTags.PODCAST) },
        onRemindClick = { action ->
            when (action) {
                "podcast" -> onShowOverlay(OverlayTags.PODCAST)
                "logbook" -> onShowOverlay(OverlayTags.LOGBOOK)
                "diy" -> onShowOverlay(OverlayTags.DIY_PODCAST)
            }
        },
        onNavigateToLibrary = onNavigateToLibrary
    )
}

@Composable
fun PodcastLibraryRoute(
    modifier: Modifier = Modifier,
    onShowOverlay: (String) -> Unit
) {
    PodcastLibraryCard(
        modifier = modifier,
        onPlayClick = { onShowOverlay(OverlayTags.PODCAST) }
    )
}

@Composable
fun PodcastSubscribeRoute(
    modifier: Modifier = Modifier
) {
    PodcastSubscribeCard(modifier = modifier)
}
