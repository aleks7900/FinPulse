package com.finpulse.app.domain.model

import com.finpulse.app.core.model.Money

enum class ReviewItemType(val displayName: String) {
    UNCATEGORIZED("Uncategorized"),
    IMPORTED_CONFIRMATION("Needs Confirmation"),
    SUSPECTED_DUPLICATE("Suspected Duplicate"),
    MISSING_MERCHANT("Missing Merchant"),
    UNUSUAL_AMOUNT("Unusual Amount"),
    OVERDUE_BILL("Overdue Bill"),
    FAILED_RECURRING("Generation Failed")
}

enum class ReviewItemPriority(val level: Int) {
    CRITICAL(1),
    WARNING(2),
    ACTIONABLE(3)
}

data class ReviewInboxItem(
    val id: String,
    val type: ReviewItemType,
    val priority: ReviewItemPriority,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val amount: Money,
    val transaction: Transaction? = null,
    val duplicateCandidate: Transaction? = null,
    val recurringRule: RecurringTransaction? = null,
    val recurringOccurrence: RecurringOccurrence? = null,
    val suggestedCategoryId: String? = null,
    val suggestedCategoryName: String? = null,
    val suggestedCategoryConfidence: Float = 0f,
    val reason: String = "",
    val accountName: String? = null,
    val categoryName: String? = null,
    val isDismissible: Boolean = true
)

data class SafeBulkSuggestion(
    val id: String,
    val merchant: String,
    val suggestedCategoryId: String,
    val suggestedCategoryName: String,
    val confidence: Float,
    val transactionIds: List<String>,
    val count: Int
)

data class ReviewInboxSummary(
    val items: List<ReviewInboxItem> = emptyList(),
    val safeBulkSuggestions: List<SafeBulkSuggestion> = emptyList(),
    val totalCount: Int = 0,
    val uncategorizedCount: Int = 0,
    val importedCount: Int = 0,
    val duplicatesCount: Int = 0,
    val missingMerchantCount: Int = 0,
    val unusualAmountsCount: Int = 0,
    val overdueBillsCount: Int = 0,
    val failedRecurringCount: Int = 0
)
