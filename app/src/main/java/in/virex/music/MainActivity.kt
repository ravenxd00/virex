@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package in.virex.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import in.virex.music.core.design.*
import in.virex.music.domain.model.*
import in.virex.music.feature.VirexViewModel
import in.virex.music.player.QueuePolicy
import in.virex.music.persistence.AppSettings
import in.virex.music.persistence.BackupDocument
import in.virex.music.persistence.BackupManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val destinations = listOf("home", "search", "playlists", "downloads")
private val categories = listOf("Trending", "Punjabi", "Hip Hop", "Bollywood", "Global Hits", "Lofi", "EDM")

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor = android.graphics.Color.BLACK
  window.navigationBarColor = android.graphics.Color.BLACK
  setContent { VirexTheme { VirexRoot() } }
 }
}

@Composable
private fun VirexRoot(vm: VirexViewModel = viewModel()) {
 val context = androidx.compose.ui.platform.LocalContext.current
 val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
 LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
 val app = context.applicationContext as VirexApplication
 val ui by vm.ui.collectAsStateWithLifecycle()
 val player by app.playerController.state.collectAsStateWithLifecycle()
 val nav = rememberNavController()
 val snackbarHostState = remember { SnackbarHostState() }
 var snackbar by remember { mutableStateOf<String?>(null) }
 LaunchedEffect(snackbar) { snackbar?.let { snackbarHostState.showSnackbar(it); snackbar = null } }
 val entry by nav.currentBackStackEntryAsState()
 val route = entry?.destination?.route ?: "home"
 var showSettings by remember { mutableStateOf(false) }
 var playerOpen by remember { mutableStateOf(false) }
 var selectedTrack by remember { mutableStateOf<Track?>(null) }
 var showTrackActions by remember { mutableStateOf(false) }
 var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
 var createPlaylist by remember { mutableStateOf(false) }
 var importPlaylist by remember { mutableStateOf(false) }
 var playlistDetail by remember { mutableStateOf<Playlist?>(null) }
 val scope = rememberCoroutineScope()
 val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
  if (uri != null) scope.launch(Dispatchers.IO) {
   val document = BackupDocument(playlists = ui.playlists, accent = ui.settings.accent, visualizer = ui.settings.visualizer, highQuality = ui.settings.highQuality, crossfade = ui.settings.crossfade)
   runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(BackupManager.encode(document).toByteArray(Charsets.UTF_8)) } ?: error("Unable to open backup destination.") }
    .onSuccess { scope.launch { snackbar = "Backup saved." } }
    .onFailure { error -> scope.launch { snackbar = error.message ?: "Backup failed." } }
  }
 }
 val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if (uri != null) scope.launch(Dispatchers.IO) {
   runCatching {
    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Unable to read backup file.")
    BackupManager.decode(json).getOrThrow()
   }.onSuccess { document -> scope.launch {
    vm.replacePlaylists(document.playlists)
    vm.updateSettings { it.copy(accent = document.accent, visualizer = document.visualizer, highQuality = document.highQuality, crossfade = document.crossfade) }
    snackbar = "Backup restored."
   } }.onFailure { error -> scope.launch { snackbar = error.message ?: "Invalid backup file." } }
  }
 }
 var importWasBusy by remember { mutableStateOf(false) }
 LaunchedEffect(ui.importBusy) { if (ui.importBusy) importWasBusy = true else if (importPlaylist && importWasBusy) { importPlaylist = false; importWasBusy = false; snackbar = ui.error ?: ("Imported " + ui.importMatched + " tracks.") } }
 LaunchedEffect(ui.error) { ui.error?.let { snackbar = it } }
 Scaffold(
  containerColor = VirexBlack,
  snackbarHost = { SnackbarHost(snackbarHostState) },
  bottomBar = {
   Column(Modifier.fillMaxWidth().background(Color(0xFF0B0B0B))) {
    AnimatedVisibility(player.current != null && !playerOpen) {
     MiniPlayer(player.current, player.isPlaying, player.positionMs, player.durationMs, { playerOpen = true }, { app.playerController.toggle() }, { app.playerController.next() })
    }
    NavigationBar(containerColor = Color(0xFF0B0B0B), contentColor = ui.settings.accent.toColor(), tonalElevation = 0.dp, windowInsets = WindowInsets.navigationBars) {
     val labels = listOf("Home", "Search", "Playlists", "Downloads")
     val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.LibraryMusic, Icons.Default.Download)
     destinations.forEachIndexed { i, destination ->
      NavigationBarItem(selected = route == destination, onClick = { nav.navigate(destination) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(icons[i], null) }, label = { Text(labels[i], fontSize = 11.sp, fontWeight = if (route == destination) FontWeight.SemiBold else FontWeight.Normal) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = ui.settings.accent.toColor(), selectedTextColor = ui.settings.accent.toColor(), indicatorColor = ui.settings.accent.toColor().copy(alpha = .13f), unselectedIconColor = Color(0xFFC8C5CD), unselectedTextColor = Color(0xFFC8C5CD)))
     }
    }
   }
  }
 ) { padding ->
  Box(Modifier.fillMaxSize().padding(padding).background(VirexBlack)) {
   NavHost(navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
    composable("home") {
     HomeScreen(ui, { vm.loadCategory(it) }, vm::refreshCategory, { track ->
      selectedTrack = track; val queue = ui.home
      app.playerController.play(track, queue, PlaybackContext.RADIO); playerOpen = true
      scope.launch { app.repository.suggestions(track, ui.settings.highQuality).getOrNull()?.let { app.playerController.append(it) } }
     }, { addToPlaylistTrack = it }, { selectedTrack = it; showTrackActions = true }, { showSettings = true }, { snackbar = it })
    }
    composable("search") {
     SearchScreen(ui, vm, { track -> app.playerController.play(track, ui.results, PlaybackContext.SEARCH); playerOpen = true }, { addToPlaylistTrack = it }, { selectedTrack = it; showTrackActions = true })
    }
    composable("playlists") {
     PlaylistsScreen(ui.playlists, vm, { createPlaylist = true }, { importPlaylist = true }, { playlistDetail = it })
    }
    composable("downloads") {
     DownloadsScreen(ui.downloads, { track -> app.playerController.play(track, ui.downloads, PlaybackContext.DOWNLOADS); playerOpen = true }, { snackbar = it })
    }
   }
   if (playerOpen && player.current != null) PlayerScreen(player, ui.settings, { playerOpen = false }, { app.playerController.toggle() }, app.playerController::next, app.playerController::previous, app.playerController::seek, app.playerController::setShuffle, app.playerController::cycleRepeat, { index -> app.playerController.playQueueIndex(index) }, { track -> addToPlaylistTrack = track }, { track -> vm.download(track); snackbar = "Download queued" }, { track -> shareTrack(context, track) })
  }
 }
 if (showSettings) SettingsDialog(ui.settings, { showSettings = false }, vm::updateSettings, { backupLauncher.launch("VirexDL-backup.json") }, { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) })
 if (showTrackActions && selectedTrack != null) TrackActionsDialog(selectedTrack!!, { showTrackActions = false }, { addToPlaylistTrack = selectedTrack; showTrackActions = false }, { vm.download(selectedTrack!!); snackbar = "Download started"; showTrackActions = false }, { shareTrack(context, selectedTrack!!); showTrackActions = false })
 if (addToPlaylistTrack != null) AddToPlaylistDialog(ui.playlists, { addToPlaylistTrack = null }, { playlist ->
  vm.addTrackToPlaylist(playlist.id, addToPlaylistTrack!!); snackbar = "Added to ${playlist.name}"; addToPlaylistTrack = null
 }, { vm.createPlaylist("My Playlist", listOf(addToPlaylistTrack!!)); snackbar = "Playlist created"; addToPlaylistTrack = null })
 if (createPlaylist) CreatePlaylistDialog({ createPlaylist = false }, { vm.createPlaylist(it); createPlaylist = false; snackbar = "Playlist created" })
 if (importPlaylist) ImportPlaylistDialog({ importPlaylist = false }, { name, lines -> vm.importPlaylistFromNames(name, lines) }, ui.importBusy, ui.importProgress, ui.importMatched)
 if (playlistDetail != null) PlaylistDetailDialog(playlistDetail!!, ui, vm, { playlistDetail = null }, { track -> app.playerController.play(track, ui.home, PlaybackContext.PLAYLIST); playerOpen = true }, { snackbar = it })
}

