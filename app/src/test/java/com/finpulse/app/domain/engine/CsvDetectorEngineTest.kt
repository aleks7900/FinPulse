package com.finpulse.app.domain.engine

import com.finpulse.app.domain.model.AmountMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvDetectorEngineTest {

    @Test
    fun testDetectStandardUsCsv() {
        val csv = """
            Transaction Date,Description,Amount,Category,Balance
            2024-03-15,"Whole Foods Market",-68.40,"Groceries",4831.60
            2024-03-15,"Shell Oil Fuel",-42.50,"Auto & Transport",4789.10
            2024-03-14,"Acme Corp Payroll",3200.00,"Salary",8031.60
        """.trimIndent()

        val (config, mapping) = CsvDetectorEngine.detectConfigAndMapping(csv)

        assertEquals(',', config.delimiter)
        assertTrue(config.hasHeader)
        assertEquals(AmountMode.SINGLE_AMOUNT, config.amountMode)
        assertEquals('.', config.decimalSeparator)
        assertEquals("yyyy-MM-dd", config.dateFormat)

        assertEquals(0, mapping.dateColumnIndex)
        assertEquals(1, mapping.descriptionColumnIndex)
        assertEquals(2, mapping.amountColumnIndex)
        assertEquals(3, mapping.categoryColumnIndex)
        assertEquals(4, mapping.balanceColumnIndex)
    }

    @Test
    fun testDetectEuropeanSemicolonCsv() {
        val csv = """
            Buchungstag;Verwendungszweck;Betrag;Währung
            15.03.2024;EDEKA Supermarkt;-45,80;EUR
            14.03.2024;Gehaltszahlung Arbeitgeber;2850,00;EUR
            12.03.2024;Deutsche Bahn Ticket;-64,20;EUR
        """.trimIndent()

        val (config, mapping) = CsvDetectorEngine.detectConfigAndMapping(csv)

        assertEquals(';', config.delimiter)
        assertTrue(config.hasHeader)
        assertEquals(',', config.decimalSeparator)
        assertEquals("dd.MM.yyyy", config.dateFormat)
        assertEquals(0, mapping.dateColumnIndex)
        assertEquals(1, mapping.descriptionColumnIndex)
        assertEquals(2, mapping.amountColumnIndex)
    }

    @Test
    fun testDetectSplitDebitCreditCsv() {
        val csv = """
            Date,Description,Debit,Credit,Balance
            03/15/2024,Trader Joe's,54.20,,3145.80
            03/14/2024,Direct Deposit Salary,,2600.00,3200.00
            03/12/2024,Uber Trip,22.40,,600.00
        """.trimIndent()

        val (config, mapping) = CsvDetectorEngine.detectConfigAndMapping(csv)

        assertEquals(',', config.delimiter)
        assertTrue(config.hasHeader)
        assertEquals(AmountMode.SEPARATE_DEBIT_CREDIT, config.amountMode)
        assertEquals("MM/dd/yyyy", config.dateFormat)
        assertEquals(0, mapping.dateColumnIndex)
        assertEquals(1, mapping.descriptionColumnIndex)
        assertEquals(2, mapping.debitColumnIndex)
        assertEquals(3, mapping.creditColumnIndex)
    }
}
