package com.ajuworks.worklog.domain

import android.content.Context
import com.ajuworks.worklog.appContainer
import com.ajuworks.worklog.widget.WorkLogWidget
import kotlinx.coroutines.flow.first

/**
 * The four punches, in one place.
 *
 * The home screen and the home-screen widget both go through here so a punch
 * has identical side effects wherever it came from: the database is written,
 * the forgot-to-clock-out reminder is (re)scheduled or cancelled, and the
 * widget is redrawn.
 */
class WorkActions(private val context: Context) {

    private val container get() = context.appContainer

    suspend fun clockIn() {
        container.workRepository.clockIn()
        val settings = container.settingsRepository.settings.first()
        if (settings.reminderEnabled) {
            container.clockOutReminder.schedule(settings.reminderAfterHours)
        }
        WorkLogWidget.refresh(context)
    }

    suspend fun clockOut() {
        container.workRepository.clockOut()
        container.clockOutReminder.cancel()
        WorkLogWidget.refresh(context)
    }

    suspend fun startBreak() {
        container.workRepository.startBreak()
        WorkLogWidget.refresh(context)
    }

    suspend fun endBreak() {
        container.workRepository.endBreak()
        WorkLogWidget.refresh(context)
    }

    /** Punches in or out depending on the current state. Used by the widget. */
    suspend fun toggle() {
        if (container.workRepository.activeRecordNow() != null) clockOut() else clockIn()
    }
}
