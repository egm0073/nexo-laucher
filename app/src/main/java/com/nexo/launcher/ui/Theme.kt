package com.nexo.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.nexo.launcher.data.AlignChoice
import com.nexo.launcher.data.FontChoice
import com.nexo.launcher.data.LauncherSettings

val AccentPalette: List<Long> = listOf(
    0xFFB9B2FF, 0xFF8AB4F8, 0xFF78D9EC, 0xFF81C995,
    0xFFFDD663, 0xFFFCAD70, 0xFFF28B82, 0xFFFF8BCB,
    0xFFFFFFFF,
)

fun FontChoice.family(): FontFamily = when (this) {
    FontChoice.SANS -> FontFamily.SansSerif
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONO -> FontFamily.Monospace
    FontChoice.CURSIVE -> FontFamily.Cursive
}

fun AlignChoice.horizontal(): Alignment.Horizontal = when (this) {
    AlignChoice.START -> Alignment.Start
    AlignChoice.CENTER -> Alignment.CenterHorizontally
    AlignChoice.END -> Alignment.End
}

fun AlignChoice.bottom(): Alignment = when (this) {
    AlignChoice.START -> Alignment.BottomStart
    AlignChoice.CENTER -> Alignment.BottomCenter
    AlignChoice.END -> Alignment.BottomEnd
}

fun AlignChoice.textAlign(): TextAlign = when (this) {
    AlignChoice.START -> TextAlign.Start
    AlignChoice.CENTER -> TextAlign.Center
    AlignChoice.END -> TextAlign.End
}

fun LauncherSettings.foreground(): Color = if (lightText) Color.White else Color(0xFF141414)

fun LauncherSettings.homeTextStyle(
    scale: Float = 1f,
    weight: FontWeight = FontWeight.Normal,
    alpha: Float = 1f,
): TextStyle = TextStyle(
    color = foreground().copy(alpha = alpha),
    fontSize = (textSizeSp * scale).sp,
    fontFamily = font.family(),
    fontWeight = weight,
    textAlign = align.textAlign(),
    shadow = if (lightText) Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 2f), 8f) else null,
)

@Composable
fun LauncherTheme(accent: Color, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        secondary = accent,
        secondaryContainer = accent.copy(alpha = 0.28f),
        onSecondaryContainer = Color.White,
        background = Color(0xFF101014),
        surface = Color(0xFF16161B),
        surfaceContainerLow = Color(0xFF1C1C22),
        surfaceContainer = Color(0xFF202027),
        surfaceContainerHigh = Color(0xFF26262E),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
