package com.ajuworks.atonannichi.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajuworks.atonannichi.MainActivity
import com.ajuworks.atonannichi.R
import com.ajuworks.atonannichi.core.DayCount
import com.ajuworks.atonannichi.data.EventDatabase
import com.ajuworks.atonannichi.data.WidgetConfigStore
import com.ajuworks.atonannichi.widget.WidgetUpdater
import kotlinx.coroutines.launch

/** ウィジェットを置いたとき（または置いたウィジェットをタップしたとき）に、表示するイベントとデザインを選ぶ。 */
class WidgetConfigActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // 選ばずに戻ったらウィジェットを置かない
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val store = WidgetConfigStore(this)
        val current = store.get(widgetId)

        setContent {
            DaysTheme {
                val events by EventDatabase.get(this).dao().observeAll().collectAsStateWithLifecycle(emptyList())
                val today = remember { DayCount.today() }
                var selected by remember { mutableLongStateOf(current?.eventId ?: -1L) }
                var design by remember { mutableStateOf(current?.design) }
                val scope = rememberCoroutineScope()
                val sorted = events.sortedBy { DayCount.sortKey(it.date, today) }
                if (selected < 0 && sorted.isNotEmpty()) selected = sorted.first().id
                val chosen = sorted.firstOrNull { it.id == selected }

                Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.widget_config_title)) }) }) { padding ->
                    if (sorted.isEmpty()) {
                        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.widget_no_events), style = MaterialTheme.typography.titleMedium)
                            Button(onClick = {
                                startActivity(Intent(this@WidgetConfigActivity, MainActivity::class.java).putExtra(MainActivity.EXTRA_ADD, true))
                                finish()
                            }) { Text(stringResource(R.string.add_first)) }
                        }
                        return@Scaffold
                    }
                    LazyColumn(
                        Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item { Text(stringResource(R.string.widget_pick_event), style = MaterialTheme.typography.titleMedium) }
                        items(sorted, key = { it.id }) { e ->
                            val sel = e.id == selected
                            EventCard(
                                e, today,
                                modifier = if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)) else Modifier,
                                design = if (sel) (design ?: e.designEnum) else e.designEnum,
                                onClick = { selected = e.id },
                            )
                        }
                        item { Text(stringResource(R.string.field_design), style = MaterialTheme.typography.titleMedium) }
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.ajuworks.atonannichi.core.Design.entries.forEach { d ->
                                    DesignSwatch(d, (design ?: chosen?.designEnum) == d) { design = d }
                                }
                            }
                        }
                        item {
                            Button(
                                enabled = chosen != null,
                                onClick = {
                                    val e = chosen ?: return@Button
                                    store.put(widgetId, WidgetConfigStore.Config(e.id, design ?: e.designEnum))
                                    scope.launch {
                                        WidgetUpdater.update(this@WidgetConfigActivity, listOf(widgetId))
                                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                                        finish()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            ) { Text(stringResource(R.string.widget_place)) }
                        }
                    }
                }
            }
        }
    }
}
