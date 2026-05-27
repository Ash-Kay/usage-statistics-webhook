package com.hcwebhook.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hcwebhook.app.BuildConfig
import com.hcwebhook.app.screens.AboutScreen
import com.hcwebhook.app.screens.ChangelogScreen
import com.hcwebhook.app.screens.ConfigurationScreen
import com.hcwebhook.app.screens.DashboardScreen
import com.hcwebhook.app.screens.LocalHttpSettingsScreen
import com.hcwebhook.app.screens.LogsScreen
import com.hcwebhook.app.screens.NotificationsScreen
import com.hcwebhook.app.screens.OnboardingScreen
import com.hcwebhook.app.screens.SettingsBackupScreen
import com.hcwebhook.app.screens.WebhooksScreen
import com.hcwebhook.app.screens.WhatsNewSheet
import com.hcwebhook.app.ui.theme.HCWebhookTheme
import androidx.compose.ui.res.stringResource

class MainActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    internal var pendingSyncCallback: (() -> Unit)? = null
    internal val openLocalHttpRequest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FlavorUtils.verifyPlayStoreInstallation(this)

        installSplashScreen()
        enableEdgeToEdge()
        preferencesManager = PreferencesManager(this)
        if (intent?.getBooleanExtra(LocalHttpServerService.EXTRA_OPEN_LOCAL_HTTP, false) == true) {
            openLocalHttpRequest.value = true
        }

        setContent {
            HCWebhookTheme {
                var showOnboarding by remember { mutableStateOf(!preferencesManager.hasSeenOnboarding()) }
                if (showOnboarding) {
                    OnboardingScreen(onFinish = {
                        preferencesManager.setHasSeenOnboarding()
                        showOnboarding = false
                    })
                } else {
                    MainScreenWithNav(
                        activity = this@MainActivity,
                        preferencesManager = preferencesManager,
                        onRestartOnboarding = { showOnboarding = true }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(LocalHttpServerService.EXTRA_OPEN_LOCAL_HTTP, false)) {
            openLocalHttpRequest.value = true
        }
    }

    @Composable
    fun MainScreenWithNav(
        activity: MainActivity,
        preferencesManager: PreferencesManager,
        onRestartOnboarding: () -> Unit = {}
    ) {
        var selectedScreen by remember { mutableStateOf<NavigationScreen>(NavigationScreen.Home) }
        var showDashboard by remember { mutableStateOf(false) }
        var showLocalHttpSettings by remember { mutableStateOf(false) }
        var showNotificationsSettings by remember { mutableStateOf(false) }
        var showSettingsBackup by remember { mutableStateOf(false) }
        var showChangelog by remember { mutableStateOf(false) }
        var showWhatsNew by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val currentCode = BuildConfig.VERSION_CODE
            val lastSeen = preferencesManager.getLastSeenVersionCode()
            when {
                lastSeen == 0 -> preferencesManager.setLastSeenVersionCode(currentCode)
                currentCode > lastSeen -> showWhatsNew = true
            }
        }

        LaunchedEffect(activity.openLocalHttpRequest.value) {
            if (activity.openLocalHttpRequest.value) {
                showLocalHttpSettings = true
                activity.openLocalHttpRequest.value = false
            }
        }

        val usageStatsDataManager = remember { UsageStatsDataManager(activity) }
        var hasPermissions by remember { mutableStateOf<Boolean?>(null) }

        LaunchedEffect(Unit) {
            hasPermissions = usageStatsDataManager.hasUsageStatsPermission()
        }

        Scaffold(
            bottomBar = {
                if (!showDashboard && !showLocalHttpSettings && !showNotificationsSettings && !showSettingsBackup && !showChangelog) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleResId)) },
                                label = { Text(stringResource(screen.titleResId)) },
                                selected = selectedScreen == screen,
                                onClick = { selectedScreen = screen }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            BackHandler {
                if (showDashboard) {
                    showDashboard = false
                } else if (showLocalHttpSettings) {
                    showLocalHttpSettings = false
                } else if (showNotificationsSettings) {
                    showNotificationsSettings = false
                } else if (showSettingsBackup) {
                    showSettingsBackup = false
                } else if (showChangelog) {
                    showChangelog = false
                } else if (selectedScreen != NavigationScreen.Home) {
                    selectedScreen = NavigationScreen.Home
                } else {
                    activity.finish()
                }
            }
            val saveableStateHolder = rememberSaveableStateHolder()
            Box(modifier = Modifier.padding(padding)) {
                if (showDashboard) {
                    DashboardScreen(
                        hasPermissions = hasPermissions,
                        onBack = { showDashboard = false },
                    )
                } else if (showLocalHttpSettings) {
                    LocalHttpSettingsScreen(onBack = { showLocalHttpSettings = false })
                } else if (showNotificationsSettings) {
                    NotificationsScreen(onBack = { showNotificationsSettings = false })
                } else if (showSettingsBackup) {
                    SettingsBackupScreen(onBack = { showSettingsBackup = false })
                } else if (showChangelog) {
                    ChangelogScreen(onBack = { showChangelog = false })
                } else {
                    saveableStateHolder.SaveableStateProvider(selectedScreen.toString()) {
                        when (selectedScreen) {
                            is NavigationScreen.Home -> ConfigurationScreen(
                                activity = activity,
                                hasPermissions = hasPermissions,
                                onPermissionsChanged = { hasPermissions = it },
                                onOpenLocalHttpSettings = { showLocalHttpSettings = true },
                                onOpenDashboard = { showDashboard = true },
                            )
                            is NavigationScreen.Webhooks -> WebhooksScreen(
                                onOpenNotificationsSettings = { showNotificationsSettings = true }
                            )
                            is NavigationScreen.Logs -> LogsScreen()
                            is NavigationScreen.About -> AboutScreen(
                                onRestartOnboarding = onRestartOnboarding,
                                onOpenLocalHttpSettings = { showLocalHttpSettings = true },
                                onOpenNotificationsSettings = { showNotificationsSettings = true },
                                onOpenSettingsBackup = { showSettingsBackup = true },
                                onOpenChangelog = { showChangelog = true },
                            )
                        }
                    }
                }
            }
        }

        WhatsNewSheet(
            visible = showWhatsNew,
            onDismiss = {
                showWhatsNew = false
                preferencesManager.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
            },
        )
    }
}