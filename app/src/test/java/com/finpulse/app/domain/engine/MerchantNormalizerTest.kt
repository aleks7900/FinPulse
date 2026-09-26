package com.finpulse.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {

    @Test
    fun testPrefixStripping() {
        assertEquals("Coffee Shop", MerchantNormalizer.normalize("SQ *Coffee Shop"))
        assertEquals("Sweet Bakery", MerchantNormalizer.normalize("TST* Sweet Bakery"))
        assertEquals("Ebay Inc", MerchantNormalizer.normalize("PAYPAL *Ebay Inc"))
        assertEquals("SaaS Provider", MerchantNormalizer.normalize("STRIPE *SaaS Provider"))
        assertEquals("Clover Cafe", MerchantNormalizer.normalize("CLOVER *Clover Cafe"))
        assertEquals("Target", MerchantNormalizer.normalize("CHECKCARD Target"))
        assertEquals("Burger King", MerchantNormalizer.normalize("POS PURCHASE Burger King"))
    }

    @Test
    fun testStoreAndLocationStripping() {
        assertEquals("Target", MerchantNormalizer.normalize("Target #1452"))
        assertEquals("Walmart", MerchantNormalizer.normalize("Walmart Store 0451"))
        assertEquals("Shell Oil", MerchantNormalizer.normalize("Shell Oil Loc 9912"))
        assertEquals("Best Buy", MerchantNormalizer.normalize("Best Buy No. 881"))
    }

    @Test
    fun testDomainAndLocationSuffixStripping() {
        assertEquals("Amazon", MerchantNormalizer.normalize("Amazon.com"))
        assertEquals("Netflix", MerchantNormalizer.normalize("Netflix.com"))
        assertEquals("Starbucks", MerchantNormalizer.normalize("Starbucks CA US"))
    }

    @Test
    fun testCanonicalAliasResolution() {
        // Amazon aliases
        assertEquals("Amazon", MerchantNormalizer.resolveCanonicalName("AMZN"))
        assertEquals("Amazon", MerchantNormalizer.resolveCanonicalName("Amazon Prime"))
        assertEquals("Amazon", MerchantNormalizer.resolveCanonicalName("AMZN MKTP US*"))

        // Walmart aliases
        assertEquals("Walmart", MerchantNormalizer.resolveCanonicalName("WMT"))
        assertEquals("Walmart", MerchantNormalizer.resolveCanonicalName("Wal-Mart"))
        assertEquals("Walmart", MerchantNormalizer.resolveCanonicalName("Walmart Supercenter #100"))

        // Starbucks
        assertEquals("Starbucks", MerchantNormalizer.resolveCanonicalName("SBUX"))
        assertEquals("Starbucks", MerchantNormalizer.resolveCanonicalName("Starbucks Coffee #42"))

        // McDonald's
        assertEquals("McDonald's", MerchantNormalizer.resolveCanonicalName("MCDONALDS"))
        assertEquals("McDonald's", MerchantNormalizer.resolveCanonicalName("Mc Donalds"))

        // Uber & Uber Eats
        assertEquals("Uber", MerchantNormalizer.resolveCanonicalName("UBER TRIP"))
        assertEquals("Uber Eats", MerchantNormalizer.resolveCanonicalName("UBER *EATS"))

        // Apple & Netflix
        assertEquals("Apple", MerchantNormalizer.resolveCanonicalName("APL* Apple Online Store"))
        assertEquals("Apple", MerchantNormalizer.resolveCanonicalName("iTunes.com"))
        assertEquals("Netflix", MerchantNormalizer.resolveCanonicalName("NFLX"))
    }

    @Test
    fun testCaseDifferencesAndLookupKeys() {
        assertEquals("starbucks", MerchantNormalizer.toLookupKey("  SBUX STORE #123  "))
        assertEquals("starbucks", MerchantNormalizer.toLookupKey("Starbucks"))
        assertEquals("starbucks", MerchantNormalizer.toLookupKey("sTaRbUcKs CoFfEe"))
        assertEquals("amazon", MerchantNormalizer.toLookupKey("AMZN MKTP US*"))
        assertEquals("walmart", MerchantNormalizer.toLookupKey("wAl-MaRt #991"))
    }
}
