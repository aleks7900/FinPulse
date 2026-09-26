package com.finpulse.app.domain.usecase.csv

import com.finpulse.app.domain.engine.CsvParserEngine
import com.finpulse.app.domain.engine.DuplicateDetectorEngine
import com.finpulse.app.domain.model.CategorizationCandidate
import com.finpulse.app.domain.model.CsvColumnMapping
import com.finpulse.app.domain.model.CsvFormatConfig
import com.finpulse.app.domain.model.ParsedCsvRow
import com.finpulse.app.domain.repository.TransactionRepository
import com.finpulse.app.domain.usecase.categorization.CategorizeTransactionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ParseCsvStatementUseCase(
    private val transactionRepository: TransactionRepository,
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase
) {

    suspend operator fun invoke(
        csvText: String,
        config: CsvFormatConfig,
        mapping: CsvColumnMapping,
        destinationAccountId: String
    ): List<ParsedCsvRow> = withContext(Dispatchers.Default) {
        // 1. Parse raw rows into domain ParsedCsvRows
        val parsedRows = CsvParserEngine.parseRows(
            csvText = csvText,
            config = config,
            mapping = mapping
        )

        if (parsedRows.isEmpty()) return@withContext emptyList()

        // 2. Fetch existing transactions to evaluate duplicates
        val existingTransactions = try {
            transactionRepository.getAllTransactionsFlow().first()
        } catch (_: Exception) {
            emptyList()
        }

        // 3. Evaluate duplicate candidates deterministically
        val rowsWithDuplicates = DuplicateDetectorEngine.evaluateDuplicates(
            rows = parsedRows,
            existingTransactions = existingTransactions,
            destinationAccountId = destinationAccountId
        )

        // 4. Enrich valid rows with category suggestions
        rowsWithDuplicates.map { row ->
            if (!row.isValid || row.parsedAmountMinor == null || row.parsedType == null) {
                row
            } else {
                val candidate = CategorizationCandidate(
                    merchant = row.parsedMerchant,
                    description = row.parsedDescription,
                    amountMinor = row.parsedAmountMinor,
                    sourceAccountId = destinationAccountId,
                    type = row.parsedType
                )
                val catResult = categorizeTransactionUseCase(candidate)

                if (row.predictedCategoryId.isNullOrBlank()) {
                    row.copy(
                        predictedCategoryId = catResult.categoryId,
                        categorizationConfidence = catResult.confidenceScore,
                        matchedRuleId = catResult.matchedRuleId
                    )
                } else {
                    row.copy(
                        categorizationConfidence = 1.0f
                    )
                }
            }
        }
    }
}
