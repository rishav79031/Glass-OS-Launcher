package com.glasslauncher.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.glasslauncher.data.IconStyle
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Converts any app icon to an iOS 26 appearance. Each icon is rendered once to a bitmap and cached, so
 * scrolling never re-rasterises vectors. Safe to call from several threads: every call has its own canvas.
 */
class GlassIconRenderer(private val sizePx: Int) {
    private val size = sizePx.toFloat()

    /** [style] must already be resolved: [IconStyle.Automatic] is mapped to Default or Dark by the caller. */
    /** [fullBleed]: the drawable is already finished square artwork (an icon pack or your own image), so it fills the shape. */
    fun render(icon: Drawable, style: IconStyle, tint: Int, fullBleed: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val adaptive = icon as? AdaptiveIconDrawable
        val rim = when (style) {
            IconStyle.Automatic, IconStyle.Default -> {
                if (fullBleed && adaptive == null) icon.drawInset(canvas, inset = 0f) else drawDefault(canvas, icon, adaptive)
                removeFrame(bitmap)
                0f
            }
            IconStyle.Dark -> {
                if (fullBleed && adaptive == null) {
                    icon.drawInset(canvas, inset = 0f)
                } else {
                    drawDark(canvas, icon, adaptive)
                }
                0.55f
            }
            IconStyle.Clear -> {
                drawGlyph(canvas, icon, adaptive, backdrop = Color.argb(30, 255, 255, 255), glyph = Color.argb(240, 255, 255, 255))
                drawSheen(canvas)
                1f
            }
            IconStyle.Tinted -> {
                drawTintedBackdrop(canvas, tint)
                drawGlyph(canvas, icon, adaptive, backdrop = Color.TRANSPARENT, glyph = lighten(tint))
                0.7f
            }
            IconStyle.Glass -> {
                drawColourGlass(canvas, icon, adaptive)
                drawSheen(canvas)
                1f
            }
        }
        clearOutsideSquircle(canvas)
        drawRim(canvas, rim)
        return bitmap
    }

    private fun drawDefault(canvas: Canvas, icon: Drawable, adaptive: AdaptiveIconDrawable?) {
        if (adaptive != null) {
            adaptive.background?.drawLayer(canvas)
            adaptive.foreground?.drawLayer(canvas)
        } else {
            drawTrimmed(canvas, icon)
        }
    }

