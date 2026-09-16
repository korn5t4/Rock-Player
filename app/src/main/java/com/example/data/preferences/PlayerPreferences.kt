package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.RepeatMode

class PlayerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rock_player_prefs", Context.MODE_PRIVATE)

    var autoPlayOnStart: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PLAY_ON_START, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PLAY_ON_START, value).apply()

    var selectedThemeId: String
        get() = prefs.getString(KEY_SELECTED_THEME_ID, "classic_sfondo") ?: "classic_sfondo"
        set(value) = prefs.edit().putString(KEY_SELECTED_THEME_ID, value).apply()

    var shuffleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHUFFLE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHUFFLE, value).apply()

    var repeatMode: RepeatMode
        get() {
            val name = prefs.getString(KEY_REPEAT_MODE, RepeatMode.OFF.name)
            return try {
                RepeatMode.valueOf(name ?: RepeatMode.OFF.name)
            } catch (e: Exception) {
                RepeatMode.OFF
            }
        }
        set(value) = prefs.edit().putString(KEY_REPEAT_MODE, value.name).apply()

    var lastSongId: String?
        get() = prefs.getString(KEY_LAST_SONG_ID, null)
        set(value) = prefs.edit().putString(KEY_LAST_SONG_ID, value).apply()

    var lastPositionMs: Long
        get() = prefs.getLong(KEY_LAST_POSITION_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_POSITION_MS, value).apply()

    var savedFolderUri: String?
        get() = prefs.getString(KEY_SAVED_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_SAVED_FOLDER_URI, value).apply()

    var savedFolderName: String?
        get() = prefs.getString(KEY_SAVED_FOLDER_NAME, null)
        set(value) = prefs.edit().putString(KEY_SAVED_FOLDER_NAME, value).apply()

    fun clearSavedFolder() {
        prefs.edit()
            .remove(KEY_SAVED_FOLDER_URI)
            .remove(KEY_SAVED_FOLDER_NAME)
            .apply()
    }

    var equalizerPreset: String
        get() = prefs.getString(KEY_EQ_PRESET, "Rock") ?: "Rock"
        set(value) = prefs.edit().putString(KEY_EQ_PRESET, value).apply()

    fun saveBandLevels(levels: List<Int>) {
        val joined = levels.joinToString(",")
        prefs.edit().putString(KEY_EQ_BANDS, joined).apply()
    }

    fun getBandLevels(): List<Int> {
        val defaultSetup = listOf(0, 7, 10, 9, 0, 0, 5, 9, 5, 0) // Exact setup from user photo
        val raw = prefs.getString(KEY_EQ_BANDS, null) ?: return defaultSetup
        return try {
            val parsed = raw.split(",").map { it.trim().toInt() }
            if (parsed.size == 10) parsed else defaultSetup
        } catch (e: Exception) {
            defaultSetup
        }
    }

    companion object {
        private const val KEY_AUTO_PLAY_ON_START = "auto_play_on_start"
        private const val KEY_SELECTED_THEME_ID = "selected_theme_id"
        private const val KEY_SHUFFLE = "shuffle_enabled"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_LAST_SONG_ID = "last_song_id"
        private const val KEY_LAST_POSITION_MS = "last_position_ms"
        private const val KEY_SAVED_FOLDER_URI = "saved_folder_uri"
        private const val KEY_SAVED_FOLDER_NAME = "saved_folder_name"
        private const val KEY_EQ_PRESET = "eq_preset"
        private const val KEY_EQ_BANDS = "eq_bands"
    }
}
