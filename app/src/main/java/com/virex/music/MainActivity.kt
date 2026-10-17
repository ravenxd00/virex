package com.virex.music

import android.content.ComponentName
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.google.common.util.concurrent.ListenableFuture
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val Black = Color(0xFF000000)
private val Panel = Color(0xFF111111)
private val Lime = Color(0xFF1DB954)
private val Grey = Color(0xFFA1A1A1)
private val Categories = listOf("Trending", "Punjabi", "Hip Hop", "Bollywood", "Global Hits", "Lofi Chill", "EDM")
private val CategoryQueries = mapOf(
 "Trending" to listOf("Arijit Singh","Diljit Dosanjh","Karan Aujla","The Weeknd","Taylor Swift","Anirudh Ravichander","Billie Eilish"),
 "Punjabi" to listOf("Karan Aujla","Diljit Dosanjh","Sidhu Moose Wala","AP Dhillon","Shubh","Amrinder Gill","Jordan Sandhu"),
 "Hip Hop" to listOf("Kendrick Lamar","Drake","Travis Scott","Divine","KRSNA","Eminem","MC Stan","Seedhe Maut"),
 "Bollywood" to listOf("Arijit Singh","Pritam","Atif Aslam","Shreya Ghoshal","KK","Vishal Mishra","Mohit Chauhan"),
 "Global Hits" to listOf("The Weeknd","Taylor Swift","Dua Lipa","Bruno Mars","Ed Sheeran","Ariana Grande","Post Malone"),
 "Lofi Chill" to listOf("Lofi Sleep","Indian Lofi","Chillhop","Aesthetic Beats","Night Drives Lofi"),
 "EDM" to listOf("Martin Garrix","Alan Walker","Avicii","David Guetta","Marshmello","DJ Snake","Chainsmokers")
)

class MainActivity : ComponentActivity() {
 private var future: ListenableFuture<MediaController>? = null
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  future = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
  setContent { VirexNative(this, future) }
 }
 override fun onDestroy() { future?.let { MediaController.releaseFuture(it) }; super.onDestroy() }
}

