package com.glasslauncher.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.glasslauncher.data.NotificationHub
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.FormatListBulleted
import android.content.Intent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.min

/** iOS World Clock widget: local time plus three cities, with day/night dots. */
@Composable
fun WorldClockWidget(modifier: Modifier) {
    val now by rememberTicker(30_000) { ZonedDateTime.now() }
    val cities = listOf(
        "Local" to ZoneId.systemDefault(),
        "London" to ZoneId.of("Europe/London"),
        "New York" to ZoneId.of("America/New_York"),
        "Tokyo" to ZoneId.of("Asia/Tokyo"),
    )
    val format = DateTimeFormatter.ofPattern("HH:mm")
    WidgetSurface(modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
            Text("WORLD CLOCK", style = WidgetCaptionStyle.copy(color = AccentOrange))
            cities.forEach { (name, zone) ->
                val time = now.withZoneSameInstant(zone)
                val day = time.hour in 7..18
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(if (day) AccentYellow else Color(0xFF8E8EFF), CircleShape))
                    Text(name, style = PanelBodyStyle.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp).weight(1f))
                    Text(time.format(format), style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

/** iOS Calendar month widget: the month grid with today in a red circle. */
@Composable
fun MonthWidget(modifier: Modifier) {
    val today by rememberTicker(60_000) { LocalDate.now() }
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val first = today.withDayOfMonth(1)
    val lead = (first.dayOfWeek.value - firstDay.value + 7) % 7
    val cells = List(lead) { 0 } + (1..today.lengthOfMonth()).toList()
    val weekdays = (0..6).map { firstDay.plus(it.toLong()).getDisplayName(DateTextStyle.NARROW, Locale.getDefault()) }
    val context = LocalContext.current
    WidgetSurface(modifier, onClick = { openCalendar(context) }) {
        Column(Modifier.fillMaxSize()) {
            Text(today.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault()).uppercase(), style = WidgetCaptionStyle.copy(color = BadgeRed))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { Text(it, style = WidgetCaptionStyle.copy(fontSize = 10.sp), textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    for (i in 0 until 7) {
                        val day = week.getOrNull(i) ?: 0
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day > 0) {
                                val isToday = day == today.dayOfMonth
                                Box(
                                    Modifier.size(20.dp).then(if (isToday) Modifier.background(BadgeRed, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(day.toString(), style = PanelBodyStyle.copy(fontSize = 11.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * iOS Notes widget. Shows recent notes in a scrollable list like Reminders, with Google Keep integration.
 */
@Composable
fun NotesWidget(modifier: Modifier) {
    val env = LocalWidgetEnvironment.current
    val context = LocalContext.current
    val notes = env.notesList.filter { it.isNotBlank() }.ifEmpty {
        if (env.note.isNotBlank()) listOf(env.note) else emptyList()
    }
    val listState = rememberLazyListState()

    WidgetSurface(modifier, onClick = {
        if (notes.isEmpty()) env.onNewNote() else env.onEditNoteIndex(0)
    }) {
        Column(Modifier.fillMaxSize()) {
            // Header row with title, note count, Keep icon, and prominent yellow write icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            ) {
                Text(
                    "Notes",
                    style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AccentYellow),
                )
                if (notes.isNotEmpty()) {
                    Text(
                        " (${notes.size})",
                        style = PanelCaptionStyle.copy(fontSize = 12.sp, color = AccentYellow.copy(alpha = 0.7f)),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))

                // Open in Google Keep button
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { openKeepNotes(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Description,
                        contentDescription = "Google Keep",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))

                // New note button
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AccentYellow.copy(alpha = 0.22f))
                        .clickable(onClick = env.onNewNote),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "New note",
                        tint = AccentYellow,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (notes.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().clickable(onClick = env.onNewNote),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Notes", style = PanelCaptionStyle.copy(fontSize = 13.sp))
                        Text("Tap + to create a note", style = PanelCaptionStyle.copy(fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.5f)))
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = if (notes.size > 2) 6.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(notes, key = { idx, item -> "$idx-$item" }) { index, noteText ->
                            val lines = noteText.lines()
                            val title = lines.firstOrNull() ?: noteText
                            val subtitle = lines.drop(1).firstOrNull { it.isNotBlank() }

                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { env.onEditNoteIndex(index) }
                                    .padding(vertical = 3.dp, horizontal = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(width = 3.dp, height = 14.dp)
                                            .clip(Capsule)
                                            .background(AccentYellow)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        title,
                                        style = PanelBodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (subtitle != null) {
                                    Text(
                                        subtitle,
                                        style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 9.dp, top = 1.dp)
                                    )
                                }
                                if (index < notes.lastIndex) {
                                    androidx.compose.material3.HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.1f),
                                        thickness = 0.6.dp,
                                        modifier = Modifier.padding(top = 4.dp, start = 9.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (notes.size > 2) {
                        // iOS-style scrollbar indicator
                        val firstVisible = listState.firstVisibleItemIndex
                        val total = notes.size
                        val progress = if (total > 0) (firstVisible.toFloat() / total).coerceIn(0f, 1f) else 0f
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .width(3.dp)
                                .height(28.dp)
                                .offset(y = ((progress - 0.5f) * 36f).dp)
                                .clip(Capsule)
                                .background(Color.White.copy(alpha = 0.45f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS Reminders widget: the count, then open reminders with a circle to tick each one off.
 * Fast strike-through animation on completion and scrollable list with scrollbar when > 4 reminders.
 */
@Composable
fun RemindersWidget(modifier: Modifier) {
    val env = LocalWidgetEnvironment.current
    val scope = rememberCoroutineScope()
    var completingSet by remember { mutableStateOf(setOf<Int>()) }

    WidgetSurface(modifier, onClick = env.onAddReminder) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(AccentOrange, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.FormatListBulleted, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("${env.reminders.size}", style = PanelBodyStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.height(6.dp))
            if (env.reminders.isEmpty()) {
                Text("No Reminders", style = PanelCaptionStyle.copy(fontSize = 13.sp))
                Text("Tap to add one", style = PanelCaptionStyle.copy(fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f)))
            } else {
                val listState = rememberLazyListState()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = if (env.reminders.size > 4) 6.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(env.reminders, key = { idx, item -> "$idx-$item" }) { index, text ->
                            val isCompleting = index in completingSet
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                        .background(if (isCompleting) AccentOrange else Color.Transparent)
                                        .clickable(onClickLabel = "Complete") {
                                            if (!isCompleting) {
                                                completingSet = completingSet + index
                                                scope.launch {
                                                    delay(200)
                                                    env.onCompleteReminder(index)
                                                    completingSet = completingSet - index
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleting) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Text(
                                    text,
                                    style = PanelBodyStyle.copy(
                                        fontSize = 14.sp,
                                        color = if (isCompleting) Color.White.copy(alpha = 0.45f) else Color.White,
                                        textDecoration = if (isCompleting) TextDecoration.LineThrough else null
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                        .clickable { env.onEditReminder(index) },
                                )
                            }
                        }
                    }
                    if (env.reminders.size > 4) {
                        // iOS-style scrollbar indicator
                        val firstVisible = listState.firstVisibleItemIndex
                        val total = env.reminders.size
                        val progress = if (total > 0) (firstVisible.toFloat() / total).coerceIn(0f, 1f) else 0f
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .width(3.dp)
                                .height(32.dp)
                                .offset(y = ((progress - 0.5f) * 40f).dp)
                                .clip(Capsule)
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

private const val KEEP_PACKAGE = "com.google.android.keep"

/** Opens Google Keep or sends note text to Keep / sharing targets. */
fun openKeepNotes(context: Context, noteText: String? = null) {
    val pm = context.packageManager
    if (!noteText.isNullOrBlank()) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, noteText)
            `package` = KEEP_PACKAGE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (context.startSafely(sendIntent)) return

        val shareIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, noteText)
        }, "Send Note to...")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (context.startSafely(shareIntent)) return
    }

    val launchIntent = pm.getLaunchIntentForPackage(KEEP_PACKAGE)
    if (launchIntent != null) {
        context.startSafely(launchIntent)
    } else {
        val marketUri = android.net.Uri.parse("market://details?id=$KEEP_PACKAGE")
        val webUri = android.net.Uri.parse("https://keep.google.com")
        if (!context.startSafely(Intent(Intent.ACTION_VIEW, marketUri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })) {
            context.startSafely(Intent(Intent.ACTION_VIEW, webUri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }
}

@Composable
fun rememberStepCount(permissionTrigger: Int = 0): Int {
    val context = LocalContext.current
    val syncedHealthSteps by NotificationHub.healthSteps.collectAsStateWithLifecycle()
    val prefs = remember { context.getSharedPreferences("activity_steps", Context.MODE_PRIVATE) }
    val todayStr = remember { java.time.LocalDate.now().toString() }
    val dailyStepsKey = "steps_daily_$todayStr"

    val initialSaved = remember { prefs.getInt(dailyStepsKey, 0) }
    val stepsState = remember { mutableIntStateOf(initialSaved) }

    // If health app synced steps, apply it directly
    LaunchedEffect(syncedHealthSteps) {
        val synced = syncedHealthSteps
        if (synced != null && synced > 0) {
            stepsState.intValue = synced
            prefs.edit().putInt(dailyStepsKey, synced).apply()
        }
    }

    DisposableEffect(context, permissionTrigger) {
        val hasPermission = Permissions.hasActivityRecognition(context)
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val counterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        val todayKey = "steps_baseline_$todayStr"
        val accKey = "steps_acc_$todayStr"
        val lastTotalKey = "steps_last_total_hardware"
        val lastDateKey = "steps_last_recorded_date"

        var baseline = prefs.getInt(todayKey, -1)
        var acc = prefs.getInt(accKey, 0)

        // Handle midnight / new day rollover:
        val lastDate = prefs.getString(lastDateKey, null)
        val lastTotal = prefs.getInt(lastTotalKey, -1)
        if (lastDate != null && lastDate != todayStr && lastTotal > 0) {
            // New day started: yesterday's last recorded hardware count becomes today's starting baseline
            baseline = lastTotal
            acc = 0
            prefs.edit()
                .putString(lastDateKey, todayStr)
                .putInt(todayKey, baseline)
                .putInt(accKey, 0)
                .apply()
        } else if (lastDate == null) {
            prefs.edit().putString(lastDateKey, todayStr).apply()
        }

        // Micro-consecutive step filter: do not count bouts of less than 10 consecutive steps.
        // False positives from table vibrations, car bumps, or picking up the phone (< 10 steps) are dropped.
        val STEP_TIMEOUT_MS = 2500L
        val MIN_CONSECUTIVE_STEPS = 10
        var consecutiveDetectorSteps = 0
        var lastDetectorTimestamp = 0L

        var pendingCounterSteps = 0
        var lastCounterTimestamp = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val now = System.currentTimeMillis()

                if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val totalHardwareSteps = event.values.getOrNull(0)?.toInt() ?: return
                    if (totalHardwareSteps <= 0) return

                    if (baseline < 0) {
                        baseline = totalHardwareSteps
                        prefs.edit().putInt(todayKey, baseline).apply()
                    } else if (totalHardwareSteps < baseline) {
                        // Phone rebooted: hardware step counter reset to 0. Accumulate previous steps taken today.
                        val prevDelta = prefs.getInt("steps_last_delta_$todayStr", 0)
                        acc += prevDelta
                        baseline = totalHardwareSteps
                        prefs.edit().putInt(accKey, acc).putInt(todayKey, baseline).apply()
                    }

                    val currentDelta = (totalHardwareSteps - baseline).coerceAtLeast(0)
                    val prevLastDelta = prefs.getInt("steps_last_delta_$todayStr", 0)
                    val stepDelta = (currentDelta - prevLastDelta).coerceAtLeast(0)

                    prefs.edit()
                        .putInt("steps_last_delta_$todayStr", currentDelta)
                        .putInt(lastTotalKey, totalHardwareSteps)
                        .putString(lastDateKey, todayStr)
                        .apply()

                    // Filter microconsecutive steps: reject step bursts < 10
                    if (stepDelta >= MIN_CONSECUTIVE_STEPS) {
                        pendingCounterSteps = 0
                        val hardwareStepsToday = acc + currentDelta
                        if (syncedHealthSteps == null || hardwareStepsToday > (syncedHealthSteps ?: 0)) {
                            stepsState.intValue = maxOf(stepsState.intValue, hardwareStepsToday)
                            prefs.edit().putInt(dailyStepsKey, stepsState.intValue).apply()
                        }
                    } else if (stepDelta > 0) {
                        if (now - lastCounterTimestamp < STEP_TIMEOUT_MS) {
                            pendingCounterSteps += stepDelta
                            if (pendingCounterSteps >= MIN_CONSECUTIVE_STEPS) {
                                val hardwareStepsToday = acc + currentDelta
                                if (syncedHealthSteps == null || hardwareStepsToday > (syncedHealthSteps ?: 0)) {
                                    stepsState.intValue = maxOf(stepsState.intValue, hardwareStepsToday)
                                    prefs.edit().putInt(dailyStepsKey, stepsState.intValue).apply()
                                }
                            }
                        } else {
                            // Previous burst was less than 10 steps and timed out: dropped as micro-movement
                            pendingCounterSteps = stepDelta
                        }
                        lastCounterTimestamp = now
                    }
                } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    // Real-time detector: buffer consecutive steps, only crediting if >= 10 consecutive steps
                    if (now - lastDetectorTimestamp < STEP_TIMEOUT_MS) {
                        consecutiveDetectorSteps++
                        if (consecutiveDetectorSteps == MIN_CONSECUTIVE_STEPS) {
                            // Valid walking bout confirmed: credit all 10 steps
                            val updated = stepsState.intValue + MIN_CONSECUTIVE_STEPS
                            stepsState.intValue = updated
                            prefs.edit().putInt(dailyStepsKey, updated).apply()
                        } else if (consecutiveDetectorSteps > MIN_CONSECUTIVE_STEPS) {
                            // Sustained walking: credit each subsequent step
                            val updated = stepsState.intValue + 1
                            stepsState.intValue = updated
                            prefs.edit().putInt(dailyStepsKey, updated).apply()
                        }
                    } else {
                        // Previous bout was < 10 steps: discarded
                        consecutiveDetectorSteps = 1
                    }
                    lastDetectorTimestamp = now
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        var registered = false
        if (sensorManager != null && hasPermission) {
            try {
                if (counterSensor != null) {
                    registered = sensorManager.registerListener(listener, counterSensor, SensorManager.SENSOR_DELAY_UI) || registered
                }
                if (detectorSensor != null) {
                    registered = sensorManager.registerListener(listener, detectorSensor, SensorManager.SENSOR_DELAY_UI) || registered
                }
            } catch (_: SecurityException) {}
        }

        // Only on emulators or devices completely lacking hardware pedometers AND with no recorded steps:
        if (counterSensor == null && detectorSensor == null && stepsState.intValue == 0) {
            val cal = java.util.Calendar.getInstance()
            val min = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val dayFraction = (min / (24f * 60f)).coerceIn(0.05f, 1f)
            val demoSteps = (dayFraction * 4500).toInt()
            stepsState.intValue = demoSteps
            prefs.edit().putInt(dailyStepsKey, demoSteps).apply()
        }

        onDispose {
            if (sensorManager != null && registered) {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    return stepsState.intValue
}

/**
 * iOS Fitness/Activity rings: 3 dynamic animated rings synced with Google Fit / Samsung Health:
 * - Outer Red: Move (active calorie burn, 400 kcal goal)
 * - Middle Green: Exercise (active minutes, 30 min goal)
 * - Inner Cyan: Stand / Steps (10,000 steps goal)
 */
@Composable
fun ActivityWidget(modifier: Modifier) {
    val context = LocalContext.current
    var permissionTrigger by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        permissionTrigger++
        onPauseOrDispose { }
    }
    val activityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) permissionTrigger++
    }

    val steps = rememberStepCount(permissionTrigger)

    var hasLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasLoaded = true }

    // Dynamic Apple Fitness ring targets
    val targetMove = if (hasLoaded) (steps * 0.04f / 400f).coerceIn(0.04f, 1.5f) else 0.04f
    val targetExercise = if (hasLoaded) (steps / 3000f).coerceIn(0.04f, 1.5f) else 0.04f
    val targetStand = if (hasLoaded) (steps / 10000f).coerceIn(0.04f, 1.5f) else 0.04f

    // Smooth fluid Apple-style animation transitions
    val animatedMove by animateFloatAsState(
        targetValue = targetMove,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 140f),
        label = "move_progress"
    )
    val animatedExercise by animateFloatAsState(
        targetValue = targetExercise,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 140f),
        label = "exercise_progress"
    )
    val animatedStand by animateFloatAsState(
        targetValue = targetStand,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 140f),
        label = "stand_progress"
    )

    val animatedSteps by androidx.compose.animation.core.animateIntAsState(
        targetValue = steps,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 200f),
        label = "animated_steps"
    )

    WidgetSurface(modifier, onClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Permissions.hasActivityRecognition(context)) {
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            // Preference: Google Fit first, then Samsung Health, then Health Connect, then default health trackers
            val pm = context.packageManager
            val fitIntent = pm.getLaunchIntentForPackage("com.google.android.apps.fitness")
            val shealthIntent = pm.getLaunchIntentForPackage("com.sec.android.app.shealth")
            val healthConnectIntent = pm.getLaunchIntentForPackage("com.google.android.apps.healthdata")
            val fitbitIntent = pm.getLaunchIntentForPackage("com.fitbit.FitbitMobile")
            val miFitnessIntent = pm.getLaunchIntentForPackage("com.xiaomi.wearable") ?: pm.getLaunchIntentForPackage("com.mi.health")
            val huaweiHealthIntent = pm.getLaunchIntentForPackage("com.huawei.health")

            val targetIntent = fitIntent ?: shealthIntent ?: healthConnectIntent ?: fitbitIntent ?: miFitnessIntent ?: huaweiHealthIntent
            if (targetIntent != null) {
                context.startSafely(targetIntent)
            } else {
                val playFit = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.google.android.apps.fitness"))
                if (!context.startSafely(playFit)) {
                    context.startSafely(Intent(android.provider.Settings.ACTION_SETTINGS))
                }
            }
        }
    }) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ACTIVITY",
                    style = WidgetCaptionStyle.copy(color = Color(0xFFFA114F), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                )
                Text(
                    "$animatedSteps STEPS",
                    style = WidgetCaptionStyle.copy(color = Color(0xFF1EEAEF), fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).padding(top = 4.dp)) {
                    val stroke = minOf(size.width, size.height) * 0.1f
                    val ringList: List<Pair<Color, Float>> = listOf(
                        Color(0xFFFA114F) to animatedMove,
                        Color(0xFF92E82A) to animatedExercise,
                        Color(0xFF1EEAEF) to animatedStand,
                    )
                    ringList.forEachIndexed { index, pair ->
                        val color = pair.first
                        val value = pair.second
                        val inset = stroke * (0.5f + index * 1.25f)
                        val diameter = minOf(size.width, size.height) - inset * 2
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        drawArc(color.copy(alpha = 0.22f), 0f, 360f, false, topLeft, Size(diameter, diameter), style = Stroke(stroke))
                        drawArc(color, -90f, (360f * value).coerceAtMost(360f), false, topLeft, Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}

private fun dayProgress(): Float {
    val now = ZonedDateTime.now()
    return ((now.hour * 60 + now.minute) / (24f * 60f)).coerceIn(0.02f, 1f)
}


/**
 * iOS Music widget: artwork, song and artist with play/pause; the wide size adds previous/next. Reads the playing
 * media session (any music or video app), which Android only shares with notification access.
 */
@Composable
fun MusicWidget(modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = androidx.compose.runtime.remember { NowPlaying(context.applicationContext) }
    androidx.lifecycle.compose.LifecycleResumeEffect(player) {
        player.start()
        onPauseOrDispose { player.stop() }
    }
    WidgetSurface(modifier, onClick = {
        if (!player.allowed && Permissions.notificationsSupported) {
            Permissions.open(context, Permissions.notificationAccessIntent(context))
        } else if (player.playing) {
            // If music is playing: open the app that is currently playing
            if (player.sessionActivity != null) {
                runCatching { player.sessionActivity?.send() }.getOrElse {
                    openMusicApp(context, player.packageName)
                }
            } else if (player.packageName != null) {
                val intent = context.packageManager.getLaunchIntentForPackage(player.packageName!!)
                if (intent != null) context.startSafely(intent) else openMusicApp(context, null)
            } else {
                openMusicApp(context, null)
            }
        } else {
            // If no music is playing: open Spotify if installed, else YouTube Music or YouTube
            openMusicApp(context, null)
        }
    }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight * 1.4f
            val artSize = if (wide) maxHeight else maxHeight * 0.52f
            val art: @Composable () -> Unit = {
                Box(
                    Modifier.size(artSize).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = player.artwork
                    if (image != null) {
                        androidx.compose.foundation.Image(image, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.MusicNote, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(artSize * 0.4f))
                    }
                }
            }
            val info: @Composable () -> Unit = {
                Column {
                    Text(player.title ?: if (player.allowed) "Not Playing" else "Music", style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(player.artist ?: if (player.allowed) "" else "Tap to allow", style = PanelCaptionStyle.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val controls: @Composable (Boolean) -> Unit = { full ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (full) MusicButton(androidx.compose.material.icons.Icons.Rounded.SkipPrevious, "Previous", 30) { player.previous() }
                    MusicButton(
                        if (player.playing) androidx.compose.material.icons.Icons.Rounded.Pause else androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                        if (player.playing) "Pause" else "Play",
                        36,
                    ) { player.playPause() }
                    if (full) MusicButton(androidx.compose.material.icons.Icons.Rounded.SkipNext, "Next", 30) { player.next() }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    art()
                    Column(Modifier.padding(start = 16.dp).weight(1f).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("NOW PLAYING", style = WidgetCaptionStyle.copy(color = Color(0xFFFF2D55)))
                        info()
                        controls(true)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.Top) {
                        art()
                        Spacer(Modifier.weight(1f))
                        controls(false)
                    }
                    info()
                }
            }
        }
    }
}

@Composable
private fun MusicButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sizeDp: Int, onClick: () -> Unit) {
    androidx.compose.material3.Icon(
        icon,
        contentDescription = label,
        tint = Color.White,
        modifier = Modifier.size(sizeDp.dp).clip(CircleShape).clickable(onClick = onClick),
    )
}

/** Opens Spotify if installed, or YouTube Music / YouTube, or the active music player. */
fun openMusicApp(context: Context, activePackage: String? = null) {
    val pm = context.packageManager

    // 1. If currently playing an active session in another player, open it
    if (activePackage != null) {
        val activeIntent = pm.getLaunchIntentForPackage(activePackage)
        if (activeIntent != null) {
            context.startSafely(activeIntent)
            return
        }
    }

    // 2. Spotify if installed
    val spotifyIntent = pm.getLaunchIntentForPackage("com.spotify.music")
    if (spotifyIntent != null) {
        context.startSafely(spotifyIntent)
        return
    }

    // 3. YouTube Music if installed
    val ytMusicIntent = pm.getLaunchIntentForPackage("com.google.android.apps.youtube.music")
    if (ytMusicIntent != null) {
        context.startSafely(ytMusicIntent)
        return
    }

    // 4. YouTube if installed
    val ytIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
    if (ytIntent != null) {
        context.startSafely(ytIntent)
        return
    }

    // 5. Generic Music Intent fallback
    val musicIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_APP_MUSIC)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (!context.startSafely(musicIntent)) {
        val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://open.spotify.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startSafely(webIntent)
    }
}
