package com.glasslauncher.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.glasslauncher.R

/**
 * Inter, the closest open-licence match to Apple's SF Pro (which may not be shipped outside Apple platforms).
 * One variable font file covers every weight. Licence: FONT_LICENSE.txt (SIL Open Font License 1.1).
 */
@OptIn(ExperimentalTextApi::class)
val AppFont = FontFamily(
    listOf(300, 400, 500, 600, 700).map { weight ->
        Font(R.font.inter, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
    },
)

private val LabelShadow = Shadow(color = Color.Black.copy(alpha = 0.40f), offset = Offset(0f, 1f), blurRadius = 3f)

// Type scale after the iOS defaults (Body 17, Subheadline 15, Footnote 13, Caption 12), with SF-like tight tracking.
val IconLabelStyle = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, shadow = LabelShadow, letterSpacing = (-0.1).sp)
val LargeTitleStyle = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
val PanelTitleStyle = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
val PanelBodyStyle = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 16.sp, letterSpacing = (-0.2).sp)
val PanelCaptionStyle = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.68f), fontSize = 13.sp, letterSpacing = (-0.05).sp)
val WidgetCaptionStyle = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

// iOS system colours (dark appearance).
val AccentBlue = Color(0xFF0A84FF)
val AccentOrange = Color(0xFFFF9F0A)
val AccentGreen = Color(0xFF30D158)
val AccentPurple = Color(0xFF5E5CE6)
val AccentYellow = Color(0xFFFFD60A)
val AccentPink = Color(0xFFFF375F)
val AccentTeal = Color(0xFF40C8E0)
val DestructiveRed = Color(0xFFFF453A)
val BadgeRed = Color(0xFFFF3B30)
val Separator = Color.White.copy(alpha = 0.16f)

/** Swatches for the Tinted icon appearance. */
val TintSwatches = listOf(AccentOrange, AccentYellow, AccentGreen, AccentTeal, AccentBlue, AccentPurple, AccentPink, Color(0xFFD1D1D6))

fun Context.startSafely(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    false
}

/** Weight and face for each Lock Screen clock style. */
fun lockClockFont(style: com.glasslauncher.data.LockClockStyle): Pair<androidx.compose.ui.text.font.FontFamily, androidx.compose.ui.text.font.FontWeight> {
    val weight = androidx.compose.ui.text.font.FontWeight
    val family = androidx.compose.ui.text.font.FontFamily
    return when (style) {
        com.glasslauncher.data.LockClockStyle.Bold -> AppFont to weight.Bold
        com.glasslauncher.data.LockClockStyle.Regular -> AppFont to weight.Medium
        com.glasslauncher.data.LockClockStyle.Light -> AppFont to weight.Light
        com.glasslauncher.data.LockClockStyle.Heavy -> AppFont to weight.Black
        com.glasslauncher.data.LockClockStyle.Serif -> family.Serif to weight.Normal
        com.glasslauncher.data.LockClockStyle.Mono -> family.Monospace to weight.Medium
        // Android's rounded system face where it exists; elsewhere the default sans at a soft weight.
        com.glasslauncher.data.LockClockStyle.Rounded -> family.SansSerif to weight.SemiBold
    }
}
