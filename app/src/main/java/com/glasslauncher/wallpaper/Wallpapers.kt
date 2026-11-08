package com.glasslauncher.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.runtime.Immutable
import kotlin.math.max

const val PRESET_PREFIX = "preset:"
const val CUSTOM_PREFIX = "custom:"
const val SHUFFLE_PREFIX = "shuffle:"
const val COLLECTION_PREFIX = "collection:"
const val DEFAULT_WALLPAPER = PRESET_PREFIX + "liquid"

@Immutable
data class Blob(val x: Float, val y: Float, val radius: Float, val color: Int)

/** An original gradient wallpaper: a vertical base gradient with soft colour blobs on top. */
@Immutable
data class WallpaperPreset(val id: String, val name: String, val top: Int, val bottom: Int, val blobs: List<Blob>)

val WallpaperPresets = listOf(
    WallpaperPreset(
        "curves", "Curved Glass", 0xFFD8B4FE.toInt(), 0xFF6D28D9.toInt(),
        listOf(Blob(0.5f, 0.5f, 0.6f, 0xFFE9D5FF.toInt())),
    ),
    WallpaperPreset(
        "sunset_palms", "Sunset Palms", 0xFF3B0764.toInt(), 0xFFF97316.toInt(),
        listOf(Blob(0.5f, 0.5f, 0.6f, 0xFFA855F7.toInt())),
    ),
    WallpaperPreset(
        "spiderman", "Spider-Man 3D", 0xFFD50000.toInt(), 0xFF1A0000.toInt(),
        listOf(Blob(0.5f, 0.5f, 0.6f, 0xFFFF0033.toInt())),
    ),
    WallpaperPreset(
        "liquid", "Liquid", 0xFF0A1F5C.toInt(), 0xFF1B0F3B.toInt(),
        listOf(Blob(0.75f, 0.2f, 0.55f, 0xFF4FC3FF.toInt()), Blob(0.2f, 0.55f, 0.6f, 0xFF7B61FF.toInt()), Blob(0.7f, 0.9f, 0.5f, 0xFFE0F4FF.toInt())),
    ),
    WallpaperPreset(
        "solstice", "Solstice", 0xFF0A0F33.toInt(), 0xFF16092F.toInt(),
        listOf(Blob(0.75f, 0.25f, 0.65f, 0xFF3860FF.toInt()), Blob(0.2f, 0.55f, 0.60f, 0xFF8338EC.toInt()), Blob(0.7f, 0.85f, 0.50f, 0xFFFF006E.toInt())),
    ),
    WallpaperPreset(
        "aurora", "Aurora", 0xFF10123A.toInt(), 0xFF2A1458.toInt(),
        listOf(Blob(0.2f, 0.25f, 0.55f, 0xFF2EE6C5.toInt()), Blob(0.85f, 0.3f, 0.6f, 0xFF8B5CF6.toInt()), Blob(0.6f, 0.85f, 0.55f, 0xFFFF5FA2.toInt())),
    ),
    WallpaperPreset(
        "titanium", "Titanium", 0xFF22201E.toInt(), 0xFF0E0E10.toInt(),
        listOf(Blob(0.35f, 0.2f, 0.60f, 0xFF7A6855.toInt()), Blob(0.80f, 0.6f, 0.55f, 0xFF4A443D.toInt()), Blob(0.25f, 0.85f, 0.50f, 0xFF35343A.toInt())),
    ),
    WallpaperPreset(
        "ocean", "Ocean", 0xFF0B2545.toInt(), 0xFF13315C.toInt(),
        listOf(Blob(0.25f, 0.3f, 0.6f, 0xFF00C2FF.toInt()), Blob(0.8f, 0.7f, 0.65f, 0xFF3A86FF.toInt()), Blob(0.6f, 0.1f, 0.4f, 0xFF48E5C2.toInt())),
    ),
    WallpaperPreset(
        "sequoia", "Sequoia", 0xFF0D231E.toInt(), 0xFF06110F.toInt(),
        listOf(Blob(0.25f, 0.25f, 0.60f, 0xFF1B6A56.toInt()), Blob(0.80f, 0.45f, 0.55f, 0xFF4E9F76.toInt()), Blob(0.40f, 0.85f, 0.50f, 0xFFD8973C.toInt())),
    ),
    WallpaperPreset(
        "graphite", "Graphite", 0xFF1C1C1E.toInt(), 0xFF0A0A0C.toInt(),
        listOf(Blob(0.3f, 0.3f, 0.6f, 0xFF3A3A3C.toInt()), Blob(0.8f, 0.8f, 0.6f, 0xFF2C3E50.toInt()), Blob(0.7f, 0.2f, 0.4f, 0xFF4B4B55.toInt())),
    ),
    WallpaperPreset(
        "prism", "Prism", 0xFF0B0B14.toInt(), 0xFF050508.toInt(),
        listOf(Blob(0.30f, 0.20f, 0.55f, 0xFFFF007F.toInt()), Blob(0.80f, 0.50f, 0.60f, 0xFF00F0FF.toInt()), Blob(0.25f, 0.85f, 0.55f, 0xFF7000FF.toInt())),
    ),
)

fun presetFor(id: String): WallpaperPreset =
    WallpaperPresets.firstOrNull { PRESET_PREFIX + it.id == id } ?: WallpaperPresets.first()

fun renderPreset(preset: WallpaperPreset, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), preset.top, preset.bottom, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    val extent = max(width, height).toFloat()
    for (blob in preset.blobs) {
        val cx = blob.x * width
        val cy = blob.y * height
        val radius = blob.radius * extent
        val solid = blob.color
        val soft = Color.argb(0x66, Color.red(solid), Color.green(solid), Color.blue(solid))
        val clear = Color.argb(0, Color.red(solid), Color.green(solid), Color.blue(solid))
        paint.shader = RadialGradient(cx, cy, radius, intArrayOf(solid, soft, clear), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, paint)
    }
    return bitmap
}
