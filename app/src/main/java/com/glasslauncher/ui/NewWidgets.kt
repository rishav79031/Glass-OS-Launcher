package com.glasslauncher.ui

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

data class ScreenTimeStats(
    val totalMillis: Long = 0L,
    val topAppName: String = "",
    val topAppMillis: Long = 0L,
    val hasPermission: Boolean = false,
    val hourlyFractions: List<Float> = listOf(0.25f, 0.65f, 0.85f, 0.45f, 0.95f, 0.35f),
    val counterRangeText: String = "00:00 – Present"
) {
    val formattedTotal: String
        get() {
            val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(totalMillis)
            val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(totalMillis) % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

    val formattedTopApp: String
        get() {
            if (topAppName.isEmpty() || topAppMillis == 0L) {
                return if (hasPermission) "No apps tracked today" else "Tap to link Wellbeing"
            }
            val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(topAppMillis)
            val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(topAppMillis) % 60
            val time = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            return "Top: $topAppName $time"
        }
}

object ScreenTimeHelper {
    fun queryStats(context: Context): ScreenTimeStats {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps?.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps?.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        val hasPermission = mode == AppOpsManager.MODE_ALLOWED
        if (!hasPermission) {
            return ScreenTimeStats(
                totalMillis = 0L,
                topAppName = "Tap to grant permission",
                topAppMillis = 0L,
                hasPermission = false,
                hourlyFractions = listOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f),
                counterRangeText = "00:00 – Present"
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ScreenTimeStats(hasPermission = false)

        // Exact local midnight (00:00:00) to present moment
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val pm = context.packageManager
        val lockedApps = runCatching {
            com.glasslauncher.data.SettingsStore(context.applicationContext).settings.value.hiddenApps
        }.getOrDefault(emptySet())

        val launchablePackages = runCatching {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet())

        fun isEligibleApp(pkg: String): Boolean {
            val isSys = pkg.contains("keyguard") || pkg.contains("lockscreen") ||
                pkg.contains("systemui") || pkg.contains("ambient") || pkg == context.packageName || pkg in lockedApps
            return !isSys && (launchablePackages.isEmpty() || pkg in launchablePackages)
        }

        // 1. Digital Wellbeing method: Query exact foreground transition events from midnight to now
        val appUsageMap = mutableMapOf<String, Long>()
        val slotDurations = LongArray(6) // 4-hour intervals: 0-4, 4-8, 8-12, 12-16, 16-20, 20-24
        var eventsProcessed = false

        runCatching {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            var currentPkg: String? = null
            var currentStart = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val time = event.timeStamp.coerceIn(startTime, endTime)
                val type = event.eventType
                val pkg = event.packageName ?: continue

                if (type == UsageEvents.Event.ACTIVITY_RESUMED || type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    if (currentPkg != null) {
                        val duration = (time - currentStart).coerceAtLeast(0L)
                        appUsageMap[currentPkg!!] = (appUsageMap[currentPkg!!] ?: 0L) + duration
                        val slot = (((currentStart - startTime) / (4 * 3600_000L)).toInt()).coerceIn(0, 5)
                        slotDurations[slot] += duration
                    }
                    if (isEligibleApp(pkg)) {
                        currentPkg = pkg
                        currentStart = time
                        eventsProcessed = true
                    } else {
                        currentPkg = null
                    }
                } else if (type == UsageEvents.Event.ACTIVITY_PAUSED ||
                           type == UsageEvents.Event.MOVE_TO_BACKGROUND ||
                           type == UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                    if (currentPkg != null) {
                        val duration = (time - currentStart).coerceAtLeast(0L)
                        appUsageMap[currentPkg!!] = (appUsageMap[currentPkg!!] ?: 0L) + duration
                        val slot = (((currentStart - startTime) / (4 * 3600_000L)).toInt()).coerceIn(0, 5)
                        slotDurations[slot] += duration
                        currentPkg = null
                        eventsProcessed = true
                    }
                }
            }

            if (currentPkg != null) {
                val duration = (endTime - currentStart).coerceAtLeast(0L)
                appUsageMap[currentPkg!!] = (appUsageMap[currentPkg!!] ?: 0L) + duration
                val slot = (((currentStart - startTime) / (4 * 3600_000L)).toInt()).coerceIn(0, 5)
                slotDurations[slot] += duration
                eventsProcessed = true
            }
        }

        // 2. Fallback to daily bucketed stats if no transition events were stored
        if (!eventsProcessed || appUsageMap.isEmpty()) {
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            if (!stats.isNullOrEmpty()) {
                for (u in stats) {
                    val pkg = u.packageName
                    if (isEligibleApp(pkg) && u.totalTimeInForeground > 0 && u.lastTimeUsed >= startTime) {
                        appUsageMap[pkg] = maxOf(appUsageMap[pkg] ?: 0L, u.totalTimeInForeground)
                    }
                }
            }
        }

        val elapsedSinceMidnight = (endTime - startTime).coerceAtLeast(0L)
        val rawTotal = appUsageMap.values.sum()
        val totalTime = rawTotal.coerceAtMost(elapsedSinceMidnight)

        val topEntry = appUsageMap.maxByOrNull { it.value }
        val topPkg = topEntry?.key.orEmpty()
        val maxTime = topEntry?.value ?: 0L

        val topName = if (topPkg.isNotEmpty()) {
            runCatching {
                val appInfo = pm.getApplicationInfo(topPkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            }.getOrDefault(topPkg.substringAfterLast("."))
        } else ""

        val peakSlot = (slotDurations.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
        val fractions = if (slotDurations.any { it > 0 }) {
            slotDurations.map { (it.toFloat() / peakSlot).coerceIn(0.12f, 1f) }
        } else {
            val peak = (topEntry?.value ?: 1L).coerceAtLeast(1L).toFloat()
            val topApps = appUsageMap.entries.sortedByDescending { it.value }.take(6)
            if (topApps.size >= 4) {
                val base = topApps.map { (it.value.toFloat() / peak).coerceIn(0.2f, 1f) }
                (base + listOf(0.4f, 0.7f, 0.3f, 0.6f)).take(6)
            } else {
                listOf(0.25f, 0.65f, 0.85f, 0.45f, 0.95f, 0.35f)
            }
        }

        return ScreenTimeStats(
            totalMillis = totalTime,
            topAppName = topName,
            topAppMillis = maxTime,
            hasPermission = true,
            hourlyFractions = fractions,
            counterRangeText = "00:00 – Present"
        )
    }

    fun openSettings(context: Context) {
        val candidates = listOf(
            context.packageManager.getLaunchIntentForPackage("com.google.android.apps.wellbeing")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.google.android.apps.wellbeing", "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.google.android.apps.wellbeing", "com.google.android.apps.wellbeing.settings.LauncherActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.google.android.apps.wellbeing", "com.google.android.apps.wellbeing.dashboard.DashboardSettingsActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent("android.settings.DIGITAL_WELLBEING").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent("com.samsung.android.forest.ACTION_DIGITAL_WELLBEING").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        ).filterNotNull()

        for (intent in candidates) {
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }
}

@Composable
fun HydrationWidget(modifier: Modifier) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val today by rememberTicker(10_000) { LocalDate.now(zone) }
    val todayKey = "hydration_$today"
    val prefs = remember { context.getSharedPreferences("hydration_data", Context.MODE_PRIVATE) }
    var currentMl by remember(todayKey) { mutableIntStateOf(prefs.getInt(todayKey, 0)) }
    val goalMl = 2000

    val streak = remember(currentMl, today) {
        var count = if (currentMl >= goalMl) 1 else 0
        var d = today.minusDays(1)
        while (true) {
            val amount = prefs.getInt("hydration_$d", 0)
            if (amount >= goalMl) {
                count++
                d = d.minusDays(1)
            } else {
                break
            }
        }
        count
    }
    val streakText = when {
        streak > 0 -> "🔥 ${streak}d Streak"
        currentMl > 0 -> "💧 Active Today"
        else -> "💧 0d Streak"
    }

    fun addWater(amount: Int) {
        val next = (currentMl + amount).coerceAtMost(5000)
        currentMl = next
        prefs.edit().putInt(todayKey, next).apply()
        AppleTaptic.selection(context)
    }

    WidgetSurface(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isWide = maxWidth > 200.dp
            val progress = (currentMl.toFloat() / goalMl).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(progress, tween(400), label = "hydroProgress")

            if (isWide) {
                // 2x4 (Medium) Layout
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Progress Dial
                    Box(
                        Modifier
                            .size(110.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()
                            val arcRadius = (size.minDimension - strokeWidth) / 2
                            val center = Offset(size.width / 2, size.height / 2)

                            // Track
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = arcRadius,
                                center = center,
                                style = Stroke(strokeWidth)
                            )

                            // Progress
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(Color(0xFF00C7BE), Color(0xFF0A84FF), Color(0xFF00C7BE))
                                ),
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                                size = Size(arcRadius * 2, arcRadius * 2)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Color(0xFF00C7BE), modifier = Modifier.size(24.dp))
                            Text("${(progress * 100).toInt()}%", style = PanelBodyStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // Right: Info & Quick Action Pills
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("HYDRATION", style = WidgetCaptionStyle.copy(color = Color(0xFF00C7BE), fontSize = 11.sp))
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    Modifier
                                        .clip(Capsule)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(streakText, style = PanelCaptionStyle.copy(fontSize = 10.sp, color = AccentOrange))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$currentMl / $goalMl ml",
                                style = LargeTitleStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "${currentMl / 250} of 8 glasses drank today",
                                style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
                            )
                        }

                        // Quick Log Pills
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .clip(Capsule)
                                    .background(Color(0xFF0A84FF).copy(alpha = 0.25f))
                                    .clickable { addWater(250) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+250 ml", style = PanelBodyStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64D2FF)))
                            }
                            Box(
                                Modifier
                                    .clip(Capsule)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .clickable { addWater(500) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+500 ml", style = PanelBodyStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }
            } else {
                // 2x2 (Small) Layout
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF00C7BE).copy(alpha = 0.22f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Color(0xFF00C7BE), modifier = Modifier.size(15.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("WATER", style = WidgetCaptionStyle.copy(color = Color(0xFF00C7BE), fontSize = 11.sp))
                        }
                        Text("${(progress * 100).toInt()}%", style = PanelCaptionStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }

                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text("$currentMl ml", style = LargeTitleStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                        Text(if (streak > 0) "Goal: $goalMl ml • 🔥 ${streak}d" else "Goal: $goalMl ml", style = PanelCaptionStyle.copy(fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)))
                    }

                    // Bottom: Tap to log button
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(Capsule)
                            .background(Color(0xFF0A84FF).copy(alpha = 0.28f))
                            .clickable { addWater(250) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF64D2FF), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Log 250ml", style = PanelBodyStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64D2FF)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenTimeWidget(modifier: Modifier) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf(ScreenTimeHelper.queryStats(context)) }

    LifecycleResumeEffect(context) {
        stats = ScreenTimeHelper.queryStats(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            stats = ScreenTimeHelper.queryStats(context)
        }
    }

    WidgetSurface(
        modifier.clickable {
            ScreenTimeHelper.openSettings(context)
        }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isWide = maxWidth > 200.dp

            if (isWide) {
                // 2x4 (Medium) Layout
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier.width(134.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(24.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = Color(0xFFDDD6FE), modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("SCREEN TIME", style = WidgetCaptionStyle.copy(color = Color(0xFFDDD6FE), fontWeight = FontWeight.Bold, fontSize = 11.sp))
                        }
                        Column {
                            Text(stats.formattedTotal, style = LargeTitleStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                            val statusText = if (stats.hasPermission) "00:00 AM – Present" else "Tap to Grant Access"
                            val statusColor = if (stats.hasPermission) AccentGreen else AccentOrange
                            Text(statusText, style = PanelCaptionStyle.copy(fontSize = 11.sp, color = statusColor))
                        }
                        Text(stats.formattedTopApp, style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f)), maxLines = 1)
                    }

                    Spacer(Modifier.width(10.dp))

                    // Right: Hourly Bar Chart
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val labels = listOf("6AM", "9AM", "12PM", "3PM", "6PM", "9PM")
                            val barColors = listOf(Color(0xFF007AFF), Color(0xFFFF9500), Color(0xFF007AFF), Color(0xFFAF52DE), Color(0xFF007AFF), Color(0xFFFF9500))
                            stats.hourlyFractions.take(6).forEachIndexed { i, fraction ->
                                val barColor = barColors.getOrElse(i) { Color(0xFF007AFF) }
                                val label = labels.getOrElse(i) { "" }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .width(12.dp)
                                            .height(55.dp * fraction)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(barColor)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, style = PanelCaptionStyle.copy(fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f)))
                                }
                            }
                        }

                        // Category Legend
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(Color(0xFF007AFF), CircleShape))
                                Spacer(Modifier.width(3.dp))
                                Text("Social", style = PanelCaptionStyle.copy(fontSize = 10.sp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(Color(0xFFFF9500), CircleShape))
                                Spacer(Modifier.width(3.dp))
                                Text("Entertainment", style = PanelCaptionStyle.copy(fontSize = 10.sp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(Color(0xFFAF52DE), CircleShape))
                                Spacer(Modifier.width(3.dp))
                                Text("Productivity", style = PanelCaptionStyle.copy(fontSize = 10.sp))
                            }
                        }
                    }
                }
            } else {
                // 2x2 (Small) Layout
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(24.dp).background(Color(0xFF8B5CF6).copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = Color(0xFFDDD6FE), modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("SCREEN TIME", style = WidgetCaptionStyle.copy(color = Color(0xFFDDD6FE), fontWeight = FontWeight.Bold, fontSize = 11.sp))
                        }
                    }

                    Column {
                        Text(stats.formattedTotal, style = LargeTitleStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                        val statusText = if (stats.hasPermission) "00:00 – Present" else "Tap to sync"
                        val statusColor = if (stats.hasPermission) AccentGreen else Color(0xFF64D2FF)
                        Text(statusText, style = PanelCaptionStyle.copy(fontSize = 11.sp, color = statusColor))
                    }

                    // Mini 4-Bar chart
                    Row(
                        Modifier.fillMaxWidth().height(36.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val miniColors = listOf(Color(0xFF007AFF), Color(0xFFFF9500), Color(0xFFAF52DE), Color(0xFF007AFF))
                        stats.hourlyFractions.take(4).forEachIndexed { i, fraction ->
                            val barColor = miniColors.getOrElse(i) { Color(0xFF007AFF) }
                            Box(
                                Modifier
                                    .width(18.dp)
                                    .height(36.dp * fraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(barColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
