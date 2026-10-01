package md.alexlab.finpulse.domain.csv

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.CategorizationCandidate
import md.alexlab.finpulse.domain.model.CategorizationRule
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.DuplicateStatus
import md.alexlab.finpulse.domain.model.ImportProfile
import md.alexlab.finpulse.domain.model.MatchType
import md.alexlab.finpulse.domain.model.MerchantSignal
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.CategorizationRuleRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.repository.ImportProfileRepository
import md.alexlab.finpulse.domain.repository.MerchantSignalRepository
import md.alexlab.finpulse.domain.repository.TransactionRepository
import md.alexlab.finpulse.domain.usecase.categorization.CategorizeTransactionUseCase
import md.alexlab.finpulse.domain.usecase.csv.AutoDetectCsvConfigUseCase
import md.alexlab.finpulse.domain.usecase.csv.ExecuteCsvImportUseCase
import md.alexlab.finpulse.domain.usecase.csv.ManageImportProfilesUseCase
import md.alexlab.finpulse.domain.usecase.csv.ParseCsvStatementUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class CsvImportLifecycleTest {

    private lateinit var transactionRepository: FakeTransactionRepo
    private lateinit var accountRepository: FakeAccountRepo
    private lateinit var categoryRepository: FakeCategoryRepo
    private lateinit var signalRepository: FakeSignalRepo
    private lateinit var ruleRepository: FakeRuleRepo
    private lateinit var profileRepository: FakeProfileRepo

    private lateinit var autoDetectUseCase: AutoDetectCsvConfigUseCase
    private lateinit var parseUseCase: ParseCsvStatementUseCase
    private lateinit var executeImportUseCase: ExecuteCsvImportUseCase
    private lateinit var manageProfilesUseCase: ManageImportProfilesUseCase
    private lateinit var categorizeUseCase: CategorizeTransactionUseCase

    private val testAccountId = "acc_checking_primary"

    @Before
    fun setup() = runBlocking {
        transactionRepository = FakeTransactionRepo()
        accountRepository = FakeAccountRepo()
        categoryRepository = FakeCategoryRepo()
        signalRepository = FakeSignalRepo()
        ruleRepository = FakeRuleRepo()
        profileRepository = FakeProfileRepo()

        categorizeUseCase = CategorizeTransactionUseCase(ruleRepository, signalRepository, categoryRepository)
        autoDetectUseCase = AutoDetectCsvConfigUseCase()
        parseUseCase = ParseCsvStatementUseCase(transactionRepository, categorizeUseCase)
        executeImportUseCase = ExecuteCsvImportUseCase(transactionRepository, accountRepository, signalRepository, categoryRepository)
        manageProfilesUseCase = ManageImportProfilesUseCase(profileRepository)

        // Seed initial account
        accountRepository.saveAccount(
            Account(
                id = testAccountId,
                name = "Checking Account",
                type = AccountType.BANK,
                balance = Money(100000L, "USD"),
                availableBalance = Money(100000L, "USD")
            )
        )

        // Seed rules
        ruleRepository.saveRule(
            CategorizationRule(
                id = "rule_payroll",
                name = "Payroll",
                targetCategoryId = "cat_salary",
                descriptionPattern = "payroll",
                descriptionMatchType = MatchType.CONTAINS,
                priority = 10
            )
        )
        ruleRepository.saveRule(
            CategorizationRule(
                id = "rule_wholefoods",
                name = "Whole Foods",
                targetCategoryId = "cat_groceries",
                descriptionPattern = "whole foods",
                descriptionMatchType = MatchType.CONTAINS,
                priority = 10
            )
        )

        // Existing transaction on 2024-03-15 for $68.40
        val march15Epoch = LocalDate.of(2024, 3, 15).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        transactionRepository.createTransaction(
            Transaction(
                id = "tx_existing_wf",
                amount = Money(6840L, "USD"),
                type = TransactionType.EXPENSE,
                sourceAccountId = testAccountId,
                categoryId = "cat_groceries",
                merchant = "Whole Foods",
                description = "Whole Foods Market",
                timestamp = march15Epoch
            )
        )
    }

    @Test
    fun testCompleteStatementImportLifecycle() = runBlocking {
        val csvStatement = """
Date,Description,Amount,Category
2024-03-15,"Whole Foods Market",-68.40,"Groceries"
2024-03-14,"Acme Corp Payroll",3200.00,"Salary"
2024-03-12,"Netflix Subscription",-15.99,"Subscriptions"
2024-03-12,"Netflix Subscription",-15.99,"Subscriptions"
bad-date,"Corrupted Entry",-10.00,"Other"
        """.trimIndent()

        // 1. Auto detect config & mapping
        val (config, mapping) = autoDetectUseCase(csvStatement)
        assertEquals(',', config.delimiter)
        assertTrue(config.hasHeader)
        assertEquals(0, mapping.dateColumnIndex)
        assertEquals(1, mapping.descriptionColumnIndex)
        assertEquals(2, mapping.amountColumnIndex)

        // 2. Parse statement & check duplicates + categorization
        val parsedRows = parseUseCase(
            csvText = csvStatement,
            config = config,
            mapping = mapping,
            destinationAccountId = testAccountId
        )

        assertEquals(5, parsedRows.size)

        // Row 1: Exact duplicate of existing Whole Foods tx
        assertTrue(parsedRows[0].isValid)
        assertEquals(DuplicateStatus.EXACT_DUPLICATE, parsedRows[0].duplicateStatus)
        assertEquals("tx_existing_wf", parsedRows[0].duplicateTransactionId)
        assertTrue(parsedRows[0].isExcluded) // Excluded by default

        // Row 2: Acme Corp Payroll (New income, categorized by rule)
        assertTrue(parsedRows[1].isValid)
        assertEquals(DuplicateStatus.NEW, parsedRows[1].duplicateStatus)
        assertEquals(320000L, parsedRows[1].parsedAmountMinor)
        assertEquals(TransactionType.INCOME, parsedRows[1].parsedType)
        assertFalse(parsedRows[1].isExcluded)

        // Row 3: Netflix (New expense)
        assertTrue(parsedRows[2].isValid)
        assertEquals(DuplicateStatus.NEW, parsedRows[2].duplicateStatus)
        assertFalse(parsedRows[2].isExcluded)

        // Row 4: Duplicate Netflix in same file (Intra-batch duplicate)
        assertTrue(parsedRows[3].isValid)
        assertEquals(DuplicateStatus.EXACT_DUPLICATE, parsedRows[3].duplicateStatus)
        assertTrue(parsedRows[3].isExcluded) // Excluded by default

        // Row 5: Corrupted row
        assertFalse(parsedRows[4].isValid)
        assertNotNull(parsedRows[4].errorReason)

        // 3. Execute import with the parsed rows
        val summary = executeImportUseCase(
            destinationAccountId = testAccountId,
            allRows = parsedRows
        )

        // 4. Assert summary statistics
        assertEquals(5, summary.totalRows)
        assertEquals(2, summary.importedCount) // Acme Payroll & 1st Netflix
        assertEquals(2, summary.skippedDuplicateCount) // WF duplicate & 2nd Netflix
        assertEquals(1, summary.invalidCount) // Corrupted row
        assertEquals(320000L, summary.totalIncomeMinor)
        assertEquals(1599L, summary.totalExpenseMinor)
        assertEquals("Checking Account", summary.destinationAccountName)

        // 5. Verify created transactions in repository
        val allTx = transactionRepository.getAllTransactions()
        assertEquals(3, allTx.size) // 1 existing + 2 newly imported
        val payrollTx = allTx.find { it.description.contains("Payroll") }
        assertNotNull(payrollTx)
        assertEquals(TransactionType.INCOME, payrollTx!!.type)
        assertEquals(320000L, payrollTx.amount.amountMinor)
    }

    @Test
    fun testManageProfilesLifecycle() = runBlocking {
        // Seed default profiles
        manageProfilesUseCase.seedDefaultProfilesIfNeeded()
        val profiles = manageProfilesUseCase.getAllProfiles()
        assertTrue(profiles.isNotEmpty())
        assertTrue(profiles.any { it.institution == "Chase" })

        // Save a custom profile
        val customProfile = ImportProfile(
            id = "profile_my_credit_union",
            name = "My Credit Union Checking",
            institution = "Credit Union",
            formatConfig = md.alexlab.finpulse.domain.model.CsvFormatConfig(delimiter = ';'),
            columnMapping = md.alexlab.finpulse.domain.model.CsvColumnMapping(dateColumnIndex = 0, amountColumnIndex = 1, descriptionColumnIndex = 2)
        )
        manageProfilesUseCase.saveProfile(customProfile)

        val retrieved = manageProfilesUseCase.getProfileById("profile_my_credit_union")
        assertNotNull(retrieved)
        assertEquals("My Credit Union Checking", retrieved!!.name)
        assertEquals(';', retrieved.formatConfig.delimiter)

        // Delete profile
        manageProfilesUseCase.deleteProfile("profile_my_credit_union")
        val afterDelete = manageProfilesUseCase.getProfileById("profile_my_credit_union")
        assertEquals(null, afterDelete)
    }
}

