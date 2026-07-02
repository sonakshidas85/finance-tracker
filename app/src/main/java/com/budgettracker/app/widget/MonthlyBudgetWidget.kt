package com.budgettracker.app.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.budgettracker.app.data.BudgetPeriod
import com.budgettracker.app.data.BudgetRepository
import kotlinx.coroutines.flow.first

/**
 * Monthly period Glance widget - structurally identical to WeeklyBudgetWidget aside from which
 * PeriodBudget it reads/renders. Kept as a SEPARATE GlanceAppWidget (rather than one toggleable
 * widget) per the architecture decision documented in README "Assumptions & tradeoffs".
 */
class MonthlyBudgetWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(100.dp, 60.dp)
        private val MEDIUM = DpSize(220.dp, 120.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = BudgetRepository.getInstance(context)
        val state = repository.applyRolloverIfNeeded()
        val colors = glanceColorsFor(context)
        // One-shot read (not a collected Flow): provideGlance already reruns on every DataStore
        // change via BudgetApplication's updateAll() observer, so this stays reactive without
        // polling - see README "Security" section on widget masking.
        val maskAmounts = repository.maskWidgetAmounts.first()

        provideContent {
            val size = androidx.glance.LocalSize.current
            if (size.width >= MEDIUM.width) {
                MediumBudgetWidgetContent(period = BudgetPeriod.MONTHLY, state = state, colors = colors, maskAmounts = maskAmounts)
            } else {
                SmallBudgetWidgetContent(period = BudgetPeriod.MONTHLY, state = state, colors = colors, maskAmounts = maskAmounts)
            }
        }
    }
}
