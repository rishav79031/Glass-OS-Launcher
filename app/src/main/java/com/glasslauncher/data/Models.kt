package com.glasslauncher.data

import android.app.PendingIntent
import android.content.ComponentName
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import androidx.compose.runtime.Immutable

@Immutable
data class AppEntry(
    /** Stable id across launches: the component plus the profile's serial number. */
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val category: Int,
    val isSystem: Boolean,
    val updatedAt: Long,
    val installedAt: Long,
) {
    val packageName: String get() = component.packageName
}

@Immutable
data class AppGroup(val title: String, val apps: List<AppEntry>)

/** The iOS 26 icon appearances, plus a colourful glass variant of our own. */
enum class IconStyle(val title: String) {
    Automatic("Automatic"),
    Default("Default"),
    Dark("Dark"),
    Clear("Clear"),
    Tinted("Tinted"),
    Glass("Colour glass"),
}

/** iOS 26.1's Liquid Glass choice: Clear is more transparent, Tinted more opaque for contrast. */
enum class GlassLook(val title: String) { Clear("Clear"), Tinted("Tinted") }

/** iOS Lock Screen "Display As" for notifications. */
enum class NotificationDisplay(val title: String) { Count("Count"), Stack("Stack"), List("List"), None("None") }

/** iOS Home Screen wallpaper options when paired with a Lock Screen. */
enum class HomeWallpaperMode(val title: String) { Original("Original"), Blur("Blur"), Colour("Colour"), Gradient("Gradient") }

/** Widgets built into the launcher; nothing extra to install. */
enum class WidgetKind(val title: String) {
    Weather("Weather"),
    Calendar("Calendar"),
    Photos("Photos"),
    Clock("Clock"),
    Batteries("Batteries"),
    UpNext("Up Next"),
    Suggestions("Suggestions"),
    Music("Music"),
    WorldClock("World Clock"),
    Month("Month"),
    Notes("Notes"),
    Activity("Activity"),
    Reminders("Reminders"),
    Storage("Storage"),
    Hydration("Hydration"),
    ScreenTime("Screen Time"),
}

enum class PhoneGrid(val rows: Int, val title: String) {
    Auto(0, "Auto (Adaptive)"),
    Grid4x6(6, "4 × 6 (Standard)"),
    Grid4x7(7, "4 × 7 (Tall Screen)"),
}

@Immutable
data class HomeFolder(
    val id: String,
    val title: String,
    val appKeys: List<String>,
    val cell: Int,
)

/** iPadOS Home Screen layout: the default, "More" (smaller icons), or a grid the user picks. */
enum class IconLayout(val title: String) { Default("Default"), More("More Apps"), Custom("Customise") }

/** Lock Screen–style clock in Notification Centre. */
/** Lock Screen clock styles, like the iOS font picker: weights plus a rounded, a serif and a monospaced face. */
enum class LockClockStyle(val title: String) {
    Bold("Bold"), Regular("Regular"), Light("Light"), Heavy("Heavy"), Serif("Serif"), Mono("Mono"), Rounded("Rounded"),
}

/** Small complications under the Notification Centre clock, like iOS Lock Screen widgets. */
enum class LockWidget(val title: String) { Battery("Battery"), Weather("Weather"), NextEvent("Next Event"), WorldClock("World Clock") }

/**
 * A widget on the Home Screen grid. [w] and [h] are iOS sizes in widget units (small 1×1, medium 2×1, large 2×2,
 * extra large 4×2 on iPad); [col] and [row] are grid cells. A negative [col] means "not placed yet": it goes into
 * the first free spot.
 */
@Immutable
data class WidgetPlacement(
    val kind: WidgetKind,
    val page: Int,
    val col: Int,
    val row: Int,
    val w: Int,
    val h: Int,
    val stack: List<WidgetKind> = emptyList(),
) {
    val allKinds: List<WidgetKind> get() = if (stack.isNotEmpty()) listOf(kind) + stack else listOf(kind)

    fun covers(cols: Int, rows: Int): Set<Int> = buildSet {
        for (r in row until row + h) for (c in col until col + w) add(page * cols * rows + r * cols + c)
    }
}

/** An app shortcut shown in the long-press menu, like iOS Quick Actions. */
@Immutable
data class QuickAction(val label: String, val info: ShortcutInfo)

@Immutable
data class NotificationItem(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val intent: PendingIntent?,
    val autoCancel: Boolean,
)

@Immutable
data class ActiveCallInfo(
    val packageName: String,
    val caller: String,
    val detail: String,
    val intent: PendingIntent?,
    val startTime: Long = System.currentTimeMillis(),
)

@Immutable
data class ActiveTimerInfo(
    val packageName: String,
    val title: String,
    val text: String,
    val intent: PendingIntent?,
    val isChronometer: Boolean = false,
)

@Immutable
data class Weather(
    val place: String,
    val temperature: Int,
    val high: Int,
    val low: Int,
    val code: Int,
    val isDay: Boolean,
    val fetchedAt: Long,
    val hourly: List<HourForecast> = emptyList(),
)

