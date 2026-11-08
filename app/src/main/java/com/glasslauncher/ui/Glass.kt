package com.glasslauncher.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.glasslauncher.data.GlassLook
import com.glasslauncher.data.IconStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Rounded rectangle with continuous ("squircle") corners like iOS: the curve eases into the straight edges
 * instead of meeting them abruptly, so cards read as soft and rounded rather than boxy.
 * Give either a fixed [radius] or a [fraction] of the shorter side; 0.5 is a capsule.
 */
@Immutable
class SmoothShape(private val radius: Dp? = null, private val fraction: Float = 0f) : Shape {
    fun radiusPx(size: Size, density: Density): Float {
        val r = radius?.let { with(density) { it.toPx() } } ?: (size.minDimension * fraction)
        return r.coerceAtMost(size.minDimension / 2f)
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = radiusPx(size, density)
        // A capsule keeps perfectly round ends, like iOS buttons and the Search pill.
        if (r >= size.minDimension / 2f - 0.5f) {
            return Outline.Rounded(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, r, r))
        }
        return Outline.Generic(continuousRect(size.width, size.height, r))
    }
}

val Capsule = SmoothShape(fraction = 0.5f)

// Each corner is a quarter superellipse spanning 1.7× the nominal radius, which matches the visual weight of
// a circular corner of that radius while blending smoothly into the edges.
private fun continuousRect(w: Float, h: Float, r: Float): Path {
    val e = min(r * 1.7f, min(w, h) / 2f)
    val path = Path()
    val steps = 14
    fun corner(cx: Float, cy: Float, sx: Float, sy: Float, fromVertical: Boolean) {
        for (i in 0..steps) {
            val progress = i / steps.toFloat()
            val t = (PI / 2.0) * (if (fromVertical) 1.0 - progress else progress.toDouble())
            val x = e * cos(t).pow(2.0 / EXPONENT).toFloat()
            val y = e * sin(t).pow(2.0 / EXPONENT).toFloat()
            path.lineTo(cx + sx * x, cy + sy * y)
        }
    }
    path.moveTo(e, 0f)
    path.lineTo(w - e, 0f)
    corner(w - e, e, 1f, -1f, fromVertical = true)
    path.lineTo(w, h - e)
    corner(w - e, h - e, 1f, 1f, fromVertical = false)
    path.lineTo(e, h)
    corner(e, h - e, -1f, 1f, fromVertical = true)
    path.lineTo(0f, e)
    corner(e, e, -1f, -1f, fromVertical = false)
    path.close()
    return path
}

private const val EXPONENT = 3.6

/** The wallpaper as drawn (sharp plus two pre-blurred copies) and where it sits, shared with every glass surface. */
@Immutable
class GlassBackdrop(val sharp: ImageBitmap, val light: ImageBitmap, val heavy: ImageBitmap, rootSize: IntSize, zoom: Float) {
    val sharpRect: IntRect = centerCrop(sharp.width, sharp.height, rootSize, zoom)
    val lightRect: IntRect = centerCrop(light.width, light.height, rootSize, zoom)
    val heavyRect: IntRect = centerCrop(heavy.width, heavy.height, rootSize, zoom)
}

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** Perspective Zoom: how far the wallpaper is currently shifted by device tilt. Read only while drawing. */
val LocalWallpaperShift = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

/** The user's Liquid Glass preferences: Clear, Tinted or Dark, and how frosted widgets and the Dock are. */
@Immutable
data class GlassTuning(
    val look: GlassLook = GlassLook.Clear,
    val widgetFrost: Float = 0.45f,
    val dockFrost: Float = 0.3f,
    val iconStyle: IconStyle = IconStyle.Default,
    val tintColor: Int = 0xFF007AFF.toInt(),
)

val LocalGlassTuning = staticCompositionLocalOf { GlassTuning() }

/**
 * True while the Home Screen is moving (page swipe, icon drag). The lens shader pauses then: refraction isn't
 * visible in motion, and skipping it keeps each frame well inside a 90/120 Hz budget.
 */
val LocalGlassMotion = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(false) }

