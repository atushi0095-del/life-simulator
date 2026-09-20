package com.ajuworks.worklog.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajuworks.worklog.R
import com.ajuworks.worklog.core.Issue
import com.ajuworks.worklog.core.Severity
import com.ajuworks.worklog.ui.isJapanese
import com.ajuworks.worklog.ui.longDateText
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

/**
 * Edit an existing shift, or add one that was never punched. Both cases share
 * this screen and [EditRecordViewModel]'s validation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordScreen(
    recordId: Long,
    onDone: () -> Unit,
    viewModel: EditRecordViewModel = viewModel(factory = EditRecordViewModel.factory(recordId)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingTimePick by remember { mutableStateOf<TimeTarget?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.title_add_record else R.string.title_edit_record
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.action_delete))
                        }
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
        ) {
            Spacer(Modifier.height(8.dp))

            FieldRow(
                label = stringResource(R.string.label_date),
                value = longDateText(state.draft.date, isJapanese()),
                onClick = { showDatePicker = true },
            )
            FieldRow(
                label = stringResource(R.string.label_clock_in),
                value = state.draft.startTime.format(TIME_LABEL),
                onClick = { pendingTimePick = TimeTarget.Start },
            )
            FieldRow(
                label = stringResource(R.string.label_clock_out),
                value = state.draft.endTime?.format(TIME_LABEL)
                    ?: stringResource(R.string.label_still_working),
                onClick = { pendingTimePick = TimeTarget.End },
            )

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.label_breaks),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))

            state.draft.breaks.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { pendingTimePick = TimeTarget.BreakStart(index) }) {
                        Text(entry.startTime.format(TIME_LABEL))
                    }
                    Text("–")
                    TextButton(onClick = { pendingTimePick = TimeTarget.BreakEnd(index) }) {
                        Text(
                            entry.endTime?.format(TIME_LABEL)
                                ?: stringResource(R.string.label_on_break_now)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.removeBreak(index) }) {
                        Icon(Icons.Default.Close, stringResource(R.string.action_remove_break))
                    }
                }
            }

            OutlinedButton(onClick = viewModel::addBreak) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_add_break))
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.draft.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.label_note)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            IssueList(state.issues)

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::save,
                enabled = !state.blocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.draft.date
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            // The picker works in UTC; convert back without
                            // letting the device zone shift the chosen day.
                            viewModel.setDate(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    pendingTimePick?.let { target ->
        val initial = when (target) {
            TimeTarget.Start -> state.draft.startTime
            TimeTarget.End -> state.draft.endTime ?: LocalTime.of(18, 0)
            is TimeTarget.BreakStart -> state.draft.breaks[target.index].startTime
            is TimeTarget.BreakEnd -> state.draft.breaks[target.index].endTime ?: LocalTime.of(13, 0)
        }
        TimePickerDialog(
            initial = initial,
            onDismiss = { pendingTimePick = null },
            onConfirm = { picked ->
                when (target) {
                    TimeTarget.Start -> viewModel.setStartTime(picked)
                    TimeTarget.End -> viewModel.setEndTime(picked)
                    is TimeTarget.BreakStart -> viewModel.setBreakStart(target.index, picked)
                    is TimeTarget.BreakEnd -> viewModel.setBreakEnd(target.index, picked)
                }
                pendingTimePick = null
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_delete_message,
                        longDateText(state.draft.date, isJapanese()),
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private sealed interface TimeTarget {
    data object Start : TimeTarget
    data object End : TimeTarget
    data class BreakStart(val index: Int) : TimeTarget
    data class BreakEnd(val index: Int) : TimeTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun FieldRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onClick) {
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Inconsistencies, worded for a person. Errors block saving; warnings and
 * inferences only explain what the app did.
 */
@Composable
private fun IssueList(issues: List<Issue>) {
    if (issues.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        issues.forEach { issue ->
            val text = when (issue) {
                Issue.OvernightAssumed -> stringResource(R.string.issue_overnight)
                Issue.OverlappingBreaks -> stringResource(R.string.issue_overlapping_breaks)
                is Issue.BreakOutsideSession ->
                    stringResource(R.string.issue_break_outside, issue.index + 1)
                Issue.BreaksExceedWork -> stringResource(R.string.issue_breaks_exceed)
                is Issue.ImplausiblyLong -> stringResource(R.string.issue_too_long, issue.hours)
                Issue.EndsInFuture -> stringResource(R.string.issue_future)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (issue.severity) {
                        Severity.ERROR -> MaterialTheme.colorScheme.errorContainer
                        Severity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                        Severity.INFO -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
