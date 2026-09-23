package com.ajuworks.atonannichi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ajuworks.atonannichi.BuildConfig
import com.ajuworks.atonannichi.R
import com.ajuworks.atonannichi.core.Countdown
import com.ajuworks.atonannichi.data.EventEntity
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    events: List<EventEntity>,
    today: LocalDate,
    snackbar: SnackbarHostState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings_title)) } },
            )
        },
        floatingActionButton = {
            if (events.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text(stringResource(R.string.add_event)) })
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (events.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("✈️  🎂  🎤", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.empty_sub), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onAdd, modifier = Modifier.heightIn(min = 56.dp)) {
                    Icon(Icons.Outlined.Add, null)
                    Text("  " + stringResource(R.string.add_first))
                }
            }
            return@Scaffold
        }
        val (active, ended) = events.partition { it.countdown(today) != Countdown.Ended }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(active, key = { it.id }) { e -> EventCard(e, today, onClick = { onOpen(e.id) }) }
            if (ended.isNotEmpty()) {
                item { Text(stringResource(R.string.ended_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(ended, key = { it.id }) { e -> EventCard(e, today, onClick = { onOpen(e.id) }) }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Widgets, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("  " + stringResource(R.string.widget_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Row(modifier: Modifier, verticalAlignment: Alignment.Vertical, content: @Composable () -> Unit) =
    androidx.compose.foundation.layout.Row(modifier, verticalAlignment = verticalAlignment) { content() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScaffold(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        content = content,
    )
}

@Composable
fun SettingsScreen(
    notificationsAllowed: Boolean,
    privacyOptionsRequired: Boolean,
    onAllowNotifications: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onPrivacyOptions: () -> Unit,
    onBack: () -> Unit,
) {
    SubScaffold(stringResource(R.string.settings_title), onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (!notificationsAllowed) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.notifications_off), color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text(stringResource(R.string.notifications_off_sub)) },
                    modifier = Modifier.clickable(onClick = onAllowNotifications),
                )
                HorizontalDivider()
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.widget_how_title)) },
                supportingContent = { Text(stringResource(R.string.widget_how_body)) },
            )
            HorizontalDivider()
            LinkRow(stringResource(R.string.privacy_policy), onPrivacyPolicy)
            if (privacyOptionsRequired) LinkRow(stringResource(R.string.ad_privacy_options), onPrivacyOptions)
            ListItem(headlineContent = { Text(stringResource(R.string.version)) }, supportingContent = { Text(BuildConfig.VERSION_NAME) })
            Text(
                stringResource(R.string.settings_on_device),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
    HorizontalDivider()
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    SubScaffold(stringResource(R.string.privacy_policy), onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.privacy_policy_body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
