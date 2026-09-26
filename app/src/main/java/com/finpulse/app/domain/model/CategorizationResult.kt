package com.finpulse.app.domain.model

import kotlinx.serialization.Serializable

enum class CategorizationConfidence {
    EXACT_RULE,                  // 1.0 - User created rule matched
    HIGH,                        // 0.85 - Exact historical merchant signal or confirmed correction
    MEDIUM,                      // 0.65 - Normalized alias match or description keyword
    LOW,                         // 0.35 - Weak token match
    NONE                         // 0.0 - Uncategorized / No suggestion
}

enum class CategorizationSource {
    USER_RULE,
    MERCHANT_CORRECTION_SIGNAL,
    HISTORICAL_MERCHANT,
    DESCRIPTION_KEYWORD,
    UNCATEGORIZED
}

@Serializable
data class CategorizationCandidate(
    val merchant: String? = null,
    val description: String = "",
    val amountMinor: Long = 0L,
    val sourceAccountId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE
)

@Serializable
data class CategorizationResult(
    val categoryId: String?,
    val confidence: CategorizationConfidence,
    val confidenceScore: Float, // 0.0f to 1.0f
    val source: CategorizationSource,
    val matchedRuleId: String? = null,
    val matchedRuleName: String? = null,
    val explanation: String
)
