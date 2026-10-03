package md.alexlab.finpulse.core.reporting

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseCrashlyticsErrorReporterTest {

    private lateinit var mockCrashlytics: FirebaseCrashlytics
    private lateinit var reporter: FirebaseCrashlyticsErrorReporter

    @Before
    fun setUp() {
        mockCrashlytics = mockk(relaxed = true)
        reporter = FirebaseCrashlyticsErrorReporter(
            crashlyticsProvider = { mockCrashlytics },
            isDebug = false,
            initialCollectionEnabled = true
        )
    }

    @Test
    fun recordException_recordsSanitizedThrowableAndCustomKeys() {
        val testException = RuntimeException("Database error with user email test@example.com")
        val context = mapOf(
            "feature" to "cloud_sync",
            "operation" to "upload",
            "password" to "should_be_stripped"
        )

        reporter.recordException(testException, context)

        // Custom keys should include safe keys, but not forbidden ones
        verify { mockCrashlytics.setCustomKey("feature", "cloud_sync") }
        verify { mockCrashlytics.setCustomKey("operation", "upload") }
        verify(exactly = 0) { mockCrashlytics.setCustomKey("password", any<String>()) }

        // Exception should be recorded with sanitized message
        verify {
            mockCrashlytics.recordException(match { ex ->
                ex.message?.contains("[REDACTED_EMAIL]") == true &&
                    ex.message?.contains("test@example.com") == false
            })
        }
    }

    @Test
    fun log_sanitizesBreadcrumbBeforeSendingToCrashlytics() {
        val breadcrumb = "Navigated to user profile with token Bearer secret_token_value"
        reporter.log(breadcrumb)

        verify {
            mockCrashlytics.log(match { msg ->
                msg.contains("[REDACTED_TOKEN]") && !msg.contains("secret_token_value")
            })
        }
    }

    @Test
    fun setUserContext_anonymizesUserIdBeforeSendingToCrashlytics() {
        val rawUid = "user_abc_789"
        reporter.setUserContext(rawUid)

        verify {
            mockCrashlytics.setUserId(match { id ->
                id.startsWith("anon_") && !id.contains("user_abc_789")
            })
        }
    }

    @Test
    fun clearUserContext_setsEmptyUserIdOnCrashlytics() {
        reporter.clearUserContext()
        verify { mockCrashlytics.setUserId("") }
    }

    @Test
    fun setCollectionEnabled_updatesStateAndDelegatesToCrashlytics() {
        reporter.setCollectionEnabled(false)

        assertFalse(reporter.isCollectionEnabled)
        verify { mockCrashlytics.setCrashlyticsCollectionEnabled(false) }

        reporter.setCollectionEnabled(true)
        assertTrue(reporter.isCollectionEnabled)
        verify { mockCrashlytics.setCrashlyticsCollectionEnabled(true) }
    }

    @Test
    fun recordException_whenCollectionDisabled_doesNotRecordToCrashlytics() {
        reporter.setCollectionEnabled(false)

        reporter.recordException(IllegalStateException("Should not be sent"))

        verify(exactly = 0) { mockCrashlytics.recordException(any()) }
    }

    @Test
    fun recordException_whenCrashlyticsThrows_swallowsExceptionSafely() {
        every { mockCrashlytics.recordException(any()) } throws RuntimeException("Crashlytics internal disk full")

        // Must never throw to caller
        reporter.recordException(IllegalStateException("App failure"))
    }
}
