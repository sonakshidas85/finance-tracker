package com.budgettracker.app.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.budgettracker.app.data.BudgetRepository
import com.budgettracker.app.widget.MonthlyBudgetWidget
import com.budgettracker.app.widget.WeeklyBudgetWidget

/**
 * Self-rescheduling one-time worker. On each run it:
 *  1. Runs the rollover check (weekly ISO-week + monthly year-month period-stamp comparison);
 *     zeroes `spent` on stale periods, updates the stamp, never touches `percent`.
 *  2. Calls GlanceAppWidgetManager to update all instances of both widgets so any home-screen
 *     placements immediately reflect the reset.
 *  3. Reschedules itself for the following local midnight.
 *
 * This same rollover check is ALSO run synchronously on app open (BudgetApplication/ViewModel
 * init) and on every widget provideGlance/update, so staleness is caught even if the device was
 * powered off at midnight and this worker's delay elapsed while asleep.
 */
class RolloverWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = BudgetRepository.getInstance(applicationContext)
            repository.applyRolloverIfNeeded()

            // Push updates to all placed instances of both widgets regardless of whether rollover
            // actually changed anything this tick - cheap, and guarantees widgets never show stale
            // data for more than one worker cycle.
            WeeklyBudgetWidget().updateAll(applicationContext)
            MonthlyBudgetWidget().updateAll(applicationContext)

            // Reschedule for the next local midnight - this is what makes the worker
            // "self-rescheduling" rather than relying on a periodic (15-min-minimum) request.
            WorkScheduler.scheduleNextMidnightRollover(applicationContext)

            Result.success()
        } catch (t: Throwable) {
            // Even on failure, try to get back on the midnight schedule rather than silently
            // dying - otherwise a single transient failure would end rollover checks forever.
            runCatching { WorkScheduler.scheduleNextMidnightRollover(applicationContext) }
            Result.retry()
        }
    }

    companion object {
        const val INPUT_SCHEDULED_AT = "scheduled_at_millis"
    }
}
