package com.glasslauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import android.app.ActivityOptions
import android.content.Context
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.Timer
import com.glasslauncher.data.ActiveTimerInfo
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glasslauncher.data.ActiveCallInfo
import com.glasslauncher.data.NotificationHub
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import android.telephony.TelephonyCallback
import android.telephony.SignalStrength
import android.os.Looper
import android.os.Handler
import android.net.Network
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.Icons
import android.telephony.TelephonyManager
import android.net.NetworkCapabilities
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import android.content.BroadcastReceiver
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Height of the launcher's own status bar: the same band the system bar uses (it's sized to the camera cutout on
 * phones), so the time and battery sit exactly where the system shows them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun glassStatusBarHeight(): Dp {
    val density = LocalDensity.current
    val system = WindowInsets.statusBarsIgnoringVisibility.getTop(density)
    val cutout = WindowInsets.displayCutout.getTop(density)
    return with(density) { maxOf(system, cutout).toDp() }.coerceAtLeast(24.dp)
}

/**
 * iOS-style status bar: time (and date on iPad) and active notification icons on the left,
 * Dynamic Island with active calls in the center, Wi-Fi, battery and Control Centre indicator on the right.
 */
@Composable
fun GlassStatusBar(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    icons: Map<String, ImageBitmap> = emptyMap(),
    onOpenControlCentre: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onDynamicIslandClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val now by rememberTicker(30_000) { LocalDateTime.now() }
    val battery by rememberTicker(60_000) {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        (if (level >= 0) level * 100 / scale else -1) to (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
    }
    val network by rememberNetworkStatus()
    val notificationItems by NotificationHub.items.collectAsStateWithLifecycle()
    val activeCall by NotificationHub.activeCall.collectAsStateWithLifecycle()
    val activeTimer by NotificationHub.activeTimer.collectAsStateWithLifecycle()
    val audioManager = remember { context.getSystemService(android.media.AudioManager::class.java) }
    val inCall = audioManager?.mode == android.media.AudioManager.MODE_IN_CALL ||
        audioManager?.mode == android.media.AudioManager.MODE_IN_COMMUNICATION ||
        activeCall != null

    val time = now.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"))
    val style = IconLabelStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

    Box(
        modifier
            .fillMaxWidth()
            .height(glassStatusBarHeight())
            // Clear of a corner or side cutout in landscape; the band's height already clears a top one.
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = if (isTablet) 28.dp else 22.dp),
    ) {
        // Left: Time (and date on iPad); tap opens Notification Centre. Also displays active notification app icons.
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .clickable(enabled = onOpenNotifications != null) { onOpenNotifications?.invoke() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(time, style = style)
            if (isTablet) Text("  " + now.format(DateTimeFormatter.ofPattern("EEE d MMM")), style = style.copy(fontWeight = FontWeight.Medium))

            // Show active notification app icons (for tablets and wide screens)
            val activePackages = remember(notificationItems) {
                notificationItems.map { it.packageName }.distinct()
            }
            if (activePackages.isNotEmpty() && (isTablet || activePackages.size <= 2)) {
                Spacer(Modifier.width(if (isTablet) 12.dp else 8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxIcons = if (isTablet) 6 else 2
                    val shown = activePackages.take(maxIcons)
                    for (pkg in shown) {
                        val icon = icons[pkg]
                        if (icon != null) {
                            Image(
                                bitmap = icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(3.5.dp))
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.75f))
                            )
                        }
                    }
                    if (activePackages.size > maxIcons) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // Center: Dynamic Island (clickable, non-swipable) - Not shown on tablets (matches iPadOS)
        if (!isTablet) {
            DynamicIsland(
                activeCall = activeCall,
                inCall = inCall,
                activeTimer = activeTimer,
                modifier = Modifier.align(Alignment.Center),
                onDynamicIslandClick = onDynamicIslandClick,
            )
        }

        // Right: Status icons, Control Centre indicator and battery
        Row(
            Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val glyph = Modifier.padding(end = 6.dp)
            if (network.focus) Icon(Icons.Rounded.DarkMode, contentDescription = "Focus", tint = Color.White, modifier = glyph.size(14.dp))
            if (network.airplane) {
                Icon(Icons.Rounded.AirplanemodeActive, contentDescription = "Airplane Mode", tint = Color.White, modifier = glyph.size(15.dp))
            }
            if (network.wifi) WifiGlyph(network.wifiLevel, glyph.size(width = 17.dp, height = 12.dp))
            if (network.hotspot) Icon(Icons.Rounded.Link, contentDescription = "Personal Hotspot", tint = Color.White, modifier = glyph.size(17.dp))
            if (network.vpn) {
                Text(
                    "VPN",
                    style = style.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    modifier = glyph.border(1.2.dp, Color.White, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp),
                )
            }
            val (percent, charging) = battery
            if (percent >= 0) Text("$percent%", style = style.copy(fontSize = 14.sp))
            BatteryGlyph(percent.coerceAtLeast(0), charging, Modifier.padding(start = 5.dp).size(width = 26.dp, height = 12.dp))

            // iOS Control Centre indicator: subtle rounded bar; only shown on tablets (removed on mobile phones)
            if (isTablet) {
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .width(18.dp)
                        .height(14.dp)
                        .clickable(enabled = onOpenControlCentre != null) { onOpenControlCentre?.invoke() },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(16.dp)
                            .height(3.5.dp)
                            .clip(Capsule)
                            .background(Color.White.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}

/**
 * Detects whether the device has native OEM dynamic island / status bar capsule support
 * (OnePlus OxygenOS Fluid Cloud, Realme Mini Capsule / Aqua Dynamics, Motorola Smart Connect).
 */
internal fun hasNativeDynamicIsland(): Boolean {
    val brand = Build.BRAND.lowercase()
    val manufacturer = Build.MANUFACTURER.lowercase()
    return brand.contains("oneplus") || manufacturer.contains("oneplus") ||
           brand.contains("oppo") || manufacturer.contains("oppo") ||
           brand.contains("realme") || manufacturer.contains("realme") ||
           brand.contains("motorola") || manufacturer.contains("motorola") ||
           brand.contains("moto")
}

private fun endActiveCall(context: Context, activeCall: ActiveCallInfo?) {
    val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            runCatching { telecom?.endCall() }
        }
    }
    activeCall?.let { call ->
        val item = com.glasslauncher.data.NotificationHub.items.value.firstOrNull { it.packageName == call.packageName }
        item?.let { com.glasslauncher.data.NotificationHub.dismiss(it.key) }
    }
}

/**
 * Reliably opens the in-call screen when tapping the Dynamic Island call pill.
 * Uses TelecomManager, PendingIntent with Background Activity Start permissions,
 * package launch with REORDER_TO_FRONT, and in-call / dialer intents.
 */
fun openInCallScreen(context: Context, activeCall: ActiveCallInfo?) {
    // 1. TelecomManager.showInCallScreen(false) brings the active in-call UI to front
    val telecom = runCatching { context.getSystemService(android.telecom.TelecomManager::class.java) }.getOrNull()
    val telecomWorked = runCatching {
        telecom?.showInCallScreen(false)
        true
    }.getOrDefault(false)

    // 2. Execute PendingIntent with background activity start mode allowed (Android 12/14+ requirement)
    val pendingIntent = activeCall?.intent
    var pendingSent = false
    if (pendingIntent != null) {
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }.toBundle()
        } else {
            ActivityOptions.makeBasic().toBundle()
        }
        pendingSent = runCatching {
            pendingIntent.send(context, 0, null, null, null, null, options)
            true
        }.getOrDefault(false)
    }

    // 3. If PendingIntent wasn't present or failed, launch the dialer package directly to the front
    if (!pendingSent) {
        val pkg = activeCall?.packageName
        val launchedPkg = if (!pkg.isNullOrBlank()) {
            runCatching {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(launchIntent)
                    true
                } else false
            }.getOrDefault(false)
        } else false

        // 4. Fallback: ACTION_CALL_BUTTON directly opens the in-call activity/dialer, fallback to ACTION_DIAL
        if (!launchedPkg && !telecomWorked) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_CALL_BUTTON).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                })
            }.onFailure {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_DIAL).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        }
    }
}