private data class NativePlaylist(val id: Long, val name: String, val songs: List<Track>)
private fun trackJson(t: Track) = JSONObject().put("id",t.id).put("title",t.title).put("artist",t.artist).put("image",t.image).put("streamUrl",t.streamUrl).put("durationSeconds",t.durationSeconds).put("localPath",t.localPath)
private fun trackFromJson(o: JSONObject) = Track(o.optString("id"),o.optString("title"),o.optString("artist"),o.optString("image"),o.optString("streamUrl"),o.optInt("durationSeconds"),o.optString("localPath").takeIf { it.isNotBlank() && it!="null" })
private fun loadPlaylists(prefs: android.content.SharedPreferences): List<NativePlaylist> = runCatching {
 val a=JSONArray(prefs.getString("playlists","[]"))
 buildList { for(i in 0 until a.length()) { val p=a.getJSONObject(i); val sa=p.optJSONArray("songs")?:JSONArray(); val ss=buildList { for(j in 0 until sa.length()) add(trackFromJson(sa.getJSONObject(j))) }; add(NativePlaylist(p.optLong("id"),p.optString("name"),ss)) } }
}.getOrDefault(emptyList())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VirexNative(context: Context, future: ListenableFuture<MediaController>?) {
 val repo=remember { MusicRepository() }; val prefs=remember { context.getSharedPreferences("virex_native",Context.MODE_PRIVATE) }; val scope=rememberCoroutineScope()
 var player by remember { mutableStateOf<MediaController?>(null) }; var tab by remember { mutableStateOf("Home") }; var category by remember { mutableStateOf("Trending") }
 var query by remember { mutableStateOf("") }; var results by remember { mutableStateOf(emptyList<Track>()) }; var home by remember { mutableStateOf(emptyList<Track>()) }
 var downloads by remember { mutableStateOf(repo.getDownloads(context)) }; var playlists by remember { mutableStateOf(loadPlaylists(prefs)) }
 var history by remember { mutableStateOf(prefs.getString("history","")!!.split("\n").filter { it.isNotBlank() }) }
 var current by remember { mutableStateOf<Track?>(null) }; var queue by remember { mutableStateOf(emptyList<Track>()) }; var playing by remember { mutableStateOf(false) }
 var busy by remember { mutableStateOf(false) }; var downloading by remember { mutableStateOf("") }; var message by remember { mutableStateOf("YOUR MUSIC. YOUR FLOW.") }
 var showPlayer by remember { mutableStateOf(false) }; var showSettings by remember { mutableStateOf(false) }; var showCreate by remember { mutableStateOf(false) }; var showImport by remember { mutableStateOf(false) }
 var showQueue by remember { mutableStateOf(false) }; var showLyrics by remember { mutableStateOf(false) }; var selected by remember { mutableStateOf<NativePlaylist?>(null) }
 var playlistName by remember { mutableStateOf("") }; var importName by remember { mutableStateOf("Imported Playlist") }; var importText by remember { mutableStateOf("") }
 var hq by remember { mutableStateOf(prefs.getBoolean("hq",true)) }; var visualizer by remember { mutableStateOf(prefs.getBoolean("viz",true)) }; var crossfade by remember { mutableStateOf(prefs.getBoolean("crossfade",false)) }
 var shuffle by remember { mutableStateOf(false) }; var repeat by remember { mutableIntStateOf(0) }; var accent by remember { mutableStateOf(Color(prefs.getLong("accent",0xFF1DB954L))) }
 var lyrics by remember { mutableStateOf(emptyList<String>()) }; var lyricsBusy by remember { mutableStateOf(false) }
 var splashVisible by remember { mutableStateOf(true) }
 val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(!granted) message="Playback notification permission was denied. You can enable it in Android Settings." }
 LaunchedEffect(Unit) { delay(1500); splashVisible=false; if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED && !prefs.getBoolean("notification_prompted",false)) { delay(350); prefs.edit().putBoolean("notification_prompted",true).apply(); notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } }

 DisposableEffect(future) {
  val pending = future
  if (pending != null) {
   pending.addListener({
    try {
     player = pending.get()
    } catch (e: Exception) {
     message = "Player unavailable. Please restart the app."
    }
   }, ContextCompat.getMainExecutor(context))
  }
  onDispose { }
 }
 DisposableEffect(player) { val c=player; val l=object: Player.Listener { override fun onIsPlayingChanged(v:Boolean){playing=v}; override fun onMediaItemTransition(item:MediaItem?,reason:Int){current=c?.currentMediaItem?.localConfiguration?.tag as? Track} }; c?.addListener(l); onDispose { c?.removeListener(l) } }
 fun savePlaylists() { val a=JSONArray(); playlists.forEach { p -> val s=JSONArray(); p.songs.forEach { s.put(trackJson(it)) }; a.put(JSONObject().put("id",p.id).put("name",p.name).put("songs",s)) }; prefs.edit().putString("playlists",a.toString()).apply() }
 fun play(t:Track,list:List<Track>) { if(t.streamUrl.isBlank() && (t.localPath==null || !File(t.localPath).exists())) { message="This result has no playable audio URL."; return }; val c=player ?: run { message="Player is starting. Try again."; return }; val q=list.ifEmpty { listOf(t) }; queue=q; val index=q.indexOfFirst { it.id==t.id }.coerceAtLeast(0); c.setMediaItems(q.map { x -> MediaItem.Builder().setMediaId(x.id).setUri(if(x.localPath!=null && File(x.localPath).exists()) "file://"+x.localPath else x.streamUrl).setMediaMetadata(MediaMetadata.Builder().setTitle(x.title).setArtist(x.artist).setArtworkUri(x.image.takeIf { it.startsWith("http") }?.let(android.net.Uri::parse)).build()).setTag(x).build() },index,0L); current=t; c.repeatMode=when(repeat){1->Player.REPEAT_MODE_ALL;2->Player.REPEAT_MODE_ONE;else->Player.REPEAT_MODE_OFF}; c.prepare(); c.play(); showPlayer=true }
 fun search(q:String) { if(q.isBlank()||busy)return; query=q; history=(listOf(q)+history.filterNot{it.equals(q,true)}).take(8); prefs.edit().putString("history",history.joinToString("\n")).apply(); tab="Search"; scope.launch { busy=true; message="Searching…"; try { results=withContext(Dispatchers.IO){repo.search(q,hq)}; message=results.size.toString()+" results" } catch(e:Exception){message=e.message?:"Search failed"} finally{busy=false} } }
 fun loadCategory(cat:String) { category=cat; scope.launch { busy=true; message="Loading "+cat+" mix…"; try { home=withContext(Dispatchers.IO){(CategoryQueries[cat]?:CategoryQueries["Trending"]!!).shuffled().take(3).flatMap{runCatching { repo.search(it,hq).take(8) }.getOrDefault(emptyList())}.distinctBy{it.id}.shuffled().take(18)}; message=cat+" Mix" } catch(e:Exception){message=e.message?:"Could not load songs"} finally{busy=false} } }
 fun download(t:Track) { if(t.streamUrl.isBlank()) { message="This result has no downloadable audio URL."; return }; scope.launch { downloading=t.id; try { repo.download(context,t); downloads=repo.getDownloads(context); message="Saved for offline listening" } catch(e:Exception){message=e.message?:"Download failed"} finally{downloading=""} } }
 LaunchedEffect(Unit){loadCategory("Trending")}
 BackHandler(showPlayer||showSettings||showCreate||showImport||showQueue||showLyrics||selected!=null) { when { showLyrics->showLyrics=false; showQueue->showQueue=false; showPlayer->showPlayer=false; showSettings->showSettings=false; showCreate->showCreate=false; showImport->showImport=false; else->selected=null } }

 MaterialTheme(colorScheme=darkColorScheme(primary=accent,onPrimary=Black,background=Black,surface=Panel,onSurface=Color.White)) {
  Scaffold(containerColor=Black,topBar={ Row(Modifier.fillMaxWidth().background(Black).windowInsetsPadding(WindowInsets.statusBars).padding(start=22.dp,end=12.dp,top=10.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){ androidx.compose.foundation.Image(painter=painterResource(id=R.drawable.ic_virex),contentDescription="Virex",modifier=Modifier.size(32.dp)); Spacer(Modifier.width(9.dp)); Row(verticalAlignment=Alignment.CenterVertically){Text("Virex",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("DL",fontSize=23.sp,fontWeight=FontWeight.Bold,color=accent)}; Spacer(Modifier.weight(1f)); IconButton(onClick={showSettings=true}){Icon(Icons.Default.Settings,"Settings",tint=Grey)} } },bottomBar={
   Column(Modifier.background(Color(0xFF101010))){
    current?.let { t -> Row(Modifier.fillMaxWidth().padding(7.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).clickable{showPlayer=true}.padding(8.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(model=t.image,contentDescription=null,modifier=Modifier.size(42.dp).clip(RoundedCornerShape(7.dp))); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)){Text(t.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold,fontSize=13.sp);Text(t.artist,color=Grey,fontSize=11.sp,maxLines=1)};IconButton(onClick={if(playing)player?.pause() else player?.play()}){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,tint=accent)};IconButton(onClick={showQueue=true}){Icon(Icons.Default.QueueMusic,null,tint=Grey)} } }
    NavigationBar(containerColor=Color(0xFF101010)){listOf("Home","Search","Playlists","Downloads").forEach{item->NavigationBarItem(selected=tab==item,onClick={tab=item;selected=null;if(item=="Downloads")downloads=repo.getDownloads(context);if(item=="Home"&&home.isEmpty())loadCategory(category)},icon={Icon(when(item){"Home"->Icons.Default.Home;"Search"->Icons.Default.Search;"Playlists"->Icons.Default.LibraryMusic;else->Icons.Default.Download},item)},label={Text(item,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=accent,selectedTextColor=accent,indicatorColor=accent.copy(alpha=.15f))) } }
   }
  }) { pad ->
   Column(Modifier.fillMaxSize().padding(pad)){
    when {
     selected!=null -> Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={selected=null}){Icon(Icons.Default.ArrowBack,null)};Column(Modifier.weight(1f)){Text(selected!!.name,fontSize=23.sp,fontWeight=FontWeight.Bold);Text(selected!!.songs.size.toString()+" Songs",color=Grey,fontSize=12.sp)};IconButton(onClick={selected!!.songs.firstOrNull()?.let{play(it,selected!!.songs)}}){Icon(Icons.Default.PlayCircle,null,tint=accent)} }; if(selected!!.songs.isEmpty()) CenterMessage("Playlist is empty",accent) else TrackList(selected!!.songs,downloads,downloading,accent,{play(it,selected!!.songs)},{download(it)})}
     tab=="Home" -> { Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal=12.dp,vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){Categories.forEach{cat->FilterChip(selected=category==cat,onClick={loadCategory(cat)},label={Text(cat,fontSize=11.sp)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=accent,selectedLabelColor=Black))} }; Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=3.dp),verticalAlignment=Alignment.CenterVertically){Text(category+" Mix",fontSize=20.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));TextButton(onClick={loadCategory(category)}){Icon(Icons.Default.Refresh,null,Modifier.size(15.dp));Text(" Shuffle Mix",fontSize=11.sp)}}; if(busy&&home.isEmpty())CenterMessage("Loading your mix…",accent) else if(home.isEmpty())Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.MusicNote,null,tint=accent.copy(alpha=.75f),modifier=Modifier.size(42.dp));Spacer(Modifier.height(12.dp));Text(if(message=="YOUR MUSIC. YOUR FLOW.")"Could not load songs yet." else message,color=Grey,textAlign=androidx.compose.ui.text.style.TextAlign.Center);TextButton(onClick={loadCategory(category)}){Text("TRY AGAIN",color=accent)}} else TrackGrid(home,accent,{play(it,home)},{download(it)},{t->if(playlists.isEmpty())playlists=listOf(NativePlaylist(System.currentTimeMillis(),"My Playlist",listOf(t))) else {val p=playlists.first();playlists=playlists.map{if(it.id==p.id)p.copy(songs=(p.songs+t).distinctBy{z->z.id})else it}};savePlaylists();message="Added to playlist"}) }
     tab=="Search" -> Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){Text("Search",fontSize=29.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp,bottom=12.dp));OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Song, Artist or Album…",color=Grey)},leadingIcon={Icon(Icons.Default.Search,null,tint=accent)},trailingIcon={if(query.isNotEmpty())IconButton(onClick={query="";results=emptyList()}){Icon(Icons.Default.Close,null)}},singleLine=true,shape=RoundedCornerShape(28.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={search(query)}),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=accent,unfocusedBorderColor=Color.DarkGray,focusedTextColor=Color.White,unfocusedTextColor=Color.White,cursorColor=accent));Row(verticalAlignment=Alignment.CenterVertically){Text(message,color=Grey,fontSize=11.sp,modifier=Modifier.weight(1f));TextButton(onClick={search(query)},enabled=!busy){if(busy)CircularProgressIndicator(Modifier.size(17.dp),strokeWidth=2.dp)else Text("SEARCH",color=accent)}};if(query.isBlank()&&history.isNotEmpty()){Row(verticalAlignment=Alignment.CenterVertically){Text("RECENT SEARCHES",color=Grey,fontSize=11.sp);Spacer(Modifier.weight(1f));TextButton(onClick={history=emptyList();prefs.edit().remove("history").apply()}){Text("Clear All",color=accent,fontSize=11.sp)}};history.forEach{h->Row(Modifier.fillMaxWidth().clickable{search(h)}.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.History,null,tint=Grey);Spacer(Modifier.width(10.dp));Text(h,Modifier.weight(1f));IconButton(onClick={history=history.filterNot{it==h};prefs.edit().putString("history",history.joinToString("\n")).apply()}){Icon(Icons.Default.Close,null,tint=Grey)}}}};if(results.isNotEmpty())TrackList(results,downloads,downloading,accent,{play(it,results)},{download(it)})}
     tab=="Playlists" -> Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){Row(Modifier.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Text("Playlists",fontSize=28.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));TextButton(onClick={showImport=true}){Icon(Icons.Default.FileOpen,null);Text("Import")};TextButton(onClick={playlistName="";showCreate=true}){Icon(Icons.Default.Add,null);Text("New")}};if(playlists.isEmpty())CenterMessage("No playlists yet. Create one or import song names.",accent)else LazyVerticalGrid(columns=GridCells.Fixed(2),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(playlists,key={it.id}){p->Column(Modifier.clip(RoundedCornerShape(12.dp)).background(Panel).clickable{selected=p}.padding(10.dp)){if(p.songs.firstOrNull()?.image?.isNotBlank()==true)AsyncImage(model=p.songs.first().image,contentDescription=null,modifier=Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)))else Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF222222)),contentAlignment=Alignment.Center){Icon(Icons.Default.MusicNote,null,tint=Grey,modifier=Modifier.size(42.dp))};Spacer(Modifier.height(8.dp));Text(p.name,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(p.songs.size.toString()+" Songs",color=Grey,fontSize=11.sp)}}}}
     else -> Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){Text("Downloads",fontSize=29.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=16.dp));Text("Saved on this device · works offline",color=Grey,fontSize=12.sp,modifier=Modifier.padding(bottom=12.dp));if(downloads.isEmpty())CenterMessage("No offline songs yet. Use a song’s download option to save it here.",accent)else{Row(verticalAlignment=Alignment.CenterVertically){Text(downloads.size.toString()+" songs saved",color=Grey,fontSize=12.sp);Spacer(Modifier.weight(1f));TextButton(onClick={downloads.forEach{it.localPath?.let{p->File(p).delete()}};prefs.edit().remove("downloads").apply();downloads=emptyList()}){Text("Clear all",color=Color.Red)}};TrackList(downloads,downloads,downloading,accent,{t->if(t.localPath!=null&&File(t.localPath).exists())play(t,downloads)else message="File missing. Download again."},{})}}
    }
   }
  }
 }

 if(showCreate)AlertDialog(onDismissRequest={showCreate=false},containerColor=Panel,title={Text("New playlist")},text={OutlinedTextField(value=playlistName,onValueChange={playlistName=it},label={Text("Playlist name")},singleLine=true)},confirmButton={TextButton(onClick={val n=playlistName.trim().ifBlank{"My Playlist"};playlists=playlists+NativePlaylist(System.currentTimeMillis(),n,emptyList());savePlaylists();showCreate=false}){Text("Create",color=accent)}},dismissButton={TextButton(onClick={showCreate=false}){Text("Cancel")}})
 if(showImport)AlertDialog(onDismissRequest={showImport=false},containerColor=Panel,title={Text("Bulk playlist importer")},text={Column{Text("Paste song names, one per line.",color=Grey,fontSize=12.sp);OutlinedTextField(value=importName,onValueChange={importName=it},label={Text("Playlist name")},singleLine=true);OutlinedTextField(value=importText,onValueChange={importText=it},label={Text("Song names")},minLines=4,maxLines=7)}},confirmButton={TextButton(enabled=!busy,onClick={val lines=importText.lines().map{it.trim()}.filter{it.length>1};if(lines.isNotEmpty())scope.launch{busy=true;val found=mutableListOf<Track>();lines.chunked(5).forEach{chunk->found+=withContext(Dispatchers.IO){chunk.mapNotNull{runCatching{repo.search(it,hq).firstOrNull()}.getOrNull()}}};val unique=found.distinctBy{it.id};if(unique.isNotEmpty()){playlists=playlists+NativePlaylist(System.currentTimeMillis(),importName.ifBlank{"Imported Playlist"},unique);savePlaylists();message="Imported "+unique.size+" songs"}else message="No songs matched";busy=false;showImport=false;importText=""}}){Text(if(busy)"Importing…" else "Import",color=accent)}},dismissButton={TextButton(onClick={showImport=false}){Text("Cancel")}})
 if(showSettings)AlertDialog(onDismissRequest={showSettings=false},containerColor=Panel,title={Text("VirexDL Settings")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){SettingToggle("High quality audio","Prefer highest available audio quality",hq){hq=it;prefs.edit().putBoolean("hq",it).apply()};SettingToggle("Visualizer","Show player visualizer",visualizer){visualizer=it;prefs.edit().putBoolean("viz",it).apply()};SettingToggle("Crossfade","Crossfade between tracks",crossfade){crossfade=it;prefs.edit().putBoolean("crossfade",it).apply()};Text("Accent colour",color=Grey,fontSize=12.sp);Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf(0xFF1DB954L,0xFFB7FF2AL,0xFF36C5F0L,0xFFFF4D8DL,0xFFB388FFL).forEach{v->Box(Modifier.size(28.dp).clip(CircleShape).background(Color(v)).clickable{accent=Color(v);prefs.edit().putLong("accent",v).apply()})}}}},confirmButton={TextButton(onClick={showSettings=false}){Text("Done",color=accent)}})
 if(showQueue)AlertDialog(onDismissRequest={showQueue=false},containerColor=Panel,title={Text("Play queue")},text={LazyColumn(Modifier.heightIn(max=400.dp)){items(queue,key={it.id}){t->Row(Modifier.fillMaxWidth().clickable{play(t,queue);showQueue=false}.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(model=t.image,contentDescription=null,modifier=Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(t.title,maxLines=1,overflow=TextOverflow.Ellipsis);Text(t.artist,color=Grey,fontSize=11.sp)};if(current?.id==t.id)Icon(Icons.Default.VolumeUp,null,tint=accent)}}}},confirmButton={TextButton(onClick={showQueue=false}){Text("Close")}})
 if(showLyrics)AlertDialog(onDismissRequest={showLyrics=false},containerColor=Panel,title={Text("Lyrics")},text={Column(Modifier.heightIn(max=400.dp)){if(lyricsBusy)CircularProgressIndicator(color=accent)else if(lyrics.isEmpty()){Text("Load lyrics for this song.",color=Grey);TextButton(onClick={val t=current;if(t!=null)scope.launch{lyricsBusy=true;lyrics=withContext(Dispatchers.IO){repo.lyrics(t)};lyricsBusy=false}}){Text("Load lyrics",color=accent)}}else LazyColumn{items(lyrics){Text(it,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(vertical=4.dp))}}}},confirmButton={TextButton(onClick={showLyrics=false}){Text("Close",color=accent)}})
 if(splashVisible) Box(Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) { androidx.compose.foundation.Image(painter=painterResource(id=R.drawable.ic_virex),contentDescription="Virex logo",modifier=Modifier.size(218.dp)); Spacer(Modifier.height(12.dp)); Text("Virex",fontSize=58.sp,fontWeight=FontWeight.Medium,color=Color.White,letterSpacing=1.sp); Spacer(Modifier.height(5.dp)); Text("M U S I C   F O R   Y O U",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFFB7FF2A),letterSpacing=2.2.sp) } }
 if(showPlayer)current?.let{t->ModalBottomSheet(onDismissRequest={showPlayer=false},containerColor=Black,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)){Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(horizontal=22.dp),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={showPlayer=false}){Icon(Icons.Default.KeyboardArrowDown,null)};Spacer(Modifier.weight(1f));Text("NOW PLAYING",fontSize=10.sp,letterSpacing=2.sp,color=Grey);Spacer(Modifier.weight(1f));IconButton(onClick={showQueue=true}){Icon(Icons.Default.QueueMusic,null)}};AsyncImage(model=t.image,contentDescription=null,modifier=Modifier.fillMaxWidth(.82f).aspectRatio(1f).clip(RoundedCornerShape(18.dp)));Spacer(Modifier.height(20.dp));Text(t.title,fontSize=23.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(t.artist,color=Grey);Spacer(Modifier.height(22.dp));var seek by remember(t.id){mutableFloatStateOf(0f)};val duration=player?.duration?.takeIf{it>0}?:0L;LaunchedEffect(playing,t.id){while(true){if(duration>0)seek=(player?.currentPosition?.toFloat()?:0f)/duration.toFloat();kotlinx.coroutines.delay(500)}};Slider(value=seek.coerceIn(0f,1f),onValueChange={seek=it},onValueChangeFinished={if(duration>0)player?.seekTo((seek*duration).toLong())},colors=SliderDefaults.colors(thumbColor=accent,activeTrackColor=accent));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(timeLabel(player?.currentPosition?:0L),color=Grey,fontSize=11.sp);Text(timeLabel(duration),color=Grey,fontSize=11.sp)};Spacer(Modifier.height(14.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){IconButton(onClick={shuffle=!shuffle}){Icon(Icons.Default.Shuffle,null,tint=if(shuffle)accent else Grey)};IconButton(onClick={player?.seekToPreviousMediaItem()}){Icon(Icons.Default.SkipPrevious,null,Modifier.size(34.dp))};FilledIconButton(onClick={if(playing)player?.pause()else player?.play()},modifier=Modifier.size(68.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=accent,contentColor=Black)){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,Modifier.size(38.dp))};IconButton(onClick={player?.seekToNextMediaItem()}){Icon(Icons.Default.SkipNext,null,Modifier.size(34.dp))};IconButton(onClick={repeat=(repeat+1)%3;player?.repeatMode=when(repeat){1->Player.REPEAT_MODE_ALL;2->Player.REPEAT_MODE_ONE;else->Player.REPEAT_MODE_OFF}}){Icon(Icons.Default.Repeat,null,tint=if(repeat==0)Grey else accent)}};Spacer(Modifier.height(20.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={download(t)},modifier=Modifier.weight(1f)){Icon(Icons.Default.Download,null);Text(" Download")};OutlinedButton(onClick={showLyrics=true;lyrics=emptyList();scope.launch{lyricsBusy=true;lyrics=withContext(Dispatchers.IO){repo.lyrics(t)};lyricsBusy=false}},modifier=Modifier.weight(1f)){Icon(Icons.Default.Mic,null);Text(" Lyrics")}};if(visualizer)Row(Modifier.padding(top=24.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){repeat(22){i->Box(Modifier.padding(horizontal=2.dp).width(3.dp).height((8+(i*13)%24).dp).clip(CircleShape).background(accent.copy(alpha=if(playing).9f else .35f)))}}} }}
}

