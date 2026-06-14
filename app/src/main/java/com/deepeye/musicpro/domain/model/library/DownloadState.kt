package com.deepeye.musicpro.domain.model.library

enum class DownloadState {
    NONE,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
}
