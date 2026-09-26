package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.RockDatabase
import com.example.data.model.Song
import com.example.data.repository.MusicCacheRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MusicCacheTest {

    private lateinit var db: RockDatabase
    private lateinit var repository: MusicCacheRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MusicCacheRepository(db.trackCacheDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveCachedSongs() = runBlocking {
        val testSongs = listOf(
            Song(
                id = "song_1",
                title = "Highway to Hell",
                artist = "AC/DC",
                album = "Highway to Hell",
                durationMs = 208000L,
                uriString = "content://media/1",
                format = "FLAC",
                isHiRes = true,
                folderName = "Rock Classics",
                bitrateKbps = 1411,
                sampleRateHz = 48000
            ),
            Song(
                id = "song_2",
                title = "Paranoid",
                artist = "Black Sabbath",
                album = "Paranoid",
                durationMs = 170000L,
                uriString = "content://media/2",
                format = "MP3",
                isHiRes = false,
                folderName = "Rock Classics",
                bitrateKbps = 320,
                sampleRateHz = 44100
            )
        )

        repository.saveScannedSongs(testSongs, folderUri = "content://folders/rock_classics", sourceType = "FOLDER")

        val retrieved = repository.getAllCachedSongs()
        assertEquals(2, retrieved.size)

        val count = repository.getTrackCount()
        assertEquals(2, count)

        val folderTracks = repository.getSongsForFolder("content://folders/rock_classics")
        assertEquals(2, folderTracks.size)

        val acdc = folderTracks.find { it.artist == "AC/DC" }
        assertNotNull(acdc)
        assertEquals("Highway to Hell", acdc?.title)
        assertTrue(acdc?.isHiRes == true)
        assertEquals("FLAC", acdc?.format)
    }

    @Test
    fun testCacheMapLookup() = runBlocking {
        val testSong = Song(
            id = "song_usb_1",
            title = "Master of Puppets",
            artist = "Metallica",
            album = "Master of Puppets",
            durationMs = 515000L,
            uriString = "file:///storage/usb/master.mp3",
            format = "MP3",
            folderName = "USB: Metal Drive",
            isUsb = true
        )

        repository.saveScannedSongs(listOf(testSong), folderUri = "USB: Metal Drive", sourceType = "USB")

        val map = repository.getCachedSongsMap()
        assertTrue(map.containsKey("file:///storage/usb/master.mp3"))

        val cached = map["file:///storage/usb/master.mp3"]
        assertNotNull(cached)
        assertEquals("Master of Puppets", cached?.title)
        assertTrue(cached?.isUsb == true)
    }

    @Test
    fun testClearCache() = runBlocking {
        val testSong = Song(
            id = "song_clear_test",
            title = "Smoke on the Water",
            artist = "Deep Purple",
            album = "Machine Head",
            durationMs = 340000L,
            uriString = "content://media/smoke",
            format = "WAV",
            isHiRes = true
        )

        repository.saveScannedSongs(listOf(testSong), folderUri = "content://folders/purple", sourceType = "FOLDER")
        assertEquals(1, repository.getTrackCount())

        repository.clearCache()
        assertEquals(0, repository.getTrackCount())
        assertTrue(repository.getAllCachedSongs().isEmpty())
    }
}
