package com.hcwebhook.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.material.icons.filled.Translate
import com.hcwebhook.app.FlavorUtils
import com.uswebhook.app.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pageCount = if (FlavorUtils.isPlayStore) 4 else 3
    val lastPage = pageCount - 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    var showSkipDialog by remember { mutableStateOf(false) }

    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text(stringResource(R.string.onboarding_skip_dialog_title)) },
            text = { Text(stringResource(R.string.onboarding_skip_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showSkipDialog = false
                    onFinish()
                }) { Text(stringResource(R.string.onboarding_skip)) }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) { Text(stringResource(R.string.onboarding_cancel)) }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageSelector()
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pageCount) { index ->
                        val color by animateColorAsState(
                            targetValue = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant,
                            label = "dot_color"
                        )
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .background(color, CircleShape)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < lastPage) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (pagerState.currentPage < lastPage) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_get_started))
                }

                if (pagerState.currentPage < lastPage) {
                    TextButton(onClick = { showSkipDialog = true }) {
                        Text(stringResource(R.string.onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> HowItWorksPage()
                2 -> PrivacyPage()
                3 -> ThankYouPage()
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Webhook,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Usage Statistics Webhook",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Track your daily screen time per app and sync it to your webhook endpoint automatically.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        FeatureRow(
            icon = Icons.Filled.Timer,
            title = "Screen Time Tracking",
            description = "Monitors daily per-app usage via Android's UsageStatsManager"
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(
            icon = Icons.Filled.Webhook,
            title = "Webhook Sync",
            description = "Pushes JSON payloads to n8n or any HTTP endpoint"
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(
            icon = Icons.Filled.Lock,
            title = "Privacy First",
            description = "Data stays on your device until you choose to sync"
        )
    }
}

@Composable
private fun HowItWorksPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "How It Works",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "This app collects daily screen time data for each app on your device and formats it as a clean JSON payload.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        val steps = listOf(
            "Grant Usage Access permission in system settings",
            "Configure your webhook URL(s)",
            "Set your preferred sync schedule",
            "Data syncs automatically in the background"
        )

        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrivacyPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_privacy_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your data is processed locally and only sent to webhook URLs you configure.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        val points = listOf(
            "No data collection by the app developer",
            "No third-party analytics or tracking",
            "Open source — inspect the code yourself",
            "Data only goes where YOU configure it",
            "Fully offline capable"
        )

        points.forEach { point ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    text = point,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThankYouPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_thank_you_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_thank_you_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LanguageSelector() {
    var expanded by remember { mutableStateOf(false) }

    val languages = listOf(
        "" to "System Default",
        "en" to "English",
        "ta" to "தமிழ்",
        "fr" to "Français",
        "de" to "Deutsch",
        "es" to "Español",
        "pt" to "Português",
        "zh" to "中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "it" to "Italiano"
    )

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Translate, contentDescription = "Select Language")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (tag, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        if (tag.isEmpty()) {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        } else {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        }
                        expanded = false
                    }
                )
            }
        }
    }
}
