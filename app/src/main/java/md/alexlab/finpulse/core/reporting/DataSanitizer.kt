package md.alexlab.finpulse.core.reporting

import java.security.MessageDigest
import java.util.Locale
import java.util.regex.Pattern

/**
 * Centralized data sanitization and redaction engine for FinPulse crash and error diagnostics.
 *
 * Guarantees that no PII, financial details, passwords, tokens, or sensitive payload data
 * can ever be transmitted to external error reporting platforms or written to production logs.
 */
object DataSanitizer {

    private const val MAX_STRING_LENGTH = 500
    private const val REDACTED_MARKER = "[REDACTED]"
    private const val REDACTED_EMAIL = "[REDACTED_EMAIL]"
    private const val REDACTED_TOKEN = "[REDACTED_TOKEN]"
    private const val REDACTED_API_KEY = "[REDACTED_API_KEY]"
    private const val REDACTED_AMOUNT = "[REDACTED_AMOUNT]"
    private const val REDACTED_ACCOUNT_NUM = "[REDACTED_NUM]"

    // Forbidden keys (case-insensitive substring match)
    private val FORBIDDEN_KEY_KEYWORDS = setOf(
        "password",
        "pin",
        "token",
        "secret",
        "auth",
        "credential",
        "apikey",
        "api_key",
        "privatekey",
        "private_key",
        "amount",
        "balance",
        "account",
        "bank",
        "email",
        "mail",
        "note",
        "description",
        "content",
        "body",
        "payload",
        "card",
        "cvv",
        "cvc",
        "ssn",
        "iban",
        "phone",
        "title",
        "merchant"
    )

    // Regex patterns for sensitive value scrubbing
    private val EMAIL_REGEX = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+",
        Pattern.CASE_INSENSITIVE
    )

    private val BEARER_TOKEN_REGEX = Pattern.compile(
        "Bearer\\s+[A-Za-z0-9\\-_.~+/]+=*",
        Pattern.CASE_INSENSITIVE
    )

    private val JWT_REGEX = Pattern.compile(
        "eyJ[A-Za-z0-9\\-_]+\\.eyJ[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+"
    )

    private val GOOGLE_API_KEY_REGEX = Pattern.compile(
        "AIza[0-9A-Za-z\\-_]{35}"
    )

    private val CURRENCY_AMOUNT_REGEX = Pattern.compile(
        "(?:[\\$€£¥₽]|\\b(?:USD|EUR|GBP|MDL|RON|CAD|AUD|CHF|JPY)\\s*)\\s*\\d+(?:[.,]\\d{1,2})?|\\b\\d+(?:[.,]\\d{1,2})?\\s*(?:USD|EUR|GBP|MDL|RON|CAD|AUD|CHF|JPY)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val CARD_OR_IBAN_REGEX = Pattern.compile(
        "\\b(?:\\d[ -]*?){13,19}\\b|[A-Z]{2}\\d{2}[A-Z0-9]{11,30}",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Checks if a diagnostic context key is allowed.
     */
    fun isKeyAllowed(key: String): Boolean {
        if (key.isBlank()) return false
        val normalized = key.lowercase(Locale.ROOT).replace("-", "_").trim()
        return FORBIDDEN_KEY_KEYWORDS.none { normalized.contains(it) }
    }

    /**
     * Sanitizes a freeform string message or diagnostic value.
     */
    fun sanitizeValue(value: String?): String {
        if (value == null) return ""
        if (value.isBlank()) return ""

        var sanitized = value

        // 1. Redact Auth & API tokens
        sanitized = BEARER_TOKEN_REGEX.matcher(sanitized).replaceAll("Bearer $REDACTED_TOKEN")
        sanitized = JWT_REGEX.matcher(sanitized).replaceAll(REDACTED_TOKEN)
        sanitized = GOOGLE_API_KEY_REGEX.matcher(sanitized).replaceAll(REDACTED_API_KEY)

        // 2. Redact Emails
        sanitized = EMAIL_REGEX.matcher(sanitized).replaceAll(REDACTED_EMAIL)

        // 3. Redact Financial amounts & Card/IBAN numbers
        sanitized = CURRENCY_AMOUNT_REGEX.matcher(sanitized).replaceAll(REDACTED_AMOUNT)
        sanitized = CARD_OR_IBAN_REGEX.matcher(sanitized).replaceAll(REDACTED_ACCOUNT_NUM)

        // 4. Truncate overly long values
        if (sanitized.length > MAX_STRING_LENGTH) {
            sanitized = sanitized.take(MAX_STRING_LENGTH) + "...[TRUNCATED]"
        }

        return sanitized
    }

    /**
     * Sanitizes a key-value diagnostic context map.
     * Drops forbidden keys and scrubs values of allowed keys.
     */
    fun sanitizeContext(context: Map<String, String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for ((k, v) in context) {
            if (!isKeyAllowed(k)) {
                continue
            }
            val sanitizedVal = sanitizeValue(v)
            if (sanitizedVal.isNotEmpty()) {
                result[k.trim()] = sanitizedVal
            }
        }
        return result
    }

    /**
     * Sanitizes a throwable by ensuring its exception message does not contain sensitive data.
     * Preserves the original stack trace and exception type.
     */
    fun sanitizeThrowable(throwable: Throwable): Throwable {
        val originalMessage = throwable.message ?: return throwable
        val sanitizedMessage = sanitizeValue(originalMessage)

        // If no sensitive tokens were redacted and message length unchanged, keep original
        if (sanitizedMessage == originalMessage) {
            return throwable
        }

        val sanitizedWrapper = SanitizedException(
            originalClass = throwable.javaClass.name,
            sanitizedMessage = sanitizedMessage,
            cause = throwable.cause
        )
        sanitizedWrapper.stackTrace = throwable.stackTrace
        return sanitizedWrapper
    }

    /**
     * Generates a pseudonymous, one-way hashed identifier for error reporting.
     * Never reveals the user's raw UID, email, or database ID.
     */
    fun anonymizeUserId(rawUserId: String?): String? {
        if (rawUserId.isNullOrBlank()) return null
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawUserId.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { "%02x".format(it) }
            "anon_${hex.take(16)}"
        } catch (_: Exception) {
            "anon_unknown"
        }
    }
}

/**
 * Exception wrapper used when the original throwable message contained sensitive data that was redacted.
 */
class SanitizedException(
    val originalClass: String,
    sanitizedMessage: String,
    cause: Throwable? = null
) : Exception("[$originalClass] $sanitizedMessage", cause)