@Composable private fun HomeScreen(ui: in.virex.music.feature.UiState, onCategory: (String)->Unit, onShuffle: ()->Unit, onPlay:(Track)->Unit, onAdd:(Track)->Unit, onLong:(Track)->Unit, onSettings:()->Unit, onMessage:(String)->Unit) {
 LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
  item {
   Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 16.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
    AsyncImage(model = R.drawable.virex_logo, contentDescription = "Virex logo", modifier = Modifier.size(32.dp).clip(CircleShape))
    Spacer(Modifier.width(10.dp))
    Text("Virex", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    Text("DL", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = ui.settings.accent.toColor())
    Spacer(Modifier.weight(1f))
    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings", tint = Color.White, modifier = Modifier.size(27.dp)) }
   }
  }
  item {
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 18.dp, end = 8.dp, top = 22.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    categories.forEach { cat ->
     val active = ui.category == cat
     Box(Modifier.clip(RoundedCornerShape(18.dp)).background(if (active) ui.settings.accent.toColor() else Color.Transparent).then(if (!active) Modifier.border(1.dp, Color(0xFF77747C), RoundedCornerShape(18.dp)) else Modifier).combinedClickable(onClick = { onCategory(cat) }, onLongClick = { onMessage(cat) }).padding(horizontal = 19.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
      Text(cat, color = if (active) Color.Black else Color(0xFFC9C5CF), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
     }
    }
   }
  }
  item {
   Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("${ui.category} Mix", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EDF4), maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
    Row(Modifier.clip(RoundedCornerShape(20.dp)).combinedClickable(onClick = onShuffle, onLongClick = onShuffle).padding(horizontal = 7.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Refresh, null, tint = ui.settings.accent.toColor(), modifier = Modifier.size(17.dp)); Text(" Shuffle Mix", color = ui.settings.accent.toColor(), fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
   }
  }
  if (ui.busy && ui.home.isEmpty()) items(6) { SkeletonCardRow() }
  else if (ui.home.isEmpty()) item { EmptyState(ui.error ?: "No songs yet", "Retry", onShuffle) }
  else items(ui.home.chunked(2)) { pair ->
   Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
    pair.forEach { track -> Box(Modifier.weight(1f)) { SongCard(track, ui.settings.accent.toColor(), onPlay = { onPlay(track) }, onAdd = { onAdd(track) }, onLong = { onLong(track) }) } }
    if (pair.size == 1) Spacer(Modifier.weight(1f))
   }
  }
 }
}

