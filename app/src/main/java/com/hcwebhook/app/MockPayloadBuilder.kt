package com.hcwebhook.app

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

object MockPayloadBuilder {
    fun build(enabledTypes: Set<String>? = null, appVersion: String = "unknown"): String {
        val now = System.currentTimeMillis()
        val payload = UsagePayload(
            timestamp = Instant.now().toString(),
            device_name = "Test Device",
            usage_stats = listOf(
                AppUsageMetric(
                    package_name = "com.instagram.android",
                    app_name = "Instagram",
                    total_time_visible_seconds = 3600,
                    last_time_used_timestamp = now - 60000
                ),
                AppUsageMetric(
                    package_name = "com.whatsapp",
                    app_name = "WhatsApp",
                    total_time_visible_seconds = 2400,
                    last_time_used_timestamp = now - 120000
                ),
                AppUsageMetric(
                    package_name = "com.google.android.youtube",
                    app_name = "YouTube",
                    total_time_visible_seconds = 1800,
                    last_time_used_timestamp = now - 300000
                ),
                AppUsageMetric(
                    package_name = "com.twitter.android",
                    app_name = "X",
                    total_time_visible_seconds = 900,
                    last_time_used_timestamp = now - 600000
                )
            ),
            usage_events = listOf(
                AppUsageEvent(
                    package_name = "com.instagram.android",
                    app_name = "Instagram",
                    event_type = "move_to_foreground",
                    timestamp = now - 3660000
                ),
                AppUsageEvent(
                    package_name = "com.instagram.android",
                    app_name = "Instagram",
                    event_type = "move_to_background",
                    timestamp = now - 60000
                ),
                AppUsageEvent(
                    package_name = "com.whatsapp",
                    app_name = "WhatsApp",
                    event_type = "move_to_foreground",
                    timestamp = now - 55000
                ),
                AppUsageEvent(
                    package_name = "com.whatsapp",
                    app_name = "WhatsApp",
                    event_type = "move_to_background",
                    timestamp = now - 10000
                )
            )
        )
        return Json.encodeToString(payload)
    }
}
