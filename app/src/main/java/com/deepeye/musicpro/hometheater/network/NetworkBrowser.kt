// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.hometheater.network

import android.util.Log
import com.deepeye.musicpro.hometheater.model.NetworkShareSource
import com.deepeye.musicpro.hometheater.model.NetworkSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Discovers, connects to, and browses network media sources:
 * SMB/Windows shares, WebDAV/Nextcloud, DLNA/UPnP, NFS, HTTP directories, and IPTV M3U lists.
 */
@Singleton
class NetworkBrowser @Inject constructor() {

    private val _sources = MutableStateFlow<List<NetworkShareSource>>(emptyList())
    val sources: StateFlow<List<NetworkShareSource>> = _sources.asStateFlow()

    private val _browseState = MutableStateFlow(BrowseEntry("", ""))
    val browseState: StateFlow<BrowseEntry> = _browseState.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _connected.asStateFlow()

    companion object {
        private const val TAG = "NetworkBrowser"
        private const val CONNECT_TIMEOUT_MS = 4000
    }

    // ── Source management ─────────────────────────────────────────

    fun addSource(source: NetworkShareSource) {
        _sources.value = _sources.value + source
    }

    fun removeSource(sourceId: String) {
        _sources.value = _sources.value.filterNot { it.id == sourceId }
    }

    fun updateSourceStatus(sourceId: String, online: Boolean) {
        _sources.value = _sources.value.map {
            if (it.id == sourceId) it.copy(isOnline = online) else it
        }
    }

    suspend fun probeAllSources() = withContext(Dispatchers.IO) {
        for (source in _sources.value) {
            val online = probeSource(source)
            updateSourceStatus(source.id, online)
        }
        _connected.value = _sources.value.any { it.isOnline }
    }

    suspend fun probeSource(source: NetworkShareSource): Boolean = withContext(Dispatchers.IO) {
        val host = source.serverAddress
            .substringAfter("://", source.serverAddress)
            .substringBefore("/")
            .substringBefore(":")
            .trim()

        if (host.isEmpty()) return@withContext false
        try {
            val address = InetAddress.getByName(host)
            address.isReachable(CONNECT_TIMEOUT_MS)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun probePort(source: NetworkShareSource, port: Int): Boolean = withContext(Dispatchers.IO) {
        val host = source.serverAddress.substringAfter("://").substringBefore("/").substringBefore(":")
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    // ── Browsing ──────────────────────────────────────────────────

    fun resolvePlayableUri(source: NetworkShareSource, relativePath: String): String {
        return when (source.type) {
            NetworkSourceType.LOCAL_STORAGE -> relativePath
            NetworkSourceType.SMB_SHARE -> {
                // Auth credentials are resolved separately by the SMB client layer;
                // embed only the host path here to avoid leaking credentials in URIs.
                "smb://${source.serverAddress}${source.sharePath}/$relativePath"
            }
            NetworkSourceType.WEBDAV -> {
                buildString {
                    append(if (source.serverAddress.startsWith("https")) "https" else "http")
                    append("://")
                    append(source.serverAddress)
                    append(source.sharePath)
                    if (!source.sharePath.endsWith("/")) append("/")
                    append(relativePath)
                }
            }
            NetworkSourceType.DLNA_UPNP -> relativePath
            NetworkSourceType.NFS -> "nfs://${source.serverAddress}:${source.sharePath}/$relativePath"
            NetworkSourceType.HTTP_DIRECT -> relativePath
            NetworkSourceType.IPTV_M3U -> relativePath
        }
    }

    suspend fun parseM3uPlaylist(content: String): List<IptvChannel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<IptvChannel>()
        var pendingExtInf: JSONObject? = null

        for (line in content.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF") -> {
                    pendingExtInf = parseExtInf(trimmed)
                }
                trimmed.startsWith("#") -> { }
                trimmed.isNotEmpty() && pendingExtInf != null -> {
                    val meta = pendingExtInf ?: continue
                    channels.add(
                        IptvChannel(
                            id = "iptv_${meta.hashCode()}",
                            name = meta.optString("name", trimmed),
                            uri = trimmed,
                            logoUrl = meta.optString("logo").ifEmpty { null },
                            groupTitle = meta.optString("group"),
                            tvgId = meta.optString("tvgId")
                        )
                    )
                    pendingExtInf = null
                }
            }
        }
        channels
    }

    private fun parseExtInf(line: String): JSONObject? {
        return try {
            val json = JSONObject()
            Regex("""tvg-id="?([^",]*)""?""").find(line)?.let { json.put("tvgId", it.groupValues[1]) }
            Regex("""tvg-logo="?([^",]*)""?""").find(line)?.let { json.put("logo", it.groupValues[1]) }
            Regex("""group-title="?([^",]*)""?""").find(line)?.let { json.put("group", it.groupValues[1]) }
            val displayName = line.substringAfterLast(",", "")
            if (displayName.isNotEmpty()) json.put("name", displayName)
            json
        } catch (e: Exception) {
            null
        }
    }

    // ── Discovery ─────────────────────────────────────────────────

    suspend fun discoverLocalServers(): List<NetworkShareSource> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<NetworkShareSource>()
        val localNet = getLocalNetworkPrefix() ?: return@withContext emptyList()
        val knownPorts = mapOf(
            "smb" to (NetworkSourceType.SMB_SHARE to 445),
            "webdav" to (NetworkSourceType.WEBDAV to 8080),
            "dlna" to (NetworkSourceType.DLNA_UPNP to 2869)
        )

        for (hostOctet in 1..254) {
            val host = "$localNet$hostOctet"
            if (host == getLocalIpAddress()) continue
            for ((key, pair) in knownPorts) {
                val (type, port) = pair
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), 300)
                        if (socket.isConnected) {
                            discovered.add(
                                NetworkShareSource(
                                    id = UUID.randomUUID().toString(),
                                    name = "$host ($key)",
                                    type = type,
                                    serverAddress = host,
                                    sharePath = "/",
                                    isOnline = true
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        discovered
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is InetAddress && addr.hostAddress.contains('.')) {
                        return addr.hostAddress
                    }
                }
            }
            ""
        } catch (e: Exception) { "" }
    }

    private fun getLocalNetworkPrefix(): String? {
        val ip = getLocalIpAddress()
        if (ip.isEmpty()) return null
        val parts = ip.split(".")
        if (parts.size != 4) return null
        val first = parts[0].toIntOrNull() ?: return null
        val second = parts[1].toIntOrNull() ?: return null
        val third = parts[2].toIntOrNull() ?: return null
        if (first == 10 || (first == 172 && second in 16..31) || (first == 192 && second == 168)) {
            return "$first.$second.$third."
        }
        return null
    }

    fun listLocalMediaFolders(): List<File> {
        val roots = listOf(File("/sdcard/Movies"), File("/sdcard/Videos"), File("/sdcard/Music"))
        return roots.filter { it.exists() && it.isDirectory }
    }
}

data class BrowseEntry(
    val path: String,
    val title: String
)

data class IptvChannel(
    val id: String,
    val name: String,
    val uri: String,
    val logoUrl: String? = null,
    val groupTitle: String = "",
    val tvgId: String = ""
)
