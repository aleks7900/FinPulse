package com.finpulse.app.domain.engine

import com.finpulse.app.domain.model.AmountMode
import com.finpulse.app.domain.model.CsvColumnMapping
import com.finpulse.app.domain.model.CsvFormatConfig
import java.io.BufferedReader
import java.io.StringReader

object CsvDetectorEngine {

    private val DateHeaders = setOf(
        "date", "trans date", "transaction date", "post date", "posting date", "booking date",
        "value date", "datum", "buchungsdatum", "wertstellung", "date operation", "fecha"
    )

    private val AmountHeaders = setOf(
        "amount", "betrag", "montant", "importe", "total", "sum", "net amount", "transaction amount"
    )

    private val DebitHeaders = setOf(
        "debit", "withdrawal", "withdrawals", "spent", "outflow", "paid out", "debit amount",
        "soll", "ausgaben", "débit", "cargo"
    )

    private val CreditHeaders = setOf(
        "credit", "deposit", "deposits", "inflow", "paid in", "credit amount",
        "haben", "einnahmen", "crédit", "abono"
    )

    private val DescriptionHeaders = setOf(
        "description", "details", "memo", "narrative", "text", "verwendungszweck",
        "reference", "transaction details", "remittance info", "libellé", "concepto"
    )

    private val MerchantHeaders = setOf(
        "merchant", "payee", "name", "beneficiary", "empfänger", "auftraggeber",
        "party", "counterparty", "comercio", "destinataire"
    )

    private val CurrencyHeaders = setOf(
        "currency", "curr", "ccy", "währung", "devise", "moneda"
    )

    private val BalanceHeaders = setOf(
        "balance", "saldo", "running balance", "kontostand", "solde"
    )

    private val CategoryHeaders = setOf(
        "category", "kategorie", "catégorie", "categoría", "tag", "classification"
    )

