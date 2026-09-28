package com.finpulse.app.core.database.util

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.finpulse.app.domain.model.TransactionFilterParams
import com.finpulse.app.domain.model.TransactionSort
import com.finpulse.app.domain.model.TransactionStatusFilter

object TransactionQueryBuilder {

    fun buildQuery(params: TransactionFilterParams): SupportSQLiteQuery {
        val whereClauses = mutableListOf<String>()
        val bindArgs = mutableListOf<Any>()

        // 1. Search across description, merchant, notes, tags, category name/id, account name, and amount
        if (params.query.isNotBlank()) {
            val rawQuery = params.query.trim()
            val q = "%$rawQuery%"
            val searchConditions = mutableListOf<String>()

            // Case-insensitive full-text search across core transaction fields
            searchConditions.add("LOWER(COALESCE(description, '')) LIKE LOWER(?)")
            bindArgs.add(q)

            searchConditions.add("LOWER(COALESCE(merchant, '')) LIKE LOWER(?)")
            bindArgs.add(q)

            searchConditions.add("LOWER(COALESCE(notes, '')) LIKE LOWER(?)")
            bindArgs.add(q)

            searchConditions.add("LOWER(COALESCE(tags, '')) LIKE LOWER(?)")
            bindArgs.add(q)

            // Category name & category ID matching
            searchConditions.add("categoryId IN (SELECT id FROM categories WHERE LOWER(name) LIKE LOWER(?) OR LOWER(id) LIKE LOWER(?))")
            bindArgs.add(q)
            bindArgs.add(q)

            // Account name matching (source or destination account)
            searchConditions.add("sourceAccountId IN (SELECT id FROM accounts WHERE LOWER(name) LIKE LOWER(?))")
            bindArgs.add(q)

            searchConditions.add("destinationAccountId IN (SELECT id FROM accounts WHERE LOWER(name) LIKE LOWER(?))")
            bindArgs.add(q)

            // Numeric amount matching (supports major and minor amounts with or without currency symbols)
            val cleanedNumeric = rawQuery.replace("$", "")
                .replace("€", "")
                .replace("£", "")
                .replace("¥", "")
                .replace("L", "")
                .replace("lei", "")
                .replace("₴", "")
                .replace("zł", "")
                .replace("Ft", "")
                .replace("kr", "")
                .replace("Kč", "")
                .replace("лв", "")
                .replace("₪", "")
                .replace("R", "")
                .replace(",", "")
                .trim()

            val doubleVal = cleanedNumeric.toDoubleOrNull()
            if (doubleVal != null && doubleVal > 0.0) {
                val minor2 = (doubleVal * 100).toLong()
                val minor0 = doubleVal.toLong()
                val minor3 = (doubleVal * 1000).toLong()

                searchConditions.add("(amountMinor = ? OR amountMinor = ? OR amountMinor = ?)")
                bindArgs.add(minor2)
                bindArgs.add(minor0)
                bindArgs.add(minor3)
            }

            whereClauses.add("(${searchConditions.joinToString(" OR ")})")
        }

        // 2. Account filter (matches either source or destination for transfers)
        if (!params.accountId.isNullOrBlank()) {
            whereClauses.add("(sourceAccountId = ? OR destinationAccountId = ?)")
            bindArgs.add(params.accountId)
            bindArgs.add(params.accountId)
        }

        // 3. Category filter
        if (!params.categoryId.isNullOrBlank()) {
            whereClauses.add("categoryId = ?")
            bindArgs.add(params.categoryId)
        }

        // 4. Transaction type
        if (params.type != null) {
            whereClauses.add("type = ?")
            bindArgs.add(params.type.name)
        }

        // 5. Date range
        val (startDate, endDate) = params.effectiveDateRange
        if (startDate != null) {
            whereClauses.add("timestamp >= ?")
            bindArgs.add(startDate)
        }
        if (endDate != null) {
            whereClauses.add("timestamp <= ?")
            bindArgs.add(endDate)
        }

        // 6. Amount range
        if (params.minAmountMinor != null) {
            whereClauses.add("amountMinor >= ?")
            bindArgs.add(params.minAmountMinor)
        }
        if (params.maxAmountMinor != null) {
            whereClauses.add("amountMinor <= ?")
            bindArgs.add(params.maxAmountMinor)
        }

        // 7. Currency
        if (!params.currencyCode.isNullOrBlank()) {
            whereClauses.add("currencyCode = ?")
            bindArgs.add(params.currencyCode)
        }

        // 8. Status filter
        when (params.status) {
            TransactionStatusFilter.ALL -> Unit
            TransactionStatusFilter.CONFIRMED -> {
                whereClauses.add("isCategoryConfirmed = 1 AND categoryId != 'cat_uncategorized'")
            }
            TransactionStatusFilter.NEEDS_REVIEW -> {
                whereClauses.add("(isCategoryConfirmed = 0 OR categoryId = 'cat_uncategorized')")
            }
            TransactionStatusFilter.EXCLUDED_FROM_BUDGET -> {
                whereClauses.add("isExcludedFromBudget = 1")
            }
            TransactionStatusFilter.RECURRING -> {
                whereClauses.add("(recurringRuleId IS NOT NULL AND recurringRuleId != '')")
            }
        }

        val sql = StringBuilder("SELECT * FROM transactions")
        if (whereClauses.isNotEmpty()) {
            sql.append(" WHERE ").append(whereClauses.joinToString(" AND "))
        }

        // 9. Sorting
        val orderSql = when (params.sortOrder) {
            TransactionSort.DATE_DESC -> "timestamp DESC"
            TransactionSort.DATE_ASC -> "timestamp ASC"
            TransactionSort.AMOUNT_DESC -> "amountMinor DESC"
            TransactionSort.AMOUNT_ASC -> "amountMinor ASC"
        }
        sql.append(" ORDER BY ").append(orderSql)

        return SimpleSQLiteQuery(sql.toString(), bindArgs.toTypedArray())
    }
}
