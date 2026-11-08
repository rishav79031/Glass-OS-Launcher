package com.glasslauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.glasslauncher.data.HomeWallpaperMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The Home Screen wallpaper as drawn, plus two small pre-blurred copies that glass surfaces sample from:
 * a light one for clear glass and a heavy one for frosted glass and the App Library background.
 */
@Immutable
class WallpaperImages(val sharp: ImageBitmap, val light: ImageBitmap, val heavy: ImageBitmap, val isLight: Boolean)

/**
 * The launcher draws its own wallpaper (Android doesn't let apps read the system one), which is what lets
 * glass blur exactly what's behind it. Wallpaper ids: "preset:<id>", "collection:<file>", "custom:<stamp>",
 * "shuffle:<stamp>".
 */
class WallpaperRepository(private val context: Context) {
    private val customFile = File(context.filesDir, "custom_wallpaper.jpg")
    private val shuffleDir = File(context.filesDir, "shuffle")
    private val collectionDir = File(context.filesDir, "collection")

    /** The longest side of this device's screen: images are kept at exactly this, e.g. 2560 px on the Tab S9 FE+. */
    private val screenSide: Int
        get() = context.resources.displayMetrics.let { max(it.widthPixels, it.heightPixels) }

    suspend fun load(id: String, mode: HomeWallpaperMode, shuffleIndex: Int): WallpaperImages = withContext(Dispatchers.Default) {
        val original = source(id, shuffleIndex, fullResolution = false)
        val sharp = applyHomeMode(original, mode)
        val light = blurForGlass(sharp, radius = 2)
        val heavy = blurForGlass(sharp, radius = 6)
        WallpaperImages(sharp.asImageBitmap(), light.asImageBitmap(), heavy.asImageBitmap(), isLight = averageLuminance(heavy) > 0.62f)
    }

    /** Copies a picked photo into app storage, scaled down, so it survives the picker's temporary grant. */
    suspend fun saveCustom(uri: Uri): Boolean = withContext(Dispatchers.IO) { savePhoto(uri, customFile) }

    /** My Collection: the user's own wallpapers, kept at screen resolution and shown beside the built-in ones. */
    suspend fun addToCollection(uris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        collectionDir.mkdirs()
        val stamp = System.currentTimeMillis()
        uris.mapIndexedNotNull { index, uri ->
            val name = "${stamp}_$index.jpg"
            if (savePhoto(uri, File(collectionDir, name))) name else null
        }
    }

    fun collection(): List<String> = collectionDir.listFiles()?.filter { it.extension == "jpg" }?.map { it.name }?.sorted().orEmpty()

    suspend fun removeFromCollection(name: String) = withContext(Dispatchers.IO) { File(collectionDir, File(name).name).delete() }

    suspend fun collectionThumbnail(name: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val file = File(collectionDir, File(name).name)
        if (!file.exists()) return@withContext null
        val options = BitmapFactory.Options().apply { inSampleSize = 8 }
        BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
    }

    /** Photo Shuffle: keeps up to [MAX_SHUFFLE] photos; a new one shows each time the device is unlocked. */
    suspend fun saveShuffle(uris: List<Uri>): Boolean = withContext(Dispatchers.IO) {
        shuffleDir.deleteRecursively()
        shuffleDir.mkdirs()
        uris.take(MAX_SHUFFLE).mapIndexed { index, uri -> savePhoto(uri, File(shuffleDir, "$index.jpg")) }.any { it }
    }