    /**
     * Icons that come pre-shaped (Samsung's are) have transparent margins and corners. Instead of setting them on a
     * white square, crop to the visible artwork, stretch it to fill the shape, and paint any leftover corners in
     * the icon's own edge colour — so there's no white patch around it.
     */
    private fun drawTrimmed(canvas: Canvas, icon: Drawable) {
        val source = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        icon.mutate().apply { setBounds(0, 0, sizePx, sizePx) }.draw(Canvas(source))
        var left = sizePx
        var top = sizePx
        var right = -1
        var bottom = -1
        val pixels = IntArray(sizePx * sizePx)
        source.getPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        for (y in 0 until sizePx) for (x in 0 until sizePx) {
            if (Color.alpha(pixels[y * sizePx + x]) > 24) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        if (right < left || bottom < top) return
        val crop = android.graphics.Rect(left, top, right + 1, bottom + 1)
        // Fill the shape's corners only when the artwork is a solid square-ish plate (all four edge midpoints opaque
        // and the same colour), so the icon fills the square edge to edge. Anything else (round icons, see-through
        // artwork) keeps a transparent surround instead of a dark or wrong-coloured patch.
        val inset = ((right - left) * 0.06f).toInt().coerceAtLeast(1)
        val edges = listOf(
            source.getPixel((left + right) / 2, (top + inset).coerceAtMost(bottom)),
            source.getPixel((left + right) / 2, (bottom - inset).coerceAtLeast(top)),
            source.getPixel((left + inset).coerceAtMost(right), (top + bottom) / 2),
            source.getPixel((right - inset).coerceAtLeast(left), (top + bottom) / 2),
        )
        val solid = edges.all { Color.alpha(it) > 230 } && edges.all { colourDistance(it, edges[0]) < 40 }
        val target = if (solid) {
            canvas.drawColor(edges[0] or 0xFF000000.toInt())
            android.graphics.RectF(0f, 0f, size, size)
        } else {
            // Fit the artwork inside the square, keeping its proportions, on a clear background.
            val scale = minOf(size / crop.width(), size / crop.height())
            val w = crop.width() * scale
            val h = crop.height() * scale
            android.graphics.RectF((size - w) / 2f, (size - h) / 2f, (size + w) / 2f, (size + h) / 2f)
        }
        canvas.drawBitmap(source, crop, target, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        source.recycle()
    }

    /**
     * Some apps draw their own rounded plate on a black or white square, which shows as a frame around the icon.
     * When the border is one colour and the inner ring is a different solid colour, the border is cleared away and
     * the plate is enlarged to fill the icon (whatever corner is left stays transparent).
     */
    private fun removeFrame(bitmap: Bitmap) {
        val n = bitmap.width
        val px = IntArray(n * n)
        bitmap.getPixels(px, 0, n, 0, 0, n, n)
        fun at(x: Int, y: Int) = px[y * n + x]
        val e = 1
        val mid = n / 2
        val border = listOf(at(mid, e), at(mid, n - 1 - e), at(e, mid), at(n - 1 - e, mid))
        val i = (n * 0.16f).toInt()
        val inner = listOf(at(mid, i), at(mid, n - 1 - i), at(i, mid), at(n - 1 - i, mid))
        val borderSame = border.all { Color.alpha(it) > 200 && colourDistance(it, border[0]) < 30 }
        val innerSame = inner.all { Color.alpha(it) > 200 && colourDistance(it, inner[0]) < 60 }
        if (!borderSame || !innerSame || colourDistance(border[0], inner[0]) < 90) return
        // Flood-fill the frame colour inward from the edges, making it transparent.
        val frame = border[0]
        val seen = BooleanArray(n * n)
        val queue = ArrayDeque<Int>()
        for (k in 0 until n) for (idx in intArrayOf(k, (n - 1) * n + k, k * n, k * n + n - 1)) if (!seen[idx]) { seen[idx] = true; queue.add(idx) }
        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            val c = px[idx]
            if (Color.alpha(c) > 24 && colourDistance(c, frame) > 60) continue
            px[idx] = Color.TRANSPARENT
            val x = idx % n
            val y = idx / n
            if (x > 0 && !seen[idx - 1]) { seen[idx - 1] = true; queue.add(idx - 1) }
            if (x < n - 1 && !seen[idx + 1]) { seen[idx + 1] = true; queue.add(idx + 1) }
            if (y > 0 && !seen[idx - n]) { seen[idx - n] = true; queue.add(idx - n) }
            if (y < n - 1 && !seen[idx + n]) { seen[idx + n] = true; queue.add(idx + n) }
        }
        var left = n; var top = n; var right = -1; var bottom = -1
        for (y in 0 until n) for (x in 0 until n) if (Color.alpha(px[y * n + x]) > 24) {
            if (x < left) left = x; if (x > right) right = x; if (y < top) top = y; if (y > bottom) bottom = y
        }
        if (right <= left || bottom <= top) return
        val plate = Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        Canvas(bitmap).drawBitmap(plate, android.graphics.Rect(left, top, right + 1, bottom + 1), android.graphics.RectF(0f, 0f, n.toFloat(), n.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        plate.recycle()
    }

    private fun colourDistance(a: Int, b: Int): Int =
        kotlin.math.abs(Color.red(a) - Color.red(b)) + kotlin.math.abs(Color.green(a) - Color.green(b)) + kotlin.math.abs(Color.blue(a) - Color.blue(b))

    // iOS dark icons: the artwork's own background is swapped for a near-black gradient.
    private fun drawDark(canvas: Canvas, icon: Drawable, adaptive: AdaptiveIconDrawable?) {
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, size, Color.rgb(0x2C, 0x2C, 0x2E), Color.rgb(0x14, 0x14, 0x16), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, size, size, paint)
        val foreground = adaptive?.foreground
        when {
            foreground != null -> {
                val lum = averageLuminance(foreground)
                if (lum < 0.28f) {
                    val invertFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                        setSaturation(0.2f)
                        postConcat(ColorMatrix(floatArrayOf(
                            -1f, 0f, 0f, 0f, 245f,
                            0f, -1f, 0f, 0f, 245f,
                            0f, 0f, -1f, 0f, 245f,
                            0f, 0f, 0f, 1f, 0f,
                        )))
                    })
                    foreground.drawLayer(canvas, filter = invertFilter)
                } else {
                    foreground.drawLayer(canvas)
                }
            }
            else -> {
                drawDarkTrimmed(canvas, icon)
            }
        }
    }

    private fun isDarkPixel(p: Int): Boolean =
        Color.alpha(p) > 200 && Color.red(p) < 45 && Color.green(p) < 45 && Color.blue(p) < 45

