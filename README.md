# Virex Music

A native Android music player built with Kotlin and Jetpack Compose.

## Features
- Search and stream songs from the JioSaavn-compatible API.
- Trending discovery, playback queue, playlists and recent searches.
- LRCLIB synced lyrics when timestamps are supplied, with plain-lyrics fallback.
- Local audio browsing through MediaStore and the Android audio file picker.
- HQ stream selection, shuffle, repeat and sleep timer controls.
- Black-glass monochrome interface.

## Your Files scope
Your Files only plays audio files already on your phone or selected through Android's system file picker. Streamed songs are never downloaded or written to storage.

## Build
Push the project to GitHub, open Actions, select the Android Debug Build workflow, and download the `virex-music-debug-apk` artifact. Install `app-debug.apk` on your Android device.

The project uses Gradlew 8.7, Android Gradle Plugin 8.5.2, Kotlin 1.9.24 and Compose compiler 1.5.14.
