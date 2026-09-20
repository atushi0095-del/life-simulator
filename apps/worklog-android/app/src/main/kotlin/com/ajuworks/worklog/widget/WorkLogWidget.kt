package com.ajuworks.worklog.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ajuworks.worklog.R
import com.ajuworks.worklog.appContainer
import com.ajuworks.worklog.core.WorkTimeFormat
import com.ajuworks.worklog.domain.WorkActions
import java.util.Locale

/**
 * Home-screen widget: current state, elapsed time, and one button that punches
 * directly.
 *
 * Punching happens in an [ActionCallback], not by launching the app, so it is a
 * genuine single tap. The callback goes through the same [WorkActions] the app
 * uses, so the reminder and the database stay consistent either way. The
 * displayed elapsed time is recomputed on each update from the stored clock-in
 * - the widget holds no timer and no state of its own.
 */
class WorkLogWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = context.appContainer.workRepository
        // Read once, outside the composition: Glance composables cannot
        // suspend, and the widget only needs a snapshot per update anyway.
        val record = repository.activeRecordNow()
        val now = repository.now()

        provideContent {
            GlanceTheme {
                Scaffold(modifier = GlanceModifier.fillMaxSize()) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when {
                                record == null -> context.getString(R.string.status_off)
                                record.isOnBreak -> context.getString(R.string.status_on_break)
                                else -> context.getString(R.string.status_working)
                            },
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                            ),
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = if (record != null) {
                                format(context, record.netMillis(now))
                            } else {
                                context.getString(R.string.app_name)
                            },
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            ),
                        )
                        Spacer(GlanceModifier.height(6.dp))
                        Text(
                            text = context.getString(
                                if (record == null) R.string.action_clock_in else R.string.action_clock_out
                            ),
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(GlanceTheme.colors.primaryContainer)
                                .clickable(actionRunCallback<PunchAction>()),
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun format(context: Context, millis: Long): String =
        if (Locale.getDefault().language == Locale.JAPANESE.language) {
            WorkTimeFormat.toJapanese(millis)
        } else {
            WorkTimeFormat.toEnglish(millis)
        }

    companion object {
        /** Redraws every placed widget. Called after any punch or edit. */
        suspend fun refresh(context: Context) {
            runCatching { WorkLogWidget().updateAll(context) }
        }
    }
}

/** Punches in or out and redraws, without opening the app. */
class PunchAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WorkActions(context).toggle()
        WorkLogWidget().updateAll(context)
    }
}

class WorkLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorkLogWidget()
}