@Composable private fun SongCard(track: Track, accent: Color, onPlay:()->Unit, onAdd:()->Unit, onLong:()->Unit) {
 Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(VirexCard).combinedClickable(onClick = onPlay, onLongClick = onLong).padding(12.dp)) {
  Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(17.dp)).background(VirexControl)) {
   AsyncImage(track.artworkUrl, null, Modifier.fillMaxSize())
   IconButton(onClick = onAdd, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(43.dp).background(Color.Black.copy(alpha = .66f), CircleShape)) { Icon(Icons.Default.Add, "Add to playlist", tint = Color.White, modifier = Modifier.size(27.dp)) }
  }
  Spacer(Modifier.height(12.dp))
  Text(track.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
  Text(track.artist, fontSize = 14.sp, color = VirexMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
  Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.End) { Icon(Icons.Default.Download, "Download available in song actions", tint = accent, modifier = Modifier.size(22.dp)) }
 }
}

@Composable private fun SearchScreen(ui: in.virex.music.feature.UiState, vm: VirexViewModel, onPlay:(Track)->Unit, onAdd:(Track)->Unit, onLong:(Track)->Unit) {
 Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
  Text("Search", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 16.dp, bottom = 15.dp))
  Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(VirexControl).padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
   Icon(Icons.Default.Search, null, tint = VirexMuted)
   Spacer(Modifier.width(10.dp))
   Box(Modifier.weight(1f)) {
    if (ui.query.isEmpty()) Text("Song, Artist or Album…", color = VirexMuted)
    BasicTextField(value = ui.query, onValueChange = vm::setQuery, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp), modifier = Modifier.fillMaxWidth())
   }
   if (ui.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }, modifier = Modifier.size(25.dp)) { Icon(Icons.Default.Close, "Clear", tint = VirexMuted) }
  }
  if (ui.query.isBlank() && ui.history.isNotEmpty()) {
   Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("Recent searches", color = Color.White, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); TextButton(onClick = vm::clearHistory) { Text("Clear All", color = ui.settings.accent.toColor()) } }
   ui.history.forEach { Text(it, color = VirexMuted, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).combinedClickable(onClick = { vm.setQuery(it); vm.searchNow(it) }, onLongClick = { vm.setQuery(it); vm.searchNow(it) }).padding(vertical = 10.dp)) }
  }
  when {
   ui.busy -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ui.settings.accent.toColor()) }
   ui.results.isNotEmpty() -> LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(ui.results, key = { it.id }) { t -> ResultRow(t, ui.settings.accent.toColor(), { onPlay(t) }, { onAdd(t) }, { onLong(t) }) } }
   ui.query.isNotBlank() && ui.error != null -> EmptyState(ui.error, "Retry") { vm.searchNow() }
   else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Find your next favourite.", color = VirexMuted) }
  }
 }
}