    /** Sets the original wallpaper (not the Home Screen variant) for the system Home and Lock Screen, like iOS pairs. */
    suspend fun applyAsSystemWallpaper(id: String, shuffleIndex: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val manager = WallpaperManager.getInstance(context)
            val bitmap = source(id, shuffleIndex, fullResolution = true) ?: return@runCatching false
            var success = false
            try {
                manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                success = true
            } catch (_: Exception) { }
            if (!success) {
                try {
                    manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    success = true
                } catch (_: Exception) { }
            }
            success
        }.getOrDefault(false)
    }

    suspend fun applyAsLockOnly(id: String, shuffleIndex: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val manager = WallpaperManager.getInstance(context)
            val bitmap = source(id, shuffleIndex, fullResolution = true)
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            true
        }.getOrDefault(false)
    }

    suspend fun applyAsHomeOnly(id: String, shuffleIndex: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val manager = WallpaperManager.getInstance(context)
            val bitmap = source(id, shuffleIndex, fullResolution = true)
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            true
        }.getOrDefault(false)
    }

    suspend fun thumbnail(preset: WallpaperPreset): ImageBitmap = withContext(Dispatchers.Default) {
        if (preset.id == "spiderman" || preset.id == "curves" || preset.id == "sunset_palms") {
            val resId = when (preset.id) {
                "curves" -> com.glasslauncher.R.drawable.wallpaper_curves
                "sunset_palms" -> com.glasslauncher.R.drawable.wallpaper_sunset_palms
                else -> com.glasslauncher.R.drawable.wallpaper_spiderman
            }
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeResource(context.resources, resId, opts)?.asImageBitmap()
            }.getOrNull() ?: renderPreset(preset, 144, 208).asImageBitmap()
        } else {
            renderPreset(preset, 144, 208).asImageBitmap()
        }
    }

    private fun source(id: String, shuffleIndex: Int, fullResolution: Boolean): Bitmap {
        val metrics = context.resources.displayMetrics
        // Square, so one bitmap covers both orientations. Generated art is smooth, so half resolution looks the same.
        val side = max(metrics.widthPixels, metrics.heightPixels) / (if (fullResolution) 1 else 2)
        return when {
            id == "preset:spiderman" || id == "spiderman" -> runCatching {
                BitmapFactory.decodeResource(context.resources, com.glasslauncher.R.drawable.wallpaper_spiderman)
            }.getOrNull()
            id == "preset:curves" || id == "curves" -> runCatching {
                BitmapFactory.decodeResource(context.resources, com.glasslauncher.R.drawable.wallpaper_curves)
            }.getOrNull()
            id == "preset:sunset_palms" || id == "sunset_palms" -> runCatching {
                BitmapFactory.decodeResource(context.resources, com.glasslauncher.R.drawable.wallpaper_sunset_palms)
            }.getOrNull()
            id.startsWith(CUSTOM_PREFIX) -> decode(customFile)
            id.startsWith(SHUFFLE_PREFIX) -> shuffleFiles().let { files -> if (files.isEmpty()) null else decode(files[Math.floorMod(shuffleIndex, files.size)]) }
            // File(..).name keeps an id from pointing outside the collection folder.
            id.startsWith(COLLECTION_PREFIX) -> decode(File(collectionDir, File(id.removePrefix(COLLECTION_PREFIX)).name))
            else -> null
        } ?: renderPreset(presetFor(id), side, side)
    }

    private fun shuffleFiles(): List<File> = shuffleDir.listFiles()?.filter { it.extension == "jpg" }?.sortedBy { it.name }.orEmpty()

    private fun decode(file: File): Bitmap? = if (file.exists()) BitmapFactory.decodeFile(file.path) else null

    private fun savePhoto(uri: Uri, target: File): Boolean = runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > screenSide) {
                val scale = screenSide / longest.toFloat()
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
        }
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    }.getOrDefault(false)

    // iOS Home Screen options for a paired wallpaper: as is, blurred, or reduced to its main colour.
    private fun applyHomeMode(source: Bitmap, mode: HomeWallpaperMode): Bitmap = when (mode) {
        HomeWallpaperMode.Original -> source
        HomeWallpaperMode.Blur -> blurForGlass(source, radius = 5)
        HomeWallpaperMode.Colour -> Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(averageColor(source)) }
        HomeWallpaperMode.Gradient -> {
            val colour = averageColor(source)
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).also {
                val paint = Paint().apply {
                    shader = LinearGradient(0f, 0f, 0f, 64f, blend(colour, Color.WHITE, 0.2f), blend(colour, Color.BLACK, 0.4f), Shader.TileMode.CLAMP)
                }
                Canvas(it).drawRect(0f, 0f, 64f, 64f, paint)
            }
        }
    }

    private fun blurForGlass(source: Bitmap, radius: Int): Bitmap {
        val height = max(1, (source.height * BLUR_WIDTH / source.width.toFloat()).roundToInt())
        val small = Bitmap.createBitmap(BLUR_WIDTH, height, Bitmap.Config.ARGB_8888)
        // A little extra saturation makes the glass read livelier, like iOS materials.
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.4f) })
        }
        Canvas(small).drawBitmap(source, null, Rect(0, 0, BLUR_WIDTH, height), paint)
        boxBlur(small, radius)
        return small
    }

    private fun averageColor(bitmap: Bitmap): Int {
        val tiny = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        return tiny.getPixel(0, 0) or 0xFF000000.toInt()
    }

    private fun averageLuminance(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val total = pixels.sumOf { p -> (0.2126 * Color.red(p) + 0.7152 * Color.green(p) + 0.0722 * Color.blue(p)) / 255.0 }
        return (total / pixels.size).toFloat()
    }

    private fun blend(from: Int, to: Int, amount: Float): Int = Color.rgb(
        (Color.red(from) + (Color.red(to) - Color.red(from)) * amount).roundToInt(),
        (Color.green(from) + (Color.green(to) - Color.green(from)) * amount).roundToInt(),
        (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount).roundToInt(),
    )

    private companion object {
        const val BLUR_WIDTH = 160
        const val MAX_SHUFFLE = 20
    }
}
