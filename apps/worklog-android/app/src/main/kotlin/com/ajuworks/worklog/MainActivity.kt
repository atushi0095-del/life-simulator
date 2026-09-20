package com.ajuworks.worklog

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.FileProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ajuworks.worklog.ads.AdsController
import com.ajuworks.worklog.ads.InterstitialAds
import com.ajuworks.worklog.ui.LocalDurationFormatter
import com.ajuworks.worklog.ui.history.EditRecordScreen
import com.ajuworks.worklog.ui.history.HistoryScreen
import com.ajuworks.worklog.ui.home.HomeScreen
import com.ajuworks.worklog.ui.rememberDurationFormatter
import com.ajuworks.worklog.ui.settings.SettingsScreen
import com.ajuworks.worklog.ui.theme.WorkLogTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Consent and the Ads SDK start in the background and are never awaited.
        AdsController.start(this)

        setContent {
            WorkLogTheme {
                CompositionLocalProvider(
                    LocalDurationFormatter provides rememberDurationFormatter(),
                ) {
                    WorkLogNavHost(
                        onShareCsv = ::shareCsv,
                        onOpenPrivacyPolicy = ::openPrivacyPolicy,
                        onOpenAdPrivacy = { AdsController.showPrivacyOptionsForm(this) },
                        onMaybeShowInterstitial = ::maybeShowInterstitial,
                    )
                }

                LaunchedEffect(Unit) { askForNotificationPermissionIfNeeded() }
            }
        }
    }

    /**
     * Asked for once, lazily, and never blocking: the reminder is a nicety and
     * the app is fully usable if it is declined.
     */
    private fun askForNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Writes the CSV to app-private cache and hands it to the system share
     * sheet. The app has no network permission of its own for this - wherever
     * the file goes, the user chose it.
     */
    private fun shareCsv(fileName: String, content: String) {
        val dir = File(cacheDir, "csv").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)

        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_export_csv)))
    }

    private fun openPrivacyPolicy() {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        }
    }

    private fun maybeShowInterstitial(historyViewCount: Int) {
        if (InterstitialAds.shouldShow(historyViewCount)) {
            InterstitialAds.showIfReady(this)
        } else {
            InterstitialAds.preload(this)
        }
    }

    companion object {
        const val PRIVACY_POLICY_URL =
            "https://ajuworks.github.io/worklog/privacy-policy.html"
    }
}

private object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val EDIT = "edit/{recordId}"

    fun edit(recordId: Long) = "edit/$recordId"
}

@androidx.compose.runtime.Composable
private fun WorkLogNavHost(
    onShareCsv: (String, String) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenAdPrivacy: () -> Unit,
    onMaybeShowInterstitial: (Int) -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.HISTORY) { entry ->
            val viewModel: com.ajuworks.worklog.ui.history.HistoryViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    viewModelStoreOwner = entry,
                    factory = com.ajuworks.worklog.ui.history.HistoryViewModel.Factory,
                )
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenRecord = { navController.navigate(Routes.edit(it)) },
                onAddRecord = { navController.navigate(Routes.edit(0L)) },
                onShareCsv = onShareCsv,
                onHistoryViewed = { onMaybeShowInterstitial(viewModel.recordHistoryView()) },
                viewModel = viewModel,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onShareCsv = onShareCsv,
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                onOpenAdPrivacy = onOpenAdPrivacy,
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType }),
        ) { entry ->
            EditRecordScreen(
                recordId = entry.arguments?.getLong("recordId") ?: 0L,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
