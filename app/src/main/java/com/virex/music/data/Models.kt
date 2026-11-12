package com.virex.music.data

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artwork: String = "",
    val streamUrl: String = "",
    val highUrl: String = "",
    val lowUrl: String = "",
    val durationSec: Int = 0,
    val isLocal: Boolean = false
) {
    fun url(high: Boolean): String =
        if (high) highUrl.ifBlank { streamUrl } else lowUrl.ifBlank { streamUrl }
}
data class LyricLine(val timeMs: Long?, val text: String)
data class LyricsResult(val lines: List<LyricLine>, val synced: Boolean)
