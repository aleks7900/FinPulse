package md.alexlab.finpulse.domain.categorization

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.CategorizationConfidence
import md.alexlab.finpulse.domain.model.CategorizationRule
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.MatchType
import md.alexlab.finpulse.domain.model.MerchantSignal
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.CategorizationRuleRepository
import md.alexlab.finpulse.domain.repository.MerchantSignalRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import md.alexlab.finpulse.domain.usecase.categorization.ApplyRuleToExistingTransactionsUseCase
import md.alexlab.finpulse.domain.usecase.categorization.CategorizeTransactionUseCase
import md.alexlab.finpulse.domain.usecase.categorization.FindMatchingTransactionsForRuleUseCase
import md.alexlab.finpulse.domain.usecase.categorization.GetReviewQueueUseCase
import md.alexlab.finpulse.domain.usecase.categorization.ManageCategorizationRuleUseCase
import md.alexlab.finpulse.domain.usecase.categorization.RecordCategoryCorrectionUseCase
import md.alexlab.finpulse.domain.usecase.categorization.SaveRuleResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CategorizationLifecycleTest {

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var ruleRepository: FakeCategorizationRuleRepository
    private lateinit var signalRepository: FakeMerchantSignalRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var manageRuleUseCase: ManageCategorizationRuleUseCase
    private lateinit var findMatchingUseCase: FindMatchingTransactionsForRuleUseCase
    private lateinit var applyRuleUseCase: ApplyRuleToExistingTransactionsUseCase
    private lateinit var recordCorrectionUseCase: RecordCategoryCorrectionUseCase
    private lateinit var getReviewQueueUseCase: GetReviewQueueUseCase
    private lateinit var categorizeUseCase: CategorizeTransactionUseCase

    @Before
    fun setup() {
        transactionRepository = FakeTransactionRepository()
        ruleRepository = FakeCategorizationRuleRepository()
        signalRepository = FakeMerchantSignalRepository()
        categoryRepository = FakeCategoryRepository()

        manageRuleUseCase = ManageCategorizationRuleUseCase(ruleRepository)
        findMatchingUseCase = FindMatchingTransactionsForRuleUseCase(ruleRepository, transactionRepository)
        applyRuleUseCase = ApplyRuleToExistingTransactionsUseCase(ruleRepository, transactionRepository, findMatchingUseCase)
        recordCorrectionUseCase = RecordCategoryCorrectionUseCase(transactionRepository, signalRepository)
        getReviewQueueUseCase = GetReviewQueueUseCase(transactionRepository, ruleRepository, signalRepository)
        categorizeUseCase = CategorizeTransactionUseCase(ruleRepository, signalRepository, categoryRepository)
    }

    @Test
    fun testRuleValidation_RequiresNameAndCondition() = runBlocking {
        // Blank name
        val blankName = manageRuleUseCase.saveRule(
            name = "",
            targetCategoryId = "cat_groceries",
            merchantPattern = "Trader Joe"
        )
        assertTrue(blankName is SaveRuleResult.Error)

        // No conditions
        val noConditions = manageRuleUseCase.saveRule(
            name = "Empty Rule",
            targetCategoryId = "cat_groceries"
        )
        assertTrue(noConditions is SaveRuleResult.Error)

        // Valid rule
        val valid = manageRuleUseCase.saveRule(
            name = "Whole Foods",
            targetCategoryId = "cat_groceries",
            merchantPattern = "Whole Foods"
        )
        assertTrue(valid is SaveRuleResult.Success)
    }

    @Test
    fun testNeverOverwriteManuallyChosenCategoryWithoutExplicitAction() = runBlocking {
        // 1. Seed two transactions matching "Starbucks":
        // - tx1: Unreviewed (isCategoryConfirmed = false, category = cat_uncategorized)
        // - tx2: Manually chosen by user (isCategoryConfirmed = true, category = cat_gifts)
        val tx1 = Transaction(
            id = "tx-1",
            amount = Money(450L, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-1",
            categoryId = "cat_uncategorized",
            merchant = "Starbucks #1234",
            isCategoryConfirmed = false
        )
        val tx2 = Transaction(
            id = "tx-2",
            amount = Money(2500L, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-1",
            categoryId = "cat_gifts", // Manually set as a gift card!
            merchant = "Starbucks Coffee",
            isCategoryConfirmed = true
        )
        transactionRepository.createTransaction(tx1)
        transactionRepository.createTransaction(tx2)

        // 2. Create rule: Starbucks -> cat_food
        val ruleResult = manageRuleUseCase.saveRule(
            name = "Starbucks to Food",
            targetCategoryId = "cat_food",
            merchantPattern = "Starbucks",
            priority = 10
        )
        val rule = (ruleResult as SaveRuleResult.Success).rule

        // 3. Find matching preview
        val preview = findMatchingUseCase(rule)
        assertEquals(1, preview.safeMatches.size)
        assertEquals("tx-1", preview.safeMatches[0].id)
        assertEquals(1, preview.manualMatches.size)
        assertEquals("tx-2", preview.manualMatches[0].id)

        // 4. Apply rule with overrideManual = FALSE (default safety behavior)
        val resultWithoutOverride = applyRuleUseCase(ruleId = rule.id, overrideManual = false)
        assertEquals(1, resultWithoutOverride.updatedCount)
        assertEquals(1, resultWithoutOverride.skippedManualCount)

        // Verify: tx1 was updated to cat_food, tx2 was PRESERVED as cat_gifts
        val refreshedTx1 = transactionRepository.getTransactionById("tx-1")
        assertEquals("cat_food", refreshedTx1?.categoryId)
        assertTrue(refreshedTx1!!.isCategoryConfirmed)

        val refreshedTx2 = transactionRepository.getTransactionById("tx-2")
        assertEquals("cat_gifts", refreshedTx2?.categoryId) // PRESERVED!

        // 5. Apply rule with overrideManual = TRUE (explicit user action)
        val resultWithOverride = applyRuleUseCase(ruleId = rule.id, overrideManual = true)
        assertEquals(1, resultWithOverride.updatedCount) // tx2 updated (tx1 already cat_food)
        val refreshedTx2AfterOverride = transactionRepository.getTransactionById("tx-2")
        assertEquals("cat_food", refreshedTx2AfterOverride?.categoryId)
    }

    @Test
    fun testReviewQueue_ProjectsUnreviewedTransactionsAndClearsOnConfirmation() = runBlocking {
        // Seed unreviewed transaction
        val unreviewedTx = Transaction(
            id = "tx-unreviewed",
            amount = Money(1599L, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat_uncategorized",
            merchant = "Netflix.com",
            isCategoryConfirmed = false
        )
        transactionRepository.createTransaction(unreviewedTx)

        // Create rule: Netflix -> Subscriptions
        manageRuleUseCase.saveRule(
            name = "Netflix Sub",
            targetCategoryId = "cat_subscriptions",
            merchantPattern = "Netflix"
        )

        // Verify it appears in review queue with high confidence suggestion
        val queue = getReviewQueueUseCase().first()
        assertEquals(1, queue.size)
        assertEquals("tx-unreviewed", queue[0].transaction.id)
        assertEquals("cat_subscriptions", queue[0].suggestion.categoryId)
        assertEquals(CategorizationConfidence.EXACT_RULE, queue[0].suggestion.confidence)

        // User confirms category
        recordCorrectionUseCase(
            transactionId = "tx-unreviewed",
            newCategoryId = "cat_subscriptions",
            matchedRuleId = queue[0].suggestion.matchedRuleId
        )

        // Queue is now clear!
        val queueAfter = getReviewQueueUseCase().first()
        assertTrue(queueAfter.isEmpty())
    }

    @Test
    fun testUserCorrectionBecomesDeterministicLocalLearningSignal() = runBlocking {
        // Seed unreviewed transaction with no rule configured
        val tx = Transaction(
            id = "tx-bakery",
            amount = Money(850L, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = "acc-checking",
            categoryId = "cat_uncategorized",
            merchant = "SQ *LITTLE CAFE ON 5TH",
            isCategoryConfirmed = false
        )
        transactionRepository.createTransaction(tx)

        // User corrects category to Food & Dining
        recordCorrectionUseCase(
            transactionId = "tx-bakery",
            newCategoryId = "cat_food"
        )

        // Verify signal was saved
        val signals = signalRepository.getAllSignals()
        assertEquals(1, signals.size)
        assertEquals("cat_food", signals[0].categoryId)

        // Next transaction with similar merchant name is automatically suggested with HIGH confidence!
        val candidate = md.alexlab.finpulse.domain.model.CategorizationCandidate(
            merchant = "Little Cafe on 5th",
            amountMinor = 1200L
        )
        val result = categorizeUseCase(candidate)
        assertEquals("cat_food", result.categoryId)
        assertEquals(CategorizationConfidence.HIGH, result.confidence)
    }

    @Test
    fun testRulePriorityReorderingAndActivation() = runBlocking {
        val rule1 = manageRuleUseCase.saveRule(
            name = "Rule 1",
            targetCategoryId = "cat_1",
            merchantPattern = "Shop",
            priority = 10
        )
        val ruleId = (rule1 as SaveRuleResult.Success).rule.id

        // Increase priority
        manageRuleUseCase.updateRulePriority(ruleId, 50)
        var updated = ruleRepository.getRuleById(ruleId)
        assertEquals(50, updated?.priority)

        // Toggle active
        manageRuleUseCase.setRuleActive(ruleId, false)
        updated = ruleRepository.getRuleById(ruleId)
        assertFalse(updated!!.isActive)

        // Active rules list excludes inactive rule
        val active = ruleRepository.getActiveRules()
        assertTrue(active.isEmpty())
    }
}

// In-memory fake repositories for categorization tests
class FakeCategorizationRuleRepository : CategorizationRuleRepository {
    private val rules = MutableStateFlow<Map<String, CategorizationRule>>(emptyMap())

    override fun getAllRulesFlow(): Flow<List<CategorizationRule>> =
        rules.asStateFlow().map { it.values.sortedByDescending { r -> r.priority } }

    override fun getActiveRulesFlow(): Flow<List<CategorizationRule>> =
        rules.asStateFlow().map { it.values.filter { r -> r.isActive }.sortedByDescending { r -> r.priority } }

    override suspend fun getActiveRules(): List<CategorizationRule> =
        rules.value.values.filter { it.isActive }.sortedByDescending { it.priority }

    override suspend fun getRuleById(id: String): CategorizationRule? =
        rules.value[id]

    override suspend fun saveRule(rule: CategorizationRule) {
        rules.value = rules.value + (rule.id to rule)
    }

    override suspend fun deleteRule(id: String) {
        rules.value = rules.value - id
    }

    override suspend fun setRuleActive(id: String, isActive: Boolean) {
        val rule = rules.value[id] ?: return
        rules.value = rules.value + (id to rule.copy(isActive = isActive))
    }

    override suspend fun updateRulePriority(id: String, priority: Int) {
        val rule = rules.value[id] ?: return
        rules.value = rules.value + (id to rule.copy(priority = priority))
    }

    override suspend fun seedDefaultRulesIfNeeded() {
        if (rules.value.isNotEmpty()) return
        saveRule(CategorizationRule(id = "rule_default_amazon", name = "Amazon Purchases", targetCategoryId = "cat_shopping", priority = 10, merchantPattern = "amazon"))
    }
}

class FakeMerchantSignalRepository : MerchantSignalRepository {
    private val signals = MutableStateFlow<Map<String, MerchantSignal>>(emptyMap())

    override fun getAllSignalsFlow(): Flow<List<MerchantSignal>> =
        signals.asStateFlow().map { it.values.toList() }

    override suspend fun getAllSignals(): List<MerchantSignal> =
        signals.value.values.toList()

    override suspend fun getSignal(normalizedMerchant: String): MerchantSignal? =
        signals.value[normalizedMerchant.lowercase().trim()]

    override suspend fun recordSignal(merchant: String, categoryId: String) {
        val key = md.alexlab.finpulse.domain.engine.MerchantNormalizer.toLookupKey(merchant)
        if (key.isBlank()) return
        val existing = signals.value[key]
        val count = (existing?.useCount ?: 0) + 1
        val signal = MerchantSignal(
            normalizedMerchant = key,
            categoryId = categoryId,
            useCount = count,
            lastUsedAt = System.currentTimeMillis()
        )
        signals.value = signals.value + (key to signal)
    }

    override suspend fun clearAllSignals() {
        signals.value = emptyMap()
    }
}

class FakeCategoryRepository : md.alexlab.finpulse.domain.repository.CategoryRepository {
    private val categories = MutableStateFlow<Map<String, Category>>(emptyMap())

    override fun getAllCategoriesFlow(): Flow<List<Category>> =
        categories.asStateFlow().map { it.values.toList() }

    override fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<Category>> =
        categories.asStateFlow().map { it.values.filter { c -> c.type == type } }

    override suspend fun getCategoryById(id: String): Category? =
        categories.value[id]

    override suspend fun saveCategory(category: Category) {
        categories.value = categories.value + (category.id to category)
    }

    override suspend fun seedDefaultCategoriesIfNeeded() {}
}

class FakeTransactionRepository : TransactionRepository {
    private val transactions = MutableStateFlow<Map<String, Transaction>>(emptyMap())

    override fun getAllTransactionsFlow(): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.toList().sortedByDescending { tx -> tx.timestamp } }

    override fun getRecentTransactionsFlow(limit: Int): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.toList().sortedByDescending { tx -> tx.timestamp }.take(limit) }

    override fun getTransactionsByAccountFlow(accountId: String): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.filter { tx -> tx.sourceAccountId == accountId || tx.destinationAccountId == accountId } }

    override fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.filter { tx -> tx.categoryId == categoryId } }

    override fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactions.asStateFlow().map { it.values.filter { tx -> tx.timestamp in startDate..endDate } }

    override fun searchTransactionsFlow(query: String): Flow<List<Transaction>> =
        transactions.asStateFlow().map {
            it.values.filter { tx ->
                tx.description.contains(query, ignoreCase = true) || (tx.merchant?.contains(query, ignoreCase = true) == true)
            }
        }

    override suspend fun getTransactionById(id: String): Transaction? =
        transactions.value[id]

    override suspend fun createTransaction(transaction: Transaction) {
        transactions.value = transactions.value + (transaction.id to transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactions.value = transactions.value + (transaction.id to transaction)
    }

    override suspend fun deleteTransaction(id: String) {
        transactions.value = transactions.value - id
    }

    override fun getFrequentCategoryIdsFlow(type: TransactionType, limit: Int): Flow<List<String>> =
        transactions.asStateFlow().map { map ->
            map.values.filter { it.type == type }
                .groupingBy { it.categoryId }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        }

    override fun getFrequentMerchantsFlow(limit: Int): Flow<List<String>> =
        transactions.asStateFlow().map { map ->
            map.values.mapNotNull { it.merchant?.takeIf { m -> m.isNotBlank() } }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        }

    override suspend fun getSuggestedCategoryForMerchant(merchant: String): String? =
        transactions.value.values
            .filter { it.merchant.equals(merchant, ignoreCase = true) }
            .groupingBy { it.categoryId }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    override fun getUnreviewedTransactionsFlow(): Flow<List<Transaction>> =
        transactions.asStateFlow().map { map ->
            map.values.filter { !it.isCategoryConfirmed || it.categoryId == "cat_uncategorized" }
                .sortedByDescending { it.timestamp }
        }

    override fun getUnreviewedCountFlow(): Flow<Int> =
        transactions.asStateFlow().map { map ->
            map.values.count { !it.isCategoryConfirmed || it.categoryId == "cat_uncategorized" }
        }

    override suspend fun confirmTransactionCategory(id: String, categoryId: String, matchedRuleId: String?, confidence: Float) {
        val tx = transactions.value[id] ?: return
        transactions.value = transactions.value + (id to tx.copy(
            categoryId = categoryId,
            isCategoryConfirmed = true,
            matchedRuleId = matchedRuleId,
            categorizationConfidence = confidence
        ))
    }

    override suspend fun bulkUpdateCategory(ids: List<String>, categoryId: String, isConfirmed: Boolean, matchedRuleId: String?) {
        val current = transactions.value.toMutableMap()
        for (id in ids) {
            val tx = current[id] ?: continue
            current[id] = tx.copy(
                categoryId = categoryId,
                isCategoryConfirmed = isConfirmed,
                matchedRuleId = matchedRuleId,
                categorizationConfidence = 1.0f
            )
        }
        transactions.value = current
    }
}
