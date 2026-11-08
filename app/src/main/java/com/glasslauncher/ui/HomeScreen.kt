package com.glasslauncher.ui
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Add
import com.glasslauncher.data.TodayWidget
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.MutableIntState
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.unit.DpSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.zIndex
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.data.AppEntry
import com.glasslauncher.data.AppGroup
import com.glasslauncher.data.LauncherSettings
import com.glasslauncher.data.WidgetKind
import com.glasslauncher.data.WidgetPlacement
import com.glasslauncher.data.IconLayout
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Grid, Dock and type sizes: iPhone proportions on phones, iPadOS proportions on tablets
 * (5×6 portrait / 6×5 landscape, or 6×7 / 8×5 with "More" icons).
 */
@Immutable
data class DeviceLayout(
    val isTablet: Boolean,
    val iconSize: Dp,
    val labelSize: TextUnit,
    val columns: Int,
    val rows: Int,
    val sidePadding: Dp,
    val dockIconSize: Dp,
    val maxDock: Int,
    val dockRecents: Int,
    /** Grid cells per widget unit: an iPad small widget fills one cell, an iPhone one fills 2×2. */
    val widgetCell: Int,
    /** Side of a small widget. */
    val widgetUnit: Dp,
) {
    companion object {
        fun compute(
            width: Dp,
            height: Dp,
            mode: IconLayout,
            customColumns: Int,
            showLabels: Boolean,
            iconScale: Float = 1f,
            labelScale: Float = 1f,
            phoneGrid: com.glasslauncher.data.PhoneGrid = com.glasslauncher.data.PhoneGrid.Auto,
        ): DeviceLayout {
            val isTablet = minOf(width, height) >= 600.dp
            val landscape = width > height
            val label = if (showLabels) 22.dp * labelScale else 4.dp
            if (!isTablet) {
                val base = when (mode) {
                    IconLayout.Default, IconLayout.Custom -> 60.dp
                    IconLayout.More -> 52.dp
                }
                // Portrait is the iPhone grid (4x6 or 4x7 on tall screens)
                val columns = if (landscape) ((width - 44.dp) / (60.dp + 40.dp)).toInt().coerceIn(5, 9) else 4
                val cellW = (width - 44.dp) / columns
                // Smaller phones shrink icons a little so labels and rows still fit.
                val chromeFor = { icon: Dp -> icon * 1.6f + 28.dp + 12.dp + 40.dp + 12.dp } // dock, its margin, pill, top
                val rows = if (landscape) {
                    ((height - chromeFor(60.dp)) / (60.dp + label + 26.dp)).toInt().coerceIn(2, 4)
                } else when (phoneGrid) {
                    com.glasslauncher.data.PhoneGrid.Grid4x7 -> 7
                    com.glasslauncher.data.PhoneGrid.Grid4x6 -> 6
                    com.glasslauncher.data.PhoneGrid.Auto -> {
                        // Taller screens that can accommodate 7 rows automatically use 7 rows, otherwise 6 rows
                        if (height >= 730.dp || (height / width) >= 1.95f) 7 else 6
                    }
                }
                // The icon size setting scales icons inside their (fixed, square) cells; widgets follow the icons.
                val icon = minOf(60.dp * iconScale, cellW * 0.78f, (height - chromeFor(60.dp)) / rows - label - 10.dp).coerceAtLeast(32.dp)
                val cellH = (height - chromeFor(icon)) / rows
                val unit = cellW + icon
                return DeviceLayout(false, icon, 12.sp * labelScale, columns, rows, 22.dp, minOf(60.dp * iconScale, cellW * 0.8f), 4, 0, 2, unit)
            }
            // iPadOS: Default is 6×4 in landscape and 5×6 in portrait, with wide margins; More is 8×5 / 6×6.
            val columns = when (mode) {
                IconLayout.Default -> if (landscape) 6 else 5
                IconLayout.More -> if (landscape) 8 else 6
                IconLayout.Custom -> customColumns.coerceIn(4, 10)
            }
            val dockIcon = (if (mode == IconLayout.Default) 64.dp else 60.dp) * iconScale
            // Dock with its margins, the page dots, and the grid's own padding.
            val chrome = dockIcon + 28.dp + 14.dp + 38.dp + 12.dp
            val baseIcon = (if (mode == IconLayout.More) 64.dp else 72.dp) * iconScale
            val fit = ((height - chrome) / (baseIcon + label + 24.dp)).toInt()
            val rows = when (mode) {
                IconLayout.Default -> minOf(if (landscape) 4 else 6, fit).coerceAtLeast(3)
                IconLayout.More -> fit.coerceIn(4, if (landscape) 5 else 6)
                IconLayout.Custom -> fit.coerceIn(4, if (landscape) 5 else 7)
            }
            val cellH = (height - chrome) / rows
            // Like iPadOS, the side margins grow until the cells are about square, so spacing is even both ways.
            val side = ((width - cellH * columns) / 2).coerceIn(width * 0.035f, width * 0.10f)
            val cellWidth = (width - side * 2) / columns
            val icon = minOf(baseIcon, cellWidth * 0.62f)
            // The Dock grows with the icon setting, but never so far that the Dock outgrows the screen.
            val dock = minOf(dockIcon, (width - 160.dp) / 9)
            val unit = minOf(cellWidth, cellH) * 0.85f
            val maxDock = ((width - 120.dp) / (dock + 18.dp)).toInt().coerceIn(4, 13) - 4 // room for the recents section
            return DeviceLayout(true, icon, 13.sp * labelScale, columns, rows, side, dock, maxDock.coerceAtLeast(4), 3, 1, unit)
        }
    }
}

/** Widget sizes in widget units, as iOS offers them: small, medium, large, and extra large on iPad. */
fun widgetSizes(layout: DeviceLayout): List<Pair<Int, Int>> =
    if (layout.isTablet) listOf(1 to 1, 2 to 1, 2 to 2, 4 to 2) else listOf(1 to 1, 2 to 1, 2 to 2)

/** Widgets are designed at this small-widget side and scaled to the screen, keeping iOS proportions everywhere. */
val WidgetBase = 170.dp

/** A widget of [size] drawn from its [WidgetBase] design, scaled by [unit] / [WidgetBase]. */
@Composable
fun ScaledWidget(
    kind: WidgetKind,
    size: DpSize,
    unit: Dp,
    suggestions: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    onLaunch: (AppEntry, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = (unit / WidgetBase).coerceIn(0.5f, 1.2f)
    // Shrinking the density (rather than scaling a finished picture) lays the widget out at its real pixel size,
    // so text and icons are drawn sharp at every widget size. Font scale stays 1, as iOS widgets ignore it.
    val density = LocalDensity.current
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalDensity provides Density(density.density * scale, 1f)) {
            LauncherWidget(kind, Modifier.requiredSize(size.width / scale, size.height / scale), suggestions, icons, onLaunch)
        }
    }
}

/** The icon being dragged in jiggle mode, drawn above everything and following the finger. */
@Stable
class DragState {
    var key by mutableStateOf<String?>(null)
        private set
    var fromDock by mutableStateOf(false)
        private set
    var pointer by mutableStateOf(Offset.Zero)
    var icon: ImageBitmap? = null
        private set
    var sizePx = 0f
        private set

    fun start(key: String, fromDock: Boolean, at: Offset, icon: ImageBitmap?, sizePx: Float) {
        this.key = key
        this.fromDock = fromDock
        this.icon = icon
        this.sizePx = sizePx
        pointer = at
    }

    fun end() {
        key = null
    }
}

