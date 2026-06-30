package com.airobot.features.aiserv.guidance

import com.airobot.features.aiserv.guidance.models.RecommendedCard
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRegistry @Inject constructor() {
    private val registry = mutableMapOf<String, RecommendedCard>()

    fun register(card: RecommendedCard) {
        registry[card.overlayTag] = card
    }

    fun getCard(tag: String): RecommendedCard? {
        return registry[tag]
    }

    fun getCards(tags: List<String>): List<RecommendedCard> {
        return tags.mapNotNull { registry[it] }
    }
}
