package md.alexlab.finpulse.domain.repository

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.Budget
import md.alexlab.finpulse.domain.model.CategorizationRule
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.Debt
import md.alexlab.finpulse.domain.model.FinancialGoal
import md.alexlab.finpulse.domain.model.InvestmentAsset
import md.alexlab.finpulse.domain.model.MerchantSignal
import md.alexlab.finpulse.domain.model.RecurringOccurrence
import md.alexlab.finpulse.domain.model.RecurringTransaction
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccountsFlow(): Flow<List<Account>>
    fun getActiveAccountsFlow(): Flow<List<Account>>
    suspend fun getAccountById(id: String): Account?
    suspend fun saveAccount(account: Account)
    suspend fun deleteAccount(id: String)
    suspend fun setArchived(id: String, isArchived: Boolean)
    suspend fun updateBalances(accountId: String, balance: Money, availableBalance: Money)
}

interface TransactionRepository {
    fun getAllTransactionsFlow(): Flow<List<Transaction>>
    fun getRecentTransactionsFlow(limit: Int = 10): Flow<List<Transaction>>
    fun getTransactionsByAccountFlow(accountId: String): Flow<List<Transaction>>
    suspend fun getTransactionCountForAccount(accountId: String): Int = 0
    fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<Transaction>>
    fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun searchTransactionsFlow(query: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun createTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
    fun getFrequentCategoryIdsFlow(type: TransactionType, limit: Int = 6): Flow<List<String>>
    fun getFrequentMerchantsFlow(limit: Int = 8): Flow<List<String>>
    suspend fun getSuggestedCategoryForMerchant(merchant: String): String?
    fun getUnreviewedTransactionsFlow(): Flow<List<Transaction>>
    fun getUnreviewedCountFlow(): Flow<Int>
    suspend fun confirmTransactionCategory(id: String, categoryId: String, matchedRuleId: String? = null, confidence: Float = 1.0f)
    suspend fun bulkUpdateCategory(ids: List<String>, categoryId: String, isConfirmed: Boolean = true, matchedRuleId: String? = null)
    fun filterTransactionsFlow(params: md.alexlab.finpulse.domain.model.TransactionFilterParams): Flow<List<Transaction>> =
        kotlinx.coroutines.flow.emptyFlow()
    suspend fun filterTransactions(params: md.alexlab.finpulse.domain.model.TransactionFilterParams): List<Transaction> =
        emptyList()
}

interface SavedFilterRepository {
    fun getAllSavedFiltersFlow(): Flow<List<md.alexlab.finpulse.domain.model.SavedFilter>>
    suspend fun getSavedFilterById(id: String): md.alexlab.finpulse.domain.model.SavedFilter?
    suspend fun saveFilter(savedFilter: md.alexlab.finpulse.domain.model.SavedFilter)
    suspend fun deleteFilter(id: String)
}

interface CategoryRepository {
    fun getAllCategoriesFlow(): Flow<List<Category>>
    fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun saveCategory(category: Category)
    suspend fun seedDefaultCategoriesIfNeeded()
}

interface BudgetRepository {
    fun getAllActiveBudgetsFlow(): Flow<List<Budget>>
    suspend fun getBudgetById(id: String): Budget?
    suspend fun saveBudget(budget: Budget)
    suspend fun deleteBudget(id: String)
}

interface RecurringRepository {
    fun getAllRecurringFlow(): Flow<List<RecurringTransaction>>
    fun getActiveRecurringFlow(): Flow<List<RecurringTransaction>>
    fun getActiveSubscriptionsFlow(): Flow<List<RecurringTransaction>>
    suspend fun getRecurringById(id: String): RecurringTransaction?
    suspend fun getDueRecurring(timestamp: Long): List<RecurringTransaction>
    suspend fun saveRecurring(recurring: RecurringTransaction)
    suspend fun deleteRecurring(id: String)
    suspend fun setRuleActive(id: String, isActive: Boolean)
    suspend fun cancelRule(id: String)

    // Occurrences
    suspend fun getOccurrenceById(id: String): RecurringOccurrence?
    fun getOccurrencesInRangeFlow(startDate: Long, endDate: Long): Flow<List<RecurringOccurrence>>
    suspend fun getOccurrencesInRange(startDate: Long, endDate: Long): List<RecurringOccurrence>
    suspend fun saveOccurrence(occurrence: RecurringOccurrence)
    suspend fun markOccurrencePaid(occurrenceId: String, paidDate: Long, transactionId: String)
    suspend fun markOccurrenceSkipped(occurrenceId: String)
    suspend fun advanceRuleDueDate(ruleId: String, newDueDate: Long, processedDate: Long)
    suspend fun processDueRecurringTransactions(nowMillis: Long = System.currentTimeMillis())
}

interface GoalRepository {
    fun getAllGoalsFlow(): Flow<List<FinancialGoal>>
    suspend fun getGoalById(id: String): FinancialGoal?
    suspend fun saveGoal(goal: FinancialGoal)
    suspend fun deleteGoal(id: String)
    suspend fun contributeToGoal(goalId: String, amount: Money)
}

interface InvestmentRepository {
    fun getAllAssetsFlow(): Flow<List<InvestmentAsset>>
    suspend fun getAssetById(id: String): InvestmentAsset?
    suspend fun saveAsset(asset: InvestmentAsset)
    suspend fun deleteAsset(id: String)
    suspend fun updateAssetPrice(id: String, currentPrice: Money)
}

interface DebtRepository {
    fun getAllDebtsFlow(): Flow<List<Debt>>
    suspend fun getDebtById(id: String): Debt?
    suspend fun saveDebt(debt: Debt)
    suspend fun deleteDebt(id: String)
    suspend fun makePayment(debtId: String, paymentAmount: Money)
}

interface CategorizationRuleRepository {
    fun getAllRulesFlow(): Flow<List<CategorizationRule>>
    fun getActiveRulesFlow(): Flow<List<CategorizationRule>>
    suspend fun getActiveRules(): List<CategorizationRule>
    suspend fun getRuleById(id: String): CategorizationRule?
    suspend fun saveRule(rule: CategorizationRule)
    suspend fun deleteRule(id: String)
    suspend fun setRuleActive(id: String, isActive: Boolean)
    suspend fun updateRulePriority(id: String, priority: Int)
    suspend fun seedDefaultRulesIfNeeded()
}

interface MerchantSignalRepository {
    fun getAllSignalsFlow(): Flow<List<MerchantSignal>>
    suspend fun getAllSignals(): List<MerchantSignal>
    suspend fun getSignal(normalizedMerchant: String): MerchantSignal?
    suspend fun recordSignal(merchant: String, categoryId: String)
    suspend fun clearAllSignals()
}

interface ImportProfileRepository {
    fun getAllProfilesFlow(): Flow<List<md.alexlab.finpulse.domain.model.ImportProfile>>
    suspend fun getAllProfiles(): List<md.alexlab.finpulse.domain.model.ImportProfile>
    suspend fun getProfileById(id: String): md.alexlab.finpulse.domain.model.ImportProfile?
    suspend fun saveProfile(profile: md.alexlab.finpulse.domain.model.ImportProfile)
    suspend fun deleteProfile(id: String)
    suspend fun seedDefaultProfilesIfNeeded()
}

