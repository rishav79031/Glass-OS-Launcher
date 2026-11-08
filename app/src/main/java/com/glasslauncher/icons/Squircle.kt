package com.glasslauncher.icons

import android.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val EXPONENT = 3.6

/** iOS app icons: continuous corners whose nominal radius is 22.37% of the side — rounded, but naturally square. */
private const val ICON_RADIUS = 0.2237f

/**
 * The iOS app-icon outline. Each corner is a quarter superellipse spanning 1.7x the nominal radius, which keeps the
 * visual weight of that radius while easing into the straight edges instead of meeting them abruptly.
 */
fun squirclePath(width: Float, height: Float, inset: Float = 0f): Path {
    val w = width - inset * 2f
    val h = height - inset * 2f
    val e = min(min(w, h) * ICON_RADIUS * 1.7f, min(w, h) / 2f)
    val path = Path()
    val steps = 16
    fun corner(cx: Float, cy: Float, sx: Float, sy: Float, fromVertical: Boolean) {
        for (i in 0..steps) {
            val progress = i / steps.toFloat()
            val t = (PI / 2.0) * (if (fromVertical) 1.0 - progress else progress.toDouble())
            val x = e * cos(t).pow(2.0 / EXPONENT).toFloat()
            val y = e * sin(t).pow(2.0 / EXPONENT).toFloat()
            path.lineTo(inset + cx + sx * x, inset + cy + sy * y)
        }
    }
    path.moveTo(inset + e, inset)
    path.lineTo(inset + w - e, inset)
    corner(w - e, e, 1f, -1f, fromVertical = true)
    path.lineTo(inset + w, inset + h - e)
    corner(w - e, h - e, 1f, 1f, fromVertical = false)
    path.lineTo(inset + e, inset + h)
    corner(e, h - e, -1f, 1f, fromVertical = true)
    path.lineTo(inset, inset + e)
    corner(e, e, -1f, -1f, fromVertical = false)
    path.close()
    return path
}

object Squircle : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(squirclePath(size.width, size.height).asComposePath())
}
