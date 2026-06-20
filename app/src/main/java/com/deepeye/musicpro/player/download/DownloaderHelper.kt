package com.deepeye.musicpro.player.download

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.OutputStream
import java.net.SocketTimeoutException
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

object DownloaderHelper {
    suspend fun downloadWithResume(
        client: OkHttpClient,
        url: String,
        out: OutputStream,
        totalBytesWritten: Long = 0L
    ): Long {
        var currentBytes = totalBytesWritten
        val maxRetries = 5
        var retries = 0

        while (retries < maxRetries && coroutineContext.isActive) {
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                
                if (currentBytes > 0) {
                    requestBuilder.addHeader("Range", "bytes=$currentBytes-")
                }

                val request = requestBuilder.build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }
                    
                    val body = response.body ?: throw Exception("Empty response body")
                    val inputStream = body.byteStream()
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    
                    while (coroutineContext.isActive) {
                        bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break
                        out.write(buffer, 0, bytesRead)
                        currentBytes += bytesRead
                    }
                }
                // If we reach here normally, the download is complete
                break
            } catch (e: Exception) {
                if (e is SocketTimeoutException || e.message?.contains("timeout") == true || e.cause is SocketTimeoutException) {
                    android.util.Log.e("DownloaderHelper", "Socket timeout! Retrying with Range: bytes=$currentBytes- (Attempt ${retries + 1})", e); retries++
                    if (retries >= maxRetries) {
                        throw Exception("Max retries reached due to timeouts", e)
                    }
                    // Wait a bit before retrying
                    kotlinx.coroutines.delay(2000L * retries)
                } else {
                    throw e
                }
            }
        }
        return currentBytes
    }
}