/**
 * Progress of the unlock entrance animation (0.05f to 1f).
 * Used by GridPage to drive the staggered row fly-in cascade without recomposing.
 */
val LocalUnlockProgress = staticCompositionLocalOf<State<Float>> { mutableStateOf(1f) }

/** Scale applied to glass drawn inside a scaled-down widget, so its wallpaper slice still lines up with the screen. */
val LocalGlassScale = staticCompositionLocalOf { 1f }

/**
 * Bar is the Dock and Search pill, Panel is widgets, menus and folders, Overlay sits over dimmed content.
 * How clear or frosted each one is comes from [GlassTuning].
 */
enum class GlassStyle { Clear, Regular, Dark }

fun centerCrop(imageWidth: Int, imageHeight: Int, container: IntSize, zoom: Float = 1f): IntRect {
    if (imageWidth == 0 || imageHeight == 0 || container == IntSize.Zero) return IntRect.Zero
    val scale = max(container.width / imageWidth.toFloat(), container.height / imageHeight.toFloat()) * zoom
    val width = (imageWidth * scale).roundToInt()
    val height = (imageHeight * scale).roundToInt()
    val left = (container.width - width) / 2
    val top = (container.height - height) / 2
    return IntRect(left, top, left + width, top + height)
}

/**
 * Liquid Glass: the slice of wallpaper behind this element (sharp, lightly or heavily blurred, depending on the
 * frost setting), bent at the rim by a lens shader on Android 13+, tinted, with a specular edge. The blurs are
 * computed once per wallpaper, so each frame costs one small bitmap draw plus a cheap shader.
 */
fun Modifier.liquidGlass(shape: Shape, cornerRadius: Dp? = null, style: GlassStyle = GlassStyle.Clear): Modifier = composed {
    val backdrop = LocalGlassBackdrop.current
    val shift = LocalWallpaperShift.current
    val tuning = LocalGlassTuning.current
    val motion = LocalGlassMotion.current
    // Read only while drawing, so moving (e.g. pager scroll) redraws without recomposing.
    var origin by remember { mutableStateOf(Offset.Zero) }
    val glassScale = LocalGlassScale.current
    val layer = rememberGraphicsLayer()
    val lens = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) LensEffect() else null }
    val density = LocalDensity.current
    val fixedRadius = cornerRadius?.let { with(density) { it.toPx() } }
    val bandPx = with(density) { LENS_BAND.toPx() }
    val rimPx = with(density) { 1.dp.toPx() }
    val isDarkTheme = tuning.iconStyle == IconStyle.Dark
    val isTintedTheme = tuning.iconStyle == IconStyle.Tinted
    val tintColor = Color(tuning.tintColor)
    val frost = when (style) {
        GlassStyle.Clear -> tuning.dockFrost
        GlassStyle.Regular -> tuning.widgetFrost
        GlassStyle.Dark -> max(tuning.widgetFrost, 0.5f)
    }
    val tint = when {
        isDarkTheme -> Color(0xFF101014).copy(alpha = 0.62f + frost * 0.22f)
        isTintedTheme -> tintColor.copy(alpha = 0.26f + frost * 0.16f)
        tuning.look == GlassLook.Tinted -> Color.Black.copy(alpha = 0.20f + frost * 0.28f)
        style == GlassStyle.Dark -> Color.Black.copy(alpha = 0.16f + frost * 0.2f)
        // Clear at the lowest setting (only the rim shows), turning milky white as frost rises.
        else -> Color.White.copy(alpha = frost * 0.3f)
    }

    Modifier
        .then(if (backdrop != null) Modifier.onGloballyPositioned { origin = it.positionInRoot() } else Modifier)
        .clip(shape)
        .drawWithContent {
            if (backdrop != null) {
                val (image, dst) = when {
                    frost < 0.12f -> backdrop.sharp to backdrop.sharpRect
                    frost < 0.55f -> backdrop.light to backdrop.lightRect
                    else -> backdrop.heavy to backdrop.heavyRect
                }
                val radius = ((shape as? SmoothShape)?.radiusPx(size, this) ?: fixedRadius ?: (size.minDimension / 2f))
                    .coerceAtMost(size.minDimension / 2f)
                layer.renderEffect = if (lens != null && !motion.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) lens.effectFor(size, radius, bandPx) else null
                val offset = shift.value
                layer.record {
                    drawImage(
                        image = image,
                        dstOffset = IntOffset(
                            ((dst.left + offset.x - origin.x) / glassScale).roundToInt(),
                            ((dst.top + offset.y - origin.y) / glassScale).roundToInt(),
                        ),
                        dstSize = if (glassScale == 1f) dst.size else IntSize((dst.width / glassScale).roundToInt(), (dst.height / glassScale).roundToInt()),
                        filterQuality = FilterQuality.Low,
                    )
                }
                drawLayer(layer)
            }
            // Over other apps there's no wallpaper to blur, so a frosted fill (following Clear or Tinted) keeps it legible.
            if (backdrop == null) {
                drawRect(when {
                    isDarkTheme -> Color(0x33141418)
                    isTintedTheme -> tintColor.copy(alpha = 0.22f)
                    tuning.look == GlassLook.Tinted -> tintColor.copy(alpha = 0.22f)
                    style == GlassStyle.Clear -> Color.White.copy(alpha = 0.14f + frost * 0.12f)
                    else -> Color.White.copy(alpha = 0.18f + frost * 0.14f)
                })
            }
            if (isTintedTheme) {
                drawRect(Color.Black.copy(alpha = 0.55f))
                drawRect(tint)
            } else {
                drawRect(tint)
            }
            if (tuning.look == GlassLook.Tinted) drawRect(Color.White.copy(alpha = 0.05f))
            val rimStrength = when {
                isDarkTheme -> 0.45f
                isTintedTheme -> 0.85f
                style == GlassStyle.Dark -> 0.55f
                else -> 0.9f
            }
            drawSpecular(
                shape = shape,
                strength = rimStrength,
                width = rimPx,
                glow = frost,
                tint = if (isTintedTheme) tintColor else null
            )
            drawContent()
        }
}

