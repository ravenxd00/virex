package com.virex.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.virex.music.domain.model.PlaybackContext
import com.virex.music.domain.model.PlayerSnapshot
import com.virex.music.domain.model.RepeatMode
import com.virex.music.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerController(context: Context) {
 private val appContext = context.applicationContext
 private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
 private var controllerFuture: ListenableFuture<MediaController>? = null
 private var controller: MediaController? = null
 private val _state = MutableStateFlow(PlayerSnapshot())
 val state: StateFlow<PlayerSnapshot> = _state.asStateFlow()
 private var playbackContext = PlaybackContext.RADIO
 private val knownTracks = linkedMapOf<String, Track>()
 private var pendingStart: Triple<Track, List<Track>, PlaybackContext>? = null
 private val tracksById = linkedMapOf<String, Track>()
 private val pendingAppend = mutableListOf<Track>()
 private val listener = object : Player.Listener {
  override fun onEvents(player: Player, events: Player.Events) { publish() }
  override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { publish() }
 }
 init {
  val token = SessionToken(appContext, ComponentName(appContext, MusicService::class.java))
  controllerFuture = MediaController.Builder(appContext, token).buildAsync().also { future ->
   future.addListener({
    runCatching { future.get() }.onSuccess { c -> controller = c; c.addListener(listener); publish(); pendingStart?.let { pending -> pendingStart = null; play(pending.first, pending.second, pending.third) }; if (pendingAppend.isNotEmpty()) { val waiting = pendingAppend.toList(); pendingAppend.clear(); append(waiting) } }
   }, appContext.mainExecutor)
  }
  scope.launch { while (isActive) { publish(); delay(400) } }
 }
 fun play(track: Track, queue: List<Track>, context: PlaybackContext = PlaybackContext.RADIO) {
  val c = controller ?: run { pendingStart = Triple(track, queue, context); return }
  val items = (queue.ifEmpty { listOf(track) }).distinctBy { it.id }
  items.forEach { knownTracks[it.id] = it }
  knownTracks[track.id] = track
  val index = items.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
  playbackContext = context
  items.forEach { tracksById[it.id] = it }
  c.setMediaItems(items.map { it.toMediaItem() }, index, 0L)
  c.prepare()
  c.play()
  publish()
 }
 fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() }; publish() }
 fun pause() { controller?.pause(); publish() }
 fun next() { controller?.seekToNextMediaItem(); publish() }
 fun previous() { controller?.seekToPreviousMediaItem(); publish() }
 fun seek(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0)); publish() }
 fun setShuffle(enabled: Boolean) { controller?.shuffleModeEnabled = enabled; publish() }
 fun cycleRepeat() {
  controller?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF } }
  publish()
 }
 fun playQueueIndex(index: Int) { controller?.seekTo(index, 0L); controller?.play(); publish() }
 fun append(tracks: List<Track>) {
  val c = controller ?: run { pendingAppend += tracks; return }
  val known = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }.toSet()
  tracks.filter { it.id !in known }.forEach { knownTracks[it.id] = it; c.addMediaItem(it.toMediaItem()) }
  publish()
 }
 fun close() {
  controller?.removeListener(listener)
  controllerFuture?.let { MediaController.releaseFuture(it) }
  controller = null
  scope.cancel()
 }
 private fun publish() {
  val c = controller ?: return
  val index = c.currentMediaItemIndex.coerceAtLeast(0)
  val items = (0 until c.mediaItemCount).mapNotNull { i ->
   val media = c.getMediaItemAt(i)
   (media.localConfiguration?.tag as? Track) ?: knownTracks[media.mediaId]
  }
  val currentMedia = c.currentMediaItem
  val current = (currentMedia?.localConfiguration?.tag as? Track) ?: currentMedia?.let { knownTracks[it.mediaId] }
  _state.value = PlayerSnapshot(current, items, index, c.isPlaying, c.currentPosition.coerceAtLeast(0), c.duration.takeIf { it > 0 } ?: 0L, c.shuffleModeEnabled, when (c.repeatMode) { Player.REPEAT_MODE_ALL -> RepeatMode.ALL; Player.REPEAT_MODE_ONE -> RepeatMode.ONE; else -> RepeatMode.OFF }, playbackContext)
 }
 private fun Track.toMediaItem(): MediaItem {
  val uri = localPath?.takeIf { it.isNotBlank() }?.let { Uri.fromFile(java.io.File(it)) } ?: Uri.parse(streamUrl)
  return MediaItem.Builder().setMediaId(id).setUri(uri).setMediaMetadata(
   MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setArtworkUri(Uri.parse(artworkUrl).takeIf { artworkUrl.startsWith("https://") }).build()
  ).setTag(this).build()
 }
}
