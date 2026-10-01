package md.alexlab.finpulse.domain.engine

import md.alexlab.finpulse.domain.model.AmountMode
import md.alexlab.finpulse.domain.model.CsvColumnMapping
import md.alexlab.finpulse.domain.model.CsvFormatConfig
import md.alexlab.finpulse.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.format.DateTimeFormatter

class CsvParserEngineTest {

    @Test
    fun testRfc4180BasicAndQuotedFields() {
        val csv = """
            Date,Description,Amount
            2024-03-15,"Grocery, Supermarket",-45.50
            2024-03-16,"Quotes ""Inside"" Description",120.00
        """.trimIndent()

        val records = CsvParserEngine.parseRawRecords(csv, delimiter = ',', hasHeader = true)
        assertEquals(3, records.size)
        assertEquals(listOf("Date", "Description", "Amount"), records[0])
        assertEquals(listOf("2024-03-15", "Grocery, Supermarket", "-45.50"), records[1])
        assertEquals(listOf("2024-03-16", "Quotes \"Inside\" Description", "120.00"), records[2])
    }

    @Test
    fun testMultiLineQuotedField() {
        val csv = "2024-03-15,\"Multi\nLine\nDescription\",-50.00\n2024-03-16,Simple,25.00"
        val records = CsvParserEngine.parseRawRecords(csv, delimiter = ',', hasHeader = false)
        assertEquals(2, records.size)
        assertEquals("Multi\nLine\nDescription", records[0][1])
        assertEquals("-50.00", records[0][2])
        assertEquals("Simple", records[1][1])
    }

    @Test
    fun testCustomDelimiters() {
        // Semicolon
        val semiCsv = "2024-03-15;Coffee Shop;3,50"
        val semiRecords = CsvParserEngine.parseRawRecords(semiCsv, delimiter = ';', hasHeader = false)
        assertEquals(listOf("2024-03-15", "Coffee Shop", "3,50"), semiRecords[0])

        // Tab
        val tabCsv = "2024-03-15\tPharmacy\t-15.20"
        val tabRecords = CsvParserEngine.parseRawRecords(tabCsv, delimiter = '\t', hasHeader = false)
        assertEquals(listOf("2024-03-15", "Pharmacy", "-15.20"), tabRecords[0])

        // Pipe
        val pipeCsv = "2024-03-15|Salary|3000.00"
        val pipeRecords = CsvParserEngine.parseRawRecords(pipeCsv, delimiter = '|', hasHeader = false)
        assertEquals(listOf("2024-03-15", "Salary", "3000.00"), pipeRecords[0])
    }

    @Test
    fun testBomHandling() {
        val bomCsv = "\uFEFFDate,Amount\n2024-01-01,10.00"
        val records = CsvParserEngine.parseRawRecords(bomCsv, delimiter = ',', hasHeader = true)
        assertEquals(2, records.size)
        assertEquals("Date", records[0][0])
    }

    @Test
    fun testAmountParsingFormats() {
        // Standard US negative
        val (amt1, type1, err1) = CsvParserEngine.parseSingleAmount("-1,250.75", '.', false)
        assertNull(err1)
        assertEquals(125075L, amt1)
        assertEquals(TransactionType.EXPENSE, type1)

        // Parentheses negative (e.g. (45.00))
        val (amt2, type2, err2) = CsvParserEngine.parseSingleAmount("(45.00)", '.', false)
        assertNull(err2)
        assertEquals(4500L, amt2)
        assertEquals(TransactionType.EXPENSE, type2)

        // Trailing minus (e.g. 50.00-)
        val (amt3, type3, err3) = CsvParserEngine.parseSingleAmount("50.00-", '.', false)
        assertNull(err3)
        assertEquals(5000L, amt3)
        assertEquals(TransactionType.EXPENSE, type3)

        // DR (Debit) indicator
        val (amt4, type4, err4) = CsvParserEngine.parseSingleAmount("75.25 DR", '.', false)
        assertNull(err4)
        assertEquals(7525L, amt4)
        assertEquals(TransactionType.EXPENSE, type4)

        // CR (Credit) indicator
        val (amt5, type5, err5) = CsvParserEngine.parseSingleAmount("2,500.00 CR", '.', false)
        assertNull(err5)
        assertEquals(250000L, amt5)
        assertEquals(TransactionType.INCOME, type5)

        // European decimal comma (e.g. -1.234,56)
        val (amt6, type6, err6) = CsvParserEngine.parseSingleAmount("-1.234,56", ',', false)
        assertNull(err6)
        assertEquals(123456L, amt6)
        assertEquals(TransactionType.EXPENSE, type6)

        // Reversed signs
        val (amt7, type7, err7) = CsvParserEngine.parseSingleAmount("45.00", '.', true)
        assertNull(err7)
        assertEquals(4500L, amt7)
        assertEquals(TransactionType.EXPENSE, type7) // positive treated as expense when reversed
    }

