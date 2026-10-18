package in.virex.music

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VirexNavigationTest {
 @get:Rule val rule = createAndroidComposeRule<MainActivity>()

 @Test fun bottomNavigationShowsFourDestinations() {
  rule.onNodeWithText("Home").assertExists()
  rule.onNodeWithText("Search").assertExists()
  rule.onNodeWithText("Playlists").assertExists()
  rule.onNodeWithText("Downloads").assertExists()
 }
 @Test fun searchDestinationShowsSearchField() {
  rule.onNodeWithText("Search").performClick()
  rule.onNodeWithText("Song, Artist or Album…").assertExists()
 }
 @Test fun playlistCanBeCreated() {
  rule.onNodeWithText("Playlists").performClick()
  rule.onNodeWithText("New").performClick()
  rule.onNodeWithText("New Playlist").assertExists()
  rule.onNodeWithText("Create").performClick()
  rule.onNodeWithText("My Playlist").assertExists()
 }
 @Test fun downloadsDestinationHasEmptyState() {
  rule.onNodeWithText("Downloads").performClick()
  rule.onNodeWithText("Saved on this device · works offline").assertExists()
 }
 @Test fun settingsExposePlayerToggles() {
  rule.onNodeWithContentDescription("Settings").performClick()
  rule.onNodeWithText("Audio Visualizer").assertExists()
  rule.onNodeWithText("High Quality").assertExists()
  rule.onNodeWithText("Crossfade").assertExists()
 }
}
