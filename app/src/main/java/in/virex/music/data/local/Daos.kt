package com.virex.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao interface TrackDao {
 @Query("SELECT * FROM tracks ORDER BY savedAt DESC") fun observeCachedTracks(): Flow<List<TrackEntity>>
 @Query("SELECT * FROM tracks WHERE localPath IS NOT NULL ORDER BY savedAt DESC") fun observeDownloads(): Flow<List<TrackEntity>>
 @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1") suspend fun get(id: String): TrackEntity?
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(track: TrackEntity)
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(tracks: List<TrackEntity>)
 @Query("DELETE FROM tracks WHERE id = :id") suspend fun delete(id: String)
 @Query("DELETE FROM tracks WHERE localPath IS NOT NULL") suspend fun clearDownloads()
}
@Dao interface PlaylistDao {
 @Query("SELECT * FROM playlists ORDER BY createdAt DESC") fun observeAll(): Flow<List<PlaylistEntity>>
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(playlist: PlaylistEntity)
 @Query("DELETE FROM playlists WHERE id = :id") suspend fun delete(id: String)
}
