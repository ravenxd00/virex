package com.virex.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
 @PrimaryKey val id: String,
 val title: String,
 val artist: String,
 val album: String,
 val artworkUrl: String,
 val durationMs: Long,
 val streamUrl: String,
 val mimeType: String,
 val localPath: String?,
 val fileSize: Long,
 val savedAt: Long
)
@Entity(tableName = "playlists")
data class PlaylistEntity(
 @PrimaryKey val id: String,
 val name: String,
 val trackIdsJson: String,
 val createdAt: Long
)
