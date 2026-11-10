package com.glasslauncher.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.data.Weather
import com.glasslauncher.data.weatherDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** What the Weather and Photos widgets need from outside: data, refresh, and permission prompts. */
@Immutable
data class WidgetEnvironment(
    val weather: Weather? = null,
    val weatherAvailable: Boolean = false,
    val onRequestLocation: () -> Unit = {},
    val onRefreshWeather: () -> Unit = {},
    /** Photos chosen for the Photos widget (no photo-library permission needed). */
    val featuredPhotos: List<java.io.File> = emptyList(),
    val onChoosePhotos: () -> Unit = {},
    val note: String = "",
    val notesList: List<String> = emptyList(),
    val onEditNote: () -> Unit = {},
    val onEditNoteIndex: (Int) -> Unit = {},
    val onNewNote: () -> Unit = {},
    val onDeleteNote: (Int) -> Unit = {},
    val reminders: List<String> = emptyList(),
    val onAddReminder: () -> Unit = {},
    val onEditReminder: (Int) -> Unit = {},
    val onCompleteReminder: (Int) -> Unit = {},
    val calendarAllowed: Boolean = false,
    val onRequestCalendar: () -> Unit = {},
    /** Changes each time the Home Screen comes back, so widgets can pick up new photos. */
    val refreshKey: Int = 0,
)

val LocalWidgetEnvironment = compositionLocalOf { WidgetEnvironment() }

/** iOS Weather widget: city, big temperature, condition and today's high/low over a sky that follows the weather. */
@Composable
fun WeatherWidget(modifier: Modifier) {
    val env = LocalWidgetEnvironment.current
    val context = LocalContext.current
    LaunchedEffect(env.weatherAvailable) { if (env.weatherAvailable) env.onRefreshWeather() }
    val weather = env.weather
    val sky = skyGradient(weather)
    Box(
        modifier
            .clip(WidgetShape)
            .liquidGlass(WidgetShape, style = GlassStyle.Regular)
            .background(sky)
            .clickable { if (env.weatherAvailable) openWeatherApp(context) else env.onRequestLocation() }
            .padding(18.dp),
    ) {
        if (weather == null) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                Spacer(Modifier.size(8.dp))
                Text("Weather", style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold))
                Text(if (env.weatherAvailable) "Loading…" else "Tap to allow location", style = PanelCaptionStyle.copy(color = Color.White.copy(alpha = 0.85f)))
            }
            return@Box
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val place = @Composable {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(weather.place, style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.Rounded.NearMe, contentDescription = null, tint = Color.White, modifier = Modifier.padding(start = 4.dp).size(13.dp))
                }
            }
            val temperature = @Composable { Text("${weather.temperature}°", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light)) }
            val caption = PanelBodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (maxWidth > maxHeight * 1.6f) {
                // Medium: place, temperature and conditions on top, the next six hours below — as in iOS.
                Column(Modifier.fillMaxSize()) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            place()
                            temperature()
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Icon(weatherIcon(weather), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(weatherDescription(weather.code), style = caption, maxLines = 1)
                            Text("H:${weather.high}° L:${weather.low}°", style = caption, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        weather.hourly.take(6).forEach { hour ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(hourLabel(hour.hour), style = caption.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f)))
                                Icon(weatherIcon(Weather("", 0, 0, 0, hour.code, hour.isDay, 0)), contentDescription = null, tint = Color.White, modifier = Modifier.padding(vertical = 4.dp).size(16.dp))
                                Text("${hour.temperature}°", style = caption)
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    place()
                    temperature()
                    Spacer(Modifier.weight(1f))
                    Icon(weatherIcon(weather), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(weatherDescription(weather.code), style = caption, maxLines = 1)
                    Text("H:${weather.high}° L:${weather.low}°", style = caption, maxLines = 1)
                }
            }
        }
    }
}

private fun hourLabel(hour: Int): String = when {
    hour == 0 -> "12AM"
    hour < 12 -> "${hour}AM"
    hour == 12 -> "12PM"
    else -> "${hour - 12}PM"
}

/** Opens the device's weather app: Samsung Weather, then Google's weather page, then a web search. */
private fun openWeatherApp(context: Context) {
    val samsung = context.packageManager.getLaunchIntentForPackage("com.sec.android.daemonapp")
    if (samsung != null && context.startSafely(samsung)) return
    val google = Intent(Intent.ACTION_VIEW, Uri.parse("dynact://velour/weather/ProxyActivity"))
        .setPackage("com.google.android.googlequicksearchbox")
    if (context.startSafely(google)) return
    context.startSafely(Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, "weather"))
}

private fun skyGradient(weather: Weather?): Brush {
    val (top, bottom) = when {
        weather == null -> Color(0xFF3F8EDB) to Color(0xFF6CB4F0)
        !weather.isDay -> Color(0xFF0B1638) to Color(0xFF2A3B6E)
        weather.code >= 51 -> Color(0xFF4B5D73) to Color(0xFF7E92A8)
        weather.code >= 3 -> Color(0xFF5E7D9C) to Color(0xFF93AFCB)
        else -> Color(0xFF2F7FD6) to Color(0xFF68B3F2)
    }
    // A light sky tint over clear glass, so the wallpaper still shows through.
    return Brush.verticalGradient(listOf(top.copy(alpha = 0.34f), bottom.copy(alpha = 0.22f)))
}

