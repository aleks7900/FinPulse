package md.alexlab.finpulse.domain.usecase.backup

import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.backup.ExportFilterParams
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.BackupRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class ExportedTransactionItem(
    val id: String,
    val date: String,
    val time: String,
    val timestamp: Long,
    val type: String,
    val amount: String,
    val amountMinor: Long,
    val currency: String,
    val accountName: String,
    val categoryName: String,
    val merchant: String?,
    val description: String,
    val notes: String?,
    val tags: List<String>,
    val isConfirmed: Boolean
)

@Serializable
data class TransactionsExportContainer(
    val exportedAt: Long,
    val count: Int,
    val transactions: List<ExportedTransactionItem>
)

class ExportTransactionsUseCase(
    private val backupRepository: BackupRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportCsv(params: ExportFilterParams): String {
        val transactions = backupRepository.getFilteredTransactions(params)
        val accountsMap = accountRepository.getAllAccountsFlow().first().associateBy { it.id }
        val categoriesMap = categoryRepository.getAllCategoriesFlow().first().associateBy { it.id }

        val sb = StringBuilder()
        sb.append("ID,Date,Time,Type,Amount,Currency,Account,Category,Merchant,Description,Notes,Tags,Status\n")

        for (tx in transactions) {
            val instant = Instant.ofEpochMilli(tx.timestamp)
            val dateStr = dateFormatter.format(instant)
            val timeStr = timeFormatter.format(instant)
            val accName = accountsMap[tx.sourceAccountId]?.name ?: tx.sourceAccountId
            val catName = categoriesMap[tx.categoryId]?.name ?: tx.categoryId
            val merchant = tx.merchant ?: ""
            val notes = tx.notes ?: ""
            val tags = tx.tags.joinToString(";")
            val status = if (tx.isCategoryConfirmed) "Confirmed" else "Needs Review"

            sb.append(escapeCsv(tx.id)).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(timeStr)).append(",")
            sb.append(escapeCsv(tx.type.name)).append(",")
            sb.append(tx.amount.amountBigDecimal.toPlainString()).append(",")
            sb.append(escapeCsv(tx.amount.currencyCode)).append(",")
            sb.append(escapeCsv(accName)).append(",")
            sb.append(escapeCsv(catName)).append(",")
            sb.append(escapeCsv(merchant)).append(",")
            sb.append(escapeCsv(tx.description)).append(",")
            sb.append(escapeCsv(notes)).append(",")
            sb.append(escapeCsv(tags)).append(",")
            sb.append(escapeCsv(status)).append("\n")
        }

        return sb.toString()
    }

    suspend fun exportJson(params: ExportFilterParams): String {
        val transactions = backupRepository.getFilteredTransactions(params)
        val accountsMap = accountRepository.getAllAccountsFlow().first().associateBy { it.id }
        val categoriesMap = categoryRepository.getAllCategoriesFlow().first().associateBy { it.id }

        val items = transactions.map { tx ->
            val instant = Instant.ofEpochMilli(tx.timestamp)
            ExportedTransactionItem(
                id = tx.id,
                date = dateFormatter.format(instant),
                time = timeFormatter.format(instant),
                timestamp = tx.timestamp,
                type = tx.type.name,
                amount = tx.amount.amountBigDecimal.toPlainString(),
                amountMinor = tx.amount.amountMinor,
                currency = tx.amount.currencyCode,
                accountName = accountsMap[tx.sourceAccountId]?.name ?: tx.sourceAccountId,
                categoryName = categoriesMap[tx.categoryId]?.name ?: tx.categoryId,
                merchant = tx.merchant,
                description = tx.description,
                notes = tx.notes,
                tags = tx.tags,
                isConfirmed = tx.isCategoryConfirmed
            )
        }

        val container = TransactionsExportContainer(
            exportedAt = System.currentTimeMillis(),
            count = items.size,
            transactions = items
        )

        return json.encodeToString(container)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
