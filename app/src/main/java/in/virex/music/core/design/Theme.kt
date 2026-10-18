package in.virex.music.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VirexBlack = Color(0xFF000000)
val VirexCard = Color(0xFF111111)
val VirexControl = Color(0xFF1A1A1A)
val VirexGreen = Color(0xFF1DB954)
val VirexMuted = Color(0xFF8A8A8A)
val VirexBorder = Color(0x0DFFFFFF)

@Composable
fun VirexTheme(accent: Color = VirexGreen, content: @Composable () -> Unit) {
 MaterialTheme(colorScheme = darkColorScheme(primary = accent, secondary = accent, background = VirexBlack, surface = VirexCard, onPrimary = VirexBlack, onBackground = Color.White, onSurface = Color.White, outline = VirexBorder), content = content)
}
