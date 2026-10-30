package com.nexo.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.nexo.launcher.R
import com.nexo.launcher.data.AlignChoice
import com.nexo.launcher.data.FontChoice
import com.nexo.launcher.data.LauncherSettings
import com.nexo.launcher.data.SilhouetteColor

val AccentPalette: List<Long> = listOf(
    0xFFB9B2FF, 0xFF8AB4F8, 0xFF78D9EC, 0xFF81C995,
    0xFFFDD663, 0xFFFCAD70, 0xFFF28B82, 0xFFFF8BCB,
    0xFFFFFFFF,
)

// Las fuentes incluidas se usan un grado más finas de lo habitual: el texto normal
// sale en Light (300), el medio en Regular (400) y el fino en ExtraLight (200).
private fun thinFamily(extraLight: Int, light: Int, regular: Int, medium: Int) = FontFamily(
    Font(extraLight, FontWeight.Light),
    Font(light, FontWeight.Normal),
    Font(regular, FontWeight.Medium),
    Font(medium, FontWeight.SemiBold),
)

private val Inter = thinFamily(R.font.inter_200, R.font.inter_300, R.font.inter_400, R.font.inter_500)
private val Lato = thinFamily(R.font.lato_300, R.font.lato_300, R.font.lato_400, R.font.lato_400)
private val Manrope = thinFamily(R.font.manrope_200, R.font.manrope_300, R.font.manrope_400, R.font.manrope_500)
private val NunitoSans = thinFamily(R.font.nunitosans_200, R.font.nunitosans_300, R.font.nunitosans_400, R.font.nunitosans_500)
private val WorkSans = thinFamily(R.font.worksans_200, R.font.worksans_300, R.font.worksans_400, R.font.worksans_500)
private val Montserrat = thinFamily(R.font.montserrat_200, R.font.montserrat_300, R.font.montserrat_400, R.font.montserrat_500)
private val Raleway = thinFamily(R.font.raleway_200, R.font.raleway_300, R.font.raleway_400, R.font.raleway_500)
private val Jost = thinFamily(R.font.jost_200, R.font.jost_300, R.font.jost_400, R.font.jost_500)
private val Quicksand = thinFamily(R.font.quicksand_300, R.font.quicksand_300, R.font.quicksand_400, R.font.quicksand_500)

fun FontChoice.family(): FontFamily = when (this) {
    FontChoice.SANS -> FontFamily.SansSerif
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONO -> FontFamily.Monospace
    FontChoice.CURSIVE -> FontFamily.Cursive
    FontChoice.INTER -> Inter
    FontChoice.LATO -> Lato
    FontChoice.MANROPE -> Manrope
    FontChoice.NUNITO_SANS -> NunitoSans
    FontChoice.WORK_SANS -> WorkSans
    FontChoice.MONTSERRAT -> Montserrat
    FontChoice.RALEWAY -> Raleway
    FontChoice.JOST -> Jost
    FontChoice.QUICKSAND -> Quicksand
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

/** Color de los iconos en modo «Silueta» en la pantalla principal. */
fun LauncherSettings.silhouetteTint(): Color = when (silhouetteColor) {
    SilhouetteColor.BLACK -> Color.Black
    SilhouetteColor.WHITE -> Color.White
    SilhouetteColor.TEXT -> foreground()
    SilhouetteColor.ACCENT -> Color(accent)
}

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
