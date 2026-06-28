package com.airobot.features.clock.cards.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airobot.framework.cards.PopupQueueWidget
import com.airobot.framework.cards.PopupQueueWidgetItem

/**
 * Sub-dial widget wrapper that delegates to the reusable [PopupQueueWidget].
 */
@Composable
fun SubDialWidget(
    items: List<PopupQueueWidgetItem>,
    scaleRatio: Float = 1f,
    modifier: Modifier = Modifier,
    onItemClick: (id: String) -> Unit = {}
) {
    PopupQueueWidget(
        items = items,
        modifier = modifier,
        size = 120.dp * scaleRatio,
        carouselIntervalMs = 5000L,
        onItemClick = onItemClick
    )
}
