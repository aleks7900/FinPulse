package com.finpulse.app.domain.repository

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.Debt
import com.finpulse.app.domain.model.FinancialGoal
import com.finpulse.app.domain.model.InvestmentAsset
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.Transaction
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
    fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<Transaction>>
    fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun searchTransactionsFlow(query: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun createTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
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
    fun getActiveSubscriptionsFlow(): Flow<List<RecurringTransaction>>
    suspend fun getDueRecurring(timestamp: Long): List<RecurringTransaction>
    suspend fun saveRecurring(recurring: RecurringTransaction)
    suspend fun deleteRecurring(id: String)
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
