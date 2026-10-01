package md.alexlab.finpulse.domain.engine

import md.alexlab.finpulse.domain.model.CustomIntervalUnit
import md.alexlab.finpulse.domain.model.OccurrenceStatus
import md.alexlab.finpulse.domain.model.PaymentFrequency
import md.alexlab.finpulse.domain.model.RecurringTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object RecurringDateEngine {

    fun calculateNextDueDate(
        currentDueDateMillis: Long,
        frequency: PaymentFrequency,
        anchorDayOfMonth: Int = 1,
        customIntervalValue: Int = 1,
        customIntervalUnit: CustomIntervalUnit = CustomIntervalUnit.MONTHS,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val currentZdt = Instant.ofEpochMilli(currentDueDateMillis).atZone(zoneId)
        val currentDate = currentZdt.toLocalDate()
        val timeOfDay = currentZdt.toLocalTime()

        val nextDate = calculateNextLocalDate(
            currentDate = currentDate,
            frequency = frequency,
            anchorDayOfMonth = anchorDayOfMonth,
            customIntervalValue = customIntervalValue,
            customIntervalUnit = customIntervalUnit
        )

        return nextDate.atTime(timeOfDay).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun calculateNextLocalDate(
        currentDate: LocalDate,
        frequency: PaymentFrequency,
        anchorDayOfMonth: Int = currentDate.dayOfMonth,
        customIntervalValue: Int = 1,
        customIntervalUnit: CustomIntervalUnit = CustomIntervalUnit.MONTHS
    ): LocalDate {
        val effectiveAnchor = if (anchorDayOfMonth in 1..31) anchorDayOfMonth else currentDate.dayOfMonth

        return when (frequency) {
            PaymentFrequency.DAILY -> currentDate.plusDays(1)
            PaymentFrequency.WEEKLY -> currentDate.plusWeeks(1)
            PaymentFrequency.BI_WEEKLY -> currentDate.plusWeeks(2)
            PaymentFrequency.MONTHLY -> addMonthsWithAnchor(currentDate, 1, effectiveAnchor)
            PaymentFrequency.QUARTERLY -> addMonthsWithAnchor(currentDate, 3, effectiveAnchor)
            PaymentFrequency.YEARLY -> addYearsWithAnchor(currentDate, 1, effectiveAnchor)
            PaymentFrequency.CUSTOM -> {
                val interval = maxOf(1, customIntervalValue).toLong()
                when (customIntervalUnit) {
                    CustomIntervalUnit.DAYS -> currentDate.plusDays(interval)
                    CustomIntervalUnit.WEEKS -> currentDate.plusWeeks(interval)
                    CustomIntervalUnit.MONTHS -> addMonthsWithAnchor(currentDate, interval, effectiveAnchor)
                    CustomIntervalUnit.YEARS -> addYearsWithAnchor(currentDate, interval, effectiveAnchor)
                }
            }
        }
    }

    private fun addMonthsWithAnchor(date: LocalDate, monthsToAdd: Long, anchorDay: Int): LocalDate {
        val targetMonth = date.plusMonths(monthsToAdd)
        val maxDaysInTargetMonth = targetMonth.lengthOfMonth()
        val targetDay = minOf(anchorDay, maxDaysInTargetMonth)
        return targetMonth.withDayOfMonth(targetDay)
    }

    private fun addYearsWithAnchor(date: LocalDate, yearsToAdd: Long, anchorDay: Int): LocalDate {
        val targetYearDate = date.plusYears(yearsToAdd)
        val maxDaysInTargetMonth = targetYearDate.lengthOfMonth()
        val targetDay = minOf(anchorDay, maxDaysInTargetMonth)
        return targetYearDate.withDayOfMonth(targetDay)
    }

    /**
     * Projects upcoming due date timestamps for a recurring rule within a given window in days.
     */
    fun projectOccurrences(
        rule: RecurringTransaction,
        startFromMillis: Long,
        windowDays: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<Long> {
        val result = mutableListOf<Long>()
        val endLimitMillis = startFromMillis + (windowDays * 86_400_000L)

        var currentDueDate = rule.nextDueDate

        // If the rule is already due or overdue before startFrom, include it as current upcoming
        while (currentDueDate <= endLimitMillis) {
            result.add(currentDueDate)

            currentDueDate = calculateNextDueDate(
                currentDueDateMillis = currentDueDate,
                frequency = rule.frequency,
                anchorDayOfMonth = rule.anchorDayOfMonth,
                customIntervalValue = rule.customIntervalValue,
                customIntervalUnit = rule.customIntervalUnit,
                zoneId = zoneId
            )

            // Guard against infinite loop if frequency is invalid
            if (result.size >= 100) break
        }

        return result
    }

    fun determineStatus(
        dueDateMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        isPaid: Boolean = false,
        isSkipped: Boolean = false,
        isGenerated: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): OccurrenceStatus {
        if (isPaid) return OccurrenceStatus.PAID
        if (isSkipped) return OccurrenceStatus.SKIPPED

        val dueDay = Instant.ofEpochMilli(dueDateMillis).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

        return when {
            dueDay.isBefore(today) -> OccurrenceStatus.OVERDUE
            isGenerated -> OccurrenceStatus.GENERATED
            else -> OccurrenceStatus.EXPECTED
        }
    }
}
