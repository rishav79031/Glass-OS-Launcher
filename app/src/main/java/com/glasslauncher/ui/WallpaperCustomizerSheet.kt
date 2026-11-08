package com.glasslauncher.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.R
import com.glasslauncher.LauncherViewModel
import com.glasslauncher.data.HomeWallpaperMode
import com.glasslauncher.data.LockClockStyle
import com.glasslauncher.wallpaper.COLLECTION_PREFIX
import com.glasslauncher.wallpaper.PRESET_PREFIX
import com.glasslauncher.wallpaper.SHUFFLE_PREFIX
import com.glasslauncher.wallpaper.WallpaperPresets

/**
 * iOS 18 style Dual-Card Wallpaper Customizer:
 * Fully consolidates Lock Screen and Home Screen wallpaper settings, presets, modes, shuffle and blur.
 */
@Composable
fun WallpaperCustomizerSheet(
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val collection by vm.collection.collectAsState()
    val currentWallpaper by vm.wallpaper.collectAsState()
    val context = LocalContext.current

    val pickHomePhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) vm.setCustomWallpaper(uri)
    }

    val pickLockPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) vm.setCustomWallpaper(uri)
    }

    val pickShuffle = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)) { uris: List<Uri> ->
        if (uris.isNotEmpty()) vm.setShuffleWallpaper(uris)
    }

    val pickCollection = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)) { uris: List<Uri> ->
        if (uris.isNotEmpty()) vm.addToCollection(uris)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .consumeTaps()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Text(
                    "Customise Wallpaper",
                    style = LargeTitleStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )

                Box(
                    Modifier
                        .height(34.dp)
                        .clip(Capsule)
                        .background(Color(0xFF007AFF))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Done", style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Main Content (Scrollable)
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Wallpaper Presets Carousel at the TOP
                Column(
                    Modifier
                        .fillMaxWidth()
                        .liquidGlass(SmoothShape(22.dp), 22.dp, GlassStyle.Regular)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Wallpaper Presets", style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item(key = "add") {
                            AddThumb { pickCollection.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        }
                        items(collection, key = { it }) { name ->
                            CollectionThumb(
                                selected = settings.wallpaper == COLLECTION_PREFIX + name,
                                thumbnail = { vm.collectionThumbnail(name) },
                                onClick = { vm.setCollectionWallpaper(name) },
                                onRemove = { vm.removeFromCollection(name) },
                            )
                        }
                        items(WallpaperPresets, key = { it.id }) { preset ->
                            PresetThumb(preset, selected = settings.wallpaper == PRESET_PREFIX + preset.id, thumbnail = { vm.presetThumbnail(preset) }) {
                                vm.setPresetWallpaper(preset)
                            }
                        }
                    }
                }

                // Dual Preview Cards: Lock Screen & Home Screen
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card 1: Lock Screen
                    Column(
                        Modifier
                            .weight(1f)
                            .liquidGlass(SmoothShape(24.dp), 24.dp, GlassStyle.Regular)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lock Screen", style = PanelBodyStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                        }

                        // Simulated Lock Screen Preview with actual applied wallpaper!
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.DarkGray.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            when {
                                settings.lockWallpaper == "curves" -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.wallpaper_curves),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                settings.lockWallpaper == "sunset_palms" -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.wallpaper_sunset_palms),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                settings.lockWallpaper == "spiderman" -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.wallpaper_spiderman),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                settings.lockWallpaper == "home" && currentWallpaper?.sharp != null -> {
                                    Image(
                                        bitmap = currentWallpaper!!.sharp!!,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                else -> {
                                    val gradient = when (settings.lockWallpaper) {
                                        "solstice" -> listOf(Color(0xFF0A0F33), Color(0xFF8338EC), Color(0xFFFF006E))
                                        "aurora" -> listOf(Color(0xFF10123A), Color(0xFF2EE6C5), Color(0xFF8B5CF6))
                                        else -> listOf(Color(0xFF0A1F5C), Color(0xFF4FC3FF), Color(0xFF1B0F3B))
                                    }
                                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(gradient)))
                                }
                            }
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
                            Column(
                                Modifier.padding(top = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val fontInfo = lockClockFont(settings.lockClockStyle)
                                Text(
                                    "9:41",
                                    style = TextStyle(
                                        fontFamily = fontInfo.first,
                                        fontSize = 32.sp,
                                        fontWeight = fontInfo.second,
                                        color = Color(settings.lockClockColor)
                                    )
                                )
                                Text("Tuesday, 9 September", style = PanelCaptionStyle.copy(fontSize = 11.sp))
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    Modifier
                                        .padding(horizontal = 12.dp)
                                        .height(32.dp)
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.15f), Capsule),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Notification Stack", style = PanelCaptionStyle.copy(fontSize = 11.sp))
                                }
                            }
                        }

                        // Action Button
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(Capsule)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    pickLockPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Choose Photo", style = PanelBodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium))
                            }
                        }
                    }

                    // Card 2: Home Screen
                    Column(
                        Modifier
                            .weight(1f)
                            .liquidGlass(SmoothShape(24.dp), 24.dp, GlassStyle.Regular)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Home Screen", style = PanelBodyStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                        }

                        // Simulated Home Screen Preview with applied home wallpaper & blur!
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.DarkGray.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val homeBmp = if (settings.wallpaperBlur > 0.05f) currentWallpaper?.heavy ?: currentWallpaper?.sharp else currentWallpaper?.sharp
                            homeBmp?.let { bmp ->
                                Image(
                                    bitmap = bmp,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (settings.homeWallpaperMode == HomeWallpaperMode.Blur) {
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                            } else if (settings.homeWallpaperMode == HomeWallpaperMode.Colour) {
                                Box(Modifier.fillMaxSize().background(Color(0xFF1C1C1E).copy(alpha = 0.75f)))
                            }
                            Column(
                                Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    repeat(3) {
                                        Box(Modifier.size(18.dp).background(Color.White.copy(alpha = 0.35f), SmoothShape(4.dp)))
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    repeat(3) {
                                        Box(Modifier.size(18.dp).background(Color.White.copy(alpha = 0.35f), SmoothShape(4.dp)))
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .background(Color.White.copy(alpha = 0.2f), Capsule)
                                )
                            }
                        }

                        // Action Button
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(Capsule)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    pickHomePhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Choose Photo", style = PanelBodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium))
                            }
                        }
                    }
                }

                // Pair as Wallpaper Pair Button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(Capsule)
                        .background(Color(0xFF007AFF).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFF007AFF).copy(alpha = 0.5f), Capsule)
                        .clickable {
                            vm.pairBothWallpapers()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pair Both Screens to Same Wallpaper", style = PanelBodyStyle.copy(fontSize = 14.sp, color = Color(0xFF0A84FF), fontWeight = FontWeight.SemiBold))
                    }
                }

                // Lock Screen Appearance & Clock Settings (Lock screen is customisable from here)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .liquidGlass(SmoothShape(22.dp), 22.dp, GlassStyle.Regular)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lock Screen Customisation", style = PanelBodyStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold))
                        }
                        // Reset Clock Position button
                        Box(
                            Modifier
                                .clip(Capsule)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable { vm.setLockClockTransform(1.0f, 0f, 0f) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset Clock", style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White))
                            }
                        }
                    }

                    // 3D Parallax Toggle
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ViewInAr, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("3D Tilt & Parallax Depth", style = PanelBodyStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold))
                        }
                        Switch(
                            checked = settings.lock3dEffect,
                            onCheckedChange = { vm.setLock3dEffect(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF007AFF))
                        )
                    }

                    // Lock Screen Wallpapers Row
                    Text("LOCK WALLPAPER", style = WidgetCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
                    val lockWallpapers = listOf(
                        "curves" to "Curved Glass",
                        "sunset_palms" to "Sunset Palms",
                        "spiderman" to "Spider-Man 3D",
                        "solstice" to "Solstice",
                        "aurora" to "Aurora",
                        "home" to "Home Wallpaper"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(lockWallpapers, key = { it.first }) { (id, name) ->
                            val selected = settings.lockWallpaper == id
                            Box(
                                Modifier
                                    .clip(Capsule)
                                    .background(if (selected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.12f))
                                    .clickable { vm.setLockWallpaper(id) }
                                    .padding(horizontal = 13.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    name,
                                    style = PanelBodyStyle.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Clock Font Style
                    Text("CLOCK FONT", style = WidgetCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LockClockStyle.entries) { style ->
                            val selected = settings.lockClockStyle == style
                            Box(
                                Modifier
                                    .clip(Capsule)
                                    .background(if (selected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.12f))
                                    .clickable { vm.setLockClockStyle(style) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    style.title,
                                    style = TextStyle(
                                        fontFamily = lockClockFont(style).first,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = lockClockFont(style).second
                                    )
                                )
                            }
                        }
                    }

                    // Clock Glass Color
                    Text("CLOCK GLASS COLOR", style = WidgetCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold))
                    val clockPresetColors = listOf(
                        0xFFD8B4FE.toInt(), // Frosted Lavender / Lilac Glass
                        0xFFFFFFFF.toInt(), // Pure White
                        0xFFFF3B30.toInt(), // Vibrant Red
                        0xFF64D2FF.toInt(), // Sky Cyan
                        0xFFFFD60A.toInt(), // Golden Sun
                        0xFF30D158.toInt(), // Emerald Green
                        0xFFAF52DE.toInt(), // Deep Violet
                        0xFFFF2D55.toInt(), // Radiant Pink
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(clockPresetColors) { color ->
                            val selected = settings.lockClockColor == color
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
                                    .clickable { vm.setLockClockColor(color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (Color(color).luminance() > 0.5f) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Home Wallpaper Mode: Original, Blur, Colour, Gradient
                Column(
                    Modifier
                        .fillMaxWidth()
                        .liquidGlass(SmoothShape(22.dp), 22.dp, GlassStyle.Regular)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Home Wallpaper Appearance", style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HomeWallpaperMode.entries.forEach { mode ->
                            val selected = settings.homeWallpaperMode == mode
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(Capsule)
                                    .background(if (selected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.12f))
                                    .clickable { vm.setHomeWallpaperMode(mode) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    mode.title,
                                    style = PanelBodyStyle.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else Color.White.copy(alpha = 0.75f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Blur Slider Card
                Column(
                    Modifier
                        .fillMaxWidth()
                        .liquidGlass(SmoothShape(22.dp), 22.dp, GlassStyle.Regular)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WallpaperBlurSlider(settings.wallpaperBlur, vm::setWallpaperBlur)
                }

                // Photo Shuffle
                PhotoTile("Photo Shuffle", Icons.Rounded.Shuffle, settings.wallpaper.startsWith(SHUFFLE_PREFIX), Modifier.fillMaxWidth()) {
                    pickShuffle.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
        }
    }
}
