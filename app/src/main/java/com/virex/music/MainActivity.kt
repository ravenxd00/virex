package com.virex.music

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.virex.music.data.*
import com.virex.music.player.PlayerState
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.theme.VirexMusicTheme
import kotlinx.coroutines.launch

private val Black = Color.Black
private val Glass = Color(0xFF111111)
private val Line = Color.White.copy(alpha = .14f)
private val Grey = Color(0xFF9A9A9A)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor = android.graphics.Color.BLACK
  window.navigationBarColor = android.graphics.Color.BLACK
  setContent { VirexMusicTheme { VirexApp() } }
 }
}

@Composable
private fun VirexApp(vm: PlayerViewModel = viewModel()) {
 val context = androidx.compose.ui.platform.LocalContext.current
 val dao = remember { AppDatabase.get(context).dao() }
 val scope = rememberCoroutineScope()
 val player by vm.state.collectAsState()
 var tab by remember { mutableStateOf("Home") }
 var query by remember { mutableStateOf("") }
 var results by remember { mutableStateOf(emptyList<Song>()) }
 var trending by remember { mutableStateOf(emptyList<Song>()) }
 var recents by remember { mutableStateOf(emptyList<RecentSearch>()) }
 var playlists by remember { mutableStateOf(emptyList<PlaylistEntity>()) }
 var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
 var playlistSongs by remember { mutableStateOf(emptyList<PlaylistSongEntity>()) }
 var localFiles by remember { mutableStateOf(emptyList<Pair<String,String>>()) }
 var busy by remember { mutableStateOf(false) }
 var message by remember { mutableStateOf<String?>(null) }
 var showPlayer by remember { mutableStateOf(false) }
 var showSettings by remember { mutableStateOf(false) }
 var createDialog by remember { mutableStateOf(false) }
 var newName by remember { mutableStateOf("") }
 var highQuality by remember { mutableStateOf(true) }
 var crossfade by remember { mutableIntStateOf(0) }
 var animationSpeed by remember { mutableFloatStateOf(1f) }
 var pureBlack by remember { mutableStateOf(true) }
 var sleepMinutes by remember { mutableIntStateOf(0) }
 val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if (uri != null) {
   runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
   vm.playLocal(uri.toString(), uri.lastPathSegment?.substringAfterLast('/') ?: "Audio file")
   showPlayer = true
  }
 }
 val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted) localFiles = queryLocalAudio(context)
 }
 LaunchedEffect(Unit) {
  recents = dao.recent()
  playlists = dao.playlists()
  busy = true
  runCatching { MusicApi.search("trending hindi songs") }
   .onSuccess { trending = it }
   .onFailure { message = "You're offline or the music service is unavailable." }
  busy = false
 }
 fun playSong(song: Song, queue: List<Song> = listOf(song)) {
  vm.setHighQuality(highQuality)
  vm.play(song, queue)
  showPlayer = true
 }
 fun searchNow(text: String) {
  if (text.isBlank()) return
  scope.launch {
   busy = true; message = null
   runCatching { MusicApi.search(text.trim()) }
    .onSuccess { found -> results = found; dao.saveSearch(RecentSearch(text.trim())); recents = dao.recent() }
    .onFailure { message = "Search failed. Check your connection and try again." }
   busy = false
  }
 }
 Scaffold(
  containerColor = Black,
  bottomBar = {
   Column(Modifier.fillMaxWidth().background(Black)) {
    if (player.song != null && !showPlayer) MiniPlayer(player, { showPlayer = true }, vm::toggle, vm::next)
    Row(Modifier.fillMaxWidth().border(1.dp, Line).padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
     val destinations = listOf("Home" to Icons.Default.Home, "Search" to Icons.Default.Search, "Library" to Icons.Default.LibraryMusic, "Your Files" to Icons.Default.Folder)
     destinations.forEach { (name, icon) ->
      Column(Modifier.weight(1f).clickable { tab = name; selectedPlaylist = null }.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
       Icon(icon, null, tint = if (tab == name) Color.White else Grey, modifier = Modifier.size(20.dp))
       Text(name, color = if (tab == name) Color.White else Grey, fontSize = 9.sp)
      }
     }
    }
   }
  }
 ) { insets ->
  Box(Modifier.fillMaxSize().padding(insets).background(if (pureBlack) Black else Color(0xFF080808))) {
   AnimatedContent(targetState = tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "main-tabs") { screen ->
    when (screen) {
     "Home" -> HomeScreen(trending, busy, message, player.song?.artwork.orEmpty(), { playSong(it, trending) }, { category -> tab = "Search"; query = category; searchNow(category) }, { showSettings = true })
     "Search" -> SearchScreen(query, { query = it }, results, recents, busy, message, ::searchNow, { scope.launch { dao.clearSearches(); recents = emptyList() } }, { playSong(it, results) }, { song ->
      scope.launch {
       val id = dao.createPlaylist(PlaylistEntity(name = "Favorites"))
       dao.addSong(song.toPlaylist(id, 0)); playlists = dao.playlists()
      }
     })
     "Library" -> LibraryScreen(playlists, selectedPlaylist, playlistSongs, { createDialog = true }, { p -> selectedPlaylist = p; scope.launch { playlistSongs = dao.songs(p.id) } }, { p ->
      scope.launch { dao.deletePlaylist(p.id); playlists = dao.playlists(); if (selectedPlaylist?.id == p.id) selectedPlaylist = null }
     }, { songs -> songs.firstOrNull()?.let { playSong(it.toSong(), songs.map { row -> row.toSong() }) } }, { song ->
      selectedPlaylist?.let { p -> scope.launch { dao.addSong(song.toPlaylist(p.id, playlistSongs.size)); playlistSongs = dao.songs(p.id) } }
     }, { row -> selectedPlaylist?.let { p -> scope.launch { dao.removeSong(p.id, row.songId); playlistSongs = dao.songs(p.id) } } })
     "Your Files" -> FilesScreen(localFiles, { if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.READ_MEDIA_AUDIO) else permission.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }, { picker.launch(arrayOf("audio/*")) }, { uri, title -> vm.playLocal(uri, title); showPlayer = true })
     else -> SettingsScreen(highQuality, { highQuality = it; vm.setHighQuality(it) }, crossfade, { crossfade = it }, animationSpeed, { animationSpeed = it }, pureBlack, { pureBlack = it }, { scope.launch { dao.clearSearches(); recents = emptyList() } })
    }
   }
   if (showPlayer && player.song != null) PlayerScreen(player, highQuality, { highQuality = it; vm.setHighQuality(it) }, vm::toggle, vm::next, vm::previous, vm::seek, vm::shuffle, vm::repeat, vm::jump, { showPlayer = false }, sleepMinutes, { sleepMinutes = it; vm.sleep(it) }, { scope.launch { val id = dao.createPlaylist(PlaylistEntity(name = "Favorites")); dao.addSong(it.toPlaylist(id, 0)); playlists = dao.playlists() } })
   if (showSettings) SettingsDialog(highQuality, { highQuality = it; vm.setHighQuality(it) }, crossfade, { crossfade = it }, animationSpeed, { animationSpeed = it }, pureBlack, { pureBlack = it }, { showSettings = false }, { scope.launch { dao.clearSearches(); recents = emptyList() } })
   if (createDialog) AlertDialog(onDismissRequest = { createDialog = false }, title = { Text("Create playlist") }, text = { OutlinedTextField(newName, { newName = it }, singleLine = true, label = { Text("Playlist name") }) }, confirmButton = { TextButton(onClick = { scope.launch { dao.createPlaylist(PlaylistEntity(name = newName.ifBlank { "New playlist" })); playlists = dao.playlists(); newName = ""; createDialog = false } }) { Text("Create") } }, dismissButton = { TextButton(onClick = { createDialog = false }) { Text("Cancel") } })
   player.error?.let { Text(it, color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).background(Glass, RoundedCornerShape(10.dp)).padding(10.dp)) }
  }
 }
}

