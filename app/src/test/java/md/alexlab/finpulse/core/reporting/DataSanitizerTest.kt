package md.alexlab.finpulse.core.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSanitizerTest {

    @Test
    fun isKeyAllowed_rejectsForbiddenFinancialAndSecurityKeys() {
        val forbiddenKeys = listOf(
            "password",
            "user_password",
            "pin",
            "auth_token",
            "bearer_token",
            "secret_key",
            "apiKey",
            "amount",
            "transaction_amount",
            "account_balance",
            "bank_name",
            "account_name",
            "user_email",
            "notes",
            "description",
            "request_payload",
            "response_body",
            "credit_card",
            "cvv",
            "iban_number"
        )

        for (key in forbiddenKeys) {
            assertFalse("Expected key '$key' to be rejected", DataSanitizer.isKeyAllowed(key))
        }
    }

    @Test
    fun isKeyAllowed_permitsSafeTechnicalDiagnostics() {
        val allowedKeys = listOf(
            "screen",
            "feature",
            "operation",
            "sync_mode",
            "status",
            "app_version",
            "build_type",
            "item_count",
            "file_type",
            "error_code"
        )

        for (key in allowedKeys) {
            assertTrue("Expected key '$key' to be allowed", DataSanitizer.isKeyAllowed(key))
        }
    }

    @Test
    fun sanitizeValue_redactsEmailAddresses() {
        val input = "Error contacting support at alex.developer@finpulse.app for account recovery"
        val output = DataSanitizer.sanitizeValue(input)

        assertFalse(output.contains("alex.developer@finpulse.app"))
        assertTrue(output.contains("[REDACTED_EMAIL]"))
    }

    @Test
    fun sanitizeValue_redactsAuthBearerAndJwtTokens() {
        val bearer = "Authorization: Bearer ya29.a0AfH6SMBabc1234567890XYZ"
        val sanitizedBearer = DataSanitizer.sanitizeValue(bearer)
        assertFalse(sanitizedBearer.contains("ya29.a0AfH6SMBabc1234567890XYZ"))
        assertTrue(sanitizedBearer.contains("[REDACTED_TOKEN]"))

        val jwt = "Session token eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeakThis"
        val sanitizedJwt = DataSanitizer.sanitizeValue(jwt)
        assertFalse(sanitizedJwt.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertTrue(sanitizedJwt.contains("[REDACTED_TOKEN]"))
    }

    @Test
    fun sanitizeValue_redactsGoogleApiKeys() {
        val input = "Failed calling service with key AIzaSyAptMbFVj25my_TsUrvPn_6DvvSJJQF_-o"
        val output = DataSanitizer.sanitizeValue(input)

        assertFalse(output.contains("AIzaSyAptMbFVj25my_TsUrvPn_6DvvSJJQF_-o"))
        assertTrue(output.contains("[REDACTED_API_KEY]"))
    }

    @Test
    fun sanitizeValue_redactsFinancialAmounts() {
        val input1 = "User attempted transfer of $1,250.50 to external entity"
        val output1 = DataSanitizer.sanitizeValue(input1)
        assertFalse(output1.contains("$1,250.50"))
        assertTrue(output1.contains("[REDACTED_AMOUNT]"))

        val input2 = "Charge failed for 450.00 EUR at payment gateway"
        val output2 = DataSanitizer.sanitizeValue(input2)
        assertFalse(output2.contains("450.00 EUR"))
        assertTrue(output2.contains("[REDACTED_AMOUNT]"))
    }

    @Test
    fun sanitizeValue_truncatesOverlyLongStrings() {
        val hugeString = "A".repeat(1000)
        val sanitized = DataSanitizer.sanitizeValue(hugeString)

        assertTrue(sanitized.length <= 520)
        assertTrue(sanitized.endsWith("...[TRUNCATED]"))
    }

    @Test
    fun sanitizeContext_dropsForbiddenKeysAndSanitizesValues() {
        val context = mapOf(
            "screen" to "cloud_sync",
            "operation" to "upload",
            "password" to "supersecret123",
            "account_name" to "Main Checking Account",
            "amount" to "$5000",
            "description" to "Bought groceries at supermarket",
            "status" to "failed with contact user@example.com"
        )

        val sanitized = DataSanitizer.sanitizeContext(context)

        assertEquals("cloud_sync", sanitized["screen"])
        assertEquals("upload", sanitized["operation"])
        assertEquals("failed with contact [REDACTED_EMAIL]", sanitized["status"])

        assertNull("Password should be dropped", sanitized["password"])
        assertNull("Account name should be dropped", sanitized["account_name"])
        assertNull("Amount should be dropped", sanitized["amount"])
        assertNull("Description should be dropped", sanitized["description"])
    }

    @Test
    fun sanitizeThrowable_redactsSensitiveMessageWhilePreservingStackTrace() {
        val originalEx = IllegalStateException("Failed with token ya29.secret and email test@finpulse.io")
        val sanitized = DataSanitizer.sanitizeThrowable(originalEx)

        assertNotEquals(originalEx.message, sanitized.message)
        assertFalse(sanitized.message!!.contains("test@finpulse.io"))
        assertTrue(sanitized.message!!.contains("[REDACTED_EMAIL]"))
        assertEquals(originalEx.stackTrace.size, sanitized.stackTrace.size)
    }

    @Test
    fun anonymizeUserId_producesConsistentPseudonymousHash() {
        val rawUid = "firebase_user_abc_123_xyz"
        val anonId = DataSanitizer.anonymizeUserId(rawUid)

        assertNotNull(anonId)
        assertTrue(anonId!!.startsWith("anon_"))
        assertFalse(anonId.contains("firebase_user_abc_123_xyz"))

        // Must be consistent for same UID
        val anonId2 = DataSanitizer.anonymizeUserId(rawUid)
        assertEquals(anonId, anonId2)

        // Different UID produces different hash
        val anonIdOther = DataSanitizer.anonymizeUserId("another_user_456")
        assertNotEquals(anonId, anonIdOther)

        // Null or blank produces null
        assertNull(DataSanitizer.anonymizeUserId(null))
        assertNull(DataSanitizer.anonymizeUserId(""))
        assertNull(DataSanitizer.anonymizeUserId("   "))
    }
}
