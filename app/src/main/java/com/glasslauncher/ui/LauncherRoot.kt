package com.glasslauncher.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.areStatusBarsVisible
import android.Manifest
import android.os.Build
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glasslauncher.LauncherViewModel
import com.glasslauncher.data.AppEntry
import com.glasslauncher.data.GlassLook
import com.glasslauncher.data.AppGroup
import com.glasslauncher.data.NotificationHub
import com.glasslauncher.data.QuickAction
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

private enum class Overlay { None, Search, ControlCentre, Notifications, Settings, Widgets, TodayWidgets, WallpaperCustomizer, Themes }

/** System moments the Home Screen reacts to: Home pressed, device unlocked, back from an app. */
@Immutable
class LauncherEvents(val homePresses: Flow<Unit>, val unlocks: Flow<Unit>, val returns: Flow<Unit>, val shadePulls: Flow<Unit>, val panels: Flow<String>)

@Composable
fun LauncherRoot(vm: LauncherViewModel, events: LauncherEvents) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val homeApps by vm.homeApps.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()
    val hiddenApps by vm.hiddenApps.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val icons by vm.icons.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val wallpaper by vm.wallpaper.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isNight = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val isTablet = configuration.smallestScreenWidthDp >= 600
    LaunchedEffect(isNight) { vm.setSystemDark(isNight) }

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var overlay by remember { mutableStateOf(Overlay.None) }
    var menu by remember { mutableStateOf<IconMenu?>(null) }
    var openGroup by remember { mutableStateOf<AppGroup?>(null) }
    // Kept after closing so the folder can animate out with its content.
    var lastGroup by remember { mutableStateOf<AppGroup?>(null) }
    var removing by remember { mutableStateOf<AppEntry?>(null) }
    var editing by remember { mutableStateOf(false) }
    // Bumped to send the home pager back to the first Home Screen page.
    var homeTick by remember { mutableIntStateOf(0) }
    var editingNote by remember { mutableStateOf(false) }
    var editingNoteIndex by remember { mutableStateOf<Int?>(null) }
    var isNewNoteMode by remember { mutableStateOf(false) }
    var addingReminder by remember { mutableStateOf(false) }
    var editingReminderIndex by remember { mutableStateOf<Int?>(null) }
    // Tapping the Search pill shows suggestions; swiping down goes straight to the A–Z list.
    var searchSuggestions by remember { mutableStateOf(false) }
    // The app whose icon is being replaced while the photo picker is open.
    var iconTarget by remember { mutableStateOf<AppEntry?>(null) }
    // How far into Today View / App Library the pager is; the wallpaper blurs by this much.
    val pageBlur = remember { mutableFloatStateOf(0f) }
    val homePage = remember { mutableIntStateOf(0) }
    // Re-checks permissions whenever the user comes back from a system settings screen.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeTick++
        vm.refreshWeather()
        onPauseOrDispose { }
    }

    fun closeAll(): Boolean {
        val anyOpen = overlay != Overlay.None || menu != null || openGroup != null || removing != null || editing || editingNote || addingReminder
        editingNote = false
        addingReminder = false
        editingReminderIndex = null
        overlay = Overlay.None
        menu = null
        openGroup = null
        removing = null
        editing = false
        return anyOpen
    }

    // iOS unlock & return: apps and widgets assemble from depth and inward with an elastic Apple spring;
    // swiping up from an app to return home springs smoothly into place from below.
    val unlockSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 300f)
    val returnSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 300f)
    val entrance = remember { Animatable(1f) }
    val entranceState = remember { entrance.asState() }
    val scope = rememberCoroutineScope()
    var isReturnEntrance by remember { mutableStateOf(false) }
    var launchedApp by remember { mutableStateOf(false) }
    // The glass lens rests while the Home Screen zooms, so every frame is a cheap one.
    val entering = remember { derivedStateOf { entrance.isRunning } }
    LaunchedEffect(Unit) {
        isReturnEntrance = false
        entrance.animateTo(1f, unlockSpring)
    }
    LaunchedEffect(events) {
        events.unlocks.collect {
            closeAll()
            isReturnEntrance = false
            entrance.snapTo(0.05f)
            entrance.animateTo(1f, unlockSpring)
        }
    }
    // Home and Back behave like iOS: first close whatever is open, then return to the first page.
    LaunchedEffect(events) { events.homePresses.collect { if (!closeAll()) homeTick++ } }
    BackHandler { if (!closeAll()) homeTick++ }

    // Glass status bar: hide Samsung/OEM bar on the Home Screen unless device has native Dynamic Island
    // support (OnePlus Fluid Cloud, Realme Mini Capsule, Motorola Smart Capsule) where the system status bar
    // must remain visible so the native capsule renders on the home screen just like in other apps.
    val nativeIsland = hasNativeDynamicIsland()
    val glassBar = settings.glassStatusBar && !nativeIsland
    val lastTouch = remember { longArrayOf(0L, 0L, 0L) } // x, y, time of the latest touch-down
    val edgePx = with(density) { 56.dp.toPx() }
    val resumedAt = remember { longArrayOf(0L) }
    LifecycleResumeEffect(glassBar, nativeIsland) {
        resumedAt[0] = System.currentTimeMillis()
        if (launchedApp) {
            launchedApp = false
            isReturnEntrance = true
            scope.launch {
                entrance.snapTo(0.15f)
                entrance.animateTo(1f, returnSpring)
                isReturnEntrance = false
            }
        }
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (nativeIsland) {
                    show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                } else if (glassBar) {
                    hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                } else {
                    show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }
        }
        onPauseOrDispose { }
    }
    // The top-edge helper asked for a panel (a swipe down from the top of the screen).
    LaunchedEffect(events) {
        events.panels.collect { panel ->
            if (!settings.onboarded) return@collect
            closeAll()
            if (panel == com.glasslauncher.MainActivity.PANEL_NOTIFICATIONS && Permissions.notificationsSupported) {
                overlay = Overlay.Notifications
            } else {
                overlay = if (isTablet) Overlay.ControlCentre else Overlay.Notifications
            }
        }
    }
    // The system shade was pulled and folded away by the helper: open our panel in its place.
    LaunchedEffect(events) {
        events.shadePulls.collect {
            if (overlay == Overlay.None && !editing && settings.onboarded) {
                val sawTouch = System.currentTimeMillis() - lastTouch[2] < 1_500
                val fromLeft = sawTouch && lastTouch[0] < rootSize.width * 0.5f
                if (fromLeft && Permissions.notificationsSupported) {
                    overlay = Overlay.Notifications
                } else {
                    overlay = if (isTablet) Overlay.ControlCentre else Overlay.Notifications
                }
            }
        }
    }
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    val barsShowing = WindowInsets.areStatusBarsVisible
    LaunchedEffect(barsShowing, glassBar, nativeIsland) {
        if (nativeIsland) return@LaunchedEffect
        if (!glassBar || !barsShowing) return@LaunchedEffect
        val now = System.currentTimeMillis()
        // The bar also flashes for a moment when we come back from an app; that isn't a swipe.
        val justResumed = now - resumedAt[0] < 900
        if (!justResumed && overlay == Overlay.None && !editing && settings.onboarded) {
            // The system often swallows the edge swipe itself, so its start point is only used when we saw it.
            val sawTouch = now - lastTouch[2] < 1_500 && lastTouch[1] < edgePx
            val fromLeft = sawTouch && lastTouch[0] < rootSize.width * 0.5f
            overlay = if (fromLeft && Permissions.notificationsSupported) Overlay.Notifications else if (isTablet) Overlay.ControlCentre else Overlay.Notifications
        }
        (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view).hide(androidx.core.view.WindowInsetsCompat.Type.statusBars()) }
    }

    val isLightWallpaper = wallpaper?.isLight == true && !(isNight && settings.dimWallpaperInDark)
    LaunchedEffect(isLightWallpaper) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = isLightWallpaper
            isAppearanceLightNavigationBars = isLightWallpaper
        }
    }

    val wallpaperShift = remember { mutableStateOf(Offset.Zero) }
    if (settings.perspectiveZoom) PerspectiveZoom(context, with(density) { 18.dp.toPx() }, wallpaperShift)
    val zoom = if (settings.perspectiveZoom) 1.1f else 1f

    val locationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.refreshWeather(force = true) }
    LaunchedEffect(settings.setupCompleted) {
        if (settings.setupCompleted && !Permissions.hasFineLocation(context) && settings.weatherPlace == null) {
            locationRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    val activityRecognitionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }
    LaunchedEffect(settings.setupCompleted) {
        if (settings.setupCompleted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Permissions.hasActivityRecognition(context)) {
            activityRecognitionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
    val featuredNames by vm.featuredPhotos.collectAsStateWithLifecycle()
    val choosePhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
        if (uris.isNotEmpty()) vm.setFeaturedPhotos(uris)
    }
    val calendarRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }
    val customIcons by vm.customIconKeys.collectAsStateWithLifecycle()
    val pickIcon = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val app = iconTarget
        if (uri != null && app != null) vm.setCustomIcon(app, uri)
        iconTarget = null
    }
    val notificationAccess = remember(resumeTick) { Permissions.hasNotificationAccess(context) }
    val widgetEnvironment = WidgetEnvironment(
        weather = weather,
        weatherAvailable = remember(resumeTick, settings.weatherPlace) { vm.hasWeatherLocation() },
        onRequestLocation = { locationRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
        onRefreshWeather = { vm.refreshWeather() },
        featuredPhotos = remember(featuredNames) { featuredNames.map(vm::featuredFile) },
        onChoosePhotos = { choosePhotos.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        calendarAllowed = remember(resumeTick) { Permissions.hasCalendar(context) },
        onRequestCalendar = { calendarRequest.launch(Manifest.permission.READ_CALENDAR) },
        refreshKey = resumeTick,
        note = settings.noteText,
        notesList = settings.notesList,
        onEditNote = {
            editingNoteIndex = 0
            isNewNoteMode = false
            editingNote = true
        },
        onEditNoteIndex = { index ->
            editingNoteIndex = index
            isNewNoteMode = false
            editingNote = true
        },
        onNewNote = {
            editingNoteIndex = null
            isNewNoteMode = true
            editingNote = true
        },
        onDeleteNote = vm::deleteNote,
        reminders = settings.reminders,
        onAddReminder = {
            editingReminderIndex = null
            addingReminder = true
        },
        onEditReminder = { _ ->
            editingReminderIndex = null
            addingReminder = true
        },
        onCompleteReminder = vm::completeReminder,
    )

    val onLaunch: (AppEntry, Rect) -> Unit = remember(view) {
        { app, bounds ->
            launchedApp = true
            vm.launch(app, view, bounds)
        }
    }
    val onMenu: (AppEntry, Rect) -> Unit = remember { { app, bounds -> menu = IconMenu(app, bounds) } }
    val folderLook = IconLook(if (isTablet) 68.dp else 60.dp, settings.iconStyle, showLabel = true, blurUnder = false, labelSize = if (isTablet) 13.sp else 12.sp)
    val tuning = GlassTuning(settings.glassLook, settings.widgetFrost, settings.dockFrost, settings.iconStyle, settings.tintColor)

    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(
            LocalWallpaperShift provides wallpaperShift,
            LocalGlassTuning provides tuning,
            LocalWidgetEnvironment provides widgetEnvironment,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .onSizeChanged { rootSize = it }
                    // Remembers where each touch starts (without consuming it) to tell a top-right swipe from a top-left one.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            lastTouch[0] = down.position.x.toLong()
                            lastTouch[1] = down.position.y.toLong()
                            lastTouch[2] = System.currentTimeMillis()
                        }
                    },
            ) {
                val dim = if (isNight && settings.dimWallpaperInDark) 0.25f else 0f
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val e = entrance.value
                            val settle = if (isReturnEntrance) {
                                1f + (1f - e) * 0.02f
                            } else {
                                1f + (1f - e) * 0.05f
                            }
                            scaleX = settle
                            scaleY = settle
                        },
                ) {
                    Crossfade(targetState = wallpaper, animationSpec = tween(450), label = "wallpaper") {
                        WallpaperLayer(it?.sharp, dim, zoom, blur = settings.wallpaperBlur, light = it?.light, heavy = it?.heavy)
                    }
                    // Read inside the draw lambda only, so swiping redraws this layer without recomposing.
                    BlurredWallpaperOverlay(if (tuning.widgetFrost < 0.3f) wallpaper?.light else wallpaper?.heavy, zoom, tuning) { pageBlur.floatValue }
                }

                // Glass never shows a sharper wallpaper than the blurred one behind it: with wallpaper blur on, its clearest
                // copy is the lightly (or heavily) blurred one.
                val blurLevel = settings.wallpaperBlur
                val backdrop = remember(wallpaper, rootSize, zoom, blurLevel > 0.5f, blurLevel > 0.05f) {
                    wallpaper?.let {
                        val sharp = when {
                            blurLevel > 0.5f -> it.heavy
                            blurLevel > 0.05f -> it.light
                            else -> it.sharp
                        }
                        GlassBackdrop(sharp, if (blurLevel > 0.5f) it.heavy else it.light, it.heavy, rootSize, zoom)
                    }
                }
                CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
                    // Panels open over a blurred Home Screen, like iOS, with the pre-blurred wallpaper fading in on top.
                    // Control Centre, Notification Centre, and Themes leave the Home Screen visible underneath.
                    val covered = (overlay != Overlay.None && overlay != Overlay.Themes) || openGroup != null || menu != null || removing != null || !settings.onboarded
                    val cover by animateFloatAsState(if (covered) 1f else 0f, spring(dampingRatio = 1f, stiffness = 320f), label = "homeCover")
                    val shrinks = covered && menu == null && overlay != Overlay.ControlCentre && overlay != Overlay.Notifications && overlay != Overlay.Themes
                    val homeScale by animateFloatAsState(if (shrinks) 0.94f else 1f, spring(dampingRatio = 0.9f, stiffness = 420f), label = "homeScale")
                    val blurPx = with(density) { 24.dp.toPx() }

                    CompositionLocalProvider(
                        LocalGlassMotion provides entering,
                        LocalUnlockProgress provides entranceState,
                    ) {
                    HomeScreen(
                        allApps = apps,
                        hiddenApps = hiddenApps,
                        homeApps = homeApps,
                        groups = groups,
                        suggestions = suggestions,
                        recents = recents,
                        icons = icons,
                        badges = badges,
                        settings = settings,
                        homeTick = homeTick,
                        editing = editing,
                        onLaunch = onLaunch,
                        onMenu = onMenu,
                        onOpenGroup = {
                            openGroup = it
                            lastGroup = it
                        },
                        onOpenSearch = {
                            searchSuggestions = true
                            overlay = Overlay.Search
                        },
                        onSwipeDown = { start ->
                            if (!editing) {
                                val topBand = with(density) { 64.dp.toPx() }
                                if (start.y <= topBand) {
                                    // Top edge swipe: center (30% to 70%) is Dynamic Island and is strictly not swipable
                                    when {
                                        start.x > rootSize.width * 0.70f -> {
                                            overlay = if (isTablet) Overlay.ControlCentre else Overlay.Notifications
                                        }
                                        start.x < rootSize.width * 0.30f -> {
                                            if (Permissions.notificationsSupported) overlay = Overlay.Notifications else expandNotifications(context)
                                        }
                                        else -> Unit // Center Dynamic Island zone: do not swipe
                                    }
                                } else {
                                    overlay = Overlay.Search.also { searchSuggestions = false }
                                }
                            }
                        },
                        onStartEditing = { editing = true },
                        onStopEditing = { editing = false },
                        onRemove = { removing = it },
                        onSaveLayout = vm::saveHomeLayout,
                        arrangeRequests = vm.arrangeRequests,
                        onDropToDock = vm::addToDock,
                        onRemoveWidget = vm::removeHomeWidget,
                        statusBar = !nativeIsland && settings.glassStatusBar,
                        onOpenControlCentre = { overlay = if (isTablet) Overlay.ControlCentre else Overlay.Notifications },
                        onOpenNotifications = { if (Permissions.notificationsSupported) overlay = Overlay.Notifications },
                        onOpenWidgetGallery = { overlay = Overlay.Widgets },
                        onSaveToday = vm::setTodayWidgets,
                        onOpenTodayGallery = { overlay = Overlay.TodayWidgets },
                        onOpenSettings = {
                            editing = false
                            overlay = Overlay.Settings
                        },
                        onOpenWallpaper = {
                            editing = false
                            overlay = Overlay.WallpaperCustomizer
                        },
                        onOpenThemes = {
                            editing = false
                            overlay = Overlay.Themes
                        },
                        onOpenCustomise = {
                            editing = false
                            overlay = Overlay.Settings
                        },
                        onLayout = { vm.setDockLimit(it.maxDock) },
                        pageBlur = pageBlur,
                        homePage = homePage,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val e = entrance.value
                                val isReturn = isReturnEntrance
                                if (isReturn) {
                                    val scale = 0.93f + e * 0.07f
                                    scaleX = homeScale * scale
                                    scaleY = homeScale * scale
                                    translationY = (1f - e) * 80f
                                    alpha = (0.35f + e * 0.65f).coerceIn(0f, 1f)
                                } else {
                                    val scale = 0.91f + e * 0.09f
                                    scaleX = homeScale * scale
                                    scaleY = homeScale * scale
                                    translationY = (1f - e) * 50f
                                    alpha = (0.25f + e * 0.75f).coerceIn(0f, 1f)
                                }
                                compositingStrategy = CompositingStrategy.Auto
                                renderEffect = if (cover > 0.01f) BlurEffect(blurPx * cover, blurPx * cover, TileMode.Decal) else null
                            },
                    )
                    }
                    BlurredWallpaperOverlay(wallpaper?.heavy, zoom, tuning) { cover * 0.45f }

                    AnimatedVisibility(
                        visible = overlay == Overlay.Search,
                        enter = fadeIn(spring(dampingRatio = 0.85f, stiffness = 280f)) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 280f)) { -it / 6 } + scaleIn(initialScale = 0.95f),
                        exit = fadeOut(tween(160)) + androidx.compose.animation.slideOutVertically(spring(dampingRatio = 1f, stiffness = 380f)) { -it / 6 } + scaleOut(targetScale = 0.95f),
                    ) {
                        SearchOverlay(
                            apps = apps,
                            suggestions = suggestions,
                            showSuggestions = searchSuggestions,
                            icons = icons,
                            onLaunch = { app, bounds ->
                                onLaunch(app, bounds)
                                overlay = Overlay.None
                            },
                            onDismiss = { overlay = Overlay.None },
                        )
                    }
                    AnimatedVisibility(
                        visible = overlay == Overlay.Notifications,
                        // Drops in from the top with iOS fluid spring curve and gentle scale depth
                        enter = fadeIn(tween(280)) + slideInVertically(spring(dampingRatio = 0.82f, stiffness = 200f)) { -it } + scaleIn(spring(dampingRatio = 0.82f, stiffness = 200f), initialScale = 0.96f),
                        exit = fadeOut(tween(220)) + androidx.compose.animation.slideOutVertically(spring(dampingRatio = 0.92f, stiffness = 280f)) { -it } + scaleOut(spring(dampingRatio = 0.92f, stiffness = 280f), targetScale = 0.96f),
                    ) {
                        NotificationCentre(
                            allItems = notifications,
                            apps = apps,
                            icons = icons,
                            display = settings.notificationDisplay,
                            clockStyle = settings.lockClockStyle,
                            clockColor = settings.lockClockColor,
                            lockClockScale = settings.lockClockScale,
                            lockClockOffsetX = settings.lockClockOffsetX,
                            lockClockOffsetY = settings.lockClockOffsetY,
                            lockWidgets = settings.lockWidgets,
                            lockWallpaper = settings.lockWallpaper,
                            lock3dEffect = settings.lock3dEffect,
                            lockWallpaperScale = settings.lockWallpaperScale,
                            lockWallpaperOffsetX = settings.lockWallpaperOffsetX,
                            lockWallpaperOffsetY = settings.lockWallpaperOffsetY,
                            homeWallpaper = wallpaper?.sharp,
                            allowed = notificationAccess,
                            glassLook = settings.notificationGlassLook,
                            onOpen = { item ->
                                NotificationHub.open(context, item)
                                overlay = Overlay.None
                            },
                            onDismissItem = NotificationHub::dismiss,
                            onClearAll = NotificationHub::clearAll,
                            onRequestAccess = {
                                Permissions.open(context, Permissions.notificationAccessIntent(context), android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            onUpdateClockStyle = vm::setLockClockStyle,
                            onUpdateClockColor = vm::setLockClockColor,
                            onUpdateClockTransform = vm::setLockClockTransform,
                            onToggleLockWidget = vm::toggleLockWidget,
                            onUpdateLockWallpaper = vm::setLockWallpaper,
                            onToggle3dEffect = vm::setLock3dEffect,
                            onUpdateWallpaperTransform = vm::setLockWallpaperTransform,
                            onClose = { overlay = Overlay.None },
                        )
                    }
                    AnimatedVisibility(
                        visible = overlay == Overlay.ControlCentre,
                        // iOS grows Control Centre out of the corner it was pulled from, on a soft fluid spring.
                        enter = fadeIn(spring(stiffness = 260f)) + scaleIn(spring(dampingRatio = 0.82f, stiffness = 240f), initialScale = 0.88f, transformOrigin = TransformOrigin(if (isTablet) 1f else 0.5f, 0f)),
                        exit = fadeOut(spring(stiffness = 450f)) + scaleOut(spring(dampingRatio = 0.95f, stiffness = 400f), targetScale = 0.92f, transformOrigin = TransformOrigin(if (isTablet) 1f else 0.5f, 0f)),
                    ) {
                        // The glass lens rests while the panel grows and shrinks, as it does while pages move.
                        val animating = remember { derivedStateOf { transition.isRunning } }
                        CompositionLocalProvider(LocalGlassMotion provides animating) {
                            ControlCentre(onDismiss = { overlay = Overlay.None }, onOpenLauncherSettings = { overlay = Overlay.Settings })
                        }
                    }
                    AnimatedVisibility(
                        visible = overlay == Overlay.Settings,
                        enter = fadeIn(spring(dampingRatio = 0.85f, stiffness = 260f)) + scaleIn(spring(dampingRatio = 0.85f, stiffness = 260f), initialScale = 0.93f),
                        exit = fadeOut(tween(160)) + scaleOut(spring(dampingRatio = 1f, stiffness = 400f), targetScale = 0.95f),
                    ) {
                        SettingsSheet(vm, settings, onDismiss = { overlay = Overlay.None })
                    }
                    AnimatedVisibility(
                        visible = overlay == Overlay.WallpaperCustomizer,
                        enter = fadeIn(spring(dampingRatio = 0.85f, stiffness = 260f)) + scaleIn(spring(dampingRatio = 0.85f, stiffness = 260f), initialScale = 0.93f),
                        exit = fadeOut(tween(160)) + scaleOut(spring(dampingRatio = 1f, stiffness = 400f), targetScale = 0.95f),
                    ) {
                        WallpaperCustomizerSheet(vm, onDismiss = { overlay = Overlay.None })
                    }
                    AnimatedVisibility(
                        visible = overlay == Overlay.Themes,
                        enter = fadeIn(spring(dampingRatio = 0.85f, stiffness = 260f)) + scaleIn(spring(dampingRatio = 0.85f, stiffness = 260f), initialScale = 0.93f),
                        exit = fadeOut(tween(160)) + scaleOut(spring(dampingRatio = 1f, stiffness = 400f), targetScale = 0.95f),
                    ) {
                        ThemeCustomizerSheet(
                            settings = settings,
                            onSelectStyle = vm::setIconStyle,
                            onSelectTint = vm::setTintColor,
                            onToggleIconSize = { isLarge ->
                                vm.setShowLabels(!isLarge)
                                vm.setIconScale(if (isLarge) 1.22f else 1.0f)
                            },
                            onToggleDarkMode = {
                                val next = if (settings.iconStyle == com.glasslauncher.data.IconStyle.Dark) com.glasslauncher.data.IconStyle.Default else com.glasslauncher.data.IconStyle.Dark
                                vm.setIconStyle(next)
                            },
                            onDismiss = { overlay = Overlay.None }
                        )
                    }
                    AnimatedVisibility(
                        visible = overlay == Overlay.Widgets,
                        enter = fadeIn(spring(dampingRatio = 0.85f, stiffness = 260f)) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 260f)) { it / 6 },
                        exit = fadeOut(tween(160)) + androidx.compose.animation.slideOutVertically(spring(dampingRatio = 1f, stiffness = 380f)) { it / 6 },
                    ) {
                        WidgetGallery(
                            settings.widgets.filter { it.page == homePage.intValue }.map { it.kind },
                            suggestions,
                            icons,
                            onToggle = { kind -> vm.toggleHomeWidget(kind, homePage.intValue, isTablet) },
                            onAddWithSize = { kind, w, h, stack -> vm.addWidgetWithSize(kind, homePage.intValue, w, h, stack) },
                            onDismiss = { overlay = Overlay.None },
                            settings = settings,
                        )
                    }
                    AnimatedVisibility(
                        visible = overlay == Overlay.TodayWidgets,
                        enter = fadeIn(spring(dampingRatio = 0.85f, stiffness = 260f)) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 260f)) { it / 6 },
                        exit = fadeOut(tween(160)) + androidx.compose.animation.slideOutVertically(spring(dampingRatio = 1f, stiffness = 380f)) { it / 6 },
                    ) {
                        WidgetGallery(
                            settings.todayWidgets.map { it.kind },
                            suggestions,
                            icons,
                            onToggle = vm::toggleTodayWidget,
                            onAddWithSize = { kind, w, h, stack -> vm.addTodayWidgetWithSize(kind, w, h, stack) },
                            onDismiss = { overlay = Overlay.None },
                            subtitle = "Tap to add to or remove from the widgets page.",
                            includeAll = true,
                            settings = settings,
                        )
                    }
                    AnimatedVisibility(
                        visible = openGroup != null,
                        // Opens and closes on the same soft spring as Control Centre.
                        enter = fadeIn(spring(stiffness = 200f)) + scaleIn(spring(dampingRatio = 0.78f, stiffness = 180f), initialScale = 0.82f),
                        exit = fadeOut(spring(stiffness = 450f)) + scaleOut(spring(dampingRatio = 1f, stiffness = 400f), targetScale = 0.92f),
                    ) {
                        lastGroup?.let { group ->
                            FolderOverlay(
                                group = group,
                                icons = icons,
                                look = folderLook,
                                onLaunch = { app, bounds ->
                                    onLaunch(app, bounds)
                                    openGroup = null
                                },
                                onMenu = onMenu,
                                onDismiss = { openGroup = null },
                            )
                        }
                    }
                    menu?.let { current ->
                        val app = current.app
                        val quickActions by produceState(emptyList<QuickAction>(), app.key) { value = vm.quickActions(app) }
                        val inDock = app.key in settings.dock.orEmpty()
                        val onHome = app.key !in settings.hiddenFromHome
                        fun close() {
                            menu = null
                        }
                        IconMenuOverlay(
                            menu = current,
                            icon = icons[app.key],
                            quickActions = quickActions.map { action ->
                                MenuEntry(action.label, Icons.AutoMirrored.Rounded.ArrowForward) {
                                    vm.runQuickAction(app, action)
                                    close()
                                }
                            },
                            actions = buildList {
                                add(MenuEntry("Edit Home Screen", Icons.Rounded.Apps) {
                                    close()
                                    openGroup = null
                                    editing = true
                                })
                                add(MenuEntry("Share App", Icons.Rounded.IosShare) {
                                    vm.share(app)
                                    close()
                                })
                                if (!onHome) {
                                    add(MenuEntry("Add to Home Screen", Icons.Rounded.AddCircleOutline) {
                                        vm.addToHome(app)
                                        close()
                                    })
                                }
                                add(MenuEntry(if (inDock) "Remove from Dock" else "Add to Dock", if (inDock) Icons.Rounded.RemoveCircleOutline else Icons.Rounded.AddCircleOutline) {
                                    vm.toggleDock(app)
                                    close()
                                })
                                add(MenuEntry("Edit Icon", Icons.Rounded.Edit) {
                                    close()
                                    iconTarget = app
                                    pickIcon.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                })
                                if (app.key in customIcons) {
                                    add(MenuEntry("Reset Icon", Icons.Rounded.Restore) {
                                        vm.resetCustomIcon(app)
                                        close()
                                    })
                                }
                                if (app.key in settings.hiddenApps) {
                                    add(MenuEntry("Unhide App", Icons.Rounded.Visibility) {
                                        vm.unhideApp(app)
                                        openGroup = null
                                        close()
                                    })
                                } else {
                                    add(MenuEntry("Hide App", Icons.Rounded.VisibilityOff) {
                                        vm.hideApp(app)
                                        close()
                                    })
                                }
                                add(MenuEntry("App Info", Icons.Rounded.Info) {
                                    vm.openAppInfo(app)
                                    close()
                                })
                                add(MenuEntry("Remove App", Icons.Rounded.RemoveCircleOutline, destructive = true) {
                                    close()
                                    removing = app
                                })
                            },
                            onDismiss = ::close,
                        )
                    }
                    removing?.let { app ->
                        IosAlert(
                            title = "Remove “${app.label}”?",
                            message = "Removing from Home Screen will keep the app in your app drawer.",
                            buttons = buildList {
                                if (!app.isSystem) {
                                    add(AlertButton("Delete App", AlertRole.Destructive) {
                                        vm.uninstall(app)
                                        removing = null
                                    })
                                }
                                add(AlertButton("Remove from Home Screen", AlertRole.Default) {
                                    vm.removeFromHome(app)
                                    removing = null
                                })
                                add(AlertButton("Cancel", AlertRole.Cancel) { removing = null })
                            },
                            onDismiss = { removing = null },
                        )
                    }
                    AnimatedVisibility(editingNote, enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.94f), exit = fadeOut(tween(150))) {
                        val currentText = if (isNewNoteMode) "" else (editingNoteIndex?.let { settings.notesList.getOrNull(it) } ?: settings.noteText)
                        NoteEditor(
                            text = currentText,
                            isNew = isNewNoteMode,
                            onSave = { text ->
                                if (isNewNoteMode) {
                                    vm.addNote(text)
                                } else {
                                    val idx = editingNoteIndex ?: 0
                                    vm.editNote(idx, text)
                                }
                            },
                            onDelete = if (!isNewNoteMode && editingNoteIndex != null) {
                                { editingNoteIndex?.let { vm.deleteNote(it) } }
                            } else null,
                            onDismiss = {
                                editingNote = false
                                editingNoteIndex = null
                            }
                        )
                    }
                    AnimatedVisibility(addingReminder, enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.94f), exit = fadeOut(tween(150))) {
                        ReminderEditor(
                            reminders = settings.reminders,
                            onAdd = vm::addReminder,
                            onComplete = vm::completeReminder,
                            onDismiss = {
                                addingReminder = false
                                editingReminderIndex = null
                            },
                            initialIndex = editingReminderIndex,
                            onEdit = vm::editReminder,
                        )
                    }
                    AnimatedVisibility(!settings.onboarded && !settings.setupCompleted, enter = fadeIn(), exit = fadeOut()) {
                        Onboarding(step = settings.onboardingStep, onStep = vm::setOnboardingStep, onFinish = vm::finishOnboarding)
                    }
                    val showFeatureTour by vm.showFeatureTour.collectAsState()
                    AnimatedVisibility(showFeatureTour, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .consumeTaps(),
                            contentAlignment = Alignment.Center
                        ) {
                            FeatureTourPager(
                                onFinishTour = vm::closeFeatureTour,
                                onClose = vm::closeFeatureTour
                            )
                        }
                    }
                    val showDefaultHomePrompt by vm.showDefaultHomePrompt.collectAsState()
                    var homePromptRefresh by remember { mutableIntStateOf(0) }
                    LifecycleResumeEffect(Unit) {
                        homePromptRefresh++
                        if (Permissions.isDefaultHome(context)) {
                            vm.closeDefaultHomePrompt()
                        }
                        onPauseOrDispose { }
                    }
                    val defaultHomeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        homePromptRefresh++
                        if (Permissions.isDefaultHome(context)) {
                            vm.closeDefaultHomePrompt()
                        }
                    }
                    val isHomeSet = remember(homePromptRefresh, showDefaultHomePrompt) { Permissions.isDefaultHome(context) }
                    AnimatedVisibility(
                        showDefaultHomePrompt && (settings.onboarded || settings.setupCompleted) && !isHomeSet,
                        enter = fadeIn(tween(250)),
                        exit = fadeOut(tween(200))
                    ) {
                        SetDefaultHomeModal(
                            onSetDefault = {
                                android.widget.Toast.makeText(context, "👉 Select 'Glass OS Launcher' as default home app", android.widget.Toast.LENGTH_LONG).show()
                                defaultHomeLauncher.launch(Permissions.homeRoleIntent(context))
                            }
                        )
                    }
                }
            }
        }
    }
}

