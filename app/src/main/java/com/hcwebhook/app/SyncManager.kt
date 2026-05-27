package com.hcwebhook.app

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class SyncManager(private val context: Context) {

    private val appVersionName: String by lazy {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (_: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    private val preferencesManager = PreferencesManager(context)
    private val usageStatsDataManager = UsageStatsDataManager(context)

    suspend fun getRealtimeJsonPayload(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!usageStatsDataManager.hasUsageStatsPermission()) {
                return@withContext Result.failure(Exception("Usage stats permission not granted"))
            }

            val usageMetrics = usageStatsDataManager.queryDailyUsageStats()
            if (usageMetrics.isEmpty()) {
                return@withContext Result.failure(Exception("No usage data available"))
            }

            val payload = UsagePayload(
                timestamp = Instant.now().toString(),
                device_name = usageStatsDataManager.getDeviceName(),
                usage_stats = usageMetrics
            )

            val jsonPayload = Json.encodeToString(payload)
            LocalHttpServerManager.publishPayload(jsonPayload)
            Result.success(jsonPayload)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun performSync(syncType: String = "auto", targetWebhooks: List<WebhookConfig>? = null): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val webhookConfigs = preferencesManager.getWebhookConfigs()
            val enabledWebhookConfigs = (targetWebhooks ?: webhookConfigs).filter { it.isEnabled }
            val localTcpEnabled = preferencesManager.isLocalTcpEnabled()

            if (enabledWebhookConfigs.isEmpty() && !localTcpEnabled) {
                return@withContext Result.failure(Exception("No enabled webhook URLs configured and local TCP server is disabled"))
            }

            if (!usageStatsDataManager.hasUsageStatsPermission()) {
                return@withContext Result.failure(Exception("Usage stats permission not granted"))
            }

            val usageMetrics = usageStatsDataManager.queryDailyUsageStats()

            if (usageMetrics.isEmpty()) {
                preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
                preferencesManager.setLastSyncSummary("No usage data")
                return@withContext Result.success(SyncResult.NoData)
            }

            val payload = UsagePayload(
                timestamp = Instant.now().toString(),
                device_name = usageStatsDataManager.getDeviceName(),
                usage_stats = usageMetrics
            )

            val jsonPayload = Json.encodeToString(payload)
            LocalHttpServerManager.publishPayload(jsonPayload)

            if (enabledWebhookConfigs.isNotEmpty()) {
                var atLeastOneSuccess = false
                var lastFailure: Throwable? = null

                val dispatcher = NotificationDispatcher()
                val globalNotifs = preferencesManager.getNotificationConfigs()
                val aggregatedNotifs = mutableMapOf<NotificationConfig, MutableList<String>>()

                for (config in enabledWebhookConfigs) {
                    val manager = WebhookManager(
                        webhookConfigs = listOf(config),
                        context = context,
                        dataType = "usage_stats",
                        recordCount = usageMetrics.size,
                        syncType = syncType,
                        payload = jsonPayload
                    )
                    val result = manager.postData(jsonPayload)

                    val notifConfigs = config.notificationConfigIds.mapNotNull { id ->
                        globalNotifs.find { it.id == id }
                    }

                    if (result.isSuccess) {
                        atLeastOneSuccess = true
                        val msg = "✅ ${config.url}: ${usageMetrics.size} apps"
                        notifConfigs.forEach { nc ->
                            aggregatedNotifs.getOrPut(nc) { mutableListOf() }.add(msg)
                        }
                    } else {
                        lastFailure = result.exceptionOrNull()
                        val msg = "❌ ${config.url}: ${lastFailure?.message ?: "Error"}"
                        notifConfigs.forEach { nc ->
                            aggregatedNotifs.getOrPut(nc) { mutableListOf() }.add(msg)
                        }
                    }
                }

                aggregatedNotifs.forEach { (nc, messages) ->
                    val title = if (messages.any { it.startsWith("❌") }) "Sync Completed with Errors" else "Sync Succeeded"
                    dispatcher.dispatch(
                        context = context,
                        config = nc,
                        title = title,
                        message = messages.joinToString("\n")
                    )
                }

                if (!atLeastOneSuccess) {
                    return@withContext Result.failure(lastFailure ?: Exception("Failed to post to webhooks"))
                }
            }

            val summary = "${usageMetrics.size} apps · ${usageMetrics.sumOf { it.total_time_visible_seconds } / 3600}h total"
            preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
            preferencesManager.setLastSyncSummary(summary)

            Result.success(SyncResult.Success(usageMetrics.size))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

sealed class SyncResult {
    object NoData : SyncResult()
    data class Success(val appCount: Int) : SyncResult()
}
