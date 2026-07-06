package com.budgettracker.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
 * Weekly period Glance widget. Uses `SizeMode.Responsive` with two declared sizes (small ~2x1,
 * medium ~4x2) so a single GlanceAppWidget renders either the compact or expanded layout
 * depending on the size the user resizes/places it at - see WidgetContent.kt for the shared
 * composables and res/xml/weekly_widget_info.xml for the matching AppWidgetProviderInfo sizes.
 */
class WeeklyBudgetWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(100.dp, 60.dp)
        private val MEDIUM = DpSize(220.dp, 120.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = BudgetRepository.getInstance(context)

        // Run the rollover check on every provideGlance invocation, per spec, so staleness is
        // caught even if the app hasn't been opened and the midnight worker hasn't ticked yet.
        // First-install / no-DataStore-value-yet state naturally falls out of
        // BudgetRepository.budgetState, which seeds BudgetDefaults if nothing has been persisted.
        val state = repository.applyRolloverIfNeeded()
        val colors = glanceColorsFor(context)
        // One-shot read (not a collected Flow): provideGlance already reruns on every DataStore
        // change via BudgetApplication's updateAll() observer, so this stays reactive without
        // polling - see README "Security" section on widget masking.
        val maskAmounts = repository.maskWidgetAmounts.first()

        provideContent {
            val size = androidx.glance.LocalSize.current
            if (size.width >= MEDIUM.width) {
                MediumBudgetWidgetContent(
                    period = BudgetPeriod.WEEKLY,
                    state = state,
                    colors = colors,
                    maskAmounts = maskAmounts
                )
            } else {
                SmallBudgetWidgetContent(period = BudgetPeriod.WEEKLY, state = state, colors = colors, maskAmounts = maskAmounts)
            }
        }
    }
}
