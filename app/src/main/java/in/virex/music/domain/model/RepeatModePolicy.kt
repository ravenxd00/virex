package in.virex.music.domain.model

object RepeatModePolicy {
 fun next(mode: RepeatMode): RepeatMode = when (mode) {
  RepeatMode.OFF -> RepeatMode.ALL
  RepeatMode.ALL -> RepeatMode.ONE
  RepeatMode.ONE -> RepeatMode.OFF
 }
}
