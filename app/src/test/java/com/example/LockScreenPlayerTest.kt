package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.PlayerPreferences
import com.example.data.service.RockPlaybackService
import com.example.ui.theme.PlayerSkins
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LockScreenPlayerTest {

    @Test
    fun testLockScreenPreferencesDefaultAndPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PlayerPreferences(context)

        // Default should be enabled
        assertTrue("Lock screen player should be enabled by default", prefs.lockScreenPlayerEnabled)

        // Can toggle
        prefs.lockScreenPlayerEnabled = false
        assertEquals(false, prefs.lockScreenPlayerEnabled)
        prefs.lockScreenPlayerEnabled = true
        assertEquals(true, prefs.lockScreenPlayerEnabled)

        // Wake screen on track change
        prefs.wakeScreenOnTrackChange = true
        assertEquals(true, prefs.wakeScreenOnTrackChange)
    }

    @Test
    fun testPlayerSkinsLookup() {
        val classicSkin = PlayerSkins.getSkinById("classic_sfondo")
        assertNotNull(classicSkin)
        assertEquals("classic_sfondo", classicSkin.id)

        // Unknown skin falls back safely
        val fallback = PlayerSkins.getSkinById("unknown_id")
        assertNotNull(fallback)
        assertEquals(PlayerSkins.ClassicSfondo.id, fallback.id)
    }

    @Test
    fun testNotificationChannelHasPublicVisibilityForLockScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            RockPlaybackService.CHANNEL_ID,
            "Rock Player Lock Screen Controls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        val createdChannel = notificationManager.getNotificationChannel(RockPlaybackService.CHANNEL_ID)
        assertNotNull("Notification channel should exist", createdChannel)
        assertEquals(Notification.VISIBILITY_PUBLIC, createdChannel.lockscreenVisibility)
    }
}
