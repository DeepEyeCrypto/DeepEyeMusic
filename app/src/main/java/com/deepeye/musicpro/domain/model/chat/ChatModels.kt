package com.deepeye.musicpro.domain.model.chat

data class ChatThread(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessagePreview: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0
)

data class EncryptedMessage(
    val id: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L,
    val encryptedPayloadBase64: String = "",
    val ivBase64: String = "",
    val encryptedAesKeyBase64: String = "",
    val senderEncryptedAesKeyBase64: String? = null,
    val expiresAt: Long? = null
)

data class DecryptedMessage(
    val id: String,
    val senderId: String,
    val timestamp: Long,
    val plaintext: String,
    val isMine: Boolean,
    val expiresAt: Long? = null
)