@Composable private fun ResultRow(track: Track, accent: Color, onPlay:()->Unit, onAdd:()->Unit, onLong:()->Unit) {
 Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(VirexCard).combinedClickable(onClick = onPlay, onLongClick = onLong).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
  AsyncImage(track.artworkUrl, null, Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)))
  Spacer(Modifier.width(12.dp))
  Column(Modifier.weight(1f)) { Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(track.artist, color = VirexMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
  IconButton(onClick = onAdd) { Icon(Icons.Default.Add, null, tint = accent) }
 }
}

@Composable private fun PlaylistsScreen(playlists: List<Playlist>, vm: VirexViewModel, onNew:()->Unit, onImport:()->Unit, onOpen:(Playlist)->Unit) {
 Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
  Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text("Playlists", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f)); TextButton(onClick = onImport) { Text("Import", color = VirexGreen) }; Button(onClick = onNew, colors = ButtonDefaults.buttonColors(containerColor = VirexGreen, contentColor = Color.Black)) { Icon(Icons.Default.Add, null); Text(" New") } }
  if (playlists.isEmpty()) EmptyState("No playlists yet.", "Create playlist", onNew)
  else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(playlists, key = { it.id }) { p -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VirexCard).combinedClickable(onClick = { onOpen(p) }, onLongClick = { vm.deletePlaylist(p.id) }).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(VirexControl), contentAlignment = Alignment.Center) { Icon(Icons.Default.LibraryMusic, null, tint = VirexGreen, modifier = Modifier.size(28.dp)) }; Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(p.name, color = Color.White, fontWeight = FontWeight.SemiBold); Text("${p.trackIds.size} songs", color = VirexMuted, fontSize = 12.sp) }; IconButton(onClick = { vm.deletePlaylist(p.id) }) { Icon(Icons.Default.Delete, "Delete playlist", tint = VirexMuted) } } } }
 }
}

@Composable private fun DownloadsScreen(downloads: List<Track>, onPlay:(Track)->Unit, onMessage:(String)->Unit) {
 Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
  Text("Downloads", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 18.dp))
  Text("Saved on this device · works offline", color = VirexMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
  Text("${downloads.size} songs · ${formatBytes(downloads.sumOf { it.fileSize })}", color = VirexMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
  if (downloads.isEmpty()) EmptyState("No downloads yet.", "Browse Home", { onMessage("Choose a song and use Download to save it offline.") })
  else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(downloads, key = { it.id }) { t -> ResultRow(t, VirexGreen, { onPlay(t) }, { onMessage("Already saved for offline listening") }, { onMessage("Offline file: ${t.localPath.orEmpty()}") }) } }
 }
}

