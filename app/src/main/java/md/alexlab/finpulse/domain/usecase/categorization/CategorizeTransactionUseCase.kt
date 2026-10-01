package md.alexlab.finpulse.domain.usecase.categorization

import md.alexlab.finpulse.domain.engine.CategorizationEngine
import md.alexlab.finpulse.domain.model.CategorizationCandidate
import md.alexlab.finpulse.domain.model.CategorizationResult
import md.alexlab.finpulse.domain.repository.CategorizationRuleRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.MerchantSignalRepository

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
