package com.example.core.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class PasswordHashResult(
    val hashBase64: String,
    val saltBase64: String
)

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String,
    val saltBase64: String
)

object CryptoSecurity {
    private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    private const val PASSWORD_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private val secureRandom = SecureRandom()

    fun hashPassword(password: String): PasswordHashResult {
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val hash = pbkdf2(password.toCharArray(), salt)
        return PasswordHashResult(
            hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP),
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        )
    }

    fun verifyPassword(password: String, expectedHashBase64: String, saltBase64: String): Boolean {
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val expectedHash = Base64.decode(expectedHashBase64, Base64.NO_WRAP)
            val actualHash = pbkdf2(password.toCharArray(), salt)
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (_: Exception) {
            false
        }
    }

    fun encryptAesGcm(plaintext: String, passphrase: String): EncryptedPayload {
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
        val keyBytes = pbkdf2(passphrase.toCharArray(), salt)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        )
    }

    fun decryptAesGcm(
        ciphertextBase64: String,
        ivBase64: String,
        saltBase64: String,
        passphrase: String
    ): Result<String> {
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val keyBytes = pbkdf2(passphrase.toCharArray(), salt)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val plainBytes = cipher.doFinal(ciphertext)
            Result.success(String(plainBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sanitizeCsvCell(value: String): String {
        val trimmed = value.replace("\r", " ").replace("\n", " ").trim()
        val formulaSafe = if (trimmed.isNotEmpty() && trimmed[0] in listOf('=', '+', '-', '@', '\t')) {
            "'$trimmed"
        } else {
            trimmed
        }
        val escaped = formulaSafe.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun isStrongPassword(password: String): Boolean =
        password.length >= 8 &&
            password.any { it.isLetterOrDigit() }

    private fun pbkdf2(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, PASSWORD_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGO).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