/** Just the blurred wallpaper behind an element, without tint, lens or rim. Used under glass-style icons. */
fun Modifier.glassUnderlay(shape: Shape): Modifier = composed {
    val backdrop = LocalGlassBackdrop.current
    val shift = LocalWallpaperShift.current
    val frost = LocalGlassTuning.current.widgetFrost
    var origin by remember { mutableStateOf(Offset.Zero) }
    val glassScale = LocalGlassScale.current
    if (backdrop == null) return@composed Modifier
    Modifier
        .onGloballyPositioned { origin = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            // Same frost as widgets and panels, so icons and glass read as one material.
            val (image, dst) = when {
                frost < 0.12f -> backdrop.sharp to backdrop.sharpRect
                frost < 0.55f -> backdrop.light to backdrop.lightRect
                else -> backdrop.heavy to backdrop.heavyRect
            }
            val offset = shift.value
            drawImage(
                image = image,
                dstOffset = IntOffset(((dst.left + offset.x - origin.x) / glassScale).roundToInt(), ((dst.top + offset.y - origin.y) / glassScale).roundToInt()),
                dstSize = if (glassScale == 1f) dst.size else IntSize((dst.width / glassScale).roundToInt(), (dst.height / glassScale).roundToInt()),
                filterQuality = FilterQuality.Low,
            )
        }
}

