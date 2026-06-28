package com.airobot.features.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AlertType — severity types for the custom top alert banner
 */
enum class AlertType {
    INFO, WARNING, ERROR
}

/**
 * TopAlertState — holds alert visibility, message, and severity
 */
data class TopAlertState(
    val visible: Boolean = false,
    val message: String = "",
    val type: AlertType = AlertType.WARNING
)

/**
 * OverlayViewModel — lightweight coordinator for global overlay dispatch (ALARM, TIMER, FOCUS, etc.).
 */
@HiltViewModel
class OverlayViewModel @Inject constructor(
    private val overlayCoordinator: OverlayCoordinator
) : ViewModel() {
    val activeOverlay: StateFlow<OverlayType> = overlayCoordinator.activeOverlay

    private val _topAlert = MutableStateFlow(TopAlertState())
    val topAlert: StateFlow<TopAlertState> = _topAlert.asStateFlow()

    private var autoDismissJob: Job? = null

    fun showOverlay(type: OverlayType) {
        overlayCoordinator.showOverlay(type)
    }

    fun hideOverlay() {
        overlayCoordinator.hideOverlay()
    }

    /**
     * Shows a top banner alert that automatically dismisses after 3 seconds.
     */
    fun showTopAlert(message: String, type: AlertType = AlertType.WARNING) {
        autoDismissJob?.cancel()
        _topAlert.value = TopAlertState(visible = true, message = message, type = type)
        autoDismissJob = viewModelScope.launch {
            delay(3000)
            _topAlert.value = _topAlert.value.copy(visible = false)
        }
    }

    /**
     * Immediately hides the active top alert.
     */
    fun hideTopAlert() {
        autoDismissJob?.cancel()
        _topAlert.value = _topAlert.value.copy(visible = false)
    }
}
