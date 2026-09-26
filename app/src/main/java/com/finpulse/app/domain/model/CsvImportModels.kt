package com.finpulse.app.domain.model

import kotlinx.serialization.Serializable

enum class AmountMode {
    SINGLE_AMOUNT,
    SEPARATE_DEBIT_CREDIT
}

@Serializable
data class CsvFormatConfig(
    val delimiter: Char = ',',
    val dateFormat: String = "yyyy-MM-dd",
    val decimalSeparator: Char = '.',
    val hasHeader: Boolean = true,
    val reversedDebitCredit: Boolean = false,
    val encoding: String = "UTF-8",
    val amountMode: AmountMode = AmountMode.SINGLE_AMOUNT
)

@Serializable
data class CsvColumnMapping(
    val dateColumnIndex: Int = -1,
    val amountColumnIndex: Int = -1,
    val debitColumnIndex: Int = -1,
    val creditColumnIndex: Int = -1,
    val descriptionColumnIndex: Int = -1,
    val merchantColumnIndex: Int = -1,
    val currencyColumnIndex: Int = -1,
    val balanceColumnIndex: Int = -1,
    val categoryColumnIndex: Int = -1
) {
    fun isValid(amountMode: AmountMode): Boolean {
        val hasDate = dateColumnIndex >= 0
        val hasAmount = when (amountMode) {
            AmountMode.SINGLE_AMOUNT -> amountColumnIndex >= 0
            AmountMode.SEPARATE_DEBIT_CREDIT -> debitColumnIndex >= 0 || creditColumnIndex >= 0
        }
        val hasDescriptionOrMerchant = descriptionColumnIndex >= 0 || merchantColumnIndex >= 0
        return hasDate && hasAmount && hasDescriptionOrMerchant
    }
}

@Serializable
data class ImportProfile(
    val id: String,
    val name: String,
    val institution: String? = null,
    val formatConfig: CsvFormatConfig,
    val columnMapping: CsvColumnMapping,
    val defaultAccountId: String? = null,
    val isSystemPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class DuplicateStatus {
    NEW,
    EXACT_DUPLICATE,
    POTENTIAL_DUPLICATE
}

@Serializable
data class ParsedCsvRow(
    val rowIndex: Int,
    val rawValues: List<String>,
    val rawLine: String = "",
    val isValid: Boolean,
    val errorReason: String? = null,
    val parsedDate: Long? = null, // Epoch millis
    val parsedAmountMinor: Long? = null,
    val parsedType: TransactionType? = null,
    val parsedDescription: String = "",
    val parsedMerchant: String? = null,
    val parsedCurrency: String? = null,
    val fingerprint: String? = null,
    val duplicateStatus: DuplicateStatus = DuplicateStatus.NEW,
    val duplicateTransactionId: String? = null,
    val duplicateTransactionDescription: String? = null,
    val duplicateTransactionAmountMinor: Long? = null,
    val duplicateTransactionTimestamp: Long? = null,
    val isExcluded: Boolean = false,
    val predictedCategoryId: String? = null,
    val categorizationConfidence: Float = 0.0f,
    val matchedRuleId: String? = null
)

@Serializable
data class ImportSummary(
    val totalRows: Int,
    val importedCount: Int,
    val skippedDuplicateCount: Int,
    val excludedCount: Int,
    val invalidCount: Int,
    val importedTransactionIds: List<String>,
    val destinationAccountId: String,
    val destinationAccountName: String,
    val totalIncomeMinor: Long = 0L,
    val totalExpenseMinor: Long = 0L,
    val importedAt: Long = System.currentTimeMillis()
)
