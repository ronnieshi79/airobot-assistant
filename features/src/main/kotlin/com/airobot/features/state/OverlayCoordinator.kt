package com.airobot.features.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OverlayCoordinator — Hilt singleton to coordinate full-screen overlays
 * across ViewModels and background services.
 */
@Singleton
class OverlayCoordinator @Inject constructor() {
    private val _activeOverlay = MutableStateFlow(OverlayType.NONE)
    val activeOverlay: StateFlow<OverlayType> = _activeOverlay.asStateFlow()

    fun showOverlay(type: OverlayType) {
        _activeOverlay.value = type
    }

    fun hideOverlay() {
        _activeOverlay.value = OverlayType.NONE
    }
}
