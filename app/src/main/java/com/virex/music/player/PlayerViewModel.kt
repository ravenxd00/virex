package com.virex.music.player

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virex.music.data.MusicApi
import com.virex.music.data.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerState(
 val song:Song?=null,val queue:List<Song> = emptyList(),val index:Int=0,
 val playing:Boolean=false,val position:Int=0,val duration:Int=0,
 val shuffle:Boolean=false,val repeat:Int=0,val sleepMinutes:Int=0,val error:String?=null
)

class PlayerViewModel(app:Application):AndroidViewModel(app) {
 private var media:MediaPlayer?=null
 private val mutable=MutableStateFlow(PlayerState())
 val state:StateFlow<PlayerState> = mutable
 private var highQuality=true
 private var sleepJob:Job?=null

 init {
  viewModelScope.launch {
   while(true) {
    delay(350)
    val p=media
    if(p!=null && runCatching{p.isPlaying}.getOrDefault(false)) {
     mutable.value=mutable.value.copy(
      position=runCatching{p.currentPosition}.getOrDefault(0),
      duration=runCatching{p.duration}.getOrDefault(0),playing=true
     )
    }
   }
  }
 }

 fun setHighQuality(high:Boolean){highQuality=high}

 fun play(song:Song,list:List<Song> = listOf(song)) {
  val q=list.ifEmpty{listOf(song)}
  val i=q.indexOfFirst{it.id==song.id}.coerceAtLeast(0)
  mutable.value=mutable.value.copy(queue=q,index=i,error=null)
  playAt(i)
  if(!song.isLocal) viewModelScope.launch {
   runCatching{MusicApi.suggestions(song.id)}.getOrNull()?.let { extra ->
    val current=mutable.value
    mutable.value=current.copy(queue=current.queue+extra.filterNot{s->current.queue.any{it.id==s.id}})
   }
  }
 }

 private fun playAt(i:Int) {
  val s=mutable.value.queue.getOrNull(i)?:return
  val url=if(s.isLocal)s.streamUrl else s.url(highQuality)
  if(url.isBlank()){mutable.value=mutable.value.copy(error="No playable stream URL was returned.");return}
  runCatching {
   media?.release()
   media=MediaPlayer().apply {
    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
    setDataSource(url)
    setOnPreparedListener {
     it.start()
     mutable.value=mutable.value.copy(song=s,index=i,playing=true,position=0,duration=it.duration,error=null)
    }
    setOnCompletionListener { if(mutable.value.repeat==2)seek(0) else next() }
    setOnErrorListener { _,_,_ -> mutable.value=mutable.value.copy(playing=false,error="Unable to play this audio stream.");true }
    prepareAsync()
   }
   mutable.value=mutable.value.copy(song=s,index=i,playing=false,position=0,duration=0,error=null)
  }.onFailure { mutable.value=mutable.value.copy(error=it.message?: "Playback failed",playing=false) }
 }

 fun toggle() {
  runCatching { if(media?.isPlaying==true)media?.pause() else media?.start() }
  mutable.value=mutable.value.copy(playing=media?.isPlaying==true)
 }
 fun seek(ms:Int){runCatching{media?.seekTo(ms.coerceAtLeast(0))};mutable.value=mutable.value.copy(position=ms.coerceAtLeast(0))}
 fun next() {
  val s=mutable.value
  if(s.queue.isEmpty())return
  val i=if(s.shuffle&&s.queue.size>1)s.queue.indices.filter{it!=s.index}.random() else s.index+1
  if(i<s.queue.size)playAt(i) else if(s.repeat==1)playAt(0) else mutable.value=s.copy(playing=false)
 }
 fun previous(){if(mutable.value.position>3000)seek(0) else playAt((mutable.value.index-1).coerceAtLeast(0))}
 fun jump(i:Int)=playAt(i)
 fun shuffle(){mutable.value=mutable.value.copy(shuffle=!mutable.value.shuffle)}
 fun repeat(){mutable.value=mutable.value.copy(repeat=(mutable.value.repeat+1)%3)}
 fun sleep(minutes:Int) {
  sleepJob?.cancel()
  mutable.value=mutable.value.copy(sleepMinutes=minutes)
  if(minutes>0)sleepJob=viewModelScope.launch {
   delay(minutes*60_000L)
   runCatching{media?.pause()}
   mutable.value=mutable.value.copy(playing=false,sleepMinutes=0)
  }
 }
 fun playLocal(uri:String,title:String) {
  val local=Song(uri,title,"On this device",streamUrl=uri,isLocal=true)
  mutable.value=PlayerState(song=local,queue=listOf(local))
  playAt(0)
 }
 override fun onCleared(){sleepJob?.cancel();media?.release();media=null;super.onCleared()}
}
