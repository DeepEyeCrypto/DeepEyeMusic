package com.deepeye.musicpro.domain.model

data class VideoDetails(
    val viewCount: Long = 0,
    val subscriberCount: Long = 0,
    val uploadDate: String = "",
    val channelName: String = "",
    val channelAvatarUrl: String = ""
)
