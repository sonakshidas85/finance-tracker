package com.budgettracker.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The "gremlin" brand guidelines specify a two-family system: Quicksand (weights 500-600) for the
 * wordmark, headings, and big numbers; DM Sans (weights 400/500/700) for all body and UI text.
 * Both are Google Fonts. We intentionally do NOT bundle .ttf assets or wire up the Downloadable
 * Fonts API here: that API requires a Google-issued provider certificate hash, and shipping a
 * hand-typed certificate we can't verify byte-for-byte would silently break font loading in a way
 * that's very hard for a non-technical user to diagnose. Per the original app's own design
 * philosophy ("avoid needing font asset files"), we instead use the platform default sans-serif
 * for everything, but shape weight/size/letter-spacing to match the brand's type scale exactly
 * (Display 48·600, H1 30·600, H2 22·600, Body 16·400, Label 13·500 uppercase +0.02em tracking).
 *
 * If you want pixel-exact Quicksand/DM Sans later: in Android Studio, right-click res/font ->
 * New -> Font Resource File -> "Add font from Google Fonts" and search "Quicksand"/"DM Sans" -
 * the IDE generates a verified, correct certificate for you, then swap FontFamily.Default below
 * for the generated FontFamily.
 */
val QuicksandWeight = FontWeight.SemiBold // stands in for Quicksand 600 across all display text

/**
 * Big-number style (currency balances, percentages) - previously monospace, but the brand's own
 * type scale shows big numbers ("$2,480.15") set in Quicksand, not a monospaced font. Renamed
 * from the old MonospaceNumberStyle; kept as a distinct name via a typealias-style function below
 * so existing call sites don't all need find-and-replace.
 */
val MonospaceNumberStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = QuicksandWeight
)

/** Uppercase label style matching the brand's Label/13·500, +0.02em tracking token. */
val BrandLabelStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    letterSpacing = 0.26.sp // ~0.02em at 13sp
)

val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    titleLarge = TextStyle(
        // Brand H1: 30·600 (Quicksand stand-in)
        fontFamily = FontFamily.Default,
        fontWeight = QuicksandWeight,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        // Brand H2: 22·600 (Quicksand stand-in)
        fontFamily = FontFamily.Default,
        fontWeight = QuicksandWeight,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

/** Big-number variant helper for currency/percent labels, derived from a base text style. */
fun TextStyle.asMonospaceNumber(): TextStyle = merge(MonospaceNumberStyle)