@Composable private fun HomeScreen(songs: List<Song>, busy: Boolean, message: String?, artwork: String, onPlay: (Song)->Unit, onCategory:(String)->Unit, onSettings:()->Unit) {
 LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp)) {
  item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Virex Music", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f)); GlassIcon(Icons.Default.Settings, onSettings) } }
  item { Text("DISCOVER", color = Grey, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 26.dp, bottom = 12.dp)) }
  item { Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Trending","Punjabi","Hip Hop","Bollywood","Lofi").forEach { category -> Text(category, color = Color.White, fontSize = 11.sp, modifier = Modifier.clip(CircleShape).border(1.dp, Line, CircleShape).clickable { onCategory(category) }.padding(horizontal = 12.dp, vertical = 8.dp)) } } }
  item { SectionHeader("TRENDING MIX") }
  if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.White, trackColor = Glass) }
  if (message != null && songs.isEmpty()) item { Text(message, color = Grey, fontSize = 12.sp) }
  items(songs.take(8), key = { it.id }) { SongRow(it, onPlay, {}) }
  item { SectionHeader("QUICK PICKS") }
  items(songs.drop(8).take(8), key = { it.id }) { SongRow(it, onPlay, {}) }
  item { SectionHeader("RECENTLY PLAYED") }
  item { Text("Recently played tracks will appear here.", color = Grey, fontSize = 12.sp) }
 }
}

