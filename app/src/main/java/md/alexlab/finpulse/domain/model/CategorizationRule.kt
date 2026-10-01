package md.alexlab.finpulse.domain.model

import kotlinx.serialization.Serializable

enum class MatchType {
    CONTAINS,
    EXACT,
    STARTS_WITH,
    REGEX
}

@Serializable
data class CategorizationRule(
    val id: String,
    val name: String,
    val targetCategoryId: String,
    val priority: Int = 0, // Higher number = higher priority
    val merchantPattern: String? = null,
    val merchantMatchType: MatchType = MatchType.CONTAINS,
    val descriptionPattern: String? = null,
    val descriptionMatchType: MatchType = MatchType.CONTAINS,
    val accountId: String? = null, // Specific account (e.g. account + merchant -> category)
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null,
    val transactionType: TransactionType? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Specificity score used for conflict resolution when two rules have identical priority.
     * More specific rules (e.g. Account + Exact Merchant) take precedence over general rules.
     */
    val specificityScore: Int
        get() {
            var score = 0
            if (!accountId.isNullOrBlank()) score += 10
            if (!merchantPattern.isNullOrBlank()) {
                score += if (merchantMatchType == MatchType.EXACT) 8 else 4
            }
            if (!descriptionPattern.isNullOrBlank()) {
                score += if (descriptionMatchType == MatchType.EXACT) 6 else 3
            }
            if (minAmountMinor != null || maxAmountMinor != null) score += 2
            if (transactionType != null) score += 1
            return score
        }
}
