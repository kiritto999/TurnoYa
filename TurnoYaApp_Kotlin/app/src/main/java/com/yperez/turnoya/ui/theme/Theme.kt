package com.yperez.turnoya.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Paleta tomada de los logos de TurnoYa: azul tecnológico + menta + acento naranja del ícono. */
object TyColors {
    val Blue = Color(0xFF0A6FCB)
    val BlueDark = Color(0xFF0753A0)
    val BlueLight = Color(0xFF2F9BFF)
    val Cyan = Color(0xFF47E8FF)
    val Mint = Color(0xFF79DEAC)
    val MintDark = Color(0xFF2FB57A)
    val Orange = Color(0xFFFF8A1F)
    val OrangeDark = Color(0xFFE5620F)
    val Navy = Color(0xFF0E1630)
    val NavySurface = Color(0xFF141F3E)
    val NavyCard = Color(0xFF1C2A50)
    val Ink = Color(0xFF16213A)
    val Muted = Color(0xFF6B7A99)
    val Background = Color(0xFFF4F7FC)
    val Danger = Color(0xFFE5484D)

    val brandGradient = Brush.linearGradient(listOf(Blue, BlueLight, Mint))
    val headerGradient = Brush.verticalGradient(listOf(Navy, NavySurface))
    val ctaGradient = Brush.horizontalGradient(listOf(Blue, BlueLight))
    val hotGradient = Brush.horizontalGradient(listOf(OrangeDark, Orange))
}

private val Light = lightColorScheme(
    primary = TyColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FB),
    onPrimaryContainer = TyColors.BlueDark,
    secondary = TyColors.MintDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF7EA),
    onSecondaryContainer = Color(0xFF0E5A3A),
    tertiary = TyColors.Orange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6D1),
    onTertiaryContainer = Color(0xFF7A3300),
    background = TyColors.Background,
    onBackground = TyColors.Ink,
    surface = Color.White,
    onSurface = TyColors.Ink,
    surfaceVariant = Color(0xFFEAF0F8),
    onSurfaceVariant = TyColors.Muted,
    outline = Color(0xFFD3DCEA),
    outlineVariant = Color(0xFFE6ECF5),
    error = TyColors.Danger,
)

private val Dark = darkColorScheme(
    primary = TyColors.BlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF123A6B),
    onPrimaryContainer = Color(0xFFD6E8FB),
    secondary = TyColors.Mint,
    onSecondary = Color(0xFF00391F),
    secondaryContainer = Color(0xFF14513A),
    onSecondaryContainer = Color(0xFFDDF7EA),
    tertiary = TyColors.Orange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5A2A06),
    onTertiaryContainer = Color(0xFFFFE6D1),
    background = TyColors.Navy,
    onBackground = Color(0xFFE6ECF5),
    surface = TyColors.NavySurface,
    onSurface = Color(0xFFE6ECF5),
    surfaceVariant = TyColors.NavyCard,
    onSurfaceVariant = Color(0xFFA5B3CF),
    outline = Color(0xFF34466F),
    outlineVariant = Color(0xFF26355A),
    error = Color(0xFFFF6B6F),
)

private val TyTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
)

private val TyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun TurnoYaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = TyTypography,
        shapes = TyShapes,
        content = content,
    )
}
