package com.glasslauncher.ui

import android.app.Activity
import android.content.ContextWrapper

import android.os.Build
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.CircularProgressIndicator
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.glasslauncher.bridge.ShizukuBridge
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Add
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Network
import android.net.Uri
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.glasslauncher.data.NotificationBadgeService
import kotlin.math.roundToInt

/**
 * Reliable Wi-Fi state check:
 * 1. Checks Settings.Global.WIFI_ON first, which Android immediately updates to 0 when the user turns Wi-Fi off.
 * 2. Checks WifiManager.wifiState explicitly, treating WIFI_STATE_DISABLED and WIFI_STATE_DISABLING as off
 *    (preventing Samsung/OEM background scanning bugs where isWifiEnabled erroneously returns true).
 */
internal fun isWifiOn(context: Context): Boolean {
    val resolver = context.contentResolver
    val globalWifi = runCatching {
        Settings.Global.getInt(resolver, Settings.Global.WIFI_ON)
    }.getOrNull()
    if (globalWifi != null && globalWifi == 0) {
        return false
    }

    val wm = context.applicationContext.getSystemService(WifiManager::class.java)
    val state = runCatching { wm?.wifiState }.getOrNull()
    if (state != null && state != WifiManager.WIFI_STATE_UNKNOWN) {
        if (state == WifiManager.WIFI_STATE_DISABLED || state == WifiManager.WIFI_STATE_DISABLING) {
            return false
        }
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            return true
        }
    }

    if (globalWifi != null) {
        return globalWifi != 0
    }

    return runCatching { wm?.isWifiEnabled == true }.getOrDefault(false)
}

/**
 * Direct Personal Hotspot / Tethering settings intent.
 * Directs straight to the Wi-Fi Hotspot / Mobile Hotspot settings rather than
 * generic "Connection & Sharing" (ACTION_WIRELESS_SETTINGS).
 */