@Composable
fun HomeScreen(
    allApps: List<AppEntry>,
    hiddenApps: List<AppEntry>,
    homeApps: List<AppEntry>,
    groups: List<AppGroup>,
    suggestions: List<AppEntry>,
    recents: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    badges: Map<String, Int>,
    settings: LauncherSettings,
    homeTick: Int,
    editing: Boolean,
    onLaunch: (AppEntry, Rect) -> Unit,
    onMenu: (AppEntry, Rect) -> Unit,
    onOpenGroup: (AppGroup) -> Unit,
    onOpenSearch: () -> Unit,
    onSwipeDown: (Offset) -> Unit,
    onStartEditing: () -> Unit,
    onStopEditing: () -> Unit,
    onRemove: (AppEntry) -> Unit,
    onSaveLayout: (Map<String, Int>, List<WidgetPlacement>, String?, Boolean, List<com.glasslauncher.data.HomeFolder>) -> Unit = { _, _, _, _, _ -> },
    arrangeRequests: kotlinx.coroutines.flow.Flow<Unit>,
    onDropToDock: (String) -> Unit,
    onRemoveWidget: (WidgetPlacement) -> Unit,
    onOpenWidgetGallery: () -> Unit,
    onSaveToday: (List<TodayWidget>) -> Unit,
    onOpenTodayGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWallpaper: () -> Unit = onOpenSettings,
    onOpenThemes: () -> Unit = onOpenSettings,
    onOpenCustomise: () -> Unit = onOpenSettings,
    onLayout: (DeviceLayout) -> Unit,
    pageBlur: MutableFloatState,
    homePage: MutableIntState,
    statusBar: Boolean,
    onOpenControlCentre: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val swipeDown by rememberUpdatedState(onSwipeDown)
    val startEditing by rememberUpdatedState(onStartEditing)
    val density = LocalDensity.current
    val context = LocalContext.current
    val threshold = with(density) { 90.dp.toPx() }
    val drag = remember { DragState() }
    val dockAnchor = remember { Anchor() }
    // Today View has its own edit mode, so a long press there mustn't also start Home Screen editing.
    val onToday = remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Box(
        modifier
            .pointerInput(threshold) {
                var start = Offset.Zero
                var travel = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        start = it
                        travel = 0f
                    },
                    onDragEnd = { if (travel > threshold) swipeDown(start) },
                ) { _, dragAmount -> travel += dragAmount }
            }
            // Icons consume their own long-presses, so this only fires on empty space: iOS enters jiggle mode.
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    if (!onToday.value) {
                        AppleTaptic.heavy(context)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        startEditing()
                    }
                })
            },
    ) {
        if (statusBar) {
            val isTabletDevice = minOf(LocalConfiguration.current.screenWidthDp, LocalConfiguration.current.screenHeightDp) >= 600
            GlassStatusBar(
                isTablet = isTabletDevice,
                icons = icons,
                onOpenControlCentre = if (isTabletDevice) onOpenControlCentre else onOpenNotifications,
                onOpenNotifications = onOpenNotifications,
                onDynamicIslandClick = {
                    onOpenNotifications?.invoke()
                },
            )
        }
        AnimatedVisibility(editing && !onToday.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)) {
            val layout = DeviceLayout.compute(
                LocalConfiguration.current.screenWidthDp.dp,
                LocalConfiguration.current.screenHeightDp.dp,
                settings.iconLayout,
                settings.customColumns,
                settings.showLabels,
                settings.iconScale,
                settings.labelScale,
                settings.phoneGrid,
            )
            EditBar(layout, onOpenWidgetGallery, onOpenThemes, onOpenCustomise, onOpenWallpaper, onStopEditing)
        }
        // With the Glass status bar, pages start right under it (its band already covers the camera cutout).
        val topBar = if (statusBar) glassStatusBarHeight() else 0.dp
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(if (statusBar) HomeInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom) else HomeInsets)
                .padding(top = topBar),
        ) {
            val layout = remember(maxWidth, maxHeight, settings.iconLayout, settings.customColumns, settings.showLabels, settings.iconScale, settings.labelScale, settings.phoneGrid) {
                DeviceLayout.compute(maxWidth, maxHeight, settings.iconLayout, settings.customColumns, settings.showLabels, settings.iconScale, settings.labelScale, settings.phoneGrid)
            }
            LaunchedEffect(layout) { onLayout(layout) }
            val look = IconLook(layout.iconSize, settings.iconStyle, settings.showLabels, settings.blurUnderIcons, layout.labelSize)
            val dockLook = look.copy(size = layout.dockIconSize, showLabel = false)
            var lastKnownDockApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
            val dockApps = remember(allApps, settings.dock, layout.maxDock) {
                val byKey = allApps.associateBy { it.key }
                val computed = settings.dock.orEmpty().mapNotNull { byKey[it] }.take(layout.maxDock)
                if (computed.isNotEmpty()) {
                    lastKnownDockApps = computed
                    computed
                } else if (allApps.isEmpty() && lastKnownDockApps.isNotEmpty()) {
                    lastKnownDockApps
                } else {
                    computed
                }
            }
            val dockRecents = remember(recents, dockApps, layout.dockRecents, settings.dockRecents) {
                if (!settings.dockRecents) emptyList() else recents.filter { it !in dockApps }.take(layout.dockRecents)
            }
            // Widgets are saved in iOS sizes (small = 1×1); on iPhone each unit spans 2×2 cells, on iPad one cell.
            // So a foldable switching between the two keeps the same widget sizes.
            val per = layout.widgetCell
            // Portrait and landscape each keep their own arrangement. One you haven't arranged yet follows the
            // other's order, reflowed for its grid.
            val landscape = maxWidth > maxHeight
            val savedPositions = if (landscape) settings.appPositionsLandscape else settings.appPositions
            val otherPositions = if (landscape) settings.appPositions else settings.appPositionsLandscape
            val savedWidgets = if (landscape && settings.widgetsLandscape.isNotEmpty()) settings.widgetsLandscape else settings.widgets
            val savedFolders = if (landscape) settings.homeFoldersLandscape else settings.homeFolders
            val orderedApps = remember(homeApps, savedPositions, otherPositions) {
                if (savedPositions.isEmpty() && otherPositions.isNotEmpty()) homeApps.sortedBy { otherPositions[it.key] ?: Int.MAX_VALUE } else homeApps
            }
            var lastKnownGrid by remember { mutableStateOf<HomeGrid?>(null) }
            val grid = remember(orderedApps, savedPositions, savedWidgets, savedFolders, layout.columns, layout.rows, per, settings.autoArrange) {
                if (orderedApps.isEmpty() && lastKnownGrid != null && allApps.isEmpty()) {
                    lastKnownGrid!!
                } else {
                    val placed = layoutGrid(orderedApps, savedPositions, savedWidgets.map { it.copy(w = it.w * per, h = it.h * per) }, layout.columns, layout.rows, savedFolders)
                    val computed = if (settings.autoArrange) layoutGrid(orderedApps, compactGrid(placed, layout.columns, layout.rows), placed.widgets, layout.columns, layout.rows, savedFolders) else placed
                    if (orderedApps.isNotEmpty()) lastKnownGrid = computed
                    computed
                }
            }
            fun stored(widgets: List<WidgetPlacement>) = widgets.map { it.copy(w = (it.w / per).coerceAtLeast(1), h = (it.h / per).coerceAtLeast(1)) }
            fun save(positions: Map<String, Int>, widgets: List<WidgetPlacement>, fromDock: String? = null, folders: List<com.glasslauncher.data.HomeFolder> = savedFolders) =
                onSaveLayout(positions, stored(widgets), fromDock, landscape, folders)

            // Apps keep their order but close up every gap, so space a widget gave back is filled from later pages.
            fun compacted(widgets: List<WidgetPlacement>): Map<String, Int> = compactGrid(grid, layout.columns, layout.rows, widgets)

            val arrange by rememberUpdatedState { save(compacted(grid.widgets), grid.widgets) }
            LaunchedEffect(arrangeRequests) { arrangeRequests.collect { arrange() } }

            fun removeWidget(placement: WidgetPlacement) {
                val remaining = grid.widgets.filterNot { it.kind == placement.kind && it.page == placement.page }
                onRemoveWidget(placement)
                save(compacted(remaining), remaining)
            }

            fun removeFolder(folder: com.glasslauncher.data.HomeFolder) {
                val updatedFolders = savedFolders.filterNot { it.id == folder.id }
                val positions = grid.positions.toMutableMap()
                val takenCells = (grid.widgetCells + updatedFolders.map { it.cell } + positions.values).toMutableSet()
                var targetCell = folder.cell
                for (appKey in folder.appKeys) {
                    while (targetCell in takenCells) {
                        targetCell++
                    }
                    positions[appKey] = targetCell
                    takenCells.add(targetCell)
                }
                save(positions, grid.widgets, null, updatedFolders)
            }

            fun renameFolder(folder: com.glasslauncher.data.HomeFolder, newTitle: String) {
                val clean = newTitle.trim().ifEmpty { com.glasslauncher.data.LanguageHelper.t("folder") }
                val updated = savedFolders.map {
                    if (it.id == folder.id) it.copy(title = clean) else it
                }
                save(grid.positions, grid.widgets, null, updated)
            }

            var renamingFolder by remember { mutableStateOf<com.glasslauncher.data.HomeFolder?>(null) }
            var folderMenu by remember { mutableStateOf<Pair<com.glasslauncher.data.HomeFolder, Rect>?>(null) }
            val pages = remember(grid.pages, editing, drag.key) {
                if (editing && drag.key != null) {
                    grid.pages + PageContent(emptyList(), emptyList())
                } else {
                    grid.pages
                }
            }
            val pageRects = remember { HashMap<Int, Rect>() }
            // Today View, the Home Screen pages, then the App Library — the iOS page order.
            val pagerState = rememberPagerState(initialPage = 1) { pages.size + 2 }
            LaunchedEffect(homeTick) { if (homeTick > 0) pagerState.animateScrollToPage(1, animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f)) }
            LaunchedEffect(pagerState) { snapshotFlow { pagerState.currentPage == 0 }.collect { onToday.value = it } }
            // Which Home Screen page is showing, so Add Widget puts the widget on this page.
            LaunchedEffect(pagerState, pages.size) {
                snapshotFlow { pagerState.currentPage }.collect { homePage.intValue = (it - 1).coerceIn(0, pages.size - 1) }
            }
            // Like iOS, the wallpaper blurs as you swipe into Today View or the App Library.
            LaunchedEffect(pagerState, pages.size) {
                snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }.collect { position ->
                    pageBlur.floatValue = maxOf((1f - position).coerceIn(0f, 1f), (position - pages.size).coerceIn(0f, 1f))
                }
            }
            val badgeFor: (AppEntry) -> Int = { app -> if (settings.showBadges && !editing) badges[app.packageName] ?: 0 else 0 }
            val topInset by animateDpAsState(if (editing) 52.dp else 0.dp, label = "editInset")
            val widthPx = with(density) { maxWidth.toPx() }

            // Drop an app on any cell: snaps strictly to grid, creates/merges folders when dropped over an app or folder
            fun drop() {
                val key = drag.key ?: return
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val point = drag.pointer
                val fromDock = drag.fromDock
                val draggedSizePx = drag.sizePx
                drag.end()
                if (dockAnchor.bounds().contains(point)) {
                    if (!fromDock) onDropToDock(key)
                    return
                }
                val page = (pagerState.currentPage - 1).coerceIn(0, pages.size - 1)
                if (page !in pages.indices) return

                val sidePaddingPx = with(density) { layout.sidePadding.toPx() }
                val topBarPx = with(density) { (if (statusBar) 44.dp else 24.dp).toPx() }
                val gridTopPx = with(density) { topInset.toPx() } + topBarPx
                val availableWidth = (widthPx - sidePaddingPx * 2).coerceAtLeast(1f)
                val cellWPx = (availableWidth / layout.columns).coerceAtLeast(1f)
                val cellHPx = with(density) {
                    val phonePortrait = !layout.isTablet && maxHeight > maxWidth
                    if (phonePortrait) {
                        if (layout.rows == 7) (maxHeight - 4.dp).toPx() / 7f
                        else minOf(maxHeight.toPx() / layout.rows, maxOf(cellWPx, (layout.iconSize + 24.dp).toPx()))
                    } else {
                        maxHeight.toPx() / layout.rows
                    }
                }
                val col = ((point.x - sidePaddingPx) / cellWPx).toInt().coerceIn(0, layout.columns - 1)
                val row = ((point.y - gridTopPx) / cellHPx).toInt().coerceIn(0, layout.rows - 1)

                val target = page * layout.columns * layout.rows + row * layout.columns + col

                if (target in grid.widgetCells) {
                    val positions = grid.positions.toMutableMap()
                    val taken = (grid.widgetCells + savedFolders.map { it.cell } + positions.values).toMutableSet()
                    val perPage = layout.columns * layout.rows
                    val pageStart = page * perPage
                    val pageEnd = pageStart + perPage
                    var free = (pageStart until pageEnd).firstOrNull { it !in taken }
                    if (free == null) {
                        free = pageEnd
                        while (free in taken) free++
                    }
                    positions[key] = free
                    save(positions, grid.widgets, if (fromDock) key else null, savedFolders)
                    return
                }

                // Calculate distance from drop point to cell center
                val targetCenterX = sidePaddingPx + col * cellWPx + cellWPx / 2f
                val targetCenterY = gridTopPx + row * cellHPx + cellHPx / 2f
                val dist = kotlin.math.hypot(point.x - targetCenterX, point.y - targetCenterY)
                val isOverItem = dist <= maxOf(cellWPx, cellHPx) * 0.70f

                // 1. Check if dropped onto an existing folder
                val targetFolder = savedFolders.firstOrNull { it.cell == target }
                if (targetFolder != null) {
                    if (key !in targetFolder.appKeys) {
                        val updated = savedFolders.map {
                            if (it.id == targetFolder.id) it.copy(appKeys = it.appKeys + key) else it
                        }
                        val positions = grid.positions.toMutableMap()
                        positions.remove(key)
                        save(positions, grid.widgets, if (fromDock) key else null, updated)
                    }
                    return
                }

                // 2. Check if dropped onto another app: place at target and shift occupant down (reorder smoothly)
                val positions = grid.positions.toMutableMap()
                val occupant = positions.entries.firstOrNull { it.value == target && it.key != key }?.key
                if (occupant != null) {
                    val takenCells = (grid.widgetCells + savedFolders.map { it.cell }).toMutableSet()
                    val sortedOccupants = positions.entries
                        .filter { it.key != key }
                        .sortedBy { it.value }

                    val newPositions = LinkedHashMap<String, Int>()
                    newPositions[key] = target
                    takenCells.add(target)

                    for (entry in sortedOccupants) {
                        if (entry.value < target) {
                            newPositions[entry.key] = entry.value
                            takenCells.add(entry.value)
                        } else {
                            var newCell = entry.value
                            if (newCell == target || newCell in takenCells) {
                                newCell = entry.value + 1
                                while (newCell in takenCells) {
                                    newCell++
                                }
                            }
                            newPositions[entry.key] = newCell
                            takenCells.add(newCell)
                        }
                    }
                    save(newPositions, grid.widgets, if (fromDock) key else null, savedFolders)
                    return
                }

                // 3. Move to empty cell
                positions[key] = target
                save(positions, grid.widgets, if (fromDock) key else null, savedFolders)
            }

            // Move or resize a widget: if dropped onto another widget of same size, stacks them into a Smart Stack.
            fun changeWidget(updated: WidgetPlacement) {
                val cols = layout.columns
                val rows = layout.rows
                val fitted = updated.copy(
                    w = updated.w.coerceAtMost(cols),
                    h = updated.h.coerceAtMost(rows),
                ).let { it.copy(col = it.col.coerceIn(0, cols - it.w), row = it.row.coerceIn(0, rows - it.h)) }
                val cells = fitted.covers(cols, rows)
                val same: (WidgetPlacement) -> Boolean = { it.kind == fitted.kind && it.page == fitted.page && it.stack == fitted.stack }

                // Check if user dropped this widget onto another widget of the exact same size on the same page
                val target = grid.widgets.firstOrNull { other ->
                    !same(other) && other.page == fitted.page && other.w == fitted.w && other.h == fitted.h &&
                    other.col == fitted.col && other.row == fitted.row
                }
                if (target != null) {
                    val combined = (target.allKinds + fitted.allKinds).distinct()
                    val stacked = target.copy(kind = combined.first(), stack = combined.drop(1))
                    val newWidgets = grid.widgets.filter { !same(it) && it != target } + stacked
                    save(compacted(newWidgets), newWidgets)
                    return
                }

                if (grid.widgets.any { !same(it) && it.covers(cols, rows).any(cells::contains) }) return
                val widgets = grid.widgets.map { if (same(it)) fitted else it }
                save(compacted(widgets), widgets)
            }

            // While dragging, resting near a screen edge turns the page smoothly, like iOS.
            val edgeThreshold = with(density) { 72.dp.toPx() }
            LaunchedEffect(drag.key) {
                if (drag.key == null) return@LaunchedEffect
                var hoverStartTime = 0L
                var hoverDirection = 0

                while (drag.key != null) {
                    kotlinx.coroutines.delay(25)
                    val x = drag.pointer.x
                    val page = pagerState.currentPage
                    val targetDirection = when {
                        x < edgeThreshold && page > 1 -> -1
                        x > widthPx - edgeThreshold && page < pages.size -> 1
                        else -> 0
                    }

                    if (targetDirection == 0) {
                        hoverDirection = 0
                        hoverStartTime = 0L
                    } else {
                        val now = System.currentTimeMillis()
                        if (hoverDirection != targetDirection) {
                            hoverDirection = targetDirection
                            hoverStartTime = now
                        } else if (now - hoverStartTime >= 260) {
                            if (!pagerState.isScrollInProgress) {
                                val targetPage = page + hoverDirection
                                if (targetPage in 1..pages.size) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pagerState.animateScrollToPage(
                                        targetPage,
                                        animationSpec = spring(dampingRatio = 0.88f, stiffness = 400f)
                                    )
                                    kotlinx.coroutines.delay(400)
                                    hoverStartTime = System.currentTimeMillis()
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(drag.key) {
                if (drag.key == null && pagerState.currentPage > grid.pages.size) {
                    pagerState.animateScrollToPage(
                        grid.pages.size.coerceAtLeast(1),
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 350f)
                    )
                }
            }

            fun dragModifier(app: AppEntry, anchor: Anchor, fromDock: Boolean): Modifier =
                if (!editing) {
                    Modifier
                } else {
                    Modifier
                        .graphicsLayer { alpha = if (drag.key == app.key) 0f else 1f }
                        .pointerInput(app.key, fromDock) {
                            detectDragGestures(
                                onDragStart = { local ->
                                    val bounds = anchor.bounds()
                                    drag.start(app.key, fromDock, Offset(bounds.left + local.x, bounds.top + local.y), icons[app.key], bounds.width)
                                },
                                onDragEnd = { drop() },
                                onDragCancel = { drop() },
                            ) { change, amount ->
                                change.consume()
                                drag.pointer += amount
                            }
                        }
                }

            // Also true while something above is animating the whole screen (the unlock zoom).
            val outerMotion = LocalGlassMotion.current
            val moving = remember(pagerState, outerMotion) { derivedStateOf { pagerState.isScrollInProgress || drag.key != null || outerMotion.value } }
            CompositionLocalProvider(LocalGlassMotion provides moving) {
            // The page dots and Dock float over the pages, so Today View and the App Library can use the full height
            // with the Dock slid away, as on iOS.
            var chromeHeight by remember(layout) { mutableStateOf(layout.dockIconSize + 70.dp) }
            val effectiveSuggestions = remember(suggestions, allApps) { suggestions.ifEmpty { allApps.take(8) } }
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !editing && drag.key == null,
                    // While an icon is dragged every page stays composed, so the drag isn't cancelled when its page scrolls away.
                    beyondViewportPageCount = if (drag.key != null) pages.size + 1 else 1,
                    // Pages settle on a soft spring, like iOS, instead of a fixed-time slide.
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapAnimationSpec = spring(dampingRatio = 0.88f, stiffness = 340f),
                    ),
                ) { page ->
                    when {
                        page == 0 -> TodayPage(layout, settings.todayWidgets, effectiveSuggestions, icons, onLaunch, onOpenSearch, onSaveToday, onOpenTodayGallery)
                        page <= pages.size -> {
                            val index = page - 1
                            Box(Modifier.fillMaxSize().padding(bottom = chromeHeight)) {
                                GridPage(
                                    content = pages[index],
                                    layout = layout,
                                    look = look,
                                    icons = icons,
                                    suggestions = effectiveSuggestions,
                                    editing = editing,
                                    topInset = topInset,
                                    badgeFor = badgeFor,
                                    onRect = { pageRects[index] = it },
                                    onLaunch = onLaunch,
                                    onMenu = onMenu,
                                    onRemove = onRemove,
                                    onRemoveWidget = ::removeWidget,
                                    onChangeWidget = ::changeWidget,
                                    onRemoveFolder = ::removeFolder,
                                    onRenameFolder = { renamingFolder = it },
                                    onFolderMenu = { f, r -> folderMenu = f to r },
                                    dragModifier = ::dragModifier,
                                    onOpenGroup = onOpenGroup,
                                    allAppsByKey = remember(allApps) { allApps.associateBy { it.key } },
                                )
                            }
                        }
                        else -> AppLibraryPage(groups, hiddenApps, icons, look, layout, onLaunch, onMenu, onOpenGroup, onOpenSearch)
                    }
                }
                run {
                    // Slid fully off-screen (so it also stops catching touches) rather than removed, so swiping to
                    // Today View or the App Library never rebuilds the Dock.
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { chromeHeight = with(density) { it.height.toDp() } }
                            .graphicsLayer {
                                val away = pageBlur.floatValue
                                alpha = 1f - away
                                translationY = (size.height + 40f) * away
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SearchPill(
                            pagerState = pagerState,
                            homePages = pages.size,
                            onClick = onOpenSearch,
                            dotsOnly = false,
                            // A little more air above the Dock on tablets, where the Dock itself is larger.
                            modifier = Modifier.padding(top = if (layout.isTablet) 2.dp else 4.dp, bottom = if (layout.isTablet) 12.dp else 18.dp),
                        )
                        Dock(
                            apps = dockApps,
                            recents = dockRecents,
                            icons = icons,
                            look = dockLook,
                            layout = layout,
                            editing = editing,
                            anchor = dockAnchor,
                            badgeFor = badgeFor,
                            onLaunch = onLaunch,
                            onMenu = onMenu,
                            onRemove = onRemove,
                            dragModifier = ::dragModifier,
                            modifier = Modifier
                                .padding(bottom = if (layout.isTablet) 14.dp else 8.dp)
                                .padding(horizontal = if (layout.isTablet) 0.dp else 8.dp),
                        )
                    }
                }
            }

            folderMenu?.let { (folder, anchor) ->
                val actions = listOf(
                    MenuEntry("Rename Folder", Icons.Rounded.Edit) {
                        val f = folder
                        folderMenu = null
                        renamingFolder = f
                    },
                    MenuEntry("Edit Home Screen", Icons.Rounded.Apps) {
                        folderMenu = null
                        startEditing()
                    },
                    MenuEntry("Remove Folder", Icons.Rounded.Delete, destructive = true) {
                        folderMenu = null
                        removeFolder(folder)
                    }
                )
                FolderMenuOverlay(
                    anchor = anchor,
                    actions = actions,
                    onDismiss = { folderMenu = null }
                )
            }

            if (renamingFolder != null) {
                val folder = renamingFolder!!
                var titleText by remember(folder.id) { mutableStateOf(folder.title) }
                Dialog(onDismissRequest = { renamingFolder = null }) {
                    Box(
                        Modifier
                            .width(310.dp)
                            .liquidGlass(SmoothShape(24.dp), 24.dp, GlassStyle.Regular)
                            .padding(22.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Rename Folder",
                                style = LargeTitleStyle.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            BasicTextField(
                                value = titleText,
                                onValueChange = { titleText = it },
                                singleLine = true,
                                textStyle = PanelBodyStyle.copy(color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                cursorBrush = SolidColor(Color.White)
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        .clickable { renamingFolder = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Cancel", style = PanelBodyStyle.copy(fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f)))
                                }
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .background(AccentBlue, RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (titleText.isNotBlank()) {
                                                renameFolder(folder, titleText.trim())
                                            }
                                            renamingFolder = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Save", style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White))
                                }
                            }
                        }
                    }
                }
            }

            }

        }

        // The dragged icon follows the finger above every other layer.
        val draggedKey = drag.key
        val draggedIcon = drag.icon
        if (draggedKey != null && draggedIcon != null) {
            val half = drag.sizePx / 2f
            Image(
                draggedIcon,
                contentDescription = null,
                modifier = Modifier
                    .offset { IntOffset((drag.pointer.x - half).roundToInt(), (drag.pointer.y - half).roundToInt()) }
                    .size(with(density) { (half * 2f).toDp() })
                    .graphicsLayer {
                        scaleX = 1.14f
                        scaleY = 1.14f
                        shadowElevation = 18f
                        alpha = 0.94f
                    },
            )
        }
    }
}

