package com.budgettracker.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.budgettracker.app.data.PeriodStamps
import java.util.concurrent.TimeUnit

/**
 * Schedules the self-rescheduling midnight rollover worker.
 *
 * We deliberately use a ONE-TIME WorkRequest with an `initialDelay` computed to the next local
 * midnight, rather than:
 *  - a `PeriodicWorkRequest`, whose minimum interval (15 minutes) can never land exactly on
 *    midnight and drifts over time, or
 *  - an exact `AlarmManager` alarm, which needs `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` on
 *    Android 12+ (extra permission friction for a feature that doesn't need to-the-second
 *    precision - a few minutes of slack after midnight is fine, and is additionally guarded by
 *    the synchronous rollover check on app open / widget update).
 *
 * The worker itself (see RolloverWorker) reschedules the NEXT midnight run as its last step,
 * so this function only needs to be called once (from BudgetApplication.onCreate).
 */
object WorkScheduler {

    const val UNIQUE_WORK_NAME = "budget_rollover_midnight"

    fun scheduleNextMidnightRollover(context: Context) {
        val delayMillis = PeriodStamps.millisUntilNextMidnight()
        val request = OneTimeWorkRequestBuilder<RolloverWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(RolloverWorker.INPUT_SCHEDULED_AT to System.currentTimeMillis()))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            // REPLACE: if the app is opened again before midnight, we want the freshly computed
            // delay (in case the system clock changed) to win over whatever was queued before.
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Runs an immediate one-off rollover+widget-update pass (used right after data edits, optionally). */
    fun runImmediateRolloverCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<RolloverWorker>()
            .setInputData(workDataOf(RolloverWorker.INPUT_SCHEDULED_AT to System.currentTimeMillis()))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
