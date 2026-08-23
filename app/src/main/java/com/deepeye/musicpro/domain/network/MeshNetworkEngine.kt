package com.deepeye.musicpro.domain.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class MeshNode(
    val nodeId: String,
    val deviceName: String,
    val address: String,
    val isDirectPeer: Boolean = true,
    val hopDistance: Int = 1,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val receiverId: String,
    val chatId: String,
    val encryptedPayload: String,
    val senderEncryptedAesKey: String? = null,
    val hopCount: Int = 0,
    val maxHops: Int = 5,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class MeshNetworkEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val TAG = "MeshNetworkEngine"
    private val MESH_PORT = 8888
    private val prefs = context.getSharedPreferences("mesh_prefs", Context.MODE_PRIVATE)

    private val _isMeshEnabled = MutableStateFlow(prefs.getBoolean("mesh_enabled", false))
    val isMeshEnabled: StateFlow<Boolean> = _isMeshEnabled.asStateFlow()

    private val _activeNodes = MutableStateFlow<List<MeshNode>>(emptyList())
    val activeNodes: StateFlow<List<MeshNode>> = _activeNodes.asStateFlow()

    private val _connectedPeersCount = MutableStateFlow(0)
    val connectedPeersCount: StateFlow<Int> = _connectedPeersCount.asStateFlow()

    private val seenPacketIds = ConcurrentHashMap.newKeySet<String>()
    private val activePeersMap = ConcurrentHashMap<String, MeshNode>()

    private var serverSocket: ServerSocket? = null
    private var isListening = false
    private val incomingPacketListeners = mutableListOf<(MeshPacket) -> Unit>()

    init {
        if (_isMeshEnabled.value) {
            startMeshService()
        }
    }

    fun setMeshEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("mesh_enabled", enabled).apply()
        _isMeshEnabled.value = enabled
        if (enabled) {
            startMeshService()
        } else {
            stopMeshService()
        }
    }

    fun addIncomingPacketListener(listener: (MeshPacket) -> Unit) {
        incomingPacketListeners.add(listener)
    }

    @Synchronized
    fun startMeshService() {
        if (isListening) return
        isListening = true
        Log.i(TAG, "Starting Off-Grid P2P Mesh Engine on port $MESH_PORT")

        // Start P2P Mesh Listening Socket
        Thread {
            try {
                serverSocket = ServerSocket(MESH_PORT)
                Log.i(TAG, "P2P Mesh Server listening on port $MESH_PORT")
                while (isListening && serverSocket?.isClosed == false) {
                    val clientSocket = serverSocket?.accept() ?: break
                    Thread { handleIncomingPeerConnection(clientSocket) }.start()
                }
            } catch (e: Exception) {
                if (isListening) {
                    Log.e(TAG, "Mesh Server Socket Error", e)
                }
            }
        }.start()

        // Discovery Ping Task (Simulate P2P Local Discovery)
        discoverLocalMeshPeers()
    }

    @Synchronized
    fun stopMeshService() {
        isListening = false
        try {
            serverSocket?.close()
            serverSocket = null
            activePeersMap.clear()
            _activeNodes.value = emptyList()
            _connectedPeersCount.value = 0
            Log.i(TAG, "Stopped Off-Grid P2P Mesh Engine")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Mesh engine", e)
        }
    }

    private fun handleIncomingPeerConnection(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val input = socket.getInputStream()
            val json = input.bufferedReader().readLine() ?: return
            val packet = gson.fromJson(json, MeshPacket::class.java)

            if (packet != null && seenPacketIds.add(packet.packetId)) {
                Log.d(TAG, "Received P2P Mesh Packet ${packet.packetId} from ${packet.senderId}")
                
                // Add peer node
                val remoteIp = socket.inetAddress?.hostAddress ?: "127.0.0.1"
                val node = MeshNode(
                    nodeId = packet.senderId,
                    deviceName = "Peer Node (${packet.senderId.take(6)})",
                    address = remoteIp,
                    hopDistance = packet.hopCount + 1
                )
                activePeersMap[packet.senderId] = node
                updatePeersState()

                // Notify listeners
                incomingPacketListeners.forEach { it.invoke(packet) }

                // Relay packet if hop limit not reached
                if (packet.hopCount < packet.maxHops) {
                    relayPacket(packet.copy(hopCount = packet.hopCount + 1))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming mesh connection", e)
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    suspend fun broadcastPacket(packet: MeshPacket): Boolean = withContext(Dispatchers.IO) {
        if (!isListening) return@withContext false
        seenPacketIds.add(packet.packetId)
        Log.d(TAG, "Broadcasting P2P Mesh packet ${packet.packetId} to ${activePeersMap.size} peers")

        var deliveredCount = 0
        activePeersMap.values.forEach { peer ->
            try {
                Socket(peer.address, MESH_PORT).use { socket ->
                    socket.soTimeout = 5000
                    val json = gson.toJson(packet) + "\n"
                    socket.getOutputStream().write(json.toByteArray(Charsets.UTF_8))
                    socket.getOutputStream().flush()
                    deliveredCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to deliver mesh packet directly to ${peer.address}")
            }
        }
        return@withContext deliveredCount > 0
    }

    private fun relayPacket(packet: MeshPacket) {
        Thread {
            activePeersMap.values.forEach { peer ->
                try {
                    Socket(peer.address, MESH_PORT).use { socket ->
                        socket.soTimeout = 5000
                        val json = gson.toJson(packet) + "\n"
                        socket.getOutputStream().write(json.toByteArray(Charsets.UTF_8))
                        socket.getOutputStream().flush()
                    }
                } catch (_: Exception) {}
            }
        }.start()
    }

    private fun discoverLocalMeshPeers() {
        Thread {
            // Simulated local node discovery for local Wi-Fi / Bluetooth subnet mesh nodes
            val mockPeers = listOf(
                MeshNode("peer_alpha", "Nearby DeepEye Node A", "192.168.1.105", hopDistance = 1),
                MeshNode("peer_beta", "Relay Node B", "192.168.1.112", hopDistance = 2)
            )
            mockPeers.forEach { activePeersMap[it.nodeId] = it }
            updatePeersState()
        }.start()
    }

    private fun updatePeersState() {
        val list = activePeersMap.values.toList()
        _activeNodes.value = list
        _connectedPeersCount.value = list.size
    }
}
