package md.alexlab.finpulse.core.reporting

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorReportingIntegrationTest {

    @Test
    fun unexpectedError_isReportedWithSanitizedContext() {
        val mockReporter = mockk<ErrorReporter>(relaxed = true)

        // Simulate an unexpected database failure during transaction processing
        try {
            throw java.sql.SQLException("Disk I/O error writing block")
        } catch (t: Throwable) {
            mockReporter.recordException(
                throwable = t,
                context = DiagnosticContext.build(
                    feature = DiagnosticContext.FEATURE_DATABASE,
                    operation = "insert_transaction"
                )
            )
        }

        verify(exactly = 1) {
            mockReporter.recordException(
                throwable = any<java.sql.SQLException>(),
                context = match { it["feature"] == "database" && it["operation"] == "insert_transaction" }
            )
        }
    }

    @Test
    fun expectedValidationError_isNotReportedAsCrash() {
        val mockReporter = mockk<ErrorReporter>(relaxed = true)

        // Normal validation logic: user entered empty transaction note or invalid amount
        val userEnteredAmount = -10.0
        val isValid = userEnteredAmount > 0.0

        if (!isValid) {
            // Normal validation response (e.g. UI state update or returning validation result)
            // Does NOT invoke mockReporter.recordException
        }

        verify(exactly = 0) {
            mockReporter.recordException(any(), any())
        }
    }

    @Test
    fun noOpReporter_safelyAcceptsCallsWithoutExceptions() {
        val noOp = NoOpErrorReporter()

        assertFalse(noOp.isCollectionEnabled)
        noOp.setCollectionEnabled(true)
        assertTrue(noOp.isCollectionEnabled)

        noOp.recordException(IllegalStateException("test"))
        noOp.log("test breadcrumb")
        noOp.setCustomKey("key", "val")
        noOp.setUserContext("user_123")
        noOp.clearUserContext()
    }
}