@Composable private fun MiniPlayer(track: Track?, playing: Boolean, position: Long, duration: Long, onOpen:()->Unit, onToggle:()->Unit, onNext:()->Unit) {
 if (track == null) return
 Column(Modifier.fillMaxWidth().background(Color(0xFF111111)).combinedClickable(onClick = onOpen, onLongClick = onOpen).padding(horizontal = 12.dp, vertical = 8.dp)) {
  Row(verticalAlignment = Alignment.CenterVertically) {
   AsyncImage(track.artworkUrl.takeIf { it.isNotBlank() }, null, Modifier.size(43.dp).clip(RoundedCornerShape(8.dp)))
   Spacer(Modifier.width(10.dp))
   Column(Modifier.weight(1f)) { Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(track.artist, color = VirexMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
   IconButton(onClick = onToggle) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White) }
   IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, null, tint = Color.White) }
  }
  LinearProgressIndicator(progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(2.dp), color = VirexGreen, trackColor = Color(0xFF333333))
 }
}

@Composable private fun PlayerScreen(state: PlayerSnapshot, settings: AppSettings, onClose:()->Unit, onToggle:()->Unit, onNext:()->Unit, onPrevious:()->Unit, onSeek:(Long)->Unit, onShuffle:(Boolean)->Unit, onRepeat:()->Unit, onQueue:(Int)->Unit, onAdd:(Track)->Unit, onDownload:(Track)->Unit, onShare:(Track)->Unit) {
 val track = state.current ?: return
 var page by remember { mutableStateOf("Player") }
 var seek by remember(track.id) { mutableFloatStateOf(0f) }
 val accent = settings.accent.toColor()
 Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
  Column(Modifier.fillMaxSize().background(VirexBlack).statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
   Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Close player", tint = Color.White) }; Spacer(Modifier.weight(1f)); Text(when(state.context){PlaybackContext.RADIO->"VIREX RADIO";PlaybackContext.SEARCH->"SEARCH RESULTS";PlaybackContext.PLAYLIST->"PLAYLIST";PlaybackContext.DOWNLOADS->"OFFLINE"}, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp); Spacer(Modifier.weight(1f)); IconButton(onClick = { page = "Queue" }) { Icon(Icons.Default.QueueMusic, "Queue", tint = Color.White) } }
   Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(VirexControl).padding(4.dp)) { listOf("Player","Lyrics","Queue").forEach { p -> Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (page == p) accent.copy(alpha = .18f) else Color.Transparent).combinedClickable(onClick = { page = p }, onLongClick = { page = p }).padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text(p, color = if (page == p) accent else VirexMuted, fontWeight = FontWeight.SemiBold) } } }
   when (page) {
    "Queue" -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(state.queue) { t -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (t.id == track.id) accent.copy(alpha = .12f) else VirexCard).combinedClickable(onClick = { onQueue(state.queue.indexOf(t)); page = "Player" }, onLongClick = { onQueue(state.queue.indexOf(t)); page = "Player" }).padding(10.dp), verticalAlignment = Alignment.CenterVertically) { AsyncImage(t.artworkUrl, null, Modifier.size(45.dp).clip(RoundedCornerShape(8.dp))); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(t.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(t.artist, color = VirexMuted, fontSize = 12.sp) }; if (t.id == track.id) Icon(Icons.Default.PlayArrow, null, tint = accent) } } }
    "Lyrics" -> LyricsPanel(track, onShare)
    else -> {
     Spacer(Modifier.weight(.5f))
     AsyncImage(track.artworkUrl.ifBlank { null }, null, Modifier.fillMaxWidth(.86f).aspectRatio(1f).clip(RoundedCornerShape(20.dp)).align(Alignment.CenterHorizontally).background(VirexCard))
     Spacer(Modifier.height(24.dp))
     Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(track.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(track.artist, color = VirexMuted, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) }; IconButton(onClick = { onAdd(track) }) { Icon(Icons.Default.Add, "Add to playlist", tint = accent) } }
     Slider(value = if (state.durationMs > 0) state.positionMs.toFloat().div(state.durationMs).coerceIn(0f,1f) else seek, onValueChange = { seek = it }, onValueChangeFinished = { onSeek((seek * state.durationMs).toLong()) }, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0xFF333333)))
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(timeLabel(state.positionMs), color = VirexMuted, fontSize = 11.sp); Text(timeLabel(state.durationMs), color = VirexMuted, fontSize = 11.sp) }
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { onShuffle(!state.shuffle) }) { Icon(Icons.Default.Shuffle, null, tint = if (state.shuffle) accent else VirexMuted) }
      IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
      FilledIconButton(onClick = onToggle, modifier = Modifier.size(72.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = Color.Black)) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(38.dp)) }
      IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
      IconButton(onClick = onRepeat) { Icon(Icons.Default.Repeat, null, tint = if (state.repeat == RepeatMode.OFF) VirexMuted else accent) }
     }
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = { onDownload(track) }, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))) { Icon(Icons.Default.Download, null, tint = accent); Text("Download", color = Color.White) }; OutlinedButton(onClick = { onShare(track) }, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))) { Icon(Icons.Default.Share, null, tint = accent); Text("Share", color = Color.White) } }
     if (settings.visualizer) Visualizer(accent, state.isPlaying)
     Spacer(Modifier.weight(.35f))
    }
   }
  }
 }
}

