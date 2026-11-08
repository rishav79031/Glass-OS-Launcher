package com.glasslauncher.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.data.AppEntry
import com.glasslauncher.data.IconStyle
import com.glasslauncher.icons.Squircle

@Immutable
data class IconLook(
    val size: Dp,
    val style: IconStyle,
    val showLabel: Boolean,
    val blurUnder: Boolean,
    val labelSize: TextUnit = 12.sp,
)

/** Holds an element's live coordinates without making them composition state. */
class Anchor {
    var coordinates: LayoutCoordinates? = null

    fun bounds(): Rect = coordinates?.takeIf { it.isAttached }?.boundsInWindow() ?: Rect.Zero
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppEntry,
    icon: ImageBitmap?,
    look: IconLook,
    onLaunch: (AppEntry, Rect) -> Unit,
    onMenu: (AppEntry, Rect) -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = look.showLabel,
    badge: Int = 0,
    editing: Boolean = false,
    onRemove: (() -> Unit)? = null,
    iconModifier: Modifier = Modifier,
    anchor: Anchor = remember { Anchor() },
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "iconPress",
    )
    val haptics = LocalHapticFeedback.current
    // Jiggle mode: each icon wobbles at a slightly different speed, like iOS.
    val wobble = if (editing) {
        val transition = rememberInfiniteTransition(label = "jiggle")
        transition.animateFloat(
            initialValue = -1.8f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 120 + (app.key.hashCode() and 0x3F)), RepeatMode.Reverse),
            label = "jiggleAngle",
        )
    } else {
        null
    }
    // iOS dark/tinted/clear icons are glass; the default appearance is opaque.
    val glassy = look.style != IconStyle.Default && look.style != IconStyle.Automatic

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(look.size)
                .onPlaced { anchor.coordinates = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = wobble?.value ?: 0f
                }
                .then(iconModifier),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (look.blurUnder && glassy) Modifier.glassUnderlay(Squircle) else Modifier)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = !editing,
                        onClick = { onLaunch(app, anchor.bounds()) },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMenu(app, anchor.bounds())
                        },
                    ),
            ) {
                if (icon != null) {
                    Image(icon, contentDescription = app.label, modifier = Modifier.fillMaxSize(), filterQuality = FilterQuality.Medium)
                } else {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.12f), Squircle))
                }
            }
            if (editing && onRemove != null) {
                RemoveBadge(Modifier.align(Alignment.TopStart).offset((-7).dp, (-7).dp), onRemove)
            } else if (badge > 0) {
                CountBadge(badge, Modifier.align(Alignment.TopEnd).offset(7.dp, (-7).dp))
            }
        }
        if (showLabel) {
            Text(
                text = app.label,
                style = IconLabelStyle.copy(fontSize = look.labelSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 5.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier) {
    Box(
        modifier
            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
            .background(BadgeRed, CircleShape)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            style = IconLabelStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, shadow = null),
        )
    }
}

@Composable
private fun RemoveBadge(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(24.dp)
            .liquidGlass(Capsule, style = GlassStyle.Dark)
            .clickable(onClickLabel = "Remove", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Remove, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
    }
}
