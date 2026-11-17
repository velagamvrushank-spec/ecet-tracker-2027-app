package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val EcetDarkColorScheme = darkColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color(0xFF06132E),
    primaryContainer = CobaltContainer,
    onPrimaryContainer = CobaltOnContainer,
    secondary = VioletSecondary,
    onSecondary = Color(0xFF150B38),
    secondaryContainer = VioletContainer,
    onSecondaryContainer = VioletOnContainer,
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF04241B),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFFCCFBEF),
    background = SlateVoid,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = SlateTextSecondary,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = SlateCardElevated,
    outline = SlateBorder,
    outlineVariant = Color(0xFF1D273E),
    error = CoralError,
    onError = Color(0xFF330A0E),
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFFFEE2E2)
)

private val EcetLightColorScheme = lightColorScheme(
    primary = LightCobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = LightCobaltContainer,
    onPrimaryContainer = Color(0xFF102A6B),
    secondary = LightVioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightVioletContainer,
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFE2E8F5),
    outline = LightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val EcetShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EcetTrackerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDarkTheme) EcetDarkColorScheme else EcetLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EcetTypography,
        shapes = EcetShapes,
        content = content
    )
}
