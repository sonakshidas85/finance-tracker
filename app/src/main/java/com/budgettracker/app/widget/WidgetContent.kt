package com.budgettracker.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.budgettracker.app.MainActivity
import com.budgettracker.app.QuickAddSpendActivity
import com.budgettracker.app.data.BudgetCalculations
import com.budgettracker.app.data.BudgetPeriod
import com.budgettracker.app.data.BudgetState
import com.budgettracker.app.data.Category
import com.budgettracker.app.data.CurrencyFormat
import com.budgettracker.app.data.PeriodStamps
import com.budgettracker.app.ui.theme.AccentEmerald
import com.budgettracker.app.ui.theme.AccentEmeraldDark
import com.budgettracker.app.ui.theme.BackgroundDark
import com.budgettracker.app.ui.theme.BackgroundLight
import com.budgettracker.app.ui.theme.HairlineDark
import com.budgettracker.app.ui.theme.HairlineLight
import com.budgettracker.app.ui.theme.SurfaceDark
import com.budgettracker.app.ui.theme.SurfaceLight
import com.budgettracker.app.ui.theme.TextMutedDark
import com.budgettracker.app.ui.theme.TextMutedLight
import com.budgettracker.app.ui.theme.TextPrimaryDark
import com.budgettracker.app.ui.theme.TextPrimaryLight
import com.budgettracker.app.ui.theme.WarningCoral
import com.budgettracker.app.ui.theme.WarningCoralDark

/**
 * Shared small/medium Glance composables + color mapping, reused by both WeeklyBudgetWidget and
 * MonthlyBudgetWidget (they only differ in which PeriodBudget they pass in and the
 * EXTRA_SELECTED_PERIOD value on their tap intent).
 *
 * DARK MODE IN GLANCE: Glance's Material3 dynamic color (`GlanceTheme.colors`) resolves to the
 * Android 12+ dynamic (wallpaper-derived) palette on S+ and a static fallback below it - that
 * would NOT reproduce this app's specific warm-off-white/emerald/coral palette. Since the spec
 * asks us to "mirror the same dark palette in the Glance widget UI", we instead branch on the
 * host Context's night-mode configuration (`Configuration.UI_MODE_NIGHT_MASK`) via [isNightMode]
 * and manually pick between our own Light/Dark Color constants (the same ones the Compose theme
 * uses from ui/theme/Color.kt). This is the idiomatic route when a widget needs a fully custom
 * (non-Material-dynamic) palette rather than the system dynamic color.
 */

private const val TARGET_ACTIVITY_PERIOD_KEY = "selected_period"

data class GlanceBudgetColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val hairline: Color,
    val accent: Color,
    val warning: Color
)

fun isNightMode(context: Context): Boolean {
    val uiMode = context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
}

fun glanceColorsFor(context: Context): GlanceBudgetColors = if (isNightMode(context)) {
    GlanceBudgetColors(
        background = BackgroundDark,
        surface = SurfaceDark,
        textPrimary = TextPrimaryDark,
        textMuted = TextMutedDark,
        hairline = HairlineDark,
        accent = AccentEmeraldDark,
        warning = WarningCoralDark
    )
} else {
    GlanceBudgetColors(
        background = BackgroundLight,
        surface = SurfaceLight,
        textPrimary = TextPrimaryLight,
        textMuted = TextMutedLight,
        hairline = HairlineLight,
        accent = AccentEmerald,
        warning = WarningCoral
    )
}

/**
 * Tap action for both widgets: launches MainActivity with the EXTRA_SELECTED_PERIOD extra so
 * MainActivity/ViewModel can pre-select the matching tab. Uses the reified
 * `actionStartActivity<T>(parameters)` overload (rather than building a raw Intent) since Glance
 * passes ActionParameters through as Intent extras keyed by the parameter's name - MainActivity's
 * existing `intent.getStringExtra(EXTRA_SELECTED_PERIOD)` read picks it up unchanged, and this
 * sidesteps an Intent-vs-ComponentName overload mismatch on the Glance version this project pins.
 */
