// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.downloader.Request
import com.yushosei.newpipe.extractor.downloader.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeDownloader @Inject constructor(
    private val client: OkHttpClient
) : Downloader() {

    override suspend fun execute(request: Request): Response {
        val okRequest = okhttp3.Request.Builder()
            .url(request.url())
            .apply {
                var hasUserAgent = false
                request.headers().forEach { (key, values) ->
                    if (key.equals("User-Agent", ignoreCase = true)) {
                        hasUserAgent = true
                    }
                    addHeader(key, values.joinToString(", "))
                }
                if (!hasUserAgent) {
                    addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                }
            }
            .apply {
                if (request.httpMethod() == "POST") {
                    val body = request.dataToSend() ?: ByteArray(0)
                    post(body.toRequestBody("application/json".toMediaType()))
                }
            }
            .build()

        android.util.Log.d("NewPipeDownloader", "Executing request: ${request.url()}")
        return try {
            val response = client.newCall(okRequest).execute()
            val bodyString = response.body?.string() ?: ""
            android.util.Log.d("NewPipeDownloader", "Response Code: ${response.code} for ${request.url()}")
            Response(
                response.code,
                response.message.ifEmpty { "OK" },
                response.headers.toMultimap(),
                bodyString,
                request.url()
            )
        } catch (e: Exception) {
            android.util.Log.e("NewPipeDownloader", "Download failed for ${request.url()}: ${e.message}", e)
            throw IOException("NewPipe download failed: ${e.message}", e)
        }
    }
}
