// Copy/adapted from app/src/main/java/com/budgettracker/app/data/PeriodStamps.kt - identical
// logic (only java.time imports, no Android imports existed in the original).

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.IsoFields

object PeriodStamps {

    fun currentWeekStamp(date: LocalDate = LocalDate.now()): String {
        val weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR)
        val weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "%d-W%02d".format(weekBasedYear, weekOfYear)
    }

    fun currentMonthStamp(date: LocalDate = LocalDate.now()): String {
        return "%d-M%02d".format(date.year, date.monthValue)
    }

    fun currentWeekStamp(zoneId: ZoneId): String = currentWeekStamp(LocalDate.now(zoneId))
    fun currentMonthStamp(zoneId: ZoneId): String = currentMonthStamp(LocalDate.now(zoneId))

    fun millisUntilNextMidnight(zoneId: ZoneId = ZoneId.systemDefault(), now: LocalDateTime = LocalDateTime.now(zoneId)): Long {
        val nextMidnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT)
        val nowZoned = now.atZone(zoneId)
        val nextMidnightZoned = nextMidnight.atZone(zoneId)
        return java.time.Duration.between(nowZoned, nextMidnightZoned).toMillis()
    }

    fun monthLabelFromStamp(stamp: String): String {
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
