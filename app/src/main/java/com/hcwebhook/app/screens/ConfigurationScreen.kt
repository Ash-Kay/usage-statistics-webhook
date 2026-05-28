package com.hcwebhook.app.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.*
import java.util.Calendar
import com.uswebhook.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    activity: MainActivity,
    hasPermissions: Boolean?,
    onPermissionsChanged: (Boolean) -> Unit = {},
    onOpenLocalHttpSettings: () -> Unit = {},
    onOpenDashboard: () -> Unit = {},
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val usageStatsDataManager = remember { UsageStatsDataManager(context) }

    var syncMode by remember { mutableStateOf(preferencesManager.getSyncMode()) }
    var syncInterval by remember { mutableStateOf(preferencesManager.getSyncIntervalMinutes().toString()) }
    var scheduledSyncs by remember { mutableStateOf(preferencesManager.getScheduledSyncs()) }

    var lastSyncTime by remember { mutableStateOf(preferencesManager.getLastSyncTime()) }
    var lastSyncSummary by remember { mutableStateOf(preferencesManager.getLastSyncSummary()) }
    var lastSyncRelativeTime by remember { mutableStateOf("") }
    val isLocalHttpEnabled = preferencesManager.isLocalTcpEnabled()
    val localHttpPort = preferencesManager.getLocalTcpPort()

    LaunchedEffect(lastSyncTime) {
        while (true) {
            val syncTime = lastSyncTime
            lastSyncRelativeTime = if (syncTime != null) {
                val elapsed = System.currentTimeMillis() - syncTime
                val seconds = elapsed / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                when {
                    seconds < 60 -> "just now"
                    minutes < 60 -> "${minutes}m ago"
                    hours < 24 -> "${hours}h ago"
                    else -> "${hours / 24}d ago"
                }
            } else ""
            kotlinx.coroutines.delay(30_000)
        }
    }

    val scrollState = rememberScrollState()
    val statusDotTransition = rememberInfiniteTransition(label = "local_http_status_dot")
    val statusDotAlpha by statusDotTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "local_http_status_dot_alpha"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (hasPermissions == false) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Filled.Shield, "Grant Permission") },
                    text = { Text("Grant Usage Access") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Usage Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (lastSyncTime != null) {
                        Text(
                            text = "Last sync $lastSyncRelativeTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onOpenDashboard,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = "Dashboard",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (lastSyncSummary != null) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (isLocalHttpEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenLocalHttpSettings() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(statusDotAlpha)
                                    .background(Color(0xFF22C55E), shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.config_local_http_status_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.config_local_http_status_desc, localHttpPort),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            } else {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenLocalHttpSettings() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.config_local_tcp_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            when {
                hasPermissions == null -> {
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking permissions…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                hasPermissions == false -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Usage Access Required", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Grant usage access permission to allow this app to read screen time data from your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Usage Access Settings")
                            }
                        }
                    }
                }
                hasPermissions == true -> {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Permission Granted", style = MaterialTheme.typography.titleSmall)
                                Text("Usage access is enabled. Screen time data will be synced.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.config_sync_schedule_title), style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = syncMode == SyncMode.INTERVAL,
                            onClick = { syncMode = SyncMode.INTERVAL; preferencesManager.setSyncMode(SyncMode.INTERVAL); (activity.application as? USWebhookApplication)?.scheduleSyncWork() },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text(stringResource(R.string.config_sync_mode_interval)) }
                        SegmentedButton(
                            selected = syncMode == SyncMode.SCHEDULED,
                            onClick = { syncMode = SyncMode.SCHEDULED; preferencesManager.setSyncMode(SyncMode.SCHEDULED); (activity.application as? USWebhookApplication)?.cancelSyncWork(); ScheduledSyncManager(context).scheduleAllAlarms() },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text(stringResource(R.string.config_sync_mode_scheduled)) }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedVisibility(visible = syncMode == SyncMode.INTERVAL, enter = expandVertically(), exit = shrinkVertically()) {
                        Column {
                            OutlinedTextField(
                                value = syncInterval,
                                onValueChange = { syncInterval = it },
                                label = { Text(stringResource(R.string.config_sync_interval_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val interval = syncInterval.toIntOrNull()
                                    if (interval != null && interval >= 15) {
                                        preferencesManager.setSyncIntervalMinutes(interval)
                                        (activity.application as? USWebhookApplication)?.scheduleSyncWork()
                                        Toast.makeText(context, context.getString(R.string.config_toast_interval_saved), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.config_toast_min_interval), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text(stringResource(R.string.config_action_update_interval)) }
                        }
                    }
                    AnimatedVisibility(visible = syncMode == SyncMode.SCHEDULED, enter = expandVertically(), exit = shrinkVertically()) {
                        Column {
                            scheduledSyncs.forEach { schedule ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = schedule.getDisplayTime(), style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(checked = schedule.enabled, onCheckedChange = { enabled ->
                                            val updatedList = scheduledSyncs.map { if (it.id == schedule.id) it.copy(enabled = enabled) else it }
                                            scheduledSyncs = updatedList
                                            preferencesManager.setScheduledSyncs(updatedList)
                                            val syncManager = ScheduledSyncManager(context)
                                            if (enabled) syncManager.scheduleAlarm(schedule) else syncManager.cancelAlarm(schedule.id)
                                        })
                                        IconButton(onClick = {
                                            val updatedList = scheduledSyncs.filter { it.id != schedule.id }
                                            scheduledSyncs = updatedList
                                            preferencesManager.setScheduledSyncs(updatedList)
                                            ScheduledSyncManager(context).cancelAlarm(schedule.id)
                                        }) { Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(18.dp)) }
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                val calendar = Calendar.getInstance()
                                TimePickerDialog(context, { _, hour, minute ->
                                    val newSchedule = ScheduledSync.create(hour, minute)
                                    val updatedList = scheduledSyncs + newSchedule
                                    scheduledSyncs = updatedList
                                    preferencesManager.setScheduledSyncs(updatedList)
                                    ScheduledSyncManager(context).scheduleAlarm(newSchedule)
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.config_action_add_schedule))
                            }
                        }
                    }
                }
            }

            com.hcwebhook.app.components.ManualSyncCard(onSyncCompleted = {
                lastSyncTime = preferencesManager.getLastSyncTime()
                lastSyncSummary = preferencesManager.getLastSyncSummary()
            })
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            val newStatus = usageStatsDataManager.hasUsageStatsPermission()
            if (newStatus != hasPermissions) {
                onPermissionsChanged(newStatus)
            }
        }
    }
}
