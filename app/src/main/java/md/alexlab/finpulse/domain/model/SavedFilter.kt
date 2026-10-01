package md.alexlab.finpulse.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedFilter(
    val id: String,
    val name: String,
    val params: TransactionFilterParams,
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
