package com.finpulse.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CurrencyConfigTest {

    @Test
    fun `fraction digits and multipliers are correct for ISO-4217 standard`() {
        // Zero-decimal currencies
        assertEquals(0, CurrencyConfig.decimalsFor("JPY"))
        assertEquals(1L, CurrencyConfig.minorUnitsPerMajor("JPY"))
        assertEquals(0, CurrencyConfig.decimalsFor("KRW"))
        assertEquals(1L, CurrencyConfig.minorUnitsPerMajor("KRW"))

        // Standard two-decimal currencies
        assertEquals(2, CurrencyConfig.decimalsFor("USD"))
        assertEquals(100L, CurrencyConfig.minorUnitsPerMajor("USD"))
        assertEquals(2, CurrencyConfig.decimalsFor("EUR"))
        assertEquals(100L, CurrencyConfig.minorUnitsPerMajor("EUR"))
        assertEquals(2, CurrencyConfig.decimalsFor("GBP"))
        assertEquals(100L, CurrencyConfig.minorUnitsPerMajor("GBP"))

        // Three-decimal currencies
        assertEquals(3, CurrencyConfig.decimalsFor("BHD"))
        assertEquals(1000L, CurrencyConfig.minorUnitsPerMajor("BHD"))
        assertEquals(3, CurrencyConfig.decimalsFor("KWD"))
        assertEquals(1000L, CurrencyConfig.minorUnitsPerMajor("KWD"))
        assertEquals(3, CurrencyConfig.decimalsFor("OMR"))
        assertEquals(1000L, CurrencyConfig.minorUnitsPerMajor("OMR"))
    }

    @Test
    fun `conversion between major and minor units applies Half-Even bankers rounding`() {
        // JPY (0 decimals)
        assertEquals(150L, CurrencyConfig.toMinor(BigDecimal("150.4"), "JPY"))
        assertEquals(150L, CurrencyConfig.toMinor(BigDecimal("150.5"), "JPY")) // rounds to nearest even (150)
        assertEquals(152L, CurrencyConfig.toMinor(BigDecimal("151.5"), "JPY")) // rounds to nearest even (152)

        // USD (2 decimals)
        assertEquals(1050L, CurrencyConfig.toMinor(BigDecimal("10.50"), "USD"))
        assertEquals(1050L, CurrencyConfig.toMinor(BigDecimal("10.504"), "USD"))
        assertEquals(1050L, CurrencyConfig.toMinor(BigDecimal("10.505"), "USD")) // rounds to even 0
        assertEquals(1052L, CurrencyConfig.toMinor(BigDecimal("10.515"), "USD")) // rounds to even 2

        // BHD (3 decimals)
        assertEquals(1234L, CurrencyConfig.toMinor(BigDecimal("1.234"), "BHD"))
        assertEquals(1234L, CurrencyConfig.toMinor(BigDecimal("1.2344"), "BHD"))
        assertEquals(1234L, CurrencyConfig.toMinor(BigDecimal("1.2345"), "BHD")) // rounds to even 4
        assertEquals(1236L, CurrencyConfig.toMinor(BigDecimal("1.2355"), "BHD")) // rounds to even 6
    }

    @Test
    fun `toMajor converts minor units back to BigDecimal preserving scale`() {
        assertEquals(BigDecimal("1500"), CurrencyConfig.toMajor(1500L, "JPY"))
        assertEquals(BigDecimal("15.00"), CurrencyConfig.toMajor(1500L, "USD"))
        assertEquals(BigDecimal("1.500"), CurrencyConfig.toMajor(1500L, "BHD"))
    }

    @Test
    fun `Money integration works seamlessly with CurrencyConfig`() {
        val usdMoney = Money.fromMajor(BigDecimal("25.50"), "USD")
        assertEquals(2550L, usdMoney.amountMinor)
        assertEquals("USD", usdMoney.currencyCode)
        assertEquals(BigDecimal("25.50"), usdMoney.amountBigDecimal)

        val jpyMoney = Money.fromMajor(BigDecimal("3000"), "JPY")
        assertEquals(3000L, jpyMoney.amountMinor)
        assertEquals("JPY", jpyMoney.currencyCode)
        assertEquals(BigDecimal("3000"), jpyMoney.amountBigDecimal)

        val bhdMoney = Money.fromMajor(BigDecimal("7.125"), "BHD")
        assertEquals(7125L, bhdMoney.amountMinor)
        assertEquals("BHD", bhdMoney.currencyCode)
        assertEquals(BigDecimal("7.125"), bhdMoney.amountBigDecimal)
    }

    @Test
    fun `Money strictly prevents adding or subtracting across different currencies`() {
        val usd = Money(1000L, "USD")
        val eur = Money(1000L, "EUR")

        assertThrows(IllegalArgumentException::class.java) {
            usd + eur
        }

        assertThrows(IllegalArgumentException::class.java) {
            usd - eur
        }
    }
}
