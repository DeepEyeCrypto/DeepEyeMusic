import re

file_path = "app/src/main/java/com/deepeye/musicpro/data/source/remote/update/AutoUpdateManager.kt"
with open(file_path, "r") as f:
    code = f.read()

# Remove DownloadManager imports and BroadcastReceiver
code = re.sub(r'import android\.app\.DownloadManager\n', '', code)
code = re.sub(r'import android\.content\.BroadcastReceiver\n', '', code)
code = re.sub(r'import android\.content\.IntentFilter\n', '', code)

# Remove downloadManager and activeDownloadId fields
code = re.sub(r'private val downloadManager =.*?\n', '', code)
code = re.sub(r'private var activeDownloadId: Long = -1L\n', '', code)

# Remove downloadReceiver
code = re.sub(r'private val downloadReceiver =.*?object : BroadcastReceiver\(\) \{.*?\}\n        \}\n', '', code, flags=re.DOTALL)

# Remove init block that registers receiver
code = re.sub(r'init \{.*?\}\n', '', code, flags=re.DOTALL)

# Replace startProgressPolling with empty (will be removed anyway)
code = re.sub(r'private fun startProgressPolling.*?\}\n    \}\n', '', code, flags=re.DOTALL)

# Refactor downloadUpdate function
new_download_update = """    fun downloadUpdate(
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
    }"""

code = re.sub(r'fun downloadUpdate\(.*?\{.*?(?=fun canRequestPackageInstalls)', new_download_update + "\n\n    ", code, flags=re.DOTALL)

with open(file_path, "w") as f:
    f.write(code)

print("AutoUpdateManager patched successfully")