@Composable private fun LyricsPanel(track: Track, onShare:(Track)->Unit) {
 val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as VirexApplication
 var lines by remember(track.id) { mutableStateOf<List<LyricsLine>>(emptyList()) }
 var busy by remember(track.id) { mutableStateOf(true) }
 var error by remember(track.id) { mutableStateOf<String?>(null) }
 LaunchedEffect(track.id) { val r = app.repository.lyrics(track); lines = r.getOrNull()?.lines.orEmpty(); error = r.exceptionOrNull()?.message ?: if (lines.isEmpty()) "Lyrics are unavailable for this track." else null; busy = false }
 Column(Modifier.fillMaxSize().padding(top = 18.dp)) {
  Row(verticalAlignment = Alignment.CenterVertically) { Text("Lyrics", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onClick = { onShare(track) }) { Text("Share", color = VirexGreen) } }
  when { busy -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = VirexGreen) }; lines.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error ?: "Instrumental or unavailable", color = VirexMuted) }; else -> LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) { items(lines) { line -> Text(line.text, color = Color.White.copy(alpha = .88f), fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 9.dp)) } } }
 }
}

@Composable private fun SettingsDialog(settings: AppSettings, onClose:()->Unit, update:((AppSettings)->AppSettings)->Unit, onBackup:()->Unit, onRestore:()->Unit) {
 AlertDialog(onDismissRequest = onClose, containerColor = Color(0xFF151515), title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) }, text = {
  Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
   Text("Accent colour", color = VirexMuted, fontSize = 12.sp)
   Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    listOf(0xFF1DB954,0xFF00D9FF,0xFFFF4D8D,0xFFB388FF,0xFFFFB020).forEach { colorLong -> val c = Color(colorLong); Box(Modifier.size(32.dp).clip(CircleShape).background(c).border(if (settings.accent == colorLong) 3.dp else 0.dp, Color.White, CircleShape).combinedClickable(onClick = { update { it.copy(accent = colorLong) } }, onLongClick = { update { it.copy(accent = colorLong) } }), contentAlignment = Alignment.Center) { if (settings.accent == colorLong) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(17.dp)) } }
   }
   SettingRow("Audio Visualizer", "Animated player visualizer", settings.visualizer) { v -> update { it.copy(visualizer = v) } }
   SettingRow("High Quality", "Prefer 320kbps when available", settings.highQuality) { v -> update { it.copy(highQuality = v) } }
   SettingRow("Crossfade", "Smooth 3s blend between tracks", settings.crossfade) { v -> update { it.copy(crossfade = v) } }
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onBackup, modifier = Modifier.weight(1f)) { Text("Backup", color = Color.White) }; OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) { Text("Restore", color = Color.White) } }
  }
 }, confirmButton = { TextButton(onClick = onClose) { Text("Done", color = VirexGreen) } })
}