// In-memory fake test implementations
private class FakeTransactionRepo : TransactionRepository {
    private val txs = mutableListOf<Transaction>()
    private val flow = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getAllTransactionsFlow(): Flow<List<Transaction>> = flow.asStateFlow()
    fun getAllTransactions(): List<Transaction> = txs.toList()

    override fun getRecentTransactionsFlow(limit: Int): Flow<List<Transaction>> = flow.map { it.take(limit) }
    override fun getTransactionsByAccountFlow(accountId: String): Flow<List<Transaction>> =
        flow.map { list -> list.filter { it.sourceAccountId == accountId || it.destinationAccountId == accountId } }
    override fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<Transaction>> =
        flow.map { list -> list.filter { it.categoryId == categoryId } }
    override fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        flow.map { list -> list.filter { it.timestamp in startDate..endDate } }
    override fun searchTransactionsFlow(query: String): Flow<List<Transaction>> =
        flow.map { list -> list.filter { it.description.contains(query, true) || it.merchant?.contains(query, true) == true } }

    override suspend fun getTransactionById(id: String): Transaction? = txs.find { it.id == id }
    override suspend fun createTransaction(transaction: Transaction) {
        txs.add(0, transaction)
        flow.value = txs.toList()
    }
    override suspend fun updateTransaction(transaction: Transaction) {
        val idx = txs.indexOfFirst { it.id == transaction.id }
        if (idx != -1) {
            txs[idx] = transaction
            flow.value = txs.toList()
        }
    }
    override suspend fun deleteTransaction(id: String) {
        txs.removeAll { it.id == id }
        flow.value = txs.toList()
    }

