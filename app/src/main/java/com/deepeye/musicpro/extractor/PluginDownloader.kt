package com.deepeye.musicpro.extractor

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.io.IOException

class PluginDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val reqBuilder = okhttp3.Request.Builder().url(url)

        headers?.forEach { (key, list) ->
            if (list.isNotEmpty()) {
                reqBuilder.addHeader(key, list[0])
            }
        }

        if (httpMethod == "POST" || httpMethod == "PUT") {
            val body = okhttp3.RequestBody.create(null, dataToSend ?: ByteArray(0))
            reqBuilder.method(httpMethod, body)
        } else if (httpMethod == "HEAD" || httpMethod == "OPTIONS" || httpMethod == "TRACE") {
            reqBuilder.method(httpMethod, null)
        } else {
            reqBuilder.get()
        }

        val okReq = reqBuilder.build()
        val okRes = client.newCall(okReq).execute()

        val resHeaders = mutableMapOf<String, List<String>>()
        for (i in 0 until okRes.headers.size) {
            val name = okRes.headers.name(i)
            val value = okRes.headers.value(i)
            resHeaders[name] = listOf(value)
        }

        val bodyStr = okRes.body?.string() ?: ""

        return Response(
            okRes.code,
            okRes.message,
            resHeaders,
            bodyStr,
            okReq.url.toString()
        )
    }
}
