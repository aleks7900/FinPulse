package com.finpulse.app.data.mapper

import com.finpulse.app.core.database.entity.AccountEntity
import com.finpulse.app.core.database.entity.AssetEntity
import com.finpulse.app.core.database.entity.BudgetEntity
import com.finpulse.app.core.database.entity.CategoryEntity
import com.finpulse.app.core.database.entity.DebtEntity
import com.finpulse.app.core.database.entity.FinancialGoalEntity
import com.finpulse.app.core.database.entity.RecurringTransactionEntity
import com.finpulse.app.core.database.entity.TransactionEntity
import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.AssetClass
import com.finpulse.app.domain.model.Budget
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.Debt
import com.finpulse.app.core.database.entity.RecurringOccurrenceEntity
import com.finpulse.app.domain.model.CustomIntervalUnit
import com.finpulse.app.domain.model.DebtType
import com.finpulse.app.domain.model.FinancialGoal
import com.finpulse.app.domain.model.InvestmentAsset
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType

// Account Mappers
fun AccountEntity.toDomain(): Account {
    val currency = currencyCode
    return Account(
        id = id,
        name = name,
        type = try { AccountType.valueOf(type) } catch (_: Exception) { AccountType.OTHER },
        balance = Money(balanceMinor, currency),
        availableBalance = Money(availableBalanceMinor, currency),
        creditLimit = creditLimitMinor?.let { Money(it, currency) },
        institution = institution,
        icon = icon,
        colorHex = colorHex,
        notes = notes,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        type = type.name,
        balanceMinor = balance.amountMinor,
        availableBalanceMinor = availableBalance.amountMinor,
        creditLimitMinor = creditLimit?.amountMinor,
        currencyCode = balance.currencyCode,
        institution = institution,
        icon = icon,
        colorHex = colorHex,
        notes = notes,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// Transaction Mappers
fun TransactionEntity.toDomain(): Transaction {
    val currency = currencyCode
    val tagList = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return Transaction(
        id = id,
        amount = Money(amountMinor, currency),
        type = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.EXPENSE },
        sourceAccountId = sourceAccountId,
        destinationAccountId = destinationAccountId,
        categoryId = categoryId,
        merchant = merchant,
        timestamp = timestamp,
        description = description,
        tags = tagList,
        notes = notes,
        recurringRuleId = recurringRuleId,
        isExcludedFromBudget = isExcludedFromBudget,
        createdAt = createdAt
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amountMinor = amount.amountMinor,
        currencyCode = amount.currencyCode,
        type = type.name,
        sourceAccountId = sourceAccountId,
        destinationAccountId = destinationAccountId,
        categoryId = categoryId,
        merchant = merchant,
        timestamp = timestamp,
        description = description,
        tags = tags.joinToString(","),
        notes = notes,
        recurringRuleId = recurringRuleId,
        isExcludedFromBudget = isExcludedFromBudget,
        createdAt = createdAt
    )
}

// Category Mappers
fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        type = try { CategoryType.valueOf(type) } catch (_: Exception) { CategoryType.EXPENSE },
        parentCategoryId = parentCategoryId,
        icon = icon,
        colorHex = colorHex,
        isDefault = isDefault,
        sortOrder = sortOrder
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        type = type.name,
        parentCategoryId = parentCategoryId,
        icon = icon,
        colorHex = colorHex,
        isDefault = isDefault,
        sortOrder = sortOrder
    )
}

// Budget Mappers
fun BudgetEntity.toDomain(): Budget {
    return Budget(
        id = id,
        categoryId = categoryId,
        name = name,
        limitAmount = Money(limitAmountMinor, currencyCode),
        periodType = try { BudgetPeriod.valueOf(periodType) } catch (_: Exception) { BudgetPeriod.MONTHLY },
        startDate = startDate,
        endDate = endDate,
        notifyAt70 = notifyAt70,
        notifyAt90 = notifyAt90,
        notifyAt100 = notifyAt100,
        isArchived = isArchived
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        categoryId = categoryId,
        name = name,
        limitAmountMinor = limitAmount.amountMinor,
        currencyCode = limitAmount.currencyCode,
        periodType = periodType.name,
        startDate = startDate,
        endDate = endDate,
        notifyAt70 = notifyAt70,
        notifyAt90 = notifyAt90,
        notifyAt100 = notifyAt100,
        isArchived = isArchived
    )
}

// Recurring Mappers
fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
    return RecurringTransaction(
        id = id,
        title = title,
        amount = Money(amountMinor, currencyCode),
        type = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.EXPENSE },
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        categoryId = categoryId,
        frequency = try { PaymentFrequency.valueOf(frequency) } catch (_: Exception) { PaymentFrequency.MONTHLY },
        customIntervalValue = customIntervalValue,
        customIntervalUnit = try { CustomIntervalUnit.valueOf(customIntervalUnit) } catch (_: Exception) { CustomIntervalUnit.MONTHS },
        anchorDayOfMonth = anchorDayOfMonth,
        nextDueDate = nextDueDate,
        lastProcessedDate = lastProcessedDate,
        isActive = isActive,
        isCancelled = isCancelled,
        isSubscription = isSubscription,
        isVariableAmount = isVariableAmount,
        reminderDaysBefore = reminderDaysBefore,
        notes = notes
    )
}

