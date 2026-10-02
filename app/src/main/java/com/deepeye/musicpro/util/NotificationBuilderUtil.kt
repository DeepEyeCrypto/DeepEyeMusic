// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.deepeye.musicpro.MainActivity
import com.deepeye.musicpro.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object NotificationBuilderUtil {
    const val CHANNEL_ID = "youtube_subscriptions"
    private const val CHANNEL_NAME = "Channel Subscriptions"
    private const val CHANNEL_DESC = "Notifications for new videos and music uploads from subscribed channels"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    lightColor = android.graphics.Color.CYAN
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    suspend fun fetchBitmap(urlStr: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (urlStr.isNullOrBlank()) return@withContext null
        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.connect()
            connection.inputStream.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun showRichSubscriptionNotification(
        context: Context,
        videoId: String,
        channelName: String,
        videoTitle: String,
        thumbnailBitmap: Bitmap?
    ) {
        ensureNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_VIDEO_ID", videoId)
            putExtra("EXTRA_VIDEO_TITLE", videoTitle)
            putExtra("EXTRA_CHANNEL_NAME", channelName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            videoId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val iconRes = R.drawable.ic_notification.takeIf { it != 0 } ?: R.mipmap.ic_launcher

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(videoTitle)
            .setContentText("New upload from $channelName")
            .setSubText(channelName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (thumbnailBitmap != null) {
            builder.setLargeIcon(thumbnailBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(thumbnailBitmap)
                    .setSummaryText("New upload from $channelName")
            )
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(videoId.hashCode(), builder.build())
    }
}
