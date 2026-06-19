package com.deepeye.musicpro.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.asImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HighResAlbumArtInterceptor(private val context: Context) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data

        if (data is Uri && data.toString().startsWith("content://media/external/audio/albumart/")) {
            val albumIdStr = data.lastPathSegment
            if (albumIdStr != null) {
                val albumId = albumIdStr.toLongOrNull()
                if (albumId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
                        // Request high quality thumbnail (1024x1024)
                        val bitmap = withContext(Dispatchers.IO) {
                            context.contentResolver.loadThumbnail(albumUri, Size(1024, 1024), null)
                        }
                        return SuccessResult(
                            image = bitmap.asImage(),
                            request = request,
                            dataSource = coil3.decode.DataSource.DISK
                        )
                    } catch (e: Exception) {
                        // Fallback to default if not found
                    }
                }
            }
        }
        
        return chain.proceed()
    }
}
