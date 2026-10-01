package md.alexlab.finpulse.core.security

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException

class BackupCryptoTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    @Test
    fun `encrypt and decrypt roundtrip restores original plaintext`() {
        val originalText = """{"schemaVersion":6,"appName":"FinPulse","testData":"Sample financial records 12345 €$£"}"""
        val password = "SuperSecretPassword123!".toCharArray()

        val container = BackupCrypto.encrypt(originalText, password)
        assertNotNull(container)

        val containerJson = json.encodeToString(container)
        assertTrue(BackupCrypto.isEncryptedBackup(containerJson))

        val parsedContainer = BackupCrypto.parseEncryptedContainer(containerJson)
        assertNotNull(parsedContainer)

        val decryptedText = BackupCrypto.decrypt(parsedContainer!!, password)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun `decrypt with wrong password throws GeneralSecurityException`() {
        val originalText = "Sensitive financial records"
        val password = "CorrectPassword123".toCharArray()
        val wrongPassword = "WrongPassword456".toCharArray()

        val container = BackupCrypto.encrypt(originalText, password)

        assertThrows(GeneralSecurityException::class.java) {
            BackupCrypto.decrypt(container, wrongPassword)
        }
    }

    @Test
    fun `isEncryptedBackup accurately identifies encrypted containers and rejects plaintext`() {
        val plaintextJson = """{"schemaVersion":6,"metadata":{"appName":"FinPulse"}}"""
        assertFalse(BackupCrypto.isEncryptedBackup(plaintextJson))

        val container = BackupCrypto.encrypt("test content", "mypassword".toCharArray())
        val containerJson = json.encodeToString(container)
        assertTrue(BackupCrypto.isEncryptedBackup(containerJson))

        assertFalse(BackupCrypto.isEncryptedBackup("not a json string"))
        assertFalse(BackupCrypto.isEncryptedBackup(""))
    }

    @Test
    fun `computeSha256 produces consistent 64-character hex hash`() {
        val input = "FinPulse Backup Checksum Test Data"
        val hash1 = BackupCrypto.computeSha256(input)
        val hash2 = BackupCrypto.computeSha256(input)

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
        assertTrue(hash1.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun `computeSha256 changes when content is modified`() {
        val input1 = "Data 1"
        val input2 = "Data 2"

        val hash1 = BackupCrypto.computeSha256(input1)
        val hash2 = BackupCrypto.computeSha256(input2)

        org.junit.Assert.assertNotEquals(hash1, hash2)
    }

    @Test
    fun `parseEncryptedContainer parses valid container and returns null on invalid`() {
        val container = BackupCrypto.encrypt("payload", "pass".toCharArray())
        val containerJson = json.encodeToString(container)
        val parsed = BackupCrypto.parseEncryptedContainer(containerJson)

        assertNotNull(parsed)
        assertEquals("AES-256-GCM", parsed?.algorithm)
        assertEquals("PBKDF2WithHmacSHA256", parsed?.kdf)
        assertEquals(65536, parsed?.iterations)
        assertTrue(parsed?.saltBase64?.isNotEmpty() == true)
        assertTrue(parsed?.ivBase64?.isNotEmpty() == true)
        assertTrue(parsed?.ciphertextBase64?.isNotEmpty() == true)

        assertNull(BackupCrypto.parseEncryptedContainer("invalid json format"))
    }
}