// Bright top-left and bottom-right edges plus a soft glow along the top, like light catching curved glass.
private fun DrawScope.drawSpecular(shape: Shape, strength: Float, width: Float, glow: Float = 1f, tint: Color? = null) {
    val outline = shape.createOutline(size, layoutDirection, this)
    val rimBase = tint ?: Color.White
    val rim = Brush.linearGradient(
        0f to rimBase.copy(alpha = 0.70f * strength),
        0.3f to rimBase.copy(alpha = 0.10f * strength),
        0.7f to rimBase.copy(alpha = 0.06f * strength),
        1f to rimBase.copy(alpha = 0.40f * strength),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    // Stroked on the outline and clipped by the shape, so only the inner half shows. A faint dark hairline goes
    // first, as in Apple's Liquid Glass, so clear glass still reads as an edge over light wallpapers.
    drawOutline(outline, Color.Black.copy(alpha = 0.14f * strength), style = Stroke(width * 3f))
    drawOutline(outline, rim, style = Stroke(width * 2f))
    // The soft sheen along the top grows with frost, so the clearest setting is clear.
    if (glow > 0.01f) {
        val glowColor = tint ?: Color.White
        drawRect(
            Brush.verticalGradient(0f to glowColor.copy(alpha = 0.10f * strength * glow), 1f to Color.Transparent, endY = size.height * 0.45f),
            size = Size(size.width, size.height * 0.45f),
        )
    }
}

private val LENS_BAND = 16.dp

// Pulls the backdrop inward near the edge (a rounded-rect SDF), so the rim magnifies and bends what's behind
// it the way Liquid Glass does, and brightens it slightly.
private const val LENS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float band;

half4 main(float2 p) {
    float2 h = size * 0.5;
    float2 d = p - h;
    float2 q = abs(d) - (h - float2(radius));
    float sd = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float t = clamp(1.0 + sd / band, 0.0, 1.0);
    float bend = t * t;
    float2 g = (q.x > 0.0 && q.y > 0.0) ? normalize(q) : ((q.x > q.y) ? float2(1.0, 0.0) : float2(0.0, 1.0));
    float2 n = g * sign(d);
    half4 c = content.eval(p - n * bend * band * 0.6);
    c.rgb = c.rgb + half3(bend * 0.10);
    return c;
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LensEffect {
    private val shader = RuntimeShader(LENS_AGSL)
    private var cachedSize = Size.Zero
    private var cachedRadius = -1f
    private var effect: RenderEffect? = null

    fun effectFor(size: Size, radius: Float, band: Float): RenderEffect {
        val current = effect
        if (current != null && size == cachedSize && radius == cachedRadius) return current
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("band", band)
        cachedSize = size
        cachedRadius = radius
        return android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect().also { effect = it }
    }
}

/** Swallows taps so they don't fall through to a dismiss scrim underneath. */
fun Modifier.consumeTaps(): Modifier = pointerInput(Unit) { detectTapGestures { } }

@Composable
fun WallpaperLayer(image: ImageBitmap?, dim: Float, zoom: Float, modifier: Modifier = Modifier, blur: Float = 0f, light: ImageBitmap? = null, heavy: ImageBitmap? = null) {
    val shift = LocalWallpaperShift.current
    Spacer(
        modifier
            .fillMaxSize()
            .drawBehind {
                if (image == null) {
                    drawRect(Color.Black)
                    return@drawBehind
                }
                val dst = centerCrop(image.width, image.height, IntSize(size.width.roundToInt(), size.height.roundToInt()), zoom)
                val offset = shift.value
                drawImage(
                    image,
                    dstOffset = IntOffset((dst.left + offset.x).roundToInt(), (dst.top + offset.y).roundToInt()),
                    dstSize = dst.size,
                    filterQuality = FilterQuality.Medium,
                )
                // Wallpaper blur: the lightly and heavily blurred copies (made once per wallpaper) fade in on top,
                // so any blur strength costs just two picture draws, not a live blur.
                fun overlay(copy: ImageBitmap?, alpha: Float) {
                    if (copy == null || alpha <= 0.01f) return
                    val rect = centerCrop(copy.width, copy.height, IntSize(size.width.roundToInt(), size.height.roundToInt()), zoom)
                    drawImage(
                        copy,
                        dstOffset = IntOffset((rect.left + offset.x).roundToInt(), (rect.top + offset.y).roundToInt()),
                        dstSize = rect.size,
                        alpha = alpha,
                        filterQuality = FilterQuality.Low,
                    )
                }
                overlay(light, (blur * 2f).coerceAtMost(1f))
                overlay(heavy, ((blur - 0.5f) * 2f).coerceIn(0f, 1f))
                if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
            },
    )
}
