package com.deepeye.musicpro.player.smarttube

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SmartTubeDashManifestGeneratorTest {

    @Test
    fun testGenerateDashManifest_prioritizesMp4SidxStreams() {
        val streamingData = JSONObject().apply {
            val adaptive = JSONArray().apply {
                put(JSONObject().apply {
                    put("itag", 137)
                    put("url", "https://rr.googlevideo.com/videoplayback?itag=137&fps=60")
                    put("mimeType", "video/mp4; codecs=\"avc1.64002a\"")
                    put("bitrate", 5816758)
                    put("width", 1920)
                    put("height", 1080)
                    put("fps", 60)
                    put("approxDurationMs", 941333)
                    put("initRange", JSONObject().apply { put("start", 0); put("end", 741) })
                    put("indexRange", JSONObject().apply { put("start", 742); put("end", 2921) })
                })
                put(JSONObject().apply {
                    put("itag", 248)
                    put("url", "https://rr.googlevideo.com/videoplayback?itag=248")
                    put("mimeType", "video/webm; codecs=\"vp9\"")
                    put("bitrate", 2800000)
                    put("width", 1280)
                    put("height", 720)
                    put("fps", 30)
                    put("approxDurationMs", 941333)
                    put("initRange", JSONObject().apply { put("start", 0); put("end", 219) })
                    put("indexRange", JSONObject().apply { put("start", 220); put("end", 1010) })
                })
                put(JSONObject().apply {
                    put("itag", 140)
                    put("url", "https://rr.googlevideo.com/videoplayback?itag=140")
                    put("mimeType", "audio/mp4; codecs=\"mp4a.40.2\"")
                    put("bitrate", 128000)
                    put("audioSampleRate", "44100")
                    put("approxDurationMs", 941333)
                    put("initRange", JSONObject().apply { put("start", 0); put("end", 722) })
                    put("indexRange", JSONObject().apply { put("start", 723); put("end", 1042) })
                })
                put(JSONObject().apply {
                    put("itag", 251)
                    put("url", "https://rr.googlevideo.com/videoplayback?itag=251")
                    put("mimeType", "audio/webm; codecs=\"opus\"")
                    put("bitrate", 160000)
                    put("audioSampleRate", "48000")
                    put("approxDurationMs", 941333)
                    put("initRange", JSONObject().apply { put("start", 0); put("end", 265) })
                    put("indexRange", JSONObject().apply { put("start", 266); put("end", 1880) })
                })
            }
            put("adaptiveFormats", adaptive)
        }

        val xml = SmartTubeDashManifestGenerator.generateDashManifest(streamingData) { it.optString("url") }
        assertNotNull(xml)
        assertTrue(xml!!.contains("<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\""))
        assertTrue(xml.contains("contentType=\"video\""))
        assertTrue(xml.contains("contentType=\"audio\""))
        assertTrue(xml.contains("id=\"137\""))
        assertTrue(xml.contains("id=\"140\""))
        // WebM is skipped in favor of MP4 to prevent 59s sidx parsing stalls
        assertFalse(xml.contains("id=\"251\""))
        assertFalse(xml.contains("id=\"248\""))

        val dataUri = SmartTubeDashManifestGenerator.generateDashDataUri(streamingData) { it.optString("url") }
        assertNotNull(dataUri)
        assertTrue(dataUri!!.startsWith("data:application/dash+xml;charset=utf-8;base64,"))
    }

    @Test
    fun testGenerateDashManifest_fallbackWhenOnlyWebmAvailable() {
        val streamingData = JSONObject().apply {
            val adaptive = JSONArray().apply {
                put(JSONObject().apply {
                    put("itag", 248)
                    put("url", "https://rr.googlevideo.com/videoplayback?itag=248")
                    put("mimeType", "video/webm; codecs=\"vp9\"")
                    put("bitrate", 2800000)
                    put("width", 1280)
                    put("height", 720)
                    put("fps", 30)
                    put("approxDurationMs", 941333)
                    put("initRange", JSONObject().apply { put("start", 0); put("end", 219) })
                    put("indexRange", JSONObject().apply { put("start", 220); put("end", 1010) })
                })
                put(JSONObject().apply {
                    put("itag", 251)
                    put("url", "https://rr.googlevideo.com/videoplayback?itag=251")
                    put("mimeType", "audio/webm; codecs=\"opus\"")
                    put("bitrate", 160000)
                    put("audioSampleRate", "48000")
                    put("approxDurationMs", 941333)
                    put("initRange", JSONObject().apply { put("start", 0); put("end", 265) })
                    put("indexRange", JSONObject().apply { put("start", 266); put("end", 1880) })
                })
            }
            put("adaptiveFormats", adaptive)
        }

        val xml = SmartTubeDashManifestGenerator.generateDashManifest(streamingData) { it.optString("url") }
        assertNotNull(xml)
        assertTrue(xml!!.contains("id=\"248\""))
        assertTrue(xml.contains("id=\"251\""))
    }
}
