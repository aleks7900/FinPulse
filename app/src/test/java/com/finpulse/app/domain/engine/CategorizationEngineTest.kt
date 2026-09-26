package com.finpulse.app.domain.engine

import com.finpulse.app.domain.model.CategorizationCandidate
import com.finpulse.app.domain.model.CategorizationConfidence
import com.finpulse.app.domain.model.CategorizationRule
import com.finpulse.app.domain.model.CategorizationSource
import com.finpulse.app.domain.model.MatchType
import com.finpulse.app.domain.model.MerchantSignal
import com.finpulse.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorizationEngineTest {

    @Test
    fun testMerchantContainsRule() {
        val rule = CategorizationRule(
            id = "rule-groceries",
            name = "Trader Joe Groceries",
            targetCategoryId = "cat_groceries",
            merchantPattern = "Trader Joe",
            merchantMatchType = MatchType.CONTAINS
        )

        val candidate = CategorizationCandidate(
            merchant = "TRADER JOE'S #521",
            amountMinor = 4500L
        )

        val result = CategorizationEngine.categorize(candidate, listOf(rule))
        assertEquals("cat_groceries", result.categoryId)
        assertEquals(CategorizationConfidence.EXACT_RULE, result.confidence)
        assertEquals(CategorizationSource.USER_RULE, result.source)
        assertEquals("rule-groceries", result.matchedRuleId)
    }

    @Test
    fun testDescriptionContainsRule() {
        val rule = CategorizationRule(
            id = "rule-transport",
            name = "Subway Pass",
            targetCategoryId = "cat_transport",
            descriptionPattern = "Monthly Transit",
            descriptionMatchType = MatchType.CONTAINS
        )

        val candidate = CategorizationCandidate(
            merchant = "MTA Vending",
            description = "Purchased Monthly Transit Pass",
            amountMinor = 13200L
        )

        val result = CategorizationEngine.categorize(candidate, listOf(rule))
        assertEquals("cat_transport", result.categoryId)
        assertEquals("rule-transport", result.matchedRuleId)
    }

    @Test
    fun testAccountPlusMerchantRule() {
        // Business card + Starbucks -> Business Meals
        val businessRule = CategorizationRule(
            id = "rule-biz-coffee",
            name = "Business Coffee",
            targetCategoryId = "cat_biz_meals",
            accountId = "acc_biz",
            merchantPattern = "Starbucks",
            priority = 50
        )

        // General Starbucks rule -> Personal Food
        val personalRule = CategorizationRule(
            id = "rule-personal-coffee",
            name = "Personal Coffee",
            targetCategoryId = "cat_food",
            merchantPattern = "Starbucks",
            priority = 10
        )

        val rules = listOf(businessRule, personalRule)

        // 1. Transaction on Business Account
        val bizCandidate = CategorizationCandidate(
            merchant = "Starbucks Store #12",
            sourceAccountId = "acc_biz",
            amountMinor = 1250L
        )
        val bizResult = CategorizationEngine.categorize(bizCandidate, rules)
        assertEquals("cat_biz_meals", bizResult.categoryId)
        assertEquals("rule-biz-coffee", bizResult.matchedRuleId)

        // 2. Transaction on Personal Checking Account
        val personalCandidate = CategorizationCandidate(
            merchant = "Starbucks Store #12",
            sourceAccountId = "acc_personal",
            amountMinor = 550L
        )
        val personalResult = CategorizationEngine.categorize(personalCandidate, rules)
        assertEquals("cat_food", personalResult.categoryId)
        assertEquals("rule-personal-coffee", personalResult.matchedRuleId)
    }

    @Test
    fun testRulePriorityAndConflictHandling_HigherPriorityWins() {
        val lowPriorityTravel = CategorizationRule(
            id = "rule-travel",
            name = "Uber Travel",
            targetCategoryId = "cat_travel",
            merchantPattern = "Uber",
            priority = 10
        )

        val highPriorityTransport = CategorizationRule(
            id = "rule-transport",
            name = "Uber Local Rides",
            targetCategoryId = "cat_transport",
            merchantPattern = "Uber",
            priority = 100
        )

        val candidate = CategorizationCandidate(
            merchant = "Uber Trip #991",
            amountMinor = 2400L
        )

        // Low priority first in list to verify sorting by priority DESC
        val result = CategorizationEngine.categorize(candidate, listOf(lowPriorityTravel, highPriorityTransport))
        assertEquals("cat_transport", result.categoryId)
        assertEquals("rule-transport", result.matchedRuleId)
    }

    @Test
    fun testRulePriorityAndConflictHandling_EqualPrioritySpecificityTieBreak() {
        // Two rules with identical priority (50)
        // Rule A: Account + Merchant (specificity = 10 + 4 = 14)
        val specificRule = CategorizationRule(
            id = "rule-corp-uber",
            name = "Corp Uber",
            targetCategoryId = "cat_corporate",
            accountId = "acc_corp",
            merchantPattern = "Uber",
            priority = 50
        )

        // Rule B: Merchant only (specificity = 4)
        val genericRule = CategorizationRule(
            id = "rule-generic-uber",
            name = "General Uber",
            targetCategoryId = "cat_transport",
            merchantPattern = "Uber",
            priority = 50
        )

        val candidate = CategorizationCandidate(
            merchant = "Uber Trip",
            sourceAccountId = "acc_corp"
        )

        val result = CategorizationEngine.categorize(candidate, listOf(genericRule, specificRule))
        assertEquals("cat_corporate", result.categoryId)
        assertEquals("rule-corp-uber", result.matchedRuleId)
    }

    @Test
    fun testDeterministicCorrectionSignalLookup() {
        // No user rules configured
        val signals = listOf(
            MerchantSignal(
                normalizedMerchant = "corner bakery cafe",
                categoryId = "cat_food",
                useCount = 5
            )
        )

        val candidate = CategorizationCandidate(
            merchant = "SQ *CORNER BAKERY CAFE #12",
            amountMinor = 1400L
        )

        val result = CategorizationEngine.categorize(
            candidate = candidate,
            rules = emptyList(),
            signals = signals
        )

        assertEquals("cat_food", result.categoryId)
        assertEquals(CategorizationConfidence.HIGH, result.confidence)
        assertEquals(CategorizationSource.MERCHANT_CORRECTION_SIGNAL, result.source)
    }

    @Test
    fun testDescriptionKeywordFallback() {
        val candidate = CategorizationCandidate(
            merchant = "UNKNOWN COMPANY ACH",
            description = "Direct Deposit Bi-Weekly Payroll",
            amountMinor = 250000L,
            type = TransactionType.INCOME
        )

        val result = CategorizationEngine.categorize(candidate, emptyList())
        assertEquals("cat_salary", result.categoryId)
        assertEquals(CategorizationConfidence.MEDIUM, result.confidence)
        assertEquals(CategorizationSource.DESCRIPTION_KEYWORD, result.source)
    }

    @Test
    fun testUncategorizedFallbackWhenNoMatchFound() {
        val candidate = CategorizationCandidate(
            merchant = "Random Store 9871625",
            description = "Misc payment"
        )

        val result = CategorizationEngine.categorize(candidate, emptyList())
        assertNull(result.categoryId)
        assertEquals(CategorizationConfidence.NONE, result.confidence)
        assertEquals(CategorizationSource.UNCATEGORIZED, result.source)
    }
}