    /**
     * Inspects raw CSV text and suggests optimal CsvFormatConfig and CsvColumnMapping.
     */
    fun detectConfigAndMapping(csvText: String): Pair<CsvFormatConfig, CsvColumnMapping> {
        val clean = if (csvText.startsWith("\uFEFF")) csvText.substring(1) else csvText
        val reader = BufferedReader(StringReader(clean))
        val sampleLines = mutableListOf<String>()

        var line = reader.readLine()
        while (line != null && sampleLines.size < 20) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                sampleLines.add(trimmed)
            }
            line = reader.readLine()
        }

        if (sampleLines.isEmpty()) {
            return Pair(CsvFormatConfig(), CsvColumnMapping())
        }

        // 1. Detect Delimiter
        val delimiter = detectDelimiter(sampleLines)

        // 2. Parse first few records with detected delimiter
        val sampleRecords = sampleLines.map { CsvParserEngine.parseRawRecords(it, delimiter, false).firstOrNull() ?: emptyList() }
            .filter { it.isNotEmpty() }

        if (sampleRecords.isEmpty()) {
            return Pair(CsvFormatConfig(delimiter = delimiter), CsvColumnMapping())
        }

        val firstRecord = sampleRecords.first()

        // 3. Detect Headers & Column Mapping
        val (hasHeader, mapping, amountMode) = detectHeadersAndMapping(sampleRecords)

        // 4. Detect Date Format from sample data rows
        val dataRows = if (hasHeader && sampleRecords.size > 1) sampleRecords.drop(1) else sampleRecords
        val dateFormat = detectDateFormat(dataRows, mapping.dateColumnIndex)

        // 5. Detect Decimal Separator
        val decimalSeparator = detectDecimalSeparator(dataRows, mapping, amountMode)

        val config = CsvFormatConfig(
            delimiter = delimiter,
            dateFormat = dateFormat,
            decimalSeparator = decimalSeparator,
            hasHeader = hasHeader,
            amountMode = amountMode,
            reversedDebitCredit = false
        )

        return Pair(config, mapping)
    }

    private fun detectDelimiter(lines: List<String>): Char {
        val candidates = listOf(',', ';', '\t', '|')
        var bestDelimiter = ','
        var maxConsistency = -1.0

        for (candidate in candidates) {
            val counts = lines.map { countOccurrencesOutsideQuotes(it, candidate) }
            val nonZero = counts.filter { it > 0 }
            if (nonZero.isNotEmpty()) {
                val average = nonZero.average()
                // Consistency is high when count variation between lines is low
                val variance = nonZero.map { Math.abs(it - average) }.average()
                val score = average / (variance + 1.0)
                if (score > maxConsistency) {
                    maxConsistency = score
                    bestDelimiter = candidate
                }
            }
        }

        return bestDelimiter
    }

    private fun countOccurrencesOutsideQuotes(line: String, target: Char): Int {
        var count = 0
        var insideQuote = false
        for (c in line) {
            if (c == '"') {
                insideQuote = !insideQuote
            } else if (c == target && !insideQuote) {
                count++
            }
        }
        return count
    }

    private fun detectHeadersAndMapping(records: List<List<String>>): Triple<Boolean, CsvColumnMapping, AmountMode> {
        val headerRow = records.first()

        var dateIdx = -1
        var amountIdx = -1
        var debitIdx = -1
        var creditIdx = -1
        var descIdx = -1
        var merchIdx = -1
        var currIdx = -1
        var balIdx = -1
        var catIdx = -1

        var matchCount = 0

        headerRow.forEachIndexed { index, rawHeader ->
            val normalized = rawHeader.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim()

            when {
                DateHeaders.any { normalized.contains(it) } && dateIdx == -1 -> {
                    dateIdx = index
                    matchCount++
                }
                DebitHeaders.any { normalized.contains(it) } && debitIdx == -1 -> {
                    debitIdx = index
                    matchCount++
                }
                CreditHeaders.any { normalized.contains(it) } && creditIdx == -1 -> {
                    creditIdx = index
                    matchCount++
                }
                AmountHeaders.any { normalized.contains(it) } && amountIdx == -1 -> {
                    amountIdx = index
                    matchCount++
                }
                MerchantHeaders.any { normalized.contains(it) } && merchIdx == -1 -> {
                    merchIdx = index
                    matchCount++
                }
                DescriptionHeaders.any { normalized.contains(it) } && descIdx == -1 -> {
                    descIdx = index
                    matchCount++
                }
                CurrencyHeaders.any { normalized.contains(it) } && currIdx == -1 -> {
                    currIdx = index
                    matchCount++
                }
                BalanceHeaders.any { normalized.contains(it) } && balIdx == -1 -> {
                    balIdx = index
                    matchCount++
                }
                CategoryHeaders.any { normalized.contains(it) } && catIdx == -1 -> {
                    catIdx = index
                    matchCount++
                }
            }
        }

        val hasHeader = matchCount >= 2

        val amountMode = if (debitIdx != -1 && creditIdx != -1) {
            AmountMode.SEPARATE_DEBIT_CREDIT
        } else {
            AmountMode.SINGLE_AMOUNT
        }

        // Fallback positional heuristics if not found by headers
        if (!hasHeader || dateIdx == -1) {
            // First column often date
            dateIdx = 0
        }
        if (!hasHeader || (amountIdx == -1 && debitIdx == -1 && creditIdx == -1)) {
            // Amount often 2nd or last column
            amountIdx = if (headerRow.size > 2) headerRow.size - 1 else 1
        }
        if (!hasHeader || (descIdx == -1 && merchIdx == -1)) {
            // Description often 2nd column
            descIdx = if (headerRow.size > 1 && dateIdx == 0) 1 else 0
        }

        val mapping = CsvColumnMapping(
            dateColumnIndex = dateIdx,
            amountColumnIndex = amountIdx,
            debitColumnIndex = debitIdx,
            creditColumnIndex = creditIdx,
            descriptionColumnIndex = descIdx,
            merchantColumnIndex = merchIdx,
            currencyColumnIndex = currIdx,
            balanceColumnIndex = balIdx,
            categoryColumnIndex = catIdx
        )

        return Triple(hasHeader, mapping, amountMode)
    }

    private fun detectDateFormat(dataRows: List<List<String>>, dateIdx: Int): String {
        if (dateIdx < 0) return "yyyy-MM-dd"

        for (row in dataRows) {
            val sample = row.getOrNull(dateIdx)?.trim() ?: continue
            if (sample.isEmpty()) continue

            when {
                sample.matches(Regex("""\d{4}-\d{2}-\d{2}""")) -> return "yyyy-MM-dd"
                sample.matches(Regex("""\d{4}/\d{2}/\d{2}""")) -> return "yyyy/MM/dd"
                sample.matches(Regex("""\d{2}\.\d{2}\.\d{4}""")) -> return "dd.MM.yyyy"
                sample.matches(Regex("""\d{1,2}/\d{1,2}/\d{4}""")) -> {
                    // Check if first digit > 12 to distinguish day/month
                    val parts = sample.split("/")
                    val first = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    return if (first > 12) "dd/MM/yyyy" else "MM/dd/yyyy"
                }
                sample.matches(Regex("""\d{1,2}/\d{1,2}/\d{2}""")) -> {
                    val parts = sample.split("/")
                    val first = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    return if (first > 12) "dd/MM/yy" else "MM/dd/yy"
                }
                sample.matches(Regex("""\d{2}-[A-Za-z]{3}-\d{4}""")) -> return "dd-MMM-yyyy"
                sample.matches(Regex("""\d{8}""")) -> return "yyyyMMdd"
            }
        }

        return "yyyy-MM-dd"
    }

    private fun detectDecimalSeparator(
        dataRows: List<List<String>>,
        mapping: CsvColumnMapping,
        amountMode: AmountMode
    ): Char {
        val targetIdx = when (amountMode) {
            AmountMode.SINGLE_AMOUNT -> mapping.amountColumnIndex
            AmountMode.SEPARATE_DEBIT_CREDIT -> if (mapping.debitColumnIndex >= 0) mapping.debitColumnIndex else mapping.creditColumnIndex
        }

        if (targetIdx < 0) return '.'

        var dotCount = 0
        var commaCount = 0

        for (row in dataRows) {
            val sample = row.getOrNull(targetIdx)?.trim() ?: continue
            // Check decimal separator: e.g. 12,34 or 12.34
            if (sample.matches(Regex(""".*,\d{2}$"""))) {
                commaCount++
            } else if (sample.matches(Regex(""".*\.\d{2}$"""))) {
                dotCount++
            }
        }

        return if (commaCount > dotCount) ',' else '.'
    }
}
