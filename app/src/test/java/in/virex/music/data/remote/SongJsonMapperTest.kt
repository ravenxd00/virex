package in.virex.music.data.remote

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongJsonMapperTest {
 private val json = JsonParser.parseString("""{
  "data":{"results":[{
   "id":"track-1","name":"Blinding Lights","primaryArtists":"The Weeknd","duration":"200",
   "image":[{"quality":"150x150","url":"https://img.example/150.jpg"},{"quality":"500x500","url":"https://img.example/500.jpg"}],
   "downloadUrl":[{"quality":"160kbps","url":"https://audio.example/160.mp3"},{"quality":"320kbps","url":"https://audio.example/320.mp3"}]
  }]}
 }""").asJsonObject
 @Test fun mapsSongMetadataAndNormalQuality() {
  val track = SongJsonMapper.mapTracks(json).single()
  assertEquals("track-1", track.id)
  assertEquals("Blinding Lights", track.title)
  assertEquals("The Weeknd", track.artist)
  assertEquals(200000L, track.durationMs)
  assertEquals("https://img.example/500.jpg", track.artworkUrl)
  assertEquals("https://audio.example/160.mp3", track.streamUrl)
 }
 @Test fun highQualityPrefers320Kbps() {
  assertEquals("https://audio.example/320.mp3", SongJsonMapper.mapTracks(json, true).single().streamUrl)
 }
 @Test fun ignoresRecordsWithoutAnId() {
  val invalid = JsonParser.parseString("""{"results":[{"name":"No ID"}]}""").asJsonObject
  assertTrue(SongJsonMapper.mapTracks(invalid).isEmpty())
 }
}
