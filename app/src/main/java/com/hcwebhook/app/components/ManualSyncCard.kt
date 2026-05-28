package com.hcwebhook.app.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.SyncManager
import com.hcwebhook.app.SyncResult
import com.hcwebhook.app.UsageStatsDataManager
import com.hcwebhook.app.WebhookConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.uswebhook.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSyncCard(onSyncCompleted: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val usageStatsDataManager = remember { UsageStatsDataManager(context) }

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmSheet by remember { mutableStateOf(false) }

    val webhookConfigs = preferencesManager.getWebhookConfigs()
    val enabledWebhooks = remember(webhookConfigs) { webhookConfigs.filter { it.isEnabled } }
    val localTcpEnabled = preferencesManager.isLocalTcpEnabled()
    var selectedWebhookUrl by remember { mutableStateOf<String?>(null) }

    if (showConfirmSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfirmSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.manual_sync_confirm_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    "Sync daily usage statistics to your configured webhooks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (enabledWebhooks.size >= 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            stringResource(R.string.manual_sync_send_to),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedWebhookUrl = null },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedWebhookUrl == null,
                                onClick = { selectedWebhookUrl = null }
                            )
                            Text(stringResource(R.string.manual_sync_all_webhooks))
                        }
                        enabledWebhooks.forEach { config ->
                            val host = remember(config.url) {
                                try { java.net.URI(config.url).host?.takeIf { it.isNotEmpty() } ?: config.url }
                                catch (_: Exception) { config.url }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWebhookUrl = config.url },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedWebhookUrl == config.url,
                                    onClick = { selectedWebhookUrl = config.url }
                                )
                                Text(host, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        showConfirmSheet = false
                        if (isSyncing) return@Button

                        scope.launch {
                            isSyncing = true
                            syncMessage = null

                            try {
                                if (!usageStatsDataManager.hasUsageStatsPermission()) {
                                    syncMessage = "Usage access permission not granted"
                                    isSyncing = false
                                    return@launch
                                }

                                val syncManager = SyncManager(context)
                                val targetWebhooks: List<WebhookConfig>? = selectedWebhookUrl?.let { url ->
                                    enabledWebhooks.filter { it.url == url }
                                }

                                val result = syncManager.performSync(syncType = "manual", targetWebhooks = targetWebhooks)

                                when {
                                    result.isSuccess -> {
                                        val syncResult = result.getOrThrow()
                                        syncMessage = when (syncResult) {
                                            is SyncResult.NoData -> "No usage data available"
                                            is SyncResult.Success -> "Synced ${syncResult.appCount} apps successfully"
                                        }
                                        onSyncCompleted()
                                    }
                                    result.isFailure -> {
                                        syncMessage = "Sync failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                syncMessage = "Sync failed: ${e.message}"
                            } finally {
                                isSyncing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.manual_sync_btn))
                }
                OutlinedButton(
                    onClick = { showConfirmSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.manual_sync_title), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sync the last 24 hours of app usage data to your webhooks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showConfirmSheet = true },
                enabled = !isSyncing && (webhookConfigs.isNotEmpty() || localTcpEnabled),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.manual_sync_syncing))
                } else {
                    Text(stringResource(R.string.manual_sync_btn))
                }
            }

            syncMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.contains("failed", ignoreCase = true))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
