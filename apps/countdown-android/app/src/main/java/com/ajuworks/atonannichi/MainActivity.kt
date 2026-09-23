package com.ajuworks.atonannichi

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ajuworks.atonannichi.ads.AdsManager
import com.ajuworks.atonannichi.ui.DaysTheme
import com.ajuworks.atonannichi.ui.EditScreen
import com.ajuworks.atonannichi.ui.HomeScreen
import com.ajuworks.atonannichi.ui.MainViewModel
import com.ajuworks.atonannichi.ui.PrivacyPolicyScreen
import com.ajuworks.atonannichi.ui.SettingsScreen
import com.ajuworks.atonannichi.widget.Tick
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var nav: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Tick.ensureChannel(this)
        AdsManager.start(this)
        // アプリを開いたときも、ウィジェット・通知・次のアラームを整える（端末再起動直後の取りこぼし対策）
        lifecycleScope.launch { Tick.run(applicationContext) }

        setContent {
            DaysTheme {
                val events by vm.events.collectAsStateWithLifecycle()
                val today by vm.today.collectAsStateWithLifecycle()
                val draft by vm.draft.collectAsStateWithLifecycle()
                val privacyRequired by AdsManager.privacyOptionsRequired.collectAsStateWithLifecycle()
                val navController = rememberNavController().also { nav = it }
                val snackbar = remember { SnackbarHostState() }
                val context = LocalContext.current
                var notificationsAllowed by remember { mutableStateOf(Tick.canPost(context)) }

                val lifecycle = LocalLifecycleOwner.current.lifecycle
                LaunchedEffect(lifecycle) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        vm.refreshToday()
                        notificationsAllowed = Tick.canPost(context)
                    }
                }
                val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    notificationsAllowed = Tick.canPost(context)
                }
                val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) vm.pickPhoto(uri)
                }

                LaunchedEffect(Unit) { handleIntent(intent) }

                NavHost(navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            events = events,
                            today = today,
                            snackbar = snackbar,
                            onAdd = {
                                vm.newDraft()
                                navController.navigate("edit")
                            },
                            onOpen = { id ->
                                vm.editDraft(id)
                                navController.navigate("edit")
                            },
                            onSettings = { navController.navigate("settings") },
                        )
                    }
                    composable("edit") {
                        val d = draft
                        val close = {
                            vm.cancelDraft()
                            navController.popBackStack("home", inclusive = false)
                        }
                        BackHandler { close() }
                        if (d == null) {
                            LaunchedEffect(Unit) { navController.popBackStack("home", inclusive = false) }
                        } else {
                            EditScreen(
                                draft = d,
                                today = today,
                                onChange = vm::update,
                                onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                onRemovePhoto = vm::removePhoto,
                                onSave = {
                                    val ok = vm.saveDraft()
                                    if (ok) {
                                        // 通知を使う設定なら、初回の保存時に許可を求める
                                        if (Build.VERSION.SDK_INT >= 33 && !Tick.canPost(context) && d.notifyMask != 0) {
                                            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            // 編集を終えたときだけ、3回に1回
                                            AdsManager.maybeShowAfterResult(this@MainActivity, vm.savesForAd)
                                        }
                                    }
                                    ok
                                },
                                onDelete = { vm.delete(d.id) },
                                onClose = { if (vm.draft.value == null) navController.popBackStack("home", inclusive = false) else close() },
                            )
                        }
                    }
                    composable("settings") {
                        SettingsScreen(
                            notificationsAllowed = notificationsAllowed,
                            privacyOptionsRequired = privacyRequired,
                            onAllowNotifications = {
                                if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            onPrivacyPolicy = { navController.navigate("privacy") },
                            onPrivacyOptions = { AdsManager.showPrivacyOptions(this@MainActivity) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("privacy") { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** ウィジェットや通知から開いたとき、そのイベントを開く。 */
    private fun handleIntent(intent: Intent?) {
        val n = nav ?: return
        val id = intent?.getLongExtra(EXTRA_EVENT_ID, -1L) ?: -1L
        when {
            id > 0 -> {
                vm.editDraft(id)
                n.navigate("edit") { launchSingleTop = true }
            }
            intent?.getBooleanExtra(EXTRA_ADD, false) == true -> {
                vm.newDraft()
                n.navigate("edit") { launchSingleTop = true }
            }
        }
        intent?.removeExtra(EXTRA_EVENT_ID)
        intent?.removeExtra(EXTRA_ADD)
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_ADD = "add"
    }
}
