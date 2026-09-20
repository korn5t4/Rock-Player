package com.example.ui.widget

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RockPlayerWidgetTest {

    @Test
    fun testWidgetConstantsAndAction() {
        assertEquals("com.example.rockplayer.ACTION_PLAY_PAUSE", RockPlayerWidgetProvider.ACTION_PLAY_PAUSE)
        assertEquals("com.example.rockplayer.ACTION_NEXT", RockPlayerWidgetProvider.ACTION_NEXT)
        assertEquals("com.example.rockplayer.ACTION_PREV", RockPlayerWidgetProvider.ACTION_PREV)
        assertEquals("com.example.rockplayer.ACTION_WIDGET_UPDATE", RockPlayerWidgetProvider.ACTION_WIDGET_UPDATE)
    }

    @Test
    fun testWidgetReceiverDoesNotCrashOnIntents() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = RockPlayerWidgetProvider()

        val playIntent = Intent(RockPlayerWidgetProvider.ACTION_PLAY_PAUSE)
        provider.onReceive(context, playIntent)

        val nextIntent = Intent(RockPlayerWidgetProvider.ACTION_NEXT)
        provider.onReceive(context, nextIntent)

        val prevIntent = Intent(RockPlayerWidgetProvider.ACTION_PREV)
        provider.onReceive(context, prevIntent)

        assertNotNull(provider)
    }

    @Test
    fun testWidgetStringsExist() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widgetName = context.getString(R.string.widget_name)
        val widgetDesc = context.getString(R.string.widget_description)
        assertEquals("Rock Player Mini", widgetName)
        assertNotNull(widgetDesc)
    }
}