@Composable private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange:(Boolean)->Unit) {
 Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(subtitle, color = VirexMuted, fontSize = 11.sp) }; Switch(checked = checked, onCheckedChange = onChange) }
}

@Composable private fun TrackActionsDialog(track: Track, close:()->Unit, add:()->Unit, download:()->Unit, share:()->Unit) {
 AlertDialog(onDismissRequest = close, containerColor = Color(0xFF171717), title = { Text(track.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) }, text = {
  Column {
   Text(track.artist, color = VirexMuted, modifier = Modifier.padding(bottom = 12.dp))
   TextButton(onClick = add) { Icon(Icons.Default.Add, null, tint = VirexGreen); Spacer(Modifier.width(8.dp)); Text("Add to Playlist", color = Color.White) }
   TextButton(onClick = download) { Icon(Icons.Default.Download, null, tint = VirexGreen); Spacer(Modifier.width(8.dp)); Text("Download", color = Color.White) }
   TextButton(onClick = share) { Icon(Icons.Default.Share, null, tint = VirexGreen); Spacer(Modifier.width(8.dp)); Text("Share Song", color = Color.White) }
  }
 }, confirmButton = { TextButton(onClick = close) { Text("Cancel", color = VirexMuted) } })
}

@Composable private fun AddToPlaylistDialog(playlists: List<Playlist>, close:()->Unit, choose:(Playlist)->Unit, create:()->Unit) {
 AlertDialog(onDismissRequest = close, containerColor = Color(0xFF171717), title = { Text("Add to Playlist", color = Color.White) }, text = {
  Column {
   if (playlists.isEmpty()) Text("No playlists yet.", color = VirexMuted)
   playlists.forEach { p -> TextButton(onClick = { choose(p) }, modifier = Modifier.fillMaxWidth()) { Text("${p.name} · ${p.trackIds.size} songs", color = Color.White) } }
   TextButton(onClick = create) { Icon(Icons.Default.Add, null, tint = VirexGreen); Text("New Playlist", color = VirexGreen) }
  }
 }, confirmButton = { TextButton(onClick = close) { Text("Cancel", color = VirexMuted) } })
}

