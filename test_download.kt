package com.deepeye.musicpro

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

fun testDownload(context: Context, url: String) {
    try {
        val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build()
        val request = Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()
        val response = client.newCall(request).execute()
        Log.d("TEST_DOWNLOAD", "Response code: ${response.code}")
    } catch (e: Exception) {
        Log.e("TEST_DOWNLOAD", "Error", e)
    }
}
