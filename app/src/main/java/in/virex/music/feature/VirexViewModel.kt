package in.virex.music.feature

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import in.virex.music.VirexApplication
import in.virex.music.domain.model.Playlist
import in.virex.music.domain.model.Track
import in.virex.music.downloads.DownloadScheduler
import in.virex.music.persistence.AppSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

data class UiState(
 val category: String = "Trending",
 val home: List<Track> = emptyList(),
 val query: String = "",
 val results: List<Track> = emptyList(),
 val history: List<String> = emptyList(),
 val downloads: List<Track> = emptyList(),
 val playlists: List<Playlist> = emptyList(),
 val settings: AppSettings = AppSettings(),
 val busy: Boolean = false,
 val downloadingId: String? = null,
 val error: String? = null,
 val importProgress: Int = 0,
 val importMatched: Int = 0,
 val importBusy: Boolean = false
)
class VirexViewModel(application: Application) : AndroidViewModel(application) {
 private val app = application as VirexApplication
 private val prefs = application.getSharedPreferences("virex_ui", 0)
 private val _ui = MutableStateFlow(UiState(history = prefs.getString("history", "").orEmpty().split("\n").filter(String::isNotBlank)))
 val ui: StateFlow<UiState> = _ui.asStateFlow()
 private var searchJob: Job? = null
 init {
  viewModelScope.launch { app.repository.observeDownloads().collectLatest { _ui.value = _ui.value.copy(downloads = it) } }
  viewModelScope.launch { app.repository.observePlaylists().collectLatest { _ui.value = _ui.value.copy(playlists = it) } }
  viewModelScope.launch { app.settings.settings.collectLatest { _ui.value = _ui.value.copy(settings = it) } }
  loadCategory("Trending")
 }
 fun setQuery(value: String) {
  _ui.value = _ui.value.copy(query = value, error = null)
  searchJob?.cancel()
  if (value.isBlank()) { _ui.value = _ui.value.copy(results = emptyList(), busy = false); return }
  searchJob = viewModelScope.launch {
   delay(350)
   searchNow(value)
  }
 }
 fun searchNow(value: String = _ui.value.query) {
  if (value.isBlank()) return
  searchJob?.cancel()
  searchJob = viewModelScope.launch {
   val history = (listOf(value.trim()) + _ui.value.history.filterNot { it.equals(value.trim(), true) }).take(8)
   prefs.edit().putString("history", history.joinToString("\n")).apply()
   _ui.value = _ui.value.copy(busy = true, error = null, history = history)
   val result = app.repository.search(value, _ui.value.settings.highQuality)
   _ui.value = _ui.value.copy(busy = false, results = result.getOrDefault(emptyList()), error = result.exceptionOrNull()?.message ?: if (result.getOrDefault(emptyList()).isEmpty()) "No songs found. Try another search." else null)
  }
 }
 fun clearHistory() { prefs.edit().remove("history").apply(); _ui.value = _ui.value.copy(history = emptyList()) }
 fun loadCategory(category: String) {
  _ui.value = _ui.value.copy(category = category, busy = true, error = null)
  viewModelScope.launch {
   val result = app.repository.category(category, _ui.value.settings.highQuality)
   _ui.value = _ui.value.copy(busy = false, home = result.getOrDefault(emptyList()), error = result.exceptionOrNull()?.message ?: if (result.getOrDefault(emptyList()).isEmpty()) "No tracks returned. Check your connection and retry." else null)
  }
 }
 fun refreshCategory() = loadCategory(_ui.value.category)
 fun createPlaylist(name: String, tracks: List<Track> = emptyList()) {
  val safeName = name.trim().ifBlank { "My Playlist" }
  viewModelScope.launch { app.repository.savePlaylist(Playlist(UUID.randomUUID().toString(), safeName, tracks.map { it.id }.distinct())) }
 }
 fun deletePlaylist(id: String) { viewModelScope.launch { app.repository.deletePlaylist(id) } }
 fun replacePlaylists(items: List<Playlist>) { viewModelScope.launch { val old = _ui.value.playlists; items.forEach { app.repository.savePlaylist(it) }; old.filter { previous -> items.none { it.id == previous.id } }.forEach { app.repository.deletePlaylist(it.id) } } }
 fun addTrackToPlaylist(playlistId: String, track: Track) {
  viewModelScope.launch {
   val p = _ui.value.playlists.firstOrNull { it.id == playlistId } ?: return@launch
   app.repository.savePlaylist(p.copy(trackIds = (p.trackIds + track.id).distinct()))
  }
 }
 fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
  viewModelScope.launch {
   val p = _ui.value.playlists.firstOrNull { it.id == playlistId } ?: return@launch
   app.repository.savePlaylist(p.copy(trackIds = p.trackIds.filterNot { it == trackId }))
  }
 }
 fun download(track: Track) {
  if (track.streamUrl.isBlank()) { _ui.value = _ui.value.copy(error = "This track has no usable audio URL to download."); return }
  runCatching { DownloadScheduler.enqueue(getApplication(), track, _ui.value.settings.highQuality) }
   .onSuccess { _ui.value = _ui.value.copy(error = "Download queued. It will appear here when complete.") }
   .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "Could not queue download.") }
 }
 fun updateSettings(transform: (AppSettings) -> AppSettings) { viewModelScope.launch { app.settings.update(transform) } }
 fun trackById(id: String): Track? = _ui.value.downloads.firstOrNull { it.id == id } ?: _ui.value.home.firstOrNull { it.id == id } ?: _ui.value.results.firstOrNull { it.id == id }
 fun importPlaylist(name: String, tracks: List<Track>) = createPlaylist(name, tracks)
 fun importPlaylistFromNames(name: String, rawLines: List<String>) {
  val lines = rawLines.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
  viewModelScope.launch {
   _ui.value = _ui.value.copy(importBusy = true, importProgress = 0, importMatched = 0, error = null)
   val matched = mutableListOf<Track>()
   try {
    lines.chunked(5).forEachIndexed { index, batch ->
     val batchResults = coroutineScope {
      batch.map { query -> async { app.repository.search(query, _ui.value.settings.highQuality).getOrDefault(emptyList()).firstOrNull() } }.map { it.await() }
     }
     matched += batchResults.filterNotNull()
     _ui.value = _ui.value.copy(importProgress = (((index + 1) * 5).coerceAtMost(lines.size) * 100 / lines.size.coerceAtLeast(1)), importMatched = matched.distinctBy { it.id }.size)
    }
    val unique = matched.distinctBy { it.id }
    app.repository.savePlaylist(Playlist(UUID.randomUUID().toString(), name.trim().ifBlank { "Imported Playlist" }, unique.map { it.id }))
    _ui.value = _ui.value.copy(importProgress = 100, importMatched = unique.size, error = if (unique.isEmpty()) "No tracks matched; an empty playlist was created." else "Imported " + unique.size + " tracks.")
   } catch (e: Exception) { _ui.value = _ui.value.copy(error = e.message ?: "Import failed.") }
   finally { _ui.value = _ui.value.copy(importBusy = false) }
  }
 }
}