@Composable private fun CenterMessage(message:String,accent:Color){Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.MusicNote,null,tint=accent.copy(alpha=.7f),modifier=Modifier.size(42.dp));Spacer(Modifier.height(12.dp));Text(message,color=Grey,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}}
@Composable private fun TrackGrid(tracks:List<Track>,accent:Color,onPlay:(Track)->Unit,onDownload:(Track)->Unit,onAdd:(Track)->Unit){LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=14.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(tracks,key={it.id}){t->Column(Modifier.clip(RoundedCornerShape(12.dp)).background(Panel).clickable{onPlay(t)}.padding(9.dp)){Box{AsyncImage(model=t.image,contentDescription=null,modifier=Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)));IconButton(onClick={onAdd(t)},modifier=Modifier.align(Alignment.TopEnd).size(32.dp).background(Color.Black.copy(alpha=.6f),CircleShape)){Icon(Icons.Default.Add,null,tint=Color.White)}};Spacer(Modifier.height(8.dp));Text(t.title,fontWeight=FontWeight.Bold,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text(t.artist,color=Grey,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){IconButton(onClick={onDownload(t)},modifier=Modifier.size(32.dp)){Icon(Icons.Default.Download,null,tint=accent,modifier=Modifier.size(18.dp))}}}}}}
@Composable private fun TrackList(tracks:List<Track>,downloads:List<Track>,downloading:String,accent:Color,onPlay:(Track)->Unit,onDownload:(Track)->Unit){LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=18.dp)){items(tracks,key={it.id}){t->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(8.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(model=t.image,contentDescription=null,modifier=Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).clickable{onPlay(t)});Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f).clickable{onPlay(t)}){Text(t.title,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(t.artist,color=Grey,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)};if(downloading==t.id)CircularProgressIndicator(Modifier.size(20.dp),color=accent,strokeWidth=2.dp)else IconButton(onClick={if(downloads.any{it.id==t.id})onPlay(downloads.first{it.id==t.id})else onDownload(t)}){Icon(if(downloads.any{it.id==t.id})Icons.Default.CheckCircle else Icons.Default.Download,null,tint=accent)}}}}}
@Composable private fun SettingToggle(title:String,subtitle:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold);Text(subtitle,color=Grey,fontSize=11.sp)};Switch(checked=value,onCheckedChange=onChange)}}
private fun timeLabel(ms:Long)="%d:%02d".format((ms/1000)/60,(ms/1000)%60)
