package com.virex.music

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val image: String,
    val streamUrl: String,
    val durationSeconds: Int = 0,
    val localPath: String? = null
)
