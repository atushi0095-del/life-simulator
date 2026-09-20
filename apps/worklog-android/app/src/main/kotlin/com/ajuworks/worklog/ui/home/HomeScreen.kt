package com.ajuworks.worklog.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajuworks.worklog.R
import com.ajuworks.worklog.ui.LocalDurationFormatter
import com.ajuworks.worklog.ui.timeText
import com.ajuworks.worklog.ui.TickEffect

/**
 * The whole point of the app. Three things must be answerable at a glance:
 * am I working, how long today, how long this month. Everything else is a tap
 * away, not on this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TickEffect(onStart = viewModel::startTicking, onStop = viewModel::stopTicking)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, stringResource(R.string.nav_history))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.nav_settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            StatusBlock(state)

            Spacer(Modifier.height(24.dp))

            PunchButtons(
                state = state,
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut,
                onStartBreak = viewModel::startBreak,
                onEndBreak = viewModel::endBreak,
            )

            Spacer(Modifier.height(28.dp))

            TotalsCard(state)

            Spacer(Modifier.height(24.dp))

            if (!state.isWorking && state.today.sessionCount == 0) {
                // The entire onboarding: one sentence, shown only when there is
                // nothing else to look at. No "Next" buttons, ever.
                Text(
                    text = stringResource(R.string.home_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatusBlock(state: HomeUiState) {
    val format = LocalDurationFormatter.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when {
                state.isOnBreak -> stringResource(R.string.status_on_break)
                state.isWorking -> stringResource(R.string.status_working)
                else -> stringResource(R.string.status_off)
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (state.isWorking) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(8.dp))

        if (state.isWorking) {
            val active = state.active!!
            Text(
                text = format(state.elapsedMillis),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state.isOnBreak) {
                    stringResource(
                        R.string.home_on_break_since,
                        timeText(active.runningBreak!!.start),
                    )
                } else {
                    stringResource(R.string.home_started_at, timeText(active.start))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = format(state.today.netMillis),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PunchButtons(
    state: HomeUiState,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onStartBreak: () -> Unit,
    onEndBreak: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Deliberately oversized: this is the one control that has to be
        // reachable and hittable with a thumb, on a train, without looking.
        Button(
            onClick = when {
                state.isOnBreak -> onEndBreak
                state.isWorking -> onClockOut
                else -> onClockIn
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = if (state.isWorking && !state.isOnBreak) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Text(
                text = when {
                    state.isOnBreak -> stringResource(R.string.action_end_break)
                    state.isWorking -> stringResource(R.string.action_clock_out)
                    else -> stringResource(R.string.action_clock_in)
                },
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        if (state.isWorking && !state.isOnBreak) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onStartBreak,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.action_start_break))
            }
        }
    }
}

@Composable
private fun TotalsCard(state: HomeUiState) {
    val format = LocalDurationFormatter.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Total(stringResource(R.string.label_today), format(state.today.netMillis))
            Total(stringResource(R.string.label_this_week), format(state.week.netMillis))
            Total(stringResource(R.string.label_this_month), format(state.month.netMillis))
        }
    }
}

@Composable
private fun Total(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}