private fun weatherIcon(weather: Weather): ImageVector = when (weather.code) {
    0, 1 -> if (weather.isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2 -> if (weather.isDay) Icons.Rounded.WbCloudy else Icons.Rounded.NightsStay
    3, 45, 48 -> Icons.Rounded.Cloud
    in 51..57 -> Icons.Rounded.WaterDrop
    in 61..67, in 80..82 -> Icons.Rounded.Grain
    in 71..77, 85, 86 -> Icons.Rounded.AcUnit
    in 95..99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.Cloud
}

/**
 * Photos widget, like iOS "Featured Photo": a photo from the gallery, changing every half hour.
 * Defaults to scenic copyright-free nature wallpapers if no photos are selected.
 * Tap opens Google Photos (or the gallery).
 */
@Composable
fun PhotosWidget(modifier: Modifier) {
    val env = LocalWidgetEnvironment.current
    val context = LocalContext.current
    // Changes photo every half hour, and only while the Home Screen is showing.
    val slot by rememberTicker(30 * 60 * 1000L) { System.currentTimeMillis() / (30 * 60 * 1000L) }
    val photo by produceState<FeaturedPhoto?>(null, env.featuredPhotos, slot) {
        val files = env.featuredPhotos
        value = if (files.isEmpty()) null else withContext(Dispatchers.IO) { loadFeaturedPhoto(files[(slot % files.size).toInt()]) }
    }
    val defaultNature = remember(slot) { DefaultNatureWallpapers.getPhoto(slot) }
    val current = photo ?: defaultNature
    Box(
        modifier
            .clip(WidgetShape)
            .liquidGlass(WidgetShape, style = GlassStyle.Regular)
            .clickable { if (env.featuredPhotos.isEmpty()) env.onChoosePhotos() else openPhotos(context, null) },
    ) {
        Image(current.image, contentDescription = "Featured photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.65f))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text(current.title, style = PanelBodyStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
            Text(current.date, style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f)), maxLines = 1)
        }
    }
}

@Immutable
private data class FeaturedPhoto(val image: ImageBitmap, val date: String, val title: String = "Featured Photo")

private data class NatureScene(
    val title: String,
    val skyColors: List<Color>,
    val mountainBack: Color,
    val mountainMid: Color,
    val mountainFront: Color,
    val hasSun: Boolean = false,
    val sunColor: Color = Color(0xFFFFE082),
    val sunCenter: Offset = Offset(0.72f, 0.35f),
)

private object DefaultNatureWallpapers {
    private val scenes = listOf(
        NatureScene(
            title = "Mountain Sunrise",
            skyColors = listOf(Color(0xFF1A1C3B), Color(0xFFFF7E5F), Color(0xFFFFD194)),
            mountainBack = Color(0xFF4A3E6D),
            mountainMid = Color(0xFF2D2350),
            mountainFront = Color(0xFF15102A),
            hasSun = true,
            sunColor = Color(0xFFFFEEB2)
        ),
        NatureScene(
            title = "Alpine Lake",
            skyColors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
            mountainBack = Color(0xFF1C3A4B),
            mountainMid = Color(0xFF142B38),
            mountainFront = Color(0xFF0D1B24),
            hasSun = true,
            sunColor = Color(0xFFE0F7FA)
        ),
        NatureScene(
            title = "Golden Twilight",
            skyColors = listOf(Color(0xFF2C0B36), Color(0xFF7A1C52), Color(0xFFE0533C)),
            mountainBack = Color(0xFF3F1937),
            mountainMid = Color(0xFF260D20),
            mountainFront = Color(0xFF140510),
            hasSun = true,
            sunColor = Color(0xFFFFC107)
        ),
        NatureScene(
            title = "Emerald Peaks",
            skyColors = listOf(Color(0xFF134E5E), Color(0xFF3B7A84), Color(0xFF71B280)),
            mountainBack = Color(0xFF1C474D),
            mountainMid = Color(0xFF13363B),
            mountainFront = Color(0xFF0A2024),
            hasSun = false
        ),
    )

    private val cache = mutableMapOf<Int, FeaturedPhoto>()

