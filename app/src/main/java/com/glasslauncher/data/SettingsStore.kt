package com.glasslauncher.data

import android.content.Context
import androidx.core.content.edit
import com.glasslauncher.wallpaper.DEFAULT_WALLPAPER
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

class SettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    private val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
    private val state = MutableStateFlow(read())
    val settings: StateFlow<LauncherSettings> = state.asStateFlow()

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        write(state.updateAndGet(transform))
    }

    private fun read(): LauncherSettings {
        val hasLegacyInstall = !prefs.contains(KEY_ONBOARDED) && (prefs.contains(KEY_WIDGET_GRID) || prefs.contains(KEY_WALLPAPER) || prefs.contains(KEY_GLASS_V2))
        val isSetupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, hasLegacyInstall)
        val isOnboarded = prefs.getBoolean(KEY_ONBOARDED, hasLegacyInstall)
        return LauncherSettings(
        wallpaper = prefs.getString(KEY_WALLPAPER, null) ?: DEFAULT_WALLPAPER,
        homeWallpaperMode = enumOr(prefs.getString(KEY_HOME_MODE, null), HomeWallpaperMode.Original),
        perspectiveZoom = prefs.getBoolean(KEY_PERSPECTIVE, false),
        shuffleIndex = prefs.getInt(KEY_SHUFFLE_INDEX, 0),
        iconStyle = parseStyle(prefs.getString(KEY_ICON_STYLE, null)),
        tintColor = prefs.getInt(KEY_TINT, DEFAULT_TINT),
        glassLook = enumOr(prefs.getString(KEY_GLASS_LOOK, null), GlassLook.Clear),
        // v2 moved the defaults to iOS's heavier frost; earlier saves of the old defaults start from the new ones.
        // v3 matches Apple's defaults: a very clear Dock and lightly frosted widgets. Saves from before start fresh.
        widgetFrost = if (prefs.getBoolean(KEY_GLASS_V2, false)) prefs.getFloat(KEY_WIDGET_FROST, 0.5f) else 0.5f,
        dockFrost = if (prefs.getBoolean(KEY_GLASS_V2, false)) prefs.getFloat(KEY_DOCK_FROST, 0.2f) else 0.2f,
        showLabels = prefs.getBoolean(KEY_SHOW_LABELS, true),
        blurUnderIcons = prefs.getBoolean(KEY_BLUR_UNDER_ICONS, true),
        showBadges = prefs.getBoolean(KEY_BADGES, true),
        wallpaperBlur = prefs.getFloat(KEY_WALLPAPER_BLUR, 0f).coerceIn(0f, 1f),
        autoArrange = prefs.getBoolean(KEY_AUTO_ARRANGE, true),
        notificationDisplay = enumOr(prefs.getString(KEY_NOTIFICATION_DISPLAY, null), NotificationDisplay.Stack),
        notificationGlassLook = enumOr(prefs.getString(KEY_NOTIFICATION_GLASS_LOOK, null), GlassLook.Clear),
        widgets = readWidgets(),
        appPositions = readPositions(KEY_APP_POSITIONS),
        homeFolders = readFolders(KEY_HOME_FOLDERS),
        widgetsLandscape = prefs.getString(KEY_WIDGET_GRID_LAND, null)?.let(::parseWidgets).orEmpty(),
        appPositionsLandscape = readPositions(KEY_APP_POSITIONS_LAND),
        homeFoldersLandscape = readFolders(KEY_HOME_FOLDERS_LAND),
        phoneGrid = enumOr(prefs.getString(KEY_PHONE_GRID, null), PhoneGrid.Auto),
        weatherPlace = prefs.getString(KEY_WEATHER_PLACE, null),
        dockRecents = true,
        glassStatusBar = true,
        dimWallpaperInDark = prefs.getBoolean(KEY_DIM_DARK, true),
        // The first versions only had an on/off "More icons" switch.
        iconPack = prefs.getString(KEY_ICON_PACK, null),
        iconLayout = enumOr(prefs.getString(KEY_ICON_LAYOUT, null), if (prefs.getBoolean(KEY_MORE_ICONS, false)) IconLayout.More else IconLayout.Default),
        customColumns = prefs.getInt(KEY_CUSTOM_COLUMNS, 6),
        noteText = run {
            val raw = prefs.getString(KEY_NOTES_LIST, null)
            val list = raw?.split(";;;;")?.filter { it.isNotBlank() }
                ?: (prefs.getString(KEY_NOTE, null)?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList())
            list.firstOrNull() ?: prefs.getString(KEY_NOTE, null).orEmpty()
        },
        notesList = run {
            val raw = prefs.getString(KEY_NOTES_LIST, null)
            raw?.split(";;;;")?.filter { it.isNotBlank() }
                ?: (prefs.getString(KEY_NOTE, null)?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList())
        },
        reminders = prefs.getString(KEY_REMINDERS, null)?.split(REMINDER_SEPARATOR)?.filter { it.isNotBlank() }.orEmpty(),
        todayWidgets = readToday(),
        iconScale = prefs.getFloat(KEY_ICON_SCALE, 1f).coerceIn(IconScaleRange),
        labelScale = prefs.getFloat(KEY_LABEL_SCALE, 1f).coerceIn(LabelScaleRange),
        lockClockStyle = enumOr(prefs.getString(KEY_LOCK_CLOCK, null), LockClockStyle.Bold),
        lockClockColor = prefs.getInt(KEY_LOCK_CLOCK_COLOR, 0xFFFFFFFF.toInt()),
        lockClockScale = prefs.getFloat(KEY_LOCK_CLOCK_SCALE, 1f).coerceIn(0.6f, 2.5f),
        lockClockOffsetX = prefs.getFloat(KEY_LOCK_CLOCK_OFFSET_X, 0f),
        lockClockOffsetY = prefs.getFloat(KEY_LOCK_CLOCK_OFFSET_Y, 0f),
        lockWidgets = prefs.getString(KEY_LOCK_WIDGETS, null)
            ?.let { names -> splitKeys(names).mapNotNull { name -> LockWidget.entries.firstOrNull { it.name == name } } }
            ?: listOf(LockWidget.Weather, LockWidget.Battery, LockWidget.NextEvent),
        lockWallpaper = prefs.getString(KEY_LOCK_WALLPAPER, "spiderman") ?: "spiderman",
        lock3dEffect = prefs.getBoolean(KEY_LOCK_3D, true),
        lockScreenEnabled = prefs.getBoolean(KEY_LOCK_ENABLED, true),
        lockWallpaperScale = prefs.getFloat(KEY_LOCK_WALLPAPER_SCALE, 1f).coerceIn(0.6f, 3.0f),
        lockWallpaperOffsetX = prefs.getFloat(KEY_LOCK_WALLPAPER_OFFSET_X, 0f),
        lockWallpaperOffsetY = prefs.getFloat(KEY_LOCK_WALLPAPER_OFFSET_Y, 0f),
        onboarded = isOnboarded,
        setupCompleted = isSetupCompleted,
        onboardingStep = prefs.getInt(KEY_ONBOARDING_STEP, 0),
        dock = prefs.getString(KEY_DOCK, null)?.let(::splitKeys),
        homeOrder = prefs.getString(KEY_HOME_ORDER, null)?.let(::splitKeys).orEmpty(),
        hiddenFromHome = prefs.getStringSet(KEY_HIDDEN, null)?.toSet().orEmpty(),
        hiddenApps = prefs.getStringSet(KEY_HIDDEN_APPS, null)?.toSet().orEmpty(),
    )
}

    private fun write(settings: LauncherSettings) = prefs.edit {
        putString(KEY_WALLPAPER, settings.wallpaper)
        putString(KEY_HOME_MODE, settings.homeWallpaperMode.name)
        putBoolean(KEY_PERSPECTIVE, settings.perspectiveZoom)
        putInt(KEY_SHUFFLE_INDEX, settings.shuffleIndex)
        putString(KEY_ICON_STYLE, settings.iconStyle.name)
        putInt(KEY_TINT, settings.tintColor)
        putString(KEY_GLASS_LOOK, settings.glassLook.name)
        putFloat(KEY_WIDGET_FROST, settings.widgetFrost)
        putFloat(KEY_DOCK_FROST, settings.dockFrost)
        putBoolean(KEY_GLASS_V2, true)
        putBoolean(KEY_SHOW_LABELS, settings.showLabels)
        putBoolean(KEY_BLUR_UNDER_ICONS, settings.blurUnderIcons)
        putBoolean(KEY_BADGES, settings.showBadges)
        putFloat(KEY_WALLPAPER_BLUR, settings.wallpaperBlur)
        putBoolean(KEY_AUTO_ARRANGE, settings.autoArrange)
        putString(KEY_NOTIFICATION_DISPLAY, settings.notificationDisplay.name)
        putString(KEY_NOTIFICATION_GLASS_LOOK, settings.notificationGlassLook.name)
        putString(KEY_WIDGET_GRID, settings.widgets.joinToString(SEPARATOR) {
            val s = if (it.stack.isNotEmpty()) ":" + it.stack.joinToString(",") { k -> k.name } else ""
            "${it.kind.name}:${it.page}:${it.col}:${it.row}:${it.w}:${it.h}$s"
        })
        putString(KEY_APP_POSITIONS, settings.appPositions.entries.joinToString(SEPARATOR) { "${it.key}=${it.value}" })
        putString(KEY_HOME_FOLDERS, serializeFolders(settings.homeFolders))
        putString(KEY_WIDGET_GRID_LAND, settings.widgetsLandscape.joinToString(SEPARATOR) {
            val s = if (it.stack.isNotEmpty()) ":" + it.stack.joinToString(",") { k -> k.name } else ""
            "${it.kind.name}:${it.page}:${it.col}:${it.row}:${it.w}:${it.h}$s"
        })
        putString(KEY_APP_POSITIONS_LAND, settings.appPositionsLandscape.entries.joinToString(SEPARATOR) { "${it.key}=${it.value}" })
        putString(KEY_HOME_FOLDERS_LAND, serializeFolders(settings.homeFoldersLandscape))
        putString(KEY_PHONE_GRID, settings.phoneGrid.name)
        putString(KEY_WEATHER_PLACE, settings.weatherPlace)
        putBoolean(KEY_DOCK_RECENTS, settings.dockRecents)
        putBoolean(KEY_GLASS_STATUS_BAR, settings.glassStatusBar)
        putBoolean(KEY_DIM_DARK, settings.dimWallpaperInDark)
        putString(KEY_ICON_PACK, settings.iconPack)
        putString(KEY_ICON_LAYOUT, settings.iconLayout.name)
        putInt(KEY_CUSTOM_COLUMNS, settings.customColumns)
        putString(KEY_NOTE, settings.noteText)
        putString(KEY_NOTES_LIST, settings.notesList.joinToString(";;;;"))
        putString(KEY_REMINDERS, settings.reminders.joinToString(REMINDER_SEPARATOR))
        putString(KEY_TODAY, settings.todayWidgets.joinToString(SEPARATOR) {
            val s = if (it.stack.isNotEmpty()) ":" + it.stack.joinToString(",") { k -> k.name } else ""
            "${it.kind.name}:${it.w}:${it.h}$s"
        })
        putFloat(KEY_ICON_SCALE, settings.iconScale)
        putFloat(KEY_LABEL_SCALE, settings.labelScale)
        putString(KEY_LOCK_CLOCK, settings.lockClockStyle.name)
        putInt(KEY_LOCK_CLOCK_COLOR, settings.lockClockColor)
        putFloat(KEY_LOCK_CLOCK_SCALE, settings.lockClockScale)
        putFloat(KEY_LOCK_CLOCK_OFFSET_X, settings.lockClockOffsetX)
        putFloat(KEY_LOCK_CLOCK_OFFSET_Y, settings.lockClockOffsetY)
        putString(KEY_LOCK_WIDGETS, settings.lockWidgets.joinToString(SEPARATOR) { it.name })
        putString(KEY_LOCK_WALLPAPER, settings.lockWallpaper)
        putBoolean(KEY_LOCK_3D, settings.lock3dEffect)
        putBoolean(KEY_LOCK_ENABLED, settings.lockScreenEnabled)
        putFloat(KEY_LOCK_WALLPAPER_SCALE, settings.lockWallpaperScale)
        putFloat(KEY_LOCK_WALLPAPER_OFFSET_X, settings.lockWallpaperOffsetX)
        putFloat(KEY_LOCK_WALLPAPER_OFFSET_Y, settings.lockWallpaperOffsetY)
        putBoolean(KEY_ONBOARDED, settings.onboarded)
        putBoolean(KEY_SETUP_COMPLETED, settings.setupCompleted)
        putInt(KEY_ONBOARDING_STEP, settings.onboardingStep)
        putString(KEY_DOCK, settings.dock?.joinToString(SEPARATOR))
        putString(KEY_HOME_ORDER, settings.homeOrder.joinToString(SEPARATOR))
        putStringSet(KEY_HIDDEN, settings.hiddenFromHome)
        putStringSet(KEY_HIDDEN_APPS, settings.hiddenApps)
    }

    // The iPad-style grid (v2) replaced the earlier layouts, so older arrangements start again from the defaults.
    private fun readWidgets(): List<WidgetPlacement> = prefs.getString(KEY_WIDGET_GRID, null)?.let(::parseWidgets) ?: defaultWidgets()

    private fun readPositions(key: String): Map<String, Int> = prefs.getString(key, null).orEmpty().split(SEPARATOR).mapNotNull { entry ->
        val app = entry.substringBeforeLast("=", "")
        val cell = entry.substringAfterLast("=", "").toIntOrNull()
        if (app.isEmpty() || cell == null) null else app to cell
    }.toMap()

    private fun parseWidgets(value: String): List<WidgetPlacement> {
        return splitKeys(value).mapNotNull { entry ->
            val parts = entry.split(":")
            val kind = WidgetKind.entries.firstOrNull { it.name == parts.getOrNull(0) } ?: return@mapNotNull null
            val numbers = parts.drop(1).take(5).mapNotNull { it.toIntOrNull() }
            if (numbers.size < 5) return@mapNotNull null
            val stack = parts.getOrNull(6)?.split(",")?.mapNotNull { sName ->
                WidgetKind.entries.firstOrNull { it.name == sName }
            }.orEmpty()
            WidgetPlacement(kind, numbers[0], numbers[1], numbers[2], numbers[3], numbers[4], stack)
        }
    }

    // iPad: balanced two rows of aesthetic widgets across page 0 and page 1; iPhone: widgets on first page.
    private fun defaultWidgets(): List<WidgetPlacement> = if (isTablet) {
        listOf(
            // Page 0: Core Daily Glance
            WidgetPlacement(WidgetKind.Weather, 0, 0, 0, 2, 1),
            WidgetPlacement(WidgetKind.Clock, 0, 2, 0, 2, 1),
            WidgetPlacement(WidgetKind.Batteries, 0, 4, 0, 1, 1),
            WidgetPlacement(WidgetKind.ScreenTime, 0, 0, 1, 2, 1),
            WidgetPlacement(WidgetKind.Calendar, 0, 2, 1, 2, 1),
            WidgetPlacement(WidgetKind.Hydration, 0, 4, 1, 1, 1),

            // Page 1: Media, Productivity & Utility
            WidgetPlacement(WidgetKind.Photos, 1, 0, 0, 2, 1),
            WidgetPlacement(WidgetKind.Music, 1, 2, 0, 2, 1),
            WidgetPlacement(WidgetKind.Storage, 1, 4, 0, 1, 1),
            WidgetPlacement(WidgetKind.Notes, 1, 0, 1, 2, 1),
            WidgetPlacement(WidgetKind.Reminders, 1, 2, 1, 2, 1),
            WidgetPlacement(WidgetKind.Activity, 1, 4, 1, 1, 1),
        )
    } else {
        listOf(
            WidgetPlacement(WidgetKind.Clock, 0, 0, 0, 1, 1),
            WidgetPlacement(WidgetKind.Hydration, 0, 2, 0, 1, 1),
            WidgetPlacement(WidgetKind.ScreenTime, 0, 0, 2, 2, 1),
        )
    }

    private fun readToday(): List<TodayWidget> {
        val value = prefs.getString(KEY_TODAY, null) ?: return defaultToday()
        return splitKeys(value).mapNotNull { entry ->
            val parts = entry.split(":")
            val kind = WidgetKind.entries.firstOrNull { it.name == parts.getOrNull(0) } ?: return@mapNotNull null
            val w = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 2) ?: 1
            val h = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 2) ?: 1
            val stack = parts.getOrNull(3)?.split(",")?.mapNotNull { name -> WidgetKind.entries.firstOrNull { it.name == name } }.orEmpty()
            TodayWidget(kind, w, h, stack)
        }
    }

    // The iOS Today View lineup; medium widgets are 2×1.
    private fun defaultToday(): List<TodayWidget> = listOf(
        TodayWidget(WidgetKind.Weather, if (isTablet) 2 else 1, 1),
        TodayWidget(WidgetKind.Calendar, 1, 1),
        TodayWidget(WidgetKind.Suggestions, 2, 1),
        TodayWidget(WidgetKind.Clock, 2, 1),
        TodayWidget(WidgetKind.Hydration, 2, 1),
        TodayWidget(WidgetKind.ScreenTime, 2, 1),
        TodayWidget(WidgetKind.Batteries, 1, 1),
        TodayWidget(WidgetKind.Music, 2, 1),
        TodayWidget(WidgetKind.Photos, 1, 1),
        TodayWidget(WidgetKind.Notes, 1, 1),
        TodayWidget(WidgetKind.UpNext, 2, 1),
        TodayWidget(WidgetKind.Reminders, 1, 1),
        TodayWidget(WidgetKind.WorldClock, 1, 1),
        TodayWidget(WidgetKind.Month, 1, 1),
        TodayWidget(WidgetKind.Activity, 1, 1),
    )

    private fun serializeFolders(folders: List<HomeFolder>): String =
        folders.joinToString(SEPARATOR) { "${it.id};${it.title};${it.appKeys.joinToString(",")};${it.cell}" }

    private fun readFolders(key: String): List<HomeFolder> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.mapNotNull { part ->
            val fields = part.split(";")
            if (fields.size >= 4) {
                val id = fields[0]
                val title = fields[1]
                val keys = fields[2].split(",").filter { it.isNotBlank() }
                val cell = fields[3].toIntOrNull() ?: 0
                HomeFolder(id, title, keys, cell)
            } else null
        }
    }

    private fun splitKeys(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    private fun parseStyle(name: String?): IconStyle = when (name) {
        null -> IconStyle.Default
        "Automatic" -> {
            val uiMode = context.resources.configuration.uiMode
            val isNight = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (isNight) IconStyle.Dark else IconStyle.Default
        }
        "Classic" -> IconStyle.Default
        else -> IconStyle.entries.firstOrNull { it.name == name } ?: IconStyle.Default
    }

    private companion object {
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_HOME_MODE = "home_wallpaper_mode"
        const val KEY_PERSPECTIVE = "perspective_zoom"
        const val KEY_SHUFFLE_INDEX = "shuffle_index"
        const val KEY_ICON_STYLE = "icon_style"
        const val KEY_TINT = "tint_color"
        const val KEY_GLASS_LOOK = "glass_look"
        const val KEY_WIDGET_FROST = "widget_frost"
        const val KEY_DOCK_FROST = "dock_frost"
        const val KEY_SHOW_LABELS = "show_labels"
        const val KEY_BLUR_UNDER_ICONS = "blur_under_icons"
        const val KEY_BADGES = "badges"
        const val KEY_NOTIFICATION_DISPLAY = "notification_display"
        const val KEY_NOTIFICATION_GLASS_LOOK = "notification_glass_look"
        const val KEY_WIDGET_GRID = "widget_grid_v3"
        const val KEY_APP_POSITIONS = "app_positions_v3"
        const val KEY_WIDGET_GRID_LAND = "widget_grid_land"
        const val KEY_APP_POSITIONS_LAND = "app_positions_land"
        const val KEY_WEATHER_PLACE = "weather_place"
        const val KEY_DOCK_RECENTS = "dock_recents"
        const val KEY_GLASS_STATUS_BAR = "glass_status_bar"
        const val KEY_DIM_DARK = "dim_dark"
        const val KEY_MORE_ICONS = "more_icons"
        const val KEY_ICON_LAYOUT = "icon_layout"
        const val KEY_PHONE_GRID = "phone_grid"
        const val KEY_HOME_FOLDERS = "home_folders_v1"
        const val KEY_HOME_FOLDERS_LAND = "home_folders_land_v1"
        const val KEY_ICON_PACK = "icon_pack"
        const val KEY_CUSTOM_COLUMNS = "custom_columns"
        const val KEY_NOTE = "note_text"
        const val KEY_NOTES_LIST = "notes_list"
        const val KEY_HIDDEN_APPS = "hidden_apps"
        const val KEY_WALLPAPER_BLUR = "wallpaper_blur"
        const val KEY_AUTO_ARRANGE = "auto_arrange"
        const val KEY_GLASS_V2 = "glass_v3"
        const val KEY_REMINDERS = "reminders"
        const val KEY_TODAY = "today_widgets"
        const val KEY_ICON_SCALE = "icon_scale"
        const val KEY_LABEL_SCALE = "label_scale"
        // A control character, so any text a reminder can hold round-trips.
        const val REMINDER_SEPARATOR = "\u001F"
        const val KEY_LOCK_CLOCK = "lock_clock_style"
        const val KEY_LOCK_CLOCK_COLOR = "lock_clock_color"
        const val KEY_LOCK_CLOCK_SCALE = "lock_clock_scale"
        const val KEY_LOCK_CLOCK_OFFSET_X = "lock_clock_offset_x"
        const val KEY_LOCK_CLOCK_OFFSET_Y = "lock_clock_offset_y"
        const val KEY_LOCK_WIDGETS = "lock_widgets"
        const val KEY_LOCK_WALLPAPER = "lock_wallpaper"
        const val KEY_LOCK_3D = "lock_3d_effect"
        const val KEY_LOCK_ENABLED = "lock_screen_enabled"
        const val KEY_LOCK_WALLPAPER_SCALE = "lock_wallpaper_scale"
        const val KEY_LOCK_WALLPAPER_OFFSET_X = "lock_wallpaper_offset_x"
        const val KEY_LOCK_WALLPAPER_OFFSET_Y = "lock_wallpaper_offset_y"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_SETUP_COMPLETED = "setup_completed"
        const val KEY_ONBOARDING_STEP = "onboarding_step"
        const val KEY_DOCK = "dock"
        const val KEY_HOME_ORDER = "home_order"
        const val KEY_HIDDEN = "hidden_from_home"
        const val SEPARATOR = "|"
        const val DEFAULT_TINT = 0xFFFF9F0A.toInt()

        // The iOS default Home Screen opens with Weather and Calendar.
    }
}
