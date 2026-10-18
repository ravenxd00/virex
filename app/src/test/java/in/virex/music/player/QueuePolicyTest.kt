package com.virex.music.player

import com.virex.music.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class QueuePolicyTest {
 @Test fun removesDuplicatesAndSeed() {
  val seed = Track("1","One","A")
  val result = QueuePolicy.distinctQueue(seed, listOf(seed, Track("2","Two","B"), Track("2","Duplicate","B")))
  assertEquals(listOf("1","2"), result.map { it.id })
 }
 @Test fun avoidsTooManySameArtistInARowWhenPossible() {
  val seed = Track("1","One","A")
  val result = QueuePolicy.distinctQueue(seed, listOf(Track("2","Two","A"),Track("3","Three","A"),Track("4","Four","B")))
  assertEquals(listOf("1","4","2","3"), result.map { it.id })
 }
}
