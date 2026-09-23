package com.ajuworks.atonannichi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajuworks.atonannichi.R
import com.ajuworks.atonannichi.core.AfterMode
import com.ajuworks.atonannichi.core.Design
import com.ajuworks.atonannichi.core.ICONS
import com.ajuworks.atonannichi.core.NotifyRules
import com.ajuworks.atonannichi.data.EventEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    draft: EventEntity,
    today: LocalDate,
    onChange: (EventEntity) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: suspend () -> Boolean,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (draft.id == 0L) R.string.add_event else R.string.edit_event)) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, stringResource(R.string.cancel)) } },
                actions = {
                    if (draft.id != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.delete)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // プレビュー（ウィジェットと同じ見た目）
            EventCard(draft.copy(title = draft.title.ifBlank { stringResource(R.string.sample_title) }), today)

            OutlinedTextField(
                value = draft.title,
                onValueChange = { onChange(draft.copy(title = it.take(40))); titleError = false },
                label = { Text(stringResource(R.string.field_title)) },
                placeholder = { Text(stringResource(R.string.title_placeholder)) },
                singleLine = true,
                isError = titleError,
                supportingText = { if (titleError) Text(stringResource(R.string.err_title)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.field_date), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { pickDate = true }) {
                    Text(draft.date.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern))))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.field_time), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                if (draft.time != null) {
                    OutlinedButton(onClick = { pickTime = true }) { Text("%02d:%02d".format(draft.time!!.hour, draft.time!!.minute)) }
                }
                Switch(
                    checked = draft.timeMinutes != null,
                    onCheckedChange = { onChange(draft.copy(timeMinutes = if (it) 10 * 60 else null)) },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Section(stringResource(R.string.field_icon))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ICONS.forEach { ic ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (ic == draft.icon) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onChange(draft.copy(icon = ic)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ic, fontSize = if (ic == draft.icon) 30.sp else 22.sp)
                    }
                }
            }

            Section(stringResource(R.string.field_photo))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPickPhoto) {
                    Icon(Icons.Outlined.Image, null)
                    Text("  " + stringResource(if (draft.photoFile == null) R.string.photo_choose else R.string.photo_change))
                }
                if (draft.photoFile != null) TextButton(onClick = onRemovePhoto) { Text(stringResource(R.string.photo_remove)) }
            }

            Section(stringResource(R.string.field_design))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Design.entries.forEach { d -> DesignSwatch(d, draft.designEnum == d) { onChange(draft.copy(design = d.name)) } }
            }
            if (draft.designEnum == Design.PHOTO && draft.photoFile == null) {
                Text(stringResource(R.string.photo_design_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Section(stringResource(R.string.field_after))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AfterMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = draft.after == m,
                        onClick = { onChange(draft.copy(afterMode = m.name)) },
                        shape = SegmentedButtonDefaults.itemShape(i, AfterMode.entries.size),
                    ) { Text(stringResource(if (m == AfterMode.COUNT_UP) R.string.after_count_up else R.string.after_end)) }
                }
            }

            Section(stringResource(R.string.field_notify))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NotifyRules.OFFSETS.forEach { off ->
                    FilterChip(
                        selected = NotifyRules.isOn(draft.notifyMask, off),
                        onClick = { onChange(draft.copy(notifyMask = NotifyRules.toggle(draft.notifyMask, off))) },
                        label = {
                            Text(
                                when (off) {
                                    0 -> stringResource(R.string.notify_on_day)
                                    1 -> stringResource(R.string.notify_day_before)
                                    else -> pluralStringResource(R.plurals.notify_n_days, off, off)
                                },
                            )
                        },
                    )
                }
            }

            OutlinedTextField(
                value = draft.memo,
                onValueChange = { onChange(draft.copy(memo = it.take(500))) },
                label = { Text(stringResource(R.string.field_memo)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    if (draft.title.isBlank()) titleError = true
                    else scope.launch { if (onSave()) onClose() }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium) }
        }
    }

    if (pickDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = draft.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onChange(draft.copy(dateEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())) }
                    pickDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(st) }
    }
    if (pickTime) {
        val t = draft.time ?: java.time.LocalTime.of(10, 0)
        val st = rememberTimePickerState(t.hour, t.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(st) },
            confirmButton = { TextButton(onClick = { onChange(draft.copy(timeMinutes = st.hour * 60 + st.minute)); pickTime = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(); onClose() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: String) {
    Column {
        HorizontalDivider()
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
    }
}
