package com.example.ui.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AppSettings
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository.getInstance(application)

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyGoal(goal)
        }
    }

    fun setDefaultTimerMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultTimerMinutes(minutes)
        }
    }

    fun setDefaultNavMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultNavMode(mode)
        }
    }

    fun setRequireAllChecklist(require: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRequireAllChecklist(require)
        }
    }
}
