package md.alexlab.finpulse.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest

class AppLockManagerTest {

    private lateinit var appLockManager: AppLockManager

    @Before
    fun setUp() {
        appLockManager = AppLockManager()
    }

    @Test
    fun `initial state is unlocked and not in background`() {
        assertFalse(appLockManager.isAppLocked.value)
        assertFalse(appLockManager.isAppInBackground)
        assertEquals(0L, appLockManager.lastBackgroundTimestamp)
    }

    @Test
    fun `LockTimeoutPolicy fromName parses policies accurately and defaults to MINUTE_1`() {
        assertEquals(LockTimeoutPolicy.IMMEDIATELY, LockTimeoutPolicy.fromName("IMMEDIATELY"))
        assertEquals(LockTimeoutPolicy.SECONDS_30, LockTimeoutPolicy.fromName("SECONDS_30"))
        assertEquals(LockTimeoutPolicy.MINUTE_1, LockTimeoutPolicy.fromName("MINUTE_1"))
        assertEquals(LockTimeoutPolicy.MINUTES_5, LockTimeoutPolicy.fromName("MINUTES_5"))
        assertEquals(LockTimeoutPolicy.MINUTES_15, LockTimeoutPolicy.fromName("MINUTES_15"))
        assertEquals(LockTimeoutPolicy.NEVER, LockTimeoutPolicy.fromName("NEVER"))
        assertEquals(LockTimeoutPolicy.MINUTE_1, LockTimeoutPolicy.fromName("UNKNOWN"))
        assertEquals(LockTimeoutPolicy.MINUTE_1, LockTimeoutPolicy.fromName(null))
    }

    @Test
    fun `isLockConfigured accurately reflects whether biometric or pin is enabled`() {
        assertTrue(appLockManager.isLockConfigured(isBiometricEnabled = true, isPinEnabled = false))
        assertTrue(appLockManager.isLockConfigured(isBiometricEnabled = false, isPinEnabled = true))
        assertTrue(appLockManager.isLockConfigured(isBiometricEnabled = true, isPinEnabled = true))
        assertFalse(appLockManager.isLockConfigured(isBiometricEnabled = false, isPinEnabled = false))
    }

    @Test
    fun `lockNow and unlock properly update lock state`() {
        appLockManager.lockNow()
        assertTrue(appLockManager.isAppLocked.value)

        appLockManager.unlock()
        assertFalse(appLockManager.isAppLocked.value)
    }

    @Test
    fun `onAppBackgrounded with IMMEDIATELY policy locks app immediately if lock is configured`() {
        val now = 10000L
        appLockManager.onAppBackgrounded(
            policy = LockTimeoutPolicy.IMMEDIATELY,
            isLockEnabled = true,
            now = now
        )

        assertTrue(appLockManager.isAppInBackground)
        assertEquals(now, appLockManager.lastBackgroundTimestamp)
        assertTrue(appLockManager.isAppLocked.value)
    }

    @Test
    fun `onAppBackgrounded with IMMEDIATELY policy does not lock if lock is disabled`() {
        appLockManager.onAppBackgrounded(
            policy = LockTimeoutPolicy.IMMEDIATELY,
            isLockEnabled = false
        )

        assertTrue(appLockManager.isAppInBackground)
        assertFalse(appLockManager.isAppLocked.value)
    }

    @Test
    fun `onAppBackgrounded with timed policy does not lock immediately`() {
        appLockManager.onAppBackgrounded(
            policy = LockTimeoutPolicy.SECONDS_30,
            isLockEnabled = true
        )

        assertTrue(appLockManager.isAppInBackground)
        assertFalse(appLockManager.isAppLocked.value)
    }

    @Test
    fun `onAppForegrounded locks app when elapsed time exceeds policy timeout`() {
        val bgTime = 100_000L
        appLockManager.onAppBackgrounded(
            policy = LockTimeoutPolicy.SECONDS_30,
            isLockEnabled = true,
            now = bgTime
        )
        assertFalse(appLockManager.isAppLocked.value)

        // Return to app after 35 seconds (exceeding 30s timeout)
        val fgTime = bgTime + 35_000L
        appLockManager.onAppForegrounded(
            policy = LockTimeoutPolicy.SECONDS_30,
            isLockEnabled = true,
            now = fgTime
        )

        assertFalse(appLockManager.isAppInBackground)
        assertTrue(appLockManager.isAppLocked.value)
    }

    @Test
    fun `onAppForegrounded does not lock app when elapsed time is within policy timeout`() {
        val bgTime = 100_000L
        appLockManager.onAppBackgrounded(
            policy = LockTimeoutPolicy.SECONDS_30,
            isLockEnabled = true,
            now = bgTime
        )

        // Return to app after only 10 seconds (less than 30s timeout)
        val fgTime = bgTime + 10_000L
        appLockManager.onAppForegrounded(
            policy = LockTimeoutPolicy.SECONDS_30,
            isLockEnabled = true,
            now = fgTime
        )

        assertFalse(appLockManager.isAppInBackground)
        assertFalse(appLockManager.isAppLocked.value)
    }

    @Test
    fun `onAppForegrounded with NEVER policy never locks automatically on resume`() {
        val bgTime = 100_000L
        appLockManager.onAppBackgrounded(
            policy = LockTimeoutPolicy.NEVER,
            isLockEnabled = true,
            now = bgTime
        )

        // Return after 2 hours
        val fgTime = bgTime + 7_200_000L
        appLockManager.onAppForegrounded(
            policy = LockTimeoutPolicy.NEVER,
            isLockEnabled = true,
            now = fgTime
        )

        assertFalse(appLockManager.isAppLocked.value)
    }

    @Test
    fun `verifyPin validates correctly and automatically unlocks app on success`() {
        appLockManager.lockNow()
        assertTrue(appLockManager.isAppLocked.value)

        val pin = "4321"
        val salt = AppKeystoreManager.generateSalt()
        val hash = AppKeystoreManager.hashPinWithSalt(pin, salt)

        // Wrong PIN: verification fails, app remains locked
        val wrongAttempt = appLockManager.verifyPin("9999", hash, salt)
        assertFalse(wrongAttempt)
        assertTrue(appLockManager.isAppLocked.value)

        // Correct PIN: verification succeeds, app unlocks
        val correctAttempt = appLockManager.verifyPin(pin, hash, salt)
        assertTrue(correctAttempt)
        assertFalse(appLockManager.isAppLocked.value)
    }

    @Test
    fun `verifyPin validates legacy un-salted SHA-256 hashes for backwards compatibility`() {
        appLockManager.lockNow()
        assertTrue(appLockManager.isAppLocked.value)

        val pin = "1234"
        val legacyHash = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        // Salt is empty string
        val result = appLockManager.verifyPin(pin, legacyHash, "")
        assertTrue(result)
        assertFalse(appLockManager.isAppLocked.value)
    }
}
