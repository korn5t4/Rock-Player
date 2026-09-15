package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.model.VisualizerMode
import com.example.ui.components.SkinPlayerView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PlayerSkins
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleSong = Song(
      id = "test_song_1",
      title = "Electric Thunder",
      artist = "Iron Riff",
      album = "Hard Rock Anthems",
      durationMs = 215000L,
      uriString = "content://test",
      format = "FLAC",
      isHiRes = true
    )

    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        SkinPlayerView(
          currentSong = sampleSong,
          isPlaying = true,
          currentPositionMs = 65000L,
          durationMs = 215000L,
          repeatMode = RepeatMode.ALL,
          isShuffle = true,
          skin = PlayerSkins.ClassicSfondo,
          visualizerBars = FloatArray(24) { (it % 5 + 1) * 0.18f },
          waveform = FloatArray(64) { 0f },
          visualizerMode = VisualizerMode.SPECTRUM_BARS,
          onPlayPause = {},
          onNext = {},
          onPrevious = {},
          onSeek = {},
          onToggleShuffle = {},
          onCycleRepeat = {},
          onOpenEqualizer = {},
          onOpenLibrary = {},
          onOpenSettings = {},
          onVisualizerModeChange = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
