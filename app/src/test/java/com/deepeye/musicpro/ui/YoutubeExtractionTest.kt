package com.deepeye.musicpro.ui

import android.util.Log
import com.deepeye.musicpro.data.source.remote.youtube.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import okhttp3.OkHttpClient
import org.junit.Test
import java.io.File

class YoutubeExtractionTest {
    @Test
    fun testExtraction() =
        runBlocking {
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
                val resultText =
                    buildString {
                        appendLine("Video ID: $videoId")
                        appendLine("Title: ${info.name}")
                        appendLine("Class: ${info::class.java.name}")

                        appendLine("\n=== STREAM METADATA ===")
                        appendLine("  - DASH MPD URL: ${info.dashMpdUrl}")
                        appendLine("  - HLS URL: ${info.hlsUrl}")

                        appendLine("\n=== AUDIO STREAMS (${info.audioStreams.size}) ===")
                        info.audioStreams.forEachIndexed { i, audio ->
                            appendLine(
                                "Stream $i: Bitrate=${audio.bitrate}, Format=${audio.format}, " +
                                    "Codec=${audio.codec}, Content=${audio.content}",
                            )
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
    fun testSearchMusic() =
        runBlocking {
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
                val extractor =
                    yt.getSearchExtractor(
                        query,
                        listOf("music_songs"),
                        "",
                    )
                extractor.fetchPage()
                val infoItems = extractor.initialPage.items
                val resultText =
                    buildString {
                        appendLine("=== searchMusic Results ===")
                        infoItems.forEachIndexed { index, item ->
                            appendLine("Item $index:")
                            appendLine("  - Class: ${item::class.java.name}")
                            appendLine("  - Name: ${item.name}")
                            appendLine("  - Url: ${item.url}")
                            if (item is org.schabi.newpipe.extractor.stream.StreamInfoItem) {
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun testDataSourceFallback() {
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        try {
            runBlocking {
                // Mock Android Log methods to print to standard output
                mockkStatic(Log::class)
                every { Log.d(any<String>(), any<String>()) } answers { println("[DEBUG] ${arg<String>(0)}: ${arg<String>(1)}"); 0 }
                every { Log.e(any<String>(), any<String>()) } answers { println("[ERROR] ${arg<String>(0)}: ${arg<String>(1)}"); 0 }
                every { Log.e(any<String>(), any<String>(), any<Throwable>()) } answers { 
                    println("[ERROR] ${arg<String>(0)}: ${arg<String>(1)}")
                    arg<Throwable>(2).printStackTrace()
                    0 
                }
                every { Log.w(any<String>(), any<String>()) } answers { println("[WARN] ${arg<String>(0)}: ${arg<String>(1)}"); 0 }
                every { Log.i(any<String>(), any<String>()) } answers { println("[INFO] ${arg<String>(0)}: ${arg<String>(1)}"); 0 }

            val client = OkHttpClient.Builder().build()
            val downloader = NewPipeDownloader(client)
            val rankingManager = mockk<com.deepeye.musicpro.diagnostics.ExtractionRankingManager>(relaxed = true)
            every { rankingManager.getRankedLayers() } returns listOf(
                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.NEWPIPE,
                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.ALT_EXTRACTOR,
                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.PIPED,
                com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.INVIDIOUS
            )
            val headlessExtractor = mockk<com.deepeye.musicpro.data.source.remote.youtube.HeadlessWebViewExtractor>(relaxed = true)
            val dataSource = com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource(downloader, client, rankingManager, headlessExtractor)

            val videoId = "68RLvhxk_4g"
            println("Starting getStreamUrl for $videoId (should trigger fallback)...")
            val result = dataSource.getStreamUrl(videoId, preferVideo = false)
            val resultText = buildString {
                appendLine("=== getStreamUrl Result ===")
                if (result != null) {
                    appendLine("Success: ${result.url}")
                    appendLine("IsVideo: ${result.isVideo}")
                } else {
                    appendLine("Failed: Result is null")
                }
            }
            println(resultText)
            File("scratch").mkdirs()
            File("scratch/fallback_result.txt").writeText(resultText)
        }
    } finally {
        Dispatchers.resetMain()
    }
}

    private fun String.extractVideoId(): String {
        val id =
            when {
                contains("v=") -> substringAfter("v=").substringBefore("&")
                contains("youtu.be/") -> substringAfterLast("/")
                contains("/shorts/") -> substringAfterLast("/")
                else -> this
            }.take(11)
        return id
    }
}
