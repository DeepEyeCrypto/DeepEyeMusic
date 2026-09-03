// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.recovery

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamRecoveryPolicyTest {

    private val policy = StreamRecoveryPolicy()
    private val mapper = PlaybackRecoveryErrorMapper()

    @Test
    fun testHttpStatus403_decidesRefreshSource() {
        val error = PlaybackRecoveryError.HttpStatus(403)
        val decision = policy.decide(error, currentAttemptCount = 0)
        assertEquals(RecoveryDecision.REFRESH_SOURCE, decision)
        assertTrue(policy.shouldRecover(error, currentAttemptCount = 0))
    }

    @Test
    fun testHttpStatus410_decidesRefreshSource() {
        val error = PlaybackRecoveryError.HttpStatus(410)
        val decision = policy.decide(error, currentAttemptCount = 0)
        assertEquals(RecoveryDecision.REFRESH_SOURCE, decision)
        assertTrue(policy.shouldRecover(error, currentAttemptCount = 0))
    }

    @Test
    fun testHttpStatus401_decidesRefreshSource() {
        val error = PlaybackRecoveryError.HttpStatus(401)
        val decision = policy.decide(error, currentAttemptCount = 0)
        assertEquals(RecoveryDecision.REFRESH_SOURCE, decision)
        assertTrue(policy.shouldRecover(error, currentAttemptCount = 0))
    }

    @Test
    fun testNonRecoverableHttpStatus_doesNotRefresh() {
        val error404 = PlaybackRecoveryError.HttpStatus(404)
        val decision404 = policy.decide(error404, currentAttemptCount = 0)
        assertEquals(RecoveryDecision.SHOW_ERROR, decision404)
        assertFalse(policy.shouldRecover(error404, currentAttemptCount = 0))

        val error400 = PlaybackRecoveryError.HttpStatus(400)
        val decision400 = policy.decide(error400, currentAttemptCount = 0)
        assertEquals(RecoveryDecision.SHOW_ERROR, decision400)
        assertFalse(policy.shouldRecover(error400, currentAttemptCount = 0))
    }

    @Test
    fun testServerErrors5xx_decidesRetryOnce() {
        val error500 = PlaybackRecoveryError.HttpStatus(500)
        val decision500 = policy.decide(error500, currentAttemptCount = 0)
        assertEquals(RecoveryDecision.RETRY_ONCE, decision500)
        assertTrue(policy.shouldRecover(error500, currentAttemptCount = 0))
    }

    @Test
    fun testSourceLoadAndDecoderErrors() {
        val srcError = PlaybackRecoveryError.SourceLoad("Timeout")
        assertEquals(RecoveryDecision.REFRESH_SOURCE, policy.decide(srcError, 0))
        assertTrue(policy.shouldRecover(srcError, 0))

        val decError = PlaybackRecoveryError.Decoder("Init failed")
        assertEquals(RecoveryDecision.FALLBACK_FORMAT, policy.decide(decError, 0))
        assertTrue(policy.shouldRecover(decError, 0))
    }

    @Test
    fun testConfigurationConstants() {
        assertEquals(1, StreamRecoveryPolicy.MAX_RECOVERY_ATTEMPTS)
        assertEquals(60_000L, StreamRecoveryPolicy.STABLE_PLAYBACK_RESET_MS)
    }

    @Test
    fun testExceededAttempts_decidesDoNotRetry() {
        val error = PlaybackRecoveryError.HttpStatus(403)
        val decision = policy.decide(error, currentAttemptCount = 1)
        assertEquals(RecoveryDecision.DO_NOT_RETRY, decision)
        assertFalse(policy.shouldRecover(error, currentAttemptCount = 1))
    }

    @Test
    fun testErrorMapper_maps403Code() {
        val exception = PlaybackException(
            "Response code: 403",
            null,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        )
        val mapped = mapper.map(exception)
        assertTrue(mapped is PlaybackRecoveryError.HttpStatus)
        assertEquals(403, (mapped as PlaybackRecoveryError.HttpStatus).statusCode)
    }

    @Test
    fun testErrorMapper_mapsDecoderFailure() {
        val exception = PlaybackException(
            "Decoder init error",
            null,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
        )
        val mapped = mapper.map(exception)
        assertTrue(mapped is PlaybackRecoveryError.Decoder)
    }

    @Test
    fun testErrorMapper_maps401And410FromMessage() {
        val ex401 = PlaybackException("HTTP 401 Unauthorized", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        val mapped401 = mapper.map(ex401)
        assertTrue(mapped401 is PlaybackRecoveryError.HttpStatus)
        assertEquals(401, (mapped401 as PlaybackRecoveryError.HttpStatus).statusCode)

        val ex410 = PlaybackException("HTTP 410 Gone", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        val mapped410 = mapper.map(ex410)
        assertTrue(mapped410 is PlaybackRecoveryError.HttpStatus)
        assertEquals(410, (mapped410 as PlaybackRecoveryError.HttpStatus).statusCode)
    }

    @Test
    fun testErrorMapper_mapsNetworkTimeout() {
        val exTimeout = PlaybackException("Network connection timeout", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
        val mapped = mapper.map(exTimeout)
        assertTrue(mapped is PlaybackRecoveryError.SourceLoad)
    }
}
