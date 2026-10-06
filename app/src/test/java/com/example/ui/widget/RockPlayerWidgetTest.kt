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

        val widget4x2Name = context.getString(R.string.widget_4x2_name)
        val widget4x2Desc = context.getString(R.string.widget_4x2_description)
        assertEquals("Rock Player Deck (4x2)", widget4x2Name)
        assertNotNull(widget4x2Desc)
    }

    @Test
    fun testWidget4x2ConstantsAndReceiver() {
        assertEquals("com.example.rockplayer.widget4x2.ACTION_PLAY_PAUSE", RockPlayerWidget4x2Provider.ACTION_PLAY_PAUSE)
        assertEquals("com.example.rockplayer.widget4x2.ACTION_NEXT", RockPlayerWidget4x2Provider.ACTION_NEXT)
        assertEquals("com.example.rockplayer.widget4x2.ACTION_PREV", RockPlayerWidget4x2Provider.ACTION_PREV)
        assertEquals("com.example.rockplayer.widget4x2.ACTION_SHUFFLE", RockPlayerWidget4x2Provider.ACTION_SHUFFLE)
        assertEquals("com.example.rockplayer.widget4x2.ACTION_REPEAT", RockPlayerWidget4x2Provider.ACTION_REPEAT)
        assertEquals("com.example.rockplayer.widget4x2.ACTION_WIDGET_UPDATE", RockPlayerWidget4x2Provider.ACTION_WIDGET_UPDATE)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider4x2 = RockPlayerWidget4x2Provider()

        provider4x2.onReceive(context, Intent(RockPlayerWidget4x2Provider.ACTION_PLAY_PAUSE))
        provider4x2.onReceive(context, Intent(RockPlayerWidget4x2Provider.ACTION_NEXT))
        provider4x2.onReceive(context, Intent(RockPlayerWidget4x2Provider.ACTION_PREV))
        provider4x2.onReceive(context, Intent(RockPlayerWidget4x2Provider.ACTION_SHUFFLE))
        provider4x2.onReceive(context, Intent(RockPlayerWidget4x2Provider.ACTION_REPEAT))

        assertNotNull(provider4x2)
    }
}
