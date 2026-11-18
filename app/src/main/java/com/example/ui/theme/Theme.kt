package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.ThemeMode

private val EcetDarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = Color(0xFF131144),
    primaryContainer = CobaltContainer,
    onPrimaryContainer = CobaltOnContainer,
    secondary = TealSecondaryDark,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = VioletContainer,
    onSecondaryContainer = VioletOnContainer,
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFFD1FAE5),
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
    outlineVariant = Color(0xFF28345C),
    error = Color(0xFFFB7185),
    onError = Color(0xFF4C0519),
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFFFFE4E6)
)

private val EcetLightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = IndigoOnContainerLight,
    secondary = TealSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = TealOnContainerLight,
    tertiary = EmeraldSuccess,
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
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = Color(0xFFE2E8F5),
    outline = LightBorder,
    outlineVariant = Color(0xFFE8EDF7),
    error = CoralError,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337)
)

val EcetShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EcetTrackerTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDarkTheme) EcetDarkColorScheme else EcetLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EcetTypography,
        shapes = EcetShapes,
        content = content
    )
}