    fun getPhoto(slot: Long): FeaturedPhoto {
        val index = (slot.coerceAtLeast(0) % scenes.size).toInt()
        return cache.getOrPut(index) {
            val scene = scenes[index]
            val width = 600
            val height = 600
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

            // 1. Sky Gradient
            val skyColorsInt = scene.skyColors.map { it.toArgb() }.toIntArray()
            paint.shader = android.graphics.LinearGradient(
                0f, 0f, 0f, height.toFloat() * 0.75f,
                skyColorsInt, null, android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // 2. Sun (if enabled)
            if (scene.hasSun) {
                val sx = scene.sunCenter.x * width
                val sy = scene.sunCenter.y * height
                val sRadius = width * 0.14f
                val sunColorInt = scene.sunColor.toArgb()
                paint.shader = android.graphics.RadialGradient(
                    sx, sy, sRadius * 2.2f,
                    intArrayOf(sunColorInt, sunColorInt and 0x00FFFFFF),
                    floatArrayOf(0.3f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
                canvas.drawCircle(sx, sy, sRadius * 2.2f, paint)

                paint.shader = null
                paint.color = sunColorInt
                canvas.drawCircle(sx, sy, sRadius, paint)
            }

            // 3. Back Mountain Range
            paint.shader = null
            paint.color = scene.mountainBack.toArgb()
            val backPath = android.graphics.Path().apply {
                moveTo(0f, height * 0.50f)
                lineTo(width * 0.20f, height * 0.38f)
                lineTo(width * 0.45f, height * 0.47f)
                lineTo(width * 0.70f, height * 0.34f)
                lineTo(width * 0.88f, height * 0.44f)
                lineTo(width.toFloat(), height * 0.40f)
                lineTo(width.toFloat(), height.toFloat())
                lineTo(0f, height.toFloat())
                close()
            }
            canvas.drawPath(backPath, paint)

            // 4. Mid Mountain Range
            paint.color = scene.mountainMid.toArgb()
            val midPath = android.graphics.Path().apply {
                moveTo(0f, height * 0.62f)
                lineTo(width * 0.15f, height * 0.52f)
                lineTo(width * 0.35f, height * 0.58f)
                lineTo(width * 0.58f, height * 0.48f)
                lineTo(width * 0.82f, height * 0.60f)
                lineTo(width.toFloat(), height * 0.54f)
                lineTo(width.toFloat(), height.toFloat())
                lineTo(0f, height.toFloat())
                close()
            }
            canvas.drawPath(midPath, paint)

            // 5. Foreground Ridge & Lake/Valley
            paint.color = scene.mountainFront.toArgb()
            val frontPath = android.graphics.Path().apply {
                moveTo(0f, height * 0.72f)
                lineTo(width * 0.28f, height * 0.66f)
                lineTo(width * 0.50f, height * 0.74f)
                lineTo(width * 0.78f, height * 0.64f)
                lineTo(width.toFloat(), height * 0.70f)
                lineTo(width.toFloat(), height.toFloat())
                lineTo(0f, height.toFloat())
                close()
            }
            canvas.drawPath(frontPath, paint)

            // 6. Pine Trees on foreground
            fun drawPineTree(cx: Float, baseY: Float, treeH: Float) {
                val treeW = treeH * 0.42f
                val treePath = android.graphics.Path().apply {
                    moveTo(cx, baseY - treeH)
                    lineTo(cx + treeW * 0.5f, baseY - treeH * 0.55f)
                    lineTo(cx + treeW * 0.32f, baseY - treeH * 0.55f)
                    lineTo(cx + treeW * 0.75f, baseY - treeH * 0.22f)
                    lineTo(cx + treeW * 0.45f, baseY - treeH * 0.22f)
                    lineTo(cx + treeW, baseY)
                    lineTo(cx - treeW, baseY)
                    lineTo(cx - treeW * 0.45f, baseY - treeH * 0.22f)
                    lineTo(cx - treeW * 0.75f, baseY - treeH * 0.22f)
                    lineTo(cx - treeW * 0.32f, baseY - treeH * 0.55f)
                    lineTo(cx - treeW * 0.5f, baseY - treeH * 0.55f)
                    close()
                }
                canvas.drawPath(treePath, paint)
            }

            drawPineTree(width * 0.10f, height * 0.82f, height * 0.16f)
            drawPineTree(width * 0.18f, height * 0.85f, height * 0.13f)
            drawPineTree(width * 0.26f, height * 0.88f, height * 0.10f)
            drawPineTree(width * 0.72f, height * 0.84f, height * 0.14f)
            drawPineTree(width * 0.82f, height * 0.86f, height * 0.17f)
            drawPineTree(width * 0.92f, height * 0.89f, height * 0.12f)

            // 7. Ambient Mist at base
            val mistPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    0f, height * 0.65f, 0f, height.toFloat(),
                    intArrayOf(0x00FFFFFF, 0x25FFFFFF, 0x10FFFFFF),
                    null, android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, height * 0.65f, width.toFloat(), height.toFloat(), mistPaint)

            FeaturedPhoto(bitmap.asImageBitmap(), "Nature Wallpaper", scene.title)
        }
    }
}

private fun loadFeaturedPhoto(file: java.io.File): FeaturedPhoto? = runCatching {
    val bitmap = android.graphics.BitmapFactory.decodeFile(file.path) ?: return@runCatching null
    FeaturedPhoto(bitmap.asImageBitmap(), DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(file.lastModified())))
}.getOrNull()

private fun openPhotos(context: Context, uri: Uri?) {
    val googlePhotos = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos")
    when {
        uri != null && googlePhotos != null -> context.startSafely(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.photos").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) || context.startSafely(googlePhotos)
        uri != null -> context.startSafely(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        googlePhotos != null -> context.startSafely(googlePhotos)
    }
}
