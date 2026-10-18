package com.virex.music.persistence

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("virex_settings")
data class AppSettings(
 val accent: Long = 0xFF1DB954,
 val visualizer: Boolean = true,
 val highQuality: Boolean = false,
 val crossfade: Boolean = false
)
class SettingsStore(private val context: Context) {
 private val accentKey = intPreferencesKey("accent")
 private val visualizerKey = booleanPreferencesKey("visualizer")
 private val highQualityKey = booleanPreferencesKey("high_quality")
 private val crossfadeKey = booleanPreferencesKey("crossfade")
 val settings = context.settingsDataStore.data.map { p ->
  AppSettings((p[accentKey] ?: 0x1DB954).toLong() or 0xFF000000, p[visualizerKey] ?: true, p[highQualityKey] ?: false, p[crossfadeKey] ?: false)
 }
 suspend fun update(transform: (AppSettings) -> AppSettings) {
  context.settingsDataStore.edit { p ->
   val current = AppSettings((p[accentKey] ?: 0x1DB954).toLong() or 0xFF000000, p[visualizerKey] ?: true, p[highQualityKey] ?: false, p[crossfadeKey] ?: false)
   val next = transform(current)
   p[accentKey] = next.accent.toInt() and 0x00FFFFFF
   p[visualizerKey] = next.visualizer
   p[highQualityKey] = next.highQuality
   p[crossfadeKey] = next.crossfade
  }
 }
}