    @Test
    fun testSeparateDebitCreditParsing() {
        // Debit populated
        val (amt1, type1, err1) = CsvParserEngine.parseSplitDebitCredit("54.20", "", '.', false)
        assertNull(err1)
        assertEquals(5420L, amt1)
        assertEquals(TransactionType.EXPENSE, type1)

        // Credit populated
        val (amt2, type2, err2) = CsvParserEngine.parseSplitDebitCredit(null, "1500.00", '.', false)
        assertNull(err2)
        assertEquals(150000L, amt2)
        assertEquals(TransactionType.INCOME, type2)

        // Both empty
        val (_, _, err3) = CsvParserEngine.parseSplitDebitCredit("", "", '.', false)
        assertNotNull(err3)

        // Both populated with net difference
        val (amt4, type4, err4) = CsvParserEngine.parseSplitDebitCredit("10.00", "25.00", '.', false)
        assertNull(err4)
        assertEquals(1500L, amt4)
        assertEquals(TransactionType.INCOME, type4)
    }

    @Test
    fun testDateParsingVariations() {
        val defaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        assertNotNull(CsvParserEngine.parseDate("2024-03-15", defaultFormatter))
        assertNotNull(CsvParserEngine.parseDate("03/15/2024", defaultFormatter))
        assertNotNull(CsvParserEngine.parseDate("15/03/2024", defaultFormatter))
        assertNotNull(CsvParserEngine.parseDate("15.03.2024", defaultFormatter))
        assertNotNull(CsvParserEngine.parseDate("2024/03/15", defaultFormatter))
        assertNotNull(CsvParserEngine.parseDate("15-Mar-2024", defaultFormatter))
        assertNotNull(CsvParserEngine.parseDate("20240315", defaultFormatter))
        assertNull(CsvParserEngine.parseDate("not-a-date", defaultFormatter))
    }

    @Test
    fun testParseRowsCompleteValidAndInvalid() {
        val csv = """
            Date,Description,Amount
            2024-03-15,Starbucks Coffee,-6.50
            invalid-date,Target Store,-25.00
            2024-03-16,, -12.00
            2024-03-17,Salary,3500.00
            2024-03-18,Zero Tx,0.00
        """.trimIndent()

        val config = CsvFormatConfig(
            delimiter = ',',
            dateFormat = "yyyy-MM-dd",
            hasHeader = true,
            amountMode = AmountMode.SINGLE_AMOUNT
        )
        val mapping = CsvColumnMapping(
            dateColumnIndex = 0,
            descriptionColumnIndex = 1,
            amountColumnIndex = 2
        )

        val parsed = CsvParserEngine.parseRows(csv, config, mapping)
        assertEquals(5, parsed.size)

        // Row 1: Valid Starbucks
        assertTrue(parsed[0].isValid)
        assertEquals(650L, parsed[0].parsedAmountMinor)
        assertEquals(TransactionType.EXPENSE, parsed[0].parsedType)
        assertEquals("Starbucks Coffee", parsed[0].parsedDescription)

        // Row 2: Invalid date
        assertFalse(parsed[1].isValid)
        assertTrue(parsed[1].errorReason!!.contains("date", ignoreCase = true))

        // Row 3: Missing description
        assertFalse(parsed[2].isValid)
        assertTrue(parsed[2].errorReason!!.contains("description", ignoreCase = true))

        // Row 4: Valid Salary
        assertTrue(parsed[3].isValid)
        assertEquals(350000L, parsed[3].parsedAmountMinor)
        assertEquals(TransactionType.INCOME, parsed[3].parsedType)

        // Row 5: Zero amount invalid
        assertFalse(parsed[4].isValid)
        assertTrue(parsed[4].errorReason!!.contains("zero", ignoreCase = true))
    }

    @Test
    fun testLargeDatasetPerformance() {
        val sb = StringBuilder()
        sb.append("Date,Description,Amount\n")
        for (i in 1..2000) {
            sb.append("2024-03-15,\"Merchant #$i\",-${(i % 100) + 1}.50\n")
        }

        val config = CsvFormatConfig(delimiter = ',', dateFormat = "yyyy-MM-dd", hasHeader = true)
        val mapping = CsvColumnMapping(dateColumnIndex = 0, descriptionColumnIndex = 1, amountColumnIndex = 2)

        val startTime = System.currentTimeMillis()
        val rows = CsvParserEngine.parseRows(sb.toString(), config, mapping)
        val duration = System.currentTimeMillis() - startTime

        assertEquals(2000, rows.size)
        assertTrue(rows.all { it.isValid })
        assertTrue("Parsing 2000 rows should take under 2000ms, took ${duration}ms", duration < 2000)
    }
}