@Composable
private fun EditBar(
    layout: DeviceLayout,
    onOpenWidgetGallery: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenCustomise: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onDone: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = glassStatusBarHeight(), start = 16.dp, end = 16.dp, bottom = 6.dp)
    ) {
        // Main Action Line: Edit on left, Done on right (always fixed on the exact same line at 38.dp height)
        Row(
            Modifier
                .fillMaxWidth()
                .height(38.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .height(38.dp)
                    .liquidGlass(Capsule, style = GlassStyle.Clear)
                    .clickable { menuOpen = !menuOpen }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(com.glasslauncher.data.LanguageHelper.string("edit"), style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (menuOpen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Top-right Done button - always stays on the exact same header line as Edit
            GlassButton(com.glasslauncher.data.LanguageHelper.string("done"), null, onDone, bold = true)
        }

        // Submenu Dropdown overlay below Edit button without changing the header line
        if (menuOpen) {
            Column(
                Modifier
                    .padding(top = 44.dp)
                    .width(220.dp)
                    .liquidGlass(SmoothShape(22.dp), 22.dp, GlassStyle.Regular)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            menuOpen = false
                            onOpenWidgetGallery()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Add Widget", style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            menuOpen = false
                            onOpenThemes()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Themes", style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            menuOpen = false
                            onOpenCustomise()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Customise", style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            menuOpen = false
                            onOpenWallpaper()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Wallpaper", style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}

@Composable
fun GlassButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit, bold: Boolean = false) {
    Row(
        Modifier
            .height(38.dp)
            .liquidGlass(Capsule, style = GlassStyle.Clear)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium))
    }
}

/** One Home Screen page after placement: apps in single cells, widgets in blocks, and folders. */
@Immutable
class PageContent(
    val apps: List<Pair<AppEntry, Int>>,
    val widgets: List<WidgetPlacement>,
    val folders: List<com.glasslauncher.data.HomeFolder> = emptyList(),
)

@Immutable
class HomeGrid(
    val pages: List<PageContent>,
    /** Every app's resolved cell, saved as-is after a drag so nothing else moves. */
    val positions: Map<String, Int>,
    val widgets: List<WidgetPlacement>,
    val widgetCells: Set<Int>,
    val folders: List<com.glasslauncher.data.HomeFolder> = emptyList(),
)

/**
 * Places widgets first (unplaced ones in the first free block), then folders, then apps at their saved cells,
 * then the rest in the free cells in order — iOS-style free placement where gaps are allowed.
 */
fun layoutGrid(
    apps: List<AppEntry>,
    saved: Map<String, Int>,
    widgets: List<WidgetPlacement>,
    cols: Int,
    rows: Int,
    folders: List<com.glasslauncher.data.HomeFolder> = emptyList(),
): HomeGrid {
    val perPage = cols * rows
    val taken = HashSet<Int>()
    val placedWidgets = ArrayList<WidgetPlacement>()
    fun fits(p: WidgetPlacement) = p.col >= 0 && p.row >= 0 && p.col + p.w <= cols && p.row + p.h <= rows && p.covers(cols, rows).none(taken::contains)
    for (raw in widgets) {
        val sized = raw.copy(w = raw.w.coerceIn(1, cols), h = raw.h.coerceIn(1, rows))
        var placed = sized.takeIf(::fits)
        var page = maxOf(0, sized.page)
        while (placed == null && page < 20) {
            placed = (0..rows - sized.h).asSequence().flatMap { r -> (0..cols - sized.w).asSequence().map { c -> sized.copy(page = page, col = c, row = r) } }
                .firstOrNull(::fits)
            page++
        }
        placed ?: continue
        taken += placed.covers(cols, rows)
        placedWidgets += placed
    }
    val widgetCells = HashSet(taken)

    val validFolders = ArrayList<com.glasslauncher.data.HomeFolder>()
    for (folder in folders) {
        if (folder.cell >= 0 && folder.cell !in taken) {
            taken += folder.cell
            validFolders += folder
        }
    }

    val folderAppKeys = validFolders.flatMap { it.appKeys }.toSet()
    val looseApps = apps.filter { it.key !in folderAppKeys }

    val positions = LinkedHashMap<String, Int>()
    for (app in looseApps) {
        val cell = saved[app.key] ?: continue
        if (cell >= 0 && cell !in taken) {
            taken += cell
            positions[app.key] = cell
        }
    }
    var next = 0
    for (app in looseApps) {
        if (app.key in positions) continue
        while (next in taken) next++
        taken += next
        positions[app.key] = next
    }
    val lastCell = maxOf(taken.maxOrNull() ?: 0, 0)
    val pageCount = lastCell / perPage + 1
    val byKey = apps.associateBy { it.key }
    val pages = (0 until pageCount).map { page ->
        PageContent(
            apps = positions.entries.filter { it.value / perPage == page }.mapNotNull { (key, cell) -> byKey[key]?.let { it to cell % perPage } },
            widgets = placedWidgets.filter { it.page == page },
            folders = validFolders.filter { it.cell / perPage == page },
        )
    }
    return HomeGrid(pages, positions, placedWidgets, widgetCells, validFolders)
}

/** Apps in their current order, packed per-page so gaps are closed without apps jumping across pages. */
fun compactGrid(grid: HomeGrid, cols: Int, rows: Int, widgets: List<WidgetPlacement> = grid.widgets): Map<String, Int> {
    val perPage = cols * rows
    val taken = layoutGrid(emptyList(), emptyMap(), widgets, cols, rows).widgetCells
    val result = LinkedHashMap<String, Int>()
    val byPage = grid.positions.entries.groupBy { it.value / perPage }
    for ((page, entries) in byPage) {
        var next = page * perPage
        for ((key, _) in entries.sortedBy { it.value }) {
            while (next in taken) next++
            result[key] = next++
        }
    }
    return result
}

@Composable
private fun GridPage(
    content: PageContent,
    layout: DeviceLayout,
    look: IconLook,
    icons: Map<String, ImageBitmap>,
    suggestions: List<AppEntry>,
    editing: Boolean,
    topInset: Dp,
    badgeFor: (AppEntry) -> Int,
    onRect: (Rect) -> Unit,
    onLaunch: (AppEntry, Rect) -> Unit,
    onMenu: (AppEntry, Rect) -> Unit,
    onRemove: (AppEntry) -> Unit,
    onRemoveWidget: (WidgetPlacement) -> Unit,
    onChangeWidget: (WidgetPlacement) -> Unit,
    onRemoveFolder: (com.glasslauncher.data.HomeFolder) -> Unit = {},
    onRenameFolder: (com.glasslauncher.data.HomeFolder) -> Unit = {},
    onFolderMenu: (com.glasslauncher.data.HomeFolder, Rect) -> Unit = { _, _ -> },
    dragModifier: (AppEntry, Anchor, Boolean) -> Modifier,
    onOpenGroup: (AppGroup) -> Unit = {},
    allAppsByKey: Map<String, AppEntry> = emptyMap(),
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(start = layout.sidePadding, end = layout.sidePadding, top = 8.dp, bottom = 4.dp),
    ) {
        val cellW = maxWidth / layout.columns
        // Height of an icon's name under it, measured, so rows and widgets line up with the icons exactly.
        val measurer = rememberTextMeasurer()
        val labelHeight = if (look.showLabel) {
            5.dp + with(LocalDensity.current) { measurer.measure("Ag", IconLabelStyle.copy(fontSize = layout.labelSize)).size.height.toDp() }
        } else {
            0.dp
        }
        val phonePortrait = !layout.isTablet && maxHeight > maxWidth
        val cellH = if (phonePortrait) {
            if (layout.rows == 7) (maxHeight - 4.dp) / 7
            else minOf(maxHeight / layout.rows, maxOf(cellW, layout.iconSize + labelHeight + 8.dp))
        } else {
            maxHeight / layout.rows
        }
        val gridTop = if (phonePortrait) {
            if (layout.rows == 7) 0.dp else ((maxHeight - cellH * layout.rows) / 2).coerceAtLeast(0.dp)
        } else 0.dp
        Box(
            Modifier
                .padding(top = gridTop + topInset)
                .fillMaxWidth()
                .height(cellH * layout.rows)
                .onGloballyPositioned { onRect(it.boundsInWindow()) },
        ) {
        val unit = if (layout.isTablet) {
            minOf(layout.widgetUnit, minOf(cellW, cellH) * layout.widgetCell * 0.92f)
        } else {
            // Square: as tall as two rows of icons, centred over its two columns.
            cellH + layout.iconSize
        }
        val unitHeight = unit
        val iconTop = if (layout.isTablet) null else ((cellH - layout.iconSize - labelHeight) / 2).coerceAtLeast(0.dp)
        val unlockProg = LocalUnlockProgress.current
        val perPage = layout.columns * layout.rows
        content.widgets.forEach { placement ->
            key(placement.kind) {
                val row = placement.row
                val rowDelay = (row * 0.04f).coerceIn(0f, 0.4f)
                Box(
                    Modifier.graphicsLayer {
                        val p = unlockProg.value
                        if (p < 0.999f) {
                            val rowProg = ((p - rowDelay) / (1f - rowDelay)).coerceIn(0f, 1f)
                            val dropY = -(1f - rowProg) * 50f
                            val s = 1.06f - rowProg * 0.06f
                            translationY = dropY
                            scaleX = s
                            scaleY = s
                            alpha = (rowProg * 1.4f).coerceIn(0f, 1f)
                        }
                    }
                ) {
                    GridWidget(placement, layout, cellW, cellH, unit, icons, suggestions, editing, onRemoveWidget, onChangeWidget, onLaunch, iconTop, unitHeight)
                }
            }
        }
        content.folders.forEach { folder ->
            key(folder.id) {
                val folderApps = remember(folder.appKeys, allAppsByKey) {
                    folder.appKeys.mapNotNull { allAppsByKey[it] }
                }
                val anchor = remember { Anchor() }
                val row = (folder.cell % perPage) / layout.columns
                val rowDelay = (row * 0.04f).coerceIn(0f, 0.4f)
                Box(
                    Modifier
                        .offset(cellW * (folder.cell % layout.columns), cellH * ((folder.cell % perPage) / layout.columns))
                        .size(cellW, cellH)
                        .graphicsLayer {
                            val p = unlockProg.value
                            if (p < 0.999f) {
                                val rowProg = ((p - rowDelay) / (1f - rowDelay)).coerceIn(0f, 1f)
                                val dropY = -(1f - rowProg) * 50f
                                val s = 1.08f - rowProg * 0.08f
                                translationY = dropY
                                scaleX = s
                                scaleY = s
                                alpha = (rowProg * 1.5f).coerceIn(0f, 1f)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    HomeFolderIcon(
                        folder = folder,
                        apps = folderApps,
                        icons = icons,
                        look = look,
                        onClick = {
                            if (!editing) {
                                onOpenGroup(AppGroup(folder.title, folderApps))
                            } else {
                                onRenameFolder(folder)
                            }
                        },
                        onLongClick = {
                            if (!editing) {
                                onFolderMenu(folder, anchor.bounds())
                            }
                        },
                        anchor = anchor,
                        editing = editing,
                        onRemove = { onRemoveFolder(folder) },
                        onRename = { onRenameFolder(folder) },
                    )
                }
            }
        }
        content.apps.forEach { (app, cell) ->
            key(app.key) {
                val anchor = remember { Anchor() }
                val row = (cell % perPage) / layout.columns
                val rowDelay = (row * 0.04f).coerceIn(0f, 0.4f)
                Box(
                    Modifier
                        .offset(cellW * (cell % layout.columns), cellH * (cell / layout.columns))
                        .size(cellW, cellH)
                        .graphicsLayer {
                            val p = unlockProg.value
                            if (p < 0.999f) {
                                val rowProg = ((p - rowDelay) / (1f - rowDelay)).coerceIn(0f, 1f)
                                val dropY = -(1f - rowProg) * 50f
                                val s = 1.08f - rowProg * 0.08f
                                translationY = dropY
                                scaleX = s
                                scaleY = s
                                alpha = (rowProg * 1.5f).coerceIn(0f, 1f)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        app = app,
                        icon = icons[app.key],
                        look = look,
                        onLaunch = onLaunch,
                        onMenu = onMenu,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        badge = badgeFor(app),
                        editing = editing,
                        onRemove = { onRemove(app) },
                        iconModifier = dragModifier(app, anchor, false),
                        anchor = anchor,
                    )
                }
            }
        }
    }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeFolderIcon(
    folder: com.glasslauncher.data.HomeFolder,
    apps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    look: IconLook,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    anchor: Anchor = remember { Anchor() },
    editing: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val wobble = if (editing) {
        val transition = rememberInfiniteTransition(label = "jiggleFolder")
        transition.animateFloat(
            initialValue = -1.8f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 120 + (folder.id.hashCode() and 0x3F)), RepeatMode.Reverse),
            label = "jiggleAngle",
        )
    } else null

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(look.size)
                .onPlaced { anchor.coordinates = it }
                .graphicsLayer {
                    rotationZ = wobble?.value ?: 0f
                }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .liquidGlass(FolderShape, style = GlassStyle.Regular)
                    .combinedClickable(
                        enabled = true,
                        onClick = onClick,
                        onLongClick = {
                            if (!editing && onLongClick != null) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongClick()
                            }
                        }
                    )
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in 0..1) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (col in 0..1) {
                                val idx = row * 2 + col
                                val app = apps.getOrNull(idx)
                                val bmp = app?.let { icons[it.key] }
                                val miniSize = (look.size - 16.dp) / 2
                                if (bmp != null) {
                                    Image(
                                        bmp,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(miniSize)
                                            .clip(SmoothShape(fraction = 0.22f))
                                    )
                                } else {
                                    Spacer(Modifier.size(miniSize))
                                }
                            }
                        }
                    }
                }
            }
            if (editing && onRemove != null) {
                EditBadge(
                    icon = Icons.Rounded.Remove,
                    label = "Remove folder",
                    modifier = Modifier.align(Alignment.TopStart).offset((-7).dp, (-7).dp),
                    onClick = onRemove,
                )
            }
        }
        if (look.showLabel) {
            Text(
                folder.title,
                style = IconLabelStyle.copy(fontSize = look.labelSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(look.size + 16.dp)
                    .clickable(enabled = editing) { onRename?.invoke() },
            )
        }
    }
}

