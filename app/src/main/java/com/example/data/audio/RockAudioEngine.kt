package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.util.Log
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class EqualizerBandInfo(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val levelDb: Int, // -12 to +12 dB
    val freqLabel: String = ""
) {
    val displayLabel: String
        get() = if (freqLabel.isNotEmpty()) freqLabel else when (bandIndex.toInt()) {
            0 -> "31.25"
            1 -> "62.5"
            2 -> "125"
            3 -> "250"
            4 -> "500"
            5 -> "1K"
            6 -> "2K"
            7 -> "4K"
            8 -> "8K"
            9 -> "16K"
            else -> if (centerFreqHz >= 1000) "${centerFreqHz / 1000}K" else "$centerFreqHz"
        }
}

class RockAudioEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var visualizer: Visualizer? = null

    // Playback state
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    // Playlist
    private var originalPlaylist: List<Song> = emptyList()
    private var activeQueue: List<Song> = emptyList()
    private var currentQueueIndex: Int = -1

    // Equalizer state
    private val _equalizerBands = MutableStateFlow<List<EqualizerBandInfo>>(emptyList())
    val equalizerBands: StateFlow<List<EqualizerBandInfo>> = _equalizerBands.asStateFlow()

    private val _currentPreset = MutableStateFlow("Rock")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    // Real-time Visualizer state: 24 frequency bars (0f..1f)
    private val _visualizerBars = MutableStateFlow(FloatArray(24) { 0.05f })
    val visualizerBars: StateFlow<FloatArray> = _visualizerBars.asStateFlow()

    // Real-time Waveform data (-1f..1f)
    private val _waveformPoints = MutableStateFlow(FloatArray(64) { 0f })
    val waveformPoints: StateFlow<FloatArray> = _waveformPoints.asStateFlow()

    // Peak levels for reactive visualizer
    private val peakHold = FloatArray(24) { 0f }

    private var tickerJob: Job? = null
    private var simVisualizerJob: Job? = null
    private var hasHardwareVisualizer = false
    private var hardwareVisualizerSupported: Boolean? = null
    private var hardwareEqualizerSupported: Boolean? = null

    var onSongCompletedListener: (() -> Unit)? = null

    companion object {
        val TEN_BAND_FREQS = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        val TEN_BAND_LABELS = listOf("31.25", "62.5", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
        // Exact setup from user's uploaded image (grafick equalizer.png)
        val PHOTO_ROCK_SETUP_DB = listOf(0, 7, 10, 9, 0, 0, 5, 9, 5, 0)
    }

    init {
        initEqualizerDefaults()
        startVisualizerSimulation()
    }

    private fun initEqualizerDefaults() {
        val bands = TEN_BAND_FREQS.mapIndexed { index, freq ->
            EqualizerBandInfo(
                bandIndex = index.toShort(),
                centerFreqHz = freq,
                levelDb = PHOTO_ROCK_SETUP_DB[index],
                freqLabel = TEN_BAND_LABELS[index]
            )
        }
        _equalizerBands.value = bands
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0, startPlaying: Boolean = false) {
        if (songs.isEmpty()) return
        originalPlaylist = songs
        updateQueue()

        val indexToPlay = if (startIndex in activeQueue.indices) startIndex else 0
        playSongAtIndex(indexToPlay, startPlaying)
    }

    private fun updateQueue() {
        if (_isShuffle.value) {
            val current = _currentSong.value
            val rest = originalPlaylist.filter { it.id != current?.id }.shuffled()
            activeQueue = if (current != null) listOf(current) + rest else originalPlaylist.shuffled()
            currentQueueIndex = if (current != null) 0 else 0
        } else {
            activeQueue = originalPlaylist
            val current = _currentSong.value
            currentQueueIndex = if (current != null) {
                originalPlaylist.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
            } else {
                0
            }
        }
    }

    fun playSongAtIndex(index: Int, autoStart: Boolean = true) {
        if (activeQueue.isEmpty()) return
        val clampedIndex = index.coerceIn(0, activeQueue.lastIndex)
        currentQueueIndex = clampedIndex
        val song = activeQueue[clampedIndex]
        loadSong(song, autoStart)
    }

    private fun loadSong(song: Song, autoStart: Boolean) {
        try {
            releaseMediaPlayer()

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(song.uriString))
                setOnPreparedListener { player ->
                    _durationMs.value = player.duration.toLong().coerceAtLeast(song.durationMs)
                    attachEqualizer(player.audioSessionId)
                    attachVisualizer(player.audioSessionId)

                    if (autoStart) {
                        player.start()
                        _isPlaying.value = true
                        startTicker()
                    }
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("RockAudioEngine", "MediaPlayer error: what=$what extra=$extra")
                    false
                }
                prepareAsync()
            }

            mediaPlayer = mp
            _currentSong.value = song
            _currentPositionMs.value = 0L
            _isPlaying.value = autoStart

            // Ensure *.jpg album art is resolved for the song being played
            if (song.albumArtUriString == null) {
                scope.launch(Dispatchers.IO) {
                    val resolved = com.example.data.repository.AlbumArtResolver.resolveAlbumArt(context, song)
                    if (resolved != null) {
                        launch(Dispatchers.Main) {
                            updateCurrentSongArt(resolved.toString())
                        }
                    }
                }
            }

            if (autoStart) {
                startTicker()
            }
        } catch (e: Exception) {
            Log.e("RockAudioEngine", "Error loading song: ${song.title}", e)
        }
    }

    fun updateCurrentSongArt(artUriString: String) {
        val current = _currentSong.value ?: return
        if (current.albumArtUriString != artUriString) {
            val updated = current.copy(albumArtUriString = artUriString)
            _currentSong.value = updated
            val idx = activeQueue.indexOfFirst { it.id == current.id }
            if (idx >= 0) {
                val mutable = activeQueue.toMutableList()
                mutable[idx] = updated
                activeQueue = mutable
            }
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: run {
            if (activeQueue.isNotEmpty()) {
                playSongAtIndex(currentQueueIndex.coerceAtLeast(0), true)
            }
            return
        }

        if (mp.isPlaying) {
            mp.pause()
            _isPlaying.value = false
            stopTicker()
        } else {
            mp.start()
            _isPlaying.value = true
            startTicker()
        }
    }

    fun playNext() {
        if (activeQueue.isEmpty()) return
        if (currentQueueIndex < activeQueue.lastIndex) {
            playSongAtIndex(currentQueueIndex + 1, true)
        } else {
            // Reached end
            if (_repeatMode.value == RepeatMode.ALL) {
                playSongAtIndex(0, true)
            } else {
                _isPlaying.value = false
                _currentPositionMs.value = 0
                stopTicker()
            }
        }
    }

    fun playPrevious() {
        if (activeQueue.isEmpty()) return
        // If current song played > 3 seconds, restart it
        val currentPos = _currentPositionMs.value
        if (currentPos > 3000) {
            seekTo(0)
            return
        }

        if (currentQueueIndex > 0) {
            playSongAtIndex(currentQueueIndex - 1, true)
        } else {
            playSongAtIndex(activeQueue.lastIndex, true)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            val clamped = positionMs.coerceIn(0, _durationMs.value)
            it.seekTo(clamped.toInt())
            _currentPositionMs.value = clamped
        }
    }

    fun toggleShuffle(): Boolean {
        val newState = !_isShuffle.value
        _isShuffle.value = newState
        updateQueue()
        return newState
    }

    fun cycleRepeatMode(): RepeatMode {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        return next
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    fun setShuffle(enabled: Boolean) {
        _isShuffle.value = enabled
        updateQueue()
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                _isPlaying.value = true
                startTicker()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (currentQueueIndex < activeQueue.lastIndex) {
                    playNext()
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    stopTicker()
                    onSongCompletedListener?.invoke()
                }
            }
        }
    }

    // --- Equalizer Logic ---

    private fun attachEqualizer(audioSessionId: Int) {
        if (hardwareEqualizerSupported == false || audioSessionId == 0) {
            return
        }
        try {
            equalizer?.release()
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            equalizer = eq
            hardwareEqualizerSupported = true

            // Keep our 10-band state as canonical, and apply to hardware
            applyStoredEqualizerLevels()
        } catch (t: Throwable) {
            Log.w("RockAudioEngine", "Hardware Equalizer not supported on this platform: ${t.message}")
            try {
                equalizer?.release()
            } catch (ignored: Throwable) {}
            equalizer = null
            hardwareEqualizerSupported = false
        }
    }

    fun setBandLevel(bandIndex: Short, dbLevel: Int) {
        val clampedDb = dbLevel.coerceIn(-12, 12)
        _equalizerBands.value = _equalizerBands.value.map { band ->
            if (band.bandIndex == bandIndex) band.copy(levelDb = clampedDb) else band
        }
        _currentPreset.value = "Custom"
        applyStoredEqualizerLevels()
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        val presetLevels = when (presetName) {
            "Rock", "Photo Setup" -> PHOTO_ROCK_SETUP_DB // [0, 7, 10, 9, 0, 0, 5, 9, 5, 0]
            "Heavy Metal" -> listOf(3, 8, 7, 1, -2, -1, 4, 8, 7, 4)
            "Bass Boost" -> listOf(8, 11, 9, 6, 2, 0, -1, -2, -2, -3)
            "Acoustic" -> listOf(3, 4, 3, 2, 2, 3, 4, 5, 4, 2)
            "Vocal Lead" -> listOf(-3, -2, 0, 2, 5, 6, 5, 3, 1, 0)
            "Flat" -> listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            else -> PHOTO_ROCK_SETUP_DB
        }

        val currentList = _equalizerBands.value
        val updated = currentList.mapIndexed { idx, band ->
            val targetDb = presetLevels.getOrElse(idx) { 0 }
            band.copy(levelDb = targetDb)
        }
        _equalizerBands.value = updated
        applyStoredEqualizerLevels()
    }

    private fun applyStoredEqualizerLevels() {
        val eq = equalizer ?: return
        try {
            val numHardwareBands = eq.numberOfBands.toInt()
            val bands = _equalizerBands.value
            if (numHardwareBands >= 10) {
                bands.forEachIndexed { idx, b ->
                    if (idx < numHardwareBands) {
                        eq.setBandLevel(idx.toShort(), (b.levelDb * 100).toShort())
                    }
                }
            } else if (numHardwareBands == 5 && bands.size >= 10) {
                // Map the 10 octave bands down to 5 hardware bands
                val hw0 = ((bands[0].levelDb + bands[1].levelDb) / 2).coerceIn(-12, 12)
                val hw1 = ((bands[2].levelDb + bands[3].levelDb) / 2).coerceIn(-12, 12)
                val hw2 = ((bands[4].levelDb + bands[5].levelDb) / 2).coerceIn(-12, 12)
                val hw3 = ((bands[6].levelDb + bands[7].levelDb) / 2).coerceIn(-12, 12)
                val hw4 = ((bands[8].levelDb + bands[9].levelDb) / 2).coerceIn(-12, 12)
                eq.setBandLevel(0, (hw0 * 100).toShort())
                eq.setBandLevel(1, (hw1 * 100).toShort())
                eq.setBandLevel(2, (hw2 * 100).toShort())
                eq.setBandLevel(3, (hw3 * 100).toShort())
                eq.setBandLevel(4, (hw4 * 100).toShort())
            } else {
                bands.forEachIndexed { idx, b ->
                    if (idx < numHardwareBands) {
                        eq.setBandLevel(idx.toShort(), (b.levelDb * 100).toShort())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("RockAudioEngine", "Error applying equalizer levels: ${e.message}")
        }
    }

    // --- Real-time Visualizer ---

    private fun attachVisualizer(audioSessionId: Int) {
        // If we already detected that hardware visualizer is unsupported, or audioSessionId is invalid
        if (hardwareVisualizerSupported == false || audioSessionId == 0) {
            hasHardwareVisualizer = false
            return
        }

        // Visualizer requires RECORD_AUDIO permission; without it AudioFlinger fails with status -1 / initCheck -3
        val hasPermission = try {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (ignored: Throwable) {
            false
        }

        if (!hasPermission) {
            hasHardwareVisualizer = false
            return
        }

        try {
            visualizer?.release()
            visualizer = null
            hasHardwareVisualizer = false

            val viz = Visualizer(audioSessionId).apply {
                val sizeRange = Visualizer.getCaptureSizeRange()
                if (sizeRange != null && sizeRange.size >= 2) {
                    captureSize = sizeRange[1]
                }
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(viz: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                            waveform?.let { processHardwareWaveform(it) }
                        }

                        override fun onFftDataCapture(viz: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            fft?.let { processHardwareFft(it) }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )
                enabled = true
            }
            visualizer = viz
            hasHardwareVisualizer = true
            hardwareVisualizerSupported = true
        } catch (t: Throwable) {
            Log.w("RockAudioEngine", "Hardware Visualizer not supported or effect creation denied (${t.message}), switching seamlessly to reactive audio visualizer.")
            try {
                visualizer?.release()
            } catch (ignored: Throwable) {}
            visualizer = null
            hasHardwareVisualizer = false
            hardwareVisualizerSupported = false
        }
    }

    private fun processHardwareFft(fft: ByteArray) {
        val numBars = 24
        val bars = FloatArray(numBars)
        val bandSize = (fft.size / 2) / numBars

        for (i in 0 until numBars) {
            var sum = 0.0
            for (j in 0 until bandSize) {
                val index = 2 + (i * bandSize + j) * 2
                if (index + 1 < fft.size) {
                    val r = fft[index].toDouble()
                    val im = fft[index + 1].toDouble()
                    sum += hypot(r, im)
                }
            }
            val mag = (sum / bandSize.coerceAtLeast(1) / 128.0).toFloat().coerceIn(0.05f, 1f)
            bars[i] = mag
        }
        _visualizerBars.value = bars
    }

    private fun processHardwareWaveform(waveform: ByteArray) {
        val numPoints = 64
        val points = FloatArray(numPoints)
        val step = waveform.size / numPoints
        for (i in 0 until numPoints) {
            val idx = (i * step).coerceIn(0, waveform.lastIndex)
            val byteVal = (waveform[idx].toInt() and 0xFF) - 128
            points[i] = (byteVal / 128f).coerceIn(-1f, 1f)
        }
        _waveformPoints.value = points
    }

    // High-performance real-time reactive visualizer simulation when hardware visualizer is restricted
    private fun startVisualizerSimulation() {
        simVisualizerJob?.cancel()
        simVisualizerJob = scope.launch {
            var phase = 0.0
            val barCount = 24
            val eqLevels = _equalizerBands.value.map { (it.levelDb + 12) / 24f }

            while (isActive) {
                if (_isPlaying.value) {
                    phase += 0.18
                    val pos = _currentPositionMs.value
                    // Real-time beat rhythm based on rock BPM (~130BPM)
                    val beatPulse = (sin(pos * 0.013) + 1.0) * 0.5
                    val bassEnergy = (sin(pos * 0.007) * 0.4 + 0.6).toFloat()

                    val newBars = FloatArray(barCount)
                    for (i in 0 until barCount) {
                        val base = (sin(phase + i * 0.4) * 0.35 + 0.4).toFloat()
                        val noise = Random.nextFloat() * 0.25f
                        val eqFactor = eqLevels.getOrElse(i / 5) { 0.5f }
                        val bassWeight = if (i < 6) bassEnergy * 0.6f else 0f
                        val barValue = (base * 0.5f + beatPulse.toFloat() * 0.3f + noise + bassWeight) * (eqFactor + 0.5f)

                        newBars[i] = barValue.coerceIn(0.08f, 0.98f)
                    }
                    _visualizerBars.value = newBars

                    // Waveform points
                    val newWave = FloatArray(64)
                    for (k in 0 until 64) {
                        val w = sin(phase * 2.0 + k * 0.25) * 0.6 + sin(phase * 0.8 + k * 0.1) * 0.3
                        newWave[k] = (w * (beatPulse * 0.5 + 0.5)).toFloat().coerceIn(-0.95f, 0.95f)
                    }
                    _waveformPoints.value = newWave
                } else {
                    // Decay gently to resting level
                    val decaying = _visualizerBars.value.map { (it * 0.85f).coerceAtLeast(0.05f) }.toFloatArray()
                    _visualizerBars.value = decaying
                    val decayingWave = _waveformPoints.value.map { it * 0.8f }.toFloatArray()
                    _waveformPoints.value = decayingWave
                }
                delay(33) // ~30 fps visualizer loop
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _currentPositionMs.value = mp.currentPosition.toLong()
                    }
                }
                delay(150)
            }
        }

        if (!hasHardwareVisualizer) {
            startVisualizerSimulation()
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun releaseMediaPlayer() {
        stopTicker()
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Throwable) {}
        visualizer = null
        hasHardwareVisualizer = false

        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (e: Throwable) {}
        equalizer = null

        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
        } catch (e: Throwable) {}
        mediaPlayer = null
    }

    fun release() {
        simVisualizerJob?.cancel()
        releaseMediaPlayer()
    }
}
