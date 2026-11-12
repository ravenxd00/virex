package com.virex.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackGlass = darkColorScheme(
    primary = Color.White, onPrimary = Color.Black,
    background = Color.Black, onBackground = Color.White,
    surface = Color(0xFF101010), onSurface = Color.White,
    secondary = Color(0xFF9A9A9A), onSecondary = Color.Black
)

@Composable
fun VirexMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BlackGlass, content = content)
}
