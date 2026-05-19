package com.deepeye.musicpro.player.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import com.deepeye.musicpro.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    init {
        // Register receiver for download completion to scan the file into MediaStore
        val filter = android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        context.registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: android.content.Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != -1L) {
                    val query = DownloadManager.Query().setFilterById(id)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val fileUri = Uri.parse(cursor.getString(uriIdx))
                            fileUri.path?.let { path ->
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                            }
                        }
                    }
                    cursor.close()
                }
            }
        }, filter, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Context.RECEIVER_EXPORTED else 0)
    }

    fun downloadTrack(item: MediaItem) {
        val streamUri = when (item) {
            is MediaItem.Remote -> item.streamUri
            is MediaItem.Local -> {
                Toast.makeText(context, "Track already in local library", Toast.LENGTH_SHORT).show()
                return
            }
        } ?: return

        try {
            val sanitizedTitle = item.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val request = DownloadManager.Request(streamUri)
                .setTitle(item.title)
                .setDescription("Downloading ${item.artist}")
                .setMimeType("audio/mpeg")
                .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "DeepEyeMusic/$sanitizedTitle.mp3")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started: ${item.title}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
