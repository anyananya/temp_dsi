package org.dsiders.nextstep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.dsiders.nextstep.R

/** Colours from the Figma prototype. Nothing in the product uses red. */
object Palette {
    val Page = Color(0xFFF8F6F2)
    val Card = Color(0xFFFFFFFF)
    val Navy = Color(0xFF2C3468)
    val NavyMuted = Color(0xFF5A6080)
    val Text = Color(0xFF1F2333)
    val TextMuted = Color(0xFF6B6F7B)
    val Orange = Color(0xFFD97B43)
    val OrangeSoft = Color(0xFFFBEADF)
    val OrangeDeep = Color(0xFFB0571F)
    val Green = Color(0xFF2E6B4B)
    val GreenSoft = Color(0xFFDFF1E6)
    val Amber = Color(0xFFE8924A)
    val AmberSoft = Color(0xFFFCEBDC)
    val AmberText = Color(0xFFB4621E)
    val BannerAmber = Color(0xFFFDF4E3)
    val BannerAmberEdge = Color(0xFFE2A93B)
    val Grey = Color(0xFF8A8D96)
    val GreySoft = Color(0xFFEDEDED)
    val Chip = Color(0xFFE9EBF6)
    val Lavender = Color(0xFFDDE1F4)
    val Divider = Color(0xFFE9E6E0)
    val Peach = Color(0xFFF7D9C8)
}

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun noto(weight: Int) = Font(
    R.font.noto_sans, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))
val Noto = FontFamily(noto(400), noto(500), noto(600), noto(700))

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, color = Palette.Navy),
    headlineMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, color = Palette.Navy),
    headlineSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = Palette.Navy),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Noto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Noto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Noto, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Noto, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Noto, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Noto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = Noto, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Noto, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp),
)

private val Colors = lightColorScheme(
    primary = Palette.Navy,
    onPrimary = Color.White,
    secondary = Palette.Orange,
    onSecondary = Color.White,
    tertiary = Palette.Green,
    background = Palette.Page,
    onBackground = Palette.Text,
    surface = Palette.Card,
    onSurface = Palette.Text,
    surfaceVariant = Palette.Chip,
    onSurfaceVariant = Palette.TextMuted,
    outline = Palette.Navy,
    outlineVariant = Palette.Divider,
    error = Palette.AmberText,
)

@Composable
fun NextStepTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
