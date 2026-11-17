package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore AES-256-GCM encryption and PIN hashing utility.
 * Protects local sensitive preferences (PINs, API keys, session secrets) at rest.
 */
object LocalCryptoBox {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "fami_master_aes_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val ENC_PREFIX = "ENCv1:"
    private const val PIN_HASH_PREFIX = "PINv1:"

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) {
                existing.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        if (plainText.startsWith(ENC_PREFIX)) return plainText
        val secretKey = getOrCreateSecretKey() ?: return plainText
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val ctB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            "$ENC_PREFIX$ivB64:$ctB64"
        } catch (_: Exception) {
            plainText
        }
    }

    fun decrypt(storedValue: String): String {
        if (storedValue.isEmpty()) return ""
        if (!storedValue.startsWith(ENC_PREFIX)) return storedValue
        val payload = storedValue.removePrefix(ENC_PREFIX)
        val sepIndex = payload.indexOf(':')
        if (sepIndex <= 0) return ""
        val secretKey = getOrCreateSecretKey() ?: return ""
        return try {
            val iv = Base64.decode(payload.substring(0, sepIndex), Base64.NO_WRAP)
            val cipherBytes = Base64.decode(payload.substring(sepIndex + 1), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun hashPin(pin: String): String {
        val clean = pin.trim()
        if (clean.isEmpty()) return ""
        if (clean.startsWith(PIN_HASH_PREFIX)) return clean
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("FamiPinSalt#2026::$clean").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return encrypt("$PIN_HASH_PREFIX$digest")
    }

    fun verifyPin(storedValue: String?, candidatePin: String): Boolean {
        val cleanCandidate = candidatePin.trim()
        if (storedValue.isNullOrBlank() || cleanCandidate.length != 5) return false
        val decrypted = decrypt(storedValue)
        if (decrypted.isBlank()) return false
        return if (decrypted.startsWith(PIN_HASH_PREFIX)) {
            val expectedHash = decrypted.removePrefix(PIN_HASH_PREFIX)
            val actualHash = MessageDigest.getInstance("SHA-256")
                .digest(("FamiPinSalt#2026::$cleanCandidate").toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            MessageDigest.isEqual(
                expectedHash.toByteArray(Charsets.UTF_8),
                actualHash.toByteArray(Charsets.UTF_8)
            )
        } else {
            // Backward compatibility with legacy plaintext 5-digit PIN before migration
            decrypted == cleanCandidate
        }
    }

    fun hasValidPinConfigured(storedValue: String?): Boolean {
        if (storedValue.isNullOrBlank()) return false
        val decrypted = decrypt(storedValue)
        if (decrypted.isBlank()) return false
        if (decrypted == "00000") return false
        val defaultDummyHash = MessageDigest.getInstance("SHA-256")
            .digest("FamiPinSalt#2026::00000".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        if (decrypted == "$PIN_HASH_PREFIX$defaultDummyHash") return false
        return decrypted.startsWith(PIN_HASH_PREFIX) || (decrypted.length == 5 && decrypted.all { it.isDigit() })
    }
}