internal fun hotspotSettingsIntent(context: Context): Intent {
    val pm = context.packageManager
    val candidates = listOf(
        // Stock Android 11+ Wi-Fi Hotspot settings
        Intent("com.android.settings.WIFI_TETHER_SETTINGS"),
        // Samsung One UI Mobile Hotspot activity candidates
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$WifiApSettingsActivity")),
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.wifi.mobileap.WifiApSettings")),
        // Standard Android Tethering settings
        Intent("android.settings.TETHER_SETTINGS"),
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$WifiTetherSettingsActivity")),
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TetherSettings")),
        // OEM specific hotspot intent
        Intent("android.settings.WIFI_AP_SETTINGS"),
    )
    for (intent in candidates) {
        val resolved = pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        if (resolved.isNotEmpty()) {
            return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Reads and changes the device settings the Control Centre shows. */
@Stable
class SystemControls(private val context: Context) {
    private val wifiManager = context.getSystemService(WifiManager::class.java)
    private val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val resolver = context.contentResolver

    /** Null on devices without a flash, so that button becomes Display instead. */
    /**
     * Looked up once per process, off the main thread (camera queries take tens of milliseconds); null on devices
     * without a flash, so that button becomes Display instead.
     */
    val torchCameraId: String? get() = cachedTorchId

    var wifiOn by mutableStateOf(false); private set
    var wifiSsid by mutableStateOf<String?>(null); private set
    var bluetoothOn by mutableStateOf(false); private set
    var online by mutableStateOf(false); private set
    var airplaneOn by mutableStateOf(false); private set
    var rotationLocked by mutableStateOf(false); private set
    var dndOn by mutableStateOf(false); private set
    var torchOn by mutableStateOf(false); private set
    var brightness by mutableFloatStateOf(0.5f); private set
    var volume by mutableFloatStateOf(0.5f); private set
    var silent by mutableStateOf(false); private set
    var mobileData by mutableStateOf(false); private set
    var hotspot by mutableStateOf(false); private set
    var vpn by mutableStateOf(false); private set

    val canWriteSettings: Boolean get() = Settings.System.canWrite(context)
    val canControlDnd: Boolean get() = notificationManager.isNotificationPolicyAccessGranted

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) torchOn = enabled
        }
    }

    private var registered = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    if (state == WifiManager.WIFI_STATE_DISABLED || state == WifiManager.WIFI_STATE_DISABLING) {
                        wifiOn = false
                        wifiSsid = null
                    } else if (state == WifiManager.WIFI_STATE_ENABLED) {
                        wifiOn = true
                        wifiSsid = getConnectedWifiSsid(context)
                    } else {
                        wifiOn = isWifiOn(context)
                        wifiSsid = if (wifiOn) getConnectedWifiSsid(context) else null
                    }
                    val caps = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
                    online = connectivityManager?.activeNetwork != null
                    mobileData = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    bluetoothOn = state == BluetoothAdapter.STATE_ON
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    airplaneOn = intent.getBooleanExtra("state", false)
                    wifiOn = isWifiOn(context)
                    wifiSsid = if (wifiOn) getConnectedWifiSsid(context) else null
                    bluetoothOn = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)
                }
                ConnectivityManager.CONNECTIVITY_ACTION,
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    refresh()
                }
            }
        }
    }

    private val settingsObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            refresh()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            mainHandler.post { refresh() }
        }
        override fun onLost(network: Network) {
            mainHandler.post { refresh() }
        }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            mainHandler.post { refresh() }
        }
    }

    fun start() {
        refresh()
        if (!registered) {
            val filter = IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            }
            runCatching {
                ContextCompat.registerReceiver(
                    context,
                    stateReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED,
                )
            }
            runCatching {
                resolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.WIFI_ON),
                    false,
                    settingsObserver,
                )
                resolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON),
                    false,
                    settingsObserver,
                )
                resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                    false,
                    settingsObserver,
                )
                resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                    false,
                    settingsObserver,
                )
            }
            runCatching {
                connectivityManager?.registerDefaultNetworkCallback(networkCallback, mainHandler)
            }
            registered = true
        }
    }

    /** The slow lookups (camera and network interfaces), run in the background after the panel is up. */
    suspend fun loadSlow() {
        val found = withContext(Dispatchers.IO) {
            if (!torchLookedUp) {
                cachedTorchId = runCatching {
                    cameraManager.cameraIdList.firstOrNull {
                        cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
                }.getOrNull()
                torchLookedUp = true
            }
            hotspotOn(context)
        }
        hotspot = found
        torchReady = true
        if (torchCameraId != null && !torchRegistered) {
            cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
            torchRegistered = true
        }
    }

    /** False until [loadSlow] has checked for a flash, so the button doesn't flicker between Flashlight and Display. */
    var torchReady by mutableStateOf(torchLookedUp); private set
    private var torchRegistered = false

    fun stop() {
        if (torchRegistered) cameraManager.unregisterTorchCallback(torchCallback)
        torchRegistered = false
        if (registered) {
            runCatching { context.unregisterReceiver(stateReceiver) }
            runCatching { resolver.unregisterContentObserver(settingsObserver) }
            runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
            registered = false
        }
    }

    fun refresh() {
        wifiOn = isWifiOn(context)
        wifiSsid = if (wifiOn) getConnectedWifiSsid(context) else null
        bluetoothOn = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)
        online = connectivityManager?.activeNetwork != null
        airplaneOn = Settings.Global.getInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        rotationLocked = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 0
        dndOn = notificationManager.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
        brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat()
        silent = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
        val caps = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
        mobileData = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true
        vpn = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
    }

    /** iOS Silent mode: the ringer goes to vibrate (fully silent would need Do Not Disturb access). */
    fun toggleSilent() {
        runCatching { audioManager.ringerMode = if (silent) AudioManager.RINGER_MODE_NORMAL else AudioManager.RINGER_MODE_VIBRATE }
        silent = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    /** Adjusts brightness; immediately updates screen window attributes for instant control, and persists to system settings if permitted. */
    fun changeBrightness(value: Float): Boolean {
        val level = value.coerceIn(0.02f, 1f)
        brightness = level
        findActivity(context)?.window?.let { w ->
            val lp = w.attributes
            lp.screenBrightness = level
            w.attributes = lp
        }
        if (!canWriteSettings) return false
        return runCatching {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, (level * 255).roundToInt())
            true
        }.getOrDefault(false)
    }

    fun changeVolume(value: Float) {
        val level = value.coerceIn(0f, 1f)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (level * maxVolume).roundToInt(), AudioManager.FLAG_SHOW_UI) }
        volume = level
    }

    fun toggleRotationLock(): Boolean {
        if (!canWriteSettings) return false
        return runCatching {
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, if (rotationLocked) 1 else 0)
            rotationLocked = !rotationLocked
            true
        }.getOrDefault(false)
    }

    fun toggleDnd(): Boolean {
        if (!canControlDnd) return false
        notificationManager.setInterruptionFilter(
            if (dndOn) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        )
        dndOn = !dndOn
        return true
    }

    fun toggleTorch() {
        val id = torchCameraId ?: return
        runCatching { cameraManager.setTorchMode(id, !torchOn) }
    }

    suspend fun toggleAirplaneMode(): Boolean {
        val target = !airplaneOn
        val success = ShizukuBridge.setAirplaneMode(target)
        if (success) airplaneOn = target
        return success
    }

    suspend fun toggleWifi(): Boolean {
        val target = !wifiOn
        val success = ShizukuBridge.setWifi(target)
        if (success) {
            wifiOn = target
            wifiSsid = if (target) getConnectedWifiSsid(context) else null
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            wifiManager?.setWifiEnabled(target)
            wifiOn = target
            wifiSsid = if (target) getConnectedWifiSsid(context) else null
        }
        return success
    }

    suspend fun toggleMobileData(): Boolean {
        val target = !mobileData
        val success = ShizukuBridge.setMobileData(target)
        if (success) mobileData = target
        return success
    }

    suspend fun toggleBluetooth(): Boolean {
        val target = !bluetoothOn
        val success = ShizukuBridge.setBluetooth(target)
        if (success) {
            bluetoothOn = target
        } else {
            runCatching {
                if (target) {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter?.enable()
                } else {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter?.disable()
                }
            }
        }
        return success
    }

    suspend fun toggleHotspot(): Boolean {
        val target = !hotspot
        val success = ShizukuBridge.setHotspot(target)
        if (success) hotspot = target
        return success
    }
}

private var cachedTorchId: String? = null
private var torchLookedUp = false

/** The playing media session, read through the notification-access grant (Android's rule for media controls). */
@Stable
class NowPlaying(private val context: Context) {
    private val sessions = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, NotificationBadgeService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null

