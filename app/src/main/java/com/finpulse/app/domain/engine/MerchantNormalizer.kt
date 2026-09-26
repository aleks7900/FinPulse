package com.finpulse.app.domain.engine

object MerchantNormalizer {

    private val PREFIX_PATTERNS = listOf(
        Regex("^(?:SQ\\s*\\*|SQUARE\\s*\\*|TST\\s*\\*|PAYPAL\\s*\\*|SP\\s*\\*|STRIPE\\s*\\*|CLOVER\\s*\\*|GOOGLE\\s*\\*|APPLE\\.COM/BILL\\s*|AMZN\\s*MKTP\\s*(?:US\\s*)?\\*|CHECKCARD\\s*|POS\\s*DEBIT\\s*|POS\\s*PURCHASE\\s*|DEBIT\\s*CARD\\s*PURCHASE\\s*)", RegexOption.IGNORE_CASE),
        Regex("^(?:FSP\\s*\\*|APL\\s*\\*|VZWRLSS\\s*\\*|MSFT\\s*\\*)", RegexOption.IGNORE_CASE)
    )

    private val STORE_PATTERNS = listOf(
        Regex("#\\s*\\d+", RegexOption.IGNORE_CASE),
        Regex("\\bSTORE\\s*#?\\s*\\d+\\b", RegexOption.IGNORE_CASE),
        Regex("\\bLOC(?:ATION)?\\s*#?\\s*\\d+\\b", RegexOption.IGNORE_CASE),
        Regex("\\bNO\\.\\s*\\d+\\b", RegexOption.IGNORE_CASE)
    )

    private val LOCATION_TRAILING_PATTERN = Regex("\\b[A-Z]{2}\\s+(?:US|USA|CA|UK)$", RegexOption.IGNORE_CASE)
    private val DOMAIN_SUFFIX_PATTERN = Regex("\\.(?:COM|NET|ORG|IO|CO)\\b", RegexOption.IGNORE_CASE)
    private val PHONE_PATTERN = Regex("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b")

    private val DEFAULT_ALIASES = mapOf(
        "amzn" to "Amazon",
        "amazon" to "Amazon",
        "amazon prime" to "Amazon",
        "amzn mktp" to "Amazon",
        "amzn mktp us" to "Amazon",
        "wmt" to "Walmart",
        "wal-mart" to "Walmart",
        "walmart" to "Walmart",
        "walmart supercenter" to "Walmart",
        "sbux" to "Starbucks",
        "sbux store" to "Starbucks",
        "starbucks" to "Starbucks",
        "starbucks coffee" to "Starbucks",
        "starbucks store" to "Starbucks",
        "mcdonalds" to "McDonald's",
        "mc donalds" to "McDonald's",
        "uber" to "Uber",
        "uber trip" to "Uber",
        "uber eats" to "Uber Eats",
        "nflx" to "Netflix",
        "netflix" to "Netflix",
        "tgt" to "Target",
        "target" to "Target",
        "apple" to "Apple",
        "apple online" to "Apple",
        "apple store" to "Apple",
        "apple online store" to "Apple",
        "itunes" to "Apple",
        "shell" to "Shell",
        "shell oil" to "Shell",
        "cvs" to "CVS",
        "cvs pharmacy" to "CVS",
        "trader joe" to "Trader Joe's",
        "trader joes" to "Trader Joe's",
        "whole foods" to "Whole Foods",
        "wholefds" to "Whole Foods",
        "costco" to "Costco",
        "costco wholesale" to "Costco"
    )

    /**
     * Cleans and normalizes merchant string, stripping bank prefixes, store numbers, and extraneous tokens.
     */
    fun normalize(rawMerchant: String?): String {
        if (rawMerchant.isNullOrBlank()) return ""

        var cleaned = rawMerchant.trim()

        // Check if raw cleaned string directly matches an alias
        val rawLower = cleaned.lowercase().replace("*", "").trim()
        if (DEFAULT_ALIASES.containsKey(rawLower)) {
            return DEFAULT_ALIASES[rawLower]!!
        }

        // 1. Strip common processor prefixes only if something remains
        for (pattern in PREFIX_PATTERNS) {
            val candidate = pattern.replace(cleaned, "").trim()
            if (candidate.isNotBlank()) {
                cleaned = candidate
            }
        }

        // 2. Strip store and location patterns
        for (pattern in STORE_PATTERNS) {
            cleaned = pattern.replace(cleaned, "")
        }
        cleaned = Regex("\\bSTORE\\b", RegexOption.IGNORE_CASE).replace(cleaned, "")

        cleaned = LOCATION_TRAILING_PATTERN.replace(cleaned, "")
        cleaned = PHONE_PATTERN.replace(cleaned, "")
        cleaned = DOMAIN_SUFFIX_PATTERN.replace(cleaned, "")

        // 3. Remove excess punctuation and collapse whitespace
        cleaned = cleaned.replace(Regex("[^a-zA-Z0-9&'\\s-]"), " ")
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        return if (cleaned.isNotBlank()) cleaned else rawMerchant.trim()
    }

    /**
     * Resolves merchant aliases to standard canonical names (e.g. "AMZN" -> "Amazon").
     */
    fun resolveCanonicalName(rawOrNormalized: String?): String {
        val normalized = normalize(rawOrNormalized)
        if (normalized.isBlank()) return ""

        val lower = normalized.lowercase().replace("*", "").trim()
        return DEFAULT_ALIASES[lower] ?: normalized
    }

    /**
     * Standardized lowercase lookup key for caching signals.
     */
    fun toLookupKey(merchant: String?): String {
        return resolveCanonicalName(merchant).lowercase().trim()
    }
}
