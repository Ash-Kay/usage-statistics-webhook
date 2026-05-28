package com.hcwebhook.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId

@Serializable
data class UsagePayload(
    val timestamp: String,
    val device_name: String,
    val usage_stats: List<AppUsageMetric>,
    val usage_events: List<AppUsageEvent> = emptyList()
)

@Serializable
data class AppUsageMetric(
    val package_name: String,
    val app_name: String,
    val total_time_visible_seconds: Long,
    val last_time_used_timestamp: Long
)

@Serializable
data class AppUsageEvent(
    val package_name: String,
    val app_name: String,
    val event_type: String,
    val timestamp: Long
)

class UsageStatsDataManager(private val context: Context) {

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun queryDailyUsageStats(startTime: Long? = null, endTime: Long? = null): List<AppUsageMetric> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        val end = endTime ?: System.currentTimeMillis()
        val start = startTime ?: (end - (24 * 60 * 60 * 1000))

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .map { usageStat ->
                val appLabel = resolveAppLabel(packageManager, usageStat.packageName)

                AppUsageMetric(
                    package_name = usageStat.packageName,
                    app_name = appLabel,
                    total_time_visible_seconds = usageStat.totalTimeInForeground / 1000,
                    last_time_used_timestamp = usageStat.lastTimeUsed
                )
            }
            .sortedByDescending { it.total_time_visible_seconds }
    }

    fun queryUsageEvents(sinceTimestamp: Long? = null): List<AppUsageEvent> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        val endTime = System.currentTimeMillis()
        val startTime = sinceTimestamp
            ?: LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val events = mutableListOf<AppUsageEvent>()
        val labelCache = mutableMapOf<String, String>()

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            val eventTypeName = mapEventType(event.eventType) ?: continue

            val appLabel = labelCache.getOrPut(event.packageName) {
                resolveAppLabel(packageManager, event.packageName)
            }

            events.add(
                AppUsageEvent(
                    package_name = event.packageName,
                    app_name = appLabel,
                    event_type = eventTypeName,
                    timestamp = event.timeStamp
                )
            )
        }

        return events
    }

    fun getDeviceName(): String {
        return Build.MODEL.ifBlank { "${Build.MANUFACTURER} ${Build.MODEL}" }
    }

    private fun resolveAppLabel(packageManager: PackageManager, packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun mapEventType(type: Int): String? {
        return when (type) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> "move_to_foreground"
            UsageEvents.Event.MOVE_TO_BACKGROUND -> "move_to_background"
            UsageEvents.Event.ACTIVITY_RESUMED -> "activity_resumed"
            UsageEvents.Event.ACTIVITY_PAUSED -> "activity_paused"
            UsageEvents.Event.ACTIVITY_STOPPED -> "activity_stopped"
            UsageEvents.Event.SCREEN_INTERACTIVE -> "screen_interactive"
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> "screen_non_interactive"
            UsageEvents.Event.DEVICE_SHUTDOWN -> "device_shutdown"
            UsageEvents.Event.DEVICE_STARTUP -> "device_startup"
            else -> null
        }
    }
}
