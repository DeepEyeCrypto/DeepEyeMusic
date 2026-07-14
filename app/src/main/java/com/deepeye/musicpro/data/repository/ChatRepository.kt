package com.deepeye.musicpro.data.repository

import android.util.Log
import com.deepeye.musicpro.domain.crypto.CryptoEngine
import com.deepeye.musicpro.domain.model.chat.ChatThread
import com.deepeye.musicpro.domain.model.chat.DecryptedMessage
import com.deepeye.musicpro.domain.model.chat.EncryptedMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    private val TAG = "ChatRepository"
    
    private val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)

    fun hasChatPassword(): Boolean {
        return prefs.getString("chat_password", null) != null
    }

    fun verifyChatPassword(password: String): Boolean {
        return prefs.getString("chat_password", null) == password
    }

    fun setChatPassword(password: String) {
        prefs.edit().putString("chat_password", password).apply()
    }

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Publishes this user's RSA Public Key to Firestore so others can send them encrypted messages.
     */
    suspend fun publishMyPublicKey() {
        val uid = getCurrentUserId() ?: return
        try {
            val pubKeyBase64 = CryptoEngine.getMyPublicKeyBase64()
            firestore.collection("users").document(uid)
                .update("publicKey", pubKeyBase64).await()
            Log.d(TAG, "Successfully published public key for user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish public key", e)
        }
    }

    /**
     * Sets a custom Chat ID for the current user.
     */
    suspend fun setCustomChatId(chatId: String): Boolean {
        val uid = getCurrentUserId() ?: return false
        return try {
            val formattedId = if (chatId.startsWith("@")) chatId else "@$chatId"
            firestore.collection("users").document(uid).update("chatId", formattedId).await()
            true
        } catch (e: Exception) {
            // If the document doesn't exist, set it instead of update
            try {
                val formattedId = if (chatId.startsWith("@")) chatId else "@$chatId"
                firestore.collection("users").document(uid).set(mapOf("chatId" to formattedId), com.google.firebase.firestore.SetOptions.merge()).await()
                true
            } catch (inner: Exception) {
                Log.e(TAG, "Failed to set custom chat ID", inner)
                false
            }
        }
    }

    /**
     * Gets the current user's custom Chat ID.
     */
    suspend fun getMyCustomChatId(): String? {
        val uid = getCurrentUserId() ?: return null
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("chatId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch custom chat ID", e)
            null
        }
    }

    /**
     * Looks up a raw Firebase UID by a custom Chat ID (e.g. "@username").
     */
    suspend fun getUserIdByCustomChatId(chatId: String): String? {
        return try {
            val formattedId = if (chatId.startsWith("@")) chatId else "@$chatId"
            val querySnapshot = firestore.collection("users").whereEqualTo("chatId", formattedId).limit(1).get().await()
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].id
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lookup user by custom chat ID", e)
            null
        }
    }

    /**
     * Fetches the public key of another user to encrypt a message for them.
     */
    suspend fun getReceiverPublicKey(receiverId: String): PublicKey? {
        return try {
            // If the receiverId is a custom ID (starts with @), resolve it to a raw UID first.
            val rawUid = if (receiverId.startsWith("@")) {
                getUserIdByCustomChatId(receiverId)
            } else {
                receiverId
            }
            
            if (rawUid == null) {
                Log.e(TAG, "Cannot find user with ID $receiverId")
                return null
            }

            val doc = firestore.collection("users").document(rawUid).get().await()
            val pubKeyBase64 = doc.getString("publicKey")
            if (!pubKeyBase64.isNullOrEmpty()) {
                CryptoEngine.parsePublicKeyFromBase64(pubKeyBase64)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch public key for $receiverId", e)
            null
        }
    }

    /**
     * Sends an end-to-end encrypted message.
     */
    suspend fun sendMessage(chatId: String, receiverId: String, plaintext: String, timeToLive: Long? = null): Boolean {
        val senderId = getCurrentUserId() ?: return false
        val receiverPubKey = getReceiverPublicKey(receiverId)
        
        if (receiverPubKey == null) {
            Log.e(TAG, "Cannot send message: Receiver has no public key published.")
            return false
        }

        return try {
            // 1. Generate one-time AES key
            val aesKey = CryptoEngine.generateRandomAesKey()
            
            // 2. Encrypt the message text with AES
            val payload = CryptoEngine.encryptAes(plaintext, aesKey)
            
            // 3. Encrypt the AES key with the receiver's RSA public key
            val encryptedAesKey = CryptoEngine.encryptAesKeyWithRsa(aesKey, receiverPubKey)

            val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
            val timestamp = System.currentTimeMillis()

            val expiresAt = if (timeToLive != null) timestamp + (timeToLive * 1000) else null

            val msg = EncryptedMessage(
                id = messageId,
                senderId = senderId,
                timestamp = timestamp,
                encryptedPayloadBase64 = payload.ciphertextBase64,
                ivBase64 = payload.ivBase64,
                encryptedAesKeyBase64 = encryptedAesKey,
                expiresAt = expiresAt
            )

            // Save message
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId).set(msg).await()
            
            // Update thread metadata
            firestore.collection("chats").document(chatId).set(
                mapOf(
                    "lastMessageTimestamp" to timestamp,
                    "participants" to listOf(senderId, receiverId)
                ), com.google.firebase.firestore.SetOptions.merge()
            ).await()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send encrypted message", e)
            false
        }
    }

    /**
     * Deletes a specific message from Firestore.
     */
    suspend fun deleteMessage(chatId: String, messageId: String) {
        try {
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message", e)
        }
    }

    /**
     * Real-time listener for incoming E2E messages. Decrypts them on the fly.
     */
    fun getMessages(chatId: String): Flow<List<DecryptedMessage>> = callbackFlow {
        val currentUserId = getCurrentUserId() ?: ""
        
        val registration = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                val decryptedMessages = mutableListOf<DecryptedMessage>()
                
                snapshot?.documents?.forEach { doc ->
                    val msg = doc.toObject(EncryptedMessage::class.java)
                    if (msg != null) {
                        try {
                            val isMine = msg.senderId == currentUserId
                            val plaintext = if (isMine) {
                                // If I am the sender, I cannot decrypt the message because I encrypted the AES key with the RECEIVER's public key!
                                // In a real system, you'd encrypt a copy of the AES key for yourself. For MVP, we'll just show a placeholder if we clear cache.
                                "(Sent Message)"
                            } else {
                                // I am the receiver. I can decrypt using my RSA private key!
                                val aesKey = CryptoEngine.decryptAesKeyWithRsa(msg.encryptedAesKeyBase64)
                                val payload = CryptoEngine.EncryptedPayload(msg.encryptedPayloadBase64, msg.ivBase64)
                                CryptoEngine.decryptAes(payload, aesKey)
                            }
                            
                            decryptedMessages.add(
                                DecryptedMessage(
                                    id = msg.id,
                                    senderId = msg.senderId,
                                    timestamp = msg.timestamp,
                                    plaintext = plaintext,
                                    isMine = isMine,
                                    expiresAt = msg.expiresAt
                                )
                            )
                        } catch (ex: Exception) {
                            Log.e(TAG, "Failed to decrypt message ${msg.id}", ex)
                            decryptedMessages.add(
                                DecryptedMessage(
                                    id = msg.id,
                                    senderId = msg.senderId,
                                    timestamp = msg.timestamp,
                                    plaintext = "[Encrypted Message - Decryption Failed]",
                                    isMine = msg.senderId == currentUserId
                                )
                            )
                        }
                    }
                }
                
                trySend(decryptedMessages).isSuccess
            }

        awaitClose { registration.remove() }
    }
}