/** The heavily blurred wallpaper faded in on top of the sharp one, by however far the pager is into a side page. */
@Composable
private fun BlurredWallpaperOverlay(image: androidx.compose.ui.graphics.ImageBitmap?, zoom: Float, tuning: GlassTuning, amount: () -> Float) {
    val shift = LocalWallpaperShift.current
    Spacer(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val alpha = amount()
                if (image == null || alpha <= 0.01f) return@drawBehind
                val dst = centerCrop(image.width, image.height, IntSize(size.width.roundToInt(), size.height.roundToInt()), zoom)
                val offset = shift.value
                drawImage(
                    image,
                    dstOffset = IntOffset((dst.left + offset.x).roundToInt(), (dst.top + offset.y).roundToInt()),
                    dstSize = dst.size,
                    alpha = alpha,
                    filterQuality = FilterQuality.Low,
                )
                // Matches the glass setting: Tinted darkens the App Library and Today View, Clear keeps them bright.
                drawRect(if (tuning.look == GlassLook.Tinted) Color.Black.copy(alpha = (0.28f + tuning.widgetFrost * 0.2f) * alpha) else Color.White.copy(alpha = tuning.widgetFrost * 0.08f * alpha))
            },
    )
}

/**
 * Perspective Zoom: the wallpaper drifts slightly against the device's tilt. Only listens while the Home Screen
 * is showing, at UI rate, and recentres slowly so it follows how you're holding the device.
 */
