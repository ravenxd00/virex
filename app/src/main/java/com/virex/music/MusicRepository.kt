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

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        require(query.isNotBlank()) { "Enter a song or artist to search." }
        val encoded = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder().url("https://jiosavan-api2.vercel.app/api/search/songs?query=$encoded&limit=25").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Music service returned HTTP ${response.code}")
            val root = JSONObject(response.body?.string().orEmpty())
            val data = root.optJSONObject("data") ?: root
            val results = data.optJSONArray("results") ?: data.optJSONArray("songs") ?: JSONArray()
            buildList {
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val artists = item.optJSONObject("artists")
                    val primary = artists?.optJSONArray("primary")
                    val artist = primary?.optJSONObject(0)?.optString("name").orEmpty()
                        .ifBlank { item.optString("artist").ifBlank { "Unknown artist" } }
                    val images = item.optJSONArray("image")
                    val image = findUrl(images)
                    val stream = findUrl(item.optJSONArray("downloadUrl") ?: item.optJSONArray("download_url"))
                        .ifBlank { item.optString("url") }
                    if (stream.startsWith("https://") || stream.startsWith("http://")) {
                        add(Track(item.optString("id").ifBlank { "track_$i" },
                            item.optString("name", "Unknown song").ifBlank { "Unknown song" }, artist, image, stream,
                            item.optString("duration").toIntOrNull() ?: item.optInt("duration", 0)))
                    }
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
}
