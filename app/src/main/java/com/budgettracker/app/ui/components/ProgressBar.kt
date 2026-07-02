package com.budgettracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.budgettracker.app.ui.theme.BudgetTheme

/**
 * Thin progress bar used by category rows and the footer summary. `fraction` is expected to
 * already be clamped to [0,1] by the caller (see BudgetCalculations.progressFraction) - this
 * composable clamps defensively again so it never mis-renders even if a caller forgets.
 */
@Composable
fun ThinProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = BudgetTheme.colors.hairline,
    progressColor: Color = BudgetTheme.colors.positive
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidthFraction(clamped)
                .clip(RoundedCornerShape(50))
                .background(progressColor)
                .align(Alignment.CenterStart)
        )
    }
}

/** Small helper: fillMaxWidth(fraction) but named for clarity at call sites above. */
private fun Modifier.fillMaxWidthFraction(fraction: Float): Modifier =
    this.then(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)))
