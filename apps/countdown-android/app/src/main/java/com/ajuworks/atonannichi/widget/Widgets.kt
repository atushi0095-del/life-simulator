package com.ajuworks.atonannichi.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.ajuworks.atonannichi.MainActivity
import com.ajuworks.atonannichi.R
import com.ajuworks.atonannichi.core.DayCount
import com.ajuworks.atonannichi.core.Design
import com.ajuworks.atonannichi.data.EventDatabase
import com.ajuworks.atonannichi.data.EventEntity
import com.ajuworks.atonannichi.data.WidgetConfigStore
import com.ajuworks.atonannichi.ui.WidgetConfigActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** ウィジェットの大きさ。レイアウト・文字の大きさ・既定の dp サイズを持つ。 */
enum class WidgetSize(val layout: Int, val shadowLayout: Int, val numberSp: Float, val defaultW: Int, val defaultH: Int) {
    SMALL(R.layout.widget_small, R.layout.widget_small_shadow, 34f, 150, 70),
    MEDIUM(R.layout.widget_medium, R.layout.widget_medium_shadow, 52f, 150, 150),
    LARGE(R.layout.widget_large, R.layout.widget_large_shadow, 64f, 320, 150),
}

abstract class CountdownWidget(private val size: WidgetSize) : AppWidgetProvider() {

    /**
     * 配置時と、3時間ごとの定期更新（updatePeriodMillis）で呼ばれる。
     * 定期更新は、アプリをしばらく開いていない端末でアラームが遅らされた場合の保険。
     * 描き直しに加えて通知の確認と次のアラームの登録も行う。
     */
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        async { Tick.run(context.applicationContext) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        async { WidgetUpdater.update(context, listOf(id)) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val store = WidgetConfigStore(context)
        ids.forEach(store::remove)
    }

    private fun async(block: suspend () -> Unit) {
        val pending = goAsync()
        WidgetUpdater.scope.launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    @Suppress("unused")
    fun sizeClass() = size
}

class SmallWidget : CountdownWidget(WidgetSize.SMALL)
class MediumWidget : CountdownWidget(WidgetSize.MEDIUM)
class LargeWidget : CountdownWidget(WidgetSize.LARGE)

object WidgetUpdater {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val providers = mapOf(
        SmallWidget::class.java to WidgetSize.SMALL,
        MediumWidget::class.java to WidgetSize.MEDIUM,
        LargeWidget::class.java to WidgetSize.LARGE,
    )

    fun allIds(context: Context): Map<Int, WidgetSize> {
        val m = AppWidgetManager.getInstance(context)
        return providers.flatMap { (cls, size) -> m.getAppWidgetIds(ComponentName(context, cls)).map { it to size } }.toMap()
    }

    suspend fun updateAll(context: Context) = update(context, allIds(context).keys.toList())

    suspend fun update(context: Context, ids: List<Int>) {
        if (ids.isEmpty()) return
        val sizes = allIds(context)
        val events = EventDatabase.get(context).dao().all().associateBy { it.id }
        val store = WidgetConfigStore(context)
        val manager = AppWidgetManager.getInstance(context)
        for (id in ids) {
            val size = sizes[id] ?: continue
            val config = store.get(id)
            val event = config?.let { events[it.eventId] }
            runCatching { manager.updateAppWidget(id, build(context, manager, id, size, event, config?.design ?: Design.SOFT)) }
        }
    }

    fun build(context: Context, manager: AppWidgetManager, id: Int, size: WidgetSize, event: EventEntity?, design: Design): RemoteViews {
        val eff = CardRenderer.effectiveDesign(event, design)
        val views = RemoteViews(context.packageName, if (eff == Design.PHOTO) size.shadowLayout else size.layout)

        val opts = manager.getAppWidgetOptions(id)
        val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: size.defaultW
        val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: size.defaultH
        val density = context.resources.displayMetrics.density
        val (pw, ph) = CardRenderer.widgetPixels(wDp, hDp, density)
        val corner = 22f * density * pw / (wDp * density)
        views.setImageViewBitmap(R.id.w_bg, CardRenderer.render(context, event, design, pw, ph, corner))

        if (event == null) {
            views.setTextViewText(R.id.w_headline, context.getString(R.string.widget_choose))
            views.setTextViewText(R.id.w_number, "+")
            views.setTextViewText(R.id.w_unit, "")
            views.setTextViewText(R.id.w_date, "")
            views.setViewVisibility(R.id.w_icon, View.GONE)
            colorize(views, eff, size.numberSp, false)
            val reconfigure = Intent(context, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            views.setOnClickPendingIntent(
                R.id.w_root,
                PendingIntent.getActivity(context, id, reconfigure, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
            return views
        }

        val l = Labels.of(context, event, DayCount.today())
        views.setTextViewText(R.id.w_headline, l.headline)
        views.setTextViewText(R.id.w_number, l.number)
        views.setTextViewText(R.id.w_unit, l.unit)
        views.setViewVisibility(R.id.w_unit, if (l.unit.isEmpty()) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.w_date, l.dateLine)
        views.setTextViewText(R.id.w_icon, event.icon)
        views.setViewVisibility(R.id.w_icon, View.VISIBLE)
        views.setContentDescription(R.id.w_root, listOf(l.headline, l.number, l.unit, l.dateLine).joinToString(" "))
        colorize(views, eff, size.numberSp, l.bigWord)

        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_EVENT_ID, event.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        views.setOnClickPendingIntent(
            R.id.w_root,
            PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
        return views
    }

    private fun colorize(v: RemoteViews, d: Design, numberSp: Float, bigWord: Boolean) {
        v.setTextColor(R.id.w_headline, d.subText)
        v.setTextColor(R.id.w_number, d.text)
        v.setTextColor(R.id.w_unit, d.accent)
        v.setTextColor(R.id.w_date, d.subText)
        // 「TODAY」など文字の場合は数字より小さめにする
        val sp = numberSp * d.numberScale * (if (bigWord) 0.62f else 1f)
        v.setTextViewTextSize(R.id.w_number, TypedValue.COMPLEX_UNIT_SP, sp)
    }
}
