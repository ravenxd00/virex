package com.virex.music

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MusicRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS).build()

    suspend fun search(query: String, highQuality: Boolean = true): List<Track> = withContext(Dispatchers.IO) {
        require(query.isNotBlank()) { "Enter a song or artist to search." }
        val encoded = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://jiosavan-api2.vercel.app/api/search/songs?query=$encoded&limit=25")
            .header("Accept", "application/json")
            .header("User-Agent", "VirexDL-Android/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Music service returned HTTP ${response.code}")
            val raw = response.body?.string().orEmpty()
            val root = JSONObject(raw)
            val data = root.optJSONObject("data") ?: root
            val results = data.optJSONArray("results")
                ?: data.optJSONArray("songs")
                ?: root.optJSONArray("results")
                ?: root.optJSONArray("songs")
                ?: JSONArray()
            buildList {
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val artists = item.optJSONObject("artists")
                    val primary = artists?.optJSONArray("primary")
                    val artist = primary?.optJSONObject(0)?.optString("name").orEmpty()
                        .ifBlank { item.optString("artist").ifBlank { item.optString("subtitle").ifBlank { "Unknown artist" } } }
                    val images = item.optJSONArray("image")
                    val image = findUrl(images).ifBlank {
                        item.optString("image").takeIf { it.startsWith("http") }.orEmpty()
                    }
                    val audioList = item.optJSONArray("downloadUrl") ?: item.optJSONArray("download_url")
                    val desiredQuality = if (highQuality) "320kbps" else "160kbps"
                    var stream = ""
                    if (audioList != null) {
                        for (j in 0 until audioList.length()) {
                            val audio = audioList.optJSONObject(j) ?: continue
                            if (audio.optString("quality").equals(desiredQuality, true)) {
                                stream = audio.optString("url")
                                break
                            }
                        }
                        if (stream.isBlank()) stream = findUrl(audioList)
                    }
                    if (stream.isBlank()) {
                        stream = item.optJSONObject("downloadUrl")?.optString("url").orEmpty()
                            .ifBlank { item.optString("url").takeIf { it.startsWith("http") }.orEmpty() }
                    }
                    val title = item.optString("name").ifBlank { item.optString("title").ifBlank { "Unknown song" } }
                    val id = item.optString("id").ifBlank { "${title}_${artist}_$i" }
                    if (title.isNotBlank()) add(
                        Track(id, title, artist, image, stream, item.optString("duration").toIntOrNull() ?: item.optInt("duration", 0))
                    )
                }
            }
        }
    }

    private fun findUrl(array: JSONArray?): String {
        if (array == null) return ""
        for (i in array.length() - 1 downTo 0) {
            val obj = array.optJSONObject(i) ?: continue
            val url = obj.optString("url")
            if (url.startsWith("https://") || url.startsWith("http://")) return url
        }
        return ""
    }

    suspend fun download(context: Context, track: Track): Track = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(track.streamUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed: HTTP ${response.code}")
            val folder = File(context.filesDir, "offline_music").apply { mkdirs() }
            val safeId = track.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val target = File(folder, "$safeId.mp3")
            response.body?.byteStream()?.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                ?: error("Empty audio response")
            val local = track.copy(localPath = target.absolutePath)
            val saved = context.getSharedPreferences("virex", Context.MODE_PRIVATE)
            val previous = saved.getString("downloads", "[]") ?: "[]"
            val array = org.json.JSONArray(previous)
            var found = false
            for (i in 0 until array.length()) if (array.optJSONObject(i)?.optString("id") == local.id) found = true
            if (!found) {
                array.put(JSONObject().put("id", local.id).put("title", local.title).put("artist", local.artist)
                    .put("image", local.image).put("streamUrl", local.streamUrl).put("localPath", local.localPath)
                    .put("durationSeconds", local.durationSeconds))
                saved.edit().putString("downloads", array.toString()).apply()
            }
            local
        }
    }

    fun getDownloads(context: Context): List<Track> {
        val array = org.json.JSONArray(context.getSharedPreferences("virex", Context.MODE_PRIVATE).getString("downloads", "[]"))
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val path = o.optString("localPath")
                if (path.isNotBlank() && File(path).exists()) add(Track(o.optString("id"), o.optString("title"),
                    o.optString("artist"), o.optString("image"), o.optString("streamUrl"), o.optInt("durationSeconds"), path))
            }
        }
    }

    suspend fun lyrics(track: Track): List<String> = withContext(Dispatchers.IO) {
        val name = java.net.URLEncoder.encode(track.title.substringBefore("(").trim(), "UTF-8")
        val artist = java.net.URLEncoder.encode(track.artist, "UTF-8")
        val request = Request.Builder().url("https://lrclib.net/api/get?track_name=$name&artist_name=$artist").build()
        val response = client.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) {
                val searchRequest = Request.Builder().url("https://lrclib.net/api/search?track_name=$name&artist_name=$artist").build()
                client.newCall(searchRequest).execute().use { searchResponse ->
                    if (!searchResponse.isSuccessful) return@withContext listOf("Lyrics unavailable for this track.")
                    val arr = JSONArray(searchResponse.body?.string().orEmpty())
                    if (arr.length() == 0) return@withContext listOf("No lyrics found.")
                    val obj = arr.optJSONObject(0) ?: return@withContext listOf("No lyrics found.")
                    return@withContext parseLyrics(obj.optString("syncedLyrics").ifBlank { obj.optString("plainLyrics") })
                }
            }
            val obj = JSONObject(it.body?.string().orEmpty())
            if (obj.optBoolean("instrumental")) return@withContext listOf("Instrumental")
            parseLyrics(obj.optString("syncedLyrics").ifBlank { obj.optString("plainLyrics") })
        }
    }

    private fun parseLyrics(raw: String): List<String> {
        if (raw.isBlank() || raw == "null") return listOf("No lyrics found.")
        val timed = Regex("\\[(\\d{2}):(\\d{2})[.](\\d{2,3})](.*)")
        val parsed = raw.lines().mapNotNull { line ->
            val m = timed.find(line)
            if (m != null) {
                val millis = m.groupValues[1].toInt() * 60000 + m.groupValues[2].toInt() * 1000
                val fraction = m.groupValues[3].padEnd(3, '0').take(3).toInt()
                Pair(millis + fraction, m.groupValues[4].trim())
            } else null
        }.sortedBy { it.first }.map { it.second }.filter { it.isNotBlank() }
        return if (parsed.isNotEmpty()) parsed else raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    }

}
