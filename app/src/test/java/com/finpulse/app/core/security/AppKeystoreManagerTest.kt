package com.finpulse.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class AppKeystoreManagerTest {

    @Test
    fun `generateSalt produces unique 16-byte base64 encoded salts`() {
        val salt1 = AppKeystoreManager.generateSalt()
        val salt2 = AppKeystoreManager.generateSalt()

        assertNotNull(salt1)
        assertNotNull(salt2)
        assertNotEquals(salt1, salt2)

        val decoded1 = Base64.getDecoder().decode(salt1)
        val decoded2 = Base64.getDecoder().decode(salt2)
        assertEquals(16, decoded1.size)
        assertEquals(16, decoded2.size)
    }

    @Test
    fun `hashPinWithSalt produces deterministic 64-character hex string`() {
        val pin = "1234"
        val salt = AppKeystoreManager.generateSalt()

        val hash1 = AppKeystoreManager.hashPinWithSalt(pin, salt)
        val hash2 = AppKeystoreManager.hashPinWithSalt(pin, salt)

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
        assertTrue(hash1.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun `hashPinWithSalt produces different outputs for different pins or salts`() {
        val salt1 = AppKeystoreManager.generateSalt()
        val salt2 = AppKeystoreManager.generateSalt()

        val hashPinA = AppKeystoreManager.hashPinWithSalt("1234", salt1)
        val hashPinB = AppKeystoreManager.hashPinWithSalt("5678", salt1)
        val hashSalt2 = AppKeystoreManager.hashPinWithSalt("1234", salt2)

        assertNotEquals(hashPinA, hashPinB)
        assertNotEquals(hashPinA, hashSalt2)
    }

    @Test
    fun `verifyPin accurately verifies matching pin and rejects incorrect pin`() {
        val pin = "9876"
        val wrongPin = "1234"
        val salt = AppKeystoreManager.generateSalt()
        val hash = AppKeystoreManager.hashPinWithSalt(pin, salt)

        assertTrue(AppKeystoreManager.verifyPin(pin, hash, salt))
        assertFalse(AppKeystoreManager.verifyPin(wrongPin, hash, salt))
        assertFalse(AppKeystoreManager.verifyPin("", hash, salt))
        assertFalse(AppKeystoreManager.verifyPin(pin, "", salt))
        assertFalse(AppKeystoreManager.verifyPin(pin, hash, ""))
    }

    @Test
    fun `encryptSecret and decryptSecret roundtrip restores original plaintext`() {
        val originalSecret = "TopSecretAccountCredential123!@#"
        val encrypted = AppKeystoreManager.encryptSecret(originalSecret)

        assertNotNull(encrypted)
        assertNotEquals(originalSecret, encrypted)

        val decrypted = AppKeystoreManager.decryptSecret(encrypted)
        assertEquals(originalSecret, decrypted)
    }

    @Test
    fun `encryptSecret and decryptSecret handles empty string`() {
        val encrypted = AppKeystoreManager.encryptSecret("")
        assertEquals("", encrypted)

        val decrypted = AppKeystoreManager.decryptSecret("")
        assertEquals("", decrypted)
    }
}