@Composable
private fun GridWidget(
    placement: WidgetPlacement,
    layout: DeviceLayout,
    cellW: Dp,
    cellH: Dp,
    unit: Dp,
    icons: Map<String, ImageBitmap>,
    suggestions: List<AppEntry>,
    editing: Boolean,
    onRemove: (WidgetPlacement) -> Unit,
    onChange: (WidgetPlacement) -> Unit,
    onLaunch: (AppEntry, Rect) -> Unit,
    /** Where icon tops sit in a cell; widgets align to it on iPhone. Null centres them (iPad). */
    iconTop: Dp? = null,
    unitHeight: Dp = unit,
) {
    val density = LocalDensity.current
    var move by remember { mutableStateOf(Offset.Zero) }
    var stretch by remember { mutableStateOf(Offset.Zero) }
    val current by rememberUpdatedState(placement)
    val cellWPx = with(density) { cellW.toPx() }
    val cellHPx = with(density) { cellH.toPx() }
    val per = layout.widgetCell
    val sizes = widgetSizes(layout)
    // Edges line up with the icon columns and rows: each extra unit adds one grid step.
    val pitchX = cellW * per
    val pitchY = cellH * per
    val unitsW = (placement.w / per).coerceAtLeast(1)
    val unitsH = (placement.h / per).coerceAtLeast(1)
    val isSquare = placement.w == placement.h
    val slotW = if (layout.isTablet) (cellW * placement.w - 14.dp) else (pitchX * (unitsW - 1) + unit)
    val slotH = if (layout.isTablet) (cellH * placement.h - 14.dp) else (pitchY * (unitsH - 1) + unitHeight)
    val width = if (isSquare) minOf(slotW, slotH) else slotW
    val height = if (isSquare) minOf(slotW, slotH) else slotH
    val effectiveUnit = if (layout.isTablet) minOf(slotW, slotH) else unit
    val stretchW = with(density) { if (isSquare) maxOf(stretch.x, stretch.y).toDp() else stretch.x.toDp() }
    val stretchH = with(density) { if (isSquare) maxOf(stretch.x, stretch.y).toDp() else stretch.y.toDp() }
    val wobble = if (editing) {
        rememberInfiniteTransition(label = "widgetJiggle").animateFloat(
            -0.8f, 0.8f, infiniteRepeatable(tween(150 + placement.kind.ordinal * 9), RepeatMode.Reverse), label = "widgetAngle",
        )
    } else {
        null
    }
    Box(
        Modifier
            .offset(cellW * placement.col, cellH * placement.row)
            .size(cellW * placement.w, cellH * placement.h)
            .zIndex(if (move != Offset.Zero || stretch != Offset.Zero) 2f else 1f)
            .padding(top = iconTop ?: 0.dp),
        contentAlignment = if (iconTop != null) Alignment.TopCenter else Alignment.Center,
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    translationX = move.x
                    translationY = move.y
                    rotationZ = wobble?.value ?: 0f
                }
                .size((width + stretchW).coerceAtLeast(effectiveUnit), (height + stretchH).coerceAtLeast(effectiveUnit)),
        ) {
            val kinds = placement.allKinds
            val context = LocalContext.current

            if (kinds.size > 1) {
                val verticalPagerState = rememberPagerState(pageCount = { kinds.size })
                LaunchedEffect(verticalPagerState.currentPage) {
                    AppleTaptic.selection(context)
                }
                Box(Modifier.fillMaxSize()) {
                    VerticalPager(
                        state = verticalPagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !editing,
                    ) { pageIdx ->
                        val pageOffset = ((verticalPagerState.currentPage - pageIdx) + verticalPagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                        val kind = kinds[pageIdx]
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    cameraDistance = 16f
                                    rotationX = pageOffset * 50f
                                    val scale = 1f - kotlin.math.abs(pageOffset) * 0.12f
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - kotlin.math.abs(pageOffset) * 0.35f
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, if (pageOffset > 0) 1.0f else 0.0f)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            ScaledWidget(kind, DpSize(width, height), effectiveUnit, suggestions, icons, onLaunch)
                        }
                    }
                    // Apple-style vertical pagination dots on the right edge
                    Column(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        repeat(kinds.size) { idx ->
                            Box(
                                Modifier
                                    .size(if (idx == verticalPagerState.currentPage) 5.dp else 4.dp)
                                    .background(
                                        Color.White.copy(alpha = if (idx == verticalPagerState.currentPage) 0.95f else 0.40f),
                                        CircleShape
                                    )
                                )
                        }
                    }
                }
            } else {
                ScaledWidget(placement.kind, DpSize(width, height), effectiveUnit, suggestions, icons, onLaunch, Modifier.align(Alignment.Center))
            }
            if (editing) {
                // Covers the widget so a drag moves it instead of using it.
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    val p = current
                                    onChange(p.copy(col = p.col + (move.x / cellWPx).roundToInt(), row = p.row + (move.y / cellHPx).roundToInt()))
                                    move = Offset.Zero
                                },
                                onDragCancel = { move = Offset.Zero },
                            ) { change, amount ->
                                change.consume()
                                move += amount
                            }
                        }
                        .pointerInput(Unit) { detectTapGestures { } },
                )
                EditBadge(Icons.Rounded.Remove, "Remove widget", Modifier.align(Alignment.TopStart).offset((-8).dp, (-8).dp)) { onRemove(current) }
                // Drag the corner to resize; it snaps to the nearest iOS size. A tap steps to the next size.
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(8.dp, 8.dp)
                        .size(30.dp)
                        .liquidGlass(Capsule, style = GlassStyle.Dark)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    val p = current
                                    val targetW = (p.w * cellWPx + stretch.x) / (cellWPx * per)
                                    val targetH = (p.h * cellHPx + stretch.y) / (cellHPx * per)
                                    val (w, h) = sizes.minBy { (w, h) -> (w - targetW) * (w - targetW) + (h - targetH) * (h - targetH) }
                                    onChange(p.copy(w = w * per, h = h * per))
                                    stretch = Offset.Zero
                                },
                                onDragCancel = { stretch = Offset.Zero },
                            ) { change, amount ->
                                change.consume()
                                stretch += amount
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures {
                                val p = current
                                val index = sizes.indexOf(p.w / per to p.h / per)
                                val (w, h) = sizes[(index + 1) % sizes.size]
                                onChange(p.copy(w = w * per, h = h * per))
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.OpenInFull, contentDescription = "Resize widget", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EditBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(28.dp)
            .liquidGlass(Capsule, style = GlassStyle.Dark)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/**
 * Today View: search plus widgets packed densely from the left, like iOS. Touch and hold to edit: widgets jiggle,
 * − removes one, the corner handle steps through small, medium and large, dragging one onto another swaps them,
 * + adds widgets and ✓ finishes.
 */
@Composable
private fun TodayPage(
    layout: DeviceLayout,
    saved: List<TodayWidget>,
    suggestions: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    onLaunch: (AppEntry, Rect) -> Unit,
    onOpenSearch: () -> Unit,
    onSave: (List<TodayWidget>) -> Unit,
    onAddWidget: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    val widgets = remember(saved) { saved.filter { it.kind != WidgetKind.Music || Permissions.notificationsSupported } }
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(
        Modifier.fillMaxSize().todayLongPress {
            if (!editing) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            editing = true
        },
    ) {
        // Full width at every size: two units per row on iPhone, more on wider screens.
        val landscape = maxWidth > maxHeight
        val perRow = when {
            layout.isTablet -> if (landscape) 6 else 4
            landscape -> 4
            else -> 2
        }
        val sidePad = if (layout.isTablet) 32.dp else 20.dp
        val gap = if (layout.isTablet) 20.dp else 16.dp
        val side = (maxWidth - sidePad * 2 - gap * (perRow - 1)) / perRow
        val width = side * perRow + gap * (perRow - 1)
        val placed = remember(widgets, perRow) { packToday(widgets, perRow) }
        val rows = (placed.maxOfOrNull { it.row + it.widget.h } ?: 0)
        val density = LocalDensity.current
        val bounds = remember { HashMap<Int, Rect>() }
        var baseBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
        var dragging by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }
        var hoverStackTarget by remember { mutableStateOf<Int?>(null) }
        var hoverReorderTarget by remember { mutableStateOf<Int?>(null) }

        fun save(list: List<TodayWidget>) = onSave(list)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = dragging == null).padding(top = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(editing, label = "todayBar") { edit ->
                if (edit) {
                    Row(Modifier.width(width).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassButton("Add", Icons.Rounded.Add, onAddWidget)
                        Spacer(Modifier.weight(1f))
                        GlassButton("Done", Icons.Rounded.Check, { editing = false; dragging = null; hoverStackTarget = null; hoverReorderTarget = null }, bold = true)
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.width(width).height(side * rows + gap * (rows - 1).coerceAtLeast(0))) {
                placed.forEachIndexed { index, item ->
                    key(item.widget.kind) {
                        val w = side * item.widget.w + gap * (item.widget.w - 1)
                        val h = side * item.widget.h + gap * (item.widget.h - 1)

                        // Smooth fluid Apple springs that animate widgets into their new slots on drop, matching other pages
                        val animCol by animateFloatAsState(
                            targetValue = item.col.toFloat(),
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 260f),
                            label = "todayCol_${item.widget.kind}"
                        )
                        val animRow by animateFloatAsState(
                            targetValue = item.row.toFloat(),
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 260f),
                            label = "todayRow_${item.widget.kind}"
                        )

                        val d = dragging
                        val isBeingDragged = d == index

                        val wobble = if (editing && !isBeingDragged) {
                            rememberInfiniteTransition(label = "todayJiggle").animateFloat(
                                -0.8f, 0.8f, infiniteRepeatable(tween(150 + item.widget.kind.ordinal * 9), RepeatMode.Reverse), label = "todayAngle",
                            )
                        } else {
                            null
                        }

                        // Glassy surrounding effect ONLY appears at 90% exact overlap over another widget of the exact same size
                        val canStack = d != null && d != index && d in widgets.indices &&
                            widgets[d].w == item.widget.w && widgets[d].h == item.widget.h
                        val isStackTarget = canStack && hoverStackTarget == index

                        Box(
                            Modifier
                                .offset((side + gap) * animCol, (side + gap) * animRow)
                                .size(w, h)
                                .zIndex(if (isBeingDragged) 10f else if (isStackTarget) 5f else 1f)
                                .onGloballyPositioned { bounds[index] = it.boundsInWindow() }
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                    } else if (isStackTarget) {
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                    }
                                    rotationZ = wobble?.value ?: 0f
                                },
                        ) {
                            // Inner Box with clipped corners and liquid glass surrounding effect when stacking at 90% overlap
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(SmoothShape(22.dp))
                                    .then(
                                        if (isStackTarget) {
                                            Modifier
                                                .shadow(16.dp, SmoothShape(22.dp), spotColor = Color(0xFF007AFF), ambientColor = Color(0x66FFFFFF))
                                                .border(
                                                    width = 3.dp,
                                                    brush = Brush.linearGradient(
                                                        listOf(Color.White, Color(0xFF64D2FF), Color.White.copy(alpha = 0.8f))
                                                    ),
                                                    shape = SmoothShape(22.dp)
                                                )
                                                .background(Color.White.copy(alpha = 0.16f), SmoothShape(22.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                val kinds = item.widget.allKinds
                                if (kinds.size > 1) {
                                    val todayPagerState = rememberPagerState(pageCount = { kinds.size })
                                    Box(Modifier.fillMaxSize()) {
                                        VerticalPager(
                                            state = todayPagerState,
                                            modifier = Modifier.fillMaxSize(),
                                            userScrollEnabled = !editing,
                                        ) { pageIdx ->
                                            val pageOffset = ((todayPagerState.currentPage - pageIdx) + todayPagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        cameraDistance = 16f
                                                        rotationX = pageOffset * 50f
                                                        val scale = 1f - kotlin.math.abs(pageOffset) * 0.12f
                                                        scaleX = scale
                                                        scaleY = scale
                                                        alpha = 1f - kotlin.math.abs(pageOffset) * 0.35f
                                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, if (pageOffset > 0) 1.0f else 0.0f)
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                ScaledWidget(kinds[pageIdx], DpSize(w, h), side, suggestions, icons, onLaunch)
                                            }
                                        }
                                        Column(
                                            Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            repeat(kinds.size) { idx ->
                                                Box(
                                                    Modifier
                                                        .size(if (idx == todayPagerState.currentPage) 5.dp else 4.dp)
                                                        .background(
                                                            Color.White.copy(alpha = if (idx == todayPagerState.currentPage) 0.95f else 0.40f),
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    ScaledWidget(item.widget.kind, DpSize(w, h), side, suggestions, icons, onLaunch)
                                }
                            }

                            if (editing) {
                                // Transparent touch detector capturing drag gesture
                                Box(
                                    Modifier.fillMaxSize().pointerInput(index, widgets) {
                                        detectDragGestures(
                                            onDragStart = {
                                                dragging = index
                                                dragOffset = Offset.Zero
                                                baseBounds = bounds.toMap()
                                                hoverStackTarget = null
                                                hoverReorderTarget = null
                                            },
                                            onDragEnd = {
                                                if (hoverStackTarget != null) {
                                                    val targetIdx = hoverStackTarget!!
                                                    val fromWidget = widgets[index]
                                                    val targetWidget = widgets[targetIdx]
                                                    val combined = (targetWidget.allKinds + fromWidget.allKinds).distinct()
                                                    val stacked = targetWidget.copy(kind = combined.first(), stack = combined.drop(1))
                                                    val list = widgets.toMutableList()
                                                    list.removeAt(index)
                                                    val newTargetIndex = if (index < targetIdx) targetIdx - 1 else targetIdx
                                                    list[newTargetIndex] = stacked
                                                    save(list)
                                                } else if (hoverReorderTarget != null && hoverReorderTarget != index) {
                                                    val list = widgets.toMutableList()
                                                    val item = list.removeAt(index)
                                                    val targetIdx = hoverReorderTarget!!.coerceIn(0, list.size)
                                                    list.add(targetIdx, item)
                                                    save(list)
                                                }
                                                dragging = null
                                                dragOffset = Offset.Zero
                                                hoverStackTarget = null
                                                hoverReorderTarget = null
                                            },
                                            onDragCancel = {
                                                dragging = null
                                                dragOffset = Offset.Zero
                                                hoverStackTarget = null
                                                hoverReorderTarget = null
                                            },
                                        ) { change, amount ->
                                            change.consume()
                                            dragOffset += amount

                                            val from = baseBounds[index]
                                            if (from != null) {
                                                val dragRect = Rect(
                                                    from.left + dragOffset.x,
                                                    from.top + dragOffset.y,
                                                    from.right + dragOffset.x,
                                                    from.bottom + dragOffset.y
                                                )
                                                var foundStack: Int? = null
                                                var foundReorder: Int? = null

                                                for ((targetIdx, targetRect) in baseBounds) {
                                                    if (targetIdx == index) continue
                                                    val interW = (minOf(dragRect.right, targetRect.right) - maxOf(dragRect.left, targetRect.left)).coerceAtLeast(0f)
                                                    val interH = (minOf(dragRect.bottom, targetRect.bottom) - maxOf(dragRect.top, targetRect.top)).coerceAtLeast(0f)
                                                    val interArea = interW * interH
                                                    val targetArea = targetRect.width * targetRect.height
                                                    val overlapFraction = if (targetArea > 0f) interArea / targetArea else 0f

                                                    val isSameDimensions = targetIdx in widgets.indices &&
                                                        widgets[targetIdx].w == widgets[index].w &&
                                                        widgets[targetIdx].h == widgets[index].h

                                                    if (isSameDimensions && overlapFraction >= 0.90f) {
                                                        foundStack = targetIdx
                                                        foundReorder = null
                                                        break
                                                    } else if (overlapFraction >= 0.35f || targetRect.contains(dragRect.center)) {
                                                        foundReorder = targetIdx
                                                    }
                                                }
                                                hoverStackTarget = foundStack
                                                hoverReorderTarget = foundReorder
                                            }
                                        }
                                    }.pointerInput(Unit) { detectTapGestures { } },
                                )

                                // Small rounded glassy minus badge at top-left corner
                                Box(
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .offset((-6).dp, (-6).dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2C2C2E).copy(alpha = 0.85f))
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                        .clickable(onClickLabel = "Remove widget") {
                                            save(widgets.filterIndexed { i, _ -> i != index })
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        Modifier
                                            .width(11.dp)
                                            .height(2.dp)
                                            .background(Color.White, RoundedCornerShape(1.dp))
                                    )
                                }

                                // Small rounded glassy resize handle at bottom-right corner
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(6.dp, 6.dp)
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2C2C2E).copy(alpha = 0.85f))
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .clickable(onClickLabel = "Change size") {
                                            val sizes = if (perRow >= 4) listOf(1 to 1, 2 to 1, 2 to 2, 4 to 2, 4 to 4) else if (perRow >= 2) listOf(1 to 1, 2 to 1, 2 to 2) else listOf(1 to 1)
                                            val currentPair = item.widget.w to item.widget.h
                                            val currentIndex = sizes.indexOf(currentPair)
                                            val next = if (currentIndex != -1) sizes[(currentIndex + 1) % sizes.size] else sizes.first()
                                            save(widgets.mapIndexed { i, w -> if (i == index) w.copy(w = next.first, h = next.second) else w })
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.OpenInFull,
                                        contentDescription = "Change size",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private class PlacedToday(val widget: TodayWidget, val col: Int, val row: Int)

/** Dense first-fit packing: each widget takes the first spot it fits, so small ones fill the gaps beside wide ones. */
private fun packToday(widgets: List<TodayWidget>, perRow: Int): List<PlacedToday> {
    val taken = HashSet<Pair<Int, Int>>()
    return widgets.map { raw ->
        val widget = raw.copy(w = raw.w.coerceAtMost(perRow))
        var row = 0
        var spot: Pair<Int, Int>? = null
        while (spot == null) {
            for (col in 0..perRow - widget.w) {
                val cells = (0 until widget.h).flatMap { dy -> (0 until widget.w).map { dx -> (col + dx) to (row + dy) } }
                if (cells.none(taken::contains)) {
                    taken += cells
                    spot = col to row
                    break
                }
            }
            row++
        }
        PlacedToday(widget, spot.first, spot.second)
    }
}

/** Touch and hold anywhere on the page, even on a widget, without stealing its taps or the page swipe. */
private fun Modifier.todayLongPress(onLongPress: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
            }
            true
        }
        if (released == null) {
            onLongPress()
            // The rest of this touch belongs to the long press, so the widget underneath doesn't also open.
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

@Composable
private fun SearchField(onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .height(44.dp)
            .liquidGlass(Capsule, style = GlassStyle.Regular)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(com.glasslauncher.data.LanguageHelper.string("search"), style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.8f)))
    }
}

/** iOS 26: a "Search" capsule above the Dock that turns into page dots while you swipe. */
@Composable
private fun SearchPill(pagerState: PagerState, homePages: Int, onClick: () -> Unit, modifier: Modifier, dotsOnly: Boolean = false) {
    // iOS 26: the Search pill, which turns into page dots while you swipe and back to Search once the page settles.
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(4_000)
            expanded = false
        }
    }
    if (pagerState.isScrollInProgress && expanded) expanded = false
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var isScrubbing by remember { mutableStateOf(false) }

    val pill = !dotsOnly || expanded
    val scrolling = if (dotsOnly) !expanded else (pagerState.isScrollInProgress || isScrubbing)
    // The glass capsule fades and swells in while the width eases to fit, so dots morph into Search smoothly.
    val glass by animateFloatAsState(if (pill) 1f else 0f, spring(dampingRatio = 0.9f, stiffness = 300f), label = "pillGlass")
    Box(
        modifier
            .height(26.dp)
            .animateContentSize(spring(dampingRatio = 0.86f, stiffness = 300f))
            .pointerInput(pagerState, homePages) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var dragStarted = false
                    var accumulated = 0f
                    val touchSlop = viewConfiguration.touchSlop
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!dragStarted) {
                                if (dotsOnly && !expanded) expanded = true else onClick()
                            }
                            isScrubbing = false
                            break
                        }
                        val dx = change.position.x - down.position.x
                        if (!dragStarted && kotlin.math.abs(dx) > touchSlop) {
                            dragStarted = true
                            isScrubbing = true
                            accumulated = 0f
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        if (dragStarted) {
                            change.consume()
                            accumulated += (change.position.x - change.previousPosition.x)
                            val stepPx = 28.dp.toPx()
                            val minHomePage = 1
                            val maxHomePage = homePages.coerceAtLeast(1)
                            if (accumulated > stepPx) {
                                val next = (pagerState.currentPage + 1).coerceAtMost(maxHomePage)
                                if (next != pagerState.currentPage) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch { pagerState.animateScrollToPage(next) }
                                }
                                accumulated = 0f
                            } else if (accumulated < -stepPx) {
                                val prev = (pagerState.currentPage - 1).coerceAtLeast(minHomePage)
                                if (prev != pagerState.currentPage) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch { pagerState.animateScrollToPage(prev) }
                                }
                                accumulated = 0f
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (glass > 0.01f) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = glass
                        scaleX = 0.85f + 0.15f * glass
                        scaleY = 0.85f + 0.15f * glass
                    }
                    .liquidGlass(Capsule, style = GlassStyle.Clear),
            )
        }
        // Both faces fill the pill's height and centre in it, so the dots sit on the same line as "Search".
        Crossfade(scrolling, animationSpec = tween(260), label = "searchPill", modifier = Modifier.fillMaxHeight().padding(horizontal = 11.dp)) { showDots ->
            if (showDots) {
                Row(Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Just the Home Screen pages, as on iOS.
                    for (page in 1..homePages) {
                        val color = Color.White.copy(alpha = if (pagerState.currentPage == page) 1f else 0.4f)
                        Box(Modifier.size(6.dp).background(color, CircleShape))
                    }
                }
            } else {
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(com.glasslauncher.data.LanguageHelper.string("search"), style = IconLabelStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun Dock(
    apps: List<AppEntry>,
    recents: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    look: IconLook,
    layout: DeviceLayout,
    editing: Boolean,
    anchor: Anchor,
    badgeFor: (AppEntry) -> Int,
    onLaunch: (AppEntry, Rect) -> Unit,
    onMenu: (AppEntry, Rect) -> Unit,
    onRemove: (AppEntry) -> Unit,
    dragModifier: (AppEntry, Anchor, Boolean) -> Modifier,
    modifier: Modifier,
) {
    val configuration = LocalConfiguration.current
    val floating = layout.isTablet || configuration.screenWidthDp > configuration.screenHeightDp
    val rowModifier = modifier
        .onPlaced { anchor.coordinates = it }
        // iPhone in portrait spans the width; iPad, and a phone turned on its side, float a compact Dock.
        .then(if (floating) Modifier else Modifier.fillMaxWidth())
        .liquidGlass(DockShape, style = GlassStyle.Clear)
        // Apple's Dock proportions: edges and gaps about 28% of an icon on iPad. The iPhone Dock spans the width with
        // the icons spread edge to edge, about a third of an icon from each side, and a little taller than a row.
        .padding(
            horizontal = look.size * (if (floating) 0.28f else 0.3f),
            vertical = look.size * (if (layout.isTablet) 0.28f else 0.3f),
        )
    Row(
        rowModifier,
        horizontalArrangement = if (floating) Arrangement.spacedBy(look.size * 0.29f) else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (apps.isEmpty()) {
            Text(
                if (editing) "Drag apps here" else "Long-press an app to add it to the Dock",
                style = PanelCaptionStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(look.size).padding(horizontal = 12.dp).widthIn(max = 280.dp),
            )
        }
        apps.forEach { app ->
            key(app.key) {
                val iconAnchor = remember { Anchor() }
                AppIcon(
                    app = app,
                    icon = icons[app.key],
                    look = look,
                    onLaunch = onLaunch,
                    onMenu = onMenu,
                    badge = badgeFor(app),
                    editing = editing,
                    onRemove = { onRemove(app) },
                    iconModifier = dragModifier(app, iconAnchor, true),
                    anchor = iconAnchor,
                )
            }
        }
        // iPadOS: recently used apps after a divider.
        if (recents.isNotEmpty() && !editing) {
            Box(Modifier.width(1.dp).height(look.size * 0.7f).background(Color.White.copy(alpha = 0.3f)))
            recents.forEach { app ->
                key("recent-" + app.key) { AppIcon(app, icons[app.key], look, onLaunch, onMenu, badge = badgeFor(app)) }
            }
        }
    }
}

@Composable
private fun AppLibraryPage(
    groups: List<AppGroup>,
    hidden: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    look: IconLook,
    layout: DeviceLayout,
    onLaunch: (AppEntry, Rect) -> Unit,
    onMenu: (AppEntry, Rect) -> Unit,
    onOpenGroup: (AppGroup) -> Unit,
    onOpenSearch: () -> Unit,
) {
    // Columns from the width: two on iPhone, more on wider screens; folders and their icons scale to fill the row.
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = if (layout.isTablet) layout.sidePadding else 20.dp)) {
        val columnGap = if (layout.isTablet) 28.dp else 22.dp
        val columns = when {
            !layout.isTablet -> if (maxWidth > maxHeight) 4 else 2
            maxWidth > maxHeight -> 4
            else -> 3
        }.coerceAtMost(((maxWidth + columnGap) / (120.dp + columnGap)).toInt().coerceAtLeast(2))
        val folderSize = minOf((maxWidth - columnGap * (columns - 1)) / columns, if (layout.isTablet) 230.dp else 200.dp)
        val gap = folderSize * 0.075f
        val miniLook = look.copy(size = (folderSize - gap * 3) / 2, showLabel = false, blurUnder = false)
        // Single-app categories would be near-empty folders, so they're gathered into one "Other" folder at the end.
        val folders = remember(groups) {
            val (full, single) = groups.partition { it.apps.size > 1 && it.title != "Other" }
            val other = single.flatMap { it.apps }.distinctBy { it.key }
            if (other.isEmpty()) full else full + AppGroup("Other", other)
        }
        Column(Modifier.fillMaxSize()) {
            SearchField(
                onOpenSearch,
                Modifier.padding(top = 12.dp, bottom = 20.dp).align(Alignment.CenterHorizontally).width(folderSize * columns + columnGap * (columns - 1)),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(columnGap, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(folders, key = { it.title }) { group ->
                    LibraryFolder(group, icons, miniLook, folderSize, gap, onLaunch, onMenu, onOpenGroup)
                }
                // Hidden apps sit in a locked folder at the end; it opens after a fingerprint, face or PIN.
                if (hidden.isNotEmpty()) {
                    item(key = "hidden") {
                        val context = LocalContext.current
                        HiddenFolder(folderSize, gap, miniLook.size) {
                            unlockHidden(context) { onOpenGroup(AppGroup("Hidden", hidden)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryFolder(
    group: AppGroup,
    icons: Map<String, ImageBitmap>,
    miniLook: IconLook,
    folderSize: Dp,
    gap: Dp,
    onLaunch: (AppEntry, Rect) -> Unit,
    onMenu: (AppEntry, Rect) -> Unit,
    onOpenGroup: (AppGroup) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.size(folderSize).liquidGlass(FolderShape, style = GlassStyle.Regular).padding(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (column in 0..1) {
                        val index = row * 2 + column
                        // Like iOS: three apps open directly, the fourth corner previews the rest and opens the folder.
                        if (index == 3 && group.apps.size > 4) {
                            MiniCluster(group.apps.subList(3, min(7, group.apps.size)), icons, miniLook.size) { onOpenGroup(group) }
                        } else {
                            val app = group.apps.getOrNull(index)
                            if (app != null) AppIcon(app, icons[app.key], miniLook, onLaunch, onMenu) else Spacer(Modifier.size(miniLook.size))
                        }
                    }
                }
            }
        }
        Text(
            group.title,
            style = IconLabelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp).width(folderSize),
        )
    }
}

@Composable
private fun MiniCluster(apps: List<AppEntry>, icons: Map<String, ImageBitmap>, size: Dp, onClick: () -> Unit) {
    val spacing = 6.dp
    val tiny = (size - spacing) / 2
    Column(
        Modifier.size(size).clickable(interactionSource = null, indication = null, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                for (column in 0..1) {
                    val bitmap = apps.getOrNull(row * 2 + column)?.let { icons[it.key] }
                    if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(tiny)) else Spacer(Modifier.size(tiny))
                }
            }
        }
    }
}

/** iOS 26 Dock: continuous corners at about 40% of its height. */
private val DockShape = SmoothShape(fraction = 0.4f)

/** App Library folders: soft continuous corners, like iOS. */
private val FolderShape = SmoothShape(fraction = 0.21f)

/** The Hidden folder: empty glass tiles, so nothing hints at what's inside. */
@Composable
private fun HiddenFolder(folderSize: Dp, gap: Dp, tile: Dp, onOpen: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.size(folderSize).liquidGlass(FolderShape, style = GlassStyle.Regular).clickable(onClick = onOpen).padding(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (column in 0..1) {
                        if (row == 1 && column == 1) {
                            // The small corner, like a folder preview.
                            val small = (tile - gap / 2) / 2
                            Column(Modifier.size(tile), verticalArrangement = Arrangement.spacedBy(gap / 2)) {
                                repeat(2) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(gap / 2)) {
                                        repeat(2) { Box(Modifier.size(small).clip(RoundedCornerShape(small * 0.25f)).background(Color.White.copy(alpha = 0.14f))) }
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.size(tile).clip(RoundedCornerShape(tile * 0.24f)).background(Color.White.copy(alpha = 0.14f)))
                        }
                    }
                }
            }
        }
        Text("Hidden", style = IconLabelStyle, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Asks for the fingerprint, face or screen lock before showing hidden apps (straight through with no screen lock). */
private fun unlockHidden(context: android.content.Context, onUnlocked: () -> Unit) {
    val keyguard = context.getSystemService(android.app.KeyguardManager::class.java)
    if (keyguard?.isDeviceSecure != true) {
        onUnlocked()
        return
    }
    val authenticators = android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
    runCatching {
        android.hardware.biometrics.BiometricPrompt.Builder(context)
            .setTitle("Show Hidden Apps")
            .setAllowedAuthenticators(authenticators)
            .build()
            .authenticate(
                android.os.CancellationSignal(),
                context.mainExecutor,
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) = onUnlocked()
                },
            )
    }
}

/**
 * The Home Screen's insets: system bars and the camera cutout, but not the keyboard. The Home Screen stays its full
 * size behind Search and the editors, so the keyboard can never squeeze the grid into an impossible layout.
 */
private val HomeInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars.union(WindowInsets.displayCutout)

/** A round glass button with just an icon, like iOS's edit-mode + and settings. */
@Composable
private fun GlassIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).liquidGlass(Capsule, style = GlassStyle.Clear).clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
