package com.balsam.sudoku.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balsam.sudoku.SudokuApp
import com.balsam.sudoku.data.AppSettings
import com.balsam.sudoku.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val app: SudokuApp) : ViewModel() {

    val settings: StateFlow<AppSettings> = app.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = launch { app.settingsRepository.setThemeMode(mode) }
    fun setHighlightRegion(value: Boolean) = launch { app.settingsRepository.setHighlightRegion(value) }
    fun setHighlightSame(value: Boolean) = launch { app.settingsRepository.setHighlightSame(value) }
    fun setSoundEnabled(value: Boolean) = launch { app.settingsRepository.setSoundEnabled(value) }
    fun setVibrationEnabled(value: Boolean) = launch { app.settingsRepository.setVibrationEnabled(value) }
    fun setShowTimer(value: Boolean) = launch { app.settingsRepository.setShowTimer(value) }
    fun setLimitMistakes(value: Boolean) = launch { app.settingsRepository.setLimitMistakes(value) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
