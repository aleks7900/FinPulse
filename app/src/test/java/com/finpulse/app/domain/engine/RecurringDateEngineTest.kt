package com.finpulse.app.domain.engine

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.CustomIntervalUnit
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurringDateEngineTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testMonthEndClampingAndAnchorRestoration_NonLeapYear() {
        // Jan 31 with anchor 31 -> Feb 28 in non-leap year (2023)
        val jan31 = LocalDate.of(2023, 1, 31)
        val feb = RecurringDateEngine.calculateNextLocalDate(
            currentDate = jan31,
            frequency = PaymentFrequency.MONTHLY,
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2023, 2, 28), feb)

        // Advancing from Feb 28 with anchor 31 MUST restore March 31 (not March 28!)
        val mar = RecurringDateEngine.calculateNextLocalDate(
            currentDate = feb,
            frequency = PaymentFrequency.MONTHLY,
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2023, 3, 31), mar)

        // Advancing from Mar 31 with anchor 31 -> Apr 30
        val apr = RecurringDateEngine.calculateNextLocalDate(
            currentDate = mar,
            frequency = PaymentFrequency.MONTHLY,
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2023, 4, 30), apr)

        // Advancing from Apr 30 with anchor 31 -> May 31
        val may = RecurringDateEngine.calculateNextLocalDate(
            currentDate = apr,
            frequency = PaymentFrequency.MONTHLY,
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2023, 5, 31), may)
    }

    @Test
    fun testMonthEndClampingAndAnchorRestoration_LeapYear() {
        // Jan 31 with anchor 31 in leap year 2024 -> Feb 29
        val jan31 = LocalDate.of(2024, 1, 31)
        val feb = RecurringDateEngine.calculateNextLocalDate(
            currentDate = jan31,
            frequency = PaymentFrequency.MONTHLY,
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2024, 2, 29), feb)

        // Advancing from Feb 29 with anchor 31 MUST restore March 31
        val mar = RecurringDateEngine.calculateNextLocalDate(
            currentDate = feb,
            frequency = PaymentFrequency.MONTHLY,
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2024, 3, 31), mar)
    }

    @Test
    fun testLeapDayStart_YearlyAdvancement() {
        // Starting on Feb 29, 2024 with yearly frequency
        val feb29 = LocalDate.of(2024, 2, 29)
        val year2025 = RecurringDateEngine.calculateNextLocalDate(
            currentDate = feb29,
            frequency = PaymentFrequency.YEARLY,
            anchorDayOfMonth = 29
        )
        assertEquals(LocalDate.of(2025, 2, 28), year2025)

        // 2025 to 2028: leap year 2028 should restore the 29th
        val year2028 = RecurringDateEngine.calculateNextLocalDate(
            currentDate = LocalDate.of(2027, 2, 28),
            frequency = PaymentFrequency.YEARLY,
            anchorDayOfMonth = 29
        )
        assertEquals(LocalDate.of(2028, 2, 29), year2028)
    }

    @Test
    fun testDailyFrequencyAcrossLeapYearBoundary() {
        val feb28_2024 = LocalDate.of(2024, 2, 28)
        val feb29_2024 = RecurringDateEngine.calculateNextLocalDate(
            currentDate = feb28_2024,
            frequency = PaymentFrequency.DAILY
        )
        assertEquals(LocalDate.of(2024, 2, 29), feb29_2024)

        val mar1_2024 = RecurringDateEngine.calculateNextLocalDate(
            currentDate = feb29_2024,
            frequency = PaymentFrequency.DAILY
        )
        assertEquals(LocalDate.of(2024, 3, 1), mar1_2024)
    }

    @Test
    fun testWeeklyAndBiWeeklyFrequency() {
        val start = LocalDate.of(2026, 9, 1) // Tuesday

        val weekly = RecurringDateEngine.calculateNextLocalDate(
            currentDate = start,
            frequency = PaymentFrequency.WEEKLY
        )
        assertEquals(LocalDate.of(2026, 9, 8), weekly)

        val biweekly = RecurringDateEngine.calculateNextLocalDate(
            currentDate = start,
            frequency = PaymentFrequency.BI_WEEKLY
        )
        assertEquals(LocalDate.of(2026, 9, 15), biweekly)
    }

    @Test
    fun testQuarterlyFrequencyWithAnchor() {
        // Nov 30 with anchor 30 -> Feb 28 (non leap 2023)
        val nov30 = LocalDate.of(2022, 11, 30)
        val feb28 = RecurringDateEngine.calculateNextLocalDate(
            currentDate = nov30,
            frequency = PaymentFrequency.QUARTERLY,
            anchorDayOfMonth = 30
        )
        assertEquals(LocalDate.of(2023, 2, 28), feb28)

        // Next quarter from Feb 28 with anchor 30 -> May 30
        val may30 = RecurringDateEngine.calculateNextLocalDate(
            currentDate = feb28,
            frequency = PaymentFrequency.QUARTERLY,
            anchorDayOfMonth = 30
        )
        assertEquals(LocalDate.of(2023, 5, 30), may30)
    }

    @Test
    fun testCustomIntervals() {
        val start = LocalDate.of(2026, 1, 15)

        // 10 Days
        val tenDays = RecurringDateEngine.calculateNextLocalDate(
            currentDate = start,
            frequency = PaymentFrequency.CUSTOM,
            customIntervalValue = 10,
            customIntervalUnit = CustomIntervalUnit.DAYS
        )
        assertEquals(LocalDate.of(2026, 1, 25), tenDays)

        // 3 Weeks
        val threeWeeks = RecurringDateEngine.calculateNextLocalDate(
            currentDate = start,
            frequency = PaymentFrequency.CUSTOM,
            customIntervalValue = 3,
            customIntervalUnit = CustomIntervalUnit.WEEKS
        )
        assertEquals(LocalDate.of(2026, 2, 5), threeWeeks)

        // 2 Months with anchor 31 (Jan 31 -> Mar 31)
        val jan31 = LocalDate.of(2026, 1, 31)
        val twoMonths = RecurringDateEngine.calculateNextLocalDate(
            currentDate = jan31,
            frequency = PaymentFrequency.CUSTOM,
            anchorDayOfMonth = 31,
            customIntervalValue = 2,
            customIntervalUnit = CustomIntervalUnit.MONTHS
        )
        assertEquals(LocalDate.of(2026, 3, 31), twoMonths)
    }

    @Test
    fun testTimePreservationAcrossBoundaries() {
        val zdt = ZonedDateTime.of(2026, 1, 31, 14, 30, 0, 0, zoneId)
        val millis = zdt.toInstant().toEpochMilli()

        val nextMillis = RecurringDateEngine.calculateNextDueDate(
            currentDueDateMillis = millis,
            frequency = PaymentFrequency.MONTHLY,
            anchorDayOfMonth = 31,
            zoneId = zoneId
        )

        val nextZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextMillis), zoneId)
        assertEquals(2026, nextZdt.year)
        assertEquals(2, nextZdt.monthValue)
        assertEquals(28, nextZdt.dayOfMonth)
        assertEquals(14, nextZdt.hour)
        assertEquals(30, nextZdt.minute)
    }

    @Test
    fun testDetermineStatusDistinctions() {
        val now = ZonedDateTime.of(2026, 9, 26, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()

        val yesterday = ZonedDateTime.of(2026, 9, 25, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        val today = ZonedDateTime.of(2026, 9, 26, 15, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        val tomorrow = ZonedDateTime.of(2026, 9, 27, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()

        // Critical: Do NOT automatically mark a bill as paid merely because its due date passed.
        // It must be OVERDUE!
        val overdueStatus = RecurringDateEngine.determineStatus(
            dueDateMillis = yesterday,
            nowMillis = now,
            isPaid = false,
            isSkipped = false,
            zoneId = zoneId
        )
        assertEquals(OccurrenceStatus.OVERDUE, overdueStatus)

        // Today is EXPECTED
        val todayStatus = RecurringDateEngine.determineStatus(
            dueDateMillis = today,
            nowMillis = now,
            isPaid = false,
            isSkipped = false,
            zoneId = zoneId
        )
        assertEquals(OccurrenceStatus.EXPECTED, todayStatus)

        // Tomorrow is EXPECTED
        val tomorrowStatus = RecurringDateEngine.determineStatus(
            dueDateMillis = tomorrow,
            nowMillis = now,
            isPaid = false,
            isSkipped = false,
            zoneId = zoneId
        )
        assertEquals(OccurrenceStatus.EXPECTED, tomorrowStatus)

        // Paid takes precedence
        val paidStatus = RecurringDateEngine.determineStatus(
            dueDateMillis = yesterday,
            nowMillis = now,
            isPaid = true,
            isSkipped = false,
            zoneId = zoneId
        )
        assertEquals(OccurrenceStatus.PAID, paidStatus)

        // Skipped takes precedence
        val skippedStatus = RecurringDateEngine.determineStatus(
            dueDateMillis = yesterday,
            nowMillis = now,
            isPaid = false,
            isSkipped = true,
            zoneId = zoneId
        )
        assertEquals(OccurrenceStatus.SKIPPED, skippedStatus)

        // Generated status
        val generatedStatus = RecurringDateEngine.determineStatus(
            dueDateMillis = tomorrow,
            nowMillis = now,
            isPaid = false,
            isSkipped = false,
            isGenerated = true,
            zoneId = zoneId
        )
        assertEquals(OccurrenceStatus.GENERATED, generatedStatus)
    }

    @Test
    fun testProjectOccurrencesForUpcomingWindow() {
        val startZdt = ZonedDateTime.of(2026, 9, 1, 10, 0, 0, 0, zoneId)
        val startMillis = startZdt.toInstant().toEpochMilli()

        val weeklyRule = RecurringTransaction(
            id = "rule-weekly",
            title = "Weekly Gym",
            amount = Money(2500L, "USD"),
            type = TransactionType.EXPENSE,
            accountId = "acc-1",
            categoryId = "cat-fitness",
            frequency = PaymentFrequency.WEEKLY,
            nextDueDate = startMillis
        )

        // In a 30-day window starting Sept 1: Sept 1, 8, 15, 22, 29 (5 occurrences)
        val occurrences = RecurringDateEngine.projectOccurrences(
            rule = weeklyRule,
            startFromMillis = startMillis,
            windowDays = 30,
            zoneId = zoneId
        )

        assertEquals(5, occurrences.size)
        assertEquals(startMillis, occurrences[0])
    }

    @Test
    fun testSubscriptionMonthlyAndAnnualCostCalculations() {
        val monthlyNetflix = RecurringTransaction(
            id = "sub-netflix",
            title = "Netflix",
            amount = Money(1599L, "USD"), // $15.99 / mo
            type = TransactionType.EXPENSE,
            accountId = "acc-1",
            categoryId = "cat-ent",
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = 0L,
            isSubscription = true
        )
        assertEquals(1599L, monthlyNetflix.calculateMonthlyCost().amountMinor)
        assertEquals(1599L * 12, monthlyNetflix.calculateAnnualCost().amountMinor)

        val yearlyAmazon = RecurringTransaction(
            id = "sub-prime",
            title = "Amazon Prime",
            amount = Money(13900L, "USD"), // $139.00 / yr
            type = TransactionType.EXPENSE,
            accountId = "acc-1",
            categoryId = "cat-sub",
            frequency = PaymentFrequency.YEARLY,
            nextDueDate = 0L,
            isSubscription = true
        )
        assertEquals(13900L, yearlyAmazon.calculateAnnualCost().amountMinor)
        assertEquals(13900L / 12, yearlyAmazon.calculateMonthlyCost().amountMinor)

        val weeklyPaper = RecurringTransaction(
            id = "sub-news",
            title = "Newspaper",
            amount = Money(500L, "USD"), // $5.00 / week
            type = TransactionType.EXPENSE,
            accountId = "acc-1",
            categoryId = "cat-news",
            frequency = PaymentFrequency.WEEKLY,
            nextDueDate = 0L,
            isSubscription = true
        )
        assertEquals(500L * 52, weeklyPaper.calculateAnnualCost().amountMinor)
        assertEquals(2167L, weeklyPaper.calculateMonthlyCost().amountMinor)
    }
}
