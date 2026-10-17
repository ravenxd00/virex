# VirexDL Android

This project packages the VirexDL web app as an installable Android APK using Capacitor. It includes a fourth **Downloads** tab that stores audio blobs in IndexedDB and plays them locally while offline.

## Build an APK with GitHub Actions

1. Upload the contents of this folder to a GitHub repository (the workflow is at `.github/workflows/build-apk.yml`).
2. Open the repository's **Actions** tab.
3. Run **Build VirexDL Android APK** with **Run workflow**, or push to `main`/`master`.
4. Open the completed workflow run and download the `VirexDL-debug-apk` artifact.
5. Extract the artifact ZIP and install `app-debug.apk` on Android. Allow installation from that source if Android asks.

## Offline downloads

The user must be online when saving a song for the first time. Downloads are stored in app web storage (IndexedDB), not in the public Downloads folder, and can be played in the Downloads tab without a network connection. Clearing app storage or uninstalling the app removes saved songs. Audio URLs must permit fetch/CORS for in-app saving to work.

## Notes

- The workflow builds a **debug APK** for testing and direct installation. A Play Store release requires a separately configured signed release build.
- Some UI libraries and APIs are loaded from CDNs, so parts of the app interface still need internet on first launch; already-saved audio playback is local.
