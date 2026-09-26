package com.finpulse.app.domain.engine

import com.finpulse.app.domain.model.CategorizationCandidate
import com.finpulse.app.domain.model.CategorizationConfidence
import com.finpulse.app.domain.model.CategorizationResult
import com.finpulse.app.domain.model.CategorizationRule
import com.finpulse.app.domain.model.CategorizationSource
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.MatchType
import com.finpulse.app.domain.model.MerchantSignal

object CategorizationEngine {

    // Common category keywords for description fallback
    private val KEYWORD_CATEGORY_HINTS = mapOf(
        "payroll" to "cat_salary",
        "salary" to "cat_salary",
        "wages" to "cat_salary",
        "dividend" to "cat_invest_return",
        "interest payment" to "cat_invest_return",
        "freelance" to "cat_freelance",
        "groceries" to "cat_groceries",
        "supermarket" to "cat_groceries",
        "pharmacy" to "cat_health",
        "hospital" to "cat_health",
        "gas station" to "cat_fuel",
        "fuel" to "cat_fuel",
        "electric utility" to "cat_housing",
        "water bill" to "cat_housing",
        "tuition" to "cat_education",
        "gym" to "cat_fitness",
        "fitness" to "cat_fitness",
        "donation" to "cat_gifts"
    )

    /**
     * Categorizes a transaction candidate against user rules, deterministic correction signals,
     * historical merchant signals, and description keywords.
     */
    fun categorize(
        candidate: CategorizationCandidate,
        rules: List<CategorizationRule>,
        signals: List<MerchantSignal> = emptyList(),
        availableCategories: List<Category> = emptyList()
    ): CategorizationResult {
        val rawMerchant = candidate.merchant?.trim() ?: ""
        val normalizedMerchant = MerchantNormalizer.normalize(rawMerchant)
        val canonicalMerchant = MerchantNormalizer.resolveCanonicalName(rawMerchant)
        val lookupKey = MerchantNormalizer.toLookupKey(rawMerchant)
        val description = candidate.description.trim()

        // 1. Evaluate User Rules (Sorted by Priority DESC, Specificity DESC, CreatedAt DESC)
        val activeRules = rules
            .filter { it.isActive }
            .sortedWith(
                compareByDescending<CategorizationRule> { it.priority }
                    .thenByDescending { it.specificityScore }
                    .thenByDescending { it.createdAt }
            )

        for (rule in activeRules) {
            if (matchesRule(candidate, rule, rawMerchant, normalizedMerchant, canonicalMerchant, description)) {
                return CategorizationResult(
                    categoryId = rule.targetCategoryId,
                    confidence = CategorizationConfidence.EXACT_RULE,
                    confidenceScore = 1.0f,
                    source = CategorizationSource.USER_RULE,
                    matchedRuleId = rule.id,
                    matchedRuleName = rule.name,
                    explanation = "Matched user rule: '${rule.name}' (Priority ${rule.priority})"
                )
            }
        }

        // 2. Evaluate Local Deterministic Correction / Merchant Signals
        if (lookupKey.isNotBlank()) {
            val signalMap = signals.associateBy { it.normalizedMerchant.lowercase().trim() }
            val matchedSignal = signalMap[lookupKey] ?: signalMap[normalizedMerchant.lowercase().trim()]
            if (matchedSignal != null) {
                return CategorizationResult(
                    categoryId = matchedSignal.categoryId,
                    confidence = CategorizationConfidence.HIGH,
                    confidenceScore = 0.90f,
                    source = CategorizationSource.MERCHANT_CORRECTION_SIGNAL,
                    explanation = "Learned from past user confirmation for '$canonicalMerchant' (${matchedSignal.useCount} times)"
                )
            }
        }

        // 3. Evaluate Description Keywords
        val descLower = description.lowercase()
        for ((keyword, targetCatId) in KEYWORD_CATEGORY_HINTS) {
            if (descLower.contains(keyword)) {
                return CategorizationResult(
                    categoryId = targetCatId,
                    confidence = CategorizationConfidence.MEDIUM,
                    confidenceScore = 0.65f,
                    source = CategorizationSource.DESCRIPTION_KEYWORD,
                    explanation = "Suggested from description keyword: '$keyword'"
                )
            }
        }

        // 4. Default / Uncategorized fallback
        return CategorizationResult(
            categoryId = null,
            confidence = CategorizationConfidence.NONE,
            confidenceScore = 0.0f,
            source = CategorizationSource.UNCATEGORIZED,
            explanation = "No matching rule or historical signal found"
        )
    }

    /**
     * Determines whether a candidate satisfies all non-empty criteria of a rule.
     */
    fun matchesRule(
        candidate: CategorizationCandidate,
        rule: CategorizationRule,
        rawMerchant: String = candidate.merchant?.trim() ?: "",
        normalizedMerchant: String = MerchantNormalizer.normalize(rawMerchant),
        canonicalMerchant: String = MerchantNormalizer.resolveCanonicalName(rawMerchant),
        description: String = candidate.description.trim()
    ): Boolean {
        // Account constraint (e.g. Account + Merchant -> Category)
        if (!rule.accountId.isNullOrBlank()) {
            if (candidate.sourceAccountId != rule.accountId) return false
        }

        // Transaction type constraint
        if (rule.transactionType != null) {
            if (candidate.type != rule.transactionType) return false
        }

        // Amount bounds
        if (rule.minAmountMinor != null && candidate.amountMinor < rule.minAmountMinor) {
            return false
        }
        if (rule.maxAmountMinor != null && candidate.amountMinor > rule.maxAmountMinor) {
            return false
        }

        var matchedAnyPattern = false

        // Merchant pattern match
        if (!rule.merchantPattern.isNullOrBlank()) {
            val pattern = rule.merchantPattern.trim()
            val matchesMerchant = evaluateTextPattern(
                textToMatch = listOf(rawMerchant, normalizedMerchant, canonicalMerchant),
                pattern = pattern,
                matchType = rule.merchantMatchType
            )
            if (!matchesMerchant) return false
            matchedAnyPattern = true
        }

        // Description pattern match
        if (!rule.descriptionPattern.isNullOrBlank()) {
            val pattern = rule.descriptionPattern.trim()
            val matchesDesc = evaluateTextPattern(
                textToMatch = listOf(description),
                pattern = pattern,
                matchType = rule.descriptionMatchType
            )
            if (!matchesDesc) return false
            matchedAnyPattern = true
        }

        // If rule has neither merchant nor description pattern, but has account or amount or type constraint:
        if (!matchedAnyPattern) {
            val hasOtherCriteria = !rule.accountId.isNullOrBlank() ||
                    rule.minAmountMinor != null ||
                    rule.maxAmountMinor != null ||
                    rule.transactionType != null
            return hasOtherCriteria
        }

        return true
    }

    private fun evaluateTextPattern(
        textToMatch: List<String>,
        pattern: String,
        matchType: MatchType
    ): Boolean {
        return textToMatch.any { text ->
            if (text.isBlank()) return@any false
            when (matchType) {
                MatchType.CONTAINS -> text.contains(pattern, ignoreCase = true)
                MatchType.EXACT -> text.equals(pattern, ignoreCase = true)
                MatchType.STARTS_WITH -> text.startsWith(pattern, ignoreCase = true)
                MatchType.REGEX -> {
                    try {
                        Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)
                    } catch (e: Exception) {
                        false
                    }
                }
            }
        }
    }
}
