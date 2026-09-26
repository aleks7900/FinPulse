package com.finpulse.app.domain.usecase.categorization

import com.finpulse.app.domain.engine.CategorizationEngine
import com.finpulse.app.domain.model.CategorizationCandidate
import com.finpulse.app.domain.model.CategorizationResult
import com.finpulse.app.domain.repository.CategorizationRuleRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.MerchantSignalRepository

class CategorizeTransactionUseCase(
    private val ruleRepository: CategorizationRuleRepository,
    private val signalRepository: MerchantSignalRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(candidate: CategorizationCandidate): CategorizationResult {
        val rules = ruleRepository.getActiveRules()
        val signals = signalRepository.getAllSignals()
        return CategorizationEngine.categorize(
            candidate = candidate,
            rules = rules,
            signals = signals
        )
    }
}
