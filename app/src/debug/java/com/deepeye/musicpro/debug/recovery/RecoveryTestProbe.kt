// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.debug.recovery

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe probe that records recovery lifecycle events.
 * Debug-only: enabled only in debug builds via Hilt.
 */
class RecoveryTestProbe {
    private val mutex = Mutex()
    
    private var currentRunId: String? = null
    private var eventSequence: Int = 0
    private var currentStage: RecoveryProbeStage = RecoveryProbeStage.IDLE
    
    private val maxEventHistory = 200
    private val eventHistory = mutableListOf<RecoveryProbeEvent>()
    
    private var preRecoverySnapshot: RecoveryProbeSnapshot? = null
    private var postRecoverySnapshot: RecoveryProbeSnapshot? = null
    
    suspend fun startRun(baseline: RecoveryProbeSnapshot) {
        mutex.withLock {
            currentRunId = UUID.randomUUID().toString().take(8)
            eventSequence = 0
            currentStage = RecoveryProbeStage.BASELINE_CAPTURED
            preRecoverySnapshot = baseline
            postRecoverySnapshot = null
            eventHistory.clear()
            
            Log.d("RecoveryProbe", "Started run ${currentRunId}: mediaHash=${baseline.mediaIdHash}, pos=${baseline.positionMs}ms")
        }
    }
    
    suspend fun recordEvent(
        stage: RecoveryProbeStage,
        mediaIdHash: String? = null,
        titleSafe: String? = null,
        queueIndex: Int? = null,
        queueSize: Int? = null,
        positionMs: Long? = null,
        playbackState: String? = null,
        wasPlaying: Boolean? = null,
        selectedVideoFormatIdSafe: String? = null,
        selectedAudioFormatIdSafe: String? = null,
        videoFormatCount: Int? = null,
        audioFormatCount: Int? = null,
        dspEnabled: Boolean? = null,
        dspPresetSafe: String? = null,
        recoveryAttempt: Int? = null,
        recoveryLockHeld: Boolean? = null,
        fallbackUsed: Boolean? = null,
        fallbackReason: String? = null,
        safeMessage: String? = null
    ) {
        mutex.withLock {
            if (currentRunId == null) return@withLock
            
            eventSequence++
            val event = RecoveryProbeEvent(
                runId = currentRunId!!,
                sequenceNumber = eventSequence,
                timestampMs = System.currentTimeMillis(),
                stage = stage,
                mediaIdHash = mediaIdHash,
                titleSafe = titleSafe,
                queueIndex = queueIndex,
                queueSize = queueSize,
                positionMs = positionMs,
                playbackState = playbackState,
                wasPlaying = wasPlaying,
                selectedVideoFormatIdSafe = selectedVideoFormatIdSafe,
                selectedAudioFormatIdSafe = selectedAudioFormatIdSafe,
                videoFormatCount = videoFormatCount,
                audioFormatCount = audioFormatCount,
                dspEnabled = dspEnabled,
                dspPresetSafe = dspPresetSafe,
                recoveryAttempt = recoveryAttempt,
                recoveryLockHeld = recoveryLockHeld,
                fallbackUsed = fallbackUsed,
                fallbackReason = fallbackReason,
                safeMessage = safeMessage
            )
            
            eventHistory.add(event)
            if (eventHistory.size > maxEventHistory) eventHistory.removeAt(0)
            currentStage = stage
        }
    }
    
    suspend fun recordPostRecoverySnapshot(snapshot: RecoveryProbeSnapshot) {
        mutex.withLock { postRecoverySnapshot = snapshot }
    }
    
    suspend fun dumpEventsAsJsonLines(): String = mutex.withLock {
        eventHistory.joinToString("\n") { it.toJsonLine() }
    }
    
    suspend fun generateVerdict(): String = mutex.withLock {
        val runId = currentRunId ?: "NO_RUN"
        val lastStage = currentStage
        val pre = preRecoverySnapshot
        val post = postRecoverySnapshot
        
        val posRestored = if (pre != null && post != null) {
            kotlin.math.abs((post.positionMs ?: 0L) - (pre.positionMs ?: 0L)) < 30_000
        } else false
        
        val playRestored = (pre?.wasPlaying == post?.wasPlaying)
        val hqRetained = (pre?.videoFormatCount ?: 0) > 0 && (post?.videoFormatCount ?: 0) > 0
        val dspPreserved = (pre?.dspEnabled == post?.dspEnabled)
        val queuePreserved = (pre?.queueSize == post?.queueSize)
        
        val finalResult = when (lastStage) {
            RecoveryProbeStage.COMPLETED -> "PASS"
            RecoveryProbeStage.FAILED -> "FAIL"
            else -> "INCOMPLETE"
        }
        
        buildString {
            append("=== Recovery Test Probe Verdict ===\n")
            append("Run: $runId | Stage: $lastStage | Result: $finalResult\n")
            append("Position Restored: $posRestored | PlayState: $playRestored\n")
            append("HQ Retained: $hqRetained | DSP Preserved: $dspPreserved\n")
            append("Queue Preserved: $queuePreserved | Events: ${eventHistory.size}\n")
        }
    }
}
