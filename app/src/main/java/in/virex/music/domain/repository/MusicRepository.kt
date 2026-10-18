package in.virex.music.domain.repository

import in.virex.music.domain.model.Playlist
import in.virex.music.domain.model.SongLyrics
import in.virex.music.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
 suspend fun search(query: String, highQuality: Boolean = false): Result<List<Track>>
 suspend fun category(category: String, highQuality: Boolean = false): Result<List<Track>>
 suspend fun suggestions(seed: Track, highQuality: Boolean = false): Result<List<Track>>
 suspend fun lyrics(track: Track): Result<SongLyrics>
 suspend fun download(track: Track, highQuality: Boolean = false): Result<Track>
 fun observeDownloads(): Flow<List<Track>>
 fun observePlaylists(): Flow<List<Playlist>>
 suspend fun savePlaylist(playlist: Playlist)
 suspend fun deletePlaylist(id: String)
 suspend fun getTrack(id: String): Track?
}
