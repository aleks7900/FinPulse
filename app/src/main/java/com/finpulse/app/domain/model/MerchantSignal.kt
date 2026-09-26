package com.finpulse.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Local deterministic signal tracking merchant corrections and confirmed categorization history.
 */
@Serializable
data class MerchantSignal(
    val normalizedMerchant: String,
    val categoryId: String,
    val useCount: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis()
)
