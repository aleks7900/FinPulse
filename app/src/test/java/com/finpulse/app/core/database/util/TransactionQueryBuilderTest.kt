package com.finpulse.app.core.database.util

import com.finpulse.app.domain.model.DateRangePreset
import com.finpulse.app.domain.model.TransactionFilterParams
import com.finpulse.app.domain.model.TransactionPresets
import com.finpulse.app.domain.model.TransactionSort
import com.finpulse.app.domain.model.TransactionStatusFilter
import com.finpulse.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionQueryBuilderTest {

    @Test
    fun testEmptyParams_GeneratesBaseQueryWithDefaultDateDescSort() {
        val params = TransactionFilterParams()
        val query = TransactionQueryBuilder.buildQuery(params)

        assertEquals("SELECT * FROM transactions ORDER BY timestamp DESC", query.sql)
        assertEquals(0, query.argCount)
    }

    @Test
    fun testSearchQuery_MatchesMerchantDescriptionNotesAndTags() {
        val params = TransactionFilterParams(query = "coffee")
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("WHERE (description LIKE ? OR merchant LIKE ? OR notes LIKE ? OR tags LIKE ?)"))
        assertTrue(query.sql.endsWith("ORDER BY timestamp DESC"))
        assertEquals(4, query.argCount)
    }

    @Test
    fun testAccountFilter_MatchesSourceOrDestinationAccount() {
        val params = TransactionFilterParams(accountId = "acc_bank")
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("(sourceAccountId = ? OR destinationAccountId = ?)"))
        assertEquals(2, query.argCount)
    }

    @Test
    fun testCategoryFilter() {
        val params = TransactionFilterParams(categoryId = "cat_groceries")
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("categoryId = ?"))
        assertEquals(1, query.argCount)
    }

    @Test
    fun testTransactionTypeFilter() {
        val params = TransactionFilterParams(type = TransactionType.EXPENSE)
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("type = ?"))
        assertEquals(1, query.argCount)
    }

    @Test
    fun testAmountRangeFilter() {
        val params = TransactionFilterParams(minAmountMinor = 1_000L, maxAmountMinor = 50_000L)
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("amountMinor >= ? AND amountMinor <= ?"))
        assertEquals(2, query.argCount)
    }

    @Test
    fun testCurrencyFilter() {
        val params = TransactionFilterParams(currencyCode = "EUR")
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("currencyCode = ?"))
        assertEquals(1, query.argCount)
    }

    @Test
    fun testStatusFilter_Confirmed() {
        val params = TransactionFilterParams(status = TransactionStatusFilter.CONFIRMED)
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("isCategoryConfirmed = 1 AND categoryId != 'cat_uncategorized'"))
        assertEquals(0, query.argCount)
    }

    @Test
    fun testStatusFilter_NeedsReview() {
        val params = TransactionFilterParams(status = TransactionStatusFilter.NEEDS_REVIEW)
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("(isCategoryConfirmed = 0 OR categoryId = 'cat_uncategorized')"))
        assertEquals(0, query.argCount)
    }

    @Test
    fun testStatusFilter_ExcludedFromBudget() {
        val params = TransactionFilterParams(status = TransactionStatusFilter.EXCLUDED_FROM_BUDGET)
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("isExcludedFromBudget = 1"))
        assertEquals(0, query.argCount)
    }

    @Test
    fun testStatusFilter_Recurring() {
        val params = TransactionFilterParams(status = TransactionStatusFilter.RECURRING)
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("(recurringRuleId IS NOT NULL AND recurringRuleId != '')"))
        assertEquals(0, query.argCount)
    }

    @Test
    fun testCombineMultipleFilters() {
        val params = TransactionFilterParams(
            query = "Starbucks",
            accountId = "acc_bank",
            categoryId = "cat_coffee",
            type = TransactionType.EXPENSE,
            minAmountMinor = 500L,
            maxAmountMinor = 2_500L,
            currencyCode = "USD",
            status = TransactionStatusFilter.CONFIRMED,
            sortOrder = TransactionSort.AMOUNT_DESC
        )
        val query = TransactionQueryBuilder.buildQuery(params)

        assertTrue(query.sql.contains("description LIKE ?"))
        assertTrue(query.sql.contains("(sourceAccountId = ? OR destinationAccountId = ?)"))
        assertTrue(query.sql.contains("categoryId = ?"))
        assertTrue(query.sql.contains("type = ?"))
        assertTrue(query.sql.contains("amountMinor >= ?"))
        assertTrue(query.sql.contains("amountMinor <= ?"))
        assertTrue(query.sql.contains("currencyCode = ?"))
        assertTrue(query.sql.contains("isCategoryConfirmed = 1 AND categoryId != 'cat_uncategorized'"))
        assertTrue(query.sql.endsWith("ORDER BY amountMinor DESC"))
    }

    @Test
    fun testSortOrders() {
        assertEquals("SELECT * FROM transactions ORDER BY timestamp DESC", TransactionQueryBuilder.buildQuery(TransactionFilterParams(sortOrder = TransactionSort.DATE_DESC)).sql)
        assertEquals("SELECT * FROM transactions ORDER BY timestamp ASC", TransactionQueryBuilder.buildQuery(TransactionFilterParams(sortOrder = TransactionSort.DATE_ASC)).sql)
        assertEquals("SELECT * FROM transactions ORDER BY amountMinor DESC", TransactionQueryBuilder.buildQuery(TransactionFilterParams(sortOrder = TransactionSort.AMOUNT_DESC)).sql)
        assertEquals("SELECT * FROM transactions ORDER BY amountMinor ASC", TransactionQueryBuilder.buildQuery(TransactionFilterParams(sortOrder = TransactionSort.AMOUNT_ASC)).sql)
    }

    @Test
    fun testPresets() {
        // This Month
        val thisMonthQuery = TransactionQueryBuilder.buildQuery(TransactionPresets.THIS_MONTH.params)
        assertTrue(thisMonthQuery.sql.contains("timestamp >= ? AND timestamp <= ?"))

        // Last Month
        val lastMonthQuery = TransactionQueryBuilder.buildQuery(TransactionPresets.LAST_MONTH.params)
        assertTrue(lastMonthQuery.sql.contains("timestamp >= ? AND timestamp <= ?"))

        // Uncategorized
        val uncatQuery = TransactionQueryBuilder.buildQuery(TransactionPresets.UNCATEGORIZED.params)
        assertTrue(uncatQuery.sql.contains("(isCategoryConfirmed = 0 OR categoryId = 'cat_uncategorized')"))

        // Subscriptions
        val subsQuery = TransactionQueryBuilder.buildQuery(TransactionPresets.SUBSCRIPTIONS.params)
        assertTrue(subsQuery.sql.contains("recurringRuleId IS NOT NULL"))

        // Large Expenses
        val largeQuery = TransactionQueryBuilder.buildQuery(TransactionPresets.LARGE_EXPENSES.params)
        assertTrue(largeQuery.sql.contains("type = ?"))
        assertTrue(largeQuery.sql.contains("amountMinor >= ?"))
        assertTrue(largeQuery.sql.endsWith("ORDER BY amountMinor DESC"))

        // Transfers
        val transferQuery = TransactionQueryBuilder.buildQuery(TransactionPresets.TRANSFERS.params)
        assertTrue(transferQuery.sql.contains("type = ?"))
    }
}
