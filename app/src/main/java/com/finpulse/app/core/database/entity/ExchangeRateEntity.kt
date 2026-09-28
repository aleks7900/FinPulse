package com.finpulse.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["fromCurrency", "toCurrency"],
    indices = [
        Index("fromCurrency"),
        Index("toCurrency"),
        Index("timestamp")
    ]
)
data class ExchangeRateEntity(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isManual: Boolean = false
)
