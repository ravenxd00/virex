package com.virex.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.virex.music.data.Song

private val HomeWhite = Color(0xFFF7F7F7)
private val HomeMuted = Color(0xFF96969B)
private val HomeLine = Color.White.copy(alpha = .12f)
private val HomeSurface = Color(0xFF111113)

@Composable
fun VirexHomeScreen(
    songs: List<Song>,
    busy: Boolean,
    message: String?,
    onPlay: (Song) -> Unit,
    onCategory: (String) -> Unit,
    onSettings: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("For you") }
    val categories = listOf("For you", "Trending", "Punjabi", "Hip-hop", "Lofi", "Bollywood", "Workout")

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("VIREX", color = HomeWhite, fontSize = 25.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp)
                    Text("MUSIC, IN MOTION", color = HomeMuted, fontSize = 9.sp, letterSpacing = 2.6.sp, modifier = Modifier.padding(top = 3.dp))
                }
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(HomeSurface).border(1.dp, HomeLine, CircleShape)
                ) { Icon(Icons.Default.Settings, null, tint = HomeWhite, modifier = Modifier.size(19.dp)) }
            }
        }
        item {
            Text("YOUR MUSIC.\nYOUR MOMENT.", color = HomeWhite, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Light, letterSpacing = (-.8).sp)
            Text("Find the sound that stays with you.", color = HomeMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val selected = selectedCategory == category
                    Box(
                        modifier = Modifier.clip(CircleShape)
                            .background(if (selected) HomeWhite else HomeSurface)
                            .border(1.dp, if (selected) HomeWhite else HomeLine, CircleShape)
                            .clickable { selectedCategory = category; onCategory(category) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(category, color = if (selected) Color.Black else HomeWhite, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
        item { HomeSectionTitle("THE VIREX EDIT", "Handpicked for your next repeat") }
        if (songs.isNotEmpty()) {
            item {
                val hero = songs.first()
                Box(
                    Modifier.fillMaxWidth().height(286.dp).clip(RoundedCornerShape(22.dp))
                        .background(HomeSurface).clickable { onPlay(hero) }
                ) {
                    AsyncImage(hero.artwork.ifBlank { null }, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .02f), Color.Black.copy(alpha = .12f), Color.Black.copy(alpha = .88f)))))
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(19.dp)) {
                        Text("VIREX SELECTS  /  01", color = HomeWhite.copy(alpha = .8f), fontSize = 9.sp, letterSpacing = 2.sp)
                        Text(hero.title, color = HomeWhite, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                        Text(hero.artist, color = HomeWhite.copy(alpha = .82f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 13.dp).clip(CircleShape).background(HomeWhite).clickable { onPlay(hero) }.padding(horizontal = 15.dp, vertical = 9.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("LISTEN NOW", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                        }
                    }
                }
            }
        } else {
            item {
                Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)).background(HomeSurface), contentAlignment = Alignment.Center) {
                    Text(if (busy) "Finding your sound…" else message ?: "Your mixes will appear here.", color = HomeMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 13.dp)) {
                HomeSectionTitle("TRENDING NOW", "Popular tracks worth a replay", Modifier.weight(1f))
                Icon(Icons.Default.ArrowForward, null, tint = HomeMuted, modifier = Modifier.size(17.dp))
            }
        }
        items(songs.drop(1).take(8), key = { it.id }) { song ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable { onPlay(song) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(song.artwork.ifBlank { null }, null, Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(song.title, color = HomeWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = HomeMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
                Icon(Icons.Default.PlayArrow, null, tint = HomeMuted, modifier = Modifier.size(20.dp))
            }
        }
        if (songs.size > 9) {
            item {
                Text("QUICK PICKS", color = HomeWhite, fontSize = 10.sp, letterSpacing = 2.2.sp, modifier = Modifier.padding(top = 25.dp, bottom = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    songs.drop(9).take(2).forEach { song ->
                        Column(Modifier.weight(1f).clickable { onPlay(song) }) {
                            AsyncImage(song.artwork.ifBlank { null }, null, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(15.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            Text(song.title, color = HomeWhite, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                            Text(song.artist, color = HomeMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(25.dp))
            Text("MADE FOR YOUR LIBRARY", color = HomeWhite, fontSize = 10.sp, letterSpacing = 2.2.sp)
            Text("Your saved tracks and playlists will live here.", color = HomeMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
private fun HomeSectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 12.dp)) {
        Text(title, color = HomeWhite, fontSize = 10.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, color = HomeMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
