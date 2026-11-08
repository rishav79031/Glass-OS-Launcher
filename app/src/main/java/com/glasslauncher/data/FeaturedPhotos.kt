package com.glasslauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Photos for the Photos widget, chosen with Android's photo picker. The launcher keeps small copies of only the
 * photos you pick, so it needs no photo-library permission at all.
 */
class FeaturedPhotos(private val context: Context) {
    private val dir = File(context.filesDir, "featured")
    private val state = MutableStateFlow(list())

    /** File names of the chosen photos; the widget shows one per half hour. */
    val photos: StateFlow<List<String>> = state.asStateFlow()

    fun file(name: String): File = File(dir, File(name).name)

    suspend fun replace(uris: List<Uri>) = withContext(Dispatchers.IO) {
        dir.deleteRecursively()
        dir.mkdirs()
        uris.take(MAX_PHOTOS).forEachIndexed { index, uri ->
            runCatching {
                val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val longest = max(info.size.width, info.size.height)
                    if (longest > MAX_SIDE) {
                        val scale = MAX_SIDE / longest.toFloat()
                        decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                    }
                }
                File(dir, "$index.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            }
        }
        state.value = list()
    }

    suspend fun remove(name: String) = withContext(Dispatchers.IO) {
        runCatching { file(name).delete() }
        state.value = list()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dir.deleteRecursively()
        dir.mkdirs()
        state.value = emptyList()
    }

    private fun list(): List<String> = dir.listFiles()?.filter { it.extension == "jpg" }?.map { it.name }?.sorted().orEmpty()

    private companion object {
        const val MAX_PHOTOS = 30
        const val MAX_SIDE = 1200
    }
}
