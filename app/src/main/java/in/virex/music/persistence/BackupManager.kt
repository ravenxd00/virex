package in.virex.music.persistence

import com.google.gson.Gson
import com.google.gson.JsonParser
import in.virex.music.domain.model.Playlist

data class BackupDocument(
 val schemaVersion: Int = 1,
 val exportedAt: Long = System.currentTimeMillis(),
 val playlists: List<Playlist> = emptyList(),
 val accent: Long = 0xFF1DB954,
 val visualizer: Boolean = true,
 val highQuality: Boolean = false,
 val crossfade: Boolean = false
)
object BackupManager {
 private val gson = Gson()
 fun encode(document: BackupDocument): String = gson.toJson(document)
 fun decode(json: String): Result<BackupDocument> = runCatching {
  require(json.length <= 5_000_000) { "Backup file is too large." }
  val root = JsonParser.parseString(json)
  require(root.isJsonObject) { "Invalid backup format." }
  val version = root.asJsonObject.get("schemaVersion")?.asInt ?: 0
  require(version == 1) { "Unsupported backup schema version: $version" }
  val decoded = gson.fromJson(root, BackupDocument::class.java)
  require(decoded.playlists.size <= 500) { "Backup contains too many playlists." }
  require(decoded.playlists.all { it.name.isNotBlank() && it.name.length <= 120 && it.trackIds.size <= 5000 }) { "Backup contains invalid playlists." }
  require(decoded.accent in 0xFF000000..0xFFFFFFFF) { "Invalid accent color." }
  decoded
 }
}
