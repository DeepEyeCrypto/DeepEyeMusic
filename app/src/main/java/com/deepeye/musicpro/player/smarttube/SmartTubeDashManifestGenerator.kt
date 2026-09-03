package com.deepeye.musicpro.player.smarttube

import org.json.JSONObject
import java.util.Base64
import java.util.Locale

object SmartTubeDashManifestGenerator {
    fun generateDashManifest(
        streamingData: JSONObject,
        urlResolver: (JSONObject) -> String?
    ): String? {
        val adaptive = streamingData.optJSONArray("adaptiveFormats") ?: return null
        if (adaptive.length() == 0) return null

        val videoFormats = mutableListOf<AdaptiveFormatItem>()
        val audioFormats = mutableListOf<AdaptiveFormatItem>()
        var maxDurationSec = 0.0

        for (i in 0 until adaptive.length()) {
            val f = adaptive.optJSONObject(i) ?: continue
            val url = urlResolver(f)?.takeIf { it.isNotBlank() } ?: continue
            val initRange = f.optJSONObject("initRange")
            val indexRange = f.optJSONObject("indexRange")
            if (initRange == null || indexRange == null) continue
            val initStart = initRange.optLong("start", -1L)
            val initEnd = initRange.optLong("end", -1L)
            val idxStart = indexRange.optLong("start", -1L)
            val idxEnd = indexRange.optLong("end", -1L)
            if (initStart < 0 || initEnd < 0 || idxStart < 0 || idxEnd < 0) continue
            val itag = f.optInt("itag", -1)
            if (itag <= 0) continue
            val mimeType = f.optString("mimeType", "")
            val bitrate = f.optLong("bitrate", 0L).takeIf { it > 0 } ?: f.optLong("averageBitrate", 0L)
            val approxDurMs = f.optLong("approxDurationMs", 0L)
            val durSec = if (approxDurMs > 0) approxDurMs / 1000.0 else 0.0
            if (durSec > maxDurationSec) maxDurationSec = durSec
            val codecs = if (mimeType.contains("codecs=\"")) mimeType.substringAfter("codecs=\"").substringBefore("\"") else ""
            val cleanMime = mimeType.substringBefore(";").trim()
            val item = AdaptiveFormatItem(itag, url, cleanMime, codecs, bitrate, f.optInt("width", 0), f.optInt("height", 0), f.optDouble("fps", 30.0).toFloat(), f.optString("audioSampleRate").ifEmpty { "44100" }, "$initStart-$initEnd", "$idxStart-$idxEnd")
            
            if (cleanMime.startsWith("video/")) videoFormats.add(item)
            else if (cleanMime.startsWith("audio/")) audioFormats.add(item)
        }

        if (videoFormats.isEmpty() && audioFormats.isEmpty()) return null
        if (maxDurationSec <= 0.0) maxDurationSec = 300.0
        val durationAttr = String.format(Locale.US, "PT%.3fS", maxDurationSec)

        val sb = java.lang.StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\" profiles=\"urn:mpeg:dash:profile:isoff-on-demand:2011\" type=\"static\" mediaPresentationDuration=\"$durationAttr\" minBufferTime=\"PT1.5S\">\n")
        sb.append("  <Period duration=\"$durationAttr\">\n")

        val videoGroups = videoFormats.groupBy { it.mimeType }
        for ((mime, items) in videoGroups) {
            sb.append("    <AdaptationSet mimeType=\"$mime\" contentType=\"video\" subsegmentAlignment=\"true\" subsegmentStartsWithSAP=\"1\">\n")
            for (v in items) {
                val fpsAttr = if (v.frameRate > 0) " frameRate=\"${v.frameRate.toInt()}\"" else ""
                val dimAttr = if (v.width > 0 && v.height > 0) " width=\"${v.width}\" height=\"${v.height}\"" else ""
                val codecAttr = if (v.codecs.isNotBlank()) " codecs=\"${escapeXml(v.codecs)}\"" else ""
                sb.append("      <Representation id=\"${v.itag}\" bandwidth=\"${v.bitrate}\"$dimAttr$fpsAttr$codecAttr>\n")
                sb.append("        <BaseURL>${escapeXml(v.url)}</BaseURL>\n")
                sb.append("        <SegmentBase indexRange=\"${v.indexRange}\">\n")
                sb.append("          <Initialization range=\"${v.initRange}\"/>\n")
                sb.append("        </SegmentBase>\n")
                sb.append("      </Representation>\n")
            }
            sb.append("    </AdaptationSet>\n")
        }

        val audioGroups = audioFormats.groupBy { it.mimeType }
        for ((mime, items) in audioGroups) {
            sb.append("    <AdaptationSet mimeType=\"$mime\" contentType=\"audio\" subsegmentAlignment=\"true\" subsegmentStartsWithSAP=\"1\">\n")
            for (a in items) {
                val codecAttr = if (a.codecs.isNotBlank()) " codecs=\"${escapeXml(a.codecs)}\"" else ""
                sb.append("      <Representation id=\"${a.itag}\" bandwidth=\"${a.bitrate}\" audioSamplingRate=\"${a.audioSampleRate}\"$codecAttr>\n")
                sb.append("        <BaseURL>${escapeXml(a.url)}</BaseURL>\n")
                sb.append("        <SegmentBase indexRange=\"${a.indexRange}\">\n")
                sb.append("          <Initialization range=\"${a.initRange}\"/>\n")
                sb.append("        </SegmentBase>\n")
                sb.append("      </Representation>\n")
            }
            sb.append("    </AdaptationSet>\n")
        }

        sb.append("  </Period>\n</MPD>")
        return sb.toString()
    }

    fun generateDashDataUri(
        streamingData: JSONObject,
        urlResolver: (JSONObject) -> String?
    ): String? {
        val xml = generateDashManifest(streamingData, urlResolver) ?: return null
        val base64 = Base64.getEncoder().encodeToString(xml.toByteArray(Charsets.UTF_8))
        return "data:application/dash+xml;charset=utf-8;base64,$base64"
    }

    private fun escapeXml(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private data class AdaptiveFormatItem(val itag: Int, val url: String, val mimeType: String, val codecs: String, val bitrate: Long, val width: Int, val height: Int, val frameRate: Float, val audioSampleRate: String, val initRange: String, val indexRange: String)
}