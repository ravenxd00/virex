package in.virex.music.player

import in.virex.music.domain.model.Track

object QueuePolicy {
 fun distinctQueue(seed: Track, candidates: List<Track>, maxSameArtistInRow: Int = 2): List<Track> {
  val unique = candidates.filter { it.id != seed.id }.distinctBy { it.id }
  val out = mutableListOf(seed)
  val remaining = unique.toMutableList()
  while (remaining.isNotEmpty()) {
   val recentArtists = out.takeLast(maxSameArtistInRow).map { normalize(it.artist) }
   val index = remaining.indexOfFirst { normalize(it.artist) !in recentArtists }
   val selected = remaining.removeAt(if (index >= 0) index else 0)
   out += selected
  }
  return out
 }
 private fun normalize(value: String) = value.trim().lowercase().ifBlank { "unknown" }
}
