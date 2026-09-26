package com.finpulse.app.domain.usecase.csv

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.DuplicateStatus
import com.finpulse.app.domain.model.ImportSummary
import com.finpulse.app.domain.model.ParsedCsvRow
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.domain.repository.AccountRepository
import com.finpulse.app.domain.repository.CategoryRepository
import com.finpulse.app.domain.repository.MerchantSignalRepository
import com.finpulse.app.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ExecuteCsvImportUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val merchantSignalRepository: MerchantSignalRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend operator fun invoke(
        destinationAccountId: String,
        allRows: List<ParsedCsvRow>,
        fallbackCategoryId: String = "cat_other"
    ): ImportSummary = withContext(Dispatchers.IO) {
        val account = accountRepository.getAccountById(destinationAccountId)
        val currency = account?.balance?.currencyCode ?: "USD"
        val accountName = account?.name ?: "Account"

        val rowsToImport = allRows.filter { it.isValid && !it.isExcluded }
        val excludedRows = allRows.filter { it.isValid && it.isExcluded }
        val invalidRows = allRows.filter { !it.isValid }

        val duplicateSkipped = excludedRows.count { it.duplicateStatus != DuplicateStatus.NEW }
        val userExcludedOnly = excludedRows.count { it.duplicateStatus == DuplicateStatus.NEW }

        val createdIds = mutableListOf<String>()
        var totalIncomeMinor = 0L
        var totalExpenseMinor = 0L

        for (row in rowsToImport) {
            val amountMinor = row.parsedAmountMinor ?: continue
            val type = row.parsedType ?: TransactionType.EXPENSE
            val txId = UUID.randomUUID().toString()

            val categoryId = if (!row.predictedCategoryId.isNullOrBlank()) {
                val cat = categoryRepository.getCategoryById(row.predictedCategoryId)
                cat?.id ?: fallbackCategoryId
            } else {
                fallbackCategoryId
            }

            val isConfirmed = row.categorizationConfidence >= 0.8f

            val transaction = Transaction(
                id = txId,
                amount = Money(amountMinor = amountMinor, currencyCode = currency),
                type = type,
                sourceAccountId = destinationAccountId,
                destinationAccountId = null,
                categoryId = categoryId,
                merchant = row.parsedMerchant,
                timestamp = row.parsedDate ?: System.currentTimeMillis(),
                description = row.parsedDescription.ifBlank { row.parsedMerchant.orEmpty() },
                isCategoryConfirmed = isConfirmed,
                categorizationConfidence = row.categorizationConfidence,
                matchedRuleId = row.matchedRuleId
            )

            transactionRepository.createTransaction(transaction)
            createdIds.add(txId)

            if (type == TransactionType.INCOME) {
                totalIncomeMinor += amountMinor
            } else if (type == TransactionType.EXPENSE) {
                totalExpenseMinor += amountMinor
            }

            // Record merchant signal if merchant is present and confident
            if (!row.parsedMerchant.isNullOrBlank() && isConfirmed) {
                merchantSignalRepository.recordSignal(row.parsedMerchant, categoryId)
            }
        }

        ImportSummary(
            totalRows = allRows.size,
            importedCount = createdIds.size,
            skippedDuplicateCount = duplicateSkipped,
            excludedCount = userExcludedOnly,
            invalidCount = invalidRows.size,
            importedTransactionIds = createdIds,
            destinationAccountId = destinationAccountId,
            destinationAccountName = accountName,
            totalIncomeMinor = totalIncomeMinor,
            totalExpenseMinor = totalExpenseMinor
        )
    }
}
