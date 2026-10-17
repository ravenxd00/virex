# Virex Native Android

Virex is being rebuilt as a **native Android application** using Kotlin, Jetpack Compose, and AndroidX Media3. This is not a WebView wrapper or a PWA.

## Build an installable debug APK

1. Open **Actions** in this repository.
2. Select **Build Virex Native Android APK** and run the workflow on branch `native-android` (or wait for the branch push build).
3. Open the completed run and download the `Virex-Native-debug-apk` artifact.
4. Extract the artifact ZIP and install `app-debug.apk` on your Android device. Android may ask you to allow installation from that source.

## Included in the native app

- Native Compose interface with Virex black-and-lime branding.
- Search against the JioSaavn-compatible API used by the existing site.
- AndroidX Media3 ExoPlayer and MediaSessionService for background playback and media notification integration.
- Download songs to app-private storage and play downloaded files from the offline library.
- Album artwork, playback controls, and basic search/download status.

## Important testing notes

This is a debug build until it has been compiled and tested on physical devices. The external music API and audio URLs can change or be unavailable. Background playback, notifications, downloads, and offline playback must be tested on the target Android version before calling the app release-ready. The original website remains on the default branch; native changes are isolated on `native-android`.


Native build workflow was configured on the default branch; new commits on this branch trigger an APK build.
