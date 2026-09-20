package com.ajuworks.worklog.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ajuworks.worklog.appContainer
import com.ajuworks.worklog.core.CsvExport
import com.ajuworks.worklog.data.WorkLogSettings
import java.time.DayOfWeek
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = application.appContainer
    private val settingsRepository = container.settingsRepository
    private val workRepository = container.workRepository

    val settings: StateFlow<WorkLogSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkLogSettings(),
    )

    fun setWeekStart(day: DayOfWeek) = viewModelScope.launch {
        settingsRepository.setWeekStart(day)
    }

    fun setReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setReminderEnabled(enabled)
        if (!enabled) {
            container.clockOutReminder.cancel()
        } else if (workRepository.activeRecordNow() != null) {
            // Turning the reminder back on mid-shift has to schedule one now,
            // otherwise it silently does nothing until the next clock-in.
            container.clockOutReminder.schedule(settings.value.reminderAfterHours)
        }
    }

    fun setReminderHours(hours: Int) = viewModelScope.launch {
        settingsRepository.setReminderAfterHours(hours)
        if (settings.value.reminderEnabled && workRepository.activeRecordNow() != null) {
            container.clockOutReminder.schedule(hours)
        }
    }

    /** Every record, as CSV. Writing and sharing the file is the caller's job. */
    suspend fun exportAllCsv(japanese: Boolean): Pair<String, String> {
        val records = workRepository.allRecords.first()
        val now = workRepository.now()
        return CsvExport.fileName("worklog-all", now) to CsvExport.render(
            records = records,
            now = now,
            headers = if (japanese) CsvExport.Headers.JAPANESE else CsvExport.Headers.ENGLISH,
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return SettingsViewModel(app) as T
            }
        }
    }
}
