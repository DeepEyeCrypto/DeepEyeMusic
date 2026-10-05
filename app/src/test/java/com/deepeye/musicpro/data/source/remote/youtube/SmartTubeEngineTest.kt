// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

import android.content.Context
import com.deepeye.musicpro.domain.auth.InnerTubeAuthManager
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartTubeEngineTest {

    private lateinit var engine: SmartTubeEngine
    private val context: Context = mockk(relaxed = true)
    private val authManager: InnerTubeAuthManager = mockk(relaxed = true)
    private val okHttpClient: OkHttpClient = mockk(relaxed = true)

    @Before
    fun setup() {
        engine = SmartTubeEngine(context, authManager, okHttpClient)
    }

    @Test
    fun verifyTvHtml5PayloadContainsRequiredSmartTubeContext() {
        val contextJson = "{" + SmartTubeEngine.TVHTML5_CONTEXT + "}"
        val parsed = JSONObject(contextJson)
        val client = parsed.getJSONObject("context").getJSONObject("client")

        assertEquals("TVHTML5", client.getString("clientName"))
        assertEquals("7.20210614.03.00", client.getString("clientVersion"))
        assertTrue(client.getString("userAgent").contains("SMART-TV"))
    }

    @Test
    fun extractAutoplayFromSampleAutoplayOverlayRenderer() {
        val sampleJson = JSONObject(
            """
            {
              "playerOverlays": {
                "playerOverlayRenderer": {
                  "autoplay": {
                    "playerOverlayAutoplayRenderer": {
                      "videoTitle": { "runs": [{ "text": "Arijit Singh Mix" }] },
                      "byline": { "runs": [{ "text": "Arijit Singh" }] },
                      "videoEndpoint": {
                        "watchEndpoint": {
                          "videoId": "SAMPLE_AUTO_ID_123"
                        }
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        // Reflection check on private method for thorough testing
        val method = SmartTubeEngine::class.java.getDeclaredMethod("extractAutoplayFromJson", JSONObject::class.java, String::class.java)
        method.isAccessible = true
        val result = method.invoke(engine, sampleJson, "CURRENT_ID") as? AutoplayTrack

        assertNotNull(result)
        assertEquals("SAMPLE_AUTO_ID_123", result?.videoId)
        assertEquals("Arijit Singh Mix", result?.title)
        assertEquals("Arijit Singh", result?.artist)
    }
}