    var title by mutableStateOf<String?>(null); private set
    var artist by mutableStateOf<String?>(null); private set
    var artwork by mutableStateOf<ImageBitmap?>(null); private set
    var playing by mutableStateOf(false); private set
    var allowed by mutableStateOf(false); private set
    val packageName: String? get() = controller?.packageName
    val sessionActivity: PendingIntent? get() = controller?.sessionActivity

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = read()
        override fun onPlaybackStateChanged(state: PlaybackState?) = read()
        override fun onSessionDestroyed() = attach(null)
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers -> attach(controllers?.firstOrNull()) }

    fun start() {
        val active = runCatching { sessions.getActiveSessions(component) }.getOrNull()
        allowed = active != null
        if (active == null) return
        runCatching { sessions.addOnActiveSessionsChangedListener(sessionsListener, component, handler) }
        attach(active.firstOrNull())
    }

    fun stop() {
        runCatching { sessions.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
    }

    fun playPause() {
        val controls = controller?.transportControls ?: return
        if (playing) controls.pause() else controls.play()
    }

    fun next() = controller?.transportControls?.skipToNext()

    fun previous() = controller?.transportControls?.skipToPrevious()

    private fun attach(next: MediaController?) {
        controller?.unregisterCallback(callback)
        controller = next
        next?.registerCallback(callback, handler)
        read()
    }

    private fun read() {
        val metadata = controller?.metadata
        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        artwork = (metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART))?.asImageBitmap()
        playing = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
    }
}

private enum class ControlPage { Main, Music, Connectivity }

enum class DetailSelector { Wifi, Bluetooth, Hotspot, Cellular }

