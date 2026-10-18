package com.virex.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TrackEntity::class, PlaylistEntity::class], version = 1, exportSchema = false)
abstract class VirexDatabase : RoomDatabase() {
 abstract fun trackDao(): TrackDao
 abstract fun playlistDao(): PlaylistDao
}
