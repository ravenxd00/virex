package com.virex.music.data

import android.content.Context
import androidx.room.*

@Entity(tableName="recent_searches")
data class RecentSearch(@PrimaryKey val query:String,val usedAt:Long=System.currentTimeMillis())

@Entity(tableName="playlists")
data class PlaylistEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String)

@Entity(tableName="playlist_songs",primaryKeys=["playlistId","songId"])
data class PlaylistSongEntity(
 val playlistId:Long,val songId:String,val title:String,val artist:String,
 val artwork:String,val streamUrl:String,val highUrl:String,val lowUrl:String,
 val durationSec:Int,val position:Int
)

@Dao interface MusicDao {
 @Query("SELECT * FROM recent_searches ORDER BY usedAt DESC LIMIT 12") suspend fun recent():List<RecentSearch>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveSearch(item:RecentSearch)
 @Query("DELETE FROM recent_searches") suspend fun clearSearches()
 @Query("SELECT * FROM playlists ORDER BY id DESC") suspend fun playlists():List<PlaylistEntity>
 @Insert suspend fun createPlaylist(item:PlaylistEntity):Long
 @Query("DELETE FROM playlists WHERE id=:id") suspend fun deletePlaylist(id:Long)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun addSong(item:PlaylistSongEntity)
 @Query("DELETE FROM playlist_songs WHERE playlistId=:pid AND songId=:sid") suspend fun removeSong(pid:Long,sid:String)
 @Query("SELECT * FROM playlist_songs WHERE playlistId=:id ORDER BY position") suspend fun songs(id:Long):List<PlaylistSongEntity>
}

@Database(entities=[RecentSearch::class,PlaylistEntity::class,PlaylistSongEntity::class],version=1,exportSchema=false)
abstract class AppDatabase:RoomDatabase(){
 abstract fun dao():MusicDao
 companion object {
  @Volatile private var instance:AppDatabase?=null
  fun get(c:Context):AppDatabase=instance?: synchronized(this) {
   instance?:Room.databaseBuilder(c.applicationContext,AppDatabase::class.java,"virex_music.db").build().also{instance=it}
  }
 }
}