/** Apple's springs: quick, settled, no bounce you'd notice. Shared by every Control Centre motion. */
private val GlassSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 220f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlCentre(onDismiss: () -> Unit, onOpenLauncherSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controls = remember { SystemControls(context.applicationContext) }
    val nowPlaying = remember { NowPlaying(context.applicationContext) }
    DisposableEffect(controls) {
        controls.start()
        nowPlaying.start()
        onDispose {
            controls.stop()
            nowPlaying.stop()
        }
    }
    LaunchedEffect(controls) { controls.loadSlow() }
    // Pick up changes made in system settings while the user was away.
    LifecycleResumeEffect(controls) {
        controls.refresh()
        onPauseOrDispose { }
    }
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    var page by remember { mutableStateOf(ControlPage.Main) }
    var detailSelector by remember { mutableStateOf<DetailSelector?>(null) }
    var cameraMenu by remember { mutableStateOf(false) }
    var showAddControls by remember { mutableStateOf(false) }

    // Back (or a tap outside) steps out of an expanded module first, as on iOS.
    BackHandler(showAddControls || detailSelector != null || page != ControlPage.Main || cameraMenu) {
        if (showAddControls) showAddControls = false
        else if (detailSelector != null) detailSelector = null
        else if (cameraMenu) cameraMenu = false
        else page = ControlPage.Main
    }

    fun open(intent: Intent) {
        context.startSafely(intent)
    }

    fun ask(message: String, intent: Intent) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        Permissions.open(context, intent, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
    }

    val order = ControlPage.entries
    val density = LocalDensity.current
    val swipeDist = with(density) { 48.dp.toPx() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // iOS Control Centre gestures:
            // 1. Swiping from the last 20% edge of screen closes Control Centre!
            // 2. Swiping vertically anywhere else on mobile changes between pages!
            .pointerInput(page, detailSelector) {
                var startY = 0f
                var travelY = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        startY = offset.y
                        travelY = 0f
                    },
                    onDragEnd = {
                        val screenHeight = size.height.toFloat()
                        val bottom20Threshold = screenHeight * 0.80f

                        if (startY >= bottom20Threshold) {
                            if (travelY < -swipeDist || travelY > swipeDist) {
                                onDismiss()
                            }
                        } else if (detailSelector == null) {
                            val index = order.indexOf(page)
                            if (travelY < -swipeDist && index < order.lastIndex) {
                                page = order[index + 1]
                            } else if (travelY > swipeDist && index > 0) {
                                page = order[index - 1]
                            } else if (travelY > swipeDist && index == 0) {
                                onDismiss()
                            }
                        }
                    },
                ) { _, amount -> travelY += amount }
            },
    ) {
        Scrim({
            if (detailSelector != null) detailSelector = null
            else if (page != ControlPage.Main || cameraMenu) { cameraMenu = false; page = ControlPage.Main }
            else onDismiss()
        }, alpha = 0.22f)

        // Compact like iOS 26: four columns sized to the screen, capped so a tablet gets a corner panel.
        val pageIcons = 34.dp
        val width = if (isTablet) 300.dp else minOf(maxWidth - 32.dp - pageIcons, 360.dp)
        val gap = width * 0.055f
        val unit = (width - gap * 3) / 4
        val double = unit * 2 + gap
        Row(
            Modifier
                .align(if (isTablet) Alignment.TopEnd else Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = if (isTablet) 12.dp else 20.dp, end = if (isTablet) 16.dp else 0.dp, start = if (isTablet) 0.dp else pageIcons),
        ) {
            Column(Modifier.width(width).consumeTaps(), verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallGlassButton(Icons.Rounded.Add, "Add Controls") { showAddControls = true }
                    Spacer(Modifier.weight(1f))
                    SmallGlassButton(Icons.Rounded.Settings, "Settings") {
                        open(Intent(Settings.ACTION_SETTINGS))
                        onDismiss()
                    }
                }
                val bouncyOffsetSpring = spring<IntOffset>(dampingRatio = 0.68f, stiffness = 220f)
                val exitOffsetSpring = spring<IntOffset>(dampingRatio = 0.82f, stiffness = 280f)
                val bouncyFloatSpring = spring<Float>(dampingRatio = 0.68f, stiffness = 220f)
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val goingNext = order.indexOf(targetState) > order.indexOf(initialState)
                        val enter = slideInVertically(
                            animationSpec = bouncyOffsetSpring,
                            initialOffsetY = { h -> if (goingNext) (h * 0.45f).toInt() else (-h * 0.45f).toInt() },
                        ) + fadeIn(animationSpec = bouncyFloatSpring) + scaleIn(animationSpec = bouncyFloatSpring, initialScale = 0.88f)

                        val exit = slideOutVertically(
                            animationSpec = exitOffsetSpring,
                            targetOffsetY = { h -> if (goingNext) (-h * 0.35f).toInt() else (h * 0.35f).toInt() },
                        ) + fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = spring(dampingRatio = 0.82f, stiffness = 280f), targetScale = 0.92f)

                        enter togetherWith exit
                    },
                    label = "controlPage",
                ) { shown ->
                    when (shown) {
                        ControlPage.Main -> Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                ConnectivityModule(
                                    controls = controls,
                                    modifier = Modifier.size(double),
                                    onOpenSelector = { detailSelector = it },
                                    onExpand = { page = ControlPage.Connectivity },
                                )
                                NowPlayingModule(nowPlaying, Modifier.size(double), onExpand = { page = ControlPage.Music }) {
                                    ask("Allow notification access for Glass Launcher to show what's playing", Permissions.notificationAccessIntent(context))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                        CircleButton(Icons.Rounded.ScreenLockRotation, "Rotation Lock", controls.rotationLocked, DestructiveRed, unit) {
                                            if (!controls.toggleRotationLock()) ask("Allow Glass Launcher to modify system settings, then come back", Permissions.writeSettingsIntent(context))
                                        }
                                        CircleButton(
                                            if (controls.silent) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                                            "Silent Mode",
                                            controls.silent,
                                            DestructiveRed,
                                            unit,
                                        ) { controls.toggleSilent() }
                                    }
                                    FocusPill(controls.dndOn, Modifier.size(double, unit)) {
                                        if (!controls.toggleDnd()) ask("Allow Do Not Disturb access for Glass Launcher, then come back", Permissions.dndIntent())
                                    }
                                }
                                VerticalGlassSlider(
                                    value = controls.brightness,
                                    icon = Icons.Rounded.LightMode,
                                    label = "Brightness",
                                    modifier = Modifier.size(unit, double),
                                    onDragStart = { true },
                                ) { controls.changeBrightness(it) }
                                VerticalGlassSlider(
                                    value = controls.volume,
                                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                    label = "Volume",
                                    modifier = Modifier.size(unit, double),
                                ) { controls.changeVolume(it) }
                            }
                            Box {
                                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                    if (controls.torchCameraId != null || !controls.torchReady) {
                                        CircleButton(Icons.Rounded.FlashlightOn, "Flashlight", controls.torchOn, AccentBlue, unit) { controls.toggleTorch() }
                                    } else {
                                        CircleButton(Icons.Rounded.LightMode, "Display", false, AccentBlue, unit) { open(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
                                    }
                                    CircleButton(Icons.Rounded.Cast, "Screen Mirroring", false, AccentBlue, unit) { open(Intent(Settings.ACTION_CAST_SETTINGS)) }
                                    CircleButton(Icons.Rounded.Timer, "Timer", false, AccentOrange, unit) { launchTimer(context) }
                                    CircleButton(Icons.Rounded.QrCodeScanner, "Scan QR Code", false, AccentBlue, unit) { scanQr(context) }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                CircleButton(Icons.Rounded.Calculate, "Calculator", false, AccentOrange, unit) {
                                    launchCalculator(context)
                                }
                                CircleButton(Icons.Rounded.Alarm, "Alarm", false, AccentOrange, unit) {
                                    launchAlarm(context)
                                }
                                // Red while recording; tap again to stop and save.
                                val recording by com.glasslauncher.record.ScreenRecording.recording.collectAsStateWithLifecycle()
                                if (com.glasslauncher.BuildConfig.SCREEN_RECORDING) {
                                    CircleButton(Icons.Rounded.RadioButtonChecked, if (recording) "Stop Recording" else "Screen Recording", recording, DestructiveRed, unit) {
                                        com.glasslauncher.record.ScreenRecording.toggle(context)
                                        if (!recording) onDismiss()
                                    }
                                }
                            }
                        }
                        ControlPage.Connectivity -> ConnectivityPage(controls, gap, onOpenSelector = { detailSelector = it }, ::open)
                        ControlPage.Music -> MusicPage(nowPlaying, controls, gap, ::open)
                    }
                }
            }
            // iOS's page icons on the right edge: favourites, music, connectivity.
            Column(
                Modifier.width(pageIcons).padding(top = unit * 2.4f).consumeTaps(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PageIcon(Icons.Rounded.Favorite, "Controls", page == ControlPage.Main) { page = ControlPage.Main }
                PageIcon(Icons.Rounded.MusicNote, "Now Playing", page == ControlPage.Music) { page = ControlPage.Music }
                PageIcon(Icons.Rounded.SettingsInputAntenna, "Connectivity", page == ControlPage.Connectivity) { page = ControlPage.Connectivity }
            }
        }

        // Details Panel Selector sheet (Wi-Fi, Bluetooth, Hotspot, Cellular)
        if (detailSelector != null) {
            DetailSelectorDialog(
                selector = detailSelector!!,
                controls = controls,
                onDismiss = { detailSelector = null },
                open = ::open,
            )
        }

        // Add Controls Gallery modal sheet
        if (showAddControls) {
            ControlsGallerySheet(
                controls = controls,
                onOpenIntent = ::open,
                onDismiss = { showAddControls = false }
            )
        }

        // Apple Home Bar indicator at bottom center: tap or swipe closes Control Centre
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 10.dp)
                .width(134.dp)
                .height(5.dp)
                .clip(Capsule)
                .background(Color.White.copy(alpha = 0.5f))
                .clickable { onDismiss() },
        )
    }
}

