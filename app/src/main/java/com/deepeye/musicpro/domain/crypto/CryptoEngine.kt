package com.deepeye.musicpro.domain.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

/**
 * Handles hardware-backed RSA key pair generation, AES-GCM symmetric encryption for message payloads,
 * and RSA asymmetric encryption for securely exchanging the AES keys.
 */
object CryptoEngine {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val RSA_ALIAS = "DeepEyeE2E_RSA_Key"
    
    // AES-GCM parameters
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12 // in bytes
    private const val GCM_TAG_LENGTH = 128 // in bits

    // RSA parameters
    private const val RSA_MODE = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

    init {
        generateRsaKeyPairIfNeeded()
    }

    /**
     * Generates a 2048-bit RSA key pair in the Android Keystore if it doesn't already exist.
     */
    private fun generateRsaKeyPairIfNeeded() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(RSA_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                RSA_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setKeySize(2048)
                .build()

            keyPairGenerator.initialize(parameterSpec)
            keyPairGenerator.generateKeyPair()
        }
    }

    /**
     * Gets the user's local Base64-encoded Public Key to share with Firebase.
     */
    fun getMyPublicKeyBase64(): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val publicKey = keyStore.getCertificate(RSA_ALIAS).publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun getMyPublicKey(): PublicKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getCertificate(RSA_ALIAS).publicKey
    }

    private fun getMyPrivateKey(): PrivateKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(RSA_ALIAS, null) as PrivateKey
    }

    fun parsePublicKeyFromBase64(base64Key: String): PublicKey {
        val keyBytes = Base64.decode(base64Key, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(spec)
    }

    // =========================================================================
    // AES Symmetric Encryption (For Message Payload)
    // =========================================================================

    fun generateRandomAesKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    data class EncryptedPayload(
        val ciphertextBase64: String,
        val ivBase64: String
    )

    fun encryptAes(plaintext: String, secretKey: SecretKey): EncryptedPayload {
        val cipher = Cipher.getInstance(AES_MODE)
        val iv = ByteArray(GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        return EncryptedPayload(
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decryptAes(payload: EncryptedPayload, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(AES_MODE)
        val iv = Base64.decode(payload.ivBase64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(payload.ciphertextBase64, Base64.NO_WRAP)
        
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val plaintextBytes = cipher.doFinal(ciphertext)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    // =========================================================================
    // RSA Asymmetric Encryption (For Encrypting the AES Key)
    // =========================================================================

    /**
     * Encrypts the raw AES key bytes using the receiver's RSA public key.
     */
    fun encryptAesKeyWithRsa(aesKey: SecretKey, receiverPublicKey: PublicKey): String {
        val cipher = Cipher.getInstance(RSA_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, receiverPublicKey)
        val encryptedBytes = cipher.doFinal(aesKey.encoded)
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    /**
     * Decrypts the AES key bytes using the local user's hardware RSA private key.
     */
    fun decryptAesKeyWithRsa(encryptedAesKeyBase64: String): SecretKey {
        val cipher = Cipher.getInstance(RSA_MODE)
        cipher.init(Cipher.DECRYPT_MODE, getMyPrivateKey())
        
        val encryptedBytes = Base64.decode(encryptedAesKeyBase64, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        // Reconstruct SecretKey
        return javax.crypto.spec.SecretKeySpec(decryptedBytes, 0, decryptedBytes.size, "AES")
    }
}
