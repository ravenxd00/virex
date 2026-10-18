package in.virex.music.domain.model

data class Track(
 val id: String,
 val title: String,
 val artist: String,
 val album: String = "",
 val artworkUrl: String = "",
 val durationMs: Long = 0,
 val streamUrl: String = "",
 val mimeType: String = "audio/mpeg",
 val localPath: String? = null,
 val fileSize: Long = 0,
 val savedAt: Long = 0
)
data class Playlist(val id: String, val name: String, val trackIds: List<String>, val createdAt: Long = System.currentTimeMillis())
data class LyricsLine(val timeMs: Long?, val text: String)
data class SongLyrics(val lines: List<LyricsLine>, val synced: Boolean, val plainText: String = "")
enum class RepeatMode { OFF, ALL, ONE }
enum class PlaybackContext { RADIO, SEARCH, PLAYLIST, DOWNLOADS }
data class PlayerSnapshot(
 val current: Track? = null,
 val queue: List<Track> = emptyList(),
 val index: Int = 0,
 val isPlaying: Boolean = false,
 val positionMs: Long = 0,
 val durationMs: Long = 0,
 val shuffle: Boolean = false,
 val repeat: RepeatMode = RepeatMode.OFF,
 val context: PlaybackContext = PlaybackContext.RADIO
)