@Composable private fun SearchScreen(query:String,onQuery:(String)->Unit,results:List<Song>,recents:List<RecentSearch>,busy:Boolean,message:String?,onSearch:(String)->Unit,onClear:()->Unit,onPlay:(Song)->Unit,onAdd:(Song)->Unit) {
 Column(Modifier.fillMaxSize().padding(horizontal=18.dp)) {
  Text("Search",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Light,modifier=Modifier.padding(top=12.dp,bottom=12.dp))
  OutlinedTextField(query,onQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("Song, artist or album")},trailingIcon={IconButton(onClick={onSearch(query)}){Icon(Icons.Default.Search,null)}},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={onSearch(query)}))
  if(query.isBlank()&&recents.isNotEmpty()) { Row(verticalAlignment=Alignment.CenterVertically){Text("RECENT SEARCHES",color=Grey,fontSize=10.sp,letterSpacing=1.5.sp,modifier=Modifier.weight(1f));TextButton(onClick=onClear){Text("Clear all",color=Grey)}};recents.forEach{Text(it.query,color=Color.White,fontSize=13.sp,modifier=Modifier.fillMaxWidth().clickable{onQuery(it.query);onSearch(it.query)}.padding(vertical=8.dp))} }
  if(busy) LinearProgressIndicator(Modifier.fillMaxWidth(),color=Color.White,trackColor=Glass)
  if(message!=null) Text(message,color=Grey,fontSize=12.sp)
  LazyColumn { items(results,key={it.id}){SongRow(it,onPlay,onAdd)} }
 }
}

@Composable private fun LibraryScreen(playlists:List<PlaylistEntity>,selected:PlaylistEntity?,songs:List<PlaylistSongEntity>,onCreate:()->Unit,onOpen:(PlaylistEntity)->Unit,onDelete:(PlaylistEntity)->Unit,onPlay:(List<PlaylistSongEntity>)->Unit,onAdd:(Song)->Unit,onRemove:(PlaylistSongEntity)->Unit) {
 Column(Modifier.fillMaxSize().padding(18.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically){Text(selected?.name?: "Library",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Light,modifier=Modifier.weight(1f));GlassIcon(Icons.Default.Add,onCreate)}
  if(selected==null) LazyColumn { items(playlists,key={it.id}){p->Row(Modifier.fillMaxWidth().padding(vertical=6.dp).clip(RoundedCornerShape(16.dp)).background(Glass).border(1.dp,Line,RoundedCornerShape(16.dp)).clickable{onOpen(p)}.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Folder,null,tint=Color.White);Spacer(Modifier.width(12.dp));Text(p.name,color=Color.White,modifier=Modifier.weight(1f));IconButton(onClick={onDelete(p)}){Icon(Icons.Default.Delete,null,tint=Grey)}}} }
  else {
   Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick={onOpen(PlaylistEntity(0,""))}){Text("← All playlists",color=Grey)};Spacer(Modifier.weight(1f));TextButton(onClick={onPlay(songs)}){Text("Play all",color=Color.White)}}
   LazyColumn { items(songs,key={it.songId}){row->SongRow(row.toSong(),{onPlay(listOf(row))},{onRemove(row)})} }
  }
 }
}

