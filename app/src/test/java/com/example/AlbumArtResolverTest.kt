package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.audio.BuiltInRockAudio
import com.example.data.repository.AlbumArtResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlbumArtResolverTest {

    @Test
    fun testBuiltInTracksHaveJpgArt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val songs = BuiltInRockAudio.getOrGenerateDemoTracks(context)
        assertTrue(songs.isNotEmpty())

        val firstSong = songs.first()
        assertNotNull("Song should have album art URI", firstSong.albumArtUriString)
        assertTrue("Album art URI should point to a jpg file", firstSong.albumArtUriString!!.endsWith(".jpg"))

        val resolvedUri = AlbumArtResolver.resolveAlbumArt(context, firstSong)
        assertNotNull("Resolved URI must not be null", resolvedUri)
    }

    @Test
    fun testFindCompanionJpg() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDir = File(context.cacheDir, "test_album_${System.currentTimeMillis()}").apply { mkdirs() }
        val audioFile = File(testDir, "test_track.mp3").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val jpgFile = File(testDir, "test_track.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }

        val companion = AlbumArtResolver.findCompanionJpg(audioFile)
        assertNotNull("Should find test_track.jpg", companion)
        assertTrue(companion!!.name.endsWith(".jpg"))
    }
}
