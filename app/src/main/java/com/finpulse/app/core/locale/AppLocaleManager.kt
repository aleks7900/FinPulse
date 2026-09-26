package com.finpulse.app.core.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.finpulse.app.R

enum class AppLanguage(
    val code: String,        // Language tag (BCP-47) or "SYSTEM"
    val displayName: String,  // Autonym (native display name)
    val localizedNameRes: Int
) {
    SYSTEM("SYSTEM", "System Default", R.string.lang_system),
    ENGLISH("en", "English", R.string.lang_en),
    RUSSIAN("ru", "Русский", R.string.lang_ru),
    SPANISH("es", "Español", R.string.lang_es),
    PORTUGUESE_BRAZIL("pt-BR", "Português (Brasil)", R.string.lang_pt_br),
    GERMAN("de", "Deutsch", R.string.lang_de),
    FRENCH("fr", "Français", R.string.lang_fr),
    ITALIAN("it", "Italiano", R.string.lang_it),
    POLISH("pl", "Polski", R.string.lang_pl),
    TURKISH("tr", "Türkçe", R.string.lang_tr),
    JAPANESE("ja", "日本語", R.string.lang_ja),
    KOREAN("ko", "한국어", R.string.lang_ko),
    SIMPLIFIED_CHINESE("zh-CN", "简体中文", R.string.lang_zh_cn);

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
    }
}

object AppLocaleManager {

    fun setLocale(context: Context, language: AppLanguage) {
        val localeList = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.code)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getCurrentLanguage(context: Context, savedCode: String): AppLanguage {
        if (savedCode != "SYSTEM") {
            return AppLanguage.fromCode(savedCode)
        }
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (appLocales.isEmpty) {
            return AppLanguage.SYSTEM
        }
        val tag = appLocales[0]?.toLanguageTag() ?: return AppLanguage.SYSTEM
        return AppLanguage.fromCode(tag)
    }
}
