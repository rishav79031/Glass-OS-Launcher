package com.glasslauncher.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.hardware.fingerprint.FingerprintManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.ui.zIndex
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.R
import com.glasslauncher.data.AppEntry
import com.glasslauncher.data.GlassLook
import com.glasslauncher.data.IconStyle
import com.glasslauncher.data.LockClockStyle
import com.glasslauncher.data.LockWidget
import com.glasslauncher.data.NotificationDisplay
import com.glasslauncher.data.NotificationItem
import com.glasslauncher.icons.Squircle
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val CLOCK_PRESET_COLORS = listOf(
    0xFFD8B4FE.toInt(), // Frosted Lavender / Lilac Glass (iOS 18 match)
    0xFFFFFFFF.toInt(), // Pure Frosted White
    0xFFFF3B30.toInt(), // Spider-Man Vibrant Red
    0xFF64D2FF.toInt(), // Sky Cyan Glass
    0xFFFFD60A.toInt(), // Golden Sun Glass
    0xFF30D158.toInt(), // Emerald Green Glass
    0xFFAF52DE.toInt(), // Deep Violet Glass
    0xFFFF2D55.toInt(), // Radiant Pink Glass
)

private data class WallpaperOption(val id: String, val name: String, val is3d: Boolean)

private val WALLPAPER_OPTIONS = listOf(
    WallpaperOption("curves", "Curved Glass", true),
    WallpaperOption("sunset_palms", "Sunset Palms", true),
    WallpaperOption("spiderman", "Spider-Man 3D", true),
    WallpaperOption("liquid", "Liquid 3D", true),
    WallpaperOption("solstice", "Solstice 3D", true),
    WallpaperOption("aurora", "Aurora 3D", true),
    WallpaperOption("home", "Home Wallpaper", false),
)

/**
 * Hook to read real-time device tilt (pitch & roll) for realistic 3D parallax lock screen wallpapers.
 */
@Composable
private fun rememberTiltOffset(enabled: Boolean): Pair<Float, Float> {
    val context = LocalContext.current
    var rawX by remember { mutableFloatStateOf(0f) }
    var rawY by remember { mutableFloatStateOf(0f) }
    val smoothX by animateFloatAsState(rawX, spring(dampingRatio = 0.85f, stiffness = 140f), label = "tiltX")
    val smoothY by animateFloatAsState(rawY, spring(dampingRatio = 0.85f, stiffness = 140f), label = "tiltY")

    DisposableEffect(enabled) {
        if (!enabled) {
            rawX = 0f
            rawY = 0f
            return@DisposableEffect onDispose {}
        }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val pitch = orientation[1] // Tilt forward/backward
                    val roll = orientation[2]  // Tilt left/right
                    rawX = (roll * 24f).coerceIn(-28f, 28f)
                    rawY = (pitch * 24f).coerceIn(-28f, 28f)
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val ax = event.values[0]
                    val ay = event.values[1]
                    rawX = (-ax * 3.8f).coerceIn(-28f, 28f)
                    rawY = ((ay - 4.5f) * 3.8f).coerceIn(-28f, 28f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensor?.let { sm?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }
    return smoothX to smoothY
}

/**
 * Photorealistic Liquid Frosted Glass Clock matching iOS 18:
 * Multi-layer rendering with diffused ambient glass aura,
 * translucent tinted crystal fill letting background show through,
 * and a crisp luminous refraction rim stroke.
 */
@Composable
private fun GlassLockClock(
    timeText: String,
    style: LockClockStyle,
    baseColor: Int,
    modifier: Modifier = Modifier
) {
    val fontInfo = lockClockFont(style)
    val color = Color(baseColor)

    // Translucent liquid crystal glass fill: transparent enough for background depth
    val glassFill = Brush.verticalGradient(
        listOf(
            color.copy(alpha = 0.58f),
            color.copy(alpha = 0.30f),
            color.copy(alpha = 0.48f),
            color.copy(alpha = 0.68f)
        )
    )

    // Luminous glass refraction rim bevel (gives the sculpted 3D liquid glass border)
    val rimBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.95f),
            color.copy(alpha = 0.70f),
            Color.White.copy(alpha = 0.88f)
        )
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // 1. Soft diffused ambient glass backglow matching tint
        Text(
            text = timeText,
            style = TextStyle(
                fontFamily = fontInfo.first,
                fontSize = 94.sp,
                fontWeight = fontInfo.second,
                letterSpacing = (-2).sp,
                color = color.copy(alpha = 0.50f),
                shadow = Shadow(color = color.copy(alpha = 0.85f), offset = Offset(0f, 3f), blurRadius = 26f)
            ),
        )

        // 2. Translucent liquid glass fill (letting wallpaper shine through)
        Text(
            text = timeText,
            style = TextStyle(
                fontFamily = fontInfo.first,
                fontSize = 94.sp,
                fontWeight = fontInfo.second,
                letterSpacing = (-2).sp,
                brush = glassFill,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.30f), offset = Offset(0f, 4f), blurRadius = 12f)
            ),
        )

        // 3. Crisp luminous refraction rim bevel
        Text(
            text = timeText,
            style = TextStyle(
                fontFamily = fontInfo.first,
                fontSize = 94.sp,
                fontWeight = fontInfo.second,
                letterSpacing = (-2).sp,
                drawStyle = Stroke(
                    width = 2.4f,
                    join = StrokeJoin.Round
                ),
                brush = rimBrush,
            ),
        )
    }
}