    private fun drawDarkTrimmed(canvas: Canvas, icon: Drawable) {
        val source = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        icon.mutate().apply { setBounds(0, 0, sizePx, sizePx) }.draw(Canvas(source))
        // If the icon is already dark-backed (like iOS dark icons or dark third-party icons), draw it directly
        val p0 = source.getPixel(0, 0)
        val pBottom = source.getPixel(sizePx / 2, sizePx - 4)
        val pBottomLeft = source.getPixel(4, sizePx - 4)
        val pBottomRight = source.getPixel(sizePx - 5, sizePx - 4)
        val isAlreadyDark = isDarkPixel(p0) || isDarkPixel(pBottom) || isDarkPixel(pBottomLeft) || isDarkPixel(pBottomRight)
        if (isAlreadyDark) {
            canvas.drawBitmap(source, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            source.recycle()
            return
        }
        removeFrame(source)
        var left = sizePx; var top = sizePx; var right = -1; var bottom = -1
        val pixels = IntArray(sizePx * sizePx)
        source.getPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        val bgSample = pixels[0]
        for (y in 0 until sizePx) for (x in 0 until sizePx) {
            val p = pixels[y * sizePx + x]
            if (Color.alpha(p) > 24) {
                val isBg = (colourDistance(p, bgSample) < 30) || (Color.red(p) > 235 && Color.green(p) > 235 && Color.blue(p) > 235)
                if (!isBg) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        val lum = averageLuminance(icon)
        val darkPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            if (lum < 0.28f) {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    setSaturation(0.2f)
                    postConcat(ColorMatrix(floatArrayOf(
                        -1f, 0f, 0f, 0f, 245f,
                        0f, -1f, 0f, 0f, 245f,
                        0f, 0f, -1f, 0f, 245f,
                        0f, 0f, 0f, 1f, 0f,
                    )))
                })
            }
        }
        if (right <= left || bottom <= top) {
            val scale = 0.72f
            val w = size * scale
            val h = size * scale
            val target = android.graphics.RectF((size - w) / 2f, (size - h) / 2f, (size + w) / 2f, (size + h) / 2f)
            canvas.drawBitmap(source, null, target, darkPaint)
            source.recycle()
            return
        }
        val crop = android.graphics.Rect(left, top, right + 1, bottom + 1)
        val scale = minOf((size * 0.72f) / crop.width(), (size * 0.72f) / crop.height())
        val w = crop.width() * scale
        val h = crop.height() * scale
        val target = android.graphics.RectF((size - w) / 2f, (size - h) / 2f, (size + w) / 2f, (size + h) / 2f)
        canvas.drawBitmap(source, crop, target, darkPaint)
        source.recycle()
    }

    private fun drawTintedBackdrop(canvas: Canvas, tint: Int) {
        val top = blend(tint, Color.BLACK, 0.72f)
        val bottom = blend(tint, Color.BLACK, 0.86f)
        val paint = Paint().apply { shader = LinearGradient(0f, 0f, 0f, size, top, bottom, Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, size, size, paint)
    }

    /** One-colour glyph: the monochrome layer when the app ships one, otherwise its artwork turned to a single tone. */
    private fun drawGlyph(canvas: Canvas, icon: Drawable, adaptive: AdaptiveIconDrawable?, backdrop: Int, glyph: Int) {
        if (Color.alpha(backdrop) > 0) canvas.drawColor(backdrop)
        val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) adaptive?.monochrome else null
        val foreground = adaptive?.foreground
        when {
            monochrome != null -> monochrome.drawLayer(canvas, filter = PorterDuffColorFilter(glyph, PorterDuff.Mode.SRC_IN))
            foreground != null -> {
                val lum = averageLuminance(foreground)
                foreground.drawLayer(canvas, filter = toneFilter(glyph, invert = lum < 0.42f))
            }
            else -> {
                val lum = averageLuminance(icon)
                icon.drawInset(canvas, inset = 0.16f, filter = toneFilter(glyph, invert = lum < 0.42f))
            }
        }
    }

    private fun drawColourGlass(canvas: Canvas, icon: Drawable, adaptive: AdaptiveIconDrawable?) {
        canvas.drawColor(Color.argb(36, 255, 255, 255))
        if (adaptive != null) {
            // The brand colour becomes tinted glass; the glyph stays fully opaque on top.
            adaptive.background?.drawLayer(canvas, alpha = 110)
            adaptive.foreground?.drawLayer(canvas)
        } else {
            canvas.drawColor(withAlpha(averageColor(icon), 90))
            icon.drawInset(canvas, inset = 0.16f)
        }
    }

