package com.budgettracker.app

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.budgettracker.app.data.BudgetRepository
import com.budgettracker.app.widget.MonthlyBudgetWidget
import com.budgettracker.app.widget.WeeklyBudgetWidget
import com.budgettracker.app.work.WorkScheduler
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Application class responsible for:
 *  1. Running the rollover check synchronously on app open (belt-and-suspenders alongside the
 *     WorkManager worker, in case the device was off at midnight).
 *  2. Scheduling the first self-rescheduling midnight rollover WorkManager request.
 *  3. Keeping a small always-alive observer (scoped to the app's process lifecycle) on the
 *     repository's `Flow<BudgetState>` so that ANY edit made in the app (salary, savings %,
 *     categories, spent) immediately pushes a GlanceAppWidgetManager update to both widgets -
 *     no manual polling required from the UI layer.
 *  4. Same as (3) but for the `maskWidgetAmounts` preference, so toggling "Mask amounts on
 *     home-screen widgets" in Settings immediately re-renders both widgets masked/unmasked.
 */
class BudgetApplication : Application() {

    lateinit var repository: BudgetRepository
        private set

    companion object {
        /**
         * Shared "push a Glance update to both weekly widget instances" helper - the SAME
         * mechanism this class's own always-alive DataStore-Flow observer already uses
         * (`updateAll` on each GlanceAppWidget). Reused by `QuickAddSpendActivity` after writing a
         * spend so the widget shows the new number essentially immediately, without inventing a
         * second update path. Only the weekly widget is refreshed here - the monthly widget is
         * unaffected by a weekly spend.
         */
        suspend fun refreshWeeklyWidget(context: Context) {
            WeeklyBudgetWidget().updateAll(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = BudgetRepository.getInstance(this)

        // Use the process lifecycle scope: it lives as long as the app process is alive (not tied
        // to any single Activity), which is exactly the "small always-alive observer scoped
        // appropriately" the widget-update path needs.
        val appScope = ProcessLifecycleOwner.get().lifecycleScope

        appScope.launch {
            // 1) Synchronous rollover check on app open - catches staleness even if the device
            //    was off/asleep when the WorkManager worker's midnight delay elapsed.
            repository.applyRolloverIfNeeded()
            WeeklyBudgetWidget().updateAll(this@BudgetApplication)
            MonthlyBudgetWidget().updateAll(this@BudgetApplication)

            // 2) Schedule (or reschedule, replacing any stale pending request) the next midnight
            //    rollover worker run.
            WorkScheduler.scheduleNextMidnightRollover(this@BudgetApplication)
        }

        appScope.launch {
            // 3) Always-alive observer: any BudgetState change (salary, savings %, categories,
            //    spent) immediately pushes a GlanceAppWidgetManager update to both widgets.
            // `drop(1)` skips the initial emission we already handled with updateAll() above,
            // avoiding a redundant double-update on cold start.
            repository.budgetState.drop(1).collect {
                WeeklyBudgetWidget().updateAll(this@BudgetApplication)
                MonthlyBudgetWidget().updateAll(this@BudgetApplication)
            }
        }

        appScope.launch {
            // 4) Always-alive observer for the widget-masking preference - same drop(1)/updateAll
            //    pattern as (3), kept as a separate collector since it's a different Flow.
            repository.maskWidgetAmounts.drop(1).collect {
                WeeklyBudgetWidget().updateAll(this@BudgetApplication)
                MonthlyBudgetWidget().updateAll(this@BudgetApplication)
            }
        }
    }
}
