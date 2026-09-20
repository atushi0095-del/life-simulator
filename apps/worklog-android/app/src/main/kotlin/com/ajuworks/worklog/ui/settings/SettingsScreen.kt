package com.ajuworks.worklog.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajuworks.worklog.BuildConfig
import com.ajuworks.worklog.R
import com.ajuworks.worklog.data.WorkLogSettings
import com.ajuworks.worklog.ui.isJapanese
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Deliberately short. Everything here is either a real preference or a legal
 * link; nothing is a feature hiding in a menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onShareCsv: (fileName: String, content: String) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenAdPrivacy: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val japanese = isJapanese()
    var showWeekStartDialog by remember { mutableStateOf(false) }
    var showReminderHoursDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_section_reminder))

            SwitchRow(
                title = stringResource(R.string.settings_reminder_enabled),
                subtitle = stringResource(R.string.settings_reminder_enabled_note),
                checked = settings.reminderEnabled,
                onCheckedChange = viewModel::setReminderEnabled,
            )
            ClickableRow(
                title = stringResource(R.string.settings_reminder_hours),
                value = stringResource(R.string.value_hours, settings.reminderAfterHours),
                enabled = settings.reminderEnabled,
                onClick = { showReminderHoursDialog = true },
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_general))

            ClickableRow(
                title = stringResource(R.string.settings_week_start),
                value = settings.weekStart.getDisplayName(
                    TextStyle.FULL,
                    if (japanese) Locale.JAPANESE else Locale.ENGLISH,
                ),
                onClick = { showWeekStartDialog = true },
            )
            ClickableRow(
                title = stringResource(R.string.settings_export_csv),
                value = stringResource(R.string.settings_export_csv_note),
                onClick = {
                    scope.launch {
                        val (name, content) = viewModel.exportAllCsv(japanese)
                        onShareCsv(name, content)
                    }
                },
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_about))

            ClickableRow(
                title = stringResource(R.string.settings_privacy_policy),
                value = "",
                onClick = onOpenPrivacyPolicy,
            )
            ClickableRow(
                title = stringResource(R.string.settings_ad_privacy),
                value = "",
                onClick = onOpenAdPrivacy,
            )
            ClickableRow(
                title = stringResource(R.string.settings_version),
                value = BuildConfig.VERSION_NAME,
                onClick = {},
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showWeekStartDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_week_start),
            options = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY),
            selected = settings.weekStart,
            label = {
                it.getDisplayName(
                    TextStyle.FULL,
                    if (japanese) Locale.JAPANESE else Locale.ENGLISH,
                )
            },
            onSelect = {
                viewModel.setWeekStart(it)
                showWeekStartDialog = false
            },
            onDismiss = { showWeekStartDialog = false },
        )
    }

    if (showReminderHoursDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_reminder_hours),
            options = WorkLogSettings.REMINDER_HOUR_CHOICES,
            selected = settings.reminderAfterHours,
            label = { stringResource(R.string.value_hours, it) },
            onSelect = {
                viewModel.setReminderHours(it)
                showReminderHoursDialog = false
            },
            onDismiss = { showReminderHoursDialog = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ClickableRow(
    title: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                        )
                        Text(label(option))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}
