package com.finpulse.app.domain.engine

import com.finpulse.app.domain.model.AmountMode
import com.finpulse.app.domain.model.CsvColumnMapping
import com.finpulse.app.domain.model.CsvFormatConfig
import com.finpulse.app.domain.model.ParsedCsvRow
import com.finpulse.app.domain.model.TransactionType
import java.io.BufferedReader
import java.io.StringReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

object CsvParserEngine {

    private val FallbackDateFormatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,                           // 2024-03-15
        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US),       // 03/15/2024
        DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US),         // 3/5/2024
        DateTimeFormatter.ofPattern("M/d/yy", Locale.US),           // 3/5/24
        DateTimeFormatter.ofPattern("MM/dd/yy", Locale.US),         // 03/15/24
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US),       // 15/03/2024
        DateTimeFormatter.ofPattern("d/M/yyyy", Locale.US),         // 5/3/2024
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY),  // 15.03.2024
        DateTimeFormatter.ofPattern("d.M.yyyy", Locale.GERMANY),    // 5.3.2024
        DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.US),       // 2024/03/15
        DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.US),       // 2024.03.15
        DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US),       // 15-03-2024
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.US),      // 15-Mar-2024
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US),      // 15 Mar 2024
        DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)          // 20240315
    )

    /**
     * Parses raw CSV content into records according to RFC 4180.
     * Supports multi-line quoted fields, escaped quotes, and custom delimiters.
     */
    fun parseRawRecords(
        csvText: String,
        delimiter: Char = ',',
        hasHeader: Boolean = true
    ): List<List<String>> {
        val cleanText = if (csvText.startsWith("\uFEFF")) csvText.substring(1) else csvText
        val records = mutableListOf<List<String>>()
        val reader = BufferedReader(StringReader(cleanText))

        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuote = false
        var prevChar = ' '

        var line = reader.readLine()
        while (line != null) {
            val len = line.length
            var i = 0

            while (i < len) {
                val c = line[i]

                if (insideQuote) {
                    if (c == '"') {
                        // Check for escaped quote ("")
                        if (i + 1 < len && line[i + 1] == '"') {
                            currentField.append('"')
                            i++ // Skip second quote
                        } else {
                            // Closing quote
                            insideQuote = false
                        }
                    } else {
                        currentField.append(c)
                    }
                } else {
                    when (c) {
                        '"' -> {
                            insideQuote = true
                        }
                        delimiter -> {
                            currentRecord.add(currentField.toString().trim())
                            currentField.clear()
                        }
                        else -> {
                            currentField.append(c)
                        }
                    }
                }
                prevChar = c
                i++
            }

            if (insideQuote) {
                // Multi-line field: append newline and continue to next line
                currentField.append("\n")
            } else {
                // End of row record
                currentRecord.add(currentField.toString().trim())
                currentField.clear()

                // Avoid adding completely empty lines
                if (currentRecord.size > 1 || (currentRecord.size == 1 && currentRecord[0].isNotEmpty())) {
                    records.add(currentRecord.toList())
                }
                currentRecord.clear()
            }

            line = reader.readLine()
        }

        // Flush any trailing field/record if EOF
        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString().trim())
            records.add(currentRecord.toList())
        }

        return records
    }

    /**
     * Parses and maps raw CSV records into domain ParsedCsvRows.
     */
    fun parseRows(
        csvText: String,
        config: CsvFormatConfig,
        mapping: CsvColumnMapping
    ): List<ParsedCsvRow> {
        val rawRecords = parseRawRecords(
            csvText = csvText,
            delimiter = config.delimiter,
            hasHeader = config.hasHeader
        )

        if (rawRecords.isEmpty()) return emptyList()

        val dataRecords = if (config.hasHeader && rawRecords.isNotEmpty()) {
            rawRecords.drop(1)
        } else {
            rawRecords
        }

        val primaryDateFormatter = try {
            DateTimeFormatter.ofPattern(config.dateFormat, Locale.US)
        } catch (_: Exception) {
            DateTimeFormatter.ISO_LOCAL_DATE
        }

        return dataRecords.mapIndexed { index, rowValues ->
            parseSingleRow(
                rowIndex = if (config.hasHeader) index + 2 else index + 1,
                rowValues = rowValues,
                config = config,
                mapping = mapping,
                primaryDateFormatter = primaryDateFormatter
            )
        }
    }

    private fun parseSingleRow(
        rowIndex: Int,
        rowValues: List<String>,
        config: CsvFormatConfig,
        mapping: CsvColumnMapping,
        primaryDateFormatter: DateTimeFormatter
    ): ParsedCsvRow {
        // 1. Check Date
        val dateRaw = rowValues.getOrNull(mapping.dateColumnIndex)?.trim()
        if (dateRaw.isNullOrBlank()) {
            return ParsedCsvRow(
                rowIndex = rowIndex,
                rawValues = rowValues,
                isValid = false,
                errorReason = "Missing date value in column ${mapping.dateColumnIndex + 1}"
            )
        }

        val parsedDate = parseDate(dateRaw, primaryDateFormatter)
        if (parsedDate == null) {
            return ParsedCsvRow(
                rowIndex = rowIndex,
                rawValues = rowValues,
                isValid = false,
                errorReason = "Unable to parse date '$dateRaw' with format '${config.dateFormat}'"
            )
        }

        // 2. Parse Description & Merchant
        val descRaw = rowValues.getOrNull(mapping.descriptionColumnIndex)?.trim().orEmpty()
        val merchRaw = rowValues.getOrNull(mapping.merchantColumnIndex)?.trim()?.takeIf { it.isNotEmpty() }
        val categoryRaw = rowValues.getOrNull(mapping.categoryColumnIndex)?.trim()?.takeIf { it.isNotEmpty() }
        val currencyRaw = rowValues.getOrNull(mapping.currencyColumnIndex)?.trim()?.takeIf { it.isNotEmpty() }

        if (descRaw.isBlank() && merchRaw.isNullOrBlank()) {
            return ParsedCsvRow(
                rowIndex = rowIndex,
                rawValues = rowValues,
                isValid = false,
                errorReason = "Both description and merchant are empty"
            )
        }

        // 3. Parse Amount and Type
        val (amountMinor, type, amountError) = when (config.amountMode) {
            AmountMode.SINGLE_AMOUNT -> {
                val amtRaw = rowValues.getOrNull(mapping.amountColumnIndex)?.trim()
                if (amtRaw.isNullOrBlank()) {
                    Triple(null, null, "Missing amount value in column ${mapping.amountColumnIndex + 1}")
                } else {
                    parseSingleAmount(amtRaw, config.decimalSeparator, config.reversedDebitCredit)
                }
            }
            AmountMode.SEPARATE_DEBIT_CREDIT -> {
                val debitRaw = rowValues.getOrNull(mapping.debitColumnIndex)?.trim()
                val creditRaw = rowValues.getOrNull(mapping.creditColumnIndex)?.trim()
                parseSplitDebitCredit(debitRaw, creditRaw, config.decimalSeparator, config.reversedDebitCredit)
            }
        }

        if (amountError != null || amountMinor == null || type == null) {
            return ParsedCsvRow(
                rowIndex = rowIndex,
                rawValues = rowValues,
                isValid = false,
                errorReason = amountError ?: "Invalid monetary amount",
                parsedDate = parsedDate,
                parsedDescription = descRaw,
                parsedMerchant = merchRaw
            )
        }

        return ParsedCsvRow(
            rowIndex = rowIndex,
            rawValues = rowValues,
            isValid = true,
            parsedDate = parsedDate,
            parsedAmountMinor = amountMinor,
            parsedType = type,
            parsedDescription = descRaw,
            parsedMerchant = merchRaw,
            parsedCurrency = currencyRaw,
            predictedCategoryId = categoryRaw
        )
    }

    /**
     * Parses a single signed amount (e.g. -12.50, (45.00), $1,250.00, 15.00-).
     */
    fun parseSingleAmount(
        raw: String,
        decimalSeparator: Char,
        reversedDebitCredit: Boolean
    ): Triple<Long?, TransactionType?, String?> {
        val clean = raw.trim()
        if (clean.isEmpty()) return Triple(null, null, "Empty amount string")

        var isNegative = false

        // Check for parentheses (e.g. (45.00))
        var processed = clean
        if (processed.startsWith("(") && processed.endsWith(")")) {
            isNegative = true
            processed = processed.substring(1, processed.length - 1)
        }

        // Check for trailing minus (e.g. 45.00-)
        if (processed.endsWith("-")) {
            isNegative = true
            processed = processed.dropLast(1)
        }

        // Check for leading minus or plus
        if (processed.startsWith("-")) {
            isNegative = true
            processed = processed.drop(1)
        } else if (processed.startsWith("+")) {
            processed = processed.drop(1)
        }

        // Check for trailing or leading DR / CR (Debit / Credit)
        if (processed.endsWith("DR", ignoreCase = true) || processed.startsWith("DR", ignoreCase = true)) {
            isNegative = true
            processed = processed.replace("DR", "", ignoreCase = true)
        } else if (processed.endsWith("CR", ignoreCase = true) || processed.startsWith("CR", ignoreCase = true)) {
            isNegative = false
            processed = processed.replace("CR", "", ignoreCase = true)
        }

        // Strip currency symbols and whitespace
        processed = processed.replace(Regex("[^0-9,.]"), "").trim()

        if (processed.isEmpty()) return Triple(null, null, "No numeric digits found in '$raw'")

        // Standardize decimal and thousand separators
        val normalizedNumberString = if (decimalSeparator == ',') {
            // German/European style: 1.234,56 -> remove dots, replace comma with dot
            processed.replace(".", "").replace(",", ".")
        } else {
            // US style: 1,234.56 -> remove commas
            processed.replace(",", "")
        }

        val bigDecimal = try {
            BigDecimal(normalizedNumberString)
        } catch (_: Exception) {
            return Triple(null, null, "Malformed number format: '$raw'")
        }

        val minorUnits = bigDecimal.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN).abs().longValueExact()

        if (minorUnits == 0L) {
            return Triple(null, null, "Zero amount transactions are not allowed")
        }

        // Determine type based on sign and reversed flag
        // Standard convention: Negative = Expense (money out), Positive = Income (money in)
        val isExpense = if (reversedDebitCredit) !isNegative else isNegative
        val type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME

        return Triple(minorUnits, type, null)
    }

    /**
     * Parses separate debit and credit columns.
     */
    fun parseSplitDebitCredit(
        debitRaw: String?,
        creditRaw: String?,
        decimalSeparator: Char,
        reversedDebitCredit: Boolean
    ): Triple<Long?, TransactionType?, String?> {
        val debitClean = debitRaw?.trim().orEmpty()
        val creditClean = creditRaw?.trim().orEmpty()

        val hasDebit = debitClean.isNotEmpty() && debitClean != "0" && debitClean != "0.00" && debitClean != "0,00"
        val hasCredit = creditClean.isNotEmpty() && creditClean != "0" && creditClean != "0.00" && creditClean != "0,00"

        if (!hasDebit && !hasCredit) {
            return Triple(null, null, "Both Debit and Credit fields are empty")
        }

        if (hasDebit && hasCredit) {
            // Both populated - evaluate net difference
            val (dAmount) = parseSingleAmount(debitClean, decimalSeparator, false)
            val (cAmount) = parseSingleAmount(creditClean, decimalSeparator, false)
            if (dAmount == null || cAmount == null) return Triple(null, null, "Invalid debit or credit amount")
            val diff = cAmount - dAmount
            return if (diff > 0) {
                Triple(diff, TransactionType.INCOME, null)
            } else if (diff < 0) {
                Triple(-diff, TransactionType.EXPENSE, null)
            } else {
                Triple(null, null, "Net transaction amount is zero")
            }
        }

        return if (hasDebit) {
            val (amount, _, err) = parseSingleAmount(debitClean, decimalSeparator, false)
            if (err != null) return Triple(null, null, "Debit error: $err")
            val type = if (reversedDebitCredit) TransactionType.INCOME else TransactionType.EXPENSE
            Triple(amount, type, null)
        } else {
            val (amount, _, err) = parseSingleAmount(creditClean, decimalSeparator, false)
            if (err != null) return Triple(null, null, "Credit error: $err")
            val type = if (reversedDebitCredit) TransactionType.EXPENSE else TransactionType.INCOME
            Triple(amount, type, null)
        }
    }

    /**
     * Parses date using primary formatter with automatic fallback cascade.
     */
    fun parseDate(dateStr: String, primaryFormatter: DateTimeFormatter): Long? {
        val clean = dateStr.trim()
        val zone = ZoneId.systemDefault()

        // 1. Try primary configured formatter
        try {
            val localDate = LocalDate.parse(clean, primaryFormatter)
            return localDate.atStartOfDay(zone).toInstant().toEpochMilli()
        } catch (_: Exception) {}

        // 2. Try fallback list
        for (formatter in FallbackDateFormatters) {
            try {
                val localDate = LocalDate.parse(clean, formatter)
                return localDate.atStartOfDay(zone).toInstant().toEpochMilli()
            } catch (_: Exception) {}
        }

        return null
    }
}
