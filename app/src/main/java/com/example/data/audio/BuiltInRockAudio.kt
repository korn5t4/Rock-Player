package com.example.data.audio

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object BuiltInRockAudio {

    suspend fun getOrGenerateDemoTracks(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val tracksDir = File(context.filesDir, "rock_tracks")
        if (!tracksDir.exists()) {
            tracksDir.mkdirs()
        }

        val trackConfigs = listOf(
            RockTrackConfig(
                fileName = "electric_thunder.wav",
                title = "Electric Thunder",
                artist = "Iron Riff",
                album = "Hard Rock Anthems",
                baseBpm = 132,
                rootFreq = 110.0, // A2 power chord
                isHiRes = true,
                format = "FLAC/WAV",
                sampleRate = 48000,
                durationSeconds = 18,
                drawableResId = R.drawable.cover_electric_thunder
            ),
            RockTrackConfig(
                fileName = "highway_overdrive.wav",
                title = "Highway Overdrive",
                artist = "The Neon Skulls",
                album = "Speed & Gasoline",
                baseBpm = 144,
                rootFreq = 82.4, // E2 heavy metal riff
                isHiRes = true,
                format = "FLAC/WAV",
                sampleRate = 48000,
                durationSeconds = 16,
                drawableResId = R.drawable.cover_highway_overdrive
            ),
            RockTrackConfig(
                fileName = "midnight_stomp.wav",
                title = "Midnight Stomp",
                artist = "Black Velvet",
                album = "Dark Voltage",
                baseBpm = 118,
                rootFreq = 98.0, // G2 rock groove
                isHiRes = false,
                format = "MP3",
                sampleRate = 44100,
                durationSeconds = 20,
                drawableResId = R.drawable.cover_midnight_stomp
            ),
            RockTrackConfig(
                fileName = "rebel_screamer.wav",
                title = "Rebel Screamer",
                artist = "Thunderstruck",
                album = "Live at Arena",
                baseBpm = 150,
                rootFreq = 146.8, // D3 classic rock
                isHiRes = true,
                format = "FLAC/WAV",
                sampleRate = 48000,
                durationSeconds = 17,
                drawableResId = R.drawable.cover_rebel_screamer
            )
        )

        val songs = mutableListOf<Song>()

        for ((index, config) in trackConfigs.withIndex()) {
            val file = File(tracksDir, config.fileName)
            if (!file.exists() || file.length() < 1000) {
                generateRockWav(file, config)
            }

            // Copy companion *.jpg album cover alongside audio file
            val jpgFileName = config.fileName.substringBeforeLast('.') + ".jpg"
            val jpgFile = File(tracksDir, jpgFileName)
            if (!jpgFile.exists() || jpgFile.length() < 1000) {
                try {
                    context.resources.openRawResource(config.drawableResId).use { input ->
                        FileOutputStream(jpgFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val artUriString = if (jpgFile.exists() && jpgFile.length() > 0) {
                Uri.fromFile(jpgFile).toString()
            } else null

            songs.add(
                Song(
                    id = "builtin_rock_$index",
                    title = config.title,
                    artist = config.artist,
                    album = config.album,
                    durationMs = (config.durationSeconds * 1000).toLong(),
                    uriString = file.toURI().toString(),
                    format = if (config.isHiRes) "FLAC (Hi-Res)" else "MP3",
                    isHiRes = config.isHiRes,
                    albumArtUriString = artUriString,
                    folderName = "Rock Player Vault",
                    bitrateKbps = if (config.isHiRes) 1536 else 320,
                    sampleRateHz = config.sampleRate,
                    isBuiltIn = true
                )
            )
        }

        songs
    }

    private data class RockTrackConfig(
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val baseBpm: Int,
        val rootFreq: Double,
        val isHiRes: Boolean,
        val format: String,
        val sampleRate: Int,
        val durationSeconds: Int,
        val drawableResId: Int
    )

    private fun generateRockWav(file: File, config: RockTrackConfig) {
        val sampleRate = config.sampleRate
        val channels = 2 // Stereo
        val totalSamples = sampleRate * config.durationSeconds
        val bytesPerSample = 2 // 16-bit
        val dataSize = totalSamples * channels * bytesPerSample

        val fos = FileOutputStream(file)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF Header
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // SubChunk1Size for PCM
        header.putShort(1) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * channels * bytesPerSample) // ByteRate
        header.putShort((channels * bytesPerSample).toShort()) // BlockAlign
        header.putShort(16) // BitsPerSample
        header.put("data".toByteArray())
        header.putInt(dataSize)

        fos.write(header.array())

        val bufferSize = 4096
        val pcmBuffer = ByteBuffer.allocate(bufferSize * channels * bytesPerSample).order(ByteOrder.LITTLE_ENDIAN)

        val beatDuration = 60.0 / config.baseBpm
        val beatsPerBar = 4

        // Riff pattern intervals: root, fifth, octave, minor third / fourth
        val intervals = listOf(1.0, 1.5, 1.334, 1.2) // Rock chord intervals

        for (sampleIndex in 0 until totalSamples) {
            val t = sampleIndex.toDouble() / sampleRate
            val currentBeat = (t / beatDuration)
            val beatPhase = currentBeat - currentBeat.toInt()
            val barIndex = (currentBeat / beatsPerBar).toInt()
            val beatInBar = (currentBeat.toInt() % beatsPerBar)

            // Chord progression change every 2 beats
            val chordStep = ((currentBeat / 2).toInt()) % intervals.size
            val chordMult = intervals[chordStep]
            val chordRoot = config.rootFreq * chordMult

            // 1. Distorted Rock Guitar (synthesized with overtones and soft clipping)
            val g1 = sin(2 * PI * chordRoot * t)
            val g2 = sin(2 * PI * (chordRoot * 1.5) * t) * 0.7 // Fifth
            val g3 = sin(2 * PI * (chordRoot * 2.0) * t) * 0.5 // Octave
            val rawGuitar = (g1 + g2 + g3) * 1.5
            // Soft-clipping distortion: tanh-like
            val distortedGuitar = Math.tanh(rawGuitar * 2.2) * 0.45

            // 2. Heavy Rock Bass
            val bassFreq = chordRoot * 0.5
            val bass = (sin(2 * PI * bassFreq * t) + 0.3 * sin(4 * PI * bassFreq * t)) * 0.4

            // 3. Drum Section (Kick on 0 & 2, Snare on 1 & 3, Hi-Hat on every 8th note)
            val isKickBeat = (beatInBar == 0 || beatInBar == 2)
            val isSnareBeat = (beatInBar == 1 || beatInBar == 3)

            // Kick drum: rapid pitch drop from 120Hz to 45Hz
            var kick = 0.0
            if (isKickBeat && beatPhase < 0.25) {
                val kickEnv = (1.0 - (beatPhase / 0.25))
                val kickFreq = 45.0 + 75.0 * kickEnv
                kick = sin(2 * PI * kickFreq * beatPhase * beatDuration) * kickEnv * 0.55
            }

            // Snare drum: white noise burst + tone
            var snare = 0.0
            if (isSnareBeat && beatPhase < 0.22) {
                val snareEnv = (1.0 - (beatPhase / 0.22))
                val noise = ((sampleIndex * 1103515245 + 12345).ushr(16) % 1000).toDouble() / 500.0 - 1.0
                val tone = sin(2 * PI * 180.0 * beatPhase * beatDuration) * 0.3
                snare = (noise * 0.7 + tone) * snareEnv * 0.45
            }

            // Hi-hat: 8th note tick
            val eighthPhase = (currentBeat * 2.0) - (currentBeat * 2.0).toInt()
            var hihat = 0.0
            if (eighthPhase < 0.08) {
                val hatEnv = 1.0 - (eighthPhase / 0.08)
                val noise = ((sampleIndex * 314159265 + 13579).ushr(16) % 1000).toDouble() / 500.0 - 1.0
                hihat = noise * hatEnv * 0.15
            }

            // Master Mix
            val monoSample = (distortedGuitar + bass + kick + snare + hihat).coerceIn(-0.95, 0.95)

            // Stereo Panning (guitar slightly left, bass centered, snare slightly right)
            val left = (monoSample * 0.95 + distortedGuitar * 0.1).coerceIn(-0.99, 0.99)
            val right = (monoSample * 0.95 - distortedGuitar * 0.1).coerceIn(-0.99, 0.99)

            val leftShort = (left * 32767).toInt().toShort()
            val rightShort = (right * 32767).toInt().toShort()

            pcmBuffer.putShort(leftShort)
            pcmBuffer.putShort(rightShort)

            if (!pcmBuffer.hasRemaining()) {
                fos.write(pcmBuffer.array())
                pcmBuffer.clear()
            }
        }

        if (pcmBuffer.position() > 0) {
            fos.write(pcmBuffer.array(), 0, pcmBuffer.position())
        }

        fos.flush()
        fos.close()
    }
}