private enum class IslandActivity {
    Idle, Call, Timer
}

@Composable
private fun DynamicIsland(
    activeCall: ActiveCallInfo?,
    inCall: Boolean,
    activeTimer: ActiveTimerInfo?,
    modifier: Modifier = Modifier,
    onDynamicIslandClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    val hasCall = activeCall != null || inCall
    val hasTimer = activeTimer != null

    val activity = when {
        hasCall -> IslandActivity.Call
        hasTimer -> IslandActivity.Timer
        else -> IslandActivity.Idle
    }

    Row(
        modifier = modifier
            .consumeTaps()
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 400f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Simple pure black Apple Dynamic Island - compact, sleek, no colored borders
        when (activity) {
            IslandActivity.Call -> {
                val callerText = activeCall?.caller?.ifBlank { "Active Call" } ?: "Active Call"
                var callDropdownExpanded by remember { mutableStateOf(false) }

                if (!callDropdownExpanded) {
                    // Default sleek compact Dynamic Island call pill
                    Row(
                        Modifier
                            .height(32.dp)
                            .clip(Capsule)
                            .background(Color(0xFF000000))
                            .border(0.6.dp, Color.White.copy(alpha = 0.12f), Capsule)
                            .clickable {
                                AppleTaptic.medium(context)
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                callDropdownExpanded = true
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Call,
                                contentDescription = "Active Call",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            callerText,
                            style = IconLabelStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        CallWaveform()
                    }
                } else {
                    // Active call screen dropdown having speaker, mute, disconnect buttons
                    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager }
                    var isMuted by remember { mutableStateOf(audioManager?.isMicrophoneMute == true) }
                    var isSpeakerOn by remember { mutableStateOf(audioManager?.isSpeakerphoneOn == true) }

                    Column(
                        Modifier
                            .widthIn(min = 280.dp, max = 340.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF000000))
                            .border(0.8.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(26.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header: Tap to collapse
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { callDropdownExpanded = false },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34C759)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(callerText, style = PanelBodyStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Active Call", style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.70f)))
                                }
                            }
                            CallWaveform()
                        }

                        // Call Action Controls: Mute, Speaker, Disconnect, Open App
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute / Unmute Button
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isMuted) Color.White.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.14f))
                                    .clickable {
                                        audioManager?.let {
                                            val next = !it.isMicrophoneMute
                                            it.isMicrophoneMute = next
                                            isMuted = next
                                        }
                                        AppleTaptic.medium(context)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                    contentDescription = "Mute",
                                    tint = if (isMuted) Color(0xFFFF3B30) else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Speaker Button
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isSpeakerOn) Color(0xFF007AFF) else Color.White.copy(alpha = 0.14f))
                                    .clickable {
                                        audioManager?.let {
                                            val next = !it.isSpeakerphoneOn
                                            it.isSpeakerphoneOn = next
                                            isSpeakerOn = next
                                        }
                                        AppleTaptic.medium(context)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Speaker",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Disconnect / End Call Button
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                                    .clickable {
                                        AppleTaptic.heavy(context)
                                        endActiveCall(context, activeCall)
                                        callDropdownExpanded = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.CallEnd, contentDescription = "Disconnect", tint = Color.White, modifier = Modifier.size(22.dp))
                            }

                            // Open App Screen Button
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .clickable {
                                        openInCallScreen(context, activeCall)
                                        callDropdownExpanded = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open App", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            IslandActivity.Timer -> {
                val timerLabel = activeTimer?.text?.ifBlank { activeTimer.title } ?: "Timer"
                Row(
                    Modifier
                        .height(32.dp)
                        .clip(Capsule)
                        .background(Color(0xFF000000))
                        .border(0.6.dp, Color.White.copy(alpha = 0.12f), Capsule)
                        .clickable {
                            AppleTaptic.medium(context)
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (activeTimer?.intent != null) {
                                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    ActivityOptions.makeBasic().apply {
                                        pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                    }.toBundle()
                                } else {
                                    ActivityOptions.makeBasic().toBundle()
                                }
                                runCatching { activeTimer.intent.send(context, 0, null, null, null, null, options) }
                            } else {
                                launchTimer(context)
                            }
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9500)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = "Timer",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        timerLabel,
                        style = IconLabelStyle.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF9500),
                        ),
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(8.dp))
                    TimerWaveform()
                }
            }

            IslandActivity.Idle -> {
                // If device has native dynamic island support (OxygenOS / Realme / Motorola), let native OS handle it
                if (!hasNativeDynamicIsland()) {
                    // Simple black Apple idle capsule for devices without native island
                    Box(
                        Modifier
                            .width(82.dp)
                            .height(24.dp)
                            .clip(Capsule)
                            .background(Color(0xFF000000))
                            .border(0.6.dp, Color.White.copy(alpha = 0.08f), Capsule)
                            .clickable {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onDynamicIslandClick?.invoke()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF141418))
                                .border(0.5.dp, Color(0xFF24242A), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.75f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ), label = "timerScale",
    )
    Box(
        Modifier
            .size(7.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Color(0xFFFF9500)),
    )
}

