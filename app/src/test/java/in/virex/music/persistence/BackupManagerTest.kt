package in.virex.music.persistence

import in.virex.music.domain.model.Playlist
import org.junit.Assert.*
import org.junit.Test

class BackupManagerTest {
 @Test fun roundTripsSettingsAndPlaylists() {
  val source = BackupDocument(playlists = listOf(Playlist("p1","Road Trip", listOf("a","b"))), accent = 0xFF00FF00, highQuality = true)
  val decoded = BackupManager.decode(BackupManager.encode(source)).getOrThrow()
  assertEquals(source.playlists, decoded.playlists)
  assertTrue(decoded.highQuality)
  assertEquals(source.accent, decoded.accent)
 }
 @Test fun rejectsUnsupportedSchema() {
  assertTrue(BackupManager.decode("""{"schemaVersion":999}""").isFailure)
 }
 @Test fun rejectsInvalidPlaylist() {
  assertTrue(BackupManager.decode("""{"schemaVersion":1,"playlists":[{"id":"x","name":"","trackIds":[]}]}""").isFailure)
 }
}
