package com.finpulse.app.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class TimePeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    THREE_MONTHS("3 Months"),
    SIX_MONTHS("6 Months"),
    YEAR("Year"),
    ALL("All Time");

    fun toDateRange(now: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val (start, end) = when (this) {
            WEEK -> {
                val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                startOfWeek to endOfWeek
            }
            MONTH -> {
                val startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth())
                val endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
                startOfMonth to endOfMonth
            }
            THREE_MONTHS -> {
                val startOfPeriod = now.minusMonths(2).with(TemporalAdjusters.firstDayOfMonth())
                val endOfPeriod = now.with(TemporalAdjusters.lastDayOfMonth())
                startOfPeriod to endOfPeriod
            }
            SIX_MONTHS -> {
                val startOfPeriod = now.minusMonths(5).with(TemporalAdjusters.firstDayOfMonth())
                val endOfPeriod = now.with(TemporalAdjusters.lastDayOfMonth())
                startOfPeriod to endOfPeriod
            }
            YEAR -> {
                val startOfYear = now.with(TemporalAdjusters.firstDayOfYear())
                val endOfYear = now.with(TemporalAdjusters.lastDayOfYear())
                startOfYear to endOfYear
            }
            ALL -> {
                val startEpoch = LocalDate.of(2000, 1, 1)
                val endEpoch = now.plusYears(10)
                startEpoch to endEpoch
            }
        }

        val startMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = end.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
        return startMillis to endMillis
    }
}
