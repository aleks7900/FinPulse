package com.finpulse.app.domain.model

data class TransactionPreset(
    val id: String,
    val name: String,
    val params: TransactionFilterParams
)

object TransactionPresets {
    val THIS_MONTH = TransactionPreset(
        id = "preset_this_month",
        name = "This Month",
        params = TransactionFilterParams(
            dateRangePreset = DateRangePreset.THIS_MONTH
        )
    )

    val LAST_MONTH = TransactionPreset(
        id = "preset_last_month",
        name = "Last Month",
        params = TransactionFilterParams(
            dateRangePreset = DateRangePreset.LAST_MONTH
        )
    )

    val UNCATEGORIZED = TransactionPreset(
        id = "preset_uncategorized",
        name = "Uncategorized",
        params = TransactionFilterParams(
            status = TransactionStatusFilter.NEEDS_REVIEW
        )
    )

    val SUBSCRIPTIONS = TransactionPreset(
        id = "preset_subscriptions",
        name = "Subscriptions",
        params = TransactionFilterParams(
            status = TransactionStatusFilter.RECURRING
        )
    )

    val LARGE_EXPENSES = TransactionPreset(
        id = "preset_large_expenses",
        name = "Large Expenses",
        params = TransactionFilterParams(
            type = TransactionType.EXPENSE,
            minAmountMinor = 10_000L,
            sortOrder = TransactionSort.AMOUNT_DESC
        )
    )

    val TRANSFERS = TransactionPreset(
        id = "preset_transfers",
        name = "Transfers",
        params = TransactionFilterParams(
            type = TransactionType.TRANSFER
        )
    )

    val ALL_PRESETS = listOf(
        THIS_MONTH,
        LAST_MONTH,
        UNCATEGORIZED,
        SUBSCRIPTIONS,
        LARGE_EXPENSES,
        TRANSFERS
    )
}
