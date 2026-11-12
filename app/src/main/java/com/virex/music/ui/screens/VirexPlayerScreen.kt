package com.virex.music.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.virex.music.data.*
import com.virex.music.player.PlayerState
import kotlinx.coroutines.launch

private val PlayerGrey = Color(0xFF9A9A9A)
private val PlayerGlass = Color(0xFF111111)
private val PlayerLine = Color.White.copy(alpha=.14f)

@Composable fun VirexPlayerScreen(state:PlayerState,hq:Boolean,onHq:(Boolean)->Unit,onToggle:()->Unit,onNext:()->Unit,onPrevious:()->Unit,onSeek:(Int)->Unit,onShuffle:()->Unit,onRepeat:()->Unit,onJump:(Int)->Unit,onClose:()->Unit,sleep:Int,onSleep:(Int)->Unit,onFavorite:(Song)->Unit) {
 val song=state.song?:return
 var panel by remember(song.id){mutableStateOf("Player")}
 var lyrics by remember(song.id){mutableStateOf<LyricsResult?>(null)}
 var lyricsLoading by remember(song.id){mutableStateOf(false)}
 val lyricListState=androidx.compose.foundation.lazy.rememberLazyListState()
 val activeIndex=lyrics?.lines?.indexOfLast{it.timeMs!=null&&it.timeMs<=state.position.toLong()}?:-1
 LaunchedEffect(song.id){lyricsLoading=true;lyrics=runCatching{MusicApi.lyrics(song)}.getOrNull();lyricsLoading=false}
 LaunchedEffect(activeIndex,panel){if(panel=="Lyrics"&&activeIndex>=0)runCatching{lyricListState.animateScrollToItem(activeIndex)}}
 Box(Modifier.fillMaxSize().background(Color.Black)) {
  AsyncImage(song.artwork.ifBlank{null},null,Modifier.fillMaxWidth().height(480.dp).align(Alignment.TopCenter).blur(55.dp).alpha(.24f),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
  Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha=.22f),Color.Black.copy(alpha=.84f),Color.Black))))
  Column(Modifier.fillMaxSize().padding(horizontal=22.dp)) {
   Row(Modifier.fillMaxWidth().height(58.dp),verticalAlignment=Alignment.CenterVertically) {
    PlayerGlassIcon(Icons.Default.KeyboardArrowDown,onClose,38)
    Spacer(Modifier.weight(1f))
    Column(horizontalAlignment=Alignment.CenterHorizontally){Text(if(panel=="Player")"NOW PLAYING" else panel.uppercase(),color=PlayerGrey,fontSize=9.sp,letterSpacing=2.sp);Text("VIREX MUSIC",color=Color.White,fontSize=11.sp,letterSpacing=1.5.sp)}
    Spacer(Modifier.weight(1f))
    GlassIcon(Icons.Default.MoreHoriz,{if(panel=="Player")panel="Queue" else panel="Player"},38)
   }
   when(panel) {
    "Queue" -> {
     Row(Modifier.fillMaxWidth().padding(top=22.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("YOUR SOUNDTRACK",color=Grey,fontSize=9.sp,letterSpacing=2.sp);Text("Up next",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Light)};TextButton(onClick={panel="Player"}){Text("DONE",color=Color.White,fontSize=10.sp,letterSpacing=1.sp)}}
     LazyColumn { itemsIndexed(state.queue){i,item->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if(i==state.index)Color.White.copy(alpha=.09f)else Color.Transparent).clickable{onJump(i);panel="Player"}.padding(9.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(item.artwork.ifBlank{null},null,Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)),contentScale=androidx.compose.ui.layout.ContentScale.Crop);Column(Modifier.weight(1f).padding(horizontal=11.dp)){Text(item.title,color=if(i==state.index)Color.White else Color.White.copy(alpha=.85f),fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text(item.artist,color=Grey,fontSize=10.sp,maxLines=1)};if(i==state.index)Icon(Icons.Default.GraphicEq,null,tint=Color.White,modifier=Modifier.size(18.dp))}} }
    }
    "Lyrics" -> {
     Row(Modifier.fillMaxWidth().padding(top=20.dp,bottom=14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("SING ALONG",color=Grey,fontSize=9.sp,letterSpacing=2.sp);Text("Lyrics",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Light)};TextButton(onClick={panel="Player"}){Text("CLOSE",color=Color.White,fontSize=10.sp)}}
     Box(Modifier.fillMaxSize().padding(bottom=14.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha=.055f)).border(1.dp,Color.White.copy(alpha=.10f),RoundedCornerShape(24.dp))) {
      Column(Modifier.fillMaxSize().padding(horizontal=18.dp,vertical=15.dp)) {
       Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Mic,null,tint=Color.White,modifier=Modifier.size(17.dp));Text("  ${song.title}",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)}
       HorizontalDivider(color=Color.White.copy(alpha=.10f),modifier=Modifier.padding(top=13.dp,bottom=10.dp))
       if(lyricsLoading)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=Color.White,strokeWidth=2.dp)}
       else if(lyrics?.lines.isNullOrEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Lyrics aren't available for this track.",color=Grey,fontSize=13.sp)}
       else LazyColumn(state=lyricListState,contentPadding=PaddingValues(vertical=20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
        itemsIndexed(lyrics!!.lines){i,line->
         val active=i==activeIndex
         Text(line.text,color=if(active)Color.White else Color.White.copy(alpha=.32f),fontSize=if(active)24.sp else 20.sp,lineHeight=if(active)31.sp else 27.sp,fontWeight=if(active)FontWeight.SemiBold else FontWeight.Medium,modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(if(active)Color.White.copy(alpha=.07f)else Color.Transparent).then(if(active)Modifier.border(1.dp,Color.White.copy(alpha=.12f),RoundedCornerShape(15.dp))else Modifier).padding(horizontal=14.dp,vertical=13.dp))
        }
       }
      }
     }
    }
    else -> {
     Spacer(Modifier.weight(.45f))
     Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
      AsyncImage(song.artwork.ifBlank{null},null,Modifier.size(286.dp).clip(RoundedCornerShape(20.dp)).border(1.dp,Color.White.copy(alpha=.15f),RoundedCornerShape(20.dp)),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
      Canvas(Modifier.size(302.dp)) {
       val progress=if(state.duration>0)state.position.toFloat()/state.duration else 0f
       drawArc(Color.White.copy(alpha=.85f),-90f,progress*360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
       val a=Math.toRadians((progress*360f-90f).toDouble());val r=size.minDimension/2
       drawCircle(Color(0xFFEAEAEA),3.dp.toPx(),androidx.compose.ui.geometry.Offset((size.width/2+r*kotlin.math.cos(a)).toFloat(),(size.height/2+r*kotlin.math.sin(a)).toFloat()))
      }
     }
     Row(Modifier.fillMaxWidth().padding(top=24.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(song.title,color=Color.White,fontSize=27.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(song.artist,color=Grey,fontSize=14.sp,modifier=Modifier.padding(top=3.dp))};GlassIcon(Icons.Default.FavoriteBorder,{onFavorite(song)},42)}
     val currentLine=lyrics?.lines?.getOrNull(activeIndex)?.text
     Text(currentLine?: " ",color=Color.White.copy(alpha=.75f),fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth().padding(top=16.dp,bottom=4.dp).clickable{panel="Lyrics"})
     Slider(value=if(state.duration>0)state.position.toFloat()/state.duration else 0f,onValueChange={onSeek((it*state.duration).toInt())},modifier=Modifier.height(32.dp),colors=SliderDefaults.colors(thumbColor=Color.White,activeTrackColor=Color.White,inactiveTrackColor=Color.White.copy(alpha=.20f)))
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(playerTimeLabel(state.position),color=Grey,fontSize=10.sp);Text(timeLabel(state.duration),color=Grey,fontSize=10.sp)}
     Row(Modifier.fillMaxWidth().padding(top=16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){GlassIcon(Icons.Default.Shuffle,onShuffle,40);GlassIcon(Icons.Default.SkipPrevious,onPrevious,44);GlassIcon(if(state.playing)Icons.Default.Pause else Icons.Default.PlayArrow,onToggle,58);GlassIcon(Icons.Default.SkipNext,onNext,44);GlassIcon(Icons.Default.Repeat,onRepeat,40)}
     Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=20.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
      TextButton(onClick={panel="Queue"}){Icon(Icons.Default.QueueMusic,null,tint=PlayerGrey,modifier=Modifier.size(15.dp));Spacer(Modifier.width(5.dp));Text("QUEUE",color=Grey,fontSize=9.sp,letterSpacing=1.sp)}
      TextButton(onClick={panel="Lyrics"}){Icon(Icons.Default.Mic,null,tint=Grey,modifier=Modifier.size(15.dp));Spacer(Modifier.width(5.dp));Text("LYRICS",color=Grey,fontSize=9.sp,letterSpacing=1.sp)}
      TextButton(onClick={onSleep(if(sleep==0)10 else if(sleep==10)20 else if(sleep==20)30 else 0)}){Icon(Icons.Default.Bedtime,null,tint=Grey,modifier=Modifier.size(15.dp));Spacer(Modifier.width(4.dp));Text(if(sleep==0)"SLEEP" else "${sleep}M",color=Grey,fontSize=9.sp)}
      TextButton(onClick={onHq(!hq)}){Text(if(hq)"HQ 320" else "160",color=Grey,fontSize=9.sp,letterSpacing=1.sp)}
     }
    }
   }
  }
 }
}

@Composable private fun PlayerSectionHeader(text:String){Text(text,color=PlayerGrey,fontSize=10.sp,letterSpacing=2.sp,modifier=Modifier.padding(top=24.dp,bottom=10.dp))}
@Composable private fun PlayerGlassIcon(icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit,size:Int=40){Box(Modifier.size(size.dp).clip(CircleShape).background(PlayerGlass).border(1.dp,PlayerLine,CircleShape).clickable(onClick=onClick),contentAlignment=Alignment.Center){Icon(icon,null,tint=Color.White,modifier=Modifier.size((size/2).dp))}}
@Composable private fun PlayerSongRow(song:Song,onPlay:(Song)->Unit,onAdd:(Song)->Unit){Row(Modifier.fillMaxWidth().clickable{onPlay(song)}.padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(song.artwork.ifBlank{null},null,Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(song.title,color=Color.White,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text(song.artist,color=PlayerGrey,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)};IconButton(onClick={onAdd(song)}){Icon(Icons.Default.Add,null,tint=PlayerGrey)}}}
private fun playerTimeLabel(ms:Int):String{val s=ms.coerceAtLeast(0)/1000;return "${s/60}:${(s%60).toString().padStart(2,'0')}"}
