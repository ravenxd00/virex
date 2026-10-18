package com.virex.music.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VirexBlack = Color(0xFF000000)
val VirexCard = Color(0xFF111111)
val VirexControl = Color(0xFF1A1A1A)
val VirexGreen = Color(0xFF1DB954)
val VirexMuted = Color(0xFF8A8A8A)
val VirexBorder = Color(0x0DFFFFFF)

@Composable
fun VirexTheme(accent: Color = VirexGreen, content: @Composable () -> Unit) {
 MaterialTheme(colorScheme = darkColorScheme(primary = accent, secondary = accent, background = VirexBlack, surface = VirexCard, onPrimary = VirexBlack, onBackground = Color.White, onSurface = Color.White, outline = VirexBorder), shapes = Shapes(extraSmall = RoundedCornerShape(50.dp), small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(26.dp), extraLarge = RoundedCornerShape(30.dp)), content = content)
}
