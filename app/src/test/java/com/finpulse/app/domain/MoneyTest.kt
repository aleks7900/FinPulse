package com.finpulse.app.domain

import com.finpulse.app.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun testMoneyAdditionAndSubtraction() {
        val m1 = Money(1550L, "USD") // $15.50
        val m2 = Money(450L, "USD")  // $4.50

        val sum = m1 + m2
        assertEquals(2000L, sum.amountMinor)
        assertEquals("USD", sum.currencyCode)
        assertEquals(BigDecimal("20.00"), sum.amountBigDecimal)

        val diff = m1 - m2
        assertEquals(1100L, diff.amountMinor)
        assertEquals(BigDecimal("11.00"), diff.amountBigDecimal)
    }

    @Test
    fun testCurrencyMismatchThrowsException() {
        val usd = Money(1000L, "USD")
        val eur = Money(1000L, "EUR")

        try {
            val result = usd + eur
            fail("Expected exception when adding USD and EUR, got $result")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Currency mismatch") == true)
        }
    }

    @Test
    fun testMoneyMultiplicationAndDivision() {
        val m = Money(10000L, "USD") // $100.00
        val half = m * 0.5
        assertEquals(5000L, half.amountMinor)

        val third = m / 3.0
        // 10000 / 3 = 3333.333 -> rounds to 3333
        assertEquals(3333L, third.amountMinor)
    }

    @Test
    fun testRatioCalculation() {
        val m1 = Money(2500L, "USD")
        val m2 = Money(10000L, "USD")
        assertEquals(0.25, m1.ratio(m2), 0.0001)
    }

    @Test
    fun testNegativeAndZeroChecks() {
        val zero = Money.zero("USD")
        assertTrue(zero.isZero)

        val neg = Money(-500L, "USD")
        assertTrue(neg.isNegative)
        assertEquals(500L, neg.absolute().amountMinor)
    }
}
