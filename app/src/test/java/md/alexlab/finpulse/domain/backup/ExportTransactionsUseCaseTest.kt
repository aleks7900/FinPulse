package md.alexlab.finpulse.domain.backup

import md.alexlab.finpulse.core.model.Money
import md.alexlab.finpulse.domain.model.Account
import md.alexlab.finpulse.domain.model.AccountType
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.DateRangePreset
import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.TransactionType
import md.alexlab.finpulse.domain.model.backup.ExportFilterParams
import md.alexlab.finpulse.domain.model.backup.FinPulseFullBackup
import md.alexlab.finpulse.domain.model.backup.RestoreSummary
import md.alexlab.finpulse.domain.repository.AccountRepository
import md.alexlab.finpulse.domain.repository.BackupRepository
import md.alexlab.finpulse.domain.repository.CategoryRepository
import md.alexlab.finpulse.domain.usecase.backup.ExportTransactionsUseCase
import md.alexlab.finpulse.domain.usecase.backup.TransactionsExportContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ExportTransactionsUseCaseTest {

    private lateinit var backupRepository: FakeBackupRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var useCase: ExportTransactionsUseCase

    private val json = Json { ignoreUnknownKeys = true }

    private val account1 = Account(
        id = "acc-main",
        name = "Main Bank",
        type = AccountType.BANK,
        balance = Money(100000L, "USD"),
        availableBalance = Money(100000L, "USD"),
        isArchived = false
    )

    private val category1 = Category(
        id = "cat-food",
        name = "Groceries & Dining",
        type = CategoryType.EXPENSE,
        icon = "restaurant",
        colorHex = 0xFF4CAF50
    )

    private val tx1 = Transaction(
        id = "tx-001",
        sourceAccountId = "acc-main",
        categoryId = "cat-food",
        amount = Money(4250L, "USD"), // $42.50
        type = TransactionType.EXPENSE,
        timestamp = 1717200000000L, // 2024-06-01
        merchant = "Whole Foods, Inc.",
        description = "Weekly \"special\" groceries",
        notes = "Bought apples, oranges\nand bread",
        tags = listOf("groceries", "organic"),
        isCategoryConfirmed = true
    )

    private val tx2 = Transaction(
        id = "tx-002",
        sourceAccountId = "acc-main",
        categoryId = "cat-food",
        amount = Money(1500L, "USD"), // $15.00
        type = TransactionType.EXPENSE,
        timestamp = 1717286400000L,
        merchant = "Coffee Shop",
        description = "Espresso",
        notes = null,
        tags = emptyList(),
        isCategoryConfirmed = false
    )

    @Before
    fun setup() {
        backupRepository = FakeBackupRepository(listOf(tx1, tx2))
        accountRepository = FakeAccountRepository(listOf(account1))
        categoryRepository = FakeCategoryRepository(listOf(category1))
        useCase = ExportTransactionsUseCase(backupRepository, accountRepository, categoryRepository)
    }

    @Test
    fun `exportCsv generates correct header and escapes quotes and commas`() = runBlocking {
        val params = ExportFilterParams(dateRangePreset = DateRangePreset.ALL)
        val csv = useCase.exportCsv(params)

        val lines = csv.trim().lines()
        assertTrue(lines.isNotEmpty())
        assertEquals("ID,Date,Time,Type,Amount,Currency,Account,Category,Merchant,Description,Notes,Tags,Status", lines[0])

        // First transaction row has escaped comma in merchant and escaped quotes in description and newline in notes
        val row1 = lines.find { it.contains("tx-001") }
        assertNotNull(row1)
        assertTrue(csv.contains("\"Whole Foods, Inc.\""))
        assertTrue(csv.contains("\"Weekly \"\"special\"\" groceries\""))
        assertTrue(csv.contains("Main Bank"))
        assertTrue(csv.contains("Groceries & Dining"))
        assertTrue(csv.contains("42.50"))
        assertTrue(csv.contains("Confirmed"))

        // Second transaction row
        val row2 = lines.find { it.contains("tx-002") }
        assertNotNull(row2)
        assertTrue(row2!!.contains("Needs Review"))
        assertTrue(row2.contains("15.00"))
    }

    @Test
    fun `exportJson generates valid TransactionsExportContainer JSON`() = runBlocking {
        val params = ExportFilterParams(dateRangePreset = DateRangePreset.ALL)
        val jsonString = useCase.exportJson(params)

        val container = json.decodeFromString<TransactionsExportContainer>(jsonString)
        assertEquals(2, container.count)
        assertEquals(2, container.transactions.size)

        val item1 = container.transactions.first { it.id == "tx-001" }
        assertEquals("42.50", item1.amount)
        assertEquals(4250L, item1.amountMinor)
        assertEquals("USD", item1.currency)
        assertEquals("Main Bank", item1.accountName)
        assertEquals("Groceries & Dining", item1.categoryName)
        assertEquals("Whole Foods, Inc.", item1.merchant)
        assertEquals("Weekly \"special\" groceries", item1.description)
        assertEquals(listOf("groceries", "organic"), item1.tags)
        assertTrue(item1.isConfirmed)
    }

    @Test
    fun `export delegates filter parameters to repository`() = runBlocking {
        val params = ExportFilterParams(
            dateRangePreset = DateRangePreset.THIS_MONTH,
            selectedAccountId = "acc-main"
        )
        useCase.exportCsv(params)

        assertEquals(params, backupRepository.lastParamsReceived)
    }

    // --- Fakes ---

    private class FakeBackupRepository(
        private val transactions: List<Transaction>
    ) : BackupRepository {
        var lastParamsReceived: ExportFilterParams? = null

        override suspend fun createFullBackup(): FinPulseFullBackup {
            throw UnsupportedOperationException()
        }

        override suspend fun restoreFullBackup(backup: FinPulseFullBackup): RestoreSummary {
            throw UnsupportedOperationException()
        }

        override suspend fun getFilteredTransactions(params: ExportFilterParams): List<Transaction> {
            lastParamsReceived = params
            return transactions
        }
    }

    private class FakeAccountRepository(
        private val accounts: List<Account>
    ) : AccountRepository {
        override fun getAllAccountsFlow(): Flow<List<Account>> = flowOf(accounts)
        override fun getActiveAccountsFlow(): Flow<List<Account>> = flowOf(accounts)
        override suspend fun getAccountById(id: String): Account? = accounts.find { it.id == id }
        override suspend fun saveAccount(account: Account) {}
        override suspend fun deleteAccount(id: String) {}
        override suspend fun setArchived(id: String, isArchived: Boolean) {}
        override suspend fun updateBalances(accountId: String, balance: Money, availableBalance: Money) {}
    }

    private class FakeCategoryRepository(
        private val categories: List<Category>
    ) : CategoryRepository {
        override fun getAllCategoriesFlow(): Flow<List<Category>> = flowOf(categories)
        override fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<Category>> =
            flowOf(categories.filter { it.type == type })
        override suspend fun getCategoryById(id: String): Category? = categories.find { it.id == id }
        override suspend fun saveCategory(category: Category) {}
        override suspend fun seedDefaultCategoriesIfNeeded() {}
    }
}
