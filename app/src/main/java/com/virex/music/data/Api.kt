package com.virex.music.data

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private interface MusicEndpoints {
    @GET("api/search/songs") suspend fun search(@Query("query") query: String, @Query("limit") limit: Int = 20): ResponseBody
    @GET("api/songs/{id}/suggestions") suspend fun suggestions(@Path("id") id: String, @Query("limit") limit: Int = 20): ResponseBody
}
private interface LyricsEndpoints {
    @GET("api/get") suspend fun get(@Query("track_name") title: String, @Query("artist_name") artist: String, @Query("duration") duration: Int): ResponseBody
    @GET("api/search") suspend fun search(@Query("track_name") title: String, @Query("artist_name") artist: String): ResponseBody
}

object MusicApi {
    private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    private val music = Retrofit.Builder().baseUrl("https://jiosavan-api2.vercel.app/").client(client).build().create(MusicEndpoints::class.java)
    private val lyrics = Retrofit.Builder().baseUrl("https://lrclib.net/").client(client).build().create(LyricsEndpoints::class.java)

    suspend fun search(query: String): List<Song> = parseSongs(music.search(query).string())
    suspend fun suggestions(id: String): List<Song> = parseSongs(music.suggestions(id).string())

    suspend fun lyrics(song: Song): LyricsResult {
        val exact = runCatching { JSONObject(lyrics.get(song.title, song.artist, song.durationSec).string()) }.getOrNull()
        val synced = exact?.optString("syncedLyrics").orEmpty()
        if (synced.isNotBlank()) return LyricsResult(parseLrc(synced), true)
        val plain = exact?.optString("plainLyrics").orEmpty()
        if (plain.isNotBlank()) return LyricsResult(plain.lines().filter { it.isNotBlank() }.map { LyricLine(null, it) }, false)
        val results = JSONArray(lyrics.search(song.title, song.artist).string())
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val lrc = item.optString("syncedLyrics")
            if (lrc.isNotBlank()) return LyricsResult(parseLrc(lrc), true)
            val text = item.optString("plainLyrics")
            if (text.isNotBlank()) return LyricsResult(text.lines().map { LyricLine(null, it) }, false)
        }
        return LyricsResult(emptyList(), false)
    }

    private fun parseSongs(raw: String): List<Song> {
        val root = JSONObject(raw)
        val data = root.optJSONObject("data") ?: root
        val songs = data.optJSONArray("results") ?: data.optJSONArray("songs") ?: root.optJSONArray("results") ?: JSONArray()
        return (0 until songs.length()).mapNotNull { index ->
            val o = songs.optJSONObject(index) ?: return@mapNotNull null
            val id = o.optString("id")
            if (id.isBlank()) return@mapNotNull null
            val title = o.optString("name", o.optString("title", "Unknown title"))
            val artists = o.optJSONObject("artists")
            val primary = artists?.optJSONObject("primary")?.optJSONArray("artists")
            val artist = when {
                o.opt("artists") is String -> o.optString("artists")
                primary != null -> (0 until primary.length()).mapNotNull { primary.optJSONObject(it)?.optString("name") }.joinToString(", ")
                artists?.optString("primary").orEmpty().isNotBlank() -> artists.optString("primary")
                else -> o.optString("artist", "Unknown artist")
            }
            val images = o.optJSONArray("image")
            val image = largestUrl(images)
            val downloads = o.optJSONArray("downloadUrl") ?: o.optJSONArray("download_url")
            val stream = chooseQuality(downloads)
            Song(id, title, artist.ifBlank { "Unknown artist" }, image, stream, o.optString("duration").toIntOrNull() ?: 0)
        }
    }
    private fun largestUrl(array: JSONArray?): String {
        if (array == null || array.length() == 0) return ""
        val values = (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            item.optString("url").takeIf { it.startsWith("http") }?.let { it to (item.optString("quality").filter { c -> c.isDigit() }.toIntOrNull() ?: 0) }
        }
        return values.maxByOrNull { it.second }?.first.orEmpty()
    }
    private fun chooseQuality(array: JSONArray?): String {
        if (array == null || array.length() == 0) return ""
        val options = (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            val url = item.optString("url").takeIf { it.startsWith("http") } ?: return@mapNotNull null
            val q = item.optString("quality").filter { it.isDigit() }.toIntOrNull() ?: 0
            url to q
        }
        return (if (QualityPreference.high) options.maxByOrNull { it.second } else options.minByOrNull { it.second })?.first.orEmpty()
    }
    private fun parseLrc(raw: String): List<LyricLine> = raw.lines().flatMap { line ->
        val regex = Regex("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{1,3}))?]")
        val matches = regex.findAll(line).toList()
        val text = line.replace(regex, "").trim()
        matches.mapNotNull { m ->
            val min = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val sec = m.groupValues[2].toLongOrNull() ?: 0
            val fraction = m.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
            LyricLine((min * 60 + sec) * 1000 + fraction, text)
        }
    }.sortedBy { it.timeMs }
}

object QualityPreference { @Volatile var high: Boolean = true }