@Composable
private fun PerspectiveZoom(context: Context, maxShift: Float, shift: MutableState<Offset>) {
    val view = LocalView.current
    LifecycleResumeEffect(maxShift) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var baseX = Float.NaN
        var baseY = Float.NaN
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Gravity is reported in the device's natural orientation; turn it into screen axes.
                val (x, y) = when (view.display?.rotation) {
                    Surface.ROTATION_90 -> -event.values[1] to event.values[0]
                    Surface.ROTATION_180 -> -event.values[0] to -event.values[1]
                    Surface.ROTATION_270 -> event.values[1] to -event.values[0]
                    else -> event.values[0] to event.values[1]
                }
                if (baseX.isNaN()) {
                    baseX = x
                    baseY = y
                }
                baseX += (x - baseX) * 0.02f
                baseY += (y - baseY) * 0.02f
                val target = Offset(((x - baseX) / 3f).coerceIn(-1f, 1f) * maxShift, (-(y - baseY) / 3f).coerceIn(-1f, 1f) * maxShift)
                shift.value = shift.value + (target - shift.value) * 0.2f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onPauseOrDispose {
            sensors?.unregisterListener(listener)
            shift.value = Offset.Zero
        }
    }
}

/**
 * On mobile phones, expands Android's native Notification shade.
 */
internal fun expandNotifications(context: Context) {
    runCatching {
        val sbm = context.getSystemService("statusbar")
        val method = sbm?.javaClass?.getMethod("expandNotificationsPanel")
        method?.invoke(sbm)
    }
}

/**
 * On mobile phones, expands Android's native Quick Settings / Control Center.
 */
internal fun expandQuickSettings(context: Context) {
    runCatching {
        val sbm = context.getSystemService("statusbar")
        val method = sbm?.javaClass?.getMethod("expandSettingsPanel")
        method?.invoke(sbm)
    }.onFailure {
        expandNotifications(context)
    }
}