/** One hour of the forecast, for the medium Weather widget. [hour] is local 0–23. */
@Immutable
data class HourForecast(val hour: Int, val temperature: Int, val code: Int, val isDay: Boolean)

@Immutable
data class LauncherSettings(
    val wallpaper: String,
    val homeWallpaperMode: HomeWallpaperMode,
    val perspectiveZoom: Boolean,
    val shuffleIndex: Int,
    val iconStyle: IconStyle,
    val tintColor: Int,
    val glassLook: GlassLook,
    /** 0 = fully clear, 1 = heavily frosted. */
    val widgetFrost: Float,
    val dockFrost: Float,
    /** How blurred the Home Screen wallpaper is: 0 sharp, 1 fully frosted. */
    val wallpaperBlur: Float,
    /** Apps always close up gaps, in order, as on iOS. */
    val autoArrange: Boolean,
    val showLabels: Boolean,
    val blurUnderIcons: Boolean,
    val showBadges: Boolean,
    val notificationDisplay: NotificationDisplay,
    val notificationGlassLook: GlassLook = GlassLook.Clear,
    /** Widgets on the Home Screen grid, each with its page, cell and size in cells. */
    val widgets: List<WidgetPlacement>,
    /** Cells (page × columns × rows index) of apps the user placed; others fill the free cells in order. */
    val appPositions: Map<String, Int>,
    val homeFolders: List<HomeFolder> = emptyList(),
    /** Landscape keeps its own arrangement; empty until you arrange it, and then it follows the portrait order. */
    val widgetsLandscape: List<WidgetPlacement>,
    val appPositionsLandscape: Map<String, Int>,
    val homeFoldersLandscape: List<HomeFolder> = emptyList(),
    val phoneGrid: PhoneGrid = PhoneGrid.Auto,
    /** "lat|lon|name" chosen by the user; null means use the device location. */
    val weatherPlace: String?,
    val dockRecents: Boolean,
    /** Hide Samsung's status bar on the Home Screen and show the Glass one, so the top-edge swipe opens Glass panels. */
    val glassStatusBar: Boolean,
    val dimWallpaperInDark: Boolean,
    /** Package of the chosen icon pack; null uses each app's own icon. */
    val iconPack: String?,
    val iconLayout: IconLayout,
    val customColumns: Int,
    val noteText: String,
    val notesList: List<String> = emptyList(),
    /** Open reminders, newest last. */
    val reminders: List<String>,
    /** Today View widgets in order, sized in iOS units (small 1×1, medium 2×1, large 2×2). */
    val todayWidgets: List<TodayWidget>,
    /** App icon size relative to the iOS default, within [IconScaleRange]. */
    val iconScale: Float,
    /** App name size relative to the default, within [LabelScaleRange]. */
    val labelScale: Float,
    val lockClockStyle: LockClockStyle,
    val lockClockColor: Int = 0xFFFFFFFF.toInt(),
    val lockClockScale: Float = 1.0f,
    val lockClockOffsetX: Float = 0f,
    val lockClockOffsetY: Float = 0f,
    val lockWidgets: List<LockWidget>,
    val lockWallpaper: String = "spiderman",
    val lock3dEffect: Boolean = true,
    val lockScreenEnabled: Boolean = true,
    val lockWallpaperScale: Float = 1.0f,
    val lockWallpaperOffsetX: Float = 0f,
    val lockWallpaperOffsetY: Float = 0f,
    val onboarded: Boolean,
    val setupCompleted: Boolean = false,
    /** Saved so setup resumes where it was: granting the Home role can restart the activity mid-setup. */
    val onboardingStep: Int,
    /** Dock app keys in order; null until defaults are picked on first run. */
    val dock: List<String>?,
    /** Home Screen order the user arranged; apps not listed follow alphabetically. */
    val homeOrder: List<String>,
    /** Apps removed from the Home Screen; they stay in the App Library. */
    val hiddenFromHome: Set<String>,
    /** Hidden apps: only in the App Library's locked Hidden folder, not on the Home Screen, Dock or in search. */
    val hiddenApps: Set<String>,
)

@Immutable
data class Usage(val counts: Map<String, Int>, val recents: List<String>)

/** How far the icon size setting can go: past this, labels collide or icons look lost in their cells. */
val IconScaleRange = 0.85f..1.15f

/** A widget in Today View: its kind and iOS size in widget units. */
@Immutable
data class TodayWidget(
    val kind: WidgetKind,
    val w: Int,
    val h: Int,
    val stack: List<WidgetKind> = emptyList(),
) {
    val allKinds: List<WidgetKind> get() = if (stack.isNotEmpty()) listOf(kind) + stack else listOf(kind)
}

/** How far the app name size setting can go: smaller gets hard to read, larger runs into the next row. */
val LabelScaleRange = 0.70f..1.20f
