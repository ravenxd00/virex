package com.virex.music.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.virex.music.core.util.SafeFiles
import com.virex.music.data.local.PlaylistDao
import com.virex.music.data.local.PlaylistEntity
import com.virex.music.data.local.TrackDao
import com.virex.music.data.local.TrackEntity
import com.virex.music.data.remote.LyricsApi
import com.virex.music.data.remote.MusicApi
import com.virex.music.data.remote.SongJsonMapper
import com.virex.music.domain.model.LyricsLine
import com.virex.music.domain.model.Playlist
import com.virex.music.domain.model.SongLyrics
import com.virex.music.domain.model.Track
import com.virex.music.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MusicRepositoryImpl(
 private val musicApi: MusicApi,
 private val lyricsApi: LyricsApi,
 private val trackDao: TrackDao,
 private val playlistDao: PlaylistDao,
 private val context: Context
) : MusicRepository {
 override suspend fun search(query: String, highQuality: Boolean): Result<List<Track>> = runCatching {
  require(query.isNotBlank()) { "Enter a song, artist, or album to search." }
  val tracks = mapTracks(musicApi.search(query.trim(), 20), highQuality); cacheTracks(tracks); tracks
 }
 override suspend fun category(category: String, highQuality: Boolean): Result<List<Track>> {
  val query = when(category.lowercase(Locale.ROOT)) {
   "punjabi" -> "latest Punjabi hits 2025"
   "hip hop" -> "popular hip hop"
   "bollywood" -> "Bollywood hits"
   "global hits" -> "global top hits"
   "lofi", "lofi chill" -> "lofi chill"
   "edm" -> "EDM dance hits"
   else -> "trending songs"
  }
  return search(query, highQuality)
 }
 override suspend fun suggestions(seed: Track, highQuality: Boolean): Result<List<Track>> = runCatching {
  val json = musicApi.suggestions(seed.id, 20)
  val tracks = mapTracks(json, highQuality).filterNot { it.id == seed.id }.distinctBy { it.id }; trackDao.upsertAll(tracks.map { it.entity() }); tracks
 }
 override suspend fun lyrics(track: Track): Result<SongLyrics> = runCatching {
  val json = runCatching { lyricsApi.get(track.title, track.artist, (track.durationMs / 1000).coerceAtLeast(0)) }.getOrNull()
  val candidate = json ?: lyricsApi.search(track.title, track.artist).minByOrNull {
   val duration = it.long("duration") * 1000L
   kotlin.math.abs(duration - track.durationMs)
  }
  val plain = candidate?.string("plainLyrics").orEmpty()
  val synced = candidate?.string("syncedLyrics").orEmpty()
  if (synced.isNotBlank()) SongLyrics(parseLrc(synced), true, plain)
  else if (plain.isNotBlank()) SongLyrics(plain.lines().filter { it.isNotBlank() }.map { LyricsLine(null, it) }, false, plain)
  else SongLyrics(emptyList(), false, "")
 }
 override suspend fun download(track: Track, highQuality: Boolean): Result<Track> = withContext(Dispatchers.IO) {
  runCatching {
   require(track.streamUrl.startsWith("https://")) { "A secure audio URL is not available for this track." }
   val dir = File(context.filesDir, "downloads").apply { mkdirs() }
   val file = File(dir, "${SafeFiles.safeName(track.title)}_${track.id.hashCode().toUInt().toString(16)}.mp3")
   require(SafeFiles.inside(dir, file)) { "Unsafe download path." }
   val connection = (URL(track.streamUrl).openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true }
   try {
    connection.connect()
    require(connection.responseCode in 200..299) { "Download failed (HTTP ${connection.responseCode})." }
    val mime = connection.contentType.orEmpty().substringBefore(';').lowercase(Locale.ROOT)
    require(mime.startsWith("audio/") || mime == "application/octet-stream") { "The server did not return an audio file." }
    connection.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    require(file.length() > 0L) { "Downloaded file is empty." }
    val saved = track.copy(localPath = file.absolutePath, fileSize = file.length(), savedAt = System.currentTimeMillis(), mimeType = mime.ifBlank { "audio/mpeg" })
    trackDao.upsert(saved.entity())
    saved
   } catch (e: Exception) { file.delete(); throw e } finally { connection.disconnect() }
  }
 }
 override fun observeCachedTracks(): Flow<List<Track>> = trackDao.observeCachedTracks().map { rows -> rows.map { it.model() } }
 override suspend fun clearDownloads() { val saved = trackDao.observeDownloads(); val rows = kotlinx.coroutines.flow.first(saved); rows.mapNotNull { it.localPath }.forEach { path -> runCatching { File(path).delete() } }; trackDao.clearDownloads() }
 override fun observeDownloads(): Flow<List<Track>> = trackDao.observeDownloads().map { rows -> rows.map { it.model() } }
 override fun observePlaylists(): Flow<List<Playlist>> = playlistDao.observeAll().map { rows -> rows.map { Playlist(it.id, it.name, runCatching { JSONArray(it.trackIdsJson).let { a -> (0 until a.length()).map(a::getString) } }.getOrDefault(emptyList()), it.createdAt) } }
 override suspend fun savePlaylist(playlist: Playlist) { playlistDao.upsert(PlaylistEntity(playlist.id, playlist.name, JSONArray(playlist.trackIds).toString(), playlist.createdAt)) }
 override suspend fun deletePlaylist(id: String) { playlistDao.delete(id) }
 override suspend fun getTrack(id: String): Track? = trackDao.get(id)?.model()

 private fun mapTracks(root: JsonObject, highQuality: Boolean): List<Track> = SongJsonMapper.mapTracks(root, highQuality)
 private suspend fun cacheTracks(tracks: List<Track>) { val safe = tracks.map { fresh -> val old = trackDao.get(fresh.id); if (old?.localPath != null) fresh.copy(localPath = old.localPath, fileSize = old.fileSize, savedAt = old.savedAt, mimeType = old.mimeType) else fresh }; trackDao.upsertAll(safe.map { it.entity() }) }
 private fun parseLrc(raw: String): List<LyricsLine> {
  val regex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")
  return raw.lines().mapNotNull { line ->
   val m = regex.matchEntire(line.trim()) ?: return@mapNotNull null
   val ms = (m.groupValues[1].toLong() * 60 + m.groupValues[2].toLong()) * 1000 + m.groupValues[3].padEnd(3, '0').take(3).toLongOrNull().orZero()
   LyricsLine(ms, m.groupValues[4].trim())
  }.sortedBy { it.timeMs }
 }
 private fun String.htmlDecode() = replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">")
 private fun JsonObject.string(vararg keys: String): String = keys.firstNotNullOfOrNull { key -> get(key)?.takeUnless { it.isJsonNull }?.let { e -> if (e.isJsonPrimitive) e.asString else if (e.isJsonObject) e.asJsonObject.get("name")?.asString else null } }.orEmpty()
 private fun JsonObject.long(vararg keys: String): Long = keys.firstNotNullOfOrNull { key -> runCatching { get(key)?.asLong }.getOrNull() } ?: 0L
 private fun JsonObject.obj(key: String): JsonObject? = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
 private fun Long?.orZero() = this ?: 0L
 private fun Track.entity() = TrackEntity(id, title, artist, album, artworkUrl, durationMs, streamUrl, mimeType, localPath, fileSize, savedAt)
 private fun TrackEntity.model() = Track(id, title, artist, album, artworkUrl, durationMs, streamUrl, mimeType, localPath, fileSize, savedAt)
}
