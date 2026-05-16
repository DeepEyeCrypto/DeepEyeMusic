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
                request.headers().forEach { (key, values) ->
                    addHeader(key, values.joinToString(", "))
                }
            }
            .apply {
                if (request.httpMethod() == "POST") {
                    val body = request.dataToSend() ?: ByteArray(0)
                    post(body.toRequestBody("application/json".toMediaType()))
                }
            }
            .build()

        return try {
            val response = client.newCall(okRequest).execute()
            val bodyString = response.body?.string() ?: ""
            Response(
                response.code,
                response.message.ifEmpty { "OK" },
                response.headers.toMultimap(),
                bodyString,
                request.url()
            )
        } catch (e: Exception) {
            throw IOException("NewPipe download failed: ${e.message}", e)
        }
    }
}
