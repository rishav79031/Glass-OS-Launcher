package com.glasslauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Which greeting to write: "hello" at setup and after a restart, "good morning" at the first unlock of the day. */
enum class Greeting(val words: List<String>) {
    Hello(listOf("hello", "bonjour", "नमस्ते", "hola", "ciao", "hallo", "こんにちは", "olá", "안녕하세요", "привет", "你好", "hello")),
    Morning(listOf("good morning", "bonjour", "सुप्रभात", "buenos días", "buongiorno", "guten Morgen", "おはよう", "bom dia", "좋은 아침", "доброе утро", "早上好", "good morning")),
}

/**
 * The iOS-style greeting: each "hello" is written on from left to right in a script face, holds, then fades as
 * the next one begins. Plays once through (about 14 seconds); a tap anywhere skips it.
 */
@Composable
fun HelloScreen(greeting: Greeting = Greeting.Hello, onFinished: () -> Unit) {
    val Greetings = greeting.words
    val finish by rememberUpdatedState(onFinished)
    var index by remember { mutableIntStateOf(0) }
    val write = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        for (i in Greetings.indices) {
            index = i
            fade.snapTo(1f)
            write.snapTo(0f)
            write.animateTo(1f, tween(900, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
            delay(if (i == Greetings.lastIndex) 900 else 450)
            fade.animateTo(0f, tween(260))
        }
        finish()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0B1E), Color(0xFF1B1446), Color(0xFF0B2A4A))))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Greetings[index],
            style = TextStyle(fontFamily = FontFamily.Cursive, fontSize = if (greeting == Greeting.Morning) 64.sp else 84.sp, fontWeight = FontWeight.Medium, color = Color.White),
            modifier = Modifier
                .graphicsLayer {
                    alpha = fade.value
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // A soft-edged wipe from left to right, like a pen writing the word.
                    val edge = size.width * 0.18f
                    val x = (size.width + edge) * write.value
                    drawRect(
                        Brush.horizontalGradient(0f to Color.Black, 1f to Color.Transparent, startX = x - edge, endX = x),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
    }
}
