package md.alexlab.finpulse.domain.usecase.transaction

import md.alexlab.finpulse.core.model.CurrencyConfig
import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.data.repository.ExchangeRateProviderImpl
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.ExchangeRateProvider
import md.alexlab.finpulse.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.util.UUID

sealed class CreateTransactionResult {
    data class Success(val transaction: Transaction) : CreateTransactionResult()
    data class Error(val message: String) : CreateTransactionResult()
}

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val rateProvider: ExchangeRateProvider? = null
) {
    suspend operator fun invoke(
        id: String? = null,
        amountMinor: Long,
        currencyCode: String,
        type: TransactionType,
        sourceAccountId: String,
        destinationAccountId: String? = null,
        categoryId: String,
        merchant: String? = null,
        description: String = "",
        tags: List<String> = emptyList(),
        notes: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        isCategoryConfirmed: Boolean = true,
        categorizationConfidence: Float = 1.0f,
        matchedRuleId: String? = null,
        // Multi-currency transfer options:
        exchangeRate: Double? = null,
        destinationAmountMinor: Long? = null
    ): CreateTransactionResult {
        // 1. Validation: Amount must be strictly positive
        if (amountMinor <= 0L) {
            return CreateTransactionResult.Error("Amount must be greater than zero")
        }

        // 2. Validation: Source account must exist and must not be archived
        val sourceAccount = accountRepository.getAccountById(sourceAccountId)
            ?: return CreateTransactionResult.Error("Source account not found")

        if (sourceAccount.isArchived) {
            return CreateTransactionResult.Error("Cannot record transactions on an archived account")
        }

        // 3. Validation: Currency match with source account
        if (sourceAccount.balance.currencyCode != currencyCode) {
            return CreateTransactionResult.Error(
                "Currency mismatch: Account uses ${sourceAccount.balance.currencyCode}, but amount specified $currencyCode"
            )
        }

        // 4. Validation: Category must not be blank
        if (categoryId.isBlank()) {
            return CreateTransactionResult.Error("Category must be specified")
        }

        // 5. Validation: Transfer specific rules
        var finalDestAmount: Money? = null
        var finalRate: Double? = exchangeRate

        if (type == TransactionType.TRANSFER) {
            if (destinationAccountId.isNullOrBlank()) {
                return CreateTransactionResult.Error("Destination account is required for transfers")
            }
            if (destinationAccountId == sourceAccountId) {
                return CreateTransactionResult.Error("Source and destination accounts cannot be identical")
            }
            val destinationAccount = accountRepository.getAccountById(destinationAccountId)
                ?: return CreateTransactionResult.Error("Destination account not found")

            if (destinationAccount.isArchived) {
                return CreateTransactionResult.Error("Cannot transfer to an archived account")
            }

            val destCurrency = destinationAccount.balance.currencyCode

            // Cross-currency transfer calculation
            if (destCurrency != currencyCode) {
                if (destinationAmountMinor != null && destinationAmountMinor > 0L) {
                    finalDestAmount = Money(destinationAmountMinor, destCurrency)
                    // Compute effective conversion rate
                    val srcMajor = CurrencyConfig.toMajor(amountMinor, currencyCode)
                    val dstMajor = CurrencyConfig.toMajor(destinationAmountMinor, destCurrency)
                    if (srcMajor.compareTo(BigDecimal.ZERO) != 0) {
                        finalRate = dstMajor.divide(srcMajor, 6, java.math.RoundingMode.HALF_EVEN).toDouble()
                    }
                } else {
                    // Compute destination amount using provided rate or provider rate
                    val effectiveRate = finalRate
                        ?: rateProvider?.getRate(currencyCode, destCurrency)?.rate
                        ?: ExchangeRateProviderImpl.computeFallbackRate(currencyCode, destCurrency)
                    finalRate = effectiveRate

                    val srcMajor = CurrencyConfig.toMajor(amountMinor, currencyCode)
                    val dstMajor = srcMajor.multiply(BigDecimal.valueOf(effectiveRate))
                    val minor = CurrencyConfig.toMinor(dstMajor, destCurrency)
                    finalDestAmount = Money(minor, destCurrency)
                }
            } else {
                finalDestAmount = Money(amountMinor, currencyCode)
                finalRate = 1.0
            }
        }

        val transaction = Transaction(
            id = id ?: UUID.randomUUID().toString(),
            amount = Money(amountMinor, currencyCode),
            type = type,
            sourceAccountId = sourceAccountId,
            destinationAccountId = if (type == TransactionType.TRANSFER) destinationAccountId else null,
            categoryId = categoryId,
            merchant = merchant?.trim()?.takeIf { it.isNotEmpty() },
            description = description.trim(),
            tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            timestamp = timestamp,
            isCategoryConfirmed = isCategoryConfirmed,
            categorizationConfidence = categorizationConfidence,
            matchedRuleId = matchedRuleId,
            exchangeRate = finalRate,
            exchangeRateDate = if (finalRate != null) timestamp else null,
            destinationAmount = finalDestAmount
        )

        if (id != null) {
            transactionRepository.updateTransaction(transaction)
        } else {
            transactionRepository.createTransaction(transaction)
        }

        return CreateTransactionResult.Success(transaction)
    }
}
