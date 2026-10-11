package com.example.state

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppThemeState(
    val isDarkMode: Boolean = false,
    val outputFormat: String = "High-Quality PNG", // "High-Quality PNG" or "Standard JPG"
    val safeZoneGuidesOnSave: Boolean = true,
    val creditBadge: Boolean = false
)

class AppThemeViewModel : ViewModel() {
    private val _state = MutableStateFlow(AppThemeState())
    val state: StateFlow<AppThemeState> = _state.asStateFlow()

    fun toggleDarkMode() {
        _state.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun setDarkMode(dark: Boolean) {
        _state.update { it.copy(isDarkMode = dark) }
    }

    fun updateOutputFormat(format: String) {
        _state.update { it.copy(outputFormat = format) }
    }

    fun updateSafeZoneGuidesOnSave(enabled: Boolean) {
        _state.update { it.copy(safeZoneGuidesOnSave = enabled) }
    }

    fun updateCreditBadge(enabled: Boolean) {
        _state.update { it.copy(creditBadge = enabled) }
    }
}
