package in.virex.music.domain.model

object PlaylistImportPolicy {
 fun uniqueQueries(lines: List<String>): List<String> = lines.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
 fun uniqueTracks(tracks: List<Track>): List<Track> = tracks.distinctBy { it.id }
}
