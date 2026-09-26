package com.finpulse.app.domain.engine

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.model.DuplicateStatus
import com.finpulse.app.domain.model.ParsedCsvRow
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DuplicateDetectorEngineTest {

    private val testAccountId = "acc_checking_01"
    private val baseEpoch = LocalDate.of(2024, 3, 15).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun testFingerprintDeterminism() {
        val fp1 = DuplicateDetectorEngine.generateFingerprint(
            accountId = testAccountId,
            amountMinor = 4500L,
            type = TransactionType.EXPENSE,
            timestamp = baseEpoch,
            description = "Whole Foods Market",
            merchant = "Whole Foods"
        )

        val fp2 = DuplicateDetectorEngine.generateFingerprint(
            accountId = testAccountId,
            amountMinor = 4500L,
            type = TransactionType.EXPENSE,
            timestamp = baseEpoch,
            description = "whole foods market",
            merchant = "WHOLE FOODS"
        )

        assertEquals("Fingerprints must be identical despite casing", fp1, fp2)

        val fpDifferentAmount = DuplicateDetectorEngine.generateFingerprint(
            accountId = testAccountId,
            amountMinor = 5000L,
            type = TransactionType.EXPENSE,
            timestamp = baseEpoch,
            description = "Whole Foods Market",
            merchant = "Whole Foods"
        )
        assertNotEquals(fp1, fpDifferentAmount)
    }

    @Test
    fun testExactDuplicateDetection() {
        val existingTx = Transaction(
            id = "tx_existing_01",
            amount = Money(4500L, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = testAccountId,
            categoryId = "cat_groceries",
            merchant = "Whole Foods",
            description = "Whole Foods Market",
            timestamp = baseEpoch
        )

        val parsedRows = listOf(
            ParsedCsvRow(
                rowIndex = 2,
                rawValues = listOf("2024-03-15", "Whole Foods Market", "-45.00"),
                isValid = true,
                parsedDate = baseEpoch,
                parsedAmountMinor = 4500L,
                parsedType = TransactionType.EXPENSE,
                parsedDescription = "Whole Foods Market",
                parsedMerchant = "Whole Foods"
            )
        )

        val result = DuplicateDetectorEngine.evaluateDuplicates(
            rows = parsedRows,
            existingTransactions = listOf(existingTx),
            destinationAccountId = testAccountId
        )

        assertEquals(1, result.size)
        assertEquals(DuplicateStatus.EXACT_DUPLICATE, result[0].duplicateStatus)
        assertEquals("tx_existing_01", result[0].duplicateTransactionId)
        assertTrue(result[0].isExcluded) // default excluded for exact duplicates
    }

    @Test
    fun testPotentialDuplicateDetectionWithinWindow() {
        // Existing transaction on March 14 (1 day before CSV row on March 15)
        val march14Epoch = LocalDate.of(2024, 3, 14).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val existingTx = Transaction(
            id = "tx_existing_02",
            amount = Money(1299L, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = testAccountId,
            categoryId = "cat_subscriptions",
            merchant = "Netflix",
            description = "Netflix Monthly",
            timestamp = march14Epoch
        )

        val parsedRows = listOf(
            ParsedCsvRow(
                rowIndex = 2,
                rawValues = listOf("2024-03-15", "NETFLIX.COM", "-12.99"),
                isValid = true,
                parsedDate = baseEpoch, // March 15
                parsedAmountMinor = 1299L,
                parsedType = TransactionType.EXPENSE,
                parsedDescription = "NETFLIX.COM",
                parsedMerchant = "Netflix"
            )
        )

        val result = DuplicateDetectorEngine.evaluateDuplicates(
            rows = parsedRows,
            existingTransactions = listOf(existingTx),
            destinationAccountId = testAccountId,
            windowDays = 3L
        )

        assertEquals(1, result.size)
        assertEquals(DuplicateStatus.POTENTIAL_DUPLICATE, result[0].duplicateStatus)
        assertEquals("tx_existing_02", result[0].duplicateTransactionId)
        assertFalse("Potential duplicates must not be silently excluded; surface for user review", result[0].isExcluded)
    }

    @Test
    fun testIntraBatchDuplicateInSameFile() {
        val parsedRows = listOf(
            ParsedCsvRow(
                rowIndex = 2,
                rawValues = listOf("2024-03-15", "Shell Gas Station", "-35.00"),
                isValid = true,
                parsedDate = baseEpoch,
                parsedAmountMinor = 3500L,
                parsedType = TransactionType.EXPENSE,
                parsedDescription = "Shell Gas Station"
            ),
            ParsedCsvRow(
                rowIndex = 3,
                rawValues = listOf("2024-03-15", "Shell Gas Station", "-35.00"),
                isValid = true,
                parsedDate = baseEpoch,
                parsedAmountMinor = 3500L,
                parsedType = TransactionType.EXPENSE,
                parsedDescription = "Shell Gas Station"
            )
        )

        val result = DuplicateDetectorEngine.evaluateDuplicates(
            rows = parsedRows,
            existingTransactions = emptyList(),
            destinationAccountId = testAccountId
        )

        assertEquals(2, result.size)
        assertEquals(DuplicateStatus.NEW, result[0].duplicateStatus)
        assertFalse(result[0].isExcluded)

        assertEquals(DuplicateStatus.EXACT_DUPLICATE, result[1].duplicateStatus)
        assertTrue(result[1].duplicateTransactionDescription!!.contains("row #2", ignoreCase = true))
        assertTrue(result[1].isExcluded)
    }

    @Test
    fun testNonDuplicatesDifferingByAccountOrDateWindow() {
        val aprilEpoch = LocalDate.of(2024, 4, 15).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val existingTxOtherMonth = Transaction(
            id = "tx_past",
            amount = Money(4500L, "USD"),
            type = TransactionType.EXPENSE,
            sourceAccountId = testAccountId,
            categoryId = "cat_groceries",
            description = "Whole Foods Market",
            timestamp = aprilEpoch
        )

        val parsedRows = listOf(
            ParsedCsvRow(
                rowIndex = 2,
                rawValues = listOf("2024-03-15", "Whole Foods Market", "-45.00"),
                isValid = true,
                parsedDate = baseEpoch, // March 15
                parsedAmountMinor = 4500L,
                parsedType = TransactionType.EXPENSE,
                parsedDescription = "Whole Foods Market"
            )
        )

        val result = DuplicateDetectorEngine.evaluateDuplicates(
            rows = parsedRows,
            existingTransactions = listOf(existingTxOtherMonth),
            destinationAccountId = testAccountId,
            windowDays = 3L
        )

        assertEquals(1, result.size)
        assertEquals(DuplicateStatus.NEW, result[0].duplicateStatus)
        assertFalse(result[0].isExcluded)
    }
}
