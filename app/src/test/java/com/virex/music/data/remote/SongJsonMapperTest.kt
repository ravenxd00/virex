package com.virex.music.data.remote

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongJsonMapperTest {
 @Test fun mapsPrimaryArtistString() {
  val root = JsonParser.parseString("""{"data":{"results":[{"id":"1","name":"Song","primaryArtists":"Artist One","duration":180,"image":[{"url":"https://example.com/cover.jpg"}],"downloadUrl":[{"quality":"160kbps","url":"https://example.com/song.mp3"}]}]}}""").asJsonObject
  val track = SongJsonMapper.mapTracks(root).single()
  assertEquals("Artist One", track.artist)
  assertEquals("Song", track.title)
  assertEquals(180_000L, track.durationMs)
 }
 @Test fun mapsNestedArtistArray() {
  val root = JsonParser.parseString("""{"data":{"results":[{"id":"2","title":"Song Two","artists":{"primary":[{"name":"Artist A"},{"name":"Artist B"}]},"duration":201}]}}""").asJsonObject
  val track = SongJsonMapper.mapTracks(root).single()
  assertEquals("Artist A, Artist B", track.artist)
 }
 @Test fun providesNonBlankFallbackWhenArtistMissing() {
  val root = JsonParser.parseString("""{"results":[{"id":"3","name":"Instrumental"}]}""").asJsonObject
  val track = SongJsonMapper.mapTracks(root).single()
  assertTrue(track.artist.isNotBlank())
 }
}
