// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deepeye.musicpro.MainActivity
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.repository.library.LibraryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ChannelSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val libraryRepository: LibraryRepository,
    private val youtubeDataSource: YoutubeRemoteDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val channels = libraryRepository.getAllSubscribedChannels()
            if (channels.isEmpty()) return Result.success()

            createNotificationChannel()

            for (channel in channels) {
                // Fetch the latest videos by searching for the channel
                val result = youtubeDataSource.searchVideosFirstPage(channel.channelName)
                val latestVideo = result.items.firstOrNull { it.channelName.contains(channel.channelName, ignoreCase = true) }
                
                if (latestVideo != null && latestVideo.id != channel.lastSeenVideoId) {
                    // Naya video mila! Send Push Notification
                    sendNotification(channel.channelName, latestVideo.title, latestVideo.id)
                    // Update database so we don't notify for this video again
                    libraryRepository.updateLastSeenVideo(channel.channelId, latestVideo.id)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Channel Subscriptions"
            val descriptionText = "Notifications for new videos from subscribed channels"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("CHANNEL_SUBS", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(channelName: String, videoTitle: String, videoId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_VIDEO_ID", videoId)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, videoId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, "CHANNEL_SUBS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New video from $channelName")
            .setContentText(videoTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(videoId.hashCode(), builder.build())
    }
}
