package com.deepeye.musicpro.ui

import android.util.Log
import com.deepeye.musicpro.extractor.SmartTubeInnertubeExtractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.Test
import java.io.File

class YoutubeExtractionTest {

    private fun mockLogger() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
    }

    @Test
    fun testStreamExtraction() = runBlocking {
        mockLogger()
        val client = OkHttpClient.Builder().build()
        val extractor = SmartTubeInnertubeExtractor(client)

        val videoId = "68RLvhxk_4g" // Bollywood Dj Non Stop Remix
        val result = extractor.extractStream(videoId, preferVideo = false)
        val resultText =
            buildString {
                appendLine("=== extractStream Result ===")
                if (result != null) {
                    appendLine("URL: ${result.url}")
                    appendLine("Container: ${result.container}")
                    appendLine("Quality: ${result.quality}")
                    appendLine("ExtractorName: ${result.extractorName}")
                } else {
                    appendLine("FAILED: result is null")
                }
            }
        println(resultText)
        File("scratch").mkdirs()
        File("scratch/smarttube_stream_result.txt").writeText(resultText)
    }

    @Test
    fun testSearch() = runBlocking {
        mockLogger()
        val client = OkHttpClient.Builder().build()
        val extractor = SmartTubeInnertubeExtractor(client)

        val page = extractor.searchVideosFirstPage("bollywood dj remix")
        val resultText =
            buildString {
                appendLine("=== searchVideosFirstPage Result ===")
                appendLine("count=${page.videos.size} hasNextPage=${page.nextPageUrl != null}")
                page.videos.take(8).forEachIndexed { i, v ->
                    appendLine("#$i id=${v.id} title=${v.title} artist=${v.artist} dur=${v.duration}s")
                }
            }
        println(resultText)
        File("scratch").mkdirs()
        File("scratch/smarttube_search_result.txt").writeText(resultText)
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    @Test
    fun testDataSourceFallback() {
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        try {
            runBlocking {
                mockLogger()
                val client = OkHttpClient.Builder().build()
                val context = mockk<android.content.Context>(relaxed = true)
                val rankingManager = mockk<com.deepeye.musicpro.diagnostics.ExtractionRankingManager>(relaxed = true)
                every { rankingManager.getRankedLayers() } returns listOf(
                    com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.SMARTTUBE,
                    com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.ALT_EXTRACTOR,
                    com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.PIPED,
                    com.deepeye.musicpro.diagnostics.ExtractionRankingManager.Layer.INVIDIOUS,
                )
                val headlessExtractor = mockk<com.deepeye.musicpro.data.source.remote.youtube.HeadlessWebViewExtractor>(relaxed = true)
                val settingsDataStore = mockk<com.deepeye.musicpro.data.prefs.SettingsDataStore>(relaxed = true)
                every { settingsDataStore.settings } returns kotlinx.coroutines.flow.flowOf(
                    com.deepeye.musicpro.data.prefs.AppSettings()
                )
                val dataSource = com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource(
                    context, client, rankingManager, headlessExtractor, settingsDataStore
                )

                val videoId = "68RLvhxk_4g"
                println("Starting getStreamUrl for $videoId (SmartTube layer should be first)...")
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
}