package md.alexlab.finpulse.core.reporting

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import md.alexlab.finpulse.BuildConfig

/**
 * Production implementation of [ErrorReporter] backed by Firebase Crashlytics.
 *
 * Guarantees:
 * 1. Safe exception recording with automated PII & sensitive financial redaction.
 * 2. Fail-safe operation: any internal reporting error is silently swallowed to prevent app crashes.
 * 3. Configurable by build type (enabled in Release, customizable in Debug).
 * 4. Pseudonymous user ID tracking with immediate session clearing on logout.
 */
class FirebaseCrashlyticsErrorReporter(
    private val crashlyticsProvider: () -> FirebaseCrashlytics = { FirebaseCrashlytics.getInstance() },
    private val isDebug: Boolean = BuildConfig.DEBUG,
    initialCollectionEnabled: Boolean = !BuildConfig.DEBUG
) : ErrorReporter {

    private val tag = "FinPulse:ErrorReporter"
    private var _isCollectionEnabled: Boolean = initialCollectionEnabled

    override val isCollectionEnabled: Boolean
        get() = _isCollectionEnabled

    init {
        try {
            val crashlytics = crashlyticsProvider()
            crashlytics.setCrashlyticsCollectionEnabled(_isCollectionEnabled)
            crashlytics.setCustomKey(DiagnosticContext.KEY_BUILD_TYPE, if (isDebug) "debug" else "release")
            crashlytics.setCustomKey(DiagnosticContext.KEY_APP_VERSION, BuildConfig.VERSION_NAME)
        } catch (t: Throwable) {
            if (isDebug) {
                Log.w(tag, "Failed to initialize FirebaseCrashlytics settings: ${t.message}")
            }
        }
    }

    override fun recordException(
        throwable: Throwable,
        context: Map<String, String>
    ) {
        try {
            val safeContext = DataSanitizer.sanitizeContext(context)
            val safeThrowable = DataSanitizer.sanitizeThrowable(throwable)

            if (isDebug) {
                Log.e(tag, "Non-Fatal Exception captured [context=$safeContext]: ${safeThrowable.message}", safeThrowable)
            }

            if (!_isCollectionEnabled) {
                return
            }

            val crashlytics = crashlyticsProvider()
            for ((key, value) in safeContext) {
                crashlytics.setCustomKey(key, value)
            }
            crashlytics.recordException(safeThrowable)
        } catch (t: Throwable) {
            // Reporting must never cause an application failure
            if (isDebug) {
                Log.w(tag, "Failed to record non-fatal exception to Crashlytics: ${t.message}")
            }
        }
    }

    override fun log(message: String) {
        try {
            val safeMessage = DataSanitizer.sanitizeValue(message)
            if (isDebug) {
                Log.d(tag, "Breadcrumb: $safeMessage")
            }

            if (_isCollectionEnabled) {
                crashlyticsProvider().log(safeMessage)
            }
        } catch (t: Throwable) {
            if (isDebug) {
                Log.w(tag, "Failed to log message to Crashlytics: ${t.message}")
            }
        }
    }

    override fun setCustomKey(key: String, value: String) {
        try {
            if (!DataSanitizer.isKeyAllowed(key)) {
                return
            }
            val safeValue = DataSanitizer.sanitizeValue(value)
            if (_isCollectionEnabled) {
                crashlyticsProvider().setCustomKey(key, safeValue)
            }
        } catch (t: Throwable) {
            if (isDebug) {
                Log.w(tag, "Failed to set custom key: ${t.message}")
            }
        }
    }

    override fun setUserContext(userId: String?) {
        try {
            val anonId = DataSanitizer.anonymizeUserId(userId) ?: ""
            crashlyticsProvider().setUserId(anonId)
        } catch (t: Throwable) {
            if (isDebug) {
                Log.w(tag, "Failed to set user context: ${t.message}")
            }
        }
    }

    override fun clearUserContext() {
        try {
            crashlyticsProvider().setUserId("")
        } catch (t: Throwable) {
            if (isDebug) {
                Log.w(tag, "Failed to clear user context: ${t.message}")
            }
        }
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        try {
            _isCollectionEnabled = enabled
            crashlyticsProvider().setCrashlyticsCollectionEnabled(enabled)
            if (isDebug) {
                Log.i(tag, "Crashlytics collection toggled to: $enabled")
            }
        } catch (t: Throwable) {
            if (isDebug) {
                Log.w(tag, "Failed to set collection enabled: ${t.message}")
            }
        }
    }
}
