package com.budgettracker.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Named color constants shared across Compose (Material3 ColorScheme) and Glance (widget UI).
 * Keeping them as plain top-level `Color` values (rather than only inside a ColorScheme) lets the
 * Glance widget code - which does not have access to Compose's `MaterialTheme.colorScheme` -
 * reuse the exact same palette. See `widget/WidgetContent.kt` for the Glance-side color mapping
 * and `ui/theme/Theme.kt` for `LocalBudgetColors`.
 *
 * These are the exact hex tokens from the "gremlin" brand guidelines (mascot: Bean), copied
 * verbatim from the guideline doc's own `tokens.css` reference:
 *   grass #33C04A · grass-deep #1E9E38 · coin #FFC83A · ink #123322 · cream #EFE9DA ·
 *   surface #FBF8EF · mint #EAF6E8 · line #E3DDCB · debt #E4572E
 *
 * Money-semantics rule from the guidelines, applied everywhere in this app: positive balances,
 * income & savings use Grass. Spending, negative balances & overspend warnings use Debt. Grass is
 * never used for a loss, and Debt is never used for a gain.
 */

// Brand tokens (identical in both themes - Bean and the wordmark never recolor per the guidelines).
val Grass = Color(0xFF33C04A)
val GrassDeep = Color(0xFF1E9E38)
val Coin = Color(0xFFFFC83A)
val Ink = Color(0xFF123322)
val Cream = Color(0xFFEFE9DA)
val Surface = Color(0xFFFBF8EF)
val Mint = Color(0xFFEAF6E8)
val Line = Color(0xFFE3DDCB)
val Debt = Color(0xFFE4572E)
val TextMutedBrand = Color(0xFF6B7D6F)

// Light palette - built from brand tokens directly.
val BackgroundLight = Cream
val SurfaceLight = Surface
val TextPrimaryLight = Ink
val TextSecondaryLight = TextMutedBrand
val TextMutedLight = TextMutedBrand
val AccentEmerald = Grass
val HeaderEmphasis = Ink
val PositiveTintLight = Mint
val WarningCoral = Debt
val WarningTintLight = Color(0xFFFDEAE4)      // matches the guideline's "Over" badge chip fill
val HairlineLight = Line

// Dark palette - the guidelines' "06 Developer tokens" panel itself is rendered on the Ink
// background with a lightened grass (#7fd68f) for its accent text, which we mirror here for a
// dark surface: same brand hues, lightened just enough for contrast on Ink.
val BackgroundDark = Ink
val SurfaceDark = Color(0xFF1A3B2A)
val TextPrimaryDark = Cream
val TextSecondaryDark = Color(0xFFA9C4B0)
val TextMutedDark = Color(0xFFA9C4B0)
val AccentEmeraldDark = Color(0xFF7FD68F)     // lightened grass for contrast on Ink, per guideline panel
val HeaderEmphasisDark = Cream
val PositiveTintDark = Color(0xFF234A34)
val WarningCoralDark = Color(0xFFF08165)      // lightened debt for contrast on Ink
val WarningTintDark = Color(0xFF3D241C)
val HairlineDark = Color(0xFF2E5A40)

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
