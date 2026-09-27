package com.finpulse.app.presentation.widget

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WidgetPreferencesTest {

    private val context: Context = mockk(relaxed = true)
    private val sharedPrefs: SharedPreferences = mockk(relaxed = true)
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)

    @Before
    fun setup() {
        every { context.getSharedPreferences("finpulse_widget_prefs", Context.MODE_PRIVATE) } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
    }

    @Test
    fun testGetWidgetConfig_DefaultsWhenEmpty() {
        every { sharedPrefs.getString("widget_type_1", any()) } returns "DASHBOARD"
        every { sharedPrefs.getString("widget_privacy_1", any()) } returns "FOLLOW_APP"
        every { sharedPrefs.getString("widget_account_1", null) } returns null

        val config = WidgetPreferences.getWidgetConfig(context, 1)

        assertEquals(1, config.appWidgetId)
        assertEquals(WidgetDisplayType.DASHBOARD, config.displayType)
        assertEquals(WidgetPrivacySetting.FOLLOW_APP, config.privacySetting)
        assertEquals(null, config.accountId)
    }

    @Test
    fun testSaveWidgetConfig_PersistsValues() {
        val config = WidgetConfig(
            appWidgetId = 42,
            displayType = WidgetDisplayType.SPENDING,
            privacySetting = WidgetPrivacySetting.ALWAYS_HIDE,
            accountId = "acc_123"
        )

        WidgetPreferences.saveWidgetConfig(context, config)

        verify { editor.putString("widget_type_42", "SPENDING") }
        verify { editor.putString("widget_privacy_42", "ALWAYS_HIDE") }
        verify { editor.putString("widget_account_42", "acc_123") }
        verify { editor.apply() }
    }

    @Test
    fun testToggleWidgetPrivacy_CyclesCorrectly() {
        every { sharedPrefs.getString("widget_privacy_1", any()) } returns "FOLLOW_APP"
        val next1 = WidgetPreferences.toggleWidgetPrivacy(context, 1)
        assertEquals(WidgetPrivacySetting.ALWAYS_HIDE, next1)

        every { sharedPrefs.getString("widget_privacy_1", any()) } returns "ALWAYS_HIDE"
        val next2 = WidgetPreferences.toggleWidgetPrivacy(context, 1)
        assertEquals(WidgetPrivacySetting.ALWAYS_SHOW, next2)

        every { sharedPrefs.getString("widget_privacy_1", any()) } returns "ALWAYS_SHOW"
        val next3 = WidgetPreferences.toggleWidgetPrivacy(context, 1)
        assertEquals(WidgetPrivacySetting.FOLLOW_APP, next3)
    }

    @Test
    fun testDeleteWidgetConfig_RemovesKeys() {
        WidgetPreferences.deleteWidgetConfig(context, 99)

        verify { editor.remove("widget_type_99") }
        verify { editor.remove("widget_privacy_99") }
        verify { editor.remove("widget_account_99") }
        verify { editor.apply() }
    }
}
