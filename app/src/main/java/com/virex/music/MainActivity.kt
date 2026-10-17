package com.virex.music

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Black = Color(0xFF080808)
private val Panel = Color(0xFF191919)
private val Lime = Color(0xFFB7FF2A)
private val Grey = Color(0xFF999999)

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controllerFuture = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
        setContent { NativeVirex(this, controllerFuture) }
    }
    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeVirex(context: Context, future: ListenableFuture<MediaController>?) {
    val repo = remember { MusicRepository() }
    val scope = rememberCoroutineScope()
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf(emptyList<Track>()) }
    var downloads by remember { mutableStateOf(repo.getDownloads(context)) }
    var current by remember { mutableStateOf<Track?>(null) }
    var playing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Search a song or artist to get started.") }
    var showDownloads by remember { mutableStateOf(false) }

    LaunchedEffect(future) {
        runCatching { future?.get() }.onSuccess { controller = it }
            .onFailure { message = "Player is connecting. Tap a song again if needed." }
    }
    DisposableEffect(controller) {
        val c = controller
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isNowPlaying: Boolean) { playing = isNowPlaying }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                current = c?.currentMediaItem?.localConfiguration?.tag as? Track
            }
        }
        c?.addListener(listener)
        onDispose { c?.removeListener(listener) }
    }

    fun play(track: Track, queue: List<Track>) {
        val c = controller
        if (c == null) { message = "Player is starting. Try again."; return }
        val list = queue.ifEmpty { listOf(track) }
        val index = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        c.setMediaItems(list.map { t ->
            MediaItem.Builder().setMediaId(t.id)
                .setUri(t.localPath?.let { "file://$it" } ?: t.streamUrl)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist)
                    .setArtworkUri(t.image.takeIf { it.startsWith("http") }?.let(android.net.Uri::parse)).build())
                .setTag(t).build()
        }, index, 0L)
        current = track
        c.prepare()
        c.play()
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Lime, onPrimary = Black, background = Black, surface = Panel, onSurface = Color.White)) {
        Scaffold(containerColor = Black, topBar = {
            TopAppBar(title = { Column {
                Text("V I R E X", fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text("YOUR MUSIC. YOUR FLOW.", color = Grey, fontSize = 10.sp, letterSpacing = 1.sp)
            } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Black, titleContentColor = Color.White))
        }, bottomBar = {
            Column(Modifier.background(Black)) {
                current?.let { track ->
                    Row(Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(14.dp)).background(Panel).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = track.image, contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(track.artist, color = Grey, fontSize = 12.sp)
                        }
                        IconButton(onClick = { if (controller?.isPlaying == true) controller?.pause() else controller?.play() }) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", tint = Lime)
                        }
                    }
                }
                NavigationBar(containerColor = Color(0xFF101010)) {
                    NavigationBarItem(selected = !showDownloads, onClick = { showDownloads = false }, icon = { Icon(Icons.Default.Search, "Search") }, label = { Text("Discover") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Lime, selectedTextColor = Lime, indicatorColor = Color(0xFF29351A)))
                    NavigationBarItem(selected = showDownloads, onClick = { showDownloads = true; downloads = repo.getDownloads(context) }, icon = { Icon(Icons.Default.Download, "Downloads") }, label = { Text("Downloads") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Lime, selectedTextColor = Lime, indicatorColor = Color(0xFF29351A)))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                if (!showDownloads) {
                    Text("YOUR SOUNDTRACK.", fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("Search, play, save for offline.", color = Grey)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Songs, artists, albums…", color = Grey) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Lime) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (query.isNotBlank()) scope.launch {
                                busy = true; message = "Searching…"
                                try { songs = withContext(Dispatchers.IO) { repo.search(query) }; message = songs.size.toString() + " results" }
                                catch (e: Exception) { message = e.message ?: "Search failed" }
                                finally { busy = false }
                            }
                        }), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Lime, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Lime))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(message, color = Grey, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Lime, strokeWidth = 2.dp)
                        else TextButton(onClick = {
                            if (query.isNotBlank()) scope.launch {
                                busy = true
                                try { songs = withContext(Dispatchers.IO) { repo.search(query) }; message = songs.size.toString() + " results" }
                                catch (e: Exception) { message = e.message ?: "Search failed" }
                                finally { busy = false }
                            }
                        }) { Text("SEARCH", color = Lime) }
                    }
                    SongRows(songs, downloads, downloading, onPlay = { play(it, songs) }, onDownload = { track ->
                        scope.launch {
                            downloading = track.id
                            try { repo.download(context, track); downloads = repo.getDownloads(context); message = "Saved for offline playback" }
                            catch (e: Exception) { message = e.message ?: "Download failed" }
                            finally { downloading = "" }
                        }
                    })
                } else {
                    Text("OFFLINE LIBRARY", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Downloaded on this device", color = Grey)
                    Spacer(Modifier.height(12.dp))
                    if (downloads.isEmpty()) Text("No downloads yet. Search for music and tap download.", color = Grey)
                    SongRows(downloads, downloads, downloading, onPlay = { track ->
                        if (track.localPath != null && File(track.localPath).exists()) play(track, downloads)
                        else message = "File missing. Download this song again."
                    }, onDownload = {})
                }
            }
        }
    }
}

@Composable
private fun SongRows(tracks: List<Track>, downloads: List<Track>, downloading: String, onPlay: (Track) -> Unit, onDownload: (Track) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(tracks, key = { it.id }) { track ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = track.image, contentDescription = "Album art", modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).clickable { onPlay(track) })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { onPlay(track) }) {
                    Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = Grey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (downloading == track.id) CircularProgressIndicator(Modifier.size(22.dp), color = Lime, strokeWidth = 2.dp)
                else IconButton(onClick = { if (downloads.any { it.id == track.id }) onPlay(downloads.first { it.id == track.id }) else onDownload(track) }) {
                    Icon(if (downloads.any { it.id == track.id }) Icons.Default.PlayArrow else Icons.Default.Download, "Download or play offline", tint = Lime)
                }
            }
        }
    }
}
