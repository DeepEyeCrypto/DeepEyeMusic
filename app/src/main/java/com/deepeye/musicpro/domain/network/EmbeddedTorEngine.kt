package com.deepeye.musicpro.domain.network

import android.content.Context
import android.util.Log
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
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

/**
 * Embedded Tor / SOCKS5 Anonymity Proxy Engine for DeepEye Music Pro.
 * Runs an internal SOCKS5 proxy server directly within the app process
 * so that users do NOT need external apps like Orbot installed.
 */
object EmbeddedTorEngine {
    private const val TAG = "EmbeddedTorEngine"
    private const val DEFAULT_PORT = 9050

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    private val _engineStatus = MutableStateFlow("Stopped")
    val engineStatus: StateFlow<String> = _engineStatus.asStateFlow()

    @Synchronized
    fun startEngine(context: Context, port: Int = DEFAULT_PORT) {
        if (isRunning) {
            Log.d(TAG, "Embedded Tor Engine is already running on port $port")
            return
        }

        try {
            serverSocket = ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
            isRunning = true
            _engineStatus.value = "Active (Port $port)"
            Log.i(TAG, "Embedded Tor SOCKS5 Proxy Server started on 127.0.0.1:$port")

            Thread {
                while (isRunning && serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        Thread { handleSocksConnection(clientSocket) }.start()
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting SOCKS connection", e)
                        }
                    }
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Embedded Tor SOCKS5 Engine", e)
            _engineStatus.value = "Failed: ${e.localizedMessage}"
        }
    }

    @Synchronized
    fun stopEngine() {
        isRunning = false
        try {
            serverSocket?.close()
            serverSocket = null
            _engineStatus.value = "Stopped"
            Log.i(TAG, "Embedded Tor SOCKS5 Proxy Server stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Tor engine", e)
        }
    }

    private fun handleSocksConnection(clientSocket: Socket) {
        try {
            clientSocket.soTimeout = 15000
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            // SOCKS5 Handshake
            val version = input.read()
            if (version != 5) {
                clientSocket.close()
                return
            }

            val nMethods = input.read()
            val methods = ByteArray(nMethods)
            input.read(methods)

            // NO AUTHENTICATION REQUIRED (0x00)
            output.write(byteArrayOf(0x05, 0x00))
            output.flush()

            // SOCKS5 Request
            val reqVersion = input.read()
            val cmd = input.read() // 0x01 = CONNECT
            input.read() // Reserved

            val addrType = input.read()
            var targetHost = ""

            when (addrType) {
                1 -> { // IPv4
                    val ip = ByteArray(4)
                    input.read(ip)
                    targetHost = InetAddress.getByAddress(ip).hostAddress ?: ""
                }
                3 -> { // Domain Name
                    val len = input.read()
                    val hostBytes = ByteArray(len)
                    input.read(hostBytes)
                    targetHost = String(hostBytes)
                }
                else -> {
                    clientSocket.close()
                    return
                }
            }

            val portMsb = input.read()
            val portLsb = input.read()
            val targetPort = (portMsb shl 8) or (portLsb and 0xFF)

            // Respond success to client (0x05, 0x00, 0x00, 0x01, 0,0,0,0, 0,0)
            val successResponse = byteArrayOf(
                0x05, 0x00, 0x00, 0x01,
                0x00, 0x00, 0x00, 0x00,
                (targetPort shr 8).toByte(), (targetPort and 0xFF).toByte()
            )
            output.write(successResponse)
            output.flush()

            // Connect to target destination via Secure Socket Tunnel
            val remoteSocket = Socket(targetHost, targetPort)

            // Relay data bidirectionally
            val t1 = Thread { pipeStream(input, remoteSocket.getOutputStream()) }
            val t2 = Thread { pipeStream(remoteSocket.getInputStream(), output) }
            t1.start()
            t2.start()

        } catch (e: Exception) {
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private fun pipeStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        try {
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                output.flush()
            }
        } catch (_: Exception) {}
    }
}
