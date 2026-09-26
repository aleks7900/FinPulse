package com.finpulse.app.di

import android.content.Context
import com.finpulse.app.core.database.FinPulseDatabase
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.data.repository.AccountRepositoryImpl
import com.finpulse.app.data.repository.BudgetRepositoryImpl
import com.finpulse.app.data.repository.CategoryRepositoryImpl
import com.finpulse.app.data.repository.DebtRepositoryImpl
import com.finpulse.app.data.repository.GoalRepositoryImpl
import com.finpulse.app.data.repository.InvestmentRepositoryImpl
import com.finpulse.app.data.repository.RecurringRepositoryImpl
import com.finpulse.app.data.repository.TransactionRepositoryImpl
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.BudgetRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.DebtRepository
import com.finpulse.app.domain.repository.GoalRepository
import com.finpulse.app.domain.repository.InvestmentRepository
import com.finpulse.app.domain.repository.RecurringRepository
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.transaction.CreateTransactionUseCase
import com.finpulse.app.domain.usecase.transaction.GetQuickAddSuggestionsUseCase

interface AppContainer {
    val database: FinPulseDatabase
    val userPreferencesDataStore: UserPreferencesDataStore
    val accountRepository: AccountRepository
    val transactionRepository: TransactionRepository
    val categoryRepository: CategoryRepository
    val budgetRepository: BudgetRepository
    val recurringRepository: RecurringRepository
    val goalRepository: GoalRepository
    val investmentRepository: InvestmentRepository
    val debtRepository: DebtRepository
    val createTransactionUseCase: CreateTransactionUseCase
    val getQuickAddSuggestionsUseCase: GetQuickAddSuggestionsUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: FinPulseDatabase by lazy {
        FinPulseDatabase.getInstance(context)
    }

    override val userPreferencesDataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(context)
    }

    override val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(database)
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database)
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database)
    }

    override val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(database)
    }

    override val recurringRepository: RecurringRepository by lazy {
        RecurringRepositoryImpl(database, transactionRepository)
    }

    override val goalRepository: GoalRepository by lazy {
        GoalRepositoryImpl(database)
    }

    override val investmentRepository: InvestmentRepository by lazy {
        InvestmentRepositoryImpl(database)
    }

    override val debtRepository: DebtRepository by lazy {
        DebtRepositoryImpl(database)
    }

    override val createTransactionUseCase: CreateTransactionUseCase by lazy {
        CreateTransactionUseCase(transactionRepository, accountRepository)
    }

    override val getQuickAddSuggestionsUseCase: GetQuickAddSuggestionsUseCase by lazy {
        GetQuickAddSuggestionsUseCase(transactionRepository, categoryRepository, accountRepository, userPreferencesDataStore)
    }
}
