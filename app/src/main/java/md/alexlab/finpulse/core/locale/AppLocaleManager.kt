package md.alexlab.finpulse.core.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import md.alexlab.finpulse.R
import java.util.Locale

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
            entries.find { it.code.equals(code, ignoreCase = true) }
                ?: when {
                    code.startsWith("zh", ignoreCase = true) -> SIMPLIFIED_CHINESE
                    code.startsWith("pt", ignoreCase = true) -> PORTUGUESE_BRAZIL
                    else -> SYSTEM
                }
    }
}

object AppLocaleManager {

    private const val PREFS_LOCALE = "finpulse_locale_prefs"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"

    fun getSavedLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_LOCALE, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_LANGUAGE, "SYSTEM") ?: "SYSTEM"
    }

    fun saveLanguageCode(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS_LOCALE, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, code).commit()
    }

    fun getTargetLocale(language: AppLanguage): Locale {
        return if (language == AppLanguage.SYSTEM) {
            val sysLocales = Resources.getSystem().configuration.locales
            if (!sysLocales.isEmpty) sysLocales[0] else Locale.getDefault()
        } else {
            Locale.forLanguageTag(language.code)
        }
    }

    fun getLocalizedContext(baseContext: Context, language: AppLanguage): Context {
        val targetLocale = getTargetLocale(language)
        Locale.setDefault(targetLocale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(targetLocale)
        val localeList = LocaleList(targetLocale)
        LocaleList.setDefault(localeList)
        config.setLocales(localeList)
        config.setLayoutDirection(targetLocale)
        val configContext = baseContext.createConfigurationContext(config)
        val activity = (baseContext as? Activity) ?: baseContext.findActivity()
        return LocalizedContext(configContext, activity)
    }

    fun setLocale(context: Context, language: AppLanguage) {
        saveLanguageCode(context, language.code)

        val targetLocale = getTargetLocale(language)
        Locale.setDefault(targetLocale)

        // 1. Android 13+ (API 33+) native Per-App Language preference
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val appLocales = if (language == AppLanguage.SYSTEM) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(language.code)
            }
            try {
                localeManager?.applicationLocales = appLocales
            } catch (_: Exception) {
                // Ignore if framework security manager blocks
            }
        }

        // 2. AppCompat delegate locales
        val localeListCompat = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.code)
        }
        try {
            AppCompatDelegate.setApplicationLocales(localeListCompat)
        } catch (_: Exception) {
            // Ignore if AppCompat delegates unavailable
        }

        // 3. Update active resources configuration for current process
        val config = Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        val localeList = LocaleList(targetLocale)
        config.setLocales(localeList)
        config.setLayoutDirection(targetLocale)

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        val appContext = context.applicationContext
        if (appContext != null && appContext !== context) {
            @Suppress("DEPRECATION")
            appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)
        }
    }

    fun getCurrentLanguage(context: Context, savedCode: String): AppLanguage {
        if (savedCode != "SYSTEM") {
            return AppLanguage.fromCode(savedCode)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val appLocales = localeManager?.applicationLocales
            if (appLocales != null && !appLocales.isEmpty) {
                val tag = appLocales[0].toLanguageTag()
                return AppLanguage.fromCode(tag)
            }
        }
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val tag = appLocales[0]?.toLanguageTag()
            if (tag != null) return AppLanguage.fromCode(tag)
        }
        return AppLanguage.SYSTEM
    }
}

class LocalizedContext(
    base: Context,
    val activity: Activity? = null
) : ContextWrapper(base) {
    override fun getSystemService(name: String): Any? {
        return activity?.getSystemService(name) ?: super.getSystemService(name)
    }

    override fun getSystemServiceName(serviceClass: Class<*>): String? {
        return activity?.getSystemServiceName(serviceClass) ?: super.getSystemServiceName(serviceClass)
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is LocalizedContext -> {
        val act = this.activity
        if (act != null) act else baseContext.findActivity()
    }
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Context.findFragmentActivity(): FragmentActivity? = findActivity() as? FragmentActivity