/** Glass that dips a little under the finger and springs back, as iOS controls do. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.glassPress(label: String, onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, GlassSpring, label = "press")
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(interactionSource = interaction, indication = null, onClickLabel = label, onLongClick = onLongClick, onClick = onClick)
}

@Composable
private fun SmallGlassButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).liquidGlass(Capsule, style = GlassStyle.Clear).glassPress(label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PageIcon(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.20f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 260f),
        label = "pageIconScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.45f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "pageIconAlpha",
    )
    Icon(
        icon,
        contentDescription = label,
        tint = Color.White.copy(alpha = alpha),
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glassPress(label, onClick = onClick),
    )
}

/** The connectivity block: big Airplane, Internet and Wi-Fi circles, plus four small ones. Individual circles toggle directly; long press opens selector. */
@Composable
private fun ConnectivityModule(
    controls: SystemControls,
    modifier: Modifier,
    onOpenSelector: (DetailSelector) -> Unit,
    onExpand: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun open(intent: Intent) = context.startSafely(intent)

    BoxWithConstraints(modifier.liquidGlass(SmoothShape(ModuleCorner), ModuleCorner, GlassStyle.Regular)) {
        val pad = maxWidth * 0.08f
        val big = (maxWidth - pad * 3) / 2
        val small = (big - pad) / 2
        Column(Modifier.padding(pad), verticalArrangement = Arrangement.spacedBy(pad)) {
            Row(horizontalArrangement = Arrangement.spacedBy(pad)) {
                StateCircle(
                    icon = Icons.Rounded.AirplanemodeActive,
                    active = controls.airplaneOn,
                    activeColor = AccentOrange,
                    size = big,
                    onClick = {
                        scope.launch {
                            if (!controls.toggleAirplaneMode()) open(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
                        }
                    },
                    onLongClick = onExpand,
                )
                StateCircle(
                    icon = Icons.Rounded.Language,
                    active = controls.online,
                    activeColor = AccentGreen,
                    size = big,
                    onClick = {
                        scope.launch {
                            if (!controls.toggleMobileData()) open(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                        }
                    },
                    onLongClick = { onOpenSelector(DetailSelector.Cellular) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(pad)) {
                StateCircle(
                    icon = Icons.Rounded.Wifi,
                    active = controls.wifiOn,
                    activeColor = AccentBlue,
                    size = big,
                    onClick = {
                        scope.launch {
                            if (!controls.toggleWifi()) {
                                runCatching { open(Intent(Settings.Panel.ACTION_WIFI)) }.getOrElse {
                                    open(Intent(Settings.ACTION_WIFI_SETTINGS))
                                }
                            }
                        }
                    },
                    onLongClick = { onOpenSelector(DetailSelector.Wifi) },
                )
                Column(verticalArrangement = Arrangement.spacedBy(pad)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(pad)) {
                        StateCircle(
                            icon = Icons.Rounded.SignalCellularAlt,
                            active = controls.mobileData,
                            activeColor = AccentGreen,
                            size = small,
                            onClick = {
                                scope.launch {
                                    if (!controls.toggleMobileData()) open(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                                }
                            },
                            onLongClick = { onOpenSelector(DetailSelector.Cellular) },
                        )
                        StateCircle(
                            icon = Icons.Rounded.Bluetooth,
                            active = controls.bluetoothOn,
                            activeColor = AccentBlue,
                            size = small,
                            onClick = {
                                scope.launch {
                                    if (!controls.toggleBluetooth()) open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                }
                            },
                            onLongClick = { onOpenSelector(DetailSelector.Bluetooth) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(pad)) {
                        StateCircle(
                            icon = Icons.Rounded.Link,
                            active = controls.hotspot,
                            activeColor = AccentGreen,
                            size = small,
                            onClick = {
                                scope.launch {
                                    if (!controls.toggleHotspot()) open(hotspotSettingsIntent(context))
                                }
                            },
                            onLongClick = { onOpenSelector(DetailSelector.Hotspot) },
                        )
                        StateCircle(
                            icon = Icons.Rounded.VpnLock,
                            active = controls.vpn,
                            activeColor = AccentBlue,
                            size = small,
                            onClick = { open(Intent(Settings.ACTION_VPN_SETTINGS)) },
                            onLongClick = onExpand,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StateCircle(
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.90f else 1f, GlassSpring, label = "circlePress")

    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (active) activeColor else Color.White.copy(alpha = 0.16f))
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.46f))
    }
}

/** Helper to get the actual connected Wi-Fi SSID across all modern Android versions. */
fun getConnectedWifiSsid(context: android.content.Context): String? {
    if (!isWifiOn(context)) return null
    try {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm?.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(network) ?: return null
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wifiInfo = caps.transportInfo as? WifiInfo
            val ssid = wifiInfo?.ssid?.trim('"')
            if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") return ssid
        }
        @Suppress("DEPRECATION")
        val wm = context.applicationContext.getSystemService(android.net.wifi.WifiManager::class.java)
        @Suppress("DEPRECATION")
        val ssid = wm?.connectionInfo?.ssid?.trim('"')
        if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") return ssid
    } catch (_: Exception) {}
    return null
}

/**
 * The expanded connectivity view matching iOS Control Centre:
 * - Smaller cards
 * - 1-tap on/off
 * - Long-hold / click opens the Wi-Fi or Bluetooth popover card
 */
@Composable
private fun ConnectivityPage(
    controls: SystemControls,
    gap: androidx.compose.ui.unit.Dp,
    onOpenSelector: (DetailSelector) -> Unit,
    open: (Intent) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun state(on: Boolean) = if (on) "On" else "Off"
    val connectedSsid = controls.wifiSsid

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        WideToggle(
            icon = Icons.Rounded.AirplanemodeActive,
            title = "Airplane Mode",
            subtitle = state(controls.airplaneOn),
            active = controls.airplaneOn,
            activeColor = AccentOrange,
            onToggle = {
                scope.launch {
                    if (!controls.toggleAirplaneMode()) open(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
                }
            },
            onSelect = { open(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            SquareToggle(
                icon = Icons.Rounded.Wifi,
                title = "Wi-Fi",
                subtitle = connectedSsid ?: if (controls.wifiOn) "On" else "Off",
                active = controls.wifiOn,
                activeColor = AccentBlue,
                modifier = Modifier.weight(1f),
                onToggle = {
                    scope.launch {
                        if (!controls.toggleWifi()) {
                            runCatching { open(Intent(Settings.Panel.ACTION_WIFI)) }.getOrElse {
                                open(Intent(Settings.ACTION_WIFI_SETTINGS))
                            }
                        }
                    }
                },
                onSelect = { onOpenSelector(DetailSelector.Wifi) },
            )
            SquareToggle(
                icon = Icons.Rounded.CellTower,
                title = "AirDrop",
                subtitle = "Receiving Off",
                active = false,
                activeColor = AccentGreen,
                modifier = Modifier.weight(1f),
                onToggle = { },
                onSelect = { onOpenSelector(DetailSelector.Bluetooth) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            SquareToggle(
                icon = Icons.Rounded.SignalCellularAlt,
                title = "Mobile Data",
                subtitle = state(controls.mobileData),
                active = controls.mobileData,
                activeColor = AccentGreen,
                modifier = Modifier.weight(1f),
                onToggle = {
                    scope.launch {
                        if (!controls.toggleMobileData()) open(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                    }
                },
                onSelect = { onOpenSelector(DetailSelector.Cellular) },
            )
            SquareToggle(
                icon = Icons.Rounded.Bluetooth,
                title = "Bluetooth",
                subtitle = state(controls.bluetoothOn),
                active = controls.bluetoothOn,
                activeColor = AccentBlue,
                modifier = Modifier.weight(1f),
                onToggle = {
                    scope.launch {
                        if (!controls.toggleBluetooth()) open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                },
                onSelect = { onOpenSelector(DetailSelector.Bluetooth) },
            )
        }
        WideToggle(
            icon = Icons.Rounded.Link,
            title = "Personal Hotspot",
            subtitle = state(controls.hotspot),
            active = controls.hotspot,
            activeColor = AccentGreen,
            onToggle = {
                scope.launch {
                    if (!controls.toggleHotspot()) open(hotspotSettingsIntent(context))
                }
            },
            onSelect = { onOpenSelector(DetailSelector.Hotspot) },
        )
        WideToggle(
            icon = Icons.Rounded.VpnLock,
            title = "VPN",
            subtitle = state(controls.vpn),
            active = controls.vpn,
            activeColor = AccentBlue,
            onToggle = { open(Intent(Settings.ACTION_VPN_SETTINGS)) },
            onSelect = { open(Intent(Settings.ACTION_VPN_SETTINGS)) },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WideToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    activeColor: Color,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .liquidGlass(Capsule, style = GlassStyle.Clear)
            .combinedClickable(
                onClick = {
                    AppleTaptic.medium(context)
                    onToggle()
                },
                onLongClick = {
                    AppleTaptic.heavy(context)
                    onSelect()
                }
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StateCircle(icon, active, activeColor, 36.dp, onClick = onToggle)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = PanelCaptionStyle.copy(fontSize = 12.sp))
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = "Details", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SquareToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier
            .height(96.dp)
            .liquidGlass(SmoothShape(22.dp), 22.dp, GlassStyle.Regular)
            .combinedClickable(
                onClick = {
                    AppleTaptic.medium(context)
                    onToggle()
                },
                onLongClick = {
                    AppleTaptic.heavy(context)
                    onSelect()
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StateCircle(icon, active, activeColor, 36.dp, onClick = onToggle)
            Box(
                Modifier
                    .size(24.dp)
                    .clickable {
                        AppleTaptic.selection(context)
                        onSelect()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.UnfoldMore,
                    contentDescription = "Expand",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Column {
            Text(title, style = PanelBodyStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
            Text(
                if (subtitle.endsWith("⇕")) subtitle else "$subtitle ⇕",
                style = PanelCaptionStyle.copy(fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f)),
                maxLines = 1,
            )
        }
    }
}

/**
 * Compact iOS floating popover card for Wi-Fi and Bluetooth
 * Matching screenshots: lists connected network with tick on connected, scanned networks, Loading..., and settings link.
 */
@Composable
private fun DetailSelectorDialog(
    selector: DetailSelector,
    controls: SystemControls,
    onDismiss: () -> Unit,
    open: (Intent) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth(0.82f)
                .liquidGlass(SmoothShape(24.dp), 24.dp, GlassStyle.Dark)
                .clickable(enabled = false) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (selector) {
                DetailSelector.Wifi -> {
                    val wifiManager = remember { context.applicationContext.getSystemService(android.net.wifi.WifiManager::class.java) }
                    val currentSsid = controls.wifiSsid
                    var isScanning by remember { mutableStateOf(true) }
                    val scanResults = remember(controls.wifiOn) {
                        if (controls.wifiOn) {
                            runCatching {
                                wifiManager?.startScan()
                                wifiManager?.scanResults
                                    ?.mapNotNull { it.SSID?.trim('"') }
                                    ?.filter { it.isNotBlank() && it != currentSsid }
                                    ?.distinct()
                                    ?.take(4)
                            }.getOrNull().orEmpty()
                        } else emptyList()
                    }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1200)
                        isScanning = false
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Connected Wi-Fi with tick on connected
                        if (currentSsid != null) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { open(Intent(Settings.Panel.ACTION_WIFI)) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = "Connected", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(currentSsid, style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Other scanned networks
                        scanResults.forEach { ssid ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { open(Intent(Settings.Panel.ACTION_WIFI)) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width(24.dp))
                                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(ssid, style = PanelBodyStyle.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Loading spinner
                        if (isScanning && scanResults.isEmpty()) {
                            Row(
                                Modifier.padding(start = 24.dp, top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Loading...", style = PanelCaptionStyle.copy(fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f)))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(0.6.dp).background(Color.White.copy(alpha = 0.15f)))
                    Spacer(Modifier.height(2.dp))

                    Text(
                        "Wi-Fi settings...",
                        style = PanelBodyStyle.copy(fontSize = 15.sp, color = Color.White),
                        modifier = Modifier
                            .clickable {
                                open(Intent(Settings.ACTION_WIFI_SETTINGS))
                                onDismiss()
                            }
                            .padding(vertical = 2.dp)
                    )
                }

                DetailSelector.Bluetooth -> {
                    val bluetoothManager = remember { context.getSystemService(android.bluetooth.BluetoothManager::class.java) }
                    val adapter = bluetoothManager?.adapter
                    val bondedDevices = remember(controls.bluetoothOn) {
                        if (controls.bluetoothOn) {
                            runCatching { adapter?.bondedDevices?.toList()?.take(4) }.getOrNull().orEmpty()
                        } else emptyList()
                    }
                    var isScanning by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1200)
                        isScanning = false
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (bondedDevices.isNotEmpty()) {
                            bondedDevices.forEach { dev ->
                                val devName = runCatching { dev.name }.getOrNull()?.ifBlank { dev.address } ?: dev.address
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                                        .padding(vertical = 4.dp),
                                ) {
                                    Text(devName, style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Not Connected", style = PanelCaptionStyle.copy(fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)))
                                }
                            }
                        } else {
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text("No Paired Devices", style = PanelBodyStyle.copy(fontSize = 15.sp))
                            }
                        }

                        if (isScanning) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Loading...", style = PanelCaptionStyle.copy(fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f)))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(0.6.dp).background(Color.White.copy(alpha = 0.15f)))
                    Spacer(Modifier.height(2.dp))

                    Text(
                        "Bluetooth Settings...",
                        style = PanelBodyStyle.copy(fontSize = 15.sp, color = Color.White),
                        modifier = Modifier
                            .clickable {
                                open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                onDismiss()
                            }
                            .padding(vertical = 2.dp)
                    )
                }

                DetailSelector.Hotspot, DetailSelector.Cellular -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (selector == DetailSelector.Hotspot) "Personal Hotspot" else "Cellular", style = PanelTitleStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                        Text(if (selector == DetailSelector.Hotspot) "Tap to open Hotspot settings" else "Tap to open Cellular settings", style = PanelCaptionStyle)
                        Box(Modifier.fillMaxWidth().height(0.6.dp).background(Color.White.copy(alpha = 0.15f)))
                        Text(
                            "Settings...",
                            style = PanelBodyStyle.copy(fontSize = 15.sp, color = Color.White),
                            modifier = Modifier.clickable {
                                open(if (selector == DetailSelector.Hotspot) hotspotSettingsIntent(context) else Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}
/** The expanded Now Playing card: artwork, the song, big transport controls, volume and the output picker. */
@Composable
private fun MusicPage(state: NowPlaying, controls: SystemControls, gap: androidx.compose.ui.unit.Dp, open: (Intent) -> Unit) {
    Column(
        Modifier.fillMaxWidth().liquidGlass(SmoothShape(44.dp), 44.dp, GlassStyle.Clear).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            val art = state.artwork
            if (art != null) {
                Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(64.dp))
            }
        }
        Column(Modifier.fillMaxWidth()) {
            Text(state.title ?: "Not Playing", style = PanelBodyStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (state.artist != null) Text(state.artist.orEmpty(), style = PanelCaptionStyle.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            MediaButton(Icons.Rounded.FastRewind, "Previous", 40) { state.previous() }
            MediaButton(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "Pause" else "Play", 56) { state.playPause() }
            MediaButton(Icons.Rounded.FastForward, "Next", 40) { state.next() }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            Slider(
                value = controls.volume,
                onValueChange = controls::changeVolume,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
            )
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
        Row(
            Modifier.height(40.dp).liquidGlass(Capsule, style = GlassStyle.Clear).glassPress("Output") { open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Cast, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text("Output", style = PanelBodyStyle.copy(fontSize = 15.sp), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** Touch and hold Camera: jump straight to a selfie, a video, or a photo. */
@Composable
private fun CameraMenu(modifier: Modifier, onPick: (Intent) -> Unit) {
    Column(modifier.liquidGlass(SmoothShape(22.dp), 22.dp, GlassStyle.Regular).padding(vertical = 6.dp)) {
        CameraOption(Icons.Rounded.CameraFront, "Selfie") {
            // Most camera apps honour one of these extras for the front camera.
            onPick(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    .putExtra("android.intent.extras.CAMERA_FACING", 1)
                    .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    .putExtra("android.intent.extra.USE_FRONT_CAMERA", true),
            )
        }
        CameraOption(Icons.Rounded.Videocam, "Video") { onPick(Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)) }
        CameraOption(Icons.Rounded.PhotoCamera, "Photo") { onPick(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) }
    }
}

@Composable
private fun CameraOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().glassPress(label, onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(label, style = PanelBodyStyle.copy(fontSize = 15.sp), modifier = Modifier.padding(start = 12.dp))
    }
}

/** The compact Now Playing module; tapping outside the buttons expands it to the full player. */
@Composable
private fun NowPlayingModule(state: NowPlaying, modifier: Modifier, onExpand: () -> Unit, onRequestAccess: () -> Unit) {
    Column(
        modifier
            .liquidGlass(SmoothShape(ModuleCorner), ModuleCorner, GlassStyle.Regular)
            .glassPress("Now Playing", onClick = if (!state.allowed && Permissions.notificationsSupported) onRequestAccess else onExpand)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                val art = state.artwork
                if (art != null) {
                    Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }
        Column {
            Text(
                state.title ?: if (state.allowed) "Not Playing" else "Now Playing",
                style = PanelBodyStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = state.artist ?: if (state.allowed || !Permissions.notificationsSupported) null else "Tap to allow"
            if (sub != null) Text(sub, style = PanelCaptionStyle.copy(fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            MediaButton(Icons.Rounded.FastRewind, "Previous", 26) { state.previous() }
            MediaButton(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "Pause" else "Play", 34) { state.playPause() }
            MediaButton(Icons.Rounded.FastForward, "Next", 26) { state.next() }
        }
    }
}

@Composable
private fun MediaButton(icon: ImageVector, label: String, sizeDp: Int, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = label,
        tint = Color.White,
        modifier = Modifier.size(sizeDp.dp).glassPress(label, onClick = onClick),
    )
}

/** iOS 26 single control: a clear glass circle that turns white with a coloured glyph when on. */
@Composable
private fun CircleButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    size: androidx.compose.ui.unit.Dp,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .then(if (active) Modifier.clip(CircleShape).background(Color.White) else Modifier.liquidGlass(Capsule, style = GlassStyle.Clear))
            .glassPress(label, onLongClick, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (active) activeColor else Color.White, modifier = Modifier.size(size * 0.4f))
    }
}

@Composable
private fun FocusPill(active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    BoxWithConstraints(
        modifier
            .then(if (active) Modifier.clip(CircleShape).background(Color.White) else Modifier.liquidGlass(Capsule, style = GlassStyle.Clear))
            .glassPress("Focus", onClick = onClick),
    ) {
        val circle = maxHeight * 0.62f
        Row(Modifier.fillMaxSize().padding(horizontal = maxHeight * 0.19f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(circle).clip(CircleShape).background(if (active) AccentPurple else Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = Color.White, modifier = Modifier.size(circle * 0.55f))
            }
            Text(
                "Focus",
                style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.Black else Color.White),
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

/** iOS tall slider: touch anywhere and drag; the white fill follows with a spring. */
@Composable
private fun VerticalGlassSlider(
    value: Float,
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    onDragStart: () -> Boolean = { true },
    onValueChange: (Float) -> Unit,
) {
    val change by rememberUpdatedState(onValueChange)
    val allowed by rememberUpdatedState(onDragStart)
    val shown by animateFloatAsState(value.coerceIn(0f, 1f), spring(dampingRatio = 1f, stiffness = 900f), label = "level")
    Box(
        modifier
            .liquidGlass(SmoothShape(ModuleCorner), ModuleCorner, GlassStyle.Regular)
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                fun valueAt(y: Float) = (1f - y / size.height).coerceIn(0f, 1f)
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!allowed()) return@awaitEachGesture
                    change(valueAt(down.position.y))
                    drag(down.id) {
                        it.consume()
                        change(valueAt(it.position.y))
                    }
                }
            },
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(shown)
                .background(Color.White),
        )
        Icon(
            icon,
            contentDescription = null,
            tint = if (shown > 0.15f) Color(0xFF48484A) else Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).size(24.dp),
        )
    }
}

private val ModuleCorner = 30.dp

/** Opens a QR scanner: Google Lens where available, otherwise the camera (which reads QR codes on most phones). */
internal fun scanQr(context: android.content.Context) {
    val lens = listOf(
        Intent().setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.search.lens.LensShortcutActivity"),
        Intent().setClassName("com.google.ar.lens", "com.google.vr.apps.ornament.app.lens.LensLauncherActivity"),
    )
    if (lens.none { context.startSafely(it) }) context.startSafely(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
}
