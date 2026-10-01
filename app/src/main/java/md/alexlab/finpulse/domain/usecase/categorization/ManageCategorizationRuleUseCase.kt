package md.alexlab.finpulse.domain.usecase.categorization

import md.alexlab.finpulse.domain.model.CategorizationRule
import md.alexlab.finpulse.domain.model.MatchType
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.CategorizationRuleRepository
import java.util.UUID

sealed class SaveRuleResult {
    data class Success(val rule: CategorizationRule) : SaveRuleResult()
    data class Error(val message: String) : SaveRuleResult()
}

class ManageCategorizationRuleUseCase(
    private val ruleRepository: CategorizationRuleRepository
) {
    suspend fun saveRule(
        id: String? = null,
        name: String,
        targetCategoryId: String,
        priority: Int = 0,
        merchantPattern: String? = null,
        merchantMatchType: MatchType = MatchType.CONTAINS,
        descriptionPattern: String? = null,
        descriptionMatchType: MatchType = MatchType.CONTAINS,
        accountId: String? = null,
        minAmountMinor: Long? = null,
        maxAmountMinor: Long? = null,
        transactionType: TransactionType? = null,
        isActive: Boolean = true
    ): SaveRuleResult {
        if (name.isBlank()) {
            return SaveRuleResult.Error("Rule name cannot be empty")
        }
        if (targetCategoryId.isBlank()) {
            return SaveRuleResult.Error("Target category must be specified")
        }

        val hasCondition = !merchantPattern.isNullOrBlank() ||
                !descriptionPattern.isNullOrBlank() ||
                !accountId.isNullOrBlank() ||
                minAmountMinor != null ||
                maxAmountMinor != null ||
                transactionType != null

        if (!hasCondition) {
            return SaveRuleResult.Error("At least one rule condition (merchant, description, account, or amount) must be provided")
        }

        val rule = CategorizationRule(
            id = id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            targetCategoryId = targetCategoryId.trim(),
            priority = priority,
            merchantPattern = merchantPattern?.trim()?.takeIf { it.isNotEmpty() },
            merchantMatchType = merchantMatchType,
            descriptionPattern = descriptionPattern?.trim()?.takeIf { it.isNotEmpty() },
            descriptionMatchType = descriptionMatchType,
            accountId = accountId?.trim()?.takeIf { it.isNotEmpty() },
            minAmountMinor = minAmountMinor,
            maxAmountMinor = maxAmountMinor,
            transactionType = transactionType,
            isActive = isActive,
            updatedAt = System.currentTimeMillis()
        )

        ruleRepository.saveRule(rule)
        return SaveRuleResult.Success(rule)
    }

    suspend fun deleteRule(id: String) {
        ruleRepository.deleteRule(id)
    }

    suspend fun setRuleActive(id: String, isActive: Boolean) {
        ruleRepository.setRuleActive(id, isActive)
    }

    suspend fun updateRulePriority(id: String, priority: Int) {
        ruleRepository.updateRulePriority(id, priority)
    }
}
