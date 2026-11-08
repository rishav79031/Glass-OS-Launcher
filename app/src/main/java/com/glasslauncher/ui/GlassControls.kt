package com.glasslauncher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** iOS 26 accent blue for slider fills. */
private val SliderBlue = Color(0xFF3B8BFF)

/**
 * The iOS 26 slider: a thin track with the filled part in blue, small step dots along it, and a clear glass
 * capsule as the thumb that swells slightly while dragged. Touch anywhere on the track to jump there.
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    trackColor: Color = SliderBlue,
    showFill: Boolean = true,
) {
    val change by rememberUpdatedState(onValueChange)
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val shown by animateFloatAsState(fraction, spring(dampingRatio = 1f, stiffness = 1400f), label = "slider")
    val pressing = remember { androidx.compose.runtime.mutableStateOf(false) }
    val swell by animateFloatAsState(if (pressing.value) 1.12f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "thumb")
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps) },
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val thumbW = 54.dp
        val thumbH = 32.dp
        val travel = maxWidth - thumbW
        val travelPx = with(density) { travel.toPx() }
        val halfThumbPx = with(density) { (thumbW / 2).toPx() }
        fun valueAt(x: Float): Float {
            var f = ((x - halfThumbPx) / travelPx).coerceIn(0f, 1f)
            if (steps > 0) f = (f * (steps + 1)).roundToInt() / (steps + 1).toFloat()
            return valueRange.start + f * span
        }
        // Track, fill and step dots, drawn between the thumb's centre positions at either end.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(travelPx, steps, valueRange) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        pressing.value = true
                        change(valueAt(down.position.x))
                        drag(down.id) {
                            it.consume()
                            change(valueAt(it.position.x))
                        }
                        pressing.value = false
                    }
                },
        ) {
            val left = halfThumbPx
            val right = size.width - halfThumbPx
            val y = size.height / 2f
            val h = 5.dp.toPx()
            drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(left, y - h / 2), Size(right - left, h), CornerRadius(h / 2))
            if (showFill) drawRoundRect(trackColor, Offset(left, y - h / 2), Size((right - left) * shown, h), CornerRadius(h / 2))
            val dots = if (steps > 0) steps + 2 else 5
            for (i in 0 until dots) {
                val x = left + (right - left) * i / (dots - 1)
                drawCircle(Color.White.copy(alpha = 0.35f), 1.6.dp.toPx(), Offset(x, y + 10.dp.toPx()))
            }
        }
        Box(
            Modifier
                .offset(x = travel * shown)
                .size(thumbW, thumbH)
                .graphicsLayer {
                    scaleX = swell
                    scaleY = swell
                }
                .liquidGlass(Capsule, style = GlassStyle.Clear),
        )
    }
}

/**
 * The iOS 26 switch: a coloured track (green when on, grey when off) with a clear glass thumb that's a little larger
 * than the track and slides across on a spring.
 */
@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val position by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.72f, stiffness = 420f), label = "switch")
    val track by animateColorAsState(if (checked) AccentGreen else Color.White.copy(alpha = 0.22f), label = "switchTrack")
    Box(
        modifier
            .size(width = 66.dp, height = 34.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.size(width = 62.dp, height = 28.dp).offset(x = 2.dp).clip(CircleShape).background(track))
        Box(
            Modifier
                .offset(x = 26.dp * position)
                .size(width = 40.dp, height = 34.dp)
                .liquidGlass(Capsule, style = GlassStyle.Clear),
        )
    }
}
