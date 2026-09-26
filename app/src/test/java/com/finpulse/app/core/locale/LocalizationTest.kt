package com.finpulse.app.core.locale

import com.finpulse.app.core.model.TimePeriod
import com.finpulse.app.core.ui.getStringRes
import com.finpulse.app.core.ui.getSubtitleRes
import com.finpulse.app.core.ui.getTitleRes
import com.finpulse.app.domain.engine.DebtStrategy
import com.finpulse.app.domain.model.AccountType
import com.finpulse.app.domain.model.AmountMode
import com.finpulse.app.domain.model.AssetClass
import com.finpulse.app.domain.model.BudgetPeriod
import com.finpulse.app.domain.model.CategoryType
import com.finpulse.app.domain.model.CustomIntervalUnit
import com.finpulse.app.domain.model.DebtType
import com.finpulse.app.domain.model.DuplicateStatus
import com.finpulse.app.domain.model.OccurrenceStatus
import com.finpulse.app.domain.model.PaymentFrequency
import com.finpulse.app.domain.model.TransactionType
import com.finpulse.app.presentation.recurring.RecurringTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationTest {

    @Test
    fun testAppLanguageEnumCodes() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("SYSTEM"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromCode("ru"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromCode("es"))
        assertEquals(AppLanguage.PORTUGUESE_BRAZIL, AppLanguage.fromCode("pt-BR"))
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromCode("de"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromCode("fr"))
        assertEquals(AppLanguage.ITALIAN, AppLanguage.fromCode("it"))
        assertEquals(AppLanguage.POLISH, AppLanguage.fromCode("pl"))
        assertEquals(AppLanguage.TURKISH, AppLanguage.fromCode("tr"))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromCode("ja"))
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromCode("ko"))
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromCode("zh-CN"))

        // Case insensitivity & fallback
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromCode("ZH-cn"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("non_existent_code"))
    }

    @Test
    fun testAllEnumExtensionsHaveValidStringResources() {
        TransactionType.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        AccountType.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        BudgetPeriod.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        PaymentFrequency.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        CustomIntervalUnit.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        OccurrenceStatus.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        AssetClass.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        DebtType.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        DuplicateStatus.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        AmountMode.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        TimePeriod.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        DebtStrategy.entries.forEach {
            assertNotEquals(0, it.getTitleRes())
            assertNotEquals(0, it.getSubtitleRes())
        }
        CategoryType.entries.forEach { assertNotEquals(0, it.getStringRes()) }
        RecurringTab.entries.forEach { assertNotEquals(0, it.getStringRes()) }
    }

    @Test
    fun testAllTwelveLocaleResourceFilesExistAndHaveKeyParity() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val resDir = File(projectDir, "src/main/res").let {
            if (it.exists()) it else File(projectDir, "app/src/main/res")
        }

        assertTrue("res directory must exist at ${resDir.absolutePath}", resDir.exists())

        val localeDirs = listOf(
            "values",
            "values-ru",
            "values-es",
            "values-pt-rBR",
            "values-de",
            "values-fr",
            "values-it",
            "values-pl",
            "values-tr",
            "values-ja",
            "values-ko",
            "values-zh-rCN"
        )

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()

        // 1. Parse base strings.xml
        val defaultStringsFile = File(resDir, "values/strings.xml")
        assertTrue("Base strings.xml must exist", defaultStringsFile.exists())
        val defaultDoc = builder.parse(defaultStringsFile)
        val defaultStringNodes = defaultDoc.getElementsByTagName("string")
        val defaultPluralNodes = defaultDoc.getElementsByTagName("plurals")

        val baseKeys = mutableSetOf<String>()
        for (i in 0 until defaultStringNodes.length) {
            val element = defaultStringNodes.item(i) as Element
            baseKeys.add(element.getAttribute("name"))
        }

        val basePlurals = mutableSetOf<String>()
        for (i in 0 until defaultPluralNodes.length) {
            val element = defaultPluralNodes.item(i) as Element
            basePlurals.add(element.getAttribute("name"))
        }

        assertTrue("Base keys should be > 100", baseKeys.size > 100)
        assertTrue("Base plurals should be > 5", basePlurals.size >= 8)

        // 2. Verify all other 11 locales
        for (dirName in localeDirs) {
            val localeFile = File(resDir, "$dirName/strings.xml")
            assertTrue("Locale file $dirName/strings.xml must exist", localeFile.exists())

            val doc = builder.parse(localeFile)
            val stringNodes = doc.getElementsByTagName("string")
            val pluralNodes = doc.getElementsByTagName("plurals")

            val localeKeys = mutableSetOf<String>()
            for (i in 0 until stringNodes.length) {
                val element = stringNodes.item(i) as Element
                localeKeys.add(element.getAttribute("name"))
            }

            val localePlurals = mutableSetOf<String>()
            for (i in 0 until pluralNodes.length) {
                val element = pluralNodes.item(i) as Element
                localePlurals.add(element.getAttribute("name"))
            }

            // Check missing keys
            val missingKeys = baseKeys - localeKeys
            assertTrue(
                "Locale $dirName is missing keys: $missingKeys",
                missingKeys.isEmpty()
            )

            // Check missing plurals
            val missingPlurals = basePlurals - localePlurals
            assertTrue(
                "Locale $dirName is missing plurals: $missingPlurals",
                missingPlurals.isEmpty()
            )
        }
    }
}
