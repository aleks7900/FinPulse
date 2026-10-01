package md.alexlab.finpulse.domain.category

import md.alexlab.finpulse.core.database.dao.CategoryDao
import md.alexlab.finpulse.core.database.entity.CategoryEntity
import md.alexlab.finpulse.core.ui.getCategoryStringRes
import md.alexlab.finpulse.core.ui.getDisplayName
import md.alexlab.finpulse.data.repository.CategoryRepositoryImpl
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType
import md.alexlab.finpulse.domain.model.DefaultCategoryCatalog
import md.alexlab.finpulse.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryCatalogTest {

    @Test
    fun testCatalogHasStableUniqueIdsAndValidFields() {
        val all = DefaultCategoryCatalog.ALL
        assertTrue("Catalog should contain over 150 categories", all.size >= 150)

        val ids = all.map { it.id }
        assertEquals("All category IDs must be strictly unique", ids.size, ids.distinct().size)

        for (category in all) {
            assertTrue("Category ID must start with cat_: ${category.id}", category.id.startsWith("cat_"))
            assertTrue("Category name must not be blank: ${category.id}", category.name.isNotBlank())
            assertTrue("Category type must be EXPENSE or INCOME: ${category.id}", category.type in listOf("EXPENSE", "INCOME"))
            assertTrue("Category icon must not be blank: ${category.id}", category.icon.isNotBlank())
            assertTrue("Category isDefault must be true: ${category.id}", category.isDefault)
            assertTrue("Category sortOrder must be positive: ${category.id}", category.sortOrder > 0)
        }
    }

    @Test
    fun testCatalogParentChildHierarchyIntegrity() {
        val all = DefaultCategoryCatalog.ALL
        val categoryMap = all.associateBy { it.id }

        for (category in all) {
            val parentId = category.parentCategoryId
            if (parentId != null) {
                val parent = categoryMap[parentId]
                assertNotNull("Parent category $parentId must exist for child ${category.id}", parent)
                assertNull("Parent category $parentId must not itself have a parent (flat 2-level hierarchy)", parent?.parentCategoryId)
                if (parentId != "cat_transfer") {
                    assertEquals("Parent and child must share the same type: ${category.id}", parent?.type, category.type)
                }
            }
        }
    }

    @Test
    fun testCatalogCoversAllRequiredExpenseAndIncomeGroups() {
        val all = DefaultCategoryCatalog.ALL
        val categoryMap = all.associateBy { it.id }

        // Expense Groups
        val expenseGroupParents = listOf(
            "cat_housing",        // Home & Utilities
            "cat_food",           // Food & Dining
            "cat_transport",      // Transportation
            "cat_shopping",       // Shopping
            "cat_health",         // Health
            "cat_entertainment",  // Entertainment
            "cat_subscriptions",  // Subscriptions & Digital Services
            "cat_personal_care",  // Personal Care
            "cat_family",         // Family & Children
            "cat_pets",           // Pets
            "cat_travel",         // Travel
            "cat_education",      // Education
            "cat_financial",      // Financial Expenses
            "cat_debt",           // Loans & Debt
            "cat_gifts",          // Charity & Donations
            "cat_other_expenses"  // Other Expenses
        )

        for (groupId in expenseGroupParents) {
            val group = categoryMap[groupId]
            assertNotNull("Expense group parent $groupId must exist in catalog", group)
            assertEquals("EXPENSE", group?.type)
            val children = all.filter { it.parentCategoryId == groupId }
            assertTrue("Expense group $groupId must have subcategories", children.isNotEmpty())
        }

        // Income Groups
        val incomeGroupParents = listOf(
            "cat_salary",             // Employment
            "cat_freelance",          // Business & Freelance
            "cat_invest_return",      // Investments
            "cat_income_government",  // Government / Benefits
            "cat_other_income"        // Other Income
        )

        for (groupId in incomeGroupParents) {
            val group = categoryMap[groupId]
            assertNotNull("Income group parent $groupId must exist in catalog", group)
            assertEquals("INCOME", group?.type)
            val children = all.filter { it.parentCategoryId == groupId }
            assertTrue("Income group $groupId must have subcategories", children.isNotEmpty())
        }

        // Transfers
        assertNotNull("Transfer parent must exist", categoryMap["cat_transfer"])
        assertNotNull("Cash withdrawal must exist", categoryMap["cat_transfer_withdrawal"])
        assertNotNull("Cash deposit must exist", categoryMap["cat_transfer_deposit"])
    }

    @Test
    fun testSeedingIsIdempotentAndPreservesCustomCategories() = runBlocking {
        val fakeDao = InMemoryCategoryDao()
        val repo = CategoryRepositoryImpl(fakeDao)

        // 1. Initial seed on empty database
        repo.seedDefaultCategoriesIfNeeded()
        val countAfterFirstSeed = fakeDao.getAllCategoryIds().size
        assertEquals(DefaultCategoryCatalog.ALL.size, countAfterFirstSeed)

        // 2. Second seed execution should be completely idempotent (no duplicates inserted)
        repo.seedDefaultCategoriesIfNeeded()
        val countAfterSecondSeed = fakeDao.getAllCategoryIds().size
        assertEquals(countAfterFirstSeed, countAfterSecondSeed)

        // 3. User creates custom category
        val customCat = CategoryEntity(
            id = "custom_crypto_mining",
            name = "My Mining Rig",
            type = "INCOME",
            parentCategoryId = null,
            icon = "work",
            colorHex = 0xFFFF9800,
            isDefault = false,
            sortOrder = 9999
        )
        fakeDao.insertCategoriesIgnore(listOf(customCat))

        // 4. Third seed after app upgrade: custom category remains untouched
        repo.seedDefaultCategoriesIfNeeded()
        val allIds = fakeDao.getAllCategoryIds()
        assertTrue("Custom category must survive seeding: custom_crypto_mining", allIds.contains("custom_crypto_mining"))
        assertEquals(countAfterFirstSeed + 1, allIds.size)

        val retrievedCustom = fakeDao.getCategoryById("custom_crypto_mining")
        assertNotNull(retrievedCustom)
        assertEquals("My Mining Rig", retrievedCustom?.name)
        assertFalse(retrievedCustom!!.isDefault)
    }

    @Test
    fun testUpgradeFromLegacyCatalogAddsNewDefaultsWithoutOverwritingExisting() = runBlocking {
        val fakeDao = InMemoryCategoryDao()
        val repo = CategoryRepositoryImpl(fakeDao)

        // Simulate legacy database that only had the original 17 categories
        val legacyIds = listOf(
            "cat_food", "cat_groceries", "cat_housing", "cat_transport", "cat_fuel",
            "cat_shopping", "cat_entertainment", "cat_health", "cat_education",
            "cat_fitness", "cat_subscriptions", "cat_gifts", "cat_salary",
            "cat_freelance", "cat_invest_return", "cat_other_income", "cat_uncategorized"
        )
        val legacyCategories = DefaultCategoryCatalog.ALL.filter { it.id in legacyIds }
        fakeDao.insertCategoriesIgnore(legacyCategories)
        assertEquals(17, fakeDao.getAllCategoryIds().size)

        // App update runs seedDefaultCategoriesIfNeeded()
        repo.seedDefaultCategoriesIfNeeded()

        val allIds = fakeDao.getAllCategoryIds()
        assertEquals(DefaultCategoryCatalog.ALL.size, allIds.size)
        assertTrue("Legacy categories must still be present", allIds.containsAll(legacyIds))
        assertTrue("New categories like rent must be added", allIds.contains("cat_home_rent"))
        assertTrue("New categories like dental must be added", allIds.contains("cat_health_dentist"))
        assertTrue("New categories like streaming must be added", allIds.contains("cat_sub_netflix"))
    }

    @Test
    fun testCategoryDisplayNameLocalizationMapping() {
        val all = DefaultCategoryCatalog.ALL

        for (category in all) {
            val resId = getCategoryStringRes(category.id)
            assertNotNull("Category ${category.id} must map to a string resource in CategoryExtensions", resId)
        }

        // Custom category test: not in resource map, uses user-provided name
        val customCategory = Category(
            id = "custom_user_cat_123",
            name = "My Personal Side Project",
            type = CategoryType.INCOME,
            parentCategoryId = null,
            icon = "work",
            colorHex = 0xFF4CAF50,
            isDefault = false,
            sortOrder = 1
        )
        assertNull(getCategoryStringRes(customCategory.id))
    }

    @Test
    fun testTransactionTypeCategoryFiltering() {
        val all = DefaultCategoryCatalog.ALL.map { it.toDomain() }

        val expenseCategories = all.filter { it.type == CategoryType.EXPENSE }
        val incomeCategories = all.filter { it.type == CategoryType.INCOME }

        assertTrue("Expense categories list should not be empty", expenseCategories.isNotEmpty())
        assertTrue("Income categories list should not be empty", incomeCategories.isNotEmpty())

        // Ensure no overlap
        val expenseIds = expenseCategories.map { it.id }.toSet()
        val incomeIds = incomeCategories.map { it.id }.toSet()
        val intersection = expenseIds.intersect(incomeIds)
        assertTrue("No category can be both expense and income: $intersection", intersection.isEmpty())

        // Expense budget verification: only expense categories should be in budgets
        val budgetEligible = all.filter { it.type == CategoryType.EXPENSE }
        assertFalse("Budget eligible categories should not contain salary", budgetEligible.any { it.id == "cat_salary" })
        assertFalse("Budget eligible categories should not contain freelance", budgetEligible.any { it.id == "cat_freelance" })
        assertTrue("Budget eligible categories must contain groceries", budgetEligible.any { it.id == "cat_groceries" })
    }

    // Helper in-memory DAO for unit testing
    private class InMemoryCategoryDao : CategoryDao {
        private val storage = mutableMapOf<String, CategoryEntity>()
        private val flow = MutableStateFlow<List<CategoryEntity>>(emptyList())

        override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = flow

        override fun getCategoriesByTypeFlow(type: String): Flow<List<CategoryEntity>> =
            flow.map { list -> list.filter { it.type == type } }

        override fun getCategoryByIdFlow(id: String): Flow<CategoryEntity?> =
            flow.map { list -> list.find { it.id == id } }

        override suspend fun getCategoryById(id: String): CategoryEntity? = storage[id]

        override suspend fun getAllCategoryIds(): List<String> = storage.keys.toList()

        override suspend fun insertCategories(categories: List<CategoryEntity>) {
            categories.forEach { storage[it.id] = it }
            flow.value = storage.values.toList()
        }

        override suspend fun insertCategoriesIgnore(categories: List<CategoryEntity>) {
            categories.forEach {
                if (!storage.containsKey(it.id)) {
                    storage[it.id] = it
                }
            }
            flow.value = storage.values.toList()
        }

        override suspend fun insertCategory(category: CategoryEntity) {
            storage[category.id] = category
            flow.value = storage.values.toList()
        }

        override suspend fun updateCategory(category: CategoryEntity) {
            storage[category.id] = category
            flow.value = storage.values.toList()
        }

        override suspend fun deleteCategory(category: CategoryEntity) {
            storage.remove(category.id)
            flow.value = storage.values.toList()
        }

        override suspend fun deleteCategoryById(id: String) {
            storage.remove(id)
            flow.value = storage.values.toList()
        }

        override suspend fun getCategoryCount(): Int = storage.size

        override suspend fun getAllCategories(): List<CategoryEntity> = storage.values.toList()

        override suspend fun deleteAllCategories() {
            storage.clear()
            flow.value = emptyList()
        }
    }

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        type = CategoryType.valueOf(type),
        parentCategoryId = parentCategoryId,
        icon = icon,
        colorHex = colorHex,
        isDefault = isDefault,
        sortOrder = sortOrder
    )
}
