package com.deepeye.musicpro.domain.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CryptoEngineTest {

    @Test
    fun testEndToEndEncryptionPipeline() {
        // 1. Generate local RSA keys (mimicking Receiver)
        val myPublicKeyBase64 = CryptoEngine.getMyPublicKeyBase64()
        val myPublicKey = CryptoEngine.parsePublicKeyFromBase64(myPublicKeyBase64)
        
        // 2. Sender prepares a message
        val originalMessage = "Hello from E2E Encryption! This should be completely secure."
        
        // 3. Sender generates a one-time AES key
        val oneTimeAesKey = CryptoEngine.generateRandomAesKey()
        
        // 4. Sender encrypts the message with AES
        val encryptedPayload = CryptoEngine.encryptAes(originalMessage, oneTimeAesKey)
        assertNotEquals(originalMessage, encryptedPayload.ciphertextBase64)
        
        // 5. Sender encrypts the AES key using the Receiver's Public Key
        val encryptedAesKeyBase64 = CryptoEngine.encryptAesKeyWithRsa(oneTimeAesKey, myPublicKey)
        
        // ================= NETWORK BOUNDARY (Firestore) =================
        // Only encryptedPayload and encryptedAesKeyBase64 traverse the network
        // ================================================================
        
        // 6. Receiver decrypts the AES key using their Private Key
        val decryptedAesKey = CryptoEngine.decryptAesKeyWithRsa(encryptedAesKeyBase64)
        
        // 7. Receiver decrypts the message payload using the decrypted AES key
        val decryptedMessage = CryptoEngine.decryptAes(encryptedPayload, decryptedAesKey)
        
        // 8. Verify perfect reconstruction
        assertEquals(originalMessage, decryptedMessage)
    }
}
