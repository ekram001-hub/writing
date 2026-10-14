package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "microwrite_preferences")

data class AppSettings(
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val dailyGoalSessions: Int = 2,
    val requireAllChecklistItems: Boolean = false,
    val defaultTimerMinutes: Int = 2,
    val defaultNavMode: String = "SEQUENTIAL" // "SEQUENTIAL", "FREE"
)

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DAILY_GOAL = intPreferencesKey("daily_goal_sessions")
        val KEY_REQUIRE_ALL_CHECKLIST = booleanPreferencesKey("require_all_checklist")
        val KEY_DEFAULT_TIMER = intPreferencesKey("default_timer_minutes")
        val KEY_DEFAULT_NAV_MODE = stringPreferencesKey("default_nav_mode")

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val settingsFlow: Flow<AppSettings> = appContext.dataStore.data
        .catch { exception ->
            if (exception is java.io.IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[KEY_THEME_MODE] ?: "SYSTEM",
                dailyGoalSessions = preferences[KEY_DAILY_GOAL] ?: 2,
                requireAllChecklistItems = preferences[KEY_REQUIRE_ALL_CHECKLIST] ?: false,
                defaultTimerMinutes = preferences[KEY_DEFAULT_TIMER] ?: 2,
                defaultNavMode = preferences[KEY_DEFAULT_NAV_MODE] ?: "SEQUENTIAL"
            )
        }

    suspend fun setThemeMode(mode: String) {
        appContext.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setDailyGoal(goal: Int) {
        appContext.dataStore.edit { it[KEY_DAILY_GOAL] = goal.coerceIn(1, 20) }
    }

    suspend fun setRequireAllChecklist(require: Boolean) {
        appContext.dataStore.edit { it[KEY_REQUIRE_ALL_CHECKLIST] = require }
    }

    suspend fun setDefaultTimerMinutes(minutes: Int) {
        appContext.dataStore.edit { it[KEY_DEFAULT_TIMER] = minutes.coerceIn(1, 30) }
    }

    suspend fun setDefaultNavMode(mode: String) {
        appContext.dataStore.edit { it[KEY_DEFAULT_NAV_MODE] = mode }
    }
}
