package com.finpulse.app.domain.model

data class SavedFilter(
    val id: String,
    val name: String,
    val params: TransactionFilterParams,
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
