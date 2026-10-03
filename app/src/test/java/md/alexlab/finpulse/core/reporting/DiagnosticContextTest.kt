package md.alexlab.finpulse.core.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiagnosticContextTest {

    @Test
    fun build_constructsStandardDiagnosticMapWithSanitization() {
        val context = DiagnosticContext.build(
            feature = DiagnosticContext.FEATURE_CLOUD_SYNC,
            operation = DiagnosticContext.OP_FULL_SYNC,
            extra = mapOf(
                DiagnosticContext.KEY_SYNC_MODE to "cloud",
                "password" to "forbidden_pass",
                "status" to "network_timeout"
            )
        )

        assertEquals("cloud_sync", context[DiagnosticContext.KEY_FEATURE])
        assertEquals("full_sync", context[DiagnosticContext.KEY_OPERATION])
        assertEquals("cloud", context[DiagnosticContext.KEY_SYNC_MODE])
        assertEquals("network_timeout", context["status"])

        // Forbidden keys must be stripped
        assertNull(context["password"])
    }
}