@Composable
private fun CallWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "callWave")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ), label = "h1",
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ), label = "h2",
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ), label = "h3",
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.70f, targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ), label = "h4",
    )
    Row(
        Modifier.height(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
    ) {
        listOf(h1, h2, h3, h4).forEach { factor ->
            Box(
                Modifier
                    .width(2.5.dp)
                    .height((14.dp * factor).coerceAtLeast(3.dp))
                    .clip(Capsule)
                    .background(Color(0xFF34C759)),
            )
        }
    }
}

/** The iOS Wi-Fi glyph: three nested arcs fanning up from a point. */
@Composable
private fun WifiGlyph(level: Int, modifier: Modifier) {
    Canvas(modifier) {
        val apex = Offset(size.width / 2f, size.height)
        val stroke = size.height * 0.2f
        // A wedge for the innermost band, then two arcs, all 90° wide like iOS.
        for (band in 1..3) {
            val radius = size.height * band / 3f - stroke / 2f
            drawArc(
                Color.White.copy(alpha = if (band <= level) 1f else 0.35f),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = band == 1,
                topLeft = Offset(apex.x - radius, apex.y - radius),
                size = Size(radius * 2, radius * 2),
                style = if (band == 1) Fill else Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** The iOS battery glyph: a rounded outline with a level bar and a nub; green with a bolt while charging. */
@Composable
private fun BatteryGlyph(percent: Int, charging: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val nub = size.width * 0.07f
        val body = Size(size.width - nub - size.width * 0.03f, size.height)
        val corner = CornerRadius(size.height * 0.3f)
        drawRoundRect(Color.White.copy(alpha = 0.4f), size = body, cornerRadius = corner, style = Stroke(size.height * 0.09f))
        drawRoundRect(
            Color.White.copy(alpha = 0.4f),
            topLeft = Offset(size.width - nub, size.height * 0.32f),
            size = Size(nub, size.height * 0.36f),
            cornerRadius = CornerRadius(nub / 2f),
        )
        val inset = size.height * 0.17f
        val level = (percent / 100f).coerceIn(0.04f, 1f)
        val fill = when {
            charging -> AccentGreen
            percent <= 20 -> Color(0xFFFF3B30)
            else -> Color.White
        }
        drawRoundRect(
            fill,
            topLeft = Offset(inset, inset),
            size = Size((body.width - inset * 2) * level, body.height - inset * 2),
            cornerRadius = CornerRadius(size.height * 0.16f),
        )
        if (charging) {
            val cx = body.width / 2f
            val h = size.height
            val bolt = Path().apply {
                moveTo(cx + h * 0.08f, h * 0.08f)
                lineTo(cx - h * 0.28f, h * 0.56f)
                lineTo(cx - h * 0.02f, h * 0.56f)
                lineTo(cx - h * 0.1f, h * 0.92f)
                lineTo(cx + h * 0.28f, h * 0.44f)
                lineTo(cx + h * 0.02f, h * 0.44f)
                close()
            }
            drawPath(bolt, Color.Black.copy(alpha = 0.75f))
        }
    }
}

/** What the status bar shows besides the time and battery. Everything here is readable without extra permissions. */
private data class NetworkStatus(
    val wifi: Boolean = false,
    /** Wi-Fi bars 1–3. */
    val wifiLevel: Int = 3,
    /** Cellular bars 0–4, or -1 when there's no SIM or the phone gives no signal reading (then it's hidden). */
    val signal: Int = -1,
    val airplane: Boolean = false,
    val hotspot: Boolean = false,
    val vpn: Boolean = false,
    val focus: Boolean = false,
)

/**
 * Live network state for the status bar: Android's network and signal callbacks (no permission needed), listened
 * to only while the Home Screen is showing. Airplane mode, Focus and the hotspot are rechecked every 30 seconds.
 */
@Composable
private fun rememberNetworkStatus(): State<NetworkStatus> {
    val context = LocalContext.current
    // capabilities, signal strength, hotspot on
    val latest = remember { arrayOf<Any?>(null, null, false) }
    val state = remember { mutableStateOf(readNetwork(context, null, null, false)) }
    val tick by rememberTicker(30_000) { System.currentTimeMillis() }
    // The hotspot check lists network interfaces, which is slow, so it runs in the background on the 30-second tick
    // rather than on every network update (several arrive together right when the screen wakes).
    LaunchedEffect(tick) {
        latest[2] = withContext(Dispatchers.IO) { hotspotOn(context) }
        state.value = readNetwork(context, latest[0] as NetworkCapabilities?, latest[1] as SignalStrength?, latest[2] as Boolean)
    }
    LifecycleStartEffect(Unit) {
        val update = { state.value = readNetwork(context, latest[0] as NetworkCapabilities?, latest[1] as SignalStrength?, latest[2] as Boolean) }
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                latest[0] = capabilities
                update()
            }

            override fun onLost(network: Network) {
                latest[0] = null
                update()
            }
        }
        runCatching { connectivity?.registerDefaultNetworkCallback(networkCallback, Handler(Looper.getMainLooper())) }
        val telephony = context.getSystemService(TelephonyManager::class.java)
        val signalCallback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                latest[1] = signalStrength
                update()
            }
        }
        val listening = runCatching { telephony?.registerTelephonyCallback(context.mainExecutor, signalCallback) }.isSuccess

        val wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: Intent?) {
                val action = intent?.action
                if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                    val ws = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    if (ws == WifiManager.WIFI_STATE_DISABLED || ws == WifiManager.WIFI_STATE_DISABLING) {
                        val current = latest[0] as? NetworkCapabilities
                        if (current?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                            latest[0] = null
                        }
                    }
                } else if (action == "android.net.wifi.WIFI_AP_STATE_CHANGED" || action == ConnectivityManager.CONNECTIVITY_ACTION) {
                    latest[2] = hotspotOn(context)
                }
                update()
            }
        }
        val wifiFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        val receiverRegistered = runCatching {
            ContextCompat.registerReceiver(
                context,
                wifiReceiver,
                wifiFilter,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }.isSuccess

        onStopOrDispose {
            runCatching { connectivity?.unregisterNetworkCallback(networkCallback) }
            if (listening) runCatching { telephony?.unregisterTelephonyCallback(signalCallback) }
            if (receiverRegistered) runCatching { context.unregisterReceiver(wifiReceiver) }
        }
    }
    return state
}

