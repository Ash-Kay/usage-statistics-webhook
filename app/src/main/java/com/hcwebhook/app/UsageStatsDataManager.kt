package com.hcwebhook.app

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import kotlinx.serialization.Serializable

@Serializable
data class UsagePayload(
    val timestamp: String,
    val device_name: String,
    val usage_stats: List<AppUsageMetric>
)

@Serializable
data class AppUsageMetric(
    val package_name: String,
    val app_name: String,
    val total_time_visible_seconds: Long,
    val last_time_used_timestamp: Long
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
                val appLabel = try {
                    val appInfo = packageManager.getApplicationInfo(usageStat.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    usageStat.packageName
                }

                AppUsageMetric(
                    package_name = usageStat.packageName,
                    app_name = appLabel,
                    total_time_visible_seconds = usageStat.totalTimeInForeground / 1000,
                    last_time_used_timestamp = usageStat.lastTimeUsed
                )
            }
            .sortedByDescending { it.total_time_visible_seconds }
    }

    fun getDeviceName(): String {
        return Build.MODEL.ifBlank { "${Build.MANUFACTURER} ${Build.MODEL}" }
    }
}

