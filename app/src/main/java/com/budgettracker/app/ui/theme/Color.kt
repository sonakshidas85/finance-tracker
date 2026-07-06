package com.budgettracker.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Named color constants shared across Compose (Material3 ColorScheme) and Glance (widget UI).
 * Keeping them as plain top-level `Color` values (rather than only inside a ColorScheme) lets the
 * Glance widget code - which does not have access to Compose's `MaterialTheme.colorScheme` -
 * reuse the exact same palette. See `widget/WidgetContent.kt` for the Glance-side color mapping
 * and `ui/theme/Theme.kt` for `LocalBudgetColors`.
 */

// Light palette
val BackgroundLight = Color(0xFFFAF9F6)      // warm off-white
val SurfaceLight = Color(0xFFFFFFFF)         // card surface
val TextPrimaryLight = Color(0xFF1C1B18)
val TextSecondaryLight = Color(0xFF6B6A64)
val TextMutedLight = Color(0xFFA6A49B)
val AccentEmerald = Color(0xFF1F7A5C)
val HeaderEmphasis = Color(0xFF0F3D30)
val PositiveTintLight = Color(0xFFE5F1EC)
val WarningCoral = Color(0xFFC2492D)
val WarningTintLight = Color(0xFFFBEAE4)
val HairlineLight = Color(0xFFE7E4DC)

// Dark palette - same accent hues, adjusted for contrast on a dark surface with light text.
val BackgroundDark = Color(0xFF17181A)
val SurfaceDark = Color(0xFF201F1C)
val TextPrimaryDark = Color(0xFFF3F1EC)
val TextSecondaryDark = Color(0xFFB9B7B0)
val TextMutedDark = Color(0xFF7C7A73)
val AccentEmeraldDark = Color(0xFF4FB894)     // lighter emerald for contrast on dark bg
val HeaderEmphasisDark = Color(0xFFBFE9D8)
val PositiveTintDark = Color(0xFF1B3A30)
val WarningCoralDark = Color(0xFFE07A5C)      // lighter coral for contrast on dark bg
val WarningTintDark = Color(0xFF3D241C)
val HairlineDark = Color(0xFF3A392F)

/**
 * Semantic colors not directly modeled by Material3's default ColorScheme roles (savings/positive
 * tint, warning/warning tint, hairline). Exposed via `LocalBudgetColors` in Theme.kt.
 */
data class BudgetColors(
    val positive: Color,
    val positiveTint: Color,
    val warning: Color,
    val warningTint: Color,
    val hairline: Color,
    val headerEmphasis: Color,
    val textMuted: Color
)

val LightBudgetColors = BudgetColors(
    positive = AccentEmerald,
    positiveTint = PositiveTintLight,
    warning = WarningCoral,
    warningTint = WarningTintLight,
    hairline = HairlineLight,
    headerEmphasis = HeaderEmphasis,
    textMuted = TextMutedLight
)

val DarkBudgetColors = BudgetColors(
    positive = AccentEmeraldDark,
    positiveTint = PositiveTintDark,
    warning = WarningCoralDark,
    warningTint = WarningTintDark,
    hairline = HairlineDark,
    headerEmphasis = HeaderEmphasisDark,
    textMuted = TextMutedDark
)
