package com.budgettracker.app.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.IsoFields

/**
 * Period-stamp computation. Pure aside from java.time - no Android imports.
 *
 *  - Weekly: ISO-8601 week-of-year + week-year, formatted like "2026-W27"
 *    (IsoFields.WEEK_BASED_YEAR + IsoFields.WEEK_OF_WEEK_BASED_YEAR, week number zero-padded to 2 digits).
 *  - Monthly: year + 2-digit month, formatted like "2026-M07".
 */
object PeriodStamps {

    /** Computes the current ISO week stamp, e.g. "2026-W27", for the given date (default: today). */
    fun currentWeekStamp(date: LocalDate = LocalDate.now()): String {
        val weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR)
        val weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "%d-W%02d".format(weekBasedYear, weekOfYear)
    }

    /** Computes the current year-month stamp, e.g. "2026-M07", for the given date (default: today). */
    fun currentMonthStamp(date: LocalDate = LocalDate.now()): String {
        return "%d-M%02d".format(date.year, date.monthValue)
    }

    /** Convenience overloads anchored to a specific zone's "today" (used by the rollover worker). */
    fun currentWeekStamp(zoneId: ZoneId): String = currentWeekStamp(LocalDate.now(zoneId))
    fun currentMonthStamp(zoneId: ZoneId): String = currentMonthStamp(LocalDate.now(zoneId))

    /**
     * Milliseconds (epoch) until the next local midnight in the given zone. Used to compute the
     * `initialDelay` for the self-rescheduling WorkManager rollover worker.
     */
    fun millisUntilNextMidnight(zoneId: ZoneId = ZoneId.systemDefault(), now: LocalDateTime = LocalDateTime.now(zoneId)): Long {
        val nextMidnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT)
        val nowZoned = now.atZone(zoneId)
        val nextMidnightZoned = nextMidnight.atZone(zoneId)
        return java.time.Duration.between(nowZoned, nextMidnightZoned).toMillis()
    }

    /** Parses the month name (e.g. "July 2026") out of a monthly stamp like "2026-M07". */
    fun monthLabelFromStamp(stamp: String): String {
        // Expected format: "YYYY-Mxx"
        val parts = stamp.split("-M")
        if (parts.size != 2) return stamp
        val year = parts[0].toIntOrNull() ?: return stamp
        val month = parts[1].toIntOrNull() ?: return stamp
        if (month !in 1..12) return stamp
        val monthName = java.time.Month.of(month)
            .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
        return "$monthName $year"
    }
}
