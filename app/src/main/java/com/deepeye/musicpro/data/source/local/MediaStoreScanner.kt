package com.deepeye.musicpro.data.source.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.deepeye.musicpro.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans the device's MediaStore for audio files and returns [SongEntity] objects.
 *
 * Handles Android version-specific differences:
 * - API 29+ (Q): Uses MediaStore.Audio.Media.RELATIVE_PATH
 * - API 30+ (R): Uses volume-aware content URIs
 * - API 33+ (Tiramisu): Requires READ_MEDIA_AUDIO permission
 */
@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")
    }

    /**
     * Scans MediaStore for all audio files and returns a list of [SongEntity].
     * Runs on [Dispatchers.IO].
     */
    suspend fun scanAllSongs(): List<SongEntity> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongEntity>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        // Filter out short audio files (ringtones, notifications) — min 30 seconds
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} >= 30000"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)

                songs.add(
                    SongEntity(
                        id = id,
                        title = cursor.getString(titleColumn) ?: "Unknown",
                        artist = cursor.getString(artistColumn) ?: "Unknown Artist",
                        album = cursor.getString(albumColumn) ?: "Unknown Album",
                        albumId = albumId,
                        artistId = cursor.getLong(artistIdColumn),
                        uri = contentUri.toString(),
                        duration = cursor.getLong(durationColumn),
                        size = cursor.getLong(sizeColumn),
                        path = cursor.getString(dataColumn) ?: "",
                        artUri = albumArtUri.toString(),
                        trackNumber = cursor.getInt(trackColumn),
                        year = cursor.getInt(yearColumn),
                        genre = "",  // Genre requires a separate MediaStore query
                        dateAdded = cursor.getLong(dateAddedColumn),
                        dateModified = cursor.getLong(dateModifiedColumn)
                    )
                )
            }
        }

        songs
    }
}
