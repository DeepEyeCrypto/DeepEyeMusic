# DeepEyeMusicPro — API Contracts

## Repository Interfaces

### MusicRepository
```kotlin
fun getAllSongs(): Flow<List<Song>>
fun getSongById(id: Long): Flow<Song?>
fun getSongsByAlbum(albumId: Long): Flow<List<Song>>
fun getSongsByArtist(artistId: Long): Flow<List<Song>>
fun getSongsByGenre(genreId: Long): Flow<List<Song>>
fun searchSongs(query: String): Flow<List<Song>>
fun getRecentlyAdded(limit: Int = 20): Flow<List<Song>>
fun getAllAlbums(): Flow<List<Album>>
fun getAlbumById(id: Long): Flow<Album?>
fun getAllArtists(): Flow<List<Artist>>
fun getArtistById(id: Long): Flow<Artist?>
fun getAllGenres(): Flow<List<Genre>>
suspend fun syncFromMediaStore()
```

### PlaylistRepository
```kotlin
fun getAllPlaylists(): Flow<List<Playlist>>
fun getPlaylistById(id: Long): Flow<Playlist?>
fun getPlaylistSongs(playlistId: Long): Flow<List<Song>>
suspend fun createPlaylist(name: String): Long
suspend fun renamePlaylist(playlistId: Long, newName: String)
suspend fun deletePlaylist(playlistId: Long)
suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
suspend fun reorderPlaylistSong(playlistId: Long, fromIndex: Int, toIndex: Int)
```

## Navigation Routes

| Route                     | Screen              | Params       |
|--------------------------|---------------------|--------------|
| `home`                   | HomeScreen          | —            |
| `library`                | LibraryScreen       | —            |
| `search`                 | SearchScreen        | —            |
| `settings`               | SettingsScreen      | —            |
| `now_playing`            | NowPlayingScreen    | —            |
| `v4a`                    | V4AScreen           | —            |
| `album/{albumId}`        | AlbumDetailScreen   | albumId: Long |
| `artist/{artistId}`      | ArtistDetailScreen  | artistId: Long|
| `playlist/{playlistId}`  | PlaylistDetailScreen| playlistId: Long|
