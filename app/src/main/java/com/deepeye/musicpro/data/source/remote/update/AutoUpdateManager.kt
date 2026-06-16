// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.deepeye.musicpro.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateState {
    object Idle : UpdateState()

    object Checking : UpdateState()

    data class UpdateAvailable(val version: String, val apkUrl: String, val releaseNotes: String) : UpdateState()

    object UpToDate : UpdateState()

    data class Downloading(val progress: Float) : UpdateState()

    data class Downloaded(val file: File, val version: String) : UpdateState()

    data class Error(val message: String) : UpdateState()
}

@Singleton
class AutoUpdateManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
) {
    private val IS_MOCK_MODE = false

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

            private var latestUpdateVersion: String = ""
    private var progressJob: Job? = null
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    
    
    fun checkForUpdate() {
        if (_updateState.value is UpdateState.Checking) return
        _updateState.value = UpdateState.Checking

        if (IS_MOCK_MODE) {
            scope.launch {
                delay(1500)
                val currentVersion = BuildConfig.VERSION_NAME.substringBefore("-").trim()
                if (isNewerVersion("2.0.0", currentVersion)) {
                    _updateState.value =
                        UpdateState.UpdateAvailable(
                            version = "2.0.0",
                            apkUrl = "https://github.com/deepeye/DeepEyeMusicPro/releases/download/v2.0.0/DeepEyeMusicPro-v2.0.0.apk",
                            releaseNotes = "Mock Update for Verification\n- Glassmorphic Banner\n- Premium auto-update flow\n- Real-time progress bar",
                        )
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            }
            return
        }

        scope.launch {
            try {
                val request =
                    Request.Builder()
                        .url("https://api.github.com/repos/DeepEyeCrypto/DeepEyeMusic/releases/latest")
                        .header("User-Agent", "DeepEyeMusicPro-App")
                        .build()

                val response = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    okHttpClient.newCall(request).execute()
                }

                if (response == null) {
                    _updateState.value = UpdateState.Error("Update check timed out")
                    return@launch
                }

                response.use { 
                    if (!it.isSuccessful) {
                        _updateState.value = UpdateState.Error("Failed to fetch release: ${it.code}")
                        return@launch
                    }

                    val bodyString = it.body?.string() ?: throw Exception("Empty response body")
                    val json = gson.fromJson(bodyString, JsonObject::class.java)
                    val tagName = json.getAsJsonPrimitive("tag_name")?.asString ?: throw Exception("tag_name not found")
                    val latestVersion = tagName.removePrefix("v").trim()
                    val body = json.getAsJsonPrimitive("body")?.asString ?: ""

                    val assetsArray = json.getAsJsonArray("assets") ?: throw Exception("No assets found")
                    var apkUrl: String? = null
                    for (i in 0 until assetsArray.size()) {
                        val asset = assetsArray.get(i).asJsonObject
                        val name = asset.getAsJsonPrimitive("name")?.asString ?: ""
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.getAsJsonPrimitive("browser_download_url")?.asString
                            break
                        }
                    }

                    if (apkUrl == null) {
                        _updateState.value = UpdateState.Error("No APK file found in the latest release assets")
                        return@launch
                    }

                    val currentVersion = BuildConfig.VERSION_NAME.substringBefore("-").trim()
                    if (isNewerVersion(latestVersion, currentVersion)) {
                        _updateState.value =
                            UpdateState.UpdateAvailable(
                                version = latestVersion,
                                apkUrl = apkUrl,
                                releaseNotes = body,
                            )
                    } else {
                        _updateState.value = UpdateState.UpToDate
                    }
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Unknown error checking for updates")
            }
        }
    }

        fun downloadUpdate(
        apkUrl: String,
        version: String,
    ) {
        if (IS_MOCK_MODE) {
            latestUpdateVersion = version
            _updateState.value = UpdateState.Downloading(0f)
            scope.launch {
                for (progress in 1..10) {
                    delay(200)
                    _updateState.value = UpdateState.Downloading(progress / 10f)
                }
                val dummyFile =
                    File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        "DeepEyeMusicPro-$version.apk"
                    )
                if (!dummyFile.exists()) dummyFile.createNewFile()
                _updateState.value = UpdateState.Downloaded(dummyFile, version)
            }
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                latestUpdateVersion = version
                _updateState.value = UpdateState.Downloading(0f)

                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "DeepEyeMusicPro-$version.apk")
                if (file.exists()) {
                    file.delete()
                }

                val request = Request.Builder().url(apkUrl).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    _updateState.value = UpdateState.Error("Download failed: ${response.code}")
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _updateState.value = UpdateState.Error("Empty response body")
                    return@launch
                }

                val contentLength = body.contentLength()
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesCopied = 0L
                        var bytes = input.read(buffer)
                        var lastEmitTime = System.currentTimeMillis()
                        
                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            
                            val currentTime = System.currentTimeMillis()
                            if (contentLength > 0 && currentTime - lastEmitTime > 100) {
                                val progress = bytesCopied.toFloat() / contentLength.toFloat()
                                _updateState.value = UpdateState.Downloading(progress)
                                lastEmitTime = currentTime
                            }
                            bytes = input.read(buffer)
                        }
                    }
                }
                
                _updateState.value = UpdateState.Downloaded(file, version)
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Failed to download: ${e.message}")
            }
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownAppSourcesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent =
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        }
    }

    fun installApk(file: File) {
        try {
            if (!file.exists()) {
                _updateState.value = UpdateState.Error("Installation file does not exist")
                return
            }

            if (!canRequestPackageInstalls()) {
                openUnknownAppSourcesSettings()
                return
            }

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file,
                )
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to start installation: ${e.message}")
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    internal fun isNewerVersion(
        latest: String,
        current: String,
    ): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(l.size, c.size)
        for (i in 0 until maxLen) {
            val li = l.getOrNull(i) ?: 0
            val ci = c.getOrNull(i) ?: 0
            if (li > ci) return true
            if (li < ci) return false
        }
        return false
    }
}