fun RecurringTransaction.toEntity(): RecurringTransactionEntity {
    return RecurringTransactionEntity(
        id = id,
        title = title,
        amountMinor = amount.amountMinor,
        currencyCode = amount.currencyCode,
        type = type.name,
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        categoryId = categoryId,
        frequency = frequency.name,
        customIntervalValue = customIntervalValue,
        customIntervalUnit = customIntervalUnit.name,
        anchorDayOfMonth = anchorDayOfMonth,
        nextDueDate = nextDueDate,
        lastProcessedDate = lastProcessedDate,
        isActive = isActive,
        isCancelled = isCancelled,
        isSubscription = isSubscription,
        isVariableAmount = isVariableAmount,
        reminderDaysBefore = reminderDaysBefore,
        notes = notes
    )
}

fun RecurringOccurrenceEntity.toDomain(rule: RecurringTransaction): RecurringOccurrence {
    val occAmount = if (amountMinor != null && currencyCode != null) {
        Money(amountMinor, currencyCode)
    } else {
        rule.amount
    }
    return RecurringOccurrence(
        id = id,
        ruleId = ruleId,
        ruleTitle = rule.title,
        amount = occAmount,
        type = rule.type,
        accountId = rule.accountId,
        destinationAccountId = rule.destinationAccountId,
        categoryId = rule.categoryId,
        dueDate = dueDate,
        status = try { OccurrenceStatus.valueOf(status) } catch (_: Exception) { OccurrenceStatus.EXPECTED },
        paidDate = paidDate,
        transactionId = transactionId,
        isVariableAmount = rule.isVariableAmount,
        isSubscription = rule.isSubscription,
        notes = notes ?: rule.notes
    )
}

fun RecurringOccurrence.toEntity(): RecurringOccurrenceEntity {
    return RecurringOccurrenceEntity(
        id = id,
        ruleId = ruleId,
        dueDate = dueDate,
        status = status.name,
        amountMinor = amount.amountMinor,
        currencyCode = amount.currencyCode,
        paidDate = paidDate,
        transactionId = transactionId,
        notes = notes
    )
}

// Goal Mappers
fun FinancialGoalEntity.toDomain(): FinancialGoal {
    return FinancialGoal(
        id = id,
        title = title,
        targetAmount = Money(targetAmountMinor, currencyCode),
        currentAmount = Money(currentAmountMinor, currencyCode),
        targetDate = targetDate,
        linkedAccountId = linkedAccountId,
        icon = icon,
        colorHex = colorHex,
        isCompleted = isCompleted,
        createdAt = createdAt
    )
}

fun FinancialGoal.toEntity(): FinancialGoalEntity {
    return FinancialGoalEntity(
        id = id,
        title = title,
        targetAmountMinor = targetAmount.amountMinor,
        currentAmountMinor = currentAmount.amountMinor,
        currencyCode = targetAmount.currencyCode,
        targetDate = targetDate,
        linkedAccountId = linkedAccountId,
        icon = icon,
        colorHex = colorHex,
        isCompleted = isCompleted,
        createdAt = createdAt
    )
}

// Asset Mappers
fun AssetEntity.toDomain(): InvestmentAsset {
    return InvestmentAsset(
        id = id,
        name = name,
        symbol = symbol,
        assetClass = try { AssetClass.valueOf(assetClass) } catch (_: Exception) { AssetClass.OTHER },
        quantity = quantity,
        purchasePrice = Money(purchasePriceMinor, currencyCode),
        currentPrice = Money(currentPriceMinor, currencyCode),
        lastUpdated = lastUpdated,
        notes = notes
    )
}

fun InvestmentAsset.toEntity(): AssetEntity {
    return AssetEntity(
        id = id,
        name = name,
        symbol = symbol,
        assetClass = assetClass.name,
        quantity = quantity,
        purchasePriceMinor = purchasePrice.amountMinor,
        currentPriceMinor = currentPrice.amountMinor,
        currencyCode = purchasePrice.currencyCode,
        lastUpdated = lastUpdated,
        notes = notes
    )
}

// Debt Mappers
fun DebtEntity.toDomain(): Debt {
    return Debt(
        id = id,
        name = name,
        type = try { DebtType.valueOf(type) } catch (_: Exception) { DebtType.OTHER },
        totalPrincipal = Money(totalPrincipalMinor, currencyCode),
        remainingBalance = Money(remainingBalanceMinor, currencyCode),
        interestRatePercent = interestRatePercent,
        minimumPayment = Money(minimumPaymentMinor, currencyCode),
        nextPaymentDate = nextPaymentDate,
        linkedAccountId = linkedAccountId,
        notes = notes
    )
}

fun Debt.toEntity(): DebtEntity {
    return DebtEntity(
        id = id,
        name = name,
        type = type.name,
        totalPrincipalMinor = totalPrincipal.amountMinor,
        remainingBalanceMinor = remainingBalance.amountMinor,
        currencyCode = remainingBalance.currencyCode,
        interestRatePercent = interestRatePercent,
        minimumPaymentMinor = minimumPayment.amountMinor,
        nextPaymentDate = nextPaymentDate,
        linkedAccountId = linkedAccountId,
        notes = notes
    )
}
