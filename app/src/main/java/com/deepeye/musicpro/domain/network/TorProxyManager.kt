package com.deepeye.musicpro.domain.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TorCheckResponse(
    val IsTor: Boolean = false,
    val IP: String = ""
)

data class TorVerificationResult(
    val isTor: Boolean = false,
    val ip: String = "Unknown",
    val errorMessage: String? = null
)

@Singleton
class TorProxyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val TAG = "TorProxyManager"
    private val prefs = context.getSharedPreferences("tor_prefs", Context.MODE_PRIVATE)

    private val _isTorEnabled = MutableStateFlow(prefs.getBoolean("tor_enabled", false))
    val isTorEnabled: StateFlow<Boolean> = _isTorEnabled.asStateFlow()

    private val _torHost = MutableStateFlow(prefs.getString("tor_host", "127.0.0.1") ?: "127.0.0.1")
    val torHost: StateFlow<String> = _torHost.asStateFlow()

    private val _torPort = MutableStateFlow(prefs.getInt("tor_port", 9050))
    val torPort: StateFlow<Int> = _torPort.asStateFlow()

    fun setTorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("tor_enabled", enabled).apply()
        _isTorEnabled.value = enabled
        if (enabled) {
            EmbeddedTorEngine.startEngine(context, _torPort.value)
        } else {
            EmbeddedTorEngine.stopEngine()
        }
        Log.d(TAG, "Tor anonymity routing set to: $enabled")
    }

    init {
        if (_isTorEnabled.value) {
            EmbeddedTorEngine.startEngine(context, _torPort.value)
        }
    }

    fun setTorConfig(host: String, port: Int) {
        prefs.edit().putString("tor_host", host).putInt("tor_port", port).apply()
        _torHost.value = host
        _torPort.value = port
    }

    fun getProxy(): Proxy {
        return if (_isTorEnabled.value) {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress(_torHost.value, _torPort.value))
        } else {
            Proxy.NO_PROXY
        }
    }

    fun getOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)

        if (_isTorEnabled.value) {
            builder.proxy(getProxy())
        }

        return builder.build()
    }

    suspend fun verifyTorConnection(): TorVerificationResult = withContext(Dispatchers.IO) {
        try {
            val client = getOkHttpClient()
            val request = Request.Builder()
                .url("https://check.torproject.org/api/ip")
                .header("User-Agent", "DeepEyeMusicPro/3.0.1 (Tor Client)")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val torCheck = gson.fromJson(body, TorCheckResponse::class.java)
                    TorVerificationResult(isTor = torCheck.IsTor, ip = torCheck.IP)
                } else {
                    TorVerificationResult(isTor = false, ip = "Direct / Unverified", errorMessage = "HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tor verification check failed", e)
            val msg = e.localizedMessage ?: "Failed to reach Tor network"
            val readableMsg = if (msg.contains("ECONNREFUSED") || msg.contains("Connection refused")) {
                "Tor SOCKS Daemon not active on ${_torHost.value}:${_torPort.value}. Start Orbot or check proxy host."
            } else {
                msg
            }
            TorVerificationResult(
                isTor = false,
                ip = "Unreachable",
                errorMessage = readableMsg
            )
        }
    }
}
