package com.finpulse.app.domain.review

import com.finpulse.app.core.model.Money
import com.finpulse.app.domain.engine.ReviewInboxEngine
import com.finpulse.app.domain.model.Account
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.Category
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.RecurringOccurrence
import com.finpulse.app.domain.model.RecurringTransaction
import com.finpulse.app.domain.model.ReviewItemPriority
import com.finpulse.app.domain.model.ReviewItemType
import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ReviewInboxEngineTest {

    private val account = Account(
        id = "acc_main",
        name = "Checking Account",
        type = AccountType.BANK,
        balance = Money(500000),
        availableBalance = Money(500000)
    )

    private val groceriesCategory = Category(
        id = "cat_groceries",
        name = "Groceries",
        type = CategoryType.EXPENSE,
        icon = "shoppingcart",
        colorHex = 0xFF4CAF50
    )

    private val uncategorizedCategory = Category(
        id = "cat_uncategorized",
        name = "Uncategorized",
        type = CategoryType.EXPENSE,
        icon = "help",
        colorHex = 0xFF9E9E9E
    )

    private val now = 1700000000000L // arbitrary fixed timestamp

    @Test
    fun `test uncategorized transactions detected`() {
        val tx = Transaction(
            id = "tx_1",
            amount = Money(2500),
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = uncategorizedCategory.id,
            merchant = "Local Mart",
            timestamp = now,
            isCategoryConfirmed = false
        )

        val summary = ReviewInboxEngine.evaluate(
            transactions = listOf(tx),
            categories = listOf(groceriesCategory, uncategorizedCategory),
            accounts = listOf(account),
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = emptyList(),
            occurrences = emptyList(),
            dismissedIds = emptySet(),
            nowMillis = now
        )

        assertEquals(1, summary.totalCount)
        assertEquals(1, summary.uncategorizedCount)
        val item = summary.items.first()
        assertEquals(ReviewItemType.UNCATEGORIZED, item.type)
        assertEquals("tx_1", item.transaction?.id)
        assertEquals(ReviewItemPriority.ACTIONABLE, item.priority)
    }

    @Test
    fun `test imported transaction needing confirmation detected`() {
        val tx = Transaction(
            id = "tx_imported",
            amount = Money(4200),
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = groceriesCategory.id,
            merchant = "Trader Joe's",
            notes = "CSV Import from bank statement",
            timestamp = now,
            isCategoryConfirmed = false
        )

        val summary = ReviewInboxEngine.evaluate(
            transactions = listOf(tx),
            categories = listOf(groceriesCategory),
            accounts = listOf(account),
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = emptyList(),
            occurrences = emptyList(),
            dismissedIds = emptySet(),
            nowMillis = now
        )

        assertEquals(1, summary.totalCount)
        assertEquals(1, summary.importedCount)
        assertEquals(ReviewItemType.IMPORTED_CONFIRMATION, summary.items.first().type)
    }

    @Test
    fun `test suspected duplicates detected for same account and identical amount within window`() {
        val tx1 = Transaction(
            id = "tx_orig",
            amount = Money(3000),
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = groceriesCategory.id,
            merchant = "Whole Foods",
            timestamp = now,
            isCategoryConfirmed = true
        )

        val tx2 = Transaction(
            id = "tx_twin",
            amount = Money(3000),
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = groceriesCategory.id,
            merchant = "Whole Foods",
            timestamp = now + 1000L, // same day
            isCategoryConfirmed = true
        )

        val summary = ReviewInboxEngine.evaluate(
            transactions = listOf(tx1, tx2),
            categories = listOf(groceriesCategory),
            accounts = listOf(account),
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = emptyList(),
            occurrences = emptyList(),
            dismissedIds = emptySet(),
            nowMillis = now
        )

        assertEquals(1, summary.duplicatesCount)
        val dupItem = summary.items.first { it.type == ReviewItemType.SUSPECTED_DUPLICATE }
        assertNotNull(dupItem.transaction)
        assertNotNull(dupItem.duplicateCandidate)
        assertEquals(ReviewItemPriority.WARNING, dupItem.priority)
    }

    @Test
    fun `test missing merchant information flagged`() {
        val tx = Transaction(
            id = "tx_no_merch",
            amount = Money(1500),
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = groceriesCategory.id,
            merchant = null,
            description = "POS DEBIT 78912",
            timestamp = now,
            isCategoryConfirmed = true
        )

        val summary = ReviewInboxEngine.evaluate(
            transactions = listOf(tx),
            categories = listOf(groceriesCategory),
            accounts = listOf(account),
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = emptyList(),
            occurrences = emptyList(),
            dismissedIds = emptySet(),
            nowMillis = now
        )

        assertEquals(1, summary.missingMerchantCount)
        assertEquals(ReviewItemType.MISSING_MERCHANT, summary.items.first().type)
    }

    @Test
    fun `test unusual amounts detected by deterministic rules`() {
        // Zero amount transaction
        val zeroTx = Transaction(
            id = "tx_zero",
            amount = Money(0),
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = groceriesCategory.id,
            merchant = "Test",
            timestamp = now,
            isCategoryConfirmed = true
        )

        // Large expense >= $1,000.00 (100,000 minor)
        val largeTx = Transaction(
            id = "tx_large",
            amount = Money(150000), // $1,500.00
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = groceriesCategory.id,
            merchant = "Appliance Depot",
            timestamp = now,
            isCategoryConfirmed = true
        )

        val summary = ReviewInboxEngine.evaluate(
            transactions = listOf(zeroTx, largeTx),
            categories = listOf(groceriesCategory),
            accounts = listOf(account),
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = emptyList(),
            occurrences = emptyList(),
            dismissedIds = emptySet(),
            nowMillis = now
        )

        assertEquals(2, summary.unusualAmountsCount)
        assertTrue(summary.items.any { it.id.contains("zero") })
        assertTrue(summary.items.any { it.id.contains("high") })
    }

    @Test
    fun `test category outlier flagged when amount exceeds 3x category average`() {
        // 3 normal transactions around $30 ($3,000 minor)
        val normalTxs = (1..3).map { idx ->
            Transaction(
                id = "tx_norm_$idx",
                amount = Money(3000),
                type = TransactionType.EXPENSE,
                sourceAccountId = account.id,
                categoryId = groceriesCategory.id,
                merchant = "Market",
                timestamp = now - idx * 86400000L,
                isCategoryConfirmed = true
            )
        }

        // Outlier transaction $150 ($15,000 minor, which is 5x the average $30)
        val outlierTx = Transaction(
            id = "tx_outlier",
            amount = Money(15000),
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = groceriesCategory.id,
            merchant = "Mega Wholesale Market",
            timestamp = now,
            isCategoryConfirmed = true
        )

        val summary = ReviewInboxEngine.evaluate(
            transactions = normalTxs + outlierTx,
            categories = listOf(groceriesCategory),
            accounts = listOf(account),
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = emptyList(),
            occurrences = emptyList(),
            dismissedIds = emptySet(),
            nowMillis = now
        )

        val outlierItem = summary.items.firstOrNull { it.id.contains("outlier") }
        assertNotNull(outlierItem)
        assertEquals(ReviewItemType.UNUSUAL_AMOUNT, outlierItem?.type)
        assertTrue(outlierItem?.reason?.contains("higher than your average") == true)
    }

    @Test
    fun `test overdue expected bill detected`() {
        val overdueOcc = RecurringOccurrence(
            id = "rec_rent_100",
            ruleId = "rule_rent",
            ruleTitle = "Apartment Rent",
            amount = Money(120000),
            type = TransactionType.EXPENSE,
            accountId = account.id,
            categoryId = groceriesCategory.id,
            dueDate = now - 3 * 86400000L, // 3 days ago
            status = OccurrenceStatus.OVERDUE
        )

        val summary = ReviewInboxEngine.evaluate(
            transactions = emptyList(),
            categories = listOf(groceriesCategory),
            accounts = listOf(account),
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = emptyList(),
            occurrences = listOf(overdueOcc),
            dismissedIds = emptySet(),
            nowMillis = now
        )

        assertEquals(1, summary.overdueBillsCount)
        val item = summary.items.first()
        assertEquals(ReviewItemType.OVERDUE_BILL, item.type)
        assertEquals(ReviewItemPriority.CRITICAL, item.priority)
    }

    @Test
    fun `test failed recurring rule generation detected when target account archived or missing`() {
        val brokenRule = RecurringTransaction(
            id = "rule_broken",
            title = "Gym Membership",
            amount = Money(5000),
            type = TransactionType.EXPENSE,
            accountId = "acc_deleted_or_archived",
            categoryId = groceriesCategory.id,
            frequency = PaymentFrequency.MONTHLY,
            nextDueDate = now,
            isActive = true
        )

        val summary = ReviewInboxEngine.evaluate(
            transactions = emptyList(),
            categories = listOf(groceriesCategory),
            accounts = listOf(account), // acc_deleted_or_archived not here!
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = listOf(brokenRule),
            occurrences = emptyList(),
            dismissedIds = emptySet(),
            nowMillis = now
        )

        assertEquals(1, summary.failedRecurringCount)
        val item = summary.items.first()
        assertEquals(ReviewItemType.FAILED_RECURRING, item.type)
        assertEquals(ReviewItemPriority.CRITICAL, item.priority)
        assertTrue(item.reason.contains("missing or archived"))
    }

    @Test
    fun `test dismissed items are excluded from inbox summary`() {
        val tx = Transaction(
            id = "tx_dismissed",
            amount = Money(100),
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = uncategorizedCategory.id,
            timestamp = now,
            isCategoryConfirmed = false
        )

        val dismissedKey = "inbox_uncat_${tx.id}"

        val summary = ReviewInboxEngine.evaluate(
            transactions = listOf(tx),
            categories = listOf(uncategorizedCategory),
            accounts = listOf(account),
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = emptyList(),
            occurrences = emptyList(),
            dismissedIds = setOf(dismissedKey),
            nowMillis = now
        )

        assertEquals(0, summary.totalCount)
    }

    @Test
    fun `test priority ordering orders critical items first`() {
        val uncatTx = Transaction(
            id = "tx_uncat",
            amount = Money(500),
            type = TransactionType.EXPENSE,
            sourceAccountId = account.id,
            categoryId = uncategorizedCategory.id,
            timestamp = now,
            isCategoryConfirmed = false
        )

        val overdueOcc = RecurringOccurrence(
            id = "rec_overdue",
            ruleId = "rule_1",
            ruleTitle = "Electric Bill",
            amount = Money(8500),
            type = TransactionType.EXPENSE,
            accountId = account.id,
            categoryId = groceriesCategory.id,
            dueDate = now - 2 * 86400000L,
            status = OccurrenceStatus.OVERDUE
        )

        val summary = ReviewInboxEngine.evaluate(
            transactions = listOf(uncatTx),
            categories = listOf(groceriesCategory, uncategorizedCategory),
            accounts = listOf(account),
            activeRules = emptyList(),
            signals = emptyList(),
            recurringRules = emptyList(),
            occurrences = listOf(overdueOcc),
            dismissedIds = emptySet(),
            nowMillis = now
        )

        assertEquals(2, summary.totalCount)
        assertEquals(ReviewItemPriority.CRITICAL, summary.items[0].priority)
        assertEquals(ReviewItemPriority.ACTIONABLE, summary.items[1].priority)
    }
}
