package com.ajuworks.worklog

import android.app.Application
import android.content.Context
import com.ajuworks.worklog.data.SettingsRepository
import com.ajuworks.worklog.data.WorkRepository
import com.ajuworks.worklog.data.local.WorkDatabase
import com.ajuworks.worklog.work.ClockOutReminder

/**
 * Manual dependency container. The graph is four objects deep, so a DI
 * framework would cost startup time and build time for nothing - and startup
 * time is a feature here: the user opens this app to press one button.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val workRepository: WorkRepository by lazy {
        WorkRepository(WorkDatabase.get(appContext).workDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val clockOutReminder: ClockOutReminder by lazy {
        ClockOutReminder(appContext)
    }
}

class WorkLogApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ClockOutReminder.createNotificationChannel(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as WorkLogApp).container
