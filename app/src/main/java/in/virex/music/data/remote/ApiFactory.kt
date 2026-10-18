package com.virex.music.data.remote

import com.virex.music.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.google.gson.JsonObject

interface MusicApi {
 @GET("search/songs") suspend fun search(@Query("query") query: String, @Query("limit") limit: Int = 20): JsonObject
 @GET("songs/{songId}/suggestions") suspend fun suggestions(@Path("songId") id: String, @Query("limit") limit: Int = 20): JsonObject
}
interface LyricsApi {
 @GET("get") suspend fun get(@Query("track_name") name: String, @Query("artist_name") artist: String, @Query("duration") durationSeconds: Long): JsonObject
 @GET("search") suspend fun search(@Query("track_name") name: String, @Query("artist_name") artist: String): List<JsonObject>
}
object ApiFactory {
 private fun <T> retrofit(base: String, type: Class<T>): T = Retrofit.Builder().baseUrl(base).addConverterFactory(GsonConverterFactory.create()).build().create(type)
 val music: MusicApi by lazy { retrofit(BuildConfig.SAAVAN_BASE_URL, MusicApi::class.java) }
 val lyrics: LyricsApi by lazy { retrofit(BuildConfig.LRCLIB_BASE_URL, LyricsApi::class.java) }
}
