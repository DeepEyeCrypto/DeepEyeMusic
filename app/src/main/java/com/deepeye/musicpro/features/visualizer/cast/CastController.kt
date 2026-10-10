// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import android.util.Log

/**
 * CastController — Manages Chromecast session lifecycle for visualizer streaming.
 */
class CastController(context: Context) {
    private val castContext = CastContext.getSharedInstance(context)
    private var castSession: CastSession? = null

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            Log.d("CastController", "Cast session started: $sessionId")
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
            Log.d("CastController", "Cast session ended")
        }

        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) { castSession = session }
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
    }

    init {
        castContext.sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }

    fun isCasting(): Boolean = castSession != null

    fun release() {
        castContext.sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }
}
