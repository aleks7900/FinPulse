package md.alexlab.finpulse.domain.usecase.csv

import md.alexlab.finpulse.domain.engine.CsvParserEngine
import md.alexlab.finpulse.domain.engine.DuplicateDetectorEngine
import md.alexlab.finpulse.domain.model.CategorizationCandidate
import md.alexlab.finpulse.domain.model.CsvColumnMapping
import md.alexlab.finpulse.domain.model.CsvFormatConfig
import md.alexlab.finpulse.domain.model.ParsedCsvRow
import md.alexlab.finpulse.domain.repository.TransactionRepository
import md.alexlab.finpulse.domain.usecase.categorization.CategorizeTransactionUseCase
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
