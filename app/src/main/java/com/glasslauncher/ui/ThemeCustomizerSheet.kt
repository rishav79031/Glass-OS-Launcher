package com.glasslauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.data.IconStyle
import com.glasslauncher.data.LauncherSettings
import kotlinx.coroutines.launch

private data class ThemeCardInfo(
    val style: IconStyle,
    val title: String,
    val glowColor: Color,
    val cardBackground: Brush,
)

private val THEME_CARDS = listOf(
    ThemeCardInfo(
        style = IconStyle.Default,
        title = "Default",
        glowColor = Color(0xFF34C759), // Radiant Green
        cardBackground = Brush.verticalGradient(
            listOf(Color(0xFF4A90E2), Color(0xFF1E5BB5))
        )
    ),
    ThemeCardInfo(
        style = IconStyle.Dark,
        title = "Dark",
        glowColor = Color(0xFF007AFF), // Azure Blue
        cardBackground = Brush.verticalGradient(
            listOf(Color(0xFF2C2C2E), Color(0xFF151517))
        )
    ),
    ThemeCardInfo(
        style = IconStyle.Clear,
        title = "Clear",
        glowColor = Color(0xFF30D158), // Emerald Green
        cardBackground = Brush.verticalGradient(
            listOf(Color(0x35FFFFFF), Color(0x18FFFFFF))
        )
    ),
    ThemeCardInfo(
        style = IconStyle.Tinted,
        title = "Tinted",
        glowColor = Color(0xFFFF2D55), // Radiant Red/Magenta
        cardBackground = Brush.verticalGradient(
            listOf(Color(0x608E24AA), Color(0x40E91E63))
        )
    )
)

private val PRESET_TINTS = listOf(
    0xFF007AFF.toInt(), // Blue
    0xFFAF52DE.toInt(), // Purple
    0xFFFF2D55.toInt(), // Pink
    0xFFFF3B30.toInt(), // Red
    0xFFFF9500.toInt(), // Orange
    0xFFFFCC00.toInt(), // Yellow
    0xFF34C759.toInt(), // Green
    0xFF00C7BE.toInt(), // Teal/Cyan
    0xFF64D2FF.toInt(), // Sky
    0xFFE5E5EA.toInt()  // Platinum
)

/**
 * iOS 18 style Customise bottom sheet:
 * Top bar: Sun icon on left, "Customise" title in center, Small/Big icon toggle on top right.
 * 4 Glowing theme cards: Default, Dark, Clear, Tinted.
 */
@Composable
fun ThemeCustomizerSheet(
    settings: LauncherSettings,
    onSelectStyle: (IconStyle) -> Unit,
    onSelectTint: (Int) -> Unit,
    onToggleIconSize: (isLarge: Boolean) -> Unit = {},
    onToggleDarkMode: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val isLarge = !settings.showLabels
    val scope = rememberCoroutineScope()
    val sheetOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 100.dp.toPx() }
    var lastDragTime by remember { mutableStateOf(0L) }
    var dragVelocityY by remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Transparent dismiss area outside sheet so background screen is completely visible
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                }
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .graphicsLayer { translationY = sheetOffsetY.value.coerceAtLeast(0f) }
                .liquidGlass(SmoothShape(32.dp), style = GlassStyle.Regular)
                .consumeTaps()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // Top Bar: Sun icon, "Customise", Big/Small icon toggle
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Sun / Light Mode Icon Button
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onToggleDarkMode),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.LightMode,
                        contentDescription = "Toggle Light/Dark",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center: Customise Title
                Text(
                    "Customise",
                    style = PanelTitleStyle.copy(fontSize = 17.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )

                // Right: Big / Small Icon Toggle (iOS 18 style)
                BigSmallIconToggle(
                    isLarge = isLarge,
                    onToggle = { onToggleIconSize(!isLarge) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // 4 Theme Cards: Default, Dark, Clear, Tinted
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                THEME_CARDS.forEach { card ->
                    val isSelected = settings.iconStyle == card.style ||
                        (card.style == IconStyle.Clear && settings.iconStyle == IconStyle.Glass)
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { onSelectStyle(card.style) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(68.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Glowing diffuse aura beneath selected card
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .offset(y = 12.dp)
                                        .graphicsLayer { alpha = 0.75f }
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(card.glowColor.copy(alpha = 0.85f), card.glowColor.copy(alpha = 0.2f), Color.Transparent),
                                                radius = 90f
                                            ),
                                            shape = CircleShape
                                        )
                                )
                            }

                            // Card Surface
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(SmoothShape(18.dp))
                                    .background(card.cardBackground)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.75.dp,
                                        color = if (isSelected) card.glowColor.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.18f),
                                        shape = SmoothShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                WeatherCloudGraphic(card.style)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Card Title with glowing text when selected
                        Text(
                            card.title,
                            style = PanelBodyStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) card.glowColor else Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }

            // Tint Color Palette when Tinted is active
            AnimatedVisibility(
                visible = settings.iconStyle == IconStyle.Tinted,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                Column(Modifier.padding(top = 16.dp)) {
                    Text(
                        "Tint Color",
                        style = PanelBodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(PRESET_TINTS) { tint ->
                            val isColorSelected = settings.tintColor == tint
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(tint))
                                    .border(
                                        width = if (isColorSelected) 3.dp else 1.dp,
                                        color = if (isColorSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectTint(tint) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isColorSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (Color(tint).luminance() > 0.5f) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HueSlider(settings.tintColor, onSelectTint)
                }
            }
        }
    }
}

