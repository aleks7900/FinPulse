package com.finpulse.app.data.repository

import com.finpulse.app.core.database.FinPulseDatabase
import com.finpulse.app.core.database.entity.CategoryEntity
import com.finpulse.app.core.model.Money
import com.finpulse.app.data.mapper.toDomain
import com.finpulse.app.data.mapper.toEntity
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.Debt
import com.finpulse.app.domain.model.FinancialGoal
import com.finpulse.app.domain.model.InvestmentAsset
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.BudgetRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.DebtRepository
import com.finpulse.app.domain.repository.GoalRepository
import com.finpulse.app.domain.repository.InvestmentRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AccountRepositoryImpl(private val database: FinPulseDatabase) : AccountRepository {
    private val dao = database.accountDao()

    override fun getAllAccountsFlow(): Flow<List<Account>> =
        dao.getAllAccountsFlow().map { list -> list.map { it.toDomain() } }

    override fun getActiveAccountsFlow(): Flow<List<Account>> =
        dao.getActiveAccountsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getAccountById(id: String): Account? =
        dao.getAccountById(id)?.toDomain()

    override suspend fun saveAccount(account: Account) {
        dao.insertAccount(account.toEntity())
    }

    override suspend fun deleteAccount(id: String) {
        dao.deleteAccountById(id)
    }

    override suspend fun setArchived(id: String, isArchived: Boolean) {
        dao.setArchived(id, isArchived)
    }

    override suspend fun updateBalances(accountId: String, balance: Money, availableBalance: Money) {
        dao.updateBalances(accountId, balance.amountMinor, availableBalance.amountMinor)
    }
}

class TransactionRepositoryImpl(private val database: FinPulseDatabase) : TransactionRepository {
    private val txDao = database.transactionDao()
    private val accountDao = database.accountDao()

    override fun getAllTransactionsFlow(): Flow<List<Transaction>> =
        txDao.getAllTransactionsFlow().map { list -> list.map { it.toDomain() } }

