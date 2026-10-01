package md.alexlab.finpulse.domain.engine

import md.alexlab.finpulse.domain.model.DuplicateStatus
import md.alexlab.finpulse.domain.model.ParsedCsvRow
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

object DuplicateDetectorEngine {

    private const val DAY_MILLIS = 86_400_000L

    /**
     * Generates a deterministic SHA-256 fingerprint for a transaction.
     */
    fun generateFingerprint(
        accountId: String,
        amountMinor: Long,
        type: TransactionType,
        timestamp: Long,
        description: String,
        merchant: String?
    ): String {
        val zone = java.time.ZoneId.systemDefault()
        val dateStr = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().toString()
        val textToNormalize = if (!merchant.isNullOrBlank()) merchant else description
        val normalizedText = normalizeForMatching(textToNormalize)

        val rawSignature = "$accountId|$amountMinor|${type.name}|$dateStr|$normalizedText"
        return sha256(rawSignature)
    }

    /**
     * Evaluates parsed CSV rows against existing transactions and intra-batch duplicates.
     * Never silently discards potential duplicates; annotates them with candidate details.
     */
    fun evaluateDuplicates(
        rows: List<ParsedCsvRow>,
        existingTransactions: List<Transaction>,
        destinationAccountId: String,
        windowDays: Long = 3L
    ): List<ParsedCsvRow> {
        val seenFingerprintsInBatch = mutableMapOf<String, Int>() // fingerprint -> first seen rowIndex
        val windowMillis = windowDays * DAY_MILLIS
        val zone = java.time.ZoneId.systemDefault()

        // Filter existing transactions relevant to this account
        val relevantExisting = existingTransactions.filter {
            it.sourceAccountId == destinationAccountId || it.destinationAccountId == destinationAccountId
        }

        return rows.map { row ->
            if (!row.isValid || row.parsedDate == null || row.parsedAmountMinor == null || row.parsedType == null) {
                return@map row
            }

            val fingerprint = generateFingerprint(
                accountId = destinationAccountId,
                amountMinor = row.parsedAmountMinor,
                type = row.parsedType,
                timestamp = row.parsedDate,
                description = row.parsedDescription,
                merchant = row.parsedMerchant
            )

            // 1. Check Intra-batch duplicates (duplicate rows inside the same CSV)
            if (seenFingerprintsInBatch.containsKey(fingerprint)) {
                val previousRowIndex = seenFingerprintsInBatch[fingerprint]!!
                return@map row.copy(
                    fingerprint = fingerprint,
                    duplicateStatus = DuplicateStatus.EXACT_DUPLICATE,
                    duplicateTransactionDescription = "Duplicate of CSV row #$previousRowIndex in this file",
                    duplicateTransactionAmountMinor = row.parsedAmountMinor,
                    duplicateTransactionTimestamp = row.parsedDate,
                    isExcluded = true // default exclude exact duplicates, user can override in UI
                )
            }

            // 2. Check Existing Transactions for Exact Match
            val rowDate = Instant.ofEpochMilli(row.parsedDate).atZone(zone).toLocalDate()
            val rowNormalizedText = normalizeForMatching(row.parsedMerchant ?: row.parsedDescription)

            var exactMatch: Transaction? = null
            var potentialMatch: Transaction? = null

            for (tx in relevantExisting) {
                if (tx.amount.amountMinor != row.parsedAmountMinor || tx.type != row.parsedType) {
                    continue
                }

                val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()
                val txNormalizedText = normalizeForMatching(tx.merchant ?: tx.description)

                val daysDiff = abs(rowDate.toEpochDay() - txDate.toEpochDay())

                if (daysDiff == 0L) {
                    // Same day, same amount, same type
                    val isTextSimilar = isTextMatch(rowNormalizedText, txNormalizedText)
                    if (isTextSimilar) {
                        exactMatch = tx
                        break
                    } else {
                        // Same day, same amount, different text -> potential duplicate
                        if (potentialMatch == null) potentialMatch = tx
                    }
                } else if (daysDiff <= windowDays) {
                    // Within ±windowDays with identical amount
                    val isTextSimilar = isTextMatch(rowNormalizedText, txNormalizedText)
                    if (isTextSimilar && potentialMatch == null) {
                        potentialMatch = tx
                    } else if (daysDiff <= 1L && potentialMatch == null) {
                        // 1 day difference (posting lag) with same amount
                        potentialMatch = tx
                    }
                }
            }

            if (exactMatch != null) {
                seenFingerprintsInBatch[fingerprint] = row.rowIndex
                row.copy(
                    fingerprint = fingerprint,
                    duplicateStatus = DuplicateStatus.EXACT_DUPLICATE,
                    duplicateTransactionId = exactMatch.id,
                    duplicateTransactionDescription = exactMatch.description.ifEmpty { exactMatch.merchant.orEmpty() },
                    duplicateTransactionAmountMinor = exactMatch.amount.amountMinor,
                    duplicateTransactionTimestamp = exactMatch.timestamp,
                    isExcluded = true // default exclude exact duplicates, user can review and toggle
                )
            } else if (potentialMatch != null) {
                seenFingerprintsInBatch[fingerprint] = row.rowIndex
                row.copy(
                    fingerprint = fingerprint,
                    duplicateStatus = DuplicateStatus.POTENTIAL_DUPLICATE,
                    duplicateTransactionId = potentialMatch.id,
                    duplicateTransactionDescription = potentialMatch.description.ifEmpty { potentialMatch.merchant.orEmpty() },
                    duplicateTransactionAmountMinor = potentialMatch.amount.amountMinor,
                    duplicateTransactionTimestamp = potentialMatch.timestamp,
                    isExcluded = false // Never silently exclude; surface for review!
                )
            } else {
                seenFingerprintsInBatch[fingerprint] = row.rowIndex
                row.copy(
                    fingerprint = fingerprint,
                    duplicateStatus = DuplicateStatus.NEW,
                    isExcluded = false
                )
            }
        }
    }

    private fun isTextMatch(textA: String, textB: String): Boolean {
        if (textA.isEmpty() || textB.isEmpty()) return true
        if (textA == textB) return true
        if (textA.contains(textB) || textB.contains(textA)) return true

        // Check token intersection
        val tokensA = textA.split(" ").filter { it.length > 2 }
        val tokensB = textB.split(" ").filter { it.length > 2 }
        if (tokensA.isNotEmpty() && tokensB.isNotEmpty()) {
            val common = tokensA.intersect(tokensB.toSet())
            if (common.isNotEmpty()) return true
        }

        return false
    }

    fun normalizeForMatching(text: String): String {
        return text.lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