fun hasFingerprintSupport(context: Context): Boolean {
    val fp = runCatching { context.getSystemService(FingerprintManager::class.java) }.getOrNull()
    if (fp != null && runCatching { fp.isHardwareDetected && fp.hasEnrolledFingerprints() }.getOrDefault(false)) {
        return true
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val bm = runCatching { context.getSystemService(BiometricManager::class.java) }.getOrNull()
        if (bm != null) {
            val status = runCatching {
                bm.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }.getOrDefault(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
            return status == BiometricManager.BIOMETRIC_SUCCESS
        }
    }
    return false
}

fun triggerBiometricPrompt(context: Context, onAuthenticated: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val builder = BiometricPrompt.Builder(context)
            .setTitle("Unlock")
            .setSubtitle("Touch the fingerprint sensor to unlock")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setNegativeButton("Cancel", context.mainExecutor) { _, _ -> }
        }
        val signal = CancellationSignal()
        runCatching {
            builder.build().authenticate(
                signal,
                context.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onAuthenticated()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        // User cancelled
                    }
                }
            )
        }.onFailure {
            onAuthenticated()
        }
    } else {
        onAuthenticated()
    }
}

/**
 * Notification Centre styled as the iOS Lock Screen with customizable 3D wallpapers,
 * liquid glass clock, interactive notifications and fluid parallax tilt effect.
 */
