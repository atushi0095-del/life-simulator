package com.ajuworks.worklog.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajuworks.worklog.R
import com.ajuworks.worklog.ads.AdBanner
import com.ajuworks.worklog.core.WorkRecord
import com.ajuworks.worklog.ui.LocalDurationFormatter
import com.ajuworks.worklog.ui.shortDateText
import com.ajuworks.worklog.ui.timeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onAddRecord: () -> Unit,
    onShareCsv: (fileName: String, content: String) -> Unit,
    onHistoryViewed: suspend () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val format = LocalDurationFormatter.current
    val japanese = com.ajuworks.worklog.ui.isJapanese()

    LaunchedEffect(Unit) { onHistoryViewed() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val (name, content) = viewModel.csvForCurrentMonth(japanese)
                            onShareCsv(name, content)
                        },
                    ) {
                        Icon(Icons.Default.Share, stringResource(R.string.action_export_csv))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRecord,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.action_add_record)) },
            )
        },
        bottomBar = {
            // The only ad on a screen the user is browsing, never one they are
            // punching on. It renders nothing at all if the SDK cannot load.
            AdBanner(modifier = Modifier.fillMaxWidth())
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            MonthSwitcher(
                label = "${state.month.year}/${state.month.monthValue}",
                canGoOlder = state.canGoOlder,
                canGoNewer = state.canGoNewer,
                onPrevious = viewModel::showPreviousMonth,
                onNext = viewModel::showNextMonth,
            )

            MonthSummary(state)

            HorizontalDivider()

            if (state.records.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.records, key = { it.id }) { record ->
                        HistoryRow(
                            record = record,
                            elapsed = format(record.netMillis(state.now)),
                            onClick = { onOpenRecord(record.id) },
                        )
                        HorizontalDivider()
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MonthSwitcher(
    label: String,
    canGoOlder: Boolean,
    canGoNewer: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoOlder) {
            Icon(Icons.Default.ChevronLeft, stringResource(R.string.action_previous_month))
        }
        Text(text = label, style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onNext, enabled = canGoNewer) {
            Icon(Icons.Default.ChevronRight, stringResource(R.string.action_next_month))
        }
    }
}

@Composable
private fun MonthSummary(state: HistoryUiState) {
    val format = LocalDurationFormatter.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryCell(
                stringResource(R.string.label_days_worked),
                stringResource(R.string.value_days, state.summary.dayCount),
            )
            SummaryCell(
                stringResource(R.string.label_total_worked),
                format(state.summary.netMillis),
            )
            SummaryCell(
                stringResource(R.string.label_total_break),
                format(state.summary.breakMillis),
            )
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun HistoryRow(record: WorkRecord, elapsed: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = shortDateText(record.workDate),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (record.isRunning) {
                    stringResource(R.string.history_in_progress, timeText(record.start))
                } else {
                    "${timeText(record.start)} → ${timeText(record.end!!)}"
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(text = elapsed, style = MaterialTheme.typography.titleMedium)
    }
}
