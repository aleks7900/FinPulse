package com.finpulse.app.domain.model

import java.time.YearMonth
import java.time.ZoneId

enum class TransactionSort(val displayName: String) {
    DATE_DESC("Newest"),
    DATE_ASC("Oldest"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

enum class TransactionStatusFilter(val displayName: String) {
    ALL("All"),
    CONFIRMED("Confirmed"),
    NEEDS_REVIEW("Needs Review"),
    EXCLUDED_FROM_BUDGET("Excluded from Budget"),
    RECURRING("Subscriptions & Recurring")
}

enum class DateRangePreset(val displayName: String) {
    ALL("All Time"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom Range")
}

data class TransactionFilterParams(
    val query: String = "",
    val accountId: String? = null,
    val categoryId: String? = null,
    val type: TransactionType? = null,
    val dateRangePreset: DateRangePreset = DateRangePreset.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null,
    val currencyCode: String? = null,
    val status: TransactionStatusFilter = TransactionStatusFilter.ALL,
    val sortOrder: TransactionSort = TransactionSort.DATE_DESC
) {
    val isActive: Boolean
        get() = query.isNotBlank() ||
                accountId != null ||
                categoryId != null ||
                type != null ||
                dateRangePreset != DateRangePreset.ALL ||
                customStartDate != null ||
                customEndDate != null ||
                minAmountMinor != null ||
                maxAmountMinor != null ||
                !currencyCode.isNullOrBlank() ||
                status != TransactionStatusFilter.ALL ||
                sortOrder != TransactionSort.DATE_DESC

    val activeFilterCount: Int
        get() {
            var count = 0
            if (query.isNotBlank()) count++
            if (accountId != null) count++
            if (categoryId != null) count++
            if (type != null) count++
            if (dateRangePreset != DateRangePreset.ALL || customStartDate != null || customEndDate != null) count++
            if (minAmountMinor != null || maxAmountMinor != null) count++
            if (!currencyCode.isNullOrBlank()) count++
            if (status != TransactionStatusFilter.ALL) count++
            return count
        }

    val effectiveDateRange: Pair<Long?, Long?>
        get() {
            val zone = ZoneId.systemDefault()
            return when (dateRangePreset) {
                DateRangePreset.ALL -> Pair(null, null)
                DateRangePreset.THIS_MONTH -> {
                    val currentMonth = YearMonth.now(zone)
                    val start = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = currentMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    Pair(start, end)
                }
                DateRangePreset.LAST_MONTH -> {
                    val lastMonth = YearMonth.now(zone).minusMonths(1)
                    val start = lastMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = lastMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    Pair(start, end)
                }
                DateRangePreset.THIS_YEAR -> {
                    val year = YearMonth.now(zone).year
                    val start = YearMonth.of(year, 1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = YearMonth.of(year, 12).atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    Pair(start, end)
                }
                DateRangePreset.CUSTOM -> Pair(customStartDate, customEndDate)
            }
        }
}
