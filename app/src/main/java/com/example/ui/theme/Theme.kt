package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class MaliExtendedColors(
    val incomeGreen: Color,
    val expenseCoral: Color,
    val mutedGold: Color,
    val onMutedGold: Color,
    val warningAmber: Color,
    val cardSurface: Color,
    val surfaceLow: Color,
    val surfaceHigh: Color,
    val surfaceBright: Color,
    val outlineVariant: Color,
    val isDark: Boolean
)

val DarkExtendedColors = MaliExtendedColors(
    incomeGreen = IncomeGreen,
    expenseCoral = ExpenseCoral,
    mutedGold = MutedGold,
    onMutedGold = OnMutedGold,
    warningAmber = WarningAmber,
    cardSurface = CardSurface,
    surfaceLow = SurfaceContainerLow,
    surfaceHigh = SurfaceContainerHigh,
    surfaceBright = SurfaceBright,
    outlineVariant = DarkOutlineVariant,
    isDark = true
)

val LightExtendedColors = MaliExtendedColors(
    incomeGreen = LightPrimary,
    expenseCoral = LightExpenseCoral,
    mutedGold = Color(0xFFB38B2D),
    onMutedGold = Color(0xFFFFFFFF),
    warningAmber = Color(0xFFC98A1A),
    cardSurface = LightSurface,
    surfaceLow = LightSurfaceContainerLow,
    surfaceHigh = LightSurfaceContainerHigh,
    surfaceBright = Color(0xFFDCE3EC),
    outlineVariant = LightOutlineVariant,
    isDark = false
)

val LocalMaliColors = staticCompositionLocalOf { DarkExtendedColors }

private val MaliDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = MutedGold,
    onSecondary = OnMutedGold,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = CharcoalBg,
    onBackground = OnDarkSurface,
    surface = CharcoalBg,
    onSurface = OnDarkSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnDarkSurfaceVariant,
    surfaceContainerLowest = SurfaceDim,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val MaliLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFD0F5E4),
    onPrimaryContainer = Color(0xFF002B17),
    secondary = Color(0xFFB38B2D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDF0D0),
    onSecondaryContainer = Color(0xFF2A1E00),
    error = LightExpenseCoral,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD7),
    onErrorContainer = Color(0xFF68000D),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightBackground,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFFDCE3EC),
    outline = Color(0xFF7A828F),
    outlineVariant = LightOutlineVariant
)

val MaliShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object MaliTheme {
    val colors: MaliExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMaliColors.current
}

@Composable
fun MaliAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MaliDarkColorScheme else MaliLightColorScheme
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalMaliColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaliTypography,
            shapes = MaliShapes,
            content = content
        )
    }
}