private fun readNetwork(context: android.content.Context, live: NetworkCapabilities?, liveSignal: SignalStrength?, hotspot: Boolean): NetworkStatus = runCatching {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val caps = live ?: connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
    val telephony = context.getSystemService(TelephonyManager::class.java)
    val hasSim = telephony?.simState == TelephonyManager.SIM_STATE_READY
    // Only trust a reading that actually describes a cell; otherwise the bars are hidden rather than wrong.
    val strength = liveSignal ?: runCatching { telephony?.signalStrength }.getOrNull()
    val signal = if (hasSim && strength != null && strength.cellSignalStrengths.isNotEmpty()) strength.level.coerceIn(0, 4) else -1
    val wifiOn = isWifiOn(context)
    val wifi = wifiOn && caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val wifiLevel = caps?.signalStrength?.takeIf { wifi && it != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED }?.let { rssi ->
        val manager = context.getSystemService(android.net.wifi.WifiManager::class.java)
        val max = manager?.maxSignalLevel?.coerceAtLeast(1) ?: 4
        val level = manager?.calculateSignalLevel(rssi) ?: max
        (1 + level * 2 / max).coerceIn(1, 3)
    } ?: 3
    NetworkStatus(
        wifi = wifi,
        wifiLevel = wifiLevel,
        signal = signal,
        airplane = android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) == 1,
        hotspot = hotspot,
        vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
        focus = context.getSystemService(android.app.NotificationManager::class.java)?.currentInterruptionFilter.let {
            it != null && it != android.app.NotificationManager.INTERRUPTION_FILTER_ALL && it != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        },
    )
}.getOrDefault(NetworkStatus())

