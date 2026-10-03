package md.alexlab.finpulse.core.reporting

/**
 * Production-ready abstraction for application crash and non-fatal error reporting.
 *
 * Decouples domain and feature modules from any third-party error reporting SDK (Firebase Crashlytics, etc.).
 * Automatically guarantees centralized data sanitization, privacy protection, and fail-safe execution.
 */
interface ErrorReporter {

    /**
     * Records an unexpected non-fatal exception with optional diagnostic technical context.
     *
     * @param throwable The unexpected exception or error.
     * @param context Technical key-value pairs providing context (e.g. feature, operation, sync mode).
     *                All keys and values are sanitized via [DataSanitizer] before recording.
     */
    fun recordException(
        throwable: Throwable,
        context: Map<String, String> = emptyMap()
    )

    /**
     * Appends a diagnostic breadcrumb log to the active session.
     * Breadcrumbs help diagnose the sequence of user or system actions leading up to an error.
     */
    fun log(message: String)

    /**
     * Sets a custom diagnostic attribute for the current session.
     */
    fun setCustomKey(key: String, value: String)

    /**
     * Associates an anonymous/pseudonymous identifier with crash reports.
     * Never provide cleartext PII, email addresses, or database IDs.
     */
    fun setUserContext(userId: String?)

    /**
     * Clears the user identifier on logout to maintain privacy across sessions.
     */
    fun clearUserContext()

    /**
     * Configures whether error collection is actively transmitted.
     * Useful for toggling behavior between Debug and Release builds or supporting opt-out.
     */
    fun setCollectionEnabled(enabled: Boolean)

    /**
     * Current status of error reporting collection.
     */
    val isCollectionEnabled: Boolean
}
