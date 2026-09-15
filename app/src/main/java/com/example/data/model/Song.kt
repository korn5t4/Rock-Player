package com.example.data.model

import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val format: String = "MP3",
    val isHiRes: Boolean = false,
    val albumArtUriString: String? = null,
    val folderName: String? = null,
    val bitrateKbps: Int = 320,
    val sampleRateHz: Int = 44100,
    val isBuiltIn: Boolean = false
) {
    val uri: Uri get() = Uri.parse(uriString)
    val albumArtUri: Uri? get() = albumArtUriString?.let { Uri.parse(it) }

    fun formattedDuration(): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    val formatBadgeText: String
        get() = when {
            isHiRes -> "HI-RES • $format • ${sampleRateHz / 1000}kHz"
            else -> "$format • ${bitrateKbps}kbps"
        }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class VisualizerMode {
    SPECTRUM_BARS,
    WAVEFORM,
    ROCK_PULSE
}