private val selectedPeriodParam = ActionParameters.Key<String>(TARGET_ACTIVITY_PERIOD_KEY)

fun widgetTapIntentAction(period: BudgetPeriod) = actionStartActivity<MainActivity>(
    parameters = actionParametersOf(
        selectedPeriodParam to if (period == BudgetPeriod.WEEKLY) MainActivity.PERIOD_WEEKLY else MainActivity.PERIOD_MONTHLY
    )
)

/** Formats the small-layout period label: "This week" / "July 2026". */
fun periodDisplayLabel(period: BudgetPeriod, state: BudgetState): String = when (period) {
    BudgetPeriod.WEEKLY -> "This week"
    BudgetPeriod.MONTHLY -> PeriodStamps.monthLabelFromStamp(state.monthly.periodStamp)
}

/**
 * Placeholder rendered instead of a real ₹ amount when the "Mask amounts on home-screen widgets"
 * setting is on - home-screen widgets are visible without unlocking the app (even with app-lock
 * enabled), so masking avoids leaking salary/spend figures to anyone glancing at the home/lock
 * screen. Category names and progress bars stay visible either way - only the amount is hidden.
 */
private const val MASKED_AMOUNT_PLACEHOLDER = "₹••••"

private fun displayAmount(amount: Double, masked: Boolean): String =
    if (masked) MASKED_AMOUNT_PLACEHOLDER else CurrencyFormat.formatINR(amount)

/**
 * Compact headline-only layout (~2x1 small size bucket): period label, remaining spending pool,
 * slim progress bar. Tapping anywhere launches MainActivity on the matching tab.
 *
 * WEEKLY ONLY: this layout gets a small compact "+" quick-add affordance in its top-right corner
 * that launches QuickAddSpendActivity's floating popup - the monthly widget stays exactly as it
 * was (display-only, tap-through-to-app), per spec.
 */
@Composable
fun SmallBudgetWidgetContent(
    period: BudgetPeriod,
    state: BudgetState,
    colors: GlanceBudgetColors,
    maskAmounts: Boolean = false
) {
    val periodBudget = if (period == BudgetPeriod.WEEKLY) state.weekly else state.monthly
    val pool = BudgetCalculations.periodPool(period, state.monthlySalary, state.savingsGoalPercent, state.weeklyAllotment)
    val remaining = BudgetCalculations.remainingPool(pool, periodBudget.categories)
    val progress = BudgetCalculations.overallProgressFraction(pool, periodBudget.categories)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.surface)
            .cornerRadius(14.dp)
            .padding(12.dp)
            .clickable(widgetTapIntentAction(period))
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = periodDisplayLabel(period, state),
                style = TextStyle(color = ColorProvider(colors.textMuted), fontSize = 11.sp),
                modifier = GlanceModifier.defaultWeight()
            )
            if (period == BudgetPeriod.WEEKLY) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = ColorProvider(colors.accent),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.clickable(actionStartActivity<QuickAddSpendActivity>())
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = displayAmount(remaining, maskAmounts),
            style = TextStyle(
                color = ColorProvider(if (remaining < 0.0) colors.warning else colors.textPrimary),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
        Text(
            text = "left to spend",
            style = TextStyle(color = ColorProvider(colors.textMuted), fontSize = 10.sp)
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        GlanceProgressBar(
            fraction = progress,
            trackColor = colors.hairline,
            progressColor = if (remaining < 0.0) colors.warning else colors.accent
        )
    }
}

/**
 * Expanded layout (~4x2 medium size bucket): everything in Small, plus top 3 categories BY
 * ALLOCATION (descending), each with name, remaining amount, and a mini progress bar.
 *
 * WEEKLY ONLY: an "Add spend" text button below the rows launches QuickAddSpendActivity's
 * floating popup, where the user picks a category from a dropdown and types an exact amount.
 * Not shown for the monthly widget, which stays display-only per spec.
 */
