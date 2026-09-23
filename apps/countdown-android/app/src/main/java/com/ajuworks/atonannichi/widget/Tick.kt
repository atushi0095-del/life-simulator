package com.ajuworks.atonannichi.widget

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ajuworks.atonannichi.MainActivity
import com.ajuworks.atonannichi.R
import com.ajuworks.atonannichi.core.DayCount
import com.ajuworks.atonannichi.core.NotifyRules
import com.ajuworks.atonannichi.core.TickSchedule
import com.ajuworks.atonannichi.data.EventDatabase
import com.ajuworks.atonannichi.data.EventEntity
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * 1日2回（日付が変わった直後・朝9時）だけ起きて、ウィジェットを描き直し、通知を確認する。
 * スリープを解除しないアラーム（RTC）なので、端末が眠っている間は次に画面が点いたときに届く。
 * 端末の再起動・時刻やタイムゾーンの変更・アプリ更新のときも同じ処理を行い、次のアラームを入れ直す。
 */
class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        WidgetUpdater.scope.launch {
            try {
                Tick.run(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}

object Tick {
    const val ACTION = "com.ajuworks.atonannichi.TICK"
    private const val CHANNEL = "countdown"

    suspend fun run(context: Context) {
        WidgetUpdater.updateAll(context)
        if (TickSchedule.isNotifyTime(ZonedDateTime.now())) notifyDue(context)
        scheduleNext(context)
    }

    fun scheduleNext(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val next = TickSchedule.next(ZonedDateTime.now()).toInstant().toEpochMilli()
        val pi = PendingIntent.getBroadcast(
            context, 0, Intent(context, TickReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // 正確さは不要（数分の遅れは許容）。exact alarm の権限は使わない
        am.setWindow(AlarmManager.RTC, next, 10 * 60_000L, pi)
    }

    /** 今日通知すべきイベントを通知し、通知済みの印を付ける。 */
    suspend fun notifyDue(context: Context) {
        val dao = EventDatabase.get(context).dao()
        val today = DayCount.today()
        for (e in dao.all()) {
            val offset = NotifyRules.due(e.date, today, e.notifyMask, e.sentMask) ?: continue
            if (post(context, e, offset)) dao.setSent(e.id, e.sentMask or NotifyRules.bitOf(offset))
        }
    }

    fun ensureChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_countdown), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun canPost(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun post(context: Context, e: EventEntity, offset: Int): Boolean {
        if (!canPost(context)) return false
        ensureChannel(context)
        val text = when (offset) {
            0 -> context.getString(R.string.notify_today, e.title)
            1 -> context.getString(R.string.notify_tomorrow, e.title)
            else -> context.resources.getQuantityString(R.plurals.notify_days, offset, e.title, offset)
        }
        val open = PendingIntent.getActivity(
            context, e.id.toInt(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_EVENT_ID, e.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_calendar)
            .setContentTitle("${e.icon} ${e.title}")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(e.id.toInt(), n)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
