package in.virex.music.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PoliciesTest {
 @Test fun repeatCyclesOffAllOneOff() {
  assertEquals(RepeatMode.ALL, RepeatModePolicy.next(RepeatMode.OFF))
  assertEquals(RepeatMode.ONE, RepeatModePolicy.next(RepeatMode.ALL))
  assertEquals(RepeatMode.OFF, RepeatModePolicy.next(RepeatMode.ONE))
 }
 @Test fun playlistImportDeduplicatesQueriesAndTracks() {
  assertEquals(listOf("Song A", "Song B"), PlaylistImportPolicy.uniqueQueries(listOf(" Song A ", "song a", "", "Song B")))
  val tracks = listOf(Track("1","One","A"), Track("1","duplicate","A"), Track("2","Two","B"))
  assertEquals(listOf("1","2"), PlaylistImportPolicy.uniqueTracks(tracks).map { it.id })
 }
}