@Composable
fun NotificationCentre(
    allItems: List<NotificationItem>,
    apps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    display: NotificationDisplay,
    clockStyle: LockClockStyle,
    clockColor: Int = 0xFFD8B4FE.toInt(), // Default to iOS Lavender glass
    lockClockScale: Float = 1.0f,
    lockClockOffsetX: Float = 0f,
    lockClockOffsetY: Float = 0f,
    lockWidgets: List<LockWidget>,
    lockWallpaper: String = "curves",
    lock3dEffect: Boolean = true,
    lockWallpaperScale: Float = 1.0f,
    lockWallpaperOffsetX: Float = 0f,
    lockWallpaperOffsetY: Float = 0f,
    homeWallpaper: ImageBitmap? = null,
    allowed: Boolean,
    glassLook: GlassLook = GlassLook.Clear,
    isLockScreen: Boolean = false,
    onUnlock: () -> Unit = {},
    onOpen: (NotificationItem) -> Unit,
    onDismissItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onRequestAccess: () -> Unit,
    onUpdateClockStyle: (LockClockStyle) -> Unit = {},
    onUpdateClockColor: (Int) -> Unit = {},
    onUpdateClockTransform: (scale: Float, x: Float, y: Float) -> Unit = { _, _, _ -> },
    onToggleLockWidget: (LockWidget) -> Unit = {},
    onUpdateLockWallpaper: (String) -> Unit = {},
    onToggle3dEffect: (Boolean) -> Unit = {},
    onUpdateWallpaperTransform: (scale: Float, x: Float, y: Float) -> Unit = { _, _, _ -> },
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(10_000)
        }
    }
    val byPackage = remember(apps) { apps.groupBy { it.packageName }.mapValues { it.value.first() } }
    var expandedCount by remember { mutableStateOf(false) }
    var expandedStacks by remember { mutableStateOf(setOf<String>()) }
    var dismissed by remember { mutableStateOf(setOf<String>()) }
    val items = remember(allItems, dismissed) { allItems.filter { it.key !in dismissed } }
    val dismiss: (String) -> Unit = { key ->
        dismissed = dismissed + key
        onDismissItem(key)
    }

    var customizingLockScreen by remember { mutableStateOf(false) }
    val (tiltX, tiltY) = rememberTiltOffset(enabled = lock3dEffect)

    Box(
        Modifier
            .fillMaxSize()
            .then(
                if (!isLockScreen) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { customizingLockScreen = true },
                            onTap = { if (!customizingLockScreen) onClose() }
                        )
                    }
                } else Modifier
            )
    ) {
        // 3D Parallax & Resizable Wallpaper Layer (Drag & pinch to fit directly)
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (lockWallpaperScale * zoom).coerceIn(0.5f, 4.0f)
                        val newX = lockWallpaperOffsetX + pan.x / density
                        val newY = lockWallpaperOffsetY + pan.y / density
                        onUpdateWallpaperTransform(newScale, newX, newY)
                    }
                }
                .graphicsLayer {
                    val baseScale = lockWallpaperScale.coerceIn(0.5f, 4.0f)
                    val parallaxScale = if (lock3dEffect) 1.15f else 1.0f
                    scaleX = baseScale * parallaxScale
                    scaleY = baseScale * parallaxScale
                    translationX = (if (lock3dEffect) tiltX * density else 0f) + (lockWallpaperOffsetX * density)
                    translationY = (if (lock3dEffect) tiltY * density else 0f) + (lockWallpaperOffsetY * density)
                }
        ) {
            when {
                lockWallpaper == "curves" -> {
                    Image(
                        painter = painterResource(id = R.drawable.wallpaper_curves),
                        contentDescription = "Curved Glass Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                lockWallpaper == "sunset_palms" -> {
                    Image(
                        painter = painterResource(id = R.drawable.wallpaper_sunset_palms),
                        contentDescription = "Sunset Palms Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                lockWallpaper == "spiderman" -> {
                    Image(
                        painter = painterResource(id = R.drawable.wallpaper_spiderman),
                        contentDescription = "Spider-Man 3D Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                lockWallpaper == "home" && homeWallpaper != null -> {
                    Image(
                        bitmap = homeWallpaper,
                        contentDescription = "Home Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    val gradient = when (lockWallpaper) {
                        "solstice" -> listOf(Color(0xFF0A0F33), Color(0xFF8338EC), Color(0xFFFF006E))
                        "aurora" -> listOf(Color(0xFF10123A), Color(0xFF2EE6C5), Color(0xFF8B5CF6))
                        else -> listOf(Color(0xFF0A1F5C), Color(0xFF4FC3FF), Color(0xFF1B0F3B))
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(gradient))
                    )
                }
            }
        }

        // Foreground Lock Screen Content
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))

            // Date
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
                style = PanelBodyStyle.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.95f),
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 8f)
                )
            )

            // Draggable & Pinchable Liquid Glass Clock
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (lockClockOffsetX * density).roundToInt(),
                            (lockClockOffsetY * density).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        val baseScale = lockClockScale.coerceIn(0.6f, 2.5f)
                        scaleX = baseScale
                        scaleY = baseScale
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (lockClockScale * zoom).coerceIn(0.6f, 2.5f)
                            val newX = (lockClockOffsetX + pan.x / density).coerceIn(-180f, 180f)
                            val newY = (lockClockOffsetY + pan.y / density).coerceIn(-220f, 280f)
                            onUpdateClockTransform(newScale, newX, newY)
                        }
                    }
            ) {
                GlassLockClock(
                    timeText = now.format(DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm")),
                    style = clockStyle,
                    baseColor = clockColor,
                    modifier = if (!isLockScreen) Modifier.clickable { customizingLockScreen = true } else Modifier
                )
            }

            // Lock Complications (Widgets) — only shown in notification shade, not on lockscreen face
            if (!isLockScreen && lockWidgets.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp).clickable { customizingLockScreen = true }
                ) {
                    lockWidgets.forEach { LockComplication(it) }
                }
            }

            if (isLockScreen) {
                // Lock screen: Clean spacer pushing stacked notifications to the bottom
                Spacer(Modifier.weight(1f))

                // Bottom stacked notifications (arranged one over another, expandable into rows)
                if (display != NotificationDisplay.None && allowed && items.isNotEmpty()) {
                    LockScreenStackedNotifications(
                        items = items,
                        byPackage = byPackage,
                        icons = icons,
                        onOpen = onOpen
                    )
                    Spacer(Modifier.height(14.dp))
                }
            } else {
                Spacer(Modifier.height(18.dp))

                // Notifications List (Full Notification Centre shade)
                Box(Modifier.weight(1f).fillMaxWidth().widthIn(max = 560.dp), contentAlignment = Alignment.TopCenter) {
                    when {
                        !allowed -> AccessCard(onRequestAccess)
                        items.isEmpty() -> Text(
                            "No Older Notifications",
                            style = PanelCaptionStyle.copy(
                                color = Color.White.copy(alpha = 0.75f),
                                shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 6f)
                            ),
                            modifier = Modifier.padding(top = 28.dp)
                        )
                        display == NotificationDisplay.Count && !expandedCount -> {
                            Row(
                                Modifier
                                    .liquidGlass(Capsule, style = GlassStyle.Regular)
                                    .clickable { expandedCount = true }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text(
                                    "${items.size} Notification${if (items.size == 1) "" else "s"}",
                                    style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                        else -> {
                            Column(Modifier.fillMaxSize()) {
                                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Notification Centre",
                                        style = PanelBodyStyle.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    var confirmClear by remember { mutableStateOf(false) }
                                    LaunchedEffect(confirmClear) {
                                        if (confirmClear) {
                                            delay(3_000)
                                            confirmClear = false
                                        }
                                    }
                                    Box(
                                        Modifier
                                            .height(30.dp)
                                            .animateContentSize()
                                            .liquidGlass(Capsule, style = GlassStyle.Regular)
                                            .clickable(onClickLabel = "Clear all") {
                                                if (confirmClear) {
                                                    dismissed = dismissed + items.map { it.key }
                                                    onClearAll()
                                                    confirmClear = false
                                                } else {
                                                    confirmClear = true
                                                }
                                            }
                                            .padding(horizontal = if (confirmClear) 12.dp else 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (confirmClear) {
                                            Text("Clear All", style = PanelBodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                                        } else {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear all", tint = Color.White, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                }
                                val stacked = display == NotificationDisplay.Stack
                                val groups = remember(items) { items.groupBy { it.packageName }.values.toList() }
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 76.dp)) {
                                    if (stacked) {
                                        items(groups, key = { it.first().packageName }) { group ->
                                            val pkg = group.first().packageName
                                            val app = byPackage[pkg]
                                            if (group.size == 1 || pkg in expandedStacks) {
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    group.forEach { item ->
                                                        SwipeToClear(item.key, dismiss) {
                                                            NotificationCard(item, app, icons[app?.key], glassLook, onOpen)
                                                        }
                                                    }
                                                    if (group.size > 1) {
                                                        Text(
                                                            "Show less",
                                                            style = PanelCaptionStyle.copy(fontWeight = FontWeight.SemiBold),
                                                            modifier = Modifier.clickable { expandedStacks = expandedStacks - pkg }.padding(4.dp),
                                                        )
                                                    }
                                                }
                                            } else {
                                                SwipeToClear(pkg, { group.forEach { dismiss(it.key) } }) {
                                                    NotificationStack(group, app, icons[app?.key], glassLook) { expandedStacks = expandedStacks + pkg }
                                                }
                                            }
                                        }
                                    } else {
                                        items(items, key = { it.key }) { item ->
                                            val app = byPackage[item.packageName]
                                            SwipeToClear(item.key, dismiss) { NotificationCard(item, app, icons[app?.key], glassLook, onOpen) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val tuning = LocalGlassTuning.current
            val dockFrost = tuning.dockFrost
            val isDarkTheme = tuning.iconStyle == IconStyle.Dark
            val isTintedTheme = tuning.iconStyle == IconStyle.Tinted
            val tintColor = Color(tuning.tintColor)

            val barBackground = when {
                isTintedTheme -> tintColor.copy(alpha = 0.22f + dockFrost * 0.35f)
                isDarkTheme -> Color(0xFF16161A).copy(alpha = 0.45f + dockFrost * 0.35f)
                tuning.iconStyle == IconStyle.Clear || tuning.look == GlassLook.Clear -> Color.White.copy(alpha = (dockFrost * 0.28f).coerceAtLeast(0.08f))
                tuning.look == GlassLook.Tinted -> tintColor.copy(alpha = 0.22f + dockFrost * 0.35f)
                else -> Color.Black.copy(alpha = 0.25f + dockFrost * 0.35f)
            }

            // Bottom Bar: Flashlight, (Fingerprint on LockScreen / Customise in Shade), Camera
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flashlight Button
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(barBackground)
                        .border(0.8.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                        .liquidGlass(CircleShape, style = GlassStyle.Clear)
                        .clickable {
                            val cam = context.getSystemService(android.hardware.camera2.CameraManager::class.java)
                            val id = cam?.cameraIdList?.firstOrNull()
                            if (id != null) {
                                runCatching { cam.setTorchMode(id, true) }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FlashlightOn, contentDescription = "Flashlight", tint = Color.White, modifier = Modifier.size(24.dp))
                }

                // Center: Customise Lock Screen Pill
                Box(
                    Modifier
                        .height(38.dp)
                        .clip(Capsule)
                        .background(barBackground)
                        .border(0.7.dp, Color.White.copy(alpha = 0.28f), Capsule)
                        .liquidGlass(Capsule, style = GlassStyle.Clear)
                        .clickable { customizingLockScreen = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Customise", style = PanelBodyStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White))
                    }
                }

                // Camera Button
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(barBackground)
                        .border(0.8.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                        .liquidGlass(CircleShape, style = GlassStyle.Clear)
                        .clickable {
                            runCatching {
                                context.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = "Camera", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Lock Screen Customizer Bottom Sheet (Accessible only from Shade or Settings)
        if (!isLockScreen) {
            AnimatedVisibility(
                visible = customizingLockScreen,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.94f),
                exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.94f)
            ) {
                LockScreenCustomizer(
                    clockStyle = clockStyle,
                    clockColor = clockColor,
                    lockWidgets = lockWidgets,
                    lockWallpaper = lockWallpaper,
                    lock3dEffect = lock3dEffect,
                    onUpdateClockStyle = onUpdateClockStyle,
                    onUpdateClockColor = onUpdateClockColor,
                    onToggleLockWidget = onToggleLockWidget,
                    onUpdateLockWallpaper = onUpdateLockWallpaper,
                    onToggle3dEffect = onToggle3dEffect,
                    onDismiss = { customizingLockScreen = false }
                )
            }
        }
    }
}

/**
 * Interactive iOS 18 style Lock Screen Customizer panel:
 * Allows configuring 3D Parallax Depth, Wallpapers (Spider-Man 3D, Liquid, etc.),
 * Clock Font Style, Clock Color, and Complication Widgets.
 */
@Composable
private fun LockScreenCustomizer(
    clockStyle: LockClockStyle,
    clockColor: Int,
    lockWidgets: List<LockWidget>,
    lockWallpaper: String,
    lock3dEffect: Boolean,
    onUpdateClockStyle: (LockClockStyle) -> Unit,
    onUpdateClockColor: (Int) -> Unit,
    onToggleLockWidget: (LockWidget) -> Unit,
    onUpdateLockWallpaper: (String) -> Unit,
    onToggle3dEffect: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val tuning = LocalGlassTuning.current
    val dockFrost = tuning.dockFrost
    val isDarkTheme = tuning.iconStyle == IconStyle.Dark
    val isTintedTheme = tuning.iconStyle == IconStyle.Tinted
    val tintColor = Color(tuning.tintColor)

    val sheetBg = when {
        isTintedTheme -> tintColor.copy(alpha = 0.45f + dockFrost * 0.40f)
        isDarkTheme -> Color(0xFF16161A).copy(alpha = 0.65f + dockFrost * 0.30f)
        tuning.iconStyle == IconStyle.Clear || tuning.look == GlassLook.Clear -> Color(0xFF202026).copy(alpha = (0.28f + dockFrost * 0.45f).coerceAtMost(0.92f))
        tuning.look == GlassLook.Tinted -> tintColor.copy(alpha = 0.45f + dockFrost * 0.40f)
        else -> Color(0xFF1C1C1E).copy(alpha = 0.60f + dockFrost * 0.35f)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .clip(SmoothShape(32.dp))
                .background(sheetBg)
                .liquidGlass(SmoothShape(32.dp), style = GlassStyle.Clear)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            // Header: "Customise Lock Screen" & "Done"
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lock Screen", style = PanelTitleStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Box(
                    Modifier
                        .clip(Capsule)
                        .background(AccentBlue)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Done", style = PanelBodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3D Parallax Tilt Effect Toggle
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(SmoothShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp).background(Color(0xFF34C759).copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ViewInAr, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("3D Parallax Depth", style = PanelBodyStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                        Text("Tilts wallpaper with phone movement", style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f)))
                    }
                }
                Switch(
                    checked = lock3dEffect,
                    onCheckedChange = onToggle3dEffect,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                )
            }

            Spacer(Modifier.height(14.dp))

            // Wallpapers Row
            Text("WALLPAPER", style = WidgetCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(WALLPAPER_OPTIONS) { opt ->
                    val selected = lockWallpaper == opt.id
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) AccentBlue else Color.White.copy(alpha = 0.12f))
                            .border(
                                width = if (selected) 2.dp else 0.5.dp,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onUpdateLockWallpaper(opt.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(opt.name, style = PanelBodyStyle.copy(fontSize = 12.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = Color.White))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Clock Font Style Row
            Text("CLOCK FONT", style = WidgetCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LockClockStyle.entries) { style ->
                    val selected = clockStyle == style
                    Box(
                        Modifier
                            .clip(Capsule)
                            .background(if (selected) AccentBlue else Color.White.copy(alpha = 0.12f))
                            .clickable { onUpdateClockStyle(style) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(style.title, style = TextStyle(fontFamily = lockClockFont(style).first, color = Color.White, fontSize = 13.sp, fontWeight = lockClockFont(style).second))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Clock Color Palette Row (with Liquid Glass Lavender)
            Text("CLOCK GLASS COLOR", style = WidgetCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CLOCK_PRESET_COLORS) { color ->
                    val selected = clockColor == color
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { onUpdateClockColor(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = if (Color(color).luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Lock Complications (Widgets)
            Text("WIDGETS", style = WidgetCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LockWidget.entries) { w ->
                    val active = w in lockWidgets
                    Box(
                        Modifier
                            .clip(Capsule)
                            .background(if (active) AccentGreen else Color.White.copy(alpha = 0.12f))
                            .clickable { onToggleLockWidget(w) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(w.title, style = PanelBodyStyle.copy(fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = Color.White))
                    }
                }
            }
        }
    }
}

/**
 * Lock Screen Stacked Notifications:
 * Placed at the bottom of the lock screen. Cards are stacked on top of each other with slight peek.
 * Tapping expands them smoothly into rows.
 * Card format:
 * App Icon - App Name • Time
 * Notification Title
 * Notification text below that
 */
@Composable
private fun LockScreenStackedNotifications(
    items: List<NotificationItem>,
    byPackage: Map<String, AppEntry>,
    icons: Map<String, ImageBitmap>,
    onOpen: (NotificationItem) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!expanded) {
            // Stacked Cards Mode (Cards arranged one over another with visible height step)
            val topItem = items.first()
            val topApp = byPackage[topItem.packageName]
            val topIcon = icons[topApp?.key]

            val secondItem = items.getOrNull(1)
            val secondApp = secondItem?.let { byPackage[it.packageName] }
            val secondIcon = icons[secondApp?.key]

            val thirdItem = items.getOrNull(2)
            val thirdApp = thirdItem?.let { byPackage[it.packageName] }
            val thirdIcon = icons[thirdApp?.key]

            val extraBottomPadding = when {
                items.size >= 3 -> 36.dp
                items.size >= 2 -> 20.dp
                else -> 0.dp
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(horizontal = 6.dp)
                    .padding(bottom = if (items.size >= 3) 24.dp else if (items.size >= 2) 12.dp else 0.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Tier 3 Card Shell (peeking lowest in height, only beneath Tier 2)
                if (items.size >= 3) {
                    val shelf3Shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
                    Box(
                        Modifier
                            .fillMaxWidth(0.90f)
                            .height(26.dp)
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                translationY = 22.dp.toPx()
                                scaleX = 0.90f
                                alpha = 0.65f
                            }
                            .zIndex(0f)
                            .liquidGlass(shelf3Shape, 22.dp, style = GlassStyle.Clear)
                            .border(0.8.dp, Color.White.copy(alpha = 0.30f), shelf3Shape)
                    )
                }

                // Tier 2 Card Shell (peeking beneath Tier 1 in height, only beneath Tier 1)
                if (items.size >= 2) {
                    val shelf2Shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
                    Box(
                        Modifier
                            .fillMaxWidth(0.95f)
                            .height(26.dp)
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                translationY = 11.dp.toPx()
                                scaleX = 0.95f
                                alpha = 0.85f
                            }
                            .zIndex(1f)
                            .liquidGlass(shelf2Shape, 22.dp, style = GlassStyle.Clear)
                            .border(0.85.dp, Color.White.copy(alpha = 0.35f), shelf2Shape)
                    )
                }

                // Tier 1: Front card (on top in full view)
                Box(Modifier.fillMaxWidth().zIndex(2f)) {
                    LockScreenNotificationCard(
                        item = topItem,
                        app = topApp,
                        icon = topIcon,
                        countBadge = items.size,
                        onClick = { expanded = true }
                    )
                }
            }
        } else {
            // Expanded Rows Mode
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${items.size} Notifications",
                    style = PanelCaptionStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.90f))
                )
                Text(
                    "Stack",
                    style = PanelCaptionStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64D2FF)),
                    modifier = Modifier.clickable { expanded = false }.padding(4.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.key }) { item ->
                    val app = byPackage[item.packageName]
                    val icon = icons[app?.key]
                    LockScreenNotificationCard(
                        item = item,
                        app = app,
                        icon = icon,
                        onClick = { onOpen(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LockScreenNotificationCard(
    item: NotificationItem,
    app: AppEntry?,
    icon: ImageBitmap?,
    countBadge: Int? = null,
    onClick: () -> Unit
) {
    val shape = SmoothShape(22.dp)
    val timeAgo = remember(item.postTime) { relativeTime(item.postTime) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape, 22.dp, style = GlassStyle.Clear)
            .border(0.85.dp, Color.White.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Header: App icon - App Name • Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(Squircle)
                    )
                } else {
                    Box(
                        Modifier
                            .size(20.dp)
                            .background(Color.White.copy(alpha = 0.2f), Squircle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(Modifier.width(7.dp))

                Text(
                    text = app?.label ?: item.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    style = PanelBodyStyle.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.width(5.dp))

                Text(
                    text = "•",
                    style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.60f))
                )

                Spacer(Modifier.width(5.dp))

                Text(
                    text = timeAgo,
                    style = PanelCaptionStyle.copy(fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.70f))
                )

                if (countBadge != null && countBadge > 1) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(Capsule)
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "$countBadge",
                            style = PanelCaptionStyle.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                }
            }

            // Notification Title
            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    style = PanelBodyStyle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Notification Content below that
            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    style = PanelBodyStyle.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.88f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItem,
    app: AppEntry?,
    icon: ImageBitmap?,
    look: GlassLook,
    onOpen: (NotificationItem) -> Unit,
    extraLine: String? = null,
) {
    val style = if (look == GlassLook.Tinted) GlassStyle.Dark else GlassStyle.Clear
    Column(
        Modifier
            .fillMaxWidth()
            .liquidGlass(NotificationShape, style = style)
            .border(0.7.dp, Color.White.copy(alpha = 0.28f), NotificationShape)
            .clickable { onOpen(item) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        // Row 1: App icon - App Name (and relative time on the right)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(18.dp).clip(Squircle))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                app?.label ?: item.packageName,
                style = PanelCaptionStyle.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.95f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                relativeTime(item.postTime),
                style = PanelCaptionStyle.copy(fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.65f)),
                maxLines = 1
            )
        }
        Spacer(Modifier.height(4.dp))
        // Row 2: Notification Title
        if (item.title.isNotBlank()) {
            Text(
                item.title,
                style = PanelBodyStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Row 3: Notification Text
        if (item.text.isNotBlank()) {
            Text(
                item.text,
                style = PanelCaptionStyle.copy(fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.88f)),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (extraLine != null) {
            Text(extraLine, style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.60f)), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun NotificationStack(
    group: List<NotificationItem>,
    app: AppEntry?,
    icon: ImageBitmap?,
    look: GlassLook,
    onExpand: () -> Unit,
) {
    val style = if (look == GlassLook.Tinted) GlassStyle.Dark else GlassStyle.Clear
    Box(Modifier.fillMaxWidth().padding(bottom = 18.dp).clickable(onClick = onExpand)) {
        if (group.size >= 3) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 16.dp)
                    .fillMaxWidth(0.86f)
                    .height(64.dp)
                    .liquidGlass(NotificationShape, style = style)
                    .graphicsLayer { alpha = 0.5f },
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 8.dp)
                .fillMaxWidth(0.93f)
                .height(68.dp)
                .liquidGlass(NotificationShape, style = style)
                .graphicsLayer { alpha = 0.75f },
        )
        NotificationCard(
            item = group.first(),
            app = app,
            icon = icon,
            look = look,
            onOpen = { onExpand() },
            extraLine = "${group.size - 1} more notification${if (group.size == 2) "" else "s"}",
        )
    }
}

@Composable
private fun SwipeToClear(key: String, onDismiss: (String) -> Unit, content: @Composable () -> Unit) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val density = LocalDensity.current
    val stopPoint = with(density) { 92.dp.toPx() }
    val bifurcationGap = with(density) { 10.dp.toPx() }
    val endMargin = with(density) { 6.dp.toPx() }
    var cardWidth by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidth = it.width.toFloat() },
    ) {
        val travel = -offset.value
        val clearThreshold = if (cardWidth > 0) cardWidth * 0.45f else with(density) { 160.dp.toPx() }
        val isArmed = travel >= clearThreshold
        val basePillWidth = with(density) { 76.dp.toPx() }
        val clearWidth = if (travel > stopPoint) {
            (travel - endMargin - bifurcationGap).coerceAtLeast(basePillWidth)
        } else {
            basePillWidth
        }

        if (travel > 8f) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .width(with(density) { clearWidth.toDp() })
                    .height(66.dp)
                    .clip(SmoothShape(22.dp))
                    .background(if (isArmed) DestructiveRed else Color(0x35FF3B30))
                    .liquidGlass(SmoothShape(22.dp), style = GlassStyle.Regular)
                    .clickable {
                        scope.launch {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            offset.animateTo(-cardWidth.coerceAtLeast(1000f), tween(200))
                            onDismiss(key)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Clear",
                        style = PanelBodyStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }

        Box(
            Modifier
                .graphicsLayer { translationX = offset.value }
                .pointerInput(key) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val threshold = if (cardWidth > 0) cardWidth * 0.45f else stopPoint * 2f
                                when {
                                    offset.value < -threshold -> {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        offset.animateTo(-cardWidth.coerceAtLeast(1000f), tween(200))
                                        onDismiss(key)
                                    }
                                    offset.value < -stopPoint * 0.45f -> {
                                        offset.animateTo(-stopPoint, spring(dampingRatio = 0.8f, stiffness = 380f))
                                    }
                                    else -> {
                                        offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 380f))
                                    }
                                }
                                armed = false
                            }
                        },
                    ) { change, amount ->
                        change.consume()
                        scope.launch {
                            val current = offset.value
                            val delta = if (current < -stopPoint && amount < 0) amount * 0.75f else amount
                            val nextTarget = (current + delta).coerceAtMost(0f)
                            offset.snapTo(nextTarget)

                            val threshold = if (cardWidth > 0) cardWidth * 0.45f else stopPoint * 2f
                            val pastThreshold = nextTarget < -threshold
                            if (pastThreshold != armed) {
                                armed = pastThreshold
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                },
        ) {
            content()
        }
    }
}

@Composable
private fun AccessCard(onRequestAccess: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .liquidGlass(NotificationShape, style = GlassStyle.Regular)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        Text("See your notifications here", style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold))
        Text("Allow notification access to show them here.", style = PanelCaptionStyle, textAlign = TextAlign.Center)
        Box(
            Modifier.width(220.dp).height(44.dp).background(AccentBlue, Capsule).clickable(onClick = onRequestAccess),
            contentAlignment = Alignment.Center,
        ) {
            Text("Allow Access", style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

private fun relativeTime(time: Long): String {
    val elapsed = System.currentTimeMillis() - time
    return if (elapsed < DateUtils.MINUTE_IN_MILLIS) "now" else DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
}

private val NotificationShape = SmoothShape(24.dp)

@Composable
internal fun LockComplication(widget: LockWidget) {
    val env = LocalWidgetEnvironment.current
    val context = LocalContext.current
    val events = rememberUpcomingEvents()
    val content: Pair<String, String> = when (widget) {
        LockWidget.Battery -> {
            val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100) ?: 100
            "Battery" to (if (level >= 0) "${level * 100 / scale}%" else "—")
        }
        LockWidget.Weather -> (env.weather?.place ?: "Weather") to (env.weather?.let { "${it.temperature}°" } ?: "—")
        LockWidget.NextEvent -> (events.firstOrNull()?.title ?: "Calendar") to (events.firstOrNull()?.let {
            if (it.allDay) "All day" else java.time.Instant.ofEpochMilli(it.begin).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        } ?: "No events")
        LockWidget.WorldClock -> "London" to java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/London")).format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    Column(
        Modifier
            .width(108.dp)
            .liquidGlass(SmoothShape(18.dp), style = GlassStyle.Regular)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(content.first, style = WidgetCaptionStyle.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(content.second, style = PanelBodyStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
