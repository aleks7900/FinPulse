package com.finpulse.app.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager {

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    var lastBackgroundTimestamp: Long = 0L
        private set

    var isAppInBackground: Boolean = false
        private set

    fun isLockConfigured(isBiometricEnabled: Boolean, isPinEnabled: Boolean): Boolean {
        return isBiometricEnabled || isPinEnabled
    }

    fun onAppBackgrounded(
        policy: LockTimeoutPolicy,
        isLockEnabled: Boolean,
        now: Long = System.currentTimeMillis()
    ) {
        lastBackgroundTimestamp = now
        isAppInBackground = true

        if (isLockEnabled && policy == LockTimeoutPolicy.IMMEDIATELY) {
            _isAppLocked.value = true
        }
    }

    fun onAppForegrounded(
        policy: LockTimeoutPolicy,
        isLockEnabled: Boolean,
        now: Long = System.currentTimeMillis()
    ) {
        isAppInBackground = false

        if (!isLockEnabled) {
            _isAppLocked.value = false
            return
        }

        if (_isAppLocked.value) {
            // Already locked, remain locked
            return
        }

        if (policy == LockTimeoutPolicy.NEVER) {
            // Never lock based on timeout
            return
        }

        val elapsed = now - lastBackgroundTimestamp
        if (lastBackgroundTimestamp > 0 && elapsed >= policy.timeoutMillis) {
            _isAppLocked.value = true
        }
    }

    fun unlock() {
        _isAppLocked.value = false
    }

    fun lockNow() {
        _isAppLocked.value = true
    }

    fun verifyPin(enteredPin: String, storedHash: String, saltBase64: String): Boolean {
        // Fallback support for legacy raw SHA-256 hashes if salt is missing
        if (saltBase64.isBlank() && storedHash.isNotBlank()) {
            val legacyHash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(enteredPin.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val matches = java.security.MessageDigest.isEqual(
                legacyHash.toByteArray(Charsets.UTF_8),
                storedHash.toByteArray(Charsets.UTF_8)
            )
            if (matches) {
                unlock()
            }
            return matches
        }

        val isValid = AppKeystoreManager.verifyPin(enteredPin, storedHash, saltBase64)
        if (isValid) {
            unlock()
        }
        return isValid
    }
}