@Composable private fun FilesScreen(files:List<Pair<String,String>>,onScan:()->Unit,onPick:()->Unit,onPlay:(String,String)->Unit) {
 Column(Modifier.fillMaxSize().padding(18.dp)) {
  Text("Your Files",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Light)
  Text("This tab only plays audio files already on your phone. Streamed songs are never downloaded or written to storage.",color=Grey,fontSize=11.sp,modifier=Modifier.padding(vertical=12.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onScan){Text("Scan device")};OutlinedButton(onClick=onPick){Text("Choose audio")}}
  LazyColumn { items(files){(uri,title)->Text(title,color=Color.White,fontSize=13.sp,modifier=Modifier.fillMaxWidth().clickable{onPlay(uri,title)}.padding(vertical=12.dp))} }
 }
}

@Composable private fun PlayerScreen(state:PlayerState,hq:Boolean,onHq:(Boolean)->Unit,onToggle:()->Unit,onNext:()->Unit,onPrevious:()->Unit,onSeek:(Int)->Unit,onShuffle:()->Unit,onRepeat:()->Unit,onJump:(Int)->Unit,onClose:()->Unit,sleep:Int,onSleep:(Int)->Unit,onFavorite:(Song)->Unit) {
 val song=state.song?:return
 var panel by remember(song.id){mutableStateOf("Player")}
 var lyrics by remember(song.id){mutableStateOf<LyricsResult?>(null)}
 var lyricsLoading by remember(song.id){mutableStateOf(false)}
 LaunchedEffect(song.id,panel){if(panel=="Lyrics"){lyricsLoading=true;lyrics=runCatching{MusicApi.lyrics(song)}.getOrNull();lyricsLoading=false}}
 Column(Modifier.fillMaxSize().background(Color.Black).padding(horizontal=22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
  Row(Modifier.fillMaxWidth().height(58.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onClose){Icon(Icons.Default.KeyboardArrowDown,null,tint=Color.White,modifier=Modifier.size(30.dp))};Spacer(Modifier.weight(1f));Column(horizontalAlignment=Alignment.CenterHorizontally){Text(if(panel=="Player")"NOW PLAYING" else panel.uppercase(),color=Grey,fontSize=10.sp,letterSpacing=1.5.sp);Text("Virex Music",color=Color.White,fontSize=12.sp)};Spacer(Modifier.weight(1f));GlassIcon(Icons.Default.FavoriteBorder,{onFavorite(song)},38)}
  when(panel){
   "Queue"->{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("UP NEXT",color=Grey,fontSize=10.sp,letterSpacing=2.sp,modifier=Modifier.weight(1f));TextButton(onClick={panel="Player"}){Text("Back",color=Grey)}};LazyColumn{itemsIndexed(state.queue){i,item->SongRow(item,{onJump(i);panel="Player"},{})}}}
   "Lyrics"->{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("LYRICS",color=Grey,fontSize=10.sp,letterSpacing=2.sp,modifier=Modifier.weight(1f));TextButton(onClick={panel="Player"}){Text("Back",color=Grey)}};if(lyricsLoading)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=Color.White)}else if(lyrics?.lines.isNullOrEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Lyrics unavailable",color=Grey)}else LazyColumn(contentPadding=PaddingValues(vertical=14.dp)){itemsIndexed(lyrics!!.lines){i,line->val active=line.timeMs!=null&&line.timeMs<=state.position.toLong();Text(line.text,color=if(active)Color.White else Grey,fontSize=if(active)21.sp else 18.sp,fontWeight=if(active)FontWeight.SemiBold else FontWeight.Normal,modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if(active)Color(0xFF191919)else Color.Transparent).then(if(active)Modifier.border(1.dp,Line,RoundedCornerShape(14.dp))else Modifier).padding(13.dp))}}}
   else->{
    Spacer(Modifier.weight(1f))
    Box(contentAlignment=Alignment.Center){AsyncImage(song.artwork.ifBlank{null},null,Modifier.size(244.dp).clip(CircleShape).border(2.dp,Color.White.copy(alpha=.55f),CircleShape));Canvas(Modifier.size(258.dp)){val sweep=if(state.duration>0)state.position.toFloat()/state.duration else 0f;drawArc(Color.White,startAngle=-90f,sweepAngle=sweep*360f,useCenter=false,style=androidx.compose.ui.graphics.drawscope.Stroke(width=2.dp.toPx()));val angle=Math.toRadians((sweep*360f-90f).toDouble());val radius=size.minDimension/2;drawCircle(Color(0xFFFF4040),radius=4.dp.toPx(),center=androidx.compose.ui.geometry.Offset((size.width/2+radius*kotlin.math.cos(angle)).toFloat(),(size.height/2+radius*kotlin.math.sin(angle)).toFloat()))}}
    Text(song.title,color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Light,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=20.dp))
    Text(song.artist,color=Grey,fontSize=14.sp)
    val activeLine=lyrics?.lines?.lastOrNull{it.timeMs!=null&&it.timeMs<=state.position.toLong()}
    Text(activeLine?.text?: " ",color=Color.White.copy(alpha=.85f),fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=16.dp))
    Slider(value=if(state.duration>0)state.position.toFloat()/state.duration else 0f,onValueChange={onSeek((it*state.duration).toInt())},colors=SliderDefaults.colors(thumbColor=Color.White,activeTrackColor=Color.White,inactiveTrackColor=Grey))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(timeLabel(state.position),color=Grey,fontSize=10.sp);Text(timeLabel(state.duration),color=Grey,fontSize=10.sp)}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){GlassIcon(Icons.Default.Shuffle,onShuffle);GlassIcon(Icons.Default.SkipPrevious,onPrevious);GlassIcon(if(state.playing)Icons.Default.Pause else Icons.Default.PlayArrow,onToggle,56);GlassIcon(Icons.Default.SkipNext,onNext);GlassIcon(Icons.Default.Repeat,onRepeat)}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){TextButton(onClick={panel="Queue"}){Text("Queue",color=Grey,fontSize=11.sp)};TextButton(onClick={panel="Lyrics"}){Text("Lyrics",color=Grey,fontSize=11.sp)};TextButton(onClick={onSleep(if(sleep==0)10 else if(sleep==10)20 else if(sleep==20)30 else 0)}){Text(if(sleep==0)"Sleep" else "Sleep · ${sleep}m",color=Grey,fontSize=11.sp)};TextButton(onClick={onHq(!hq)}){Text(if(hq)"HQ · 320" else "160",color=Grey,fontSize=11.sp)}}
    Spacer(Modifier.weight(1f))
   }
  }
 }
}

