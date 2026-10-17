# VirexDL Android project

## Build
Push to `main` or run the GitHub Actions workflow manually. Download the `VirexDL-debug-apk` artifact after a successful build.

## Cross-device work
- Uses viewport and safe-area rules intended for compact phones, tall phones, tablets, and desktop WebViews.
- Keeps zoom available for accessibility and has a `100vh` fallback for WebViews without `100dvh` support.
- Includes Capacitor Haptics, Status Bar and Splash Screen dependencies/configuration.
- Backup & Restore UI was removed from Settings.

## Device testing still required
Test on multiple screen sizes and Android versions. WebView Media Session behavior and OnePlus live-status UI vary by device; guaranteed background playback/notification controls can require native media-session and foreground-service implementation.
