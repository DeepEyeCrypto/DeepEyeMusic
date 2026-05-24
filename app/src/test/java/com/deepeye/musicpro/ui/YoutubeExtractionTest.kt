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
        
        val videoId = "68RLvhxk_4g" // Bollywood Dj Non Stop Remix
        val url = "https://www.youtube.com/watch?v=$videoId"
        
        try {
            val info = StreamInfo.getInfo(yt, url)
            val resultText = buildString {
                appendLine("Video ID: $videoId")
                appendLine("Title: ${info.name}")
                appendLine("Class: ${info::class.java.name}")
                
                appendLine("\n=== STREAM METADATA ===")
                appendLine("  - DASH MPD URL: ${info.dashMpdUrl}")
                appendLine("  - HLS URL: ${info.hlsUrl}")
 
                appendLine("\n=== AUDIO STREAMS (${info.audioStreams.size}) ===")
                info.audioStreams.forEachIndexed { i, audio ->
                    appendLine("Stream $i: Bitrate=${audio.bitrate}, Format=${audio.format}, Codec=${audio.codec}, Content=${audio.content}")
                }
                
                appendLine("\n=== ERRORS (${info.getErrors().size}) ===")
                info.getErrors().forEachIndexed { i, err ->
                    appendLine("Error $i: ${err.message}\n${err.stackTraceToString()}")
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

    @Test
    fun testSearchMusic() = runBlocking {
        // Mock Android Log methods
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        val client = OkHttpClient.Builder().build()
        val downloader = NewPipeDownloader(client)
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT)
        val yt = ServiceList.YouTube
        
        try {
            val query = "trending music"
            val extractor = yt.getSearchExtractor(
                query,
                listOf("music_songs"),
                ""
            )
            extractor.fetchPage()
            val infoItems = extractor.initialPage.items
            val resultText = buildString {
                appendLine("=== searchMusic Results ===")
                infoItems.forEachIndexed { index, item ->
                    appendLine("Item $index:")
                    appendLine("  - Class: ${item::class.java.name}")
                    appendLine("  - Name: ${item.name}")
                    appendLine("  - Url: ${item.url}")
                    if (item is com.yushosei.newpipe.extractor.stream.StreamInfoItem) {
                        val extractedId = item.url.extractVideoId()
                        appendLine("  - Extracted ID: $extractedId")
                    }
                }
            }
            println(resultText)
            File("scratch").mkdirs()
            File("scratch/search_music_result.txt").writeText(resultText)
        } catch (e: Exception) {
            val err = "searchMusic failed: ${e.message}\n${e.stackTraceToString()}"
            println(err)
            File("scratch").mkdirs()
            File("scratch/search_music_result.txt").writeText(err)
        }
    }

    private fun String.extractVideoId(): String {
        val id = when {
            contains("v=") -> substringAfter("v=").substringBefore("&")
            contains("youtu.be/") -> substringAfterLast("/")
            contains("/shorts/") -> substringAfterLast("/")
            else -> this
        }.take(11)
        return id
    }
}