    override fun getFrequentCategoryIdsFlow(type: TransactionType, limit: Int): Flow<List<String>> =
        flow.map { list -> list.filter { it.type == type }.groupBy { it.categoryId }.keys.take(limit) }
    override fun getFrequentMerchantsFlow(limit: Int): Flow<List<String>> =
        flow.map { list -> list.mapNotNull { it.merchant }.distinct().take(limit) }
    override suspend fun getSuggestedCategoryForMerchant(merchant: String): String? = null
    override fun getUnreviewedTransactionsFlow(): Flow<List<Transaction>> =
        flow.map { list -> list.filter { !it.isCategoryConfirmed } }
    override fun getUnreviewedCountFlow(): Flow<Int> =
        flow.map { list -> list.count { !it.isCategoryConfirmed } }
    override suspend fun confirmTransactionCategory(id: String, categoryId: String, matchedRuleId: String?, confidence: Float) {}
    override suspend fun bulkUpdateCategory(ids: List<String>, categoryId: String, isConfirmed: Boolean, matchedRuleId: String?) {}
}

private class FakeAccountRepo : AccountRepository {
    private val accounts = mutableListOf<Account>()
    private val flow = MutableStateFlow<List<Account>>(emptyList())

    override fun getAllAccountsFlow(): Flow<List<Account>> = flow.asStateFlow()
    override fun getActiveAccountsFlow(): Flow<List<Account>> = flow.map { list -> list.filter { !it.isArchived } }
    override suspend fun getAccountById(id: String): Account? = accounts.find { it.id == id }
    override suspend fun saveAccount(account: Account) {
        accounts.removeAll { it.id == account.id }
        accounts.add(account)
        flow.value = accounts.toList()
    }
    override suspend fun deleteAccount(id: String) {
        accounts.removeAll { it.id == id }
        flow.value = accounts.toList()
    }
    override suspend fun setArchived(id: String, isArchived: Boolean) {}
    override suspend fun updateBalances(accountId: String, balance: Money, availableBalance: Money) {}
}

