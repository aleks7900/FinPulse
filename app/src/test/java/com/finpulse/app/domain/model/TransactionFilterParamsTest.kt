package com.finpulse.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class TransactionFilterParamsTest {

    @Test
    fun testActiveFilterCount_DefaultParams_ReturnsZero() {
        val params = TransactionFilterParams()
        assertEquals(0, params.activeFilterCount)
    }

    @Test
    fun testActiveFilterCount_EachFilterIncrementsCount() {
        var params = TransactionFilterParams()
        assertEquals(0, params.activeFilterCount)

        params = params.copy(query = "coffee")
        assertEquals(1, params.activeFilterCount)

        params = params.copy(type = TransactionType.EXPENSE)
        assertEquals(2, params.activeFilterCount)

        params = params.copy(accountId = "acc_1")
        assertEquals(3, params.activeFilterCount)

        params = params.copy(categoryId = "cat_1")
        assertEquals(4, params.activeFilterCount)

        params = params.copy(dateRangePreset = DateRangePreset.THIS_MONTH)
        assertEquals(5, params.activeFilterCount)

        params = params.copy(minAmountMinor = 1000L)
        assertEquals(6, params.activeFilterCount)

        // Setting maxAmountMinor as well shouldn't double-count the amount range
        params = params.copy(maxAmountMinor = 5000L)
        assertEquals(6, params.activeFilterCount)

        params = params.copy(currencyCode = "USD")
        assertEquals(7, params.activeFilterCount)

        params = params.copy(status = TransactionStatusFilter.CONFIRMED)
        assertEquals(8, params.activeFilterCount)
    }

    @Test
    fun testEffectiveDateRange_ThisMonth() {
        val params = TransactionFilterParams(dateRangePreset = DateRangePreset.THIS_MONTH)
        val range = params.effectiveDateRange
        val start = range.first
        val end = range.second

        val ym = YearMonth.now()
        val expectedStart = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val expectedEnd = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
        assertTrue(start != null && end != null && start < end)
    }

    @Test
    fun testEffectiveDateRange_LastMonth() {
        val params = TransactionFilterParams(dateRangePreset = DateRangePreset.LAST_MONTH)
        val range = params.effectiveDateRange
        val start = range.first
        val end = range.second

        val ym = YearMonth.now().minusMonths(1)
        val expectedStart = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val expectedEnd = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
        assertTrue(start != null && end != null && start < end)
    }

    @Test
    fun testEffectiveDateRange_ThisYear() {
        val params = TransactionFilterParams(dateRangePreset = DateRangePreset.THIS_YEAR)
        val range = params.effectiveDateRange
        val start = range.first
        val end = range.second

        val year = LocalDate.now().year
        val expectedStart = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val expectedEnd = LocalDate.of(year, 12, 31).atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
        assertTrue(start != null && end != null && start < end)
    }

    @Test
    fun testEffectiveDateRange_Custom() {
        val customStart = 1700000000000L
        val customEnd = 1700500000000L
        val params = TransactionFilterParams(
            dateRangePreset = DateRangePreset.CUSTOM,
            customStartDate = customStart,
            customEndDate = customEnd
        )
        val range = params.effectiveDateRange

        assertEquals(customStart, range.first)
        assertEquals(customEnd, range.second)
    }

    @Test
    fun testEffectiveDateRange_All_ReturnsNulls() {
        val params = TransactionFilterParams(dateRangePreset = DateRangePreset.ALL)
        assertNull(params.effectiveDateRange.first)
        assertNull(params.effectiveDateRange.second)
    }

    @Test
    fun testPresetsDefinitions() {
        val presets = TransactionPresets.ALL_PRESETS
        assertEquals(6, presets.size)

        // Verify all 6 required presets exist
        assertTrue(presets.any { it.id == "preset_this_month" })
        assertTrue(presets.any { it.id == "preset_last_month" })
        assertTrue(presets.any { it.id == "preset_uncategorized" })
        assertTrue(presets.any { it.id == "preset_subscriptions" })
        assertTrue(presets.any { it.id == "preset_large_expenses" })
        assertTrue(presets.any { it.id == "preset_transfers" })

        // Check specific preset parameters
        assertEquals(TransactionType.EXPENSE, TransactionPresets.LARGE_EXPENSES.params.type)
        assertEquals(10_000L, TransactionPresets.LARGE_EXPENSES.params.minAmountMinor)
        assertEquals(TransactionSort.AMOUNT_DESC, TransactionPresets.LARGE_EXPENSES.params.sortOrder)

        assertEquals(TransactionType.TRANSFER, TransactionPresets.TRANSFERS.params.type)
        assertEquals(TransactionStatusFilter.NEEDS_REVIEW, TransactionPresets.UNCATEGORIZED.params.status)
        assertEquals(TransactionStatusFilter.RECURRING, TransactionPresets.SUBSCRIPTIONS.params.status)
    }
}
