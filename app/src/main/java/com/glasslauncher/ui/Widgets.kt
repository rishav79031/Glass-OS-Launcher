package com.glasslauncher.ui
import android.os.Environment
import android.os.StatFs
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.rounded.Storage

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.glasslauncher.data.AppEntry
import com.glasslauncher.data.IconStyle
import com.glasslauncher.data.WidgetKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The widgets the launcher draws itself (Android app widgets need a full widget host, which isn't "lite"). */
/** iOS widgets: continuous corners at about a sixth of the widget's shorter side. */
val WidgetShape = SmoothShape(fraction = 0.17f)

@Immutable
data class CalendarEvent(val title: String, val begin: Long, val allDay: Boolean, val color: Int)

@Composable
fun WidgetSurface(modifier: Modifier, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .liquidGlass(WidgetShape, style = GlassStyle.Regular)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun LauncherWidget(
    kind: WidgetKind,
    modifier: Modifier,
    suggestions: List<AppEntry> = emptyList(),
    icons: Map<String, ImageBitmap> = emptyMap(),
    onLaunch: (AppEntry, Rect) -> Unit = { _, _ -> },
) {
    when (kind) {
        WidgetKind.Weather -> WeatherWidget(modifier)
        WidgetKind.Photos -> PhotosWidget(modifier)
        WidgetKind.Clock -> ClockWidget(modifier)
        WidgetKind.Calendar -> CalendarWidget(modifier)
        WidgetKind.Batteries -> BatteryWidget(modifier)
        WidgetKind.UpNext -> UpNextWidget(modifier)
        WidgetKind.Suggestions -> SuggestionsWidget(suggestions, icons, onLaunch, modifier)
        WidgetKind.Music -> MusicWidget(modifier)
        WidgetKind.WorldClock -> WorldClockWidget(modifier)
        WidgetKind.Month -> MonthWidget(modifier)
        WidgetKind.Notes -> NotesWidget(modifier)
        WidgetKind.Activity -> ActivityWidget(modifier)
        WidgetKind.Reminders -> RemindersWidget(modifier)
        WidgetKind.Storage -> StorageWidget(modifier)
        WidgetKind.Hydration -> HydrationWidget(modifier)
        WidgetKind.ScreenTime -> ScreenTimeWidget(modifier)
    }
}

/** The iOS Clock widget: a white face with numerals, black hands and an orange second hand, set in glass. */
@Composable
fun ClockWidget(modifier: Modifier) {
    val context = LocalContext.current
    val now by rememberTicker(1_000) { LocalDateTime.now() }
    val measurer = rememberTextMeasurer()
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager }

    val nextAlarmInfo = remember(now.minute, now.hour) {
        try {
            alarmManager?.nextAlarmClock
        } catch (_: Exception) {
            null
        }
    }

    val nextAlarmText = remember(nextAlarmInfo) {
        if (nextAlarmInfo != null) {
            val triggerInstant = Instant.ofEpochMilli(nextAlarmInfo.triggerTime)
            val zone = ZoneId.systemDefault()
            val alarmZdt = triggerInstant.atZone(zone)
            val nowZdt = java.time.ZonedDateTime.now(zone)
            val timeStr = alarmZdt.format(DateTimeFormatter.ofPattern("h:mm a"))
            when {
                alarmZdt.toLocalDate() == nowZdt.toLocalDate() -> timeStr
                alarmZdt.toLocalDate() == nowZdt.toLocalDate().plusDays(1) -> "Tomorrow $timeStr"
                else -> alarmZdt.format(DateTimeFormatter.ofPattern("EEE h:mm a"))
            }
        } else null
    }

    fun openClock() {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val clockIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.deskclock")
            ?: context.packageManager.getLaunchIntentForPackage("com.sec.android.app.clockpackage")
            ?: context.packageManager.getLaunchIntentForPackage("com.android.deskclock")
            ?: context.packageManager.getLaunchIntentForPackage("com.oneplus.deskclock")
        if (!context.startSafely(intent)) {
            if (clockIntent != null) context.startSafely(clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Box(
        modifier
            .liquidGlass(WidgetShape, style = GlassStyle.Regular)
            .clickable(onClick = ::openClock)
            .padding(10.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isWide = maxWidth > maxHeight * 1.35f
            if (isWide) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left: Analog Clock
                    Box(
                        Modifier
                            .size(this@BoxWithConstraints.maxHeight - 4.dp)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnalogClockFace(now, measurer)
                    }

                    // Right: Clean Cupertino / Local City Time & Date
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val cityName = remember {
                            val id = ZoneId.systemDefault().id.substringAfterLast("/").replace("_", " ").uppercase()
                            if (id.isNotBlank()) id else "LOCAL"
                        }
                        val zoneOffsetStr = remember {
                            val totalSeconds = ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds
                            val hours = totalSeconds / 3600
                            val minutes = Math.abs((totalSeconds % 3600) / 60)
                            val sign = if (totalSeconds >= 0) "+" else "-"
                            if (minutes == 0) "$sign${Math.abs(hours)}HRS" else "$sign${Math.abs(hours)}:${minutes.toString().padStart(2, '0')}"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                cityName,
                                style = WidgetCaptionStyle.copy(
                                    color = AccentOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                zoneOffsetStr,
                                style = PanelCaptionStyle.copy(
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        val hourMinute = remember(now.minute, now.hour) {
                            now.format(DateTimeFormatter.ofPattern("h:mm"))
                        }
                        val amPm = remember(now.hour) {
                            now.format(DateTimeFormatter.ofPattern("a"))
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                hourMinute,
                                style = LargeTitleStyle.copy(
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp
                                )
                            )
                            Text(
                                amPm,
                                style = PanelBodyStyle.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.75f)
                                ),
                                modifier = Modifier.padding(bottom = 5.dp, start = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(3.dp))

                        val dateStr = remember(now.dayOfMonth, now.month, now.dayOfWeek) {
                            now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                        }
                        Text(
                            dateStr,
                            style = PanelCaptionStyle.copy(
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                AnalogClockFace(now, measurer)
            }
        }
    }
}

@Composable
private fun AnalogClockFace(now: LocalDateTime, measurer: androidx.compose.ui.text.TextMeasurer) {
    val tuning = LocalGlassTuning.current
    val isDark = tuning.iconStyle == IconStyle.Dark
    val isTinted = tuning.iconStyle == IconStyle.Tinted
    val tintColor = Color(tuning.tintColor)

    val dialColor = when {
        isDark -> Color(0xFF1C1C1E)
        isTinted -> Color(0xFF16161A)
        else -> Color.White.copy(alpha = 0.94f)
    }
    val markColor = when {
        isDark -> Color.White
        isTinted -> tintColor
        else -> Color.Black
    }
    val handColor = when {
        isDark -> Color.White
        isTinted -> tintColor
        else -> Color.Black
    }
    val centerRim = when {
        isDark -> Color(0xFF2C2C2E)
        isTinted -> tintColor
        else -> Color.White
    }

    Canvas(Modifier.fillMaxSize()) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(dialColor, radius, center)
        if (isDark || isTinted) {
            drawCircle(
                color = if (isTinted) tintColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        for (i in 0 until 60) {
            val major = i % 5 == 0
            val angle = i * 6f * PI.toFloat() / 180f
            val outer = radius * 0.95f
            val inner = radius * (if (major) 0.87f else 0.91f)
            drawLine(
                markColor.copy(alpha = if (major) 0.85f else 0.35f),
                Offset(center.x + sin(angle) * inner, center.y - cos(angle) * inner),
                Offset(center.x + sin(angle) * outer, center.y - cos(angle) * outer),
                strokeWidth = if (major) radius * 0.022f else radius * 0.012f,
            )
        }
        val numeral = TextStyle(fontFamily = AppFont, fontSize = (radius * 0.24f).toSp(), fontWeight = FontWeight.Medium, color = markColor)
        for (hour in 1..12) {
            val angle = hour * 30f * PI.toFloat() / 180f
            val layout = measurer.measure(hour.toString(), numeral)
            val at = Offset(center.x + sin(angle) * radius * 0.7f, center.y - cos(angle) * radius * 0.7f)
            drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
        }
        val minutes = now.minute + now.second / 60f
        val hours = (now.hour % 12) + minutes / 60f
        hand(center, hours * 30f, radius * 0.5f, radius * 0.07f, handColor)
        hand(center, minutes * 6f, radius * 0.78f, radius * 0.05f, handColor)
        drawCircle(handColor, radius * 0.06f, center)
        hand(center, now.second * 6f, radius * 0.86f, radius * 0.016f, AccentOrange)
        drawCircle(AccentOrange, radius * 0.035f, center)
        drawCircle(centerRim, radius * 0.014f, center)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.hand(center: Offset, degrees: Float, length: Float, width: Float, color: Color) {
    rotate(degrees, center) {
        drawLine(color, Offset(center.x, center.y + length * 0.12f), Offset(center.x, center.y - length), strokeWidth = width, cap = StrokeCap.Round)
    }
}

/** Day and date in the iOS Calendar widget layout, with the next event when calendar access is allowed. */
@Composable
fun CalendarWidget(modifier: Modifier) {
    val today = rememberToday()
    val events = rememberUpcomingEvents()
    val context = LocalContext.current
    val env = LocalWidgetEnvironment.current
    WidgetSurface(modifier, onClick = { if (env.calendarAllowed) openCalendar(context) else env.onRequestCalendar() }) {
        // Bigger widgets (iPad) list more of the day, like the iOS Calendar widget.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight * 1.6f
            val shown = if (maxHeight > 220.dp) 3 else if (wide) 2 else 1
            val dayName = @Composable {
                Text(
                    today.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale.getDefault()).uppercase(),
                    style = WidgetCaptionStyle.copy(color = BadgeRed, fontSize = 13.sp, letterSpacing = 0.4.sp),
                    maxLines = 1,
                )
            }
            val dayNumber = @Composable {
                Text(today.dayOfMonth.toString(), style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Normal, letterSpacing = (-1).sp), maxLines = 1)
            }
            val agenda = @Composable { modifier: Modifier ->
                Box(modifier, contentAlignment = Alignment.BottomStart) {
                    when {
                        !env.calendarAllowed -> Text("Tap to show your events", style = PanelCaptionStyle.copy(fontSize = 13.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        events.isEmpty() -> Text("No more events today", style = PanelCaptionStyle.copy(fontSize = 13.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { events.take(shown).forEach { EventLine(it) } }
                    }
                }
            }
            if (wide) {
                // Medium: the date on the left, the day's events on the right, as on iOS.
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxHeight()) {
                        dayName()
                        dayNumber()
                    }
                    Spacer(Modifier.width(16.dp))
                    agenda(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                // The agenda takes only the room left under the date, so the two never overlap.
                Column(Modifier.fillMaxSize()) {
                    dayName()
                    dayNumber()
                    agenda(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp).clipToBounds())
                }
            }
        }
    }
}

/** Opens Google Calendar when it's installed (events sync from there), otherwise any calendar app. */
internal fun openCalendar(context: Context) {
    // Opens the calendar app normally, so it shows the view it was last left in (Month, once chosen there). Opening
    // a specific date would force the day's events view instead.
    for (pkg in listOf("com.google.android.calendar", "com.samsung.android.calendar")) {
        val app = context.packageManager.getLaunchIntentForPackage(pkg)
        if (app != null && context.startSafely(app)) return
    }
    val today = android.content.ContentUris.appendId(CalendarContract.CONTENT_URI.buildUpon().appendPath("time"), System.currentTimeMillis()).build()
    context.startSafely(Intent(Intent.ACTION_VIEW, today))
}

/** Medium widget: the next few events. */
@Composable
fun UpNextWidget(modifier: Modifier) {
    val events = rememberUpcomingEvents()
    val today = rememberToday()
    val context = LocalContext.current
    WidgetSurface(modifier, onClick = { openCalendar(context) }) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("UP NEXT", style = WidgetCaptionStyle.copy(color = BadgeRed), modifier = Modifier.weight(1f))
                Text(today.format(DateTimeFormatter.ofPattern("EEE d MMM")), style = WidgetCaptionStyle)
            }
            if (events.isEmpty()) {
                Text("No upcoming events", style = PanelCaptionStyle)
            }
            events.take(3).forEach { EventLine(it) }
        }
    }
}

@Composable
private fun EventLine(event: CalendarEvent) {
    val time = if (event.allDay) {
        "All day"
    } else {
        Instant.ofEpochMilli(event.begin).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    Row(Modifier.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(30.dp).background(Color(event.color or 0xFF000000.toInt()), RoundedCornerShape(2.dp)))
        Column(Modifier.padding(start = 10.dp)) {
            Text(event.title, style = PanelBodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(time, style = PanelCaptionStyle.copy(fontSize = 12.sp), maxLines = 1)
        }
    }
}

/** iOS Batteries widget: a ring with the level, bolt while charging. */
@Composable
fun BatteryWidget(modifier: Modifier) {
    val battery = rememberBattery()
    val color = when {
        battery.charging -> AccentGreen
        battery.percent <= 20 -> DestructiveRed
        else -> AccentGreen
    }
    WidgetSurface(modifier) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxHeight().fillMaxWidth()) {
                    val stroke = min(size.width, size.height) * 0.11f
                    val diameter = min(size.width, size.height) - stroke
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
                    drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                    drawArc(color, -90f, 360f * battery.percent / 100f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                if (battery.charging) Icon(Icons.Rounded.Bolt, contentDescription = "Charging", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text("${battery.percent}%", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** Most-used apps, matching the Search pebble suggestions card with compact height, 56dp icons and identical styling. */
@Composable
fun SuggestionsWidget(
    apps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    onLaunch: (AppEntry, Rect) -> Unit,
    modifier: Modifier,
    iconSize: Dp = 56.dp
) {
    BoxWithConstraints(modifier) {
        val isLarge = maxHeight > 190.dp
        val isSmall = maxWidth < 190.dp
        val columns = if (isSmall) 2 else 4
        val rows = if (isLarge) 2 else 1
        val shown = apps.take(columns * rows)

        // Matches the Search pebble overlay suggestions card: compact height (~128dp), 26dp liquid glass, 16dp padding
        Box(
            Modifier
                .fillMaxWidth()
                .then(if (!isLarge) Modifier.wrapContentHeight().align(Alignment.Center) else Modifier.fillMaxSize())
                .liquidGlass(SmoothShape(26.dp), 26.dp, GlassStyle.Regular)
                .padding(16.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top
            ) {
                // Header: "SUGGESTIONS" with 16dp top padding matching Search overlay
                Text("SUGGESTIONS", style = WidgetCaptionStyle)

                Spacer(Modifier.height(12.dp))

                if (rows == 1) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until columns) {
                            val app = shown.getOrNull(c)
                            if (app != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable { onLaunch(app, Rect.Zero) }
                                ) {
                                    icons[app.key]?.let {
                                        androidx.compose.foundation.Image(
                                            bitmap = it,
                                            contentDescription = app.label,
                                            modifier = Modifier.size(if (isSmall) 44.dp else iconSize)
                                        )
                                    }
                                    Text(
                                        app.label,
                                        style = IconLabelStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(72.dp))
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (r in 0 until rows) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (c in 0 until columns) {
                                    val slot = r * columns + c
                                    val app = shown.getOrNull(slot)
                                    if (app != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(if (isSmall) 64.dp else 72.dp)
                                                .clickable { onLaunch(app, Rect.Zero) }
                                        ) {
                                            icons[app.key]?.let {
                                                androidx.compose.foundation.Image(
                                                    bitmap = it,
                                                    contentDescription = app.label,
                                                    modifier = Modifier.size(if (isSmall) 44.dp else iconSize)
                                                )
                                            }
                                            Text(
                                                app.label,
                                                style = IconLabelStyle,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.width(if (isSmall) 64.dp else 72.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Immutable
private data class BatteryState(val percent: Int, val charging: Boolean)

@Composable
private fun rememberBattery(): BatteryState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readBattery(context)) }
    LifecycleResumeEffect(Unit) {
        state = readBattery(context)
        onPauseOrDispose { }
    }
    // Refresh every minute while visible; the sticky broadcast read is cheap.
    val tick by rememberTicker(60_000) { System.currentTimeMillis() / 60_000 }
    androidx.compose.runtime.LaunchedEffect(tick) { state = readBattery(context) }
    return state
}

private fun readBattery(context: Context): BatteryState {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0
    return BatteryState(percent, status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
}

@Composable
private fun rememberToday(): LocalDate {
    val today by rememberTicker(60_000) { LocalDate.now() }
    return today
}

/** Today's and tomorrow's events; empty until the user allows calendar access. */
@Composable
fun rememberUpcomingEvents(): List<CalendarEvent> {
    val context = LocalContext.current
    var events by remember { mutableStateOf(emptyList<CalendarEvent>()) }
    var refresh by remember { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    androidx.compose.runtime.LaunchedEffect(refresh) {
        events = if (Permissions.hasCalendar(context)) withContext(Dispatchers.IO) { queryEvents(context) } else emptyList()
    }
    return events
}

private fun queryEvents(context: Context): List<CalendarEvent> = runCatching {
    val now = System.currentTimeMillis()
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        ContentUris.appendId(it, now)
        ContentUris.appendId(it, now + TimeUnit.DAYS.toMillis(2))
    }.build()
    val projection = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.DISPLAY_COLOR,
    )
    context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
        buildList {
            while (cursor.moveToNext() && size < 4) {
                add(
                    CalendarEvent(
                        title = cursor.getString(0) ?: "Event",
                        begin = cursor.getLong(1),
                        allDay = cursor.getInt(2) == 1,
                        color = cursor.getInt(3),
                    ),
                )
            }
        }
    }.orEmpty()
}.getOrDefault(emptyList())


/** iOS Storage widget: real-time disk storage gauge and break-down */
@Composable
fun StorageWidget(modifier: Modifier) {
    val context = LocalContext.current
    val stat = remember {
        runCatching {
            val path = Environment.getDataDirectory().path
            val statFs = StatFs(path)
            val total = statFs.totalBytes
            val avail = statFs.availableBytes
            val used = (total - avail).coerceAtLeast(0L)
            val totalGb = (total / (1024L * 1024L * 1024L)).toInt()
            val usedGb = "%.1f".format(used / (1024.0 * 1024.0 * 1024.0))
            val freeGb = "%.1f".format(avail / (1024.0 * 1024.0 * 1024.0))
            val pct = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0.5f
            Triple(totalGb, "$usedGb GB of $totalGb GB used", Triple(pct, freeGb, "$usedGb GB"))
        }.getOrElse {
            Triple(128, "64.0 GB of 128 GB used", Triple(0.5f, "64.0", "64.0 GB"))
        }
    }

    Box(
        modifier
            .liquidGlass(WidgetShape, style = GlassStyle.Regular)
            .clickable {
                context.startSafely(Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            }
            .padding(14.dp)
    ) {
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
                            .size(28.dp)
                            .background(AccentBlue, SmoothShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Storage",
                        style = PanelBodyStyle.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    )
                }
                Text(
                    "${(stat.third.first * 100).toInt()}%",
                    style = PanelCaptionStyle.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stat.third.third,
                    style = LargeTitleStyle.copy(fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                )
                Text(
                    stat.second,
                    style = PanelCaptionStyle.copy(fontSize = 11.sp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.White.copy(alpha = 0.15f), Capsule)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(stat.third.first)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AccentBlue, AccentPurple, AccentPink)
                                ),
                                Capsule
                            )
                    )
                }
                Text(
                    "Available: ${stat.third.second} GB",
                    style = PanelCaptionStyle.copy(fontSize = 11.sp, color = AccentGreen)
                )
            }
        }
    }
}