/**
 * Big / Small Icon Toggle in top right corner of the Customise sheet:
 * Shows two squircles arranged diagonally (one above another diagonally) as in iOS 18:
 * - Small solid squircle on the bottom-left
 * - Large outlined squircle on the top-right
 */
@Composable
private fun BigSmallIconToggle(
    isLarge: Boolean,
    onToggle: () -> Unit
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (isLarge) 0.22f else 0.12f))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(24.dp, 22.dp)) {
            val strokeWidth = 1.8.dp.toPx()

            // Small squircle (bottom-left)
            val smallSize = Size(8.5.dp.toPx(), 8.5.dp.toPx())
            val smallTopLeft = Offset(1.5.dp.toPx(), 11.5.dp.toPx())
            val smallCorner = CornerRadius(2.6.dp.toPx(), 2.6.dp.toPx())
            if (!isLarge) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = smallTopLeft,
                    size = smallSize,
                    cornerRadius = smallCorner,
                    style = Fill
                )
            } else {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = smallTopLeft,
                    size = smallSize,
                    cornerRadius = smallCorner,
                    style = Stroke(1.3.dp.toPx())
                )
            }

            // Large squircle (top-right, diagonally one above another)
            val largeSize = Size(13.5.dp.toPx(), 13.5.dp.toPx())
            val largeTopLeft = Offset(10.dp.toPx(), 1.dp.toPx())
            val largeCorner = CornerRadius(4.2.dp.toPx(), 4.2.dp.toPx())
            if (isLarge) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = largeTopLeft,
                    size = largeSize,
                    cornerRadius = largeCorner,
                    style = Fill
                )
            } else {
                drawRoundRect(
                    color = Color.White,
                    topLeft = largeTopLeft,
                    size = largeSize,
                    cornerRadius = largeCorner,
                    style = Stroke(strokeWidth)
                )
            }
        }
    }
}

/**
 * Weather Cloud with Sun Graphic matching iOS 18 Customise weather cards:
 * Default: Golden sun behind fluffy white cloud.
 * Dark: Golden sun behind charcoal grey cloud.
 * Clear: Translucent frosted glass cloud with soft glow.
 * Tinted: Vibrant tinted magenta/purple cloud.
 */
@Composable
private fun WeatherCloudGraphic(style: IconStyle) {
    Canvas(Modifier.size(36.dp, 28.dp)) {
        val w = size.width
        val h = size.height

        val sunColor = when (style) {
            IconStyle.Default -> Color(0xFFFFCC00)
            IconStyle.Dark -> Color(0xFFFFB300)
            IconStyle.Clear -> Color(0xFF81C784)
            IconStyle.Tinted -> Color(0xFFFF80AB)
            else -> Color(0xFFFFCC00)
        }

        val cloudColor = when (style) {
            IconStyle.Default -> Color.White
            IconStyle.Dark -> Color(0xFF48484A)
            IconStyle.Clear -> Color.White.copy(alpha = 0.65f)
            IconStyle.Tinted -> Color(0xFFFF2D55).copy(alpha = 0.75f)
            else -> Color.White
        }

        // Sun disc (upper right)
        drawCircle(
            color = sunColor,
            radius = w * 0.22f,
            center = Offset(w * 0.66f, h * 0.36f)
        )

        // Cloud base (circles + pill)
        drawCircle(
            color = cloudColor,
            radius = w * 0.22f,
            center = Offset(w * 0.38f, h * 0.56f)
        )
        drawCircle(
            color = cloudColor,
            radius = w * 0.17f,
            center = Offset(w * 0.62f, h * 0.62f)
        )
        drawCircle(
            color = cloudColor,
            radius = w * 0.14f,
            center = Offset(w * 0.22f, h * 0.68f)
        )
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(w * 0.16f, h * 0.60f),
            size = Size(w * 0.58f, h * 0.24f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )
    }
}