@Composable private fun CreatePlaylistDialog(close:()->Unit, create:(String)->Unit) {
 var name by remember { mutableStateOf("My Playlist") }
 AlertDialog(onDismissRequest = close, containerColor = Color(0xFF171717), title = { Text("New Playlist", color = Color.White) }, text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Playlist name") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VirexGreen, focusedLabelColor = VirexGreen, cursorColor = VirexGreen)) }, confirmButton = { TextButton(onClick = { create(name.trim().ifBlank { "My Playlist" }) }) { Text("Create", color = VirexGreen) } }, dismissButton = { TextButton(onClick = close) { Text("Cancel", color = VirexMuted) } })
}

@Composable private fun ImportPlaylistDialog(close:()->Unit, onImport:(String,List<String>)->Unit, busy:Boolean, progress:Int, matched:Int) {
 var name by remember { mutableStateOf("Imported Playlist") }
 var songs by remember { mutableStateOf("") }
 AlertDialog(onDismissRequest = { if (!busy) close() }, containerColor = Color(0xFF171717), title = { Text("Import Playlist", color = Color.White) }, text = {
  Column {
   OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist title") }, singleLine = true, enabled = !busy)
   Spacer(Modifier.height(8.dp))
   OutlinedTextField(value = songs, onValueChange = { songs = it }, label = { Text("Paste song names, one per line") }, minLines = 4, maxLines = 8, enabled = !busy)
   if (busy) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth(), color = VirexGreen); Text("Resolving songs… $progress% · $matched matched", color = VirexMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
  }
 }, confirmButton = { TextButton(enabled = !busy && songs.lines().any { it.isNotBlank() }, onClick = { onImport(name.ifBlank { "Imported Playlist" }, songs.lines()) }) { Text(if (busy) "Importing…" else "Import", color = VirexGreen) } }, dismissButton = { TextButton(enabled = !busy, onClick = close) { Text("Cancel", color = VirexMuted) } })
}
@Composable private fun PlaylistDetailDialog(playlist: Playlist, ui: in.virex.music.feature.UiState, vm: VirexViewModel, close:()->Unit, play:(Track)->Unit, message:(String)->Unit) {
 val tracks = playlist.trackIds.mapNotNull { id -> ui.home.firstOrNull { it.id == id } ?: ui.results.firstOrNull { it.id == id } ?: ui.downloads.firstOrNull { it.id == id } }
 AlertDialog(onDismissRequest = close, containerColor = Color(0xFF151515), title = { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = close) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }; Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold) } }, text = {
  Column {
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { tracks.firstOrNull()?.let(play) }, colors = ButtonDefaults.buttonColors(containerColor = VirexGreen, contentColor = Color.Black)) { Icon(Icons.Default.PlayArrow, null); Text("Play all") }; Text("${playlist.trackIds.size} songs", color = VirexMuted, modifier = Modifier.align(Alignment.CenterVertically)) }
   if (tracks.isEmpty()) Text("Search for songs and add them to this playlist.", color = VirexMuted, modifier = Modifier.padding(top = 12.dp))
   tracks.forEach { t -> Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f).combinedClickable(onClick = { play(t) }, onLongClick = { play(t) })) { Text(t.title, color = Color.White, maxLines = 1); Text(t.artist, color = VirexMuted, fontSize = 11.sp) }; IconButton(onClick = { vm.removeTrackFromPlaylist(playlist.id, t.id); message("Removed from playlist") }) { Icon(Icons.Default.Close, null, tint = VirexMuted) } } }
  }
 }, confirmButton = { TextButton(onClick = close) { Text("Close", color = VirexGreen) } })
}

@Composable private fun EmptyState(message: String, action: String, onAction:()->Unit) {
 Column(Modifier.fillMaxWidth().padding(vertical = 42.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
  Icon(Icons.Default.Album, null, tint = VirexGreen.copy(alpha = .7f), modifier = Modifier.size(42.dp))
  Spacer(Modifier.height(12.dp)); Text(message, color = VirexMuted, fontSize = 14.sp)
  Spacer(Modifier.height(8.dp)); TextButton(onClick = onAction) { Text(action, color = VirexGreen) }
 }
}
@Composable private fun SkeletonCardRow() {
 Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
  repeat(2) { Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(VirexCard).padding(12.dp)) { Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(15.dp)).background(VirexControl)); Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth(.75f).height(14.dp).clip(CircleShape).background(Color(0xFF242424))); Spacer(Modifier.height(8.dp)); Box(Modifier.fillMaxWidth(.5f).height(12.dp).clip(CircleShape).background(Color(0xFF202020))) } }
 }
}
@Composable private fun Visualizer(accent: Color, playing: Boolean) {
 Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { repeat(25) { i -> Box(Modifier.padding(horizontal = 2.dp).width(3.dp).height((if (playing) 8 + (i * 17 % 29) else 5).dp).clip(CircleShape).background(accent.copy(alpha = if (playing) .9f else .3f))) } }
}
private fun Long.toColor(): Color = Color(this)
private fun AppSettings.accentColor() = Color(accent)
private fun AppSettings.accentColorLong() = accent.toColor()
private fun Long.formatBytes(): String = formatBytes(this)
private fun formatBytes(bytes: Long): String = when { bytes < 1024 -> "${bytes} B"; bytes < 1024*1024 -> "${bytes/1024} KB"; else -> String.format("%.1f MB", bytes / (1024.0*1024.0)) }
private fun timeLabel(ms: Long): String = "%d:%02d".format((ms.coerceAtLeast(0)/1000)/60, (ms.coerceAtLeast(0)/1000)%60)
private fun shareTrack(context: android.content.Context, track: Track) {
 val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${track.title} — ${track.artist}")
 context.startActivity(Intent.createChooser(send, "Share song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
