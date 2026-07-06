package com.budgettracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Grass,
    onPrimary = Color.White,
    secondary = GrassDeep,
    tertiary = Coin,
    onTertiary = Ink,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = HairlineLight,
    error = WarningCoral,
    onError = Color.White,
    errorContainer = WarningTintLight,
    onErrorContainer = WarningCoral
)

private val DarkColors = darkColorScheme(
    primary = AccentEmeraldDark,
    onPrimary = Color.Black,
    secondary = AccentEmeraldDark,
    tertiary = Coin,
    onTertiary = Ink,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = HairlineDark,
    error = WarningCoralDark,
    onError = Color.Black,
    errorContainer = WarningTintDark,
    onErrorContainer = WarningCoralDark
)

/** CompositionLocal exposing the semantic BudgetColors not covered by Material3's ColorScheme roles. */
val LocalBudgetColors = staticCompositionLocalOf { LightBudgetColors }

@Composable
fun BudgetTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val budgetColors = if (darkTheme) DarkBudgetColors else LightBudgetColors

    CompositionLocalProvider(LocalBudgetColors provides budgetColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

/** Convenience accessor: `BudgetTheme.colors.positive` etc. inside composables. */
object BudgetTheme {
    val colors: BudgetColors
        @Composable
        get() = LocalBudgetColors.current
}
