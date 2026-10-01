package md.alexlab.finpulse.domain.model

import kotlinx.serialization.Serializable

enum class CategoryType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income")
}

@Serializable
data class Category(
    val id: String,
    val name: String,
    val type: CategoryType,
    val parentCategoryId: String? = null,
    val icon: String = "category",
    val colorHex: Long = 0xFF607D8B,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