@Composable private fun SettingsScreen(hq:Boolean,onHq:(Boolean)->Unit,crossfade:Int,onCrossfade:(Int)->Unit,speed:Float,onSpeed:(Float)->Unit,pureBlack:Boolean,onBlack:(Boolean)->Unit,onClear:()->Unit) {
 LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
  item{Text("Settings",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Light)}
  item{SettingSwitch("High quality audio","Prefer the 320kbps stream when available",hq,onHq)}
  item{SettingChoice("Crossfade","Volume fade between tracks",listOf(0,2,4,6).map{"${it}s"},crossfade/2){onCrossfade((it*2))}}
  item{SettingChoice("Animation speed","UI motion timing",listOf("0.5x","1x","1.5x"),listOf(.5f,1f,1.5f).indexOf(speed).coerceAtLeast(0)){onSpeed(listOf(.5f,1f,1.5f)[it])}}
  item{SettingSwitch("Pure black","Use #000000 as the app background",pureBlack,onBlack)}
  item{TextButton(onClick=onClear){Text("Clear recent searches",color=Color.White)}}
  item{Text("ABOUT",color=Grey,fontSize=10.sp,letterSpacing=2.sp,modifier=Modifier.padding(top=22.dp))}
  item{Text("Virex Music · Version 1.0",color=Color.White,fontSize=13.sp,modifier=Modifier.padding(top=8.dp))}
  item{Text("Music metadata and streams: JioSaavn API · Lyrics: LRCLIB",color=Grey,fontSize=11.sp)}
 }
}
@Composable private fun SettingsDialog(hq:Boolean,onHq:(Boolean)->Unit,crossfade:Int,onCrossfade:(Int)->Unit,speed:Float,onSpeed:(Float)->Unit,black:Boolean,onBlack:(Boolean)->Unit,onClose:()->Unit,onClear:()->Unit){AlertDialog(onDismissRequest=onClose,title={Text("Settings")},text={Column{SettingSwitch("HQ audio","320kbps when available",hq,onHq);SettingSwitch("Pure black","Black background",black,onBlack);TextButton(onClick=onClear){Text("Clear recent searches")};Text("Crossfade: ${crossfade}s · Motion: ${speed}x",color=Grey,fontSize=11.sp)}} ,confirmButton={TextButton(onClick=onClose){Text("Done")}})}
@Composable private fun SettingSwitch(title:String,description:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,color=Color.White,fontSize=14.sp);Text(description,color=Grey,fontSize=11.sp)};Switch(checked,onChange)}}
@Composable private fun SettingChoice(title:String,description:String,options:List<String>,selected:Int,onSelect:(Int)->Unit){Column(Modifier.fillMaxWidth().padding(vertical=10.dp)){Text(title,color=Color.White,fontSize=14.sp);Text(description,color=Grey,fontSize=11.sp);Row{options.forEachIndexed{i,s->Text(s,color=if(i==selected)Color.Black else Color.White,fontSize=11.sp,modifier=Modifier.padding(end=6.dp,top=8.dp).clip(CircleShape).background(if(i==selected)Color.White else Glass).clickable{onSelect(i)}.padding(horizontal=10.dp,vertical=7.dp))}}}}
@Composable private fun MiniPlayer(state:PlayerState,onOpen:()->Unit,onToggle:()->Unit,onNext:()->Unit){val song=state.song?:return;Column(Modifier.fillMaxWidth().background(Glass).clickable(onClick=onOpen).padding(horizontal=10.dp,vertical=6.dp)){LinearProgressIndicator(progress={if(state.duration>0)state.position.toFloat()/state.duration else 0f},Modifier.fillMaxWidth().height(2.dp),color=Color.White,trackColor=Line);Row(verticalAlignment=Alignment.CenterVertically){AsyncImage(song.artwork.ifBlank{null},null,Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)){Text(song.title,color=Color.White,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text(song.artist,color=Grey,fontSize=10.sp)};IconButton(onClick=onToggle){Icon(if(state.playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,tint=Color.White)};IconButton(onClick=onNext){Icon(Icons.Default.SkipNext,null,tint=Color.White)}}}}
@Composable private fun SongRow(song:Song,onPlay:(Song)->Unit,onAdd:(Song)->Unit){Row(Modifier.fillMaxWidth().clickable{onPlay(song)}.padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(song.artwork.ifBlank{null},null,Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(song.title,color=Color.White,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text(song.artist,color=Grey,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)};IconButton(onClick={onAdd(song)}){Icon(Icons.Default.Add,null,tint=Grey)}}}
@Composable private fun SectionHeader(text:String){Text(text,color=Grey,fontSize=10.sp,letterSpacing=2.sp,modifier=Modifier.padding(top=24.dp,bottom=10.dp))}
@Composable private fun GlassIcon(icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit,size:Int=40){Box(Modifier.size(size.dp).clip(CircleShape).background(Glass).border(1.dp,Line,CircleShape).clickable(onClick=onClick),contentAlignment=Alignment.Center){Icon(icon,null,tint=Color.White,modifier=Modifier.size((size/2).dp))}}
private fun Song.toPlaylist(id:Long,pos:Int)=PlaylistSongEntity(id,this.id,title,artist,artwork,streamUrl,highUrl,lowUrl,durationSec,pos)
private fun PlaylistSongEntity.toSong()=Song(songId,title,artist,artwork,streamUrl,highUrl,lowUrl,durationSec)
private fun timeLabel(ms:Int):String{val s=ms.coerceAtLeast(0)/1000;return "${s/60}:${(s%60).toString().padStart(2,'0')}"}
private fun queryLocalAudio(context:Context):List<Pair<String,String>>{
 val collection=if(Build.VERSION.SDK_INT>=29)MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
 val out=mutableListOf<Pair<String,String>>()
 context.contentResolver.query(collection,arrayOf(MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE),null,null,MediaStore.Audio.Media.TITLE+" ASC")?.use{c->val id=c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);val title=c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);while(c.moveToNext()){val uri=ContentUris.withAppendedId(collection,c.getLong(id));out+=uri.toString() to (c.getString(title)?: "Audio file")}}
 return out
}
