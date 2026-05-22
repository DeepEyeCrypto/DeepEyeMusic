package com.deepeye.musicpro.ui

import com.deepeye.musicpro.data.source.remote.youtube.NewPipeDownloader
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.ServiceList
import com.yushosei.newpipe.extractor.stream.StreamInfo
import com.yushosei.newpipe.extractor.localization.Localization
import com.yushosei.newpipe.extractor.localization.ContentCountry
import okhttp3.OkHttpClient
import org.junit.Test
import java.io.File
import kotlinx.coroutines.runBlocking
import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic

class YoutubeExtractionTest {

    @Test
    fun testExtraction() = runBlocking {
        // Mock Android Log methods with explicit types
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        val client = OkHttpClient.Builder().build()
        val downloader = NewPipeDownloader(client)
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT)
        val yt = ServiceList.YouTube
        
        val videoId = "5XTxuz-_myg" // A popular video
        val url = "https://www.youtube.com/watch?v=$videoId"
        
        try {
            val info = StreamInfo.getInfo(yt, url)
            val resultText = buildString {
                appendLine("Video ID: $videoId")
                appendLine("Title: ${info.name}")
                appendLine("Class: ${info::class.java.name}")
                
                appendLine("\n=== FIELDS ===")
                info::class.java.fields.forEach { field ->
                    try {
                        field.isAccessible = true
                        appendLine("  - Field: ${field.name} (${field.type.name}) = ${field.get(info)}")
                    } catch (e: Exception) {
                        appendLine("  - Field: ${field.name} (error: ${e.message})")
                    }
                }
                
                appendLine("\n=== DECLARED FIELDS ===")
                info::class.java.declaredFields.forEach { field ->
                    try {
                        field.isAccessible = true
                        appendLine("  - Declared Field: ${field.name} (${field.type.name}) = ${field.get(info)}")
                    } catch (e: Exception) {
                        appendLine("  - Declared Field: ${field.name} (error: ${e.message})")
                    }
                }

                appendLine("\n=== DECLARED METHODS ===")
                info::class.java.declaredMethods.filter { it.parameterCount == 0 }.forEach { method ->
                    try {
                        method.isAccessible = true
                        appendLine("  - Method: ${method.name} returns ${method.returnType.name} = ${method.invoke(info)}")
                    } catch (e: Exception) {
                        // ignore exceptions for getter side effects
                    }
                }
            }
            println(resultText)
            File("scratch").mkdirs()
            File("scratch/extraction_result.txt").writeText(resultText)
        } catch (e: Exception) {
            val err = "Extraction failed: ${e.message}\n${e.stackTraceToString()}"
            println(err)
            File("scratch").mkdirs()
            File("scratch/extraction_result.txt").writeText(err)
        }
    }
}
