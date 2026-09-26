package com.finpulse.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val symbol: String,
    val assetClass: String, // STOCK, ETF, CRYPTO, BOND, REAL_ESTATE, etc.
    val quantity: Double,
    val purchasePriceMinor: Long,
    val currentPriceMinor: Long,
    val currencyCode: String = "USD",
    val lastUpdated: Long = System.currentTimeMillis(),
    val notes: String? = null
)
