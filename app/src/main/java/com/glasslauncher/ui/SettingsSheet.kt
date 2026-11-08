package com.glasslauncher.ui
import android.os.Build
import com.glasslauncher.data.IconScaleRange
import com.glasslauncher.data.LabelScaleRange

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.TextStyle

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.Info
import android.Manifest
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.delay
import com.glasslauncher.data.WeatherPlaceSuggestion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glasslauncher.LauncherViewModel
import com.glasslauncher.data.GlassLook
import com.glasslauncher.data.HomeWallpaperMode
import com.glasslauncher.data.IconStyle
import com.glasslauncher.data.IconLayout
import com.glasslauncher.data.LockClockStyle
import com.glasslauncher.data.LockWidget
import com.glasslauncher.data.LauncherSettings
import com.glasslauncher.data.NotificationDisplay
import com.glasslauncher.wallpaper.COLLECTION_PREFIX
import com.glasslauncher.wallpaper.PRESET_PREFIX
import com.glasslauncher.wallpaper.SHUFFLE_PREFIX
import com.glasslauncher.wallpaper.WallpaperPreset
import com.glasslauncher.wallpaper.WallpaperPresets
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsSheet(vm: LauncherViewModel, settings: LauncherSettings, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    // Grants happen in system settings, so re-check whenever the user comes back.
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val collection by vm.collection.collectAsStateWithLifecycle()
    val pickCollection = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) vm.addToCollection(uris)
    }
    val pickShuffle = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) vm.setShuffleWallpaper(uris)
    }
    val pickFeatured = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
        if (uris.isNotEmpty()) vm.setFeaturedPhotos(uris)
    }
    val requestHome = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh++ }
    val requestLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            vm.useDeviceLocationForWeather()
            android.widget.Toast.makeText(context, "Using device GPS location", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Location permission required for GPS weather", android.widget.Toast.LENGTH_SHORT).show()
        }
        refresh++
        vm.refreshWeather(force = true)
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
        vm.refreshWeather(force = true)
    }
    // Only what's still missing, so a finished setup shows nothing here.
    val missing = remember(refresh) {
        buildList<Pair<String, () -> Unit>> {
            if (!Permissions.isDefaultHome(context)) add("Default Home app" to { requestHome.launch(Permissions.homeRoleIntent(context)) })
            if (Permissions.notificationsSupported && !Permissions.hasNotificationAccess(context)) {
                add("Notifications" to { Permissions.open(context, Permissions.notificationAccessIntent(context), android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
            }
            if (!Permissions.hasLocation(context)) add("Location & GPS for Weather" to { requestLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) })
            if (!Permissions.hasCalendar(context)) add("Calendar" to { requestPermission.launch(Manifest.permission.READ_CALENDAR) })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Permissions.hasActivityRecognition(context)) {
                add("Physical Activity (Step Tracker)" to { requestPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION) })
            }
            if (!Permissions.canWriteSettings(context)) add("Brightness & Rotation" to { Permissions.openWriteSettings(context) })
            if (!Permissions.canControlDnd(context)) add("Focus (Do Not Disturb)" to { context.startSafely(Permissions.dndIntent()) })
            // Optional: a swipe just below the status bar opens Glass Control Centre inside other apps too.
            if (settings.glassStatusBar && !com.glasslauncher.ui.OverlayControlCentre.canDraw(context)) {
                add("Control Centre in All Apps" to { context.startSafely(com.glasslauncher.ui.OverlayControlCentre.permissionIntent(context)) })
            }
        }
    }
    var setupOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Scrim(onDismiss, alpha = 0.28f)
        Column(
            Modifier
                .align(Alignment.Center)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .liquidGlass(SmoothShape(40.dp), style = GlassStyle.Regular)
                .consumeTaps()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(com.glasslauncher.data.LanguageHelper.string("home_screen"), style = LargeTitleStyle.copy(fontSize = 26.sp), modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White) }
            }

            if (missing.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(SmoothShape(18.dp))
                        .background(AccentBlue.copy(alpha = 0.35f))
                        .clickable { setupOpen = !setupOpen }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(com.glasslauncher.data.LanguageHelper.string("finish_setup"), style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                    Text("${missing.size}", style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold))
                    Icon(if (setupOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = Color.White, modifier = Modifier.padding(start = 6.dp))
                }
                AnimatedVisibility(setupOpen) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        missing.forEach { (label, request) -> PermissionRow(label, false, request) }
                        if (Permissions.showRestrictedHint && (Permissions.notificationsSupported && !Permissions.hasNotificationAccess(context)) || (Permissions.panelGuardSupported && settings.glassStatusBar && !Permissions.hasPanelGuard(context))) {
                            Text(Permissions.RESTRICTED_HINT, style = PanelCaptionStyle.copy(fontSize = 13.sp))
                            PillButton("Open App Info", Icons.Rounded.Info) { context.startSafely(Permissions.appInfoIntent(context)) }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SectionTitle(com.glasslauncher.data.LanguageHelper.string("glass")) }
                IconButton(onClick = vm::resetGlass, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset glass", tint = Color.White)
                }
            }
            Label(com.glasslauncher.data.LanguageHelper.string("look"))
            Choice(GlassLook.entries, settings.glassLook, label = { it.title }, onSelect = vm::setGlassLook)
            FrostSlider(com.glasslauncher.data.LanguageHelper.string("widgets_panels"), settings.widgetFrost, vm::setWidgetFrost)
            FrostSlider(com.glasslauncher.data.LanguageHelper.string("dock_search"), settings.dockFrost, vm::setDockFrost)

            SectionTitle(com.glasslauncher.data.LanguageHelper.string("icon_pack"))
            val packs by produceState(emptyList<com.glasslauncher.icons.IconPack>(), refresh) { value = vm.installedIconPacks() }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { PackChip("Glass OS", settings.iconPack == null) { vm.setIconPack(null) } }
                items(packs, key = { it.packageName }) { pack ->
                    PackChip(pack.label, settings.iconPack == pack.packageName) { vm.setIconPack(pack.packageName) }
                }
                item {
                    PackChip("Download Icon Packs ▾", false) {
                        context.startSafely(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://search?q=icon pack&c=apps")))
                    }
                }
            }

            SwitchRow(com.glasslauncher.data.LanguageHelper.string("show_app_names"), settings.showLabels, vm::setShowLabels)
            if (settings.showLabels) LabelSizeSlider(settings.labelScale, vm::setLabelScale)
            // On: apps always close up gaps in order (portrait and landscape each keep their own order).
            SwitchRow(com.glasslauncher.data.LanguageHelper.string("arrange_auto"), settings.autoArrange, vm::setAutoArrange)

            SectionTitle("Lock Screen")
            SwitchRow("3D Parallax Effect", settings.lock3dEffect, vm::setLock3dEffect)
            Label("Clock Style")
            Choice(LockClockStyle.entries, settings.lockClockStyle, label = { it.title }, onSelect = vm::setLockClockStyle)
            Label("Wallpaper")
            Choice(
                listOf("spiderman" to "Spider-Man 3D", "liquid" to "Liquid 3D", "solstice" to "Solstice 3D", "aurora" to "Aurora 3D", "home" to "Home Wallpaper"),
                (settings.lockWallpaper to when (settings.lockWallpaper) {
                    "spiderman" -> "Spider-Man 3D"
                    "solstice" -> "Solstice 3D"
                    "aurora" -> "Aurora 3D"
                    "home" -> "Home Wallpaper"
                    else -> "Liquid 3D"
                }),
                label = { it.second },
                onSelect = { vm.setLockWallpaper(it.first) }
            )
            Label("Lock Screen Widgets")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LockWidget.entries) { w ->
                    val active = w in settings.lockWidgets
                    Box(
                        Modifier
                            .clip(Capsule)
                            .background(if (active) AccentGreen else Color.White.copy(alpha = 0.12f))
                            .clickable { vm.toggleLockWidget(w) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(w.title, style = PanelBodyStyle.copy(fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = Color.White))
                    }
                }
            }

            SectionTitle(com.glasslauncher.data.LanguageHelper.string("widgets"))
            PillButton(com.glasslauncher.data.LanguageHelper.string("choose_photos"), Icons.Rounded.AddPhotoAlternate) {
                pickFeatured.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            val featuredPhotoNames by vm.featuredPhotos.collectAsStateWithLifecycle()
            if (featuredPhotoNames.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Featured Photos (${featuredPhotoNames.size})", style = PanelCaptionStyle.copy(fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f)))
                    Text(
                        "Clear all",
                        style = PanelCaptionStyle.copy(fontSize = 12.sp, color = DestructiveRed, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clickable { vm.clearFeaturedPhotos() }
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(featuredPhotoNames, key = { it }) { name ->
                        val file = remember(name) { vm.featuredFile(name) }
                        val bitmap = remember(name) {
                            runCatching {
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                            }.getOrNull()
                        }
                        Box(Modifier.size(64.dp)) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Featured photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                )
                            }
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .clickable { vm.removeFeaturedPhoto(name) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
            WeatherLocation(
                current = settings.weatherPlace?.substringAfterLast("|"),
                isDevice = settings.weatherPlace == null,
                onSearchSuggestions = { q -> vm.searchWeatherSuggestions(q) },
                onSelectSuggestion = { suggestion ->
                    scope.launch {
                        vm.setWeatherPlace(suggestion.placeKey)
                        android.widget.Toast.makeText(context, "${suggestion.name} selected", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onSearch = { name ->
                    scope.launch {
                        if (!vm.setWeatherCity(name)) {
                            android.widget.Toast.makeText(context, "City not found", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "$name selected", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onUseDevice = {
                    if (!Permissions.hasFineLocation(context)) {
                        requestLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    } else {
                        vm.useDeviceLocationForWeather()
                        android.widget.Toast.makeText(context, "Using device GPS location", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
            )
            // MET Norway's licence (CC BY 4.0) asks for credit wherever the app is published.
            if (com.glasslauncher.BuildConfig.FLAVOR == "play") {
                Text("Weather data: MET Norway (CC BY 4.0)", style = PanelCaptionStyle.copy(fontSize = 11.sp))
            }

        }
    }
}

@Composable
fun PresetThumb(preset: WallpaperPreset, selected: Boolean, thumbnail: suspend () -> ImageBitmap, onClick: () -> Unit) {
    val image by produceState<ImageBitmap?>(null, preset.id) { value = thumbnail() }
    val shape = SmoothShape(16.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(72.dp, 104.dp)
                .clip(shape)
                .border(if (selected) 2.5.dp else 1.dp, Color.White.copy(alpha = if (selected) 1f else 0.25f), shape)
                .clickable(onClick = onClick),
        ) {
            image?.let { Image(it, contentDescription = preset.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Text(preset.name, style = PanelCaptionStyle, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun CollectionThumb(selected: Boolean, thumbnail: suspend () -> ImageBitmap?, onClick: () -> Unit, onRemove: () -> Unit) {
    val image by produceState<ImageBitmap?>(null) { value = thumbnail() }
    val shape = SmoothShape(16.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                Modifier
                    .size(72.dp, 104.dp)
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(if (selected) 2.5.dp else 1.dp, Color.White.copy(alpha = if (selected) 1f else 0.25f), shape)
                    .clickable(onClick = onClick),
            ) {
                image?.let { Image(it, contentDescription = "My wallpaper", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .clickable(onClickLabel = "Remove", onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Text("Mine", style = PanelCaptionStyle, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun AddThumb(onClick: () -> Unit) {
    val shape = SmoothShape(16.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(72.dp, 104.dp)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Add wallpapers", tint = Color.White)
        }
        Text("Add", style = PanelCaptionStyle, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun PhotoTile(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = SmoothShape(18.dp)
    Row(
        modifier
            .height(52.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(if (selected) 2.5.dp else 0.dp, Color.White, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(label, style = PanelBodyStyle.copy(fontSize = 15.sp), modifier = Modifier.padding(start = 10.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    // Wraps onto more lines on narrow screens instead of running off the edge.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.12f))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = PanelBodyStyle.copy(fontSize = 15.sp, color = if (isSelected) Color(0xFF1C1C1E) else Color.White, fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = PanelBodyStyle, modifier = Modifier.weight(1f))
        GlassSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PillButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(text, style = PanelBodyStyle, modifier = Modifier.padding(start = 12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiChoice(options: List<T>, selected: List<T>, label: (T) -> String, onToggle: (T) -> Unit) {
    // Wraps onto more lines on narrow screens instead of running off the edge.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option in selected
            Box(
                Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.12f))
                    .clickable { onToggle(option) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = PanelBodyStyle.copy(fontSize = 15.sp, color = if (isSelected) Color(0xFF1C1C1E) else Color.White, fontWeight = FontWeight.Medium))
            }
        }
    }
}

/** iOS Tinted colour picker: a hue slider over a rainbow track. */
@Composable
internal fun HueSlider(color: Int, onChange: (Int) -> Unit) {
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) }
    Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .height(10.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 0.8f, 1f) })),
        )
        GlassSlider(
            showFill = false,
            value = hsv[0],
            onValueChange = { onChange(android.graphics.Color.HSVToColor(floatArrayOf(it, 0.8f, 1f))) },
            valueRange = 0f..359f,
        )
    }
}

@Composable
private fun StepSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Row {
            Text(label, style = PanelBodyStyle, modifier = Modifier.weight(1f))
            Text(value.toString(), style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold))
        }
        GlassSlider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
        )
    }
}

@Composable
private fun PackChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PanelBodyStyle.copy(fontSize = 15.sp, color = if (selected) Color(0xFF1C1C1E) else Color.White, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

@Composable
fun Label(text: String) {
    Text(text, style = PanelCaptionStyle.copy(fontSize = 13.sp), modifier = Modifier.padding(top = 2.dp))
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = PanelCaptionStyle.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
fun FrostSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = PanelBodyStyle, modifier = Modifier.weight(1f))
            Text(
                when {
                    value < 0.15f -> "Clear"
                    value < 0.55f -> "Light Frost"
                    else -> "Frosted"
                },
                style = PanelCaptionStyle,
            )
        }
        GlassSlider(
            value = value,
            onValueChange = onChange,
        )
    }
}

@Composable
fun WallpaperBlurSlider(value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Blur Home Screen Wallpaper", style = PanelBodyStyle, modifier = Modifier.weight(1f))
            Text(
                when {
                    value < 0.05f -> "Off"
                    value < 0.5f -> "Soft"
                    else -> "Strong"
                },
                style = PanelCaptionStyle,
            )
        }
        GlassSlider(
            value = value,
            onValueChange = onChange,
        )
    }
}

@Composable
fun LabelSizeSlider(value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(com.glasslauncher.data.LanguageHelper.string("app_name_size"), style = PanelBodyStyle, modifier = Modifier.weight(1f))
            Text("${(value * 100).roundToInt()}%", style = PanelCaptionStyle)
        }
        GlassSlider(
            value = value,
            onValueChange = onChange,
            valueRange = LabelScaleRange,
        )
    }
}

@Composable
fun IconSizeSlider(value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(com.glasslauncher.data.LanguageHelper.string("app_icon_size"), style = PanelBodyStyle, modifier = Modifier.weight(1f))
            Text("${(value * 100).roundToInt()}%", style = PanelCaptionStyle)
        }
        GlassSlider(
            value = value,
            onValueChange = onChange,
            valueRange = IconScaleRange,
        )
    }
}

@Composable
fun WeatherLocation(
    current: String?,
    isDevice: Boolean,
    onSearchSuggestions: suspend (String) -> List<WeatherPlaceSuggestion>,
    onSelectSuggestion: (WeatherPlaceSuggestion) -> Unit,
    onSearch: (String) -> Unit,
    onUseDevice: () -> Unit,
) {
    var query by remember(current, isDevice) { mutableStateOf(if (isDevice) "" else (current ?: "")) }
    var suggestions by remember { mutableStateOf<List<WeatherPlaceSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length >= 2 && trimmed != current) {
            delay(280)
            isSearching = true
            suggestions = runCatching { onSearchSuggestions(trimmed) }.getOrDefault(emptyList())
            isSearching = false
        } else {
            suggestions = emptyList()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Weather Location", style = PanelBodyStyle, modifier = Modifier.weight(1f))
            if (isDevice) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.NearMe, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(12.dp))
                    Text("Device GPS", style = PanelCaptionStyle.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold))
                }
            } else if (!current.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(12.dp))
                    Text(current, style = PanelCaptionStyle.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(Capsule)
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = PanelBodyStyle.copy(fontSize = 13.5.sp),
                            cursorBrush = SolidColor(AccentBlue),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    val trimmed = query.trim()
                                    if (trimmed.isNotBlank()) {
                                        val match = suggestions.firstOrNull()
                                        if (match != null) {
                                            onSelectSuggestion(match)
                                            query = match.name
                                        } else {
                                            onSearch(trimmed)
                                        }
                                        suggestions = emptyList()
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            decorationBox = { field ->
                                if (query.isEmpty()) {
                                    Text(
                                        if (isDevice) "Search city (e.g. London)" else "Search city…",
                                        style = PanelCaptionStyle.copy(fontSize = 13.5.sp)
                                    )
                                }
                                field()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (isSearching) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = AccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (query.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    query = ""
                                    suggestions = emptyList()
                                }
                        )
                    }
                }
            }

            // Location icon button (same AccentBlue color, tapping asks permission if needed)
            Box(
                Modifier
                    .size(38.dp)
                    .clip(Capsule)
                    .background(if (isDevice) AccentBlue else Color.White.copy(alpha = 0.12f))
                    .clickable {
                        query = ""
                        suggestions = emptyList()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onUseDevice()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.NearMe,
                    contentDescription = "Device GPS",
                    tint = if (isDevice) Color.White else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Suggestions Dropdown/List
        if (suggestions.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(SmoothShape(14.dp))
                    .background(Color(0xFF222428).copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), SmoothShape(14.dp))
                    .padding(vertical = 4.dp)
            ) {
                suggestions.forEachIndexed { index, item ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSuggestion(item)
                                query = item.name
                                suggestions = emptyList()
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name,
                                style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            )
                            if (item.subtitle.isNotBlank()) {
                                Text(
                                    item.subtitle,
                                    style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp)
                                )
                            }
                        }
                        if (current == item.name) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
