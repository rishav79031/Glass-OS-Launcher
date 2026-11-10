package com.glasslauncher

import com.glasslauncher.data.PhoneGrid
import com.glasslauncher.data.TodayWidget
import android.app.ActivityOptions
import android.app.Application
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toAndroidRect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glasslauncher.data.AppEntry
import com.glasslauncher.data.AppGroup
import com.glasslauncher.data.AppRepository
import com.glasslauncher.data.IconStyle
import com.glasslauncher.data.LauncherSettings
import com.glasslauncher.data.NotificationHub
import com.glasslauncher.data.QuickAction
import com.glasslauncher.data.SettingsStore
import com.glasslauncher.data.UsageStore
import com.glasslauncher.data.GlassLook
import com.glasslauncher.data.HomeWallpaperMode
import com.glasslauncher.data.NotificationDisplay
import com.glasslauncher.data.NotificationItem
import com.glasslauncher.data.Weather
import com.glasslauncher.data.WeatherRepository
import com.glasslauncher.data.WidgetKind
import com.glasslauncher.data.WidgetPlacement
import com.glasslauncher.data.IconLayout
import com.glasslauncher.data.LockClockStyle
import com.glasslauncher.data.LockWidget
import com.glasslauncher.wallpaper.COLLECTION_PREFIX
import com.glasslauncher.wallpaper.SHUFFLE_PREFIX
import com.glasslauncher.icons.GlassIconRenderer
import com.glasslauncher.icons.CustomIconStore
import com.glasslauncher.icons.IconPack
import com.glasslauncher.icons.IconPackRepository
import com.glasslauncher.icons.LoadedIconPack
import com.glasslauncher.wallpaper.CUSTOM_PREFIX
import com.glasslauncher.wallpaper.PRESET_PREFIX
import com.glasslauncher.wallpaper.WallpaperImages
import com.glasslauncher.wallpaper.WallpaperPreset
import com.glasslauncher.wallpaper.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application, viewModelScope)
    private val settingsStore = SettingsStore(application)
    private val usageStore = UsageStore(application)
    private val wallpaperRepository = WallpaperRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val featured = com.glasslauncher.data.FeaturedPhotos(application)
    private val iconRenderer = GlassIconRenderer((ICON_RENDER_DP * application.resources.displayMetrics.density).roundToInt())
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val systemDark = MutableStateFlow(false)
    private val iconPacks = IconPackRepository(application)
    private val customIcons = CustomIconStore(application)
    @Volatile private var loadedPack: LoadedIconPack? = null
    private var dockLimit = 4

    val settings: StateFlow<LauncherSettings> = settingsStore.settings
    private val installedApps: StateFlow<List<AppEntry>> = appRepository.apps
    private val hiddenKeys = settings.map { it.hiddenApps }.distinctUntilChanged()

    /** Every app except hidden ones; hidden apps only appear in the App Library's Hidden folder. */
    val apps: StateFlow<List<AppEntry>> = combine(installedApps, hiddenKeys) { list, hidden -> list.filter { it.key !in hidden } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val hiddenApps: StateFlow<List<AppEntry>> = combine(installedApps, hiddenKeys) { list, hidden -> list.filter { it.key in hidden } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val badges: StateFlow<Map<String, Int>> = NotificationHub.badges

    /** Home Screen apps in the user's order, without Dock apps and apps removed from the Home Screen. */
    val homeApps: StateFlow<List<AppEntry>> = combine(apps, settings) { list, s -> orderHome(list, s) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Most-opened apps, like Siri Suggestions, with smart time-based and activity-based fallback. */
    val suggestions: StateFlow<List<AppEntry>> = combine(apps, usageStore.usage) { list, usage ->
        val byKey = list.associateBy { it.key }
        val actualUsed = usage.counts.entries.sortedByDescending { it.value }.mapNotNull { byKey[it.key] }

        if (actualUsed.size >= MAX_SUGGESTIONS) {
            actualUsed.take(MAX_SUGGESTIONS)
        } else {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val timeKeywords = when (hour) {
                in 5..10 -> listOf("weather", "clock", "calendar", "news", "mail", "music", "maps")
                in 11..17 -> listOf("chrome", "browser", "message", "phone", "dialer", "drive", "docs", "notes", "camera")
                in 18..22 -> listOf("youtube", "music", "spotify", "instagram", "whatsapp", "photos", "video")
                else -> listOf("clock", "alarm", "music", "settings", "calculator", "notes")
            }
            val timeBased = list.filter { app ->
                val pkg = app.packageName.lowercase()
                val lbl = app.label.lowercase()
                timeKeywords.any { pkg.contains(it) || lbl.contains(it) }
            }
            val combined = (actualUsed + timeBased + list).distinctBy { it.key }
            combined.take(MAX_SUGGESTIONS)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recents: StateFlow<List<AppEntry>> = combine(apps, usageStore.usage) { list, usage ->
        val byKey = list.associateBy { it.key }
        usage.recents.mapNotNull { byKey[it] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groups: StateFlow<List<AppGroup>> = combine(apps, suggestions) { list, suggested -> buildGroups(list, suggested) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Every icon pre-rendered in the current appearance, keyed by app key. */
    val icons: StateFlow<Map<String, ImageBitmap>> = combine(
        installedApps,
        settings.map { Triple(it.iconStyle, it.tintColor, it.iconPack) }.distinctUntilChanged(),
        systemDark,
        customIcons.icons,
    ) { list, (style, tint, pack), dark, custom ->
        val resolved = if (style == IconStyle.Automatic) (if (dark) IconStyle.Dark else IconStyle.Default) else style
        IconRequest(list, resolved, tint, pack, custom)
    }
        .mapLatest { renderIcons(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val wallpaper: StateFlow<WallpaperImages?> = settings
        // The shuffle index only matters (and only reloads) for Photo Shuffle.
        .map { Triple(it.wallpaper, it.homeWallpaperMode, if (it.wallpaper.startsWith(SHUFFLE_PREFIX)) it.shuffleIndex else 0) }
        .distinctUntilChanged()
        .mapLatest { (id, mode, index) -> wallpaperRepository.load(id, mode, index) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val weather: StateFlow<Weather?> = weatherRepository.weather
    val notifications: StateFlow<List<NotificationItem>> = NotificationHub.items

    init {
        viewModelScope.launch {
            val installed = apps.first { it.isNotEmpty() }
            if (settings.value.dock == null) settingsStore.update { it.copy(dock = defaultDock(installed)) }
        }
    }

    fun setSystemDark(dark: Boolean) {
        systemDark.value = dark
    }

    /** The Dock holds 4 apps on a phone and more on a tablet; the UI reports the limit for the current window. */
    fun setDockLimit(limit: Int) {
        dockLimit = limit
    }

    fun launch(app: AppEntry, view: View, bounds: Rect) {
        // iOS shared-element squircle clip-reveal zoom animation with spring expansion
        val options = if (bounds.isEmpty) null else {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                ActivityOptions.makeClipRevealAnimation(
                    view, bounds.left.roundToInt(), bounds.top.roundToInt(), bounds.width.roundToInt(), bounds.height.roundToInt(),
                ).toBundle()
            } else {
                ActivityOptions.makeScaleUpAnimation(
                    view, bounds.left.roundToInt(), bounds.top.roundToInt(), bounds.width.roundToInt(), bounds.height.roundToInt(),
                ).toBundle()
            }
        }
        appRepository.launch(app, if (bounds.isEmpty) null else bounds.toAndroidRect(), options)
        usageStore.recordLaunch(app.key)
    }

    suspend fun quickActions(app: AppEntry): List<QuickAction> = withContext(Dispatchers.IO) { appRepository.quickActions(app) }

    fun runQuickAction(app: AppEntry, action: QuickAction) {
        appRepository.startQuickAction(action)
        usageStore.recordLaunch(app.key)
    }

    fun openAppInfo(app: AppEntry) = appRepository.openAppInfo(app)

    fun share(app: AppEntry) = appRepository.share(app)

    fun uninstall(app: AppEntry) = appRepository.uninstall(app)

    fun toggleDock(app: AppEntry) {
        if (app.key in settings.value.dock.orEmpty()) removeFromDock(app.key) else addToDock(app.key)
    }

    fun addToDock(key: String) {
        val dock = settings.value.dock.orEmpty()
        if (key in dock) return
        if (dock.size >= dockLimit) {
            toast("The Dock is full")
            return
        }
        settingsStore.update { it.copy(dock = it.dock.orEmpty() + key, hiddenFromHome = it.hiddenFromHome - key) }
    }

    fun removeFromDock(key: String) = settingsStore.update { it.copy(dock = it.dock.orEmpty() - key) }


    fun removeFromHome(app: AppEntry) = settingsStore.update {
        it.copy(hiddenFromHome = it.hiddenFromHome + app.key, dock = it.dock.orEmpty() - app.key)
    }

    fun hideApp(app: AppEntry) = settingsStore.update {
        it.copy(hiddenApps = it.hiddenApps + app.key, dock = it.dock.orEmpty() - app.key)
    }

    fun unhideApp(app: AppEntry) = settingsStore.update { it.copy(hiddenApps = it.hiddenApps - app.key) }

    fun addToHome(app: AppEntry) = settingsStore.update { it.copy(hiddenFromHome = it.hiddenFromHome - app.key) }

    fun isOnHome(app: AppEntry): Boolean = app.key !in settings.value.hiddenFromHome

    fun setIconStyle(style: IconStyle) = settingsStore.update { it.copy(iconStyle = style) }

    fun setTintColor(color: Int) = settingsStore.update { it.copy(tintColor = color) }

    fun setShowLabels(show: Boolean) = settingsStore.update { it.copy(showLabels = show) }

    fun setBlurUnderIcons(blur: Boolean) = settingsStore.update { it.copy(blurUnderIcons = blur) }

    fun setShowBadges(show: Boolean) = settingsStore.update { it.copy(showBadges = show) }

    fun setDockRecents(show: Boolean) = settingsStore.update { it.copy(dockRecents = show) }

    fun setGlassStatusBar(on: Boolean) = settingsStore.update { it.copy(glassStatusBar = on) }

    fun setDimWallpaperInDark(dim: Boolean) = settingsStore.update { it.copy(dimWallpaperInDark = dim) }

    fun setIconLayout(layout: IconLayout) = settingsStore.update { it.copy(iconLayout = layout) }

    fun setPhoneGrid(grid: PhoneGrid) = settingsStore.update { it.copy(phoneGrid = grid) }

    val featuredPhotos: StateFlow<List<String>> = featured.photos

    fun featuredFile(name: String): java.io.File = featured.file(name)

    fun setFeaturedPhotos(uris: List<Uri>) {
        viewModelScope.launch { featured.replace(uris) }
    }

    fun removeFeaturedPhoto(name: String) {
        viewModelScope.launch { featured.remove(name) }
    }

    fun clearFeaturedPhotos() {
        viewModelScope.launch { featured.clear() }
    }

    /** Icon packs installed on the device, for the App Icons setting. */
    suspend fun installedIconPacks(): List<IconPack> = withContext(Dispatchers.IO) { iconPacks.installed() }

    fun setIconPack(packageName: String?) = settingsStore.update { it.copy(iconPack = packageName) }

    val customIconKeys: StateFlow<Map<String, Long>> = customIcons.icons

    fun setCustomIcon(app: AppEntry, uri: Uri) {
        viewModelScope.launch { if (!customIcons.set(app.key, uri)) toast("Couldn't use that image") }
    }

    fun resetCustomIcon(app: AppEntry) = customIcons.reset(app.key)

    fun setCustomColumns(columns: Int) = settingsStore.update { it.copy(customColumns = columns) }

    /**
     * Saves the Home Screen after a drag: every app's cell (so new apps don't shuffle the rest) and the widgets.
     * [fromDock] is an app that was dragged out of the Dock onto the grid.
     */
    /** Saves the arrangement for the orientation it was made in; the other orientation keeps its own. */
    fun saveHomeLayout(
        positions: Map<String, Int>,
        widgets: List<WidgetPlacement>,
        fromDock: String? = null,
        landscape: Boolean = false,
        folders: List<com.glasslauncher.data.HomeFolder> = emptyList(),
    ) = settingsStore.update {
        val withLayout = if (landscape) {
            it.copy(appPositionsLandscape = positions, widgetsLandscape = widgets, homeFoldersLandscape = folders)
        } else {
            it.copy(appPositions = positions, widgets = widgets, homeFolders = folders)
        }
        withLayout.copy(
            dock = if (fromDock != null) it.dock.orEmpty() - fromDock else it.dock,
            hiddenFromHome = if (fromDock != null) it.hiddenFromHome - fromDock else it.hiddenFromHome,
        )
    }

    fun setNote(text: String) {
        val clean = text.trim().take(1000)
        settingsStore.update {
            val list = it.notesList.toMutableList()
            if (list.isNotEmpty()) {
                if (clean.isNotBlank()) list[0] = clean else list.removeAt(0)
            } else if (clean.isNotBlank()) {
                list.add(clean)
            }
            it.copy(noteText = clean, notesList = list)
        }
    }

    fun addNote(text: String) {
        val clean = text.trim().take(1000)
        if (clean.isBlank()) return
        settingsStore.update {
            val list = mutableListOf(clean)
            list.addAll(it.notesList.filter { note -> note != clean })
            it.copy(noteText = clean, notesList = list)
        }
    }

    fun editNote(index: Int, text: String) {
        val clean = text.trim().take(1000)
        settingsStore.update {
            val list = it.notesList.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
                if (clean.isNotBlank()) list.add(0, clean) // Edited note moves to top as the latest!
            } else if (clean.isNotBlank()) {
                list.add(0, clean)
            }
            val top = list.firstOrNull().orEmpty()
            it.copy(noteText = top, notesList = list)
        }
    }

    fun deleteNote(index: Int) {
        settingsStore.update {
            val list = it.notesList.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
            }
            val top = list.firstOrNull().orEmpty()
            it.copy(noteText = top, notesList = list)
        }
    }

    fun addReminder(text: String) {
        val clean = text.trim().take(200)
        if (clean.isNotEmpty()) settingsStore.update { it.copy(reminders = (it.reminders + clean).takeLast(30)) }
    }

    fun editReminder(index: Int, text: String) {
        val clean = text.trim().take(200)
        if (clean.isEmpty()) return
        settingsStore.update {
            if (index in it.reminders.indices) {
                val list = it.reminders.toMutableList()
                list[index] = clean
                it.copy(reminders = list)
            } else it
        }
    }

    /** Ticking a reminder off removes it, as on iOS once the list refreshes. */
    fun completeReminder(index: Int) = settingsStore.update { it.copy(reminders = it.reminders.filterIndexed { i, _ -> i != index }) }

    fun setLabelScale(scale: Float) = settingsStore.update { it.copy(labelScale = scale.coerceIn(com.glasslauncher.data.LabelScaleRange)) }

    fun setIconScale(scale: Float) = settingsStore.update { it.copy(iconScale = scale.coerceIn(com.glasslauncher.data.IconScaleRange)) }

    fun setLockClockStyle(style: LockClockStyle) = settingsStore.update { it.copy(lockClockStyle = style) }

    fun setLockClockColor(color: Int) = settingsStore.update { it.copy(lockClockColor = color) }

    fun setLockWallpaper(id: String) = settingsStore.update { it.copy(lockWallpaper = id) }

    fun setLock3dEffect(enabled: Boolean) = settingsStore.update { it.copy(lock3dEffect = enabled) }

    fun setLockScreenEnabled(enabled: Boolean) = settingsStore.update { it.copy(lockScreenEnabled = enabled) }

    fun setLockWallpaperTransform(scale: Float, offsetX: Float, offsetY: Float) = settingsStore.update {
        it.copy(lockWallpaperScale = scale.coerceIn(0.5f, 4.0f), lockWallpaperOffsetX = offsetX, lockWallpaperOffsetY = offsetY)
    }

    fun setLockClockTransform(scale: Float, offsetX: Float, offsetY: Float) = settingsStore.update {
        it.copy(lockClockScale = scale.coerceIn(0.6f, 2.5f), lockClockOffsetX = offsetX, lockClockOffsetY = offsetY)
    }

    fun toggleLockWidget(widget: LockWidget) = settingsStore.update {
        val list = if (widget in it.lockWidgets) it.lockWidgets - widget else (it.lockWidgets + widget).take(4)
        it.copy(lockWidgets = list)
    }

    /** Back to the iOS defaults: Clear, light frost on widgets, near-clear Dock. */
    fun resetGlass() = settingsStore.update { it.copy(glassLook = GlassLook.Clear, widgetFrost = 0.5f, dockFrost = 0.2f) }

    fun finishOnboarding() = settingsStore.update { it.copy(onboarded = true, setupCompleted = true) }

    val showFeatureTour = MutableStateFlow(false)
    fun openFeatureTour() { showFeatureTour.value = true }
    fun closeFeatureTour() { showFeatureTour.value = false }
    fun reopenOnboarding() = openFeatureTour()

    val showDefaultHomePrompt = MutableStateFlow(false)
    fun openDefaultHomePrompt() { showDefaultHomePrompt.value = true }
    fun closeDefaultHomePrompt() { showDefaultHomePrompt.value = false }

    fun setOnboardingStep(step: Int) = settingsStore.update { it.copy(onboardingStep = step) }

    fun setPresetWallpaper(preset: WallpaperPreset) {
        val id = PRESET_PREFIX + preset.id
        settingsStore.update { it.copy(wallpaper = id) }
        applySystemWallpaper(id)
    }

    fun setCustomWallpaper(uri: Uri) {
        viewModelScope.launch {
            if (wallpaperRepository.saveCustom(uri)) {
                // A fresh id each time, so picking a second photo still reloads.
                val id = CUSTOM_PREFIX + System.currentTimeMillis()
                settingsStore.update { it.copy(wallpaper = id) }
                applySystemWallpaper(id)
            } else {
                toast("Couldn't open that photo")
            }
        }
    }

    /**
     * Choosing a wallpaper also sets it as the phone's Lock Screen and Home Screen wallpaper, as on iOS, in the
     * background.
     */
    fun setLockWallpaperOnly(id: String) {
        viewModelScope.launch {
            val applied = wallpaperRepository.applyAsLockOnly(id, settings.value.shuffleIndex)
            toast(if (applied) "Lock Screen Wallpaper updated" else "Couldn't update Lock Screen")
        }
    }

    fun setHomeWallpaperOnly(id: String) {
        viewModelScope.launch {
            settingsStore.update { it.copy(wallpaper = id) }
            val applied = wallpaperRepository.applyAsHomeOnly(id, settings.value.shuffleIndex)
            toast(if (applied) "Home Screen Wallpaper updated" else "Couldn't update Home Screen")
        }
    }

    fun pairBothWallpapers() {
        applySystemWallpaper()
    }

    private fun applySystemWallpaper(wallpaperId: String? = null) {
        viewModelScope.launch {
            val id = wallpaperId ?: settings.value.wallpaper
            val applied = wallpaperRepository.applyAsSystemWallpaper(id, settings.value.shuffleIndex)
            if (applied) {
                toast("Wallpaper set for Lock & Home screen")
            } else {
                toast("Couldn't set system wallpaper")
            }
        }
    }

    private val collectionState = MutableStateFlow(wallpaperRepository.collection())

    /** File names of the user's own wallpapers in Collections. */
    val collection: StateFlow<List<String>> = collectionState

    fun addToCollection(uris: List<Uri>) {
        viewModelScope.launch {
            val added = wallpaperRepository.addToCollection(uris)
            collectionState.value = wallpaperRepository.collection()
            // Like picking in iOS: the first new wallpaper is applied straight away.
            added.firstOrNull()?.let(::setCollectionWallpaper) ?: toast("Couldn't open those photos")
        }
    }

    fun setCollectionWallpaper(name: String) {
        val id = COLLECTION_PREFIX + name
        settingsStore.update { it.copy(wallpaper = id) }
        applySystemWallpaper(id)
    }

    fun removeFromCollection(name: String) {
        viewModelScope.launch {
            wallpaperRepository.removeFromCollection(name)
            collectionState.value = wallpaperRepository.collection()
            if (settings.value.wallpaper == COLLECTION_PREFIX + name) settingsStore.update { it.copy(wallpaper = com.glasslauncher.wallpaper.DEFAULT_WALLPAPER) }
        }
    }

    suspend fun collectionThumbnail(name: String): ImageBitmap? = wallpaperRepository.collectionThumbnail(name)

    fun setShuffleWallpaper(uris: List<Uri>) {
        viewModelScope.launch {
            if (wallpaperRepository.saveShuffle(uris)) {
                settingsStore.update { it.copy(wallpaper = SHUFFLE_PREFIX + System.currentTimeMillis(), shuffleIndex = 0) }
                applySystemWallpaper()
            } else {
                toast("Couldn't open those photos")
            }
        }
    }

    /**
     * Screen off: Photo Shuffle moves to the next photo now, so it's decoded and blurred while the screen is dark
     * and the unlock shows it instantly ("Shuffle on Lock" with no work left for the unlock itself).
     */
    fun onScreenOff() {
        if (settings.value.wallpaper.startsWith(SHUFFLE_PREFIX)) {
            settingsStore.update { it.copy(shuffleIndex = it.shuffleIndex + 1) }
            // The Lock Screen gets the same next photo, so waking the phone shows it there too.
            applySystemWallpaper()
        }
    }

    /**
     * Greet with "hello" on first setup, and on the first Home Screen after the phone starts up (the launcher's
     * process is then only moments older than the boot itself).
     */
    private val greetingPrefs = application.getSharedPreferences("greeting", android.content.Context.MODE_PRIVATE)

    // Hello and morning greeting disabled per user request
    val showHello = MutableStateFlow<com.glasslauncher.ui.Greeting?>(null)

    fun helloShown() {
        showHello.value = null
    }

    private fun maybeGreetMorning() {
        // Disabled per user request
    }

    fun setAutoArrange(on: Boolean) = settingsStore.update { it.copy(autoArrange = on) }

    fun onUnlock() {
        maybeGreetMorning()
        refreshWeather()
    }

    fun setHomeWallpaperMode(mode: HomeWallpaperMode) = settingsStore.update { it.copy(homeWallpaperMode = mode) }

    fun setPerspectiveZoom(on: Boolean) = settingsStore.update { it.copy(perspectiveZoom = on) }

    fun setGlassLook(look: GlassLook) = settingsStore.update { it.copy(glassLook = look) }

    fun setWidgetFrost(frost: Float) = settingsStore.update { it.copy(widgetFrost = frost) }

    fun setDockFrost(frost: Float) = settingsStore.update { it.copy(dockFrost = frost) }

    fun setWallpaperBlur(blur: Float) = settingsStore.update { it.copy(wallpaperBlur = blur.coerceIn(0f, 1f)) }

    fun setNotificationDisplay(display: NotificationDisplay) = settingsStore.update { it.copy(notificationDisplay = display) }

    fun setNotificationGlassLook(look: GlassLook) = settingsStore.update { it.copy(notificationGlassLook = look) }

    /** Adds [kind] to Home Screen [page] (in the first free spot), or removes it from that page if it's there. */
    fun toggleHomeWidget(kind: WidgetKind, page: Int, isTablet: Boolean) = settingsStore.update {
        val present = it.widgets.any { w -> w.kind == kind && w.page == page }
        if (present) {
            return@update it.copy(
                widgets = it.widgets.filterNot { w -> w.kind == kind && w.page == page },
                widgetsLandscape = it.widgetsLandscape.filterNot { w -> w.kind == kind && w.page == page },
            )
        }
        // Starting sizes as on iPad and iPhone: small for most, medium for Weather, large for Calendar and Photos.
        // In iOS sizes (small = 1×1); the Home Screen turns them into grid cells for the device.
        val (w, h) = when {
            !isTablet -> if (kind == WidgetKind.Music || kind == WidgetKind.UpNext) 2 to 1 else 1 to 1
            kind == WidgetKind.Calendar || kind == WidgetKind.Photos -> 2 to 2
            kind == WidgetKind.Weather || kind == WidgetKind.Music || kind == WidgetKind.UpNext -> 2 to 1
            else -> 1 to 1
        }
        val added = WidgetPlacement(kind, page, -1, -1, w, h)
        // Added to both orientations; an unarranged landscape follows portrait anyway.
        it.copy(widgets = it.widgets + added, widgetsLandscape = if (it.widgetsLandscape.isEmpty()) it.widgetsLandscape else it.widgetsLandscape + added)
    }

    /** Adds a widget of explicit size (small 1x1, medium 2x1, large 2x2) or Smart Stack from the Widget Gallery. */
    fun addWidgetWithSize(kind: WidgetKind, page: Int, w: Int, h: Int, stack: List<WidgetKind> = emptyList()) = settingsStore.update {
        val added = WidgetPlacement(kind, page, -1, -1, w, h, stack)
        it.copy(
            widgets = it.widgets + added,
            widgetsLandscape = if (it.widgetsLandscape.isEmpty()) it.widgetsLandscape else it.widgetsLandscape + added,
        )
    }

    /** Asks the Home Screen to close up gaps on every page, keeping the apps' order. */
    val arrangeRequests = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun rearrangeIcons() {
        arrangeRequests.tryEmit(Unit)
    }

    fun setTodayWidgets(widgets: List<TodayWidget>) = settingsStore.update { it.copy(todayWidgets = widgets) }

    /** Adds [kind] to the end of Today View, or takes it out if it's already there. */
    fun toggleTodayWidget(kind: WidgetKind) = settingsStore.update {
        val present = it.todayWidgets.any { w -> w.kind == kind }
        val wide = kind == WidgetKind.Music || kind == WidgetKind.UpNext || kind == WidgetKind.Suggestions
        it.copy(todayWidgets = if (present) it.todayWidgets.filterNot { w -> w.kind == kind } else it.todayWidgets + TodayWidget(kind, if (wide) 2 else 1, 1))
    }

    /** Adds [kind] with explicit width, height and optional stack to Today View. */
    fun addTodayWidgetWithSize(kind: WidgetKind, w: Int, h: Int, stack: List<WidgetKind> = emptyList()) = settingsStore.update {
        it.copy(todayWidgets = it.todayWidgets + TodayWidget(kind, w, h, stack))
    }

    fun removeHomeWidget(placement: WidgetPlacement) = settingsStore.update {
        it.copy(
            widgets = it.widgets.filterNot { w -> w.kind == placement.kind && w.page == placement.page },
            widgetsLandscape = it.widgetsLandscape.filterNot { w -> w.kind == placement.kind && w.page == placement.page },
        )
    }

    fun refreshWeather(force: Boolean = false) {
        viewModelScope.launch { weatherRepository.refresh(settings.value.weatherPlace, force) }
    }

    fun hasWeatherLocation(): Boolean = true

    /** Sets the Weather widget's city by name; returns false when it can't be found. */
    suspend fun setWeatherCity(name: String): Boolean {
        val place = weatherRepository.search(name) ?: return false
        settingsStore.update { it.copy(weatherPlace = place) }
        weatherRepository.refresh(place, force = true)
        return true
    }

    suspend fun setWeatherPlace(place: String, forceRefresh: Boolean = true) {
        settingsStore.update { it.copy(weatherPlace = place) }
        if (forceRefresh) weatherRepository.refresh(place, force = true)
    }

    suspend fun searchWeatherSuggestions(query: String): List<com.glasslauncher.data.WeatherPlaceSuggestion> {
        return weatherRepository.searchSuggestions(query)
    }

    fun useDeviceLocationForWeather() {
        settingsStore.update { it.copy(weatherPlace = null) }
        refreshWeather(force = true)
    }

    suspend fun presetThumbnail(preset: WallpaperPreset): ImageBitmap = wallpaperRepository.thumbnail(preset)

    override fun onCleared() = appRepository.close()

    private fun orderHome(list: List<AppEntry>, s: LauncherSettings): List<AppEntry> {
        val dock = s.dock.orEmpty().toSet()
        val byKey = list.associateBy { it.key }
        val arranged = s.homeOrder.mapNotNull { byKey[it] }
        val arrangedKeys = s.homeOrder.toSet()
        val rest = list.filter { it.key !in arrangedKeys }
        return (arranged + rest).filter { it.key !in dock && it.key !in s.hiddenFromHome }
    }

    private class IconRequest(val apps: List<AppEntry>, val style: IconStyle, val tint: Int, val pack: String?, val custom: Map<String, Long>)

    // Your own image wins, then the icon pack, then the app's icon. Each result is cached until something changes.
    private suspend fun renderIcons(request: IconRequest): Map<String, ImageBitmap> = coroutineScope {
        val prefix = "${request.style.name}:${request.tint}:${request.pack}|"
        iconCache.keys.removeAll { !it.startsWith(prefix) }
        val pack = request.pack?.let { name -> loadedPack?.takeIf { it.packageName == name } ?: iconPacks.load(name).also { loadedPack = it } }
        request.apps.chunked(ICON_BATCH).map { batch ->
            async {
                batch.map { app ->
                    val cacheKey = "$prefix${app.key}|${app.updatedAt}|${request.custom[app.key] ?: 0}"
                    app.key to iconCache.getOrPut(cacheKey) {
                        val custom = if (app.key in request.custom) customIcons.drawable(app.key) else null
                        val packed = if (custom == null) pack?.icon(app.component) else null
                        val iosIcon = if (custom == null && packed == null) com.glasslauncher.icons.IOSIconProvider.getDrawable(getApplication(), app.packageName, isDark = request.style == IconStyle.Dark) else null
                        val source = custom ?: packed ?: iosIcon ?: appRepository.loadIcon(app)
                        iconRenderer.render(source, request.style, request.tint, fullBleed = custom != null || packed != null || iosIcon != null).asImageBitmap()
                    }
                }
            }
        }.awaitAll().flatten().toMap()
    }

    // App Library: Suggestions and Recently Added first, then iOS-style Apple categories.
    private fun buildGroups(list: List<AppEntry>, suggested: List<AppEntry>): List<AppGroup> {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RECENTLY_ADDED_DAYS)
        val recentlyAdded = list.filter { !it.isSystem && it.installedAt > since }.sortedByDescending { it.installedAt }.take(MAX_SUGGESTIONS)
        val categoryMap = list.groupBy(::classifyAppleCategory)
        val appleOrder = listOf(
            "Utilities",
            "Shopping & Food",
            "Travel",
            "Entertainment",
            "Creativity",
            "Social",
            "Productivity & Finance",
            "Games",
            "Health & Fitness",
            "Information & Reading",
            "Other",
        )
        return buildList {
            if (suggested.isNotEmpty()) add(AppGroup("Suggestions", suggested))
            if (recentlyAdded.isNotEmpty()) add(AppGroup("Recently Added", recentlyAdded))
            for (cat in appleOrder) {
                categoryMap[cat]?.let { if (it.isNotEmpty()) add(AppGroup(cat, it)) }
            }
            for ((cat, apps) in categoryMap) {
                if (cat !in appleOrder && apps.isNotEmpty()) add(AppGroup(cat, apps))
            }
        }
    }

    private fun classifyAppleCategory(app: AppEntry): String {
        val pkg = app.packageName.lowercase()
        val label = app.label.lowercase()

        // 0. Dedicated Finance & Payments check upfront so payment/banking apps never fall into Utilities
        val isFinance = pkg.contains("phonepe") || pkg.contains("paytm") || pkg.contains("gpay") ||
            pkg.contains("bhim") || pkg.contains("cred") || pkg.contains("mobikwik") ||
            pkg.contains("wallet") || pkg.contains("bank") || pkg.contains("finance") ||
            pkg.contains("invest") || pkg.contains("zerodha") || pkg.contains("groww") ||
            pkg.contains("upstox") || pkg.contains("paypal") || pkg.contains("venmo") ||
            pkg.contains("cashapp") || pkg.contains("revolut") || pkg.contains("chase") ||
            pkg.contains("wells") || pkg.contains("citi") || pkg.contains("sbi") ||
            pkg.contains("hdfc") || pkg.contains("icici") || pkg.contains("axis") ||
            pkg.contains("kotak") || pkg.contains("crypto") || pkg.contains("binance") ||
            pkg.contains("coinbase") || label.contains("phonepe") || label.contains("phone pe") ||
            label.contains("paytm") || label.contains("gpay") || label.contains("google pay") ||
            label.contains("bank") || label.contains("wallet") || label.contains("finance") ||
            label.contains("payment") || label.contains("pay") || label.contains("money")

        // 0b. Health, Pharmacy & Medical check upfront
        val isHealth = pkg.contains("pharmeasy") || pkg.contains("1mg") || pkg.contains("netmeds") ||
            pkg.contains("apollo") || pkg.contains("pharm") || pkg.contains("med") ||
            pkg.contains("doctor") || pkg.contains("practo") || pkg.contains("health") ||
            pkg.contains("fitness") || pkg.contains("fit") || pkg.contains("strava") ||
            pkg.contains("workout") || pkg.contains("sleep") || pkg.contains("shealth") ||
            pkg.contains("cult") || pkg.contains("pedometer") ||
            label.contains("pharmeasy") || label.contains("pharmacy") || label.contains("medicine") ||
            label.contains("medical") || label.contains("doctor") || label.contains("practo") ||
            label.contains("fitness") || label.contains("health") || label.contains("workout") ||
            label.contains("1mg") || label.contains("netmeds") || label.contains("apollo")

        if (isFinance) {
            return "Productivity & Finance"
        }

        if (isHealth) {
            return "Health & Fitness"
        }

        // 1. Social
        if (app.category == ApplicationInfo.CATEGORY_SOCIAL ||
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("instagram") ||
            pkg.contains("facebook") || pkg.contains("twitter") || pkg.contains("discord") ||
            pkg.contains("messenger") || pkg.contains("signal") || pkg.contains("snapchat") ||
            pkg.contains("tiktok") || pkg.contains("linkedin") || pkg.contains("reddit") ||
            pkg.contains("messaging") || label.contains("messages") || label.contains("chat") ||
            label.contains("social") || pkg.contains("threads") || pkg.contains("viber") ||
            pkg.contains("wechat") || pkg.contains("line") || pkg.contains("skype")
        ) {
            return "Social"
        }

        // 2. Shopping & Food
        if (pkg.contains("amazon") || pkg.contains("ebay") || pkg.contains("flipkart") ||
            pkg.contains("walmart") || pkg.contains("target") || pkg.contains("aliexpress") ||
            pkg.contains("zomato") || pkg.contains("swiggy") || pkg.contains("doordash") ||
            pkg.contains("ubereats") || pkg.contains("uber.eats") || pkg.contains("starbucks") ||
            pkg.contains("mcdonald") || pkg.contains("shopping") || pkg.contains("store") ||
            pkg.contains("shop") || pkg.contains("instacart") || pkg.contains("deliveroo") ||
            pkg.contains("grubhub") || pkg.contains("food") || pkg.contains("zepto") ||
            pkg.contains("blinkit") || pkg.contains("bigbasket") ||
            label.contains("shopping") || label.contains("food") || label.contains("market") ||
            label.contains("grocery") || label.contains("store") || pkg.contains("myntra") ||
            pkg.contains("shein") || pkg.contains("ajio") || pkg.contains("nykaa")
        ) {
            return "Shopping & Food"
        }

        // 3. Travel
        if (app.category == ApplicationInfo.CATEGORY_MAPS ||
            pkg.contains("maps") || pkg.contains("uber") || pkg.contains("lyft") ||
            pkg.contains("airbnb") || pkg.contains("booking") || pkg.contains("transit") ||
            pkg.contains("navigation") || pkg.contains("waze") || pkg.contains("flight") ||
            pkg.contains("airline") || pkg.contains("hotel") || pkg.contains("travel") ||
            pkg.contains("trip") || pkg.contains("expedia") || pkg.contains("rail") ||
            pkg.contains("irctc") || pkg.contains("ola") || pkg.contains("rapido") ||
            label.contains("maps") || label.contains("travel") || label.contains("flight") ||
            label.contains("taxi") || label.contains("ride") || label.contains("metro") ||
            label.contains("train") || label.contains("rapido")
        ) {
            return "Travel"
        }

        // 4. Entertainment
        if (app.category == ApplicationInfo.CATEGORY_AUDIO || app.category == ApplicationInfo.CATEGORY_VIDEO ||
            pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("spotify") ||
            pkg.contains("primevideo") || pkg.contains("disney") || pkg.contains("twitch") ||
            pkg.contains("music") || pkg.contains("audio") || pkg.contains("video") ||
            pkg.contains("player") || pkg.contains("tv") || pkg.contains("podcast") ||
            pkg.contains("radio") || pkg.contains("hulu") || pkg.contains("max") ||
            pkg.contains("hotstar") || pkg.contains("soundcloud") || pkg.contains("deezer") ||
            label.contains("music") || label.contains("tv") || label.contains("video") ||
            label.contains("player") || label.contains("podcast") || label.contains("radio")
        ) {
            return "Entertainment"
        }

        // 5. Creativity
        if (app.category == ApplicationInfo.CATEGORY_IMAGE ||
            pkg.contains("camera") || pkg.contains("gallery") || pkg.contains("photos") ||
            pkg.contains("canva") || pkg.contains("photoshop") || pkg.contains("lightroom") ||
            pkg.contains("snapseed") || pkg.contains("vsco") || pkg.contains("recorder") ||
            pkg.contains("voice") || pkg.contains("drawing") || pkg.contains("paint") ||
            pkg.contains("editor") || pkg.contains("capcut") || pkg.contains("inshot") ||
            label.contains("camera") || label.contains("photos") || label.contains("gallery") ||
            label.contains("editor") || label.contains("draw") || label.contains("paint") ||
            label.contains("voice memos") || label.contains("recorder")
        ) {
            return "Creativity"
        }

        // 6. Utilities (only actual system dialer, tools, clock, settings, NOT payment apps)
        val isSystemDialer = (pkg.contains("dialer") || pkg.contains("telecom") || pkg.contains("incallui") ||
            pkg.endsWith(".phone") || label == "phone" || label == "dialer") && !isFinance
        if (app.category == ApplicationInfo.CATEGORY_ACCESSIBILITY ||
            isSystemDialer ||
            pkg.contains("settings") || pkg.contains("calculator") || pkg.contains("clock") ||
            pkg.contains("alarm") || pkg.contains("file") || pkg.contains("files") ||
            pkg.contains("contact") || pkg.contains("tools") || pkg.contains("utility") ||
            pkg.contains("terminal") || pkg.contains("shizuku") || pkg.contains("compass") ||
            pkg.contains("measure") || pkg.contains("starthere") || pkg.contains("deskclock") ||
            pkg.contains("authenticator") || label.contains("calculator") || label.contains("clock") ||
            label.contains("settings") || label.contains("files") || label.contains("contacts") ||
            label.contains("tools") || label.contains("compass")
        ) {
            return "Utilities"
        }

        // 7. Productivity & Finance
        if (app.category == ApplicationInfo.CATEGORY_PRODUCTIVITY ||
            pkg.contains("calendar") || pkg.contains("notes") || pkg.contains("keep") ||
            pkg.contains("reminder") || pkg.contains("mail") || pkg.contains("gmail") ||
            pkg.contains("outlook") || pkg.contains("drive") || pkg.contains("docs") ||
            pkg.contains("sheets") || pkg.contains("slides") || pkg.contains("office") ||
            pkg.contains("word") || pkg.contains("excel") || pkg.contains("powerpoint") ||
            pkg.contains("slack") || pkg.contains("teams") || pkg.contains("zoom") ||
            pkg.contains("meet") || pkg.contains("notion") || pkg.contains("trello") ||
            label.contains("calendar") || label.contains("notes") || label.contains("mail") ||
            label.contains("reminders") || label.contains("drive") || label.contains("docs")
        ) {
            return "Productivity & Finance"
        }

        // 8. Games
        if (app.category == ApplicationInfo.CATEGORY_GAME ||
            pkg.contains("game") || pkg.contains("play.games") || pkg.contains("steam") ||
            label.contains("game")
        ) {
            return "Games"
        }

        // 9. Information & Reading
        if (app.category == ApplicationInfo.CATEGORY_NEWS ||
            pkg.contains("news") || pkg.contains("books") || pkg.contains("kindle") ||
            pkg.contains("weather") || pkg.contains("browser") || pkg.contains("chrome") ||
            pkg.contains("firefox") || pkg.contains("opera") || pkg.contains("edge") ||
            label.contains("news") || label.contains("books") || label.contains("weather") ||
            label.contains("browser")
        ) {
            return "Information & Reading"
        }

        return "Other"
    }

    private fun defaultDock(installed: List<AppEntry>): List<String> {
        val byPackage = installed.groupBy { it.packageName }
        val preferred = DEFAULT_DOCK_ROLES
            .mapNotNull { role -> role.firstNotNullOfOrNull { byPackage[it]?.first()?.key } }
            .take(DEFAULT_DOCK_SIZE)
        if (preferred.size >= 4) return preferred
        return (preferred + installed.map { it.key }).distinct().take(DEFAULT_DOCK_SIZE)
    }

    private fun toast(message: String) = Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()

    private companion object {
        const val ICON_RENDER_DP = 80
        const val ICON_BATCH = 12
        const val DEFAULT_DOCK_SIZE = 4
        const val MAX_SUGGESTIONS = 8
        const val RECENTLY_ADDED_DAYS = 14L
        const val OTHER_GROUP = "Other"

        // The iOS default Dock — Phone, Browser, Messages, Camera — with Photos for tablets that have no dialer.
        // Each role lists the Samsung app first, then the Google one.
        val DEFAULT_DOCK_ROLES = listOf(
            listOf("com.samsung.android.dialer", "com.google.android.dialer"),
            listOf("com.sec.android.app.sbrowser", "com.android.chrome"),
            listOf("com.samsung.android.messaging", "com.google.android.apps.messaging"),
            listOf("com.sec.android.app.camera", "com.google.android.GoogleCamera", "com.android.camera2", "com.android.camera"),
            listOf("com.sec.android.gallery3d", "com.google.android.apps.photos"),
        )
    }
}
