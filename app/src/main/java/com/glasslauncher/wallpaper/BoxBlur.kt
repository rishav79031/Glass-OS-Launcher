package com.glasslauncher.wallpaper

import android.graphics.Bitmap

/**
 * Three-pass box blur (close to a Gaussian), in place. It runs once per wallpaper on a ~160px-wide copy,
 * so it costs a few milliseconds and the glass never has to blur live while you scroll.
 */
internal fun boxBlur(bitmap: Bitmap, radius: Int, passes: Int = 3) {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    val scratch = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    repeat(passes) {
        // Each call blurs rows and writes them transposed, so two calls cover both axes.
        blurRowsTransposed(pixels, scratch, width, height, radius)
        blurRowsTransposed(scratch, pixels, height, width, radius)
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
}

private fun blurRowsTransposed(input: IntArray, output: IntArray, width: Int, height: Int, radius: Int) {
    val window = radius * 2 + 1
    for (y in 0 until height) {
        val row = y * width
        var a = 0
        var r = 0
        var g = 0
        var b = 0
        for (i in -radius..radius) {
            val p = input[row + i.coerceIn(0, width - 1)]
            a += p ushr 24
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
        }
        for (x in 0 until width) {
            output[x * height + y] = ((a / window) shl 24) or ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
            val leaving = input[row + (x - radius).coerceAtLeast(0)]
            val entering = input[row + (x + radius + 1).coerceAtMost(width - 1)]
            a += (entering ushr 24) - (leaving ushr 24)
            r += ((entering shr 16) and 0xFF) - ((leaving shr 16) and 0xFF)
            g += ((entering shr 8) and 0xFF) - ((leaving shr 8) and 0xFF)
            b += (entering and 0xFF) - (leaving and 0xFF)
        }
    }
}
