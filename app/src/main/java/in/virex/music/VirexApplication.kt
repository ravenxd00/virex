package com.virex.music

import android.app.Application
import androidx.room.Room
import com.virex.music.data.local.VirexDatabase
import com.virex.music.data.remote.ApiFactory
import com.virex.music.data.repository.MusicRepositoryImpl
import com.virex.music.persistence.SettingsStore
import com.virex.music.player.PlayerController

class VirexApplication : Application() {
 lateinit var database: VirexDatabase
 lateinit var repository: MusicRepositoryImpl
 lateinit var settings: SettingsStore
 lateinit var playerController: PlayerController
 override fun onCreate() {
  super.onCreate()
  database = Room.databaseBuilder(this, VirexDatabase::class.java, "virex.db").fallbackToDestructiveMigration().build()
  repository = MusicRepositoryImpl(ApiFactory.music, ApiFactory.lyrics, database.trackDao(), database.playlistDao(), this)
  settings = SettingsStore(this)
  playerController = PlayerController(this)
 }
}
