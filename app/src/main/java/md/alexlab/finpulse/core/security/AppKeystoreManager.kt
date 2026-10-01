package md.alexlab.finpulse.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AppKeystoreManager {

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "FinPulseMasterSecretKey"
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 10000
    private const val PBKDF2_KEY_LENGTH = 256
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    // Fallback key used only when AndroidKeyStore is not available (e.g. JVM unit tests)
    private var testFallbackKey: SecretKey? = null

    private fun isAndroidKeyStoreAvailable(): Boolean {
        return try {
            KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        if (!isAndroidKeyStoreAvailable()) {
            return testFallbackKey ?: synchronized(this) {
                testFallbackKey ?: run {
                    val keyBytes = ByteArray(32).also { secureRandom.nextBytes(it) }
                    SecretKeySpec(keyBytes, "AES").also { testFallbackKey = it }
                }
            }
        }

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hashPinWithSalt(pin: String, saltBase64: String): String {
        require(pin.isNotBlank()) { "PIN must not be blank" }
        require(saltBase64.isNotBlank()) { "Salt must not be blank" }

        val saltBytes = Base64.getDecoder().decode(saltBase64)
        val keySpec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(keySpec).encoded
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, storedHash: String, saltBase64: String): Boolean {
        if (enteredPin.isBlank() || storedHash.isBlank() || saltBase64.isBlank()) {
            return false
        }
        val computedHash = hashPinWithSalt(enteredPin, saltBase64)
        return MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun encryptSecret(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val secretKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Prepend 12-byte IV to ciphertext
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    fun decryptSecret(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        val combined = Base64.getDecoder().decode(encryptedBase64)
        require(combined.size > GCM_IV_LENGTH_BYTES) { "Invalid encrypted secret payload" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES)

        val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.size)

        val secretKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        val decrypted = cipher.doFinal(ciphertext)
        return String(decrypted, Charsets.UTF_8)
    }
}
