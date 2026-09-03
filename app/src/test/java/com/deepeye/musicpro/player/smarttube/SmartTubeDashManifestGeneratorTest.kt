package com.deepeye.musicpro.player.smarttube

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SmartTubeDashManifestGeneratorTest {

    @Test
    fun testGenerateDashManifest_createsValidXml() {
        val streamingData = JSONObject().apply {
            val adaptive = JSONArray().apply {
                put(JSONObject().apply {
                    put("itag", 299)
                    put("url", "https://rr.googlevideo.com/videoplayback?itag=299&fps=60")
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
                    put("itag", 298)
                    put("url", "https://rr.googlevideo.com/videoplayback?itag=298&fps=60")
                    put("mimeType", "video/mp4; codecs=\"avc1.4d4020\"")
                    put("bitrate", 2800000)
                    put("width", 1280)
                    put("height", 720)
                    put("fps", 60)
                    put("approxDurationMs", 941333)
                    put("initRange", JSONObject().apply { put("start", 0); put("end", 739) })
                    put("indexRange", JSONObject().apply { put("start", 740); put("end", 2919) })
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
        assertTrue(xml.contains("id=\"299\""))
        assertTrue(xml.contains("width=\"1920\" height=\"1080\" frameRate=\"60\""))
        assertTrue(xml.contains("id=\"298\""))
        assertTrue(xml.contains("width=\"1280\" height=\"720\" frameRate=\"60\""))
        assertTrue(xml.contains("id=\"251\""))
        assertTrue(xml.contains("audioSamplingRate=\"48000\""))
        assertTrue(xml.contains("<Initialization range=\"0-741\"/>"))
        assertTrue(xml.contains("<SegmentBase indexRange=\"742-2921\">"))

        val dataUri = SmartTubeDashManifestGenerator.generateDashDataUri(streamingData) { it.optString("url") }
        assertNotNull(dataUri)
        assertTrue(dataUri!!.startsWith("data:application/dash+xml;charset=utf-8;base64,"))
    }
}
