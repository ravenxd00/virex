package com.virex.music.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.virex.music.domain.model.Track

object SongJsonMapper {
 fun mapTracks(root: JsonObject, highQuality: Boolean = false): List<Track> {
  val array = findArray(root) ?: return emptyList()
  return array.mapNotNull { element ->
   runCatching {
    if (!element.isJsonObject) return@runCatching null
    val song = element.asJsonObject
    val id = song.string("id", "songId", "trackId", "perma_url")
    if (id.isBlank()) return@runCatching null
    val title = song.string("name", "title", "song", "trackName").decodeHtml().ifBlank { "Unknown title" }
    val artists = artistName(song).decodeHtml().ifBlank { "Unknown artist" }
    val album = song.objectAt("album")?.string("name", "title").orEmpty()
    val image = bestImage(song)
    val audio = bestAudio(song, highQuality)
    val duration = if (song.has("durationMs")) song.long("durationMs") else song.long("duration", "length") * 1000L
    Track(id, title, artists, album, image, duration, audio)
   }.getOrNull()
  }
 }
 private fun findArray(root: JsonObject): JsonArray? {
  fun find(element: JsonElement, depth: Int): JsonArray? {
   if (depth > 6 || element.isJsonNull) return null
   if (element.isJsonArray) return element.asJsonArray
   if (!element.isJsonObject) return null
   val objectValue = element.asJsonObject
   for (key in listOf("results", "songs", "data", "items", "tracks", "suggestions")) {
    val child = objectValue.get(key) ?: continue
    val found = find(child, depth + 1)
    if (found != null) return found
   }
   return null
  }
  return find(root, 0)
 }
 private fun artistName(song: JsonObject): String {
  val direct = song.string("primaryArtists", "primary_artists", "artist", "singers", "artistName", "artist_name")
  if (direct.isNotBlank()) return direct
  fun names(element: JsonElement?): String {
   if (element == null || element.isJsonNull) return ""
   if (element.isJsonPrimitive) return element.asString
   if (element.isJsonArray) return element.asJsonArray.mapNotNull { names(it).takeIf(String::isNotBlank) }.distinct().joinToString(", ")
   if (element.isJsonObject) {
    val obj = element.asJsonObject
    val named = obj.string("name", "title", "artistName", "artist_name")
    if (named.isNotBlank()) return named
    for (key in listOf("primary", "primary_artists", "artists", "all", "featured", "list")) {
     val nested = names(obj.get(key))
     if (nested.isNotBlank()) return nested
    }
   }
   return ""
  }
  for (key in listOf("primaryArtists", "primary_artists", "artists", "artist", "singers", "artistMap", "artist_map")) {
   val value = names(song.get(key))
   if (value.isNotBlank()) return value
  }
  val subtitle = song.string("subtitle")
  return subtitle.takeIf { it.isNotBlank() }.orEmpty()
 }
 private fun bestImage(song: JsonObject): String {
  val image = song.get("image") ?: song.get("images") ?: song.get("artwork") ?: song.get("album")?.takeIf { it.isJsonObject }?.asJsonObject?.get("image")
   ?: return ""
  if (image.isJsonPrimitive) return image.asString.takeIf { it.startsWith("https://") }.orEmpty()
  if (image.isJsonArray) {
   val candidates = image.asJsonArray.mapNotNull { item ->
    if (item.isJsonObject) item.asJsonObject.string("url", "link").takeIf { it.startsWith("https://") }
    else item.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.startsWith("https://") }
   }
   return candidates.lastOrNull().orEmpty()
  }
  if (image.isJsonObject) {
   val obj = image.asJsonObject
   return obj.string("url", "link", "high", "large", "500x500", "500")
    .takeIf { it.startsWith("https://") }
    ?: obj.entrySet().mapNotNull { it.value.takeIf { v -> v.isJsonPrimitive }?.asString?.takeIf { it.startsWith("https://") } }.lastOrNull().orEmpty()
  }
  return ""
 }
 private fun bestAudio(song: JsonObject, highQuality: Boolean): String {
  val candidates = listOf("downloadUrl", "download_url", "downloadUrls", "download_urls", "media", "stream")
   .firstNotNullOfOrNull { song.get(it)?.takeIf { v -> v.isJsonArray }?.asJsonArray }
  if (candidates != null) {
   val audio = candidates.mapNotNull { item ->
    if (!item.isJsonObject) return@mapNotNull null
    val obj = item.asJsonObject
    val url = obj.string("url", "link", "src")
    if (!url.startsWith("https://")) return@mapNotNull null
    val label = obj.string("quality", "bitrate", "label")
    val bitrate = obj.long("bitrate", "quality").takeIf { it > 0 } ?: Regex("(\\d{2,3})").find(label)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    Pair(url, bitrate)
   }
   if (audio.isNotEmpty()) return if (highQuality) audio.maxByOrNull { it.second }!!.first else audio.minByOrNull { if (it.second == 0L) Long.MAX_VALUE else kotlin.math.abs(it.second - 160L) }!!.first
  }
  val direct = song.string("audioUrl", "audio_url", "media_url", "url", "downloadUrl", "download_url")
  if (direct.startsWith("https://")) return direct
  val nested = song.get("media")?.takeIf { it.isJsonObject }?.asJsonObject ?: song.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject
  return nested?.string("url", "src").orEmpty().takeIf { it.startsWith("https://") }.orEmpty()
 }
 private fun JsonObject.string(vararg keys: String): String = keys.firstNotNullOfOrNull { key ->
  get(key)?.takeUnless { it.isJsonNull }?.let { value ->
   when { value.isJsonPrimitive -> value.asString; value.isJsonObject -> value.asJsonObject.get("name")?.asString; else -> null }
  }
 }.orEmpty()
 private fun JsonObject.long(vararg keys: String): Long = keys.firstNotNullOfOrNull { key -> runCatching { get(key)?.asLong }.getOrNull() } ?: 0L
 private fun JsonObject.objectAt(key: String): JsonObject? = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
 private fun String.decodeHtml() = replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">")
}
