package com.deepeye.musicpro.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.repository.ChatRepository
import com.deepeye.musicpro.domain.model.chat.DecryptedMessage
import com.deepeye.musicpro.domain.network.MeshNetworkEngine
import com.deepeye.musicpro.domain.network.MeshNode
import com.deepeye.musicpro.domain.network.TorProxyManager
import com.deepeye.musicpro.domain.network.TorVerificationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val torProxyManager: TorProxyManager,
    private val meshNetworkEngine: MeshNetworkEngine
) : ViewModel() {

    val isTorEnabled: StateFlow<Boolean> = torProxyManager.isTorEnabled
    val isMeshEnabled: StateFlow<Boolean> = meshNetworkEngine.isMeshEnabled
    val connectedPeersCount: StateFlow<Int> = meshNetworkEngine.connectedPeersCount
    val activeMeshNodes: StateFlow<List<MeshNode>> = meshNetworkEngine.activeNodes

    private val _torStatus = MutableStateFlow<TorVerificationResult?>(null)
    val torStatus: StateFlow<TorVerificationResult?> = _torStatus.asStateFlow()

    private val _messages = MutableStateFlow<List<DecryptedMessage>>(emptyList())
    val messages: StateFlow<List<DecryptedMessage>> = _messages.asStateFlow()

    private val _myChatId = MutableStateFlow<String?>(null)
    val myChatId: StateFlow<String?> = _myChatId.asStateFlow()

    private val _selectedTtl = MutableStateFlow<Long?>(null)
    val selectedTtl: StateFlow<Long?> = _selectedTtl.asStateFlow()

    private var currentChatId: String = ""
    private var currentReceiverId: String = ""

    init {
        viewModelScope.launch {
            chatRepository.publishMyPublicKey()
            _myChatId.value = chatRepository.getMyCustomChatId()
        }
    }

    fun toggleTor(enabled: Boolean) {
        torProxyManager.setTorEnabled(enabled)
        if (enabled) {
            verifyTor()
        } else {
            _torStatus.value = null
        }
    }

    fun toggleMesh(enabled: Boolean) {
        meshNetworkEngine.setMeshEnabled(enabled)
    }

    fun verifyTor() {
        viewModelScope.launch {
            _torStatus.value = torProxyManager.verifyTorConnection()
        }
    }

    fun updateCustomChatId(newId: String) {
        if (newId.isBlank() || newId.contains(" ")) return
        viewModelScope.launch {
            val success = chatRepository.setCustomChatId(newId)
            if (success) {
                _myChatId.value = if (newId.startsWith("@")) newId else "@$newId"
            }
        }
    }

    fun hasChatPassword(): Boolean {
        return chatRepository.hasChatPassword()
    }

    fun verifyChatPassword(password: String): Boolean {
        return chatRepository.verifyChatPassword(password)
    }

    fun setChatPassword(password: String) {
        chatRepository.setChatPassword(password)
    }

    fun resetChatPassword() {
        chatRepository.resetChatPassword()
    }

    fun initChat(chatId: String, receiverId: String) {
        currentChatId = chatId
        currentReceiverId = receiverId
        
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { msgs ->
                _messages.value = msgs
            }
        }
        
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                val now = System.currentTimeMillis()
                _messages.value.forEach { msg ->
                    if (msg.expiresAt != null && now >= msg.expiresAt) {
                        chatRepository.deleteMessage(currentChatId, msg.id)
                    }
                }
            }
        }
    }

    fun setTtl(seconds: Long?) {
        _selectedTtl.value = seconds
    }

    fun sendMessage(text: String, onComplete: (Boolean) -> Unit = {}) {
        if (text.isBlank()) return
        
        val ttl = _selectedTtl.value
        viewModelScope.launch {
            val success = chatRepository.sendMessage(currentChatId, currentReceiverId, text, ttl)
            onComplete(success)
        }
    }
}
