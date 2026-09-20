# Rock Player 🎸

A high-performance rock audio player for Android built with Kotlin, Jetpack Compose, Material Design 3, and a custom rock audio engine.

## Features

- **10-Band Graphic Equalizer**: Inspired by professional analog graphic equalizers (31.25Hz to 16kHz) with dedicated rock presets, Bass Boost, and Treble Boost.
- **Dynamic Real-Time Visualizer**: Audio waveform and frequency bar visualizer responding to active playback.
- **Custom Hardware Skins**:
  - *Vintage Silver*: Classic brushed aluminum and chrome dials.
  - *Midnight Carbon*: Sleek carbon fiber with electric cyan indicators.
  - *Gold Rush*: Polished brass and warm golden vu-meter accents.
  - *Crimson Riot*: Aggressive matte-black with hot-rod red accents.
- **Home Screen Mini Player Widget**: Interactive home screen widget (`Rock Player Mini`) with play/pause, skip controls, track metadata, and album artwork.
- **Folder Scanner & Library**: SAF (Storage Access Framework) folder picker with persistent Uri permissions, background indexing, and instant caching.
- **Hi-Res Audio Support**: FLAC, MP3, WAV, AAC, and OGG playback support.

---

## Opening in Android Studio

This project is fully configured for **Android Studio** (Koala, Ladybug, or newer recommended).

### Prerequisites
- **Android Studio**: Android Studio Ladybug (2024.2.1+) or newer.
- **JDK**: Java 17 or Java 21 (bundled with Android Studio by default).
- **Android SDK**: Compile SDK 36 (Android 15+ / 16 preview), Min SDK 24 (Android 7.0+).

### Quick Start
1. **Open Project**: Launch Android Studio, select **Open**, and choose this project's root folder.
2. **Gradle Sync**: Android Studio will automatically recognize the Gradle wrapper (`gradlew` / `gradle-wrapper.jar`) and run a Gradle sync.
3. **Gradle JDK Setting**: Ensure your Gradle JDK is set to **Java 17** or **Java 21**:
   - `Settings` (or `Preferences` on macOS) ➔ `Build, Execution, Deployment` ➔ `Build Tools` ➔ `Gradle` ➔ **Gradle JDK**.
4. **Run App**: Select the `app` configuration in the toolbar and press **Run** (`Shift + F10`) on an emulator or physical device.

---

## Building from Command Line

Using the included Gradle wrapper:

- **Build Debug APK**:
  ```bash
  # Linux / macOS
  ./gradlew assembleDebug

  # Windows
  gradlew.bat assembleDebug
  ```
  The generated APK will be in `app/build/outputs/apk/debug/app-debug.apk`.

- **Run Unit & Robolectric Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```

---

## Architecture & Tech Stack

- **UI Framework**: Jetpack Compose with Material Design 3 (M3).
- **Architecture**: MVVM with Kotlin Coroutines and StateFlow.
- **Local Persistence**: Room Database for playlist and track library caching.
- **Audio Engine**: Android `MediaPlayer` integration with hardware-accelerated `Equalizer` and fallback high-resolution audio processing.
- **Image Loading**: Coil Compose with hardware bitmap caching.
- **Widgets**: Android AppWidgetProvider with `RemoteViews` and reactive broadcast events.