    private fun drawSheen(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, size * 0.55f, Color.argb(64, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, size, size * 0.55f, paint)
    }

    private fun clearOutsideSquircle(canvas: Canvas) {
        val outside = squirclePath(size, size).apply { fillType = Path.FillType.INVERSE_WINDING }
        canvas.drawPath(outside, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
    }

    // Specular edge: bright top-left, faint middle, softer bottom-right — the Liquid Glass rim.
    private fun drawRim(canvas: Canvas, strength: Float) {
        val stroke = max(1.5f, size * 0.022f)
        fun white(alpha: Int) = Color.argb((alpha * strength).roundToInt(), 255, 255, 255)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            shader = LinearGradient(
                0f, 0f, size, size,
                intArrayOf(white(210), white(30), white(120)),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(squirclePath(size, size, inset = stroke / 2f), paint)
    }

    // Adaptive layers are 108dp with the visible icon in the middle 72dp, so they bleed a quarter past each edge.
    private fun Drawable.drawLayer(canvas: Canvas, alpha: Int = 255, filter: ColorFilter? = null) {
        val layer = mutate()
        val bleed = sizePx / 4
        layer.setBounds(-bleed, -bleed, sizePx + bleed, sizePx + bleed)
        layer.alpha = alpha
        layer.colorFilter = filter
        layer.draw(canvas)
    }

    private fun Drawable.drawInset(canvas: Canvas, inset: Float, filter: ColorFilter? = null) {
        val layer = mutate()
        val px = (sizePx * inset).roundToInt()
        layer.setBounds(px, px, sizePx - px, sizePx - px)
        layer.colorFilter = filter
        layer.draw(canvas)
    }

    private fun averageColor(icon: Drawable): Int {
        val sample = Bitmap.createBitmap(SAMPLE, SAMPLE, Bitmap.Config.ARGB_8888)
        icon.mutate().apply { setBounds(0, 0, SAMPLE, SAMPLE) }.draw(Canvas(sample))
        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0
        for (x in 0 until SAMPLE) for (y in 0 until SAMPLE) {
            val p = sample.getPixel(x, y)
            if (Color.alpha(p) < 128) continue
            r += Color.red(p)
            g += Color.green(p)
            b += Color.blue(p)
            count++
        }
        sample.recycle()
        if (count == 0) return Color.GRAY
        return Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
    }

    private fun averageLuminance(drawable: Drawable): Float {
        val sample = Bitmap.createBitmap(SAMPLE, SAMPLE, Bitmap.Config.ARGB_8888)
        drawable.mutate().apply { setBounds(0, 0, SAMPLE, SAMPLE) }.draw(Canvas(sample))
        var sumLum = 0f
        var count = 0
        for (x in 0 until SAMPLE) for (y in 0 until SAMPLE) {
            val p = sample.getPixel(x, y)
            if (Color.alpha(p) > 30) {
                val lum = 0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)
                sumLum += lum
                count++
            }
        }
        sample.recycle()
        return if (count > 0) (sumLum / count) / 255f else 0.5f
    }

    private companion object {
        const val SAMPLE = 16

        fun withAlpha(color: Int, alpha: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

        fun blend(from: Int, to: Int, amount: Float): Int = Color.rgb(
            (Color.red(from) + (Color.red(to) - Color.red(from)) * amount).roundToInt(),
            (Color.green(from) + (Color.green(to) - Color.green(from)) * amount).roundToInt(),
            (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount).roundToInt(),
        )

        fun lighten(color: Int): Int = blend(color, Color.WHITE, 0.22f)

        // Greyscale, then mapped onto target colour. If invert is true (for dark glyphs), dark pixels become bright tinted highlights!
        fun toneFilter(target: Int, invert: Boolean = false): ColorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0f)
            if (invert) {
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            -1f,  0f,  0f,  0f, 255f,
                             0f, -1f,  0f,  0f, 255f,
                             0f,  0f, -1f,  0f, 255f,
                             0f,  0f,  0f,  1f,   0f,
                        )
                    )
                )
            }
            val r = (Color.red(target) / 255f * 1.35f).coerceAtMost(1.6f)
            val g = (Color.green(target) / 255f * 1.35f).coerceAtMost(1.6f)
            val b = (Color.blue(target) / 255f * 1.35f).coerceAtMost(1.6f)
            val a = Color.alpha(target) / 255f
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        r, 0f, 0f, 0f, 32f,
                        0f, g, 0f, 0f, 32f,
                        0f, 0f, b, 0f, 32f,
                        0f, 0f, 0f, a, 0f,
                    ),
                ),
            )
        })
    }
}
