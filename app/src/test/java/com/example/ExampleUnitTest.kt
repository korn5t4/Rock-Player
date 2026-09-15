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
}