@Composable
fun MediumBudgetWidgetContent(
    period: BudgetPeriod,
    state: BudgetState,
    colors: GlanceBudgetColors,
    maskAmounts: Boolean = false
) {
    val periodBudget = if (period == BudgetPeriod.WEEKLY) state.weekly else state.monthly
    val pool = BudgetCalculations.periodPool(period, state.monthlySalary, state.savingsGoalPercent, state.weeklyAllotment)
    val remaining = BudgetCalculations.remainingPool(pool, periodBudget.categories)
    val progress = BudgetCalculations.overallProgressFraction(pool, periodBudget.categories)
    val topCategories = BudgetCalculations.topCategoriesByAllocation(pool, periodBudget.categories, 3)
    val isWeekly = period == BudgetPeriod.WEEKLY

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.surface)
            .cornerRadius(14.dp)
            .padding(14.dp)
            .clickable(widgetTapIntentAction(period))
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = periodDisplayLabel(period, state),
                    style = TextStyle(color = ColorProvider(colors.textMuted), fontSize = 12.sp)
                )
                Text(
                    text = displayAmount(remaining, maskAmounts),
                    style = TextStyle(
                        color = ColorProvider(if (remaining < 0.0) colors.warning else colors.textPrimary),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "left to spend",
                    style = TextStyle(color = ColorProvider(colors.textMuted), fontSize = 10.sp)
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        GlanceProgressBar(
            fraction = progress,
            trackColor = colors.hairline,
            progressColor = if (remaining < 0.0) colors.warning else colors.accent
        )
        Spacer(modifier = GlanceModifier.height(10.dp))

        topCategories.forEach { category ->
            MiniCategoryRow(
                category = category,
                periodPool = pool,
                colors = colors,
                maskAmounts = maskAmounts
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
        }

        if (isWeekly) {
            Text(
                text = "Add spend",
                style = TextStyle(
                    color = ColorProvider(colors.accent),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.clickable(actionStartActivity<QuickAddSpendActivity>())
            )
        }
    }
}

@Composable
private fun MiniCategoryRow(
    category: Category,
    periodPool: Double,
    colors: GlanceBudgetColors,
    maskAmounts: Boolean = false
) {
    val allocation = BudgetCalculations.categoryAllocation(periodPool, category)
    val remaining = BudgetCalculations.categoryRemaining(periodPool, category)
    val progress = BudgetCalculations.progressFraction(category.spent, allocation)
    val isOver = remaining < 0.0

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = TextStyle(color = ColorProvider(colors.textPrimary), fontSize = 12.sp),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = displayAmount(remaining, maskAmounts),
                style = TextStyle(
                    color = ColorProvider(if (isOver) colors.warning else colors.accent),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(3.dp))
        GlanceProgressBar(
            fraction = progress,
            trackColor = colors.hairline,
            progressColor = if (isOver) colors.warning else colors.accent,
            height = 4.dp
        )
    }
}

/**
 * Minimal progress bar for Glance. Glance's `GlanceModifier.fillMaxWidth()` is boolean-only (it
 * fills 100% of available width) - there is no fractional-width modifier equivalent to Compose's
 * `Modifier.fillMaxWidth(Float)`. To render a fractional-width fill we instead split a full-width
 * Row into two nested Rows sized by integer `defaultWeight()` (filled portion vs. remainder),
 * using a 0-1000 integer scale for reasonable sub-percent resolution.
 */
@Composable
fun GlanceProgressBar(
    fraction: Float,
    trackColor: Color,
    progressColor: Color,
    height: Dp = 6.dp
) {
    val filledWeight = (fraction.coerceIn(0f, 1f) * 1000).toInt().coerceAtLeast(0)
    val remainderWeight = 1000 - filledWeight

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height)
            .cornerRadius(height / 2)
            .background(trackColor)
    ) {
        if (filledWeight > 0) {
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(height)
                    .background(progressColor)
            ) {}
        }
        if (remainderWeight > 0) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}
