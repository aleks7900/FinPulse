package md.alexlab.finpulse.core.security

import md.alexlab.finpulse.domain.model.backup.BACKUP_ENCRYPTED_FORMAT_ID
import md.alexlab.finpulse.domain.model.backup.EncryptedBackupContainer
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val DEFAULT_ITERATIONS = 65536

    private val secureRandom = SecureRandom()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun isEncryptedBackup(jsonContent: String): Boolean {
        if (jsonContent.contains(BACKUP_ENCRYPTED_FORMAT_ID)) return true
        return try {
            val container = parseEncryptedContainer(jsonContent)
            container != null && container.ciphertextBase64.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun parseEncryptedContainer(jsonContent: String): EncryptedBackupContainer? {
        return try {
            json.decodeFromString<EncryptedBackupContainer>(jsonContent)
        } catch (_: Exception) {
            null
        }
    }

    fun encrypt(rawJson: String, password: CharArray): EncryptedBackupContainer {
        require(password.isNotEmpty()) { "Password must not be empty" }

        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        val keySpec = PBEKeySpec(password, salt, DEFAULT_ITERATIONS, KEY_LENGTH_BITS)
        val secretKeyFactory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        val ciphertext = cipher.doFinal(rawJson.toByteArray(Charsets.UTF_8))

        return EncryptedBackupContainer(
            format = BACKUP_ENCRYPTED_FORMAT_ID,
            version = 1,
            algorithm = "AES-256-GCM",
            kdf = KDF_ALGORITHM,
            iterations = DEFAULT_ITERATIONS,
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            ivBase64 = Base64.getEncoder().encodeToString(iv),
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext)
        )
    }

    fun decrypt(container: EncryptedBackupContainer, password: CharArray): String {
        require(password.isNotEmpty()) { "Password must not be empty" }

        val salt = Base64.getDecoder().decode(container.saltBase64)
        val iv = Base64.getDecoder().decode(container.ivBase64)
        val ciphertext = Base64.getDecoder().decode(container.ciphertextBase64)

        val keySpec = PBEKeySpec(password, salt, container.iterations, KEY_LENGTH_BITS)
        val secretKeyFactory = SecretKeyFactory.getInstance(container.kdf)
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        val plaintextBytes = cipher.doFinal(ciphertext)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    fun computeSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
