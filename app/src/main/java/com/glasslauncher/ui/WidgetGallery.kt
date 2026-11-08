package com.glasslauncher.ui
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.CompositionLocalProvider
import com.glasslauncher.data.LauncherSettings
import com.glasslauncher.data.IconStyle
import com.glasslauncher.data.GlassLook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.EditNote

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.data.AppEntry
import com.glasslauncher.data.WidgetKind

data class GalleryItemDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val appName: String,
    val description: String,
    val defaultKind: WidgetKind,
    val sizes: List<Pair<Int, Int>> = listOf(1 to 1, 2 to 1),
    val stack: List<WidgetKind> = emptyList(),
)

/**
 * iOS Widget Gallery matching the reference screenshots:
 * - Rounded top grab handle
 * - Search bar "Search Widgets"
 * - Featured widgets (Music "Rishav's Station", Fitness "Activity", Snapchat "My Snapcode", WhatsApp "Meta AI", Instagram "Instants")
 * - Detail sub-sheet with swipeable widget sizes, pagination dots, and vibrant blue "+ Add Widget" button.
 */
@Composable
fun WidgetGallery(
    chosen: List<WidgetKind>,
    suggestions: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    onToggle: (WidgetKind) -> Unit,
    onAddWithSize: ((WidgetKind, Int, Int, List<WidgetKind>) -> Unit)? = null,
    onDismiss: () -> Unit,
    subtitle: String = "Tap to add to or remove from this Home Screen page.",
    includeAll: Boolean = false,
    settings: LauncherSettings? = null,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<GalleryItemDef?>(null) }

    val allGalleryItems = remember {
        listOf(
            GalleryItemDef(
                id = "weather",
                title = "Weather",
                subtitle = "Check current conditions and hourly forecast.",
                appName = "Weather",
                description = "Keep track of current temperatures and conditions.",
                defaultKind = WidgetKind.Weather,
                sizes = listOf(1 to 1, 2 to 1)
            ),
            GalleryItemDef(
                id = "clock",
                title = "Clock",
                subtitle = "Keep track of time in analog and world clocks.",
                appName = "Clock",
                description = "Analog clock dial with real-time second hand.",
                defaultKind = WidgetKind.Clock,
                sizes = listOf(1 to 1, 2 to 1)
            ),
            GalleryItemDef(
                id = "calendar",
                title = "Up Next",
                subtitle = "View upcoming meetings, events and schedule.",
                appName = "Calendar",
                description = "See what's next on your schedule.",
                defaultKind = WidgetKind.Calendar,
                sizes = listOf(1 to 1, 2 to 1, 2 to 2)
            ),
            GalleryItemDef(
                id = "fitness",
                title = "Activity",
                subtitle = "Track your daily active calories, steps and movement.",
                appName = "Fitness",
                description = "See your activity progress at a glance.",
                defaultKind = WidgetKind.Activity,
                sizes = listOf(1 to 1, 2 to 1)
            ),
            GalleryItemDef(
                id = "batteries",
                title = "Batteries",
                subtitle = "View device and connected accessory battery levels.",
                appName = "Batteries",
                description = "Battery levels for your phone and accessories.",
                defaultKind = WidgetKind.Batteries,
                sizes = listOf(1 to 1, 2 to 1)
            ),
            GalleryItemDef(
                id = "storage",
                title = "Storage",
                subtitle = "Check internal disk usage, available free space and capacity.",
                appName = "Storage",
                description = "Monitor storage usage and free disk space at a glance.",
                defaultKind = WidgetKind.Storage,
                sizes = listOf(1 to 1, 2 to 1)
            ),
            GalleryItemDef(
                id = "music",
                title = "Music",
                subtitle = "Play your recently played music station and songs.",
                appName = "Music",
                description = "Play music from your library and recommendations.",
                defaultKind = WidgetKind.Music,
                sizes = listOf(2 to 1, 1 to 1)
            ),
            GalleryItemDef(
                id = "smart_stack",
                title = "Smart Stack",
                subtitle = "A stack of widgets that flips automatically or with a swipe.",
                appName = "Smart Stack",
                description = "Swipe through your most relevant widgets.",
                defaultKind = WidgetKind.Weather,
                sizes = listOf(1 to 1, 2 to 1),
                stack = listOf(WidgetKind.Music, WidgetKind.Activity, WidgetKind.Calendar)
            ),
            GalleryItemDef(
                id = "photos",
                title = "Featured Photos",
                subtitle = "Browse your favorite memories and featured photos.",
                appName = "Photos",
                description = "Relive your best moments right on your home screen.",
                defaultKind = WidgetKind.Photos,
                sizes = listOf(1 to 1, 2 to 1, 2 to 2)
            ),
            GalleryItemDef(
                id = "notes",
                title = "Quick Note",
                subtitle = "Capture ideas and notes quickly.",
                appName = "Notes",
                description = "Pin your important thoughts and reminders.",
                defaultKind = WidgetKind.Notes,
                sizes = listOf(1 to 1, 2 to 1)
            ),
            GalleryItemDef(
                id = "reminders",
                title = "Reminders",
                subtitle = "Stay organized with your to-do lists.",
                appName = "Reminders",
                description = "Check off completed tasks with a single tap.",
                defaultKind = WidgetKind.Reminders,
                sizes = listOf(1 to 1, 2 to 1)
            ),
            GalleryItemDef(
                id = "suggestions",
                title = "Suggestions",
                subtitle = "Smart app suggestions based on time and your activity.",
                appName = "Suggestions",
                description = "Suggests what you might want to do next based on your routine, like opening your music in the morning or messaging your contacts.",
                defaultKind = WidgetKind.Suggestions,
                sizes = listOf(2 to 1, 2 to 2, 1 to 1)
            ),
            GalleryItemDef(
                id = "hydration",
                title = "Hydration",
                subtitle = "Track your daily water intake and stay hydrated.",
                appName = "Hydration",
                description = "Monitor daily water intake, log glasses with a tap, and maintain your hydration streak.",
                defaultKind = WidgetKind.Hydration,
                sizes = listOf(1 to 1, 2 to 1)
            ),
            GalleryItemDef(
                id = "screentime",
                title = "Screen Time",
                subtitle = "See how much time you spend on your device.",
                appName = "Screen Time",
                description = "Get insights on your daily device usage, top app categories, and daily trends.",
                defaultKind = WidgetKind.ScreenTime,
                sizes = listOf(1 to 1, 2 to 1)
            )
        )
    }

    val filteredItems = remember(query, allGalleryItems) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allGalleryItems
        else allGalleryItems.filter { it.title.lowercase().contains(q) || it.appName.lowercase().contains(q) }
    }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val sheetOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 110.dp.toPx() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f && sheetOffsetY.value > 0f) {
                    val consumed = delta.coerceAtLeast(-sheetOffsetY.value)
                    scope.launch {
                        sheetOffsetY.snapTo(sheetOffsetY.value + consumed)
                    }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0f && source == NestedScrollSource.UserInput) {
                    scope.launch {
                        sheetOffsetY.snapTo(sheetOffsetY.value + delta)
                    }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (sheetOffsetY.value > 0f) {
                    val shouldDismiss = sheetOffsetY.value > dismissThresholdPx || available.y > 600f
                    if (shouldDismiss) {
                        sheetOffsetY.animateTo(1400f, tween(180, easing = FastOutSlowInEasing))
                        onDismiss()
                    } else {
                        sheetOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                    }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (sheetOffsetY.value > 0f) {
                    val shouldDismiss = sheetOffsetY.value > dismissThresholdPx || available.y > 600f
                    if (shouldDismiss) {
                        sheetOffsetY.animateTo(1400f, tween(180, easing = FastOutSlowInEasing))
                        onDismiss()
                    } else {
                        sheetOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = (1f - (sheetOffsetY.value / 450f)).coerceIn(0f, 1f)
                }
        ) {
            Scrim(onDismiss, alpha = 0.38f)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(max = 780.dp)
                .graphicsLayer { translationY = sheetOffsetY.value.coerceAtLeast(0f) }
                .nestedScroll(nestedScrollConnection)
                .liquidGlass(SmoothShape(38.dp), 38.dp, GlassStyle.Regular)
                .consumeTaps()
                .padding(top = 8.dp, bottom = 18.dp, start = 18.dp, end = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // iOS Grab Handle Dock (Swipe down smoothly anywhere above the search widget to dismiss)
            var lastDragTime by remember { mutableStateOf(0L) }
            var dragVelocityY by remember { mutableFloatStateOf(0f) }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                lastDragTime = System.currentTimeMillis()
                                dragVelocityY = 0f
                            },
                            onDragEnd = {
                                scope.launch {
                                    val shouldDismiss = sheetOffsetY.value > dismissThresholdPx || dragVelocityY > 600f
                                    if (shouldDismiss) {
                                        sheetOffsetY.animateTo(1400f, tween(180, easing = FastOutSlowInEasing))
                                        onDismiss()
                                    } else {
                                        sheetOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    sheetOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                }
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            val now = System.currentTimeMillis()
                            val dt = (now - lastDragTime).coerceAtLeast(1)
                            dragVelocityY = (dragAmount / dt) * 1000f
                            lastDragTime = now
                            scope.launch {
                                sheetOffsetY.snapTo((sheetOffsetY.value + dragAmount).coerceAtLeast(0f))
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 38.dp, height = 5.dp)
                        .background(Color.White.copy(alpha = 0.32f), Capsule)
                )
            }

            Spacer(Modifier.height(8.dp))

            AnimatedContent(
                targetState = selectedItem,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "galleryNav"
            ) { item ->
                if (item != null) {
                    // Detail Sheet (Matches media_1793444889587_5f640a73.jpg & media_1793444889587_4e89cb78.jpg)
                    WidgetDetailSheet(
                        item = item,
                        suggestions = suggestions,
                        icons = icons,
                        onAdd = { w, h ->
                            AppleTaptic.medium(context)
                            if (onAddWithSize != null) {
                                onAddWithSize(item.defaultKind, w, h, item.stack)
                            } else {
                                onToggle(item.defaultKind)
                            }
                            onDismiss()
                        },
                        onBack = { selectedItem = null },
                        settings = settings,
                    )
                } else {
                    // Main Gallery (Matches media_1793444889588_4715189d.jpg)
                    Column(Modifier.fillMaxWidth()) {
                        // Search Bar: Search Widgets
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(Color(0x33767680), Capsule)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = PanelBodyStyle.copy(fontSize = 17.sp),
                                cursorBrush = SolidColor(AccentBlue),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    Box {
                                        if (query.isEmpty()) {
                                            Text("Search Widgets", style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.45f)))
                                        }
                                        inner()
                                    }
                                }
                            )
                            if (query.isNotEmpty()) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { query = "" }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (query.isEmpty()) {
                                // Featured Wide Widget Card at top (matching reference media_1793444889588_4715189d.jpg)
                                val featured = filteredItems.firstOrNull { it.id == "music" } ?: filteredItems.firstOrNull()
                                if (featured != null) {
                                    item(key = "featured_${featured.id}") {
                                        Column(
                                            Modifier
                                                .fillMaxWidth()
                                        ) {
                                            BoxWithConstraints(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(148.dp)
                                                    .clip(SmoothShape(24.dp))
                                            ) {
                                                ScaledWidget(
                                                    kind = featured.defaultKind,
                                                    size = DpSize(maxWidth, 148.dp),
                                                    unit = 148.dp,
                                                    suggestions = suggestions,
                                                    icons = icons,
                                                    onLaunch = { _, _ -> }
                                                )
                                                // Tap overlay to select widget detail
                                                Box(
                                                    Modifier
                                                        .matchParentSize()
                                                        .clickable {
                                                            selectedItem = featured
                                                            AppleTaptic.selection(context)
                                                        }
                                                )
                                                // Direct Add Button Pill
                                                Box(
                                                    Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(10.dp)
                                                        .size(30.dp)
                                                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                                        .clickable {
                                                            AppleTaptic.medium(context)
                                                            if (onAddWithSize != null) {
                                                                onAddWithSize(featured.defaultKind, 2, 1, featured.stack)
                                                            } else {
                                                                onToggle(featured.defaultKind)
                                                            }
                                                            onDismiss()
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Rounded.Add, contentDescription = "Add Widget", tint = Color.White, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(featured.appName, style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                                        }
                                    }
                                }

                                // 2-Column Grid of Squarish Cards (occupies full space, no black border, same theme as leftmost screen)
                                val remaining = filteredItems.filter { it != featured }
                                val pairs = remaining.chunked(2)
                                items(pairs, key = { it.joinToString("-") { def -> def.id } }) { pair ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        for (itemDef in pair) {
                                            Column(
                                                Modifier.weight(1f)
                                            ) {
                                                BoxWithConstraints(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(SmoothShape(24.dp))
                                                ) {
                                                    ScaledWidget(
                                                        kind = itemDef.defaultKind,
                                                        size = DpSize(maxWidth, maxWidth),
                                                        unit = maxWidth,
                                                        suggestions = suggestions,
                                                        icons = icons,
                                                        onLaunch = { _, _ -> }
                                                    )
                                                    // Tap overlay to select widget detail
                                                    Box(
                                                        Modifier
                                                            .matchParentSize()
                                                            .clickable {
                                                                selectedItem = itemDef
                                                                AppleTaptic.selection(context)
                                                            }
                                                    )
                                                    // Direct Add '+' Button in corner
                                                    Box(
                                                        Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(8.dp)
                                                            .size(28.dp)
                                                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                                            .clickable {
                                                                AppleTaptic.medium(context)
                                                                if (onAddWithSize != null) {
                                                                    onAddWithSize(itemDef.defaultKind, 1, 1, itemDef.stack)
                                                                } else {
                                                                    onToggle(itemDef.defaultKind)
                                                                }
                                                                onDismiss()
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Rounded.Add, contentDescription = "Add Widget", tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(itemDef.appName, style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                // Filtered search results: 2-column grid of squarish cards
                                val pairs = filteredItems.chunked(2)
                                items(pairs, key = { it.joinToString("-") { def -> def.id } }) { pair ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        for (itemDef in pair) {
                                            Column(
                                                Modifier.weight(1f)
                                            ) {
                                                BoxWithConstraints(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(SmoothShape(24.dp))
                                                ) {
                                                    ScaledWidget(
                                                        kind = itemDef.defaultKind,
                                                        size = DpSize(maxWidth, maxWidth),
                                                        unit = maxWidth,
                                                        suggestions = suggestions,
                                                        icons = icons,
                                                        onLaunch = { _, _ -> }
                                                    )
                                                    // Tap overlay to select widget detail
                                                    Box(
                                                        Modifier
                                                            .matchParentSize()
                                                            .clickable {
                                                                selectedItem = itemDef
                                                                AppleTaptic.selection(context)
                                                            }
                                                    )
                                                    Box(
                                                        Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(8.dp)
                                                            .size(28.dp)
                                                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                                            .clickable {
                                                                AppleTaptic.medium(context)
                                                                if (onAddWithSize != null) {
                                                                    onAddWithSize(itemDef.defaultKind, 1, 1, itemDef.stack)
                                                                } else {
                                                                    onToggle(itemDef.defaultKind)
                                                                }
                                                                onDismiss()
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Rounded.Add, contentDescription = "Add Widget", tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(itemDef.appName, style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(Modifier.weight(1f))
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
}

/** Detail sub-sheet matching screenshots 2 & 3: swipable sizes, dots, and blue + Add Widget pill */
@Composable
private fun WidgetDetailSheet(
    item: GalleryItemDef,
    suggestions: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    onAdd: (Int, Int) -> Unit,
    onBack: () -> Unit,
    settings: LauncherSettings? = null,
) {
    val pagerState = rememberPagerState(pageCount = { item.sizes.size })

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation: App Icon + App Name on left, 'X' on right
        val widgetBgColor = when (item.defaultKind) {
            WidgetKind.Weather -> AccentBlue
            WidgetKind.Calendar -> DestructiveRed
            WidgetKind.Photos -> AccentPurple
            WidgetKind.Batteries -> AccentGreen
            WidgetKind.Storage -> Color(0xFF5E5CE6)
            WidgetKind.Activity -> Color(0xFFFF2D55)
            WidgetKind.Music -> Color(0xFFFA2D48)
            WidgetKind.Clock -> Color(0xFF3A3A3C)
            WidgetKind.Suggestions -> Color(0xFF5856D6)
            WidgetKind.Hydration -> Color(0xFF00C7BE)
            WidgetKind.ScreenTime -> Color(0xFF5856D6)
            else -> AccentOrange
        }
        val widgetIcon = when (item.defaultKind) {
            WidgetKind.Weather -> Icons.Rounded.WbSunny
            WidgetKind.Music -> Icons.Rounded.MusicNote
            WidgetKind.Clock -> Icons.Rounded.Schedule
            WidgetKind.Calendar -> Icons.Rounded.CalendarToday
            WidgetKind.Batteries -> Icons.Rounded.BatteryChargingFull
            WidgetKind.Storage -> Icons.Rounded.Storage
            WidgetKind.Notes -> Icons.Rounded.EditNote
            WidgetKind.Reminders -> Icons.Rounded.CheckCircle
            WidgetKind.Photos -> Icons.Rounded.Photo
            WidgetKind.Suggestions -> Icons.Rounded.AutoAwesome
            WidgetKind.Hydration -> Icons.Rounded.WaterDrop
            WidgetKind.ScreenTime -> Icons.Rounded.HourglassTop
            else -> Icons.Rounded.Widgets
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(widgetBgColor, SmoothShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(widgetIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(item.appName, style = PanelTitleStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Large Centered Title & Description
        Text(item.title, style = LargeTitleStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            item.subtitle,
            style = PanelCaptionStyle.copy(fontSize = 15.sp, color = Color.White.copy(alpha = 0.65f)),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(28.dp))

        // 3D Perspective Slider of every size widget (as in Apple iOS)
        val hasLarge = item.sizes.any { it.second > 1 }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (hasLarge) 270.dp else 190.dp),
        ) { pageIndex ->
            val (w, h) = item.sizes[pageIndex]
            val isSmall = w == 1 && h == 1
            val isLarge = h > 1
            val cardW = if (isSmall) 160.dp else 300.dp
            val cardH = if (isLarge) 250.dp else 160.dp
            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)

            val isTinted = settings?.iconStyle == IconStyle.Tinted
            val tintColor = Color(settings?.tintColor ?: 0xFF007AFF.toInt())
            val frost = settings?.widgetFrost ?: 0.45f
            val isDark = settings?.glassLook == GlassLook.Tinted
            val cardShape = SmoothShape(26.dp)

            var manualTiltRight by remember(pageIndex) { mutableStateOf(pageIndex % 2 != 0) }
            val baseTiltSign = if (manualTiltRight) 1f else -1f
            val targetBaseY = 11f * baseTiltSign
            val targetBaseZ = 2f * baseTiltSign
            val animatedBaseY by animateFloatAsState(targetBaseY, spring(dampingRatio = 0.75f, stiffness = 280f), label = "tiltY")
            val animatedBaseZ by animateFloatAsState(targetBaseZ, spring(dampingRatio = 0.75f, stiffness = 280f), label = "tiltZ")

            val rotY = animatedBaseY - pageOffset * 22f
            val rotX = 7.5f - pageOffset * 3.5f
            val rotZ = animatedBaseZ - pageOffset * 1.5f

            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        android.util.Log.d("WidgetTilt", "Card tapped! Old manualTiltRight: $manualTiltRight")
                        manualTiltRight = !manualTiltRight
                    }
                    .graphicsLayer {
                        // 3D perspective tilt for widget size selector (supports left & right tilt)
                        rotationY = rotY
                        rotationX = rotX
                        rotationZ = rotZ
                        cameraDistance = 18f * density
                        val scale = (1f - kotlin.math.abs(pageOffset) * 0.10f).coerceIn(0.80f, 1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - kotlin.math.abs(pageOffset) * 0.35f).coerceIn(0.4f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(cardW, cardH)
                        .shadow(
                            elevation = 18.dp,
                            shape = cardShape,
                            ambientColor = Color.Black.copy(alpha = 0.45f),
                            spotColor = if (isTinted) tintColor.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.55f),
                        )
                        .then(
                            when {
                                isTinted -> Modifier
                                    .background(Color(0xFF1E1E22).copy(alpha = 0.88f), cardShape)
                                    .background(tintColor.copy(alpha = 0.22f + frost * 0.15f), cardShape)
                                    .border(1.2.dp, tintColor.copy(alpha = 0.5f), cardShape)
                                isDark -> Modifier
                                    .background(Color(0xFF1C1C1E).copy(alpha = 0.82f + frost * 0.15f), cardShape)
                                    .border(0.8.dp, Color.White.copy(alpha = 0.18f), cardShape)
                                else -> Modifier
                                    .background(Color.White.copy(alpha = 0.12f + frost * 0.18f), cardShape)
                                    .border(0.8.dp, Color.White.copy(alpha = 0.28f), cardShape)
                            }
                        )
                        .clip(cardShape),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(
                        LocalGlassTuning provides GlassTuning(
                            look = settings?.glassLook ?: GlassLook.Clear,
                            widgetFrost = frost,
                            dockFrost = settings?.dockFrost ?: 0.3f,
                            iconStyle = settings?.iconStyle ?: IconStyle.Default,
                            tintColor = settings?.tintColor ?: 0xFF007AFF.toInt(),
                        )
                    ) {
                        ScaledWidget(
                            kind = item.defaultKind,
                            size = DpSize(cardW, cardH),
                            unit = 160.dp,
                            suggestions = if (item.defaultKind == WidgetKind.Suggestions) emptyList() else suggestions,
                            icons = icons,
                            onLaunch = { _, _ -> }
                        )
                    }
                    // Tap overlay to flip tilt left/right and prevent launching widget apps in preview
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable { manualTiltRight = !manualTiltRight }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Widget size label caption
        val (curW, curH) = item.sizes[pagerState.currentPage]
        val sizeLabel = when {
            curW == 1 && curH == 1 -> "Small (2×2)"
            curW == 2 && curH == 1 -> "Medium (4×2)"
            curW == 2 && curH == 2 -> "Large (4×4)"
            else -> "${curW * 2}×${curH * 2}"
        }
        Text(
            sizeLabel,
            style = PanelCaptionStyle.copy(fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
        )

        Spacer(Modifier.height(10.dp))

        // Pager Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(item.sizes.size) { idx ->
                Box(
                    Modifier
                        .size(if (pagerState.currentPage == idx) 8.dp else 6.dp)
                        .background(
                            if (pagerState.currentPage == idx) Color.White else Color.White.copy(alpha = 0.35f),
                            CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        // Bottom + Add Widget Blue Pill Button
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(0xFF007AFF), Capsule)
                .clickable {
                    val (w, h) = item.sizes[pagerState.currentPage]
                    onAdd(w, h)
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Widget", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}