private class FakeCategoryRepo : CategoryRepository {
    private val categories = mutableListOf<Category>()
    override fun getAllCategoriesFlow(): Flow<List<Category>> = MutableStateFlow(categories)
    override fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<Category>> = MutableStateFlow(categories.filter { it.type == type })
    override suspend fun getCategoryById(id: String): Category? = categories.find { it.id == id }
    override suspend fun saveCategory(category: Category) { categories.add(category) }
    override suspend fun seedDefaultCategoriesIfNeeded() {}
}

private class FakeSignalRepo : MerchantSignalRepository {
    private val signals = mutableMapOf<String, MerchantSignal>()
    override fun getAllSignalsFlow(): Flow<List<MerchantSignal>> = MutableStateFlow(signals.values.toList())
    override suspend fun getAllSignals(): List<MerchantSignal> = signals.values.toList()
    override suspend fun getSignal(normalizedMerchant: String): MerchantSignal? = signals[normalizedMerchant]
    override suspend fun recordSignal(merchant: String, categoryId: String) {
        signals[merchant] = MerchantSignal(merchant, categoryId, 1, System.currentTimeMillis())
    }
    override suspend fun clearAllSignals() { signals.clear() }
}

private class FakeRuleRepo : CategorizationRuleRepository {
    private val rules = mutableListOf<CategorizationRule>()
    override fun getAllRulesFlow(): Flow<List<CategorizationRule>> = MutableStateFlow(rules)
    override fun getActiveRulesFlow(): Flow<List<CategorizationRule>> = MutableStateFlow(rules.filter { it.isActive })
    override suspend fun getActiveRules(): List<CategorizationRule> = rules.filter { it.isActive }
    override suspend fun getRuleById(id: String): CategorizationRule? = rules.find { it.id == id }
    override suspend fun saveRule(rule: CategorizationRule) {
        rules.removeAll { it.id == rule.id }
        rules.add(rule)
    }
    override suspend fun deleteRule(id: String) { rules.removeAll { it.id == id } }
    override suspend fun setRuleActive(id: String, isActive: Boolean) {}
    override suspend fun updateRulePriority(id: String, priority: Int) {}
    override suspend fun seedDefaultRulesIfNeeded() {}
}

private class FakeProfileRepo : ImportProfileRepository {
    private val profiles = mutableListOf<ImportProfile>()
    override fun getAllProfilesFlow(): Flow<List<ImportProfile>> = MutableStateFlow(profiles)
    override suspend fun getAllProfiles(): List<ImportProfile> = profiles.toList()
    override suspend fun getProfileById(id: String): ImportProfile? = profiles.find { it.id == id }
    override suspend fun saveProfile(profile: ImportProfile) {
        profiles.removeAll { it.id == profile.id }
        profiles.add(profile)
    }
    override suspend fun deleteProfile(id: String) { profiles.removeAll { it.id == id } }
    override suspend fun seedDefaultProfilesIfNeeded() {
        if (profiles.isEmpty()) {
            profiles.add(
                ImportProfile(
                    id = "profile_chase",
                    name = "Chase",
                    institution = "Chase",
                    formatConfig = md.alexlab.finpulse.domain.model.CsvFormatConfig(),
                    columnMapping = md.alexlab.finpulse.domain.model.CsvColumnMapping(0, 1, 2)
                )
            )
        }
    }
}
