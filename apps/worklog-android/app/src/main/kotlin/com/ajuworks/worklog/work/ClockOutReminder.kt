package com.ajuworks.worklog.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ajuworks.worklog.MainActivity
import com.ajuworks.worklog.R
import com.ajuworks.worklog.appContainer
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * "Did you forget to clock out?" - a single local notification, scheduled with
 * WorkManager when the user clocks in and cancelled when they clock out.
 *
 * Nothing is pushed from a server, and nothing polls: WorkManager holds one
 * delayed job and the OS decides when to run it.
 */
class ClockOutReminder(private val context: Context) {

    fun schedule(afterHours: Int) {
        val request = OneTimeWorkRequestBuilder<ClockOutReminderWorker>()
            .setInitialDelay(afterHours.toLong(), TimeUnit.HOURS)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "clock_out_reminder"
        const val TAG = "clock_out_reminder"
        const val CHANNEL_ID = "clock_out_reminder"
        const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}

/**
 * Re-checks the database before notifying. The job may fire long after it was
 * scheduled, by which time the user may have clocked out from the widget - in
 * that case it must stay quiet.
 */
class ClockOutReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val settings = container.settingsRepository.settings.first()
        if (!settings.reminderEnabled) return Result.success()

        val active = container.workRepository.activeRecordNow() ?: return Result.success()

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // The user declined notifications. Nothing to do, and not a failure.
            return Result.success()
        }

        val hours = active.grossMillis(container.workRepository.now()) / 3_600_000L
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(applicationContext, ClockOutReminder.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.reminder_title))
            .setContentText(
                applicationContext.resources.getQuantityString(
                    R.plurals.reminder_body,
                    hours.toInt(),
                    hours,
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(ClockOutReminder.NOTIFICATION_ID, notification)
        return Result.success()
    }
}