/**
 * Android has no public "is the hotspot on" call, but a hotspot (or USB/Bluetooth tethering) brings up its own
 * network interface, so we look for one of those being up.
 */
internal fun hotspotOn(context: android.content.Context? = null): Boolean {
    // The system's sticky hotspot broadcast is exact where it's readable (13 = on); interfaces are the fallback.
    val sticky = context?.let { runCatching { it.registerReceiver(null, android.content.IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED")) }.getOrNull() }
    sticky?.getIntExtra("wifi_state", -1)?.takeIf { it >= 0 }?.let { return it == 13 }
    return interfaceHotspot()
}

private fun interfaceHotspot(): Boolean = runCatching {
    java.net.NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { nic ->
        val name = nic.name.lowercase()
        nic.isUp && (name.startsWith("ap") || name.startsWith("swlan") || name.startsWith("softap") || name.startsWith("rndis") || name.startsWith("bt-pan")) &&
            nic.inetAddresses.toList().any { it is java.net.Inet4Address }
    }
}.getOrDefault(false)

/** The iOS signal glyph: four rising bars, the unused ones dimmed. */
@Composable
private fun SignalGlyph(level: Int, modifier: Modifier) {
    Canvas(modifier) {
        val gap = size.width * 0.1f
        val bar = (size.width - gap * 3) / 4f
        for (i in 0 until 4) {
            val height = size.height * (0.35f + 0.65f * i / 3f)
            drawRoundRect(
                Color.White.copy(alpha = if (i < level) 1f else 0.35f),
                topLeft = Offset(i * (bar + gap), size.height - height),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar * 0.35f),
            )
        }
    }
}
