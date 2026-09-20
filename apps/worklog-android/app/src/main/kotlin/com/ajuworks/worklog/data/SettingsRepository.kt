package com.ajuworks.worklog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "worklog_settings")

data class WorkLogSettings(
    /** Monday by default: the common convention for Japanese work weeks. */
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val reminderEnabled: Boolean = true,
    val reminderAfterHours: Int = DEFAULT_REMINDER_HOURS,
    val historyViewCount: Int = 0,
) {
    companion object {
        const val DEFAULT_REMINDER_HOURS = 12
        val REMINDER_HOUR_CHOICES = listOf(6, 8, 10, 12, 16, 24)
    }
}

/** Plain key-value settings. Nothing here leaves the device. */
class SettingsRepository(private val context: Context) {

    val settings: Flow<WorkLogSettings> = context.dataStore.data.map { prefs ->
        WorkLogSettings(
            weekStart = prefs[KEY_WEEK_START]?.let { runCatching { DayOfWeek.of(it) }.getOrNull() }
                ?: DayOfWeek.MONDAY,
            reminderEnabled = prefs[KEY_REMINDER_ENABLED] ?: true,
            reminderAfterHours = prefs[KEY_REMINDER_HOURS]
                ?: WorkLogSettings.DEFAULT_REMINDER_HOURS,
            historyViewCount = prefs[KEY_HISTORY_VIEWS] ?: 0,
        )
    }

    suspend fun setWeekStart(day: DayOfWeek) {
        context.dataStore.edit { it[KEY_WEEK_START] = day.value }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderAfterHours(hours: Int) {
        context.dataStore.edit { it[KEY_REMINDER_HOURS] = hours }
    }

    /** Returns the new count. Drives the low-frequency interstitial. */
    suspend fun recordHistoryView(): Int {
        var next = 0
        context.dataStore.edit { prefs ->
            next = (prefs[KEY_HISTORY_VIEWS] ?: 0) + 1
            prefs[KEY_HISTORY_VIEWS] = next
        }
        return next
    }

    private companion object {
        val KEY_WEEK_START = intPreferencesKey("week_start")
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMINDER_HOURS = intPreferencesKey("reminder_hours")
        val KEY_HISTORY_VIEWS = intPreferencesKey("history_views")
    }
}
