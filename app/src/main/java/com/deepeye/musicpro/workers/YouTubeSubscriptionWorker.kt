// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.data.repository.notification.NotificationStateRepo
import com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient
import com.deepeye.musicpro.data.source.remote.youtube.MusicFilter
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.repository.library.LibraryRepository
import com.deepeye.musicpro.util.NotificationBuilderUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Battery-optimized background worker that polls for new uploads from subscribed YouTube channels.
 * Adheres strictly to Zero-Shorts policy, prevents notification spam, and renders rich BigPicture notifications.
 */
@HiltWorker
class YouTubeSubscriptionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationStateRepo: NotificationStateRepo,
    private val authenticatedYouTubeClient: AuthenticatedYouTubeClient,
    private val libraryRepository: LibraryRepository,
    private val youtubeDataSource: YoutubeRemoteDataSource,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (!notificationStateRepo.isNotificationsEnabled()) {
                Log.d(TAG, "Notifications disabled by user preference.")
                return@withContext Result.success()
            }

            NotificationBuilderUtil.ensureNotificationChannel(context)

            val settings = settingsDataStore.settings.first()
            val isAuthenticated = !settings.youtubeAccessToken.isNullOrBlank()

            var newUploadsFound = 0

            // 1. If Authenticated with YouTube: Fetch authentic Subscriptions Feed
            if (isAuthenticated) {
                try {
                    val feedVideos = authenticatedYouTubeClient.getSubscriptionsFeed()
                    Log.d(TAG, "Fetched ${feedVideos.size} videos from authentic YouTube Subscriptions Feed")

                    for (video in feedVideos) {
                        if (newUploadsFound >= MAX_NOTIFICATIONS_PER_RUN) break

                        // Strictly filter out shorts
                        if (MusicFilter.isShort(video.title, video.duration, video.isShort)) {
                            continue
                        }

                        val videoId = video.id
                        if (videoId.isBlank() || notificationStateRepo.isVideoNotified(videoId)) {
                            continue
                        }

                        // Download thumbnail bitmap for BigPictureStyle notification
                        val bitmap = NotificationBuilderUtil.fetchBitmap(video.thumbnailUrl)
                        NotificationBuilderUtil.showRichSubscriptionNotification(
                            context = context,
                            videoId = videoId,
                            channelName = video.channelName.ifBlank { "Subscribed Channel" },
                            videoTitle = video.title,
                            thumbnailBitmap = bitmap
                        )

                        notificationStateRepo.markVideoNotified(videoId, video.channelName, video.title)
                        newUploadsFound++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking authenticated subscriptions feed", e)
                }
            }

            // 2. Also check local subscribed channels from LibraryRepository
            val localChannels = libraryRepository.getAllSubscribedChannels()
            if (localChannels.isNotEmpty()) {
                for (channel in localChannels) {
                    if (newUploadsFound >= MAX_NOTIFICATIONS_PER_RUN) break

                    try {
                        val result = youtubeDataSource.searchVideosFirstPage(channel.channelName)
                        val latestVideo = result.items.firstOrNull { item ->
                            item.channelName.contains(channel.channelName, ignoreCase = true) &&
                                !MusicFilter.isShort(item.title, item.duration, item.isShort)
                        }

                        if (latestVideo != null && latestVideo.id != channel.lastSeenVideoId) {
                            val videoId = latestVideo.id
                            if (!notificationStateRepo.isVideoNotified(videoId)) {
                                val bitmap = NotificationBuilderUtil.fetchBitmap(latestVideo.thumbnailUrl)
                                NotificationBuilderUtil.showRichSubscriptionNotification(
                                    context = context,
                                    videoId = videoId,
                                    channelName = channel.channelName,
                                    videoTitle = latestVideo.title,
                                    thumbnailBitmap = bitmap
                                )

                                notificationStateRepo.markVideoNotified(videoId, channel.channelName, latestVideo.title)
                                libraryRepository.updateLastSeenVideo(channel.channelId, videoId)
                                newUploadsFound++
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking local channel ${channel.channelName}", e)
                    }
                }
            }

            Log.i(TAG, "YouTubeSubscriptionWorker completed successfully. New notifications sent: $newUploadsFound")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in YouTubeSubscriptionWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "YTSubWorker"
        private const val MAX_NOTIFICATIONS_PER_RUN = 5
    }
}
