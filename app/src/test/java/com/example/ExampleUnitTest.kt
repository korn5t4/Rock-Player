package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `verify ten band equalizer setup matches photo specifications`() {
    val labels = com.example.data.audio.RockAudioEngine.TEN_BAND_LABELS
    assertEquals(10, labels.size)
    assertEquals(listOf("31.25", "62.5", "125", "250", "500", "1K", "2K", "4K", "8K", "16K"), labels)

    val photoSetup = com.example.data.audio.RockAudioEngine.PHOTO_ROCK_SETUP_DB
    assertEquals(10, photoSetup.size)
    assertEquals(listOf(0, 7, 10, 9, 0, 0, 5, 9, 5, 0), photoSetup)
  }

  @Test
  fun `verify precision octave interpolation for arbitrary hardware band frequencies`() {
    val freqs = com.example.data.audio.RockAudioEngine.TEN_BAND_FREQS
    val dbs = com.example.data.audio.RockAudioEngine.PHOTO_ROCK_SETUP_DB
    val bands = freqs.mapIndexed { idx, freq ->
      com.example.data.audio.EqualizerBandInfo(
        bandIndex = idx.toShort(),
        centerFreqHz = freq,
        levelDb = dbs[idx]
      )
    }

    // Exact frequency center matches
    assertEquals(0f, com.example.data.audio.RockAudioEngine.interpolateDbForFreq(31, bands), 0.01f)
    assertEquals(7f, com.example.data.audio.RockAudioEngine.interpolateDbForFreq(62, bands), 0.01f)
    assertEquals(10f, com.example.data.audio.RockAudioEngine.interpolateDbForFreq(125, bands), 0.01f)

    // Intermediate frequency: halfway in log2 space between 62Hz (7dB) and 125Hz (10dB)
    val midFreq = Math.round(Math.sqrt(62.0 * 125.0)).toInt() // ~88 Hz
    val interpolated = com.example.data.audio.RockAudioEngine.interpolateDbForFreq(midFreq, bands)
    assertTrue("Interpolated gain $interpolated should be between 7dB and 10dB", interpolated in 7.0f..10.0f)
    assertEquals(8.5f, interpolated, 0.2f)

    // Hardware frequencies (e.g. 230Hz, 3600Hz)
    val hw230 = com.example.data.audio.RockAudioEngine.interpolateDbForFreq(230, bands)
    assertTrue("230Hz should smoothly interpolate near 250Hz", hw230 in 8.5f..10.0f)
  }
}
