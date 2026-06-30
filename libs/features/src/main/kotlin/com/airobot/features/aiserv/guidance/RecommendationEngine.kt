package com.airobot.features.aiserv.guidance

import com.airobot.features.aiserv.guidance.data.CardUsageRecord
import com.airobot.features.aiserv.guidance.data.CardUsageStore
import com.airobot.features.aiserv.guidance.data.RecommendedCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor(
    private val cardRegistry: CardRegistry,
    private val usageStore: CardUsageStore
) {
    private val _supportedTags = MutableStateFlow<List<String>>(emptyList())
    private val _updateTrigger = MutableStateFlow(0)

    fun setSupportedTags(tags: List<String>) {
        _supportedTags.value = tags
    }

    fun getRecommendedCards(): Flow<List<RecommendedCard>> = combine(_supportedTags, _updateTrigger) { tags, _ ->
        val records = usageStore.getAllRecords(tags)
        val cards = cardRegistry.getCards(tags)

        // Calculate median usage
        val usageCounts = records.values.map { it.usageCount }.sorted()
        val medianUsage = if (usageCounts.isNotEmpty()) {
            usageCounts[usageCounts.size / 2]
        } else {
            0
        }

        val now = System.currentTimeMillis()

        cards.sortedWith { a, b ->
            val scoreA = computeScore(a, records[a.overlayTag] ?: CardUsageRecord(), medianUsage, now)
            val scoreB = computeScore(b, records[b.overlayTag] ?: CardUsageRecord(), medianUsage, now)

            val p = scoreB.compareTo(scoreA) // descending by score
            if (p != 0) p
            else a.overlayTag.compareTo(b.overlayTag) // stable fallback
        }
    }

    private fun computeScore(card: RecommendedCard, record: CardUsageRecord, medianUsage: Int, now: Long): Int {
        var score = card.basePriority

        if (record.hasNewContent) {
            score += 30
        }

        if (now - record.lastUsedTimestamp < 5 * 60 * 1000L) { // 5 minutes
            score -= 20
        }

        if (record.usageCount < medianUsage) {
            score += 10
        }

        return score
    }

    fun recordUsage(tag: String) {
        val current = usageStore.getUsageRecord(tag)
        usageStore.saveUsageRecord(tag, current.copy(
            usageCount = current.usageCount + 1,
            lastUsedTimestamp = System.currentTimeMillis(),
            hasNewContent = false // Reset new content when used
        ))
        _updateTrigger.value = _updateTrigger.value + 1
    }

    fun markNewContent(tag: String, hasNew: Boolean) {
        val current = usageStore.getUsageRecord(tag)
        usageStore.saveUsageRecord(tag, current.copy(
            hasNewContent = hasNew
        ))
        _updateTrigger.value = _updateTrigger.value + 1
    }
}