    override fun getRecentTransactionsFlow(limit: Int): Flow<List<Transaction>> =
        txDao.getRecentTransactionsFlow(limit).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByAccountFlow(accountId: String): Flow<List<Transaction>> =
        txDao.getTransactionsByAccountFlow(accountId).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByCategoryFlow(categoryId: String): Flow<List<Transaction>> =
        txDao.getTransactionsByCategoryFlow(categoryId).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        txDao.getTransactionsByDateRangeFlow(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override fun searchTransactionsFlow(query: String): Flow<List<Transaction>> =
        txDao.searchTransactionsFlow(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getTransactionById(id: String): Transaction? =
        txDao.getTransactionById(id)?.toDomain()

    override suspend fun createTransaction(transaction: Transaction) {
        // Enforce balance update in accounts
        applyTransactionBalanceChange(transaction, isReversal = false)
        txDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val existing = txDao.getTransactionById(transaction.id)?.toDomain()
        if (existing != null) {
            // Reverse previous balance change
            applyTransactionBalanceChange(existing, isReversal = true)
        }
        // Apply new balance change
        applyTransactionBalanceChange(transaction, isReversal = false)
        txDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String) {
        val existing = txDao.getTransactionById(id)?.toDomain()
        if (existing != null) {
            applyTransactionBalanceChange(existing, isReversal = true)
            txDao.deleteTransactionById(id)
        }
    }

    private suspend fun applyTransactionBalanceChange(tx: Transaction, isReversal: Boolean) {
        val multiplier = if (isReversal) -1 else 1

        val sourceAccount = accountDao.getAccountById(tx.sourceAccountId) ?: return
        val currentBalance = sourceAccount.balanceMinor
        val currentAvailable = sourceAccount.availableBalanceMinor

        when (tx.type) {
            TransactionType.EXPENSE -> {
                // Deduction: normal is -amount, reversal is +amount
                val delta = tx.amount.amountMinor * multiplier
                accountDao.updateBalances(
                    accountId = tx.sourceAccountId,
                    balanceMinor = currentBalance - delta,
                    availableBalanceMinor = currentAvailable - delta
                )
            }
            TransactionType.INCOME, TransactionType.REFUND -> {
                // Addition: normal is +amount, reversal is -amount
                val delta = tx.amount.amountMinor * multiplier
                accountDao.updateBalances(
                    accountId = tx.sourceAccountId,
                    balanceMinor = currentBalance + delta,
                    availableBalanceMinor = currentAvailable + delta
                )
            }
            TransactionType.TRANSFER -> {
                // Deduct from source
                val delta = tx.amount.amountMinor * multiplier
                accountDao.updateBalances(
                    accountId = tx.sourceAccountId,
                    balanceMinor = currentBalance - delta,
                    availableBalanceMinor = currentAvailable - delta
                )

                // Add to destination
                if (tx.destinationAccountId != null) {
                    val destAccount = accountDao.getAccountById(tx.destinationAccountId)
                    if (destAccount != null) {
                        accountDao.updateBalances(
                            accountId = tx.destinationAccountId,
                            balanceMinor = destAccount.balanceMinor + delta,
                            availableBalanceMinor = destAccount.availableBalanceMinor + delta
                        )
                    }
                }
            }
        }
    }
}

class CategoryRepositoryImpl(private val database: FinPulseDatabase) : CategoryRepository {
    private val dao = database.categoryDao()

    override fun getAllCategoriesFlow(): Flow<List<Category>> =
        dao.getAllCategoriesFlow().map { list -> list.map { it.toDomain() } }

    override fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<Category>> =
        dao.getCategoriesByTypeFlow(type.name).map { list -> list.map { it.toDomain() } }

    override suspend fun getCategoryById(id: String): Category? =
        dao.getCategoryById(id)?.toDomain()

    override suspend fun saveCategory(category: Category) {
        dao.insertCategory(category.toEntity())
    }

    override suspend fun seedDefaultCategoriesIfNeeded() {
        if (dao.getCategoryCount() > 0) return

        val defaults = listOf(
            CategoryEntity("cat_food", "Food & Dining", "EXPENSE", null, "fastfood", 0xFFFF9800, true, 1),
            CategoryEntity("cat_groceries", "Groceries", "EXPENSE", "cat_food", "shopping", 0xFF4CAF50, true, 2),
            CategoryEntity("cat_housing", "Housing & Rent", "EXPENSE", null, "home", 0xFF3F51B5, true, 3),
            CategoryEntity("cat_transport", "Transportation", "EXPENSE", null, "transport", 0xFF2196F3, true, 4),
            CategoryEntity("cat_fuel", "Fuel & Gas", "EXPENSE", "cat_transport", "fuel", 0xFF00BCD4, true, 5),
            CategoryEntity("cat_shopping", "Shopping", "EXPENSE", null, "shopping", 0xFFE91E63, true, 6),
            CategoryEntity("cat_entertainment", "Entertainment", "EXPENSE", null, "entertainment", 0xFF9C27B0, true, 7),
            CategoryEntity("cat_health", "Health & Medical", "EXPENSE", null, "health", 0xFFF44336, true, 8),
            CategoryEntity("cat_education", "Education", "EXPENSE", null, "education", 0xFF795548, true, 9),
            CategoryEntity("cat_fitness", "Fitness & Sports", "EXPENSE", null, "fitness", 0xFF009688, true, 10),
            CategoryEntity("cat_subscriptions", "Subscriptions", "EXPENSE", null, "subscriptions", 0xFF673AB7, true, 11),
            CategoryEntity("cat_gifts", "Gifts & Donations", "EXPENSE", null, "gifts", 0xFFFF4081, true, 12),
            CategoryEntity("cat_salary", "Salary", "INCOME", null, "salary", 0xFF4CAF50, true, 1),
            CategoryEntity("cat_freelance", "Freelance & Consulting", "INCOME", null, "work", 0xFF8BC34A, true, 2),
            CategoryEntity("cat_invest_return", "Investment Return", "INCOME", null, "investments", 0xFF009688, true, 3),
            CategoryEntity("cat_other_income", "Other Income", "INCOME", null, "paid", 0xFF607D8B, true, 4)
        )
        dao.insertCategories(defaults)
    }
}

class BudgetRepositoryImpl(private val database: FinPulseDatabase) : BudgetRepository {
    private val dao = database.budgetDao()

    override fun getAllActiveBudgetsFlow(): Flow<List<Budget>> =
        dao.getAllActiveBudgetsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getBudgetById(id: String): Budget? =
        dao.getBudgetById(id)?.toDomain()

    override suspend fun saveBudget(budget: Budget) {
        dao.insertBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(id: String) {
        dao.deleteBudgetById(id)
    }
}

class RecurringRepositoryImpl(
    private val database: FinPulseDatabase,
    private val transactionRepository: TransactionRepository
) : RecurringRepository {
    private val dao = database.recurringTransactionDao()

    override fun getAllRecurringFlow(): Flow<List<RecurringTransaction>> =
        dao.getAllRecurringFlow().map { list -> list.map { it.toDomain() } }

    override fun getActiveSubscriptionsFlow(): Flow<List<RecurringTransaction>> =
        dao.getActiveSubscriptionsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getDueRecurring(timestamp: Long): List<RecurringTransaction> =
        dao.getDueRecurring(timestamp).map { it.toDomain() }

    override suspend fun saveRecurring(recurring: RecurringTransaction) {
        dao.insertRecurring(recurring.toEntity())
    }

    override suspend fun deleteRecurring(id: String) {
        dao.deleteRecurringById(id)
    }

    override suspend fun processDueRecurringTransactions(nowMillis: Long) {
        val dueList = dao.getDueRecurring(nowMillis)
        for (item in dueList) {
            // Generate automatic transaction for this recurring entry
            val frequency = try { PaymentFrequency.valueOf(item.frequency) } catch (_: Exception) { PaymentFrequency.MONTHLY }
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                amount = Money(item.amountMinor, item.currencyCode),
                type = TransactionType.EXPENSE,
                sourceAccountId = item.accountId,
                categoryId = item.categoryId,
                merchant = item.title,
                timestamp = item.nextDueDate,
                description = "Recurring: ${item.title}",
                recurringRuleId = item.id
            )
            transactionRepository.createTransaction(tx)

            // Calculate next due date
            val nextDue = calculateNextDueDate(item.nextDueDate, frequency)
            dao.updateProcessedDate(item.id, nextDue, nowMillis)
        }
    }

    private fun calculateNextDueDate(currentDueDate: Long, frequency: PaymentFrequency): Long {
        val millisPerDay = 86_400_000L
        return currentDueDate + (frequency.approxDays * millisPerDay)
    }
}

class GoalRepositoryImpl(private val database: FinPulseDatabase) : GoalRepository {
    private val dao = database.financialGoalDao()

    override fun getAllGoalsFlow(): Flow<List<FinancialGoal>> =
        dao.getAllGoalsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getGoalById(id: String): FinancialGoal? =
        dao.getGoalById(id)?.toDomain()

    override suspend fun saveGoal(goal: FinancialGoal) {
        dao.insertGoal(goal.toEntity())
    }

    override suspend fun deleteGoal(id: String) {
        dao.deleteGoalById(id)
    }

    override suspend fun contributeToGoal(goalId: String, amount: Money) {
        val goal = dao.getGoalById(goalId) ?: return
        val newAmount = goal.currentAmountMinor + amount.amountMinor
        val isCompleted = newAmount >= goal.targetAmountMinor
        dao.updateProgress(goalId, newAmount, isCompleted)
    }
}

class InvestmentRepositoryImpl(private val database: FinPulseDatabase) : InvestmentRepository {
    private val dao = database.assetDao()

    override fun getAllAssetsFlow(): Flow<List<InvestmentAsset>> =
        dao.getAllAssetsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getAssetById(id: String): InvestmentAsset? =
        dao.getAssetById(id)?.toDomain()

    override suspend fun saveAsset(asset: InvestmentAsset) {
        dao.insertAsset(asset.toEntity())
    }

    override suspend fun deleteAsset(id: String) {
        dao.deleteAssetById(id)
    }

    override suspend fun updateAssetPrice(id: String, currentPrice: Money) {
        dao.updatePrice(id, currentPrice.amountMinor)
    }
}

class DebtRepositoryImpl(private val database: FinPulseDatabase) : DebtRepository {
    private val dao = database.debtDao()

    override fun getAllDebtsFlow(): Flow<List<Debt>> =
        dao.getAllDebtsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getDebtById(id: String): Debt? =
        dao.getDebtById(id)?.toDomain()

    override suspend fun saveDebt(debt: Debt) {
        dao.insertDebt(debt.toEntity())
    }

    override suspend fun deleteDebt(id: String) {
        dao.deleteDebtById(id)
    }

    override suspend fun makePayment(debtId: String, paymentAmount: Money) {
        val debt = dao.getDebtById(debtId) ?: return
        val newBalance = (debt.remainingBalanceMinor - paymentAmount.amountMinor).coerceAtLeast(0L)
        dao.updateBalance(debtId, newBalance)
    }
}
