# VirexDL Native Android

A Kotlin + Jetpack Compose Android music player inspired by the VirexDL website. This is a native app; it does not render the website inside a WebView.

## Stack
- Kotlin 2.0.21, Android Gradle Plugin 8.7.3, Java 17
- Jetpack Compose + Material 3 + Navigation Compose
- Retrofit/Gson adapter for the JioSaavn-compatible API
- Media3 ExoPlayer + MediaSessionService
- Room for downloaded-track and playlist metadata
- DataStore Preferences for player/UI settings
- WorkManager dependency included for background work

## Open and build
1. Open the repository in Android Studio (Ladybug or newer).
2. Use JDK 17 and install Android SDK platform 35.
3. Sync Gradle.
4. Run the `app` configuration on a device or emulator (Android 8.0+).
5. Build an APK with `gradle assembleDebug`.

GitHub Actions builds the debug APK on pushes to `native-v2`; download it from a successful run's Artifacts section.

## API configuration
The API base URLs are defined as Gradle BuildConfig fields in `app/build.gradle.kts`:
- `SAAVAN_BASE_URL`: `https://jiosavan-api2.vercel.app/api/`
- `LRCLIB_BASE_URL`: `https://lrclib.net/api/`

Replace these values at build time if you use another compatible provider. All API calls are behind interfaces in `data/remote`; UI composables do not contain endpoint strings.

## Architecture
- `domain/model`: application models
- `domain/repository`: repository contract
- `data/remote`: API contracts and Retrofit setup
- `data/local`: Room entities and DAOs
- `data/repository`: API mapping, lyrics parsing and offline persistence
- `player`: Media3 service and observable player controller
- `persistence`: DataStore settings
- `feature`: UI state holder
- `core/design`: design tokens
- `MainActivity.kt`: Compose navigation and screens

## Assumptions and current implementation notes
- The public JioSaavn-compatible API can change shape or become unavailable; mapping is defensive and error states are shown rather than pretending a result succeeded.
- Streaming and downloading require a valid HTTPS audio URL from the provider. Downloads are saved under app-private storage; no storage permission is needed.
- The UI uses Virex black/green tokens and the original site layout pattern. The original logo asset should be packaged into drawable resources for fully offline branding.
- Crossfade is persisted as a preference but requires a dedicated overlapping-player implementation before it should be considered active.
- Visualizer is a lightweight animated fallback, not device audio FFT capture.
- Test and device verification remains required before production release. A successful CI build is necessary but not sufficient for production readiness.
