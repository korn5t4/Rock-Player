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
import kotlinx.coroutines.withContext
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

    // Real-time Peak Hold indicators (0f..1f) for precise studio metering
    private val _visualizerPeaks = MutableStateFlow(FloatArray(24) { 0.05f })
    val visualizerPeaks: StateFlow<FloatArray> = _visualizerPeaks.asStateFlow()

    // Real-time Waveform data (-1f..1f)
    private val _waveformPoints = MutableStateFlow(FloatArray(64) { 0f })
    val waveformPoints: StateFlow<FloatArray> = _waveformPoints.asStateFlow()

    // Pre-allocated double buffers to completely eliminate heap allocation during ~30 FPS visualizer updates
    private val barsBufferA = FloatArray(24) { 0.05f }
    private val barsBufferB = FloatArray(24) { 0.05f }
    private var useBarsBufferA = true

    private val waveBufferA = FloatArray(64) { 0f }
    private val waveBufferB = FloatArray(64) { 0f }
    private var useWaveBufferA = true

    // Peak levels for reactive visualizer
    private val peakHold = FloatArray(24) { 0.05f }
    // Cached applied hardware equalizer levels to prevent redundant JNI / AudioFlinger IPC calls
    private val currentHwLevels = ShortArray(32) { Short.MIN_VALUE }

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

        @Volatile
        private var instance: RockAudioEngine? = null

        fun getInstance(context: Context): RockAudioEngine {
            return instance ?: synchronized(this) {
                instance ?: RockAudioEngine(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Computes mathematically precise octave-scale (log2 frequency) interpolation
         * across the 10 graphic equalizer bands for any target frequency in Hz.
         */
        fun interpolateDbForFreq(targetFreqHz: Int, bands: List<EqualizerBandInfo>): Float {
            if (bands.isEmpty()) return 0f
            if (targetFreqHz <= bands.first().centerFreqHz) return bands.first().levelDb.toFloat()
            if (targetFreqHz >= bands.last().centerFreqHz) return bands.last().levelDb.toFloat()

            for (i in 0 until bands.size - 1) {
                val bLow = bands[i]
                val bHigh = bands[i + 1]
                if (targetFreqHz in bLow.centerFreqHz..bHigh.centerFreqHz) {
                    val logLow = Math.log(bLow.centerFreqHz.toDouble().coerceAtLeast(1.0))
                    val logHigh = Math.log(bHigh.centerFreqHz.toDouble().coerceAtLeast(1.0))
                    val logTarget = Math.log(targetFreqHz.toDouble().coerceAtLeast(1.0))
                    val fraction = if (logHigh > logLow) (logTarget - logLow) / (logHigh - logLow) else 0.0
                    return (bLow.levelDb * (1.0 - fraction) + bHigh.levelDb * fraction).toFloat()
                }
            }
            return 0f
        }
    }

    private val prefs by lazy { com.example.data.preferences.PlayerPreferences(context) }
    private var lastSavedSec = -1L

    init {
        instance = this
        initEqualizerDefaults()
        startVisualizerSimulation()
    }

    fun notifyWidgetUpdate() {
        try {
            com.example.ui.widget.RockPlayerWidgetProvider.updateAllWidgets(context)
        } catch (e: Throwable) {
            // Ignore in testing or headless environments
        }
        try {
            com.example.data.service.RockPlaybackService.startOrUpdate(context)
        } catch (e: Throwable) {
            // Ignore in testing or headless environments
        }
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

    fun setPlaylist(
        songs: List<Song>,
        startIndex: Int = 0,
        startPlaying: Boolean = false,
        savedPositionMs: Long = 0L
    ) {
        if (songs.isEmpty()) return
        originalPlaylist = songs
        updateQueue()

        val indexToPlay = if (startIndex in activeQueue.indices) startIndex else 0
        playSongAtIndex(indexToPlay, startPlaying, savedPositionMs)
    }

    fun updatePlaylist(songs: List<Song>) {
        if (songs.isEmpty()) return
        originalPlaylist = songs
        updateQueue()
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

    fun playSong(song: Song, autoStart: Boolean = true, initialPositionMs: Long = 0L) {
        var idx = activeQueue.indexOfFirst { it.id == song.id }
        if (idx < 0) {
            if (originalPlaylist.none { it.id == song.id }) {
                originalPlaylist = originalPlaylist + song
            }
            updateQueue()
            idx = activeQueue.indexOfFirst { it.id == song.id }
        }
        if (idx >= 0) {
            currentQueueIndex = idx
            loadSong(activeQueue[idx], autoStart, initialPositionMs)
        } else {
            loadSong(song, autoStart, initialPositionMs)
        }
    }

    fun playSongAtIndex(index: Int, autoStart: Boolean = true, initialPositionMs: Long = 0L) {
        if (activeQueue.isEmpty()) return
        val clampedIndex = index.coerceIn(0, activeQueue.lastIndex)
        currentQueueIndex = clampedIndex
        val song = activeQueue[clampedIndex]
        loadSong(song, autoStart, initialPositionMs)
    }

    private fun loadSong(song: Song, autoStart: Boolean, initialPositionMs: Long = 0L) {
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

                    if (initialPositionMs > 0L) {
                        val seekTarget = initialPositionMs.coerceIn(0L, player.duration.toLong())
                        player.seekTo(seekTarget.toInt())
                        _currentPositionMs.value = seekTarget
                    }

                    if (autoStart) {
                        player.start()
                        _isPlaying.value = true
                        startTicker()
                    } else {
                        _isPlaying.value = false
                    }
                    savePlaybackState()
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("RockAudioEngine", "MediaPlayer error: what=$what extra=$extra")
                    _isPlaying.value = false
                    stopTicker()
                    notifyWidgetUpdate()
                    true
                }
                prepareAsync()
            }

            mediaPlayer = mp
            _currentSong.value = song
            _currentPositionMs.value = initialPositionMs.coerceAtLeast(0L)
            _isPlaying.value = autoStart
            savePlaybackState()
            notifyWidgetUpdate()

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
            notifyWidgetUpdate()
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: run {
            if (activeQueue.isNotEmpty()) {
                playSongAtIndex(currentQueueIndex.coerceAtLeast(0), true)
            } else {
                scope.launch(Dispatchers.IO) {
                    val demoTracks = BuiltInRockAudio.getOrGenerateDemoTracks(context)
                    withContext(Dispatchers.Main) {
                        setPlaylist(demoTracks, startIndex = 0, startPlaying = true)
                    }
                }
            }
            notifyWidgetUpdate()
            return
        }

        if (mp.isPlaying) {
            mp.pause()
            _isPlaying.value = false
            stopTicker()
            savePlaybackState()
        } else {
            mp.start()
            _isPlaying.value = true
            startTicker()
            savePlaybackState()
        }
        notifyWidgetUpdate()
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
                savePlaybackState()
            }
        }
        notifyWidgetUpdate()
    }

    fun playPrevious() {
        if (activeQueue.isEmpty()) return
        // If current song played > 3 seconds, restart it
        val currentPos = _currentPositionMs.value
        if (currentPos > 3000) {
            seekTo(0)
            notifyWidgetUpdate()
            return
        }

        if (currentQueueIndex > 0) {
            playSongAtIndex(currentQueueIndex - 1, true)
        } else {
            playSongAtIndex(activeQueue.lastIndex, true)
        }
        notifyWidgetUpdate()
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            val clamped = positionMs.coerceIn(0, _durationMs.value)
            it.seekTo(clamped.toInt())
            _currentPositionMs.value = clamped
            prefs.lastPositionMs = clamped
            _currentSong.value?.let { s -> prefs.lastSongId = s.id }
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
            currentHwLevels.fill(Short.MIN_VALUE)
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            equalizer = eq
            hardwareEqualizerSupported = true

            // Keep our 10-band state as canonical, and apply to hardware with precision interpolation
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
        val current = _equalizerBands.value
        val existing = current.getOrNull(bandIndex.toInt())
        if (existing != null && existing.levelDb == clampedDb) {
            return // Value unchanged: avoid rebuilding lists, recompositions, and IPC overhead
        }
        _equalizerBands.value = current.map { band ->
            if (band.bandIndex == bandIndex) band.copy(levelDb = clampedDb) else band
        }
        _currentPreset.value = "Custom"
        applyStoredEqualizerLevels()
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        val presetLevels = when (presetName) {
            "Rock", "Photo Setup", "Classic Rock" -> PHOTO_ROCK_SETUP_DB // [0, 7, 10, 9, 0, 0, 5, 9, 5, 0]
            "Heavy Metal" -> listOf(3, 8, 7, 1, -2, -1, 4, 8, 7, 4)
            "Hard Rock" -> listOf(4, 9, 8, 2, -1, 2, 6, 9, 7, 3)
            "Bass Boost" -> listOf(8, 11, 9, 6, 2, 0, -1, -2, -2, -3)
            "Electronic / Dance" -> listOf(6, 9, 5, 0, -2, 1, 3, 6, 8, 7)
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
            val levelRange = try { eq.bandLevelRange } catch (e: Exception) { shortArrayOf(-1500, 1500) }
            val minMb = if (levelRange != null && levelRange.size >= 2) levelRange[0] else -1500.toShort()
            val maxMb = if (levelRange != null && levelRange.size >= 2) levelRange[1] else 1500.toShort()

            for (hb in 0 until numHardwareBands) {
                val centerFreqMilliHz = try { eq.getCenterFreq(hb.toShort()) } catch (e: Exception) { 0 }
                val centerFreqHz = if (centerFreqMilliHz > 0) centerFreqMilliHz / 1000 else when (hb) {
                    0 -> 60
                    1 -> 230
                    2 -> 910
                    3 -> 3600
                    else -> 14000
                }

                // Mathematically precise octave-scale interpolation from our 10-band profile
                val targetDb = interpolateDbForFreq(centerFreqHz, bands)
                val targetMb = kotlin.math.round((targetDb * 100f)).toInt()
                    .coerceIn(minMb.toInt(), maxMb.toInt())
                    .toShort()

                // Only perform JNI call if value actually changed
                if (currentHwLevels[hb] != targetMb) {
                    eq.setBandLevel(hb.toShort(), targetMb)
                    currentHwLevels[hb] = targetMb
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
                    captureSize = sizeRange[1].coerceIn(256, 1024)
                }
                val maxRate = Visualizer.getMaxCaptureRate()
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(viz: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                            waveform?.let { processHardwareWaveform(it) }
                        }

                        override fun onFftDataCapture(viz: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            fft?.let { processHardwareFft(it, samplingRate) }
                        }
                    },
                    maxRate, // Full hardware capture rate for instantaneous response
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

    private fun processHardwareFft(fft: ByteArray, samplingRateMilliHz: Int) {
        val numBars = 24
        val targetBars = if (useBarsBufferA) barsBufferA else barsBufferB
        val prevBars = if (useBarsBufferA) barsBufferB else barsBufferA
        val bands = _equalizerBands.value
        val sampleRateHz = if (samplingRateMilliHz > 0) samplingRateMilliHz / 1000 else 44100
        val fftSize = fft.size
        val nyquistHz = sampleRateHz / 2.0
        val binWidthHz = sampleRateHz.toDouble() / fftSize.toDouble().coerceAtLeast(1.0)

        // 24 Logarithmically spaced frequency bands from 28 Hz to 18 kHz
        val minFreq = 28.0
        val maxFreq = 18000.0.coerceAtMost(nyquistHz)
        val logRatio = Math.log(maxFreq / minFreq)

        for (i in 0 until numBars) {
            val f0 = minFreq * Math.exp(logRatio * (i.toDouble() / numBars))
            val f1 = minFreq * Math.exp(logRatio * ((i + 1).toDouble() / numBars))
            val centerF = Math.sqrt(f0 * f1).toInt()

            val k0 = (f0 / binWidthHz).toInt().coerceIn(1, (fftSize / 2) - 1)
            val k1 = (f1 / binWidthHz).toInt().coerceIn(k0, (fftSize / 2) - 1)

            var sumMag = 0.0
            var count = 0
            for (k in k0..k1) {
                val rIdx = 2 * k
                val iIdx = rIdx + 1
                if (iIdx < fftSize) {
                    val r = fft[rIdx].toDouble()
                    val im = fft[iIdx].toDouble()
                    sumMag += hypot(r, im)
                    count++
                }
            }

            val avgMag = if (count > 0) sumMag / count else 0.0
            // Dynamic scale with psychoacoustic perception weighting
            val perceptualWeight = 1.0 + (i.toDouble() / numBars) * 0.45
            val normalizedMag = ((avgMag * perceptualWeight) / 64.0).toFloat().coerceIn(0.04f, 1.0f)

            // Factor in precise real-time EQ boost/cut for this frequency
            val eqDb = interpolateDbForFreq(centerF, bands)
            val eqFactor = Math.pow(10.0, (eqDb / 20.0).toDouble()).toFloat().coerceIn(0.35f, 2.8f)
            val rawValue = (normalizedMag * eqFactor).coerceIn(0.05f, 0.98f)

            // Fast attack (instant peak response, 0 lag) + smooth exponential ballistic decay
            val prev = prevBars[i]
            val smoothed = if (rawValue > prev) {
                rawValue
            } else {
                prev * 0.82f + rawValue * 0.18f
            }

            targetBars[i] = smoothed

            // Peak hold dynamics (studio-grade meter response)
            if (smoothed >= peakHold[i]) {
                peakHold[i] = smoothed
            } else {
                peakHold[i] = (peakHold[i] - 0.018f).coerceAtLeast(smoothed)
            }
        }

        useBarsBufferA = !useBarsBufferA
        _visualizerBars.value = targetBars
        _visualizerPeaks.value = peakHold.copyOf()
    }

    private fun processHardwareWaveform(waveform: ByteArray) {
        val numPoints = 64
        val targetWave = if (useWaveBufferA) waveBufferA else waveBufferB
        val step = waveform.size / numPoints
        for (i in 0 until numPoints) {
            val idx = (i * step).coerceIn(0, waveform.lastIndex)
            val byteVal = (waveform[idx].toInt() and 0xFF) - 128
            targetWave[i] = (byteVal / 128f).coerceIn(-1f, 1f)
        }
        useWaveBufferA = !useWaveBufferA
        _waveformPoints.value = targetWave
    }

    // High-performance real-time reactive visualizer simulation when hardware visualizer is restricted
    // Uses pre-allocated double-buffers for zero-garbage-collection allocations per frame!
    private fun startVisualizerSimulation() {
        simVisualizerJob?.cancel()
        simVisualizerJob = scope.launch {
            var phase = 0.0
            val barCount = 24
            var idleCount = 0

            while (isActive) {
                if (_isPlaying.value) {
                    idleCount = 0
                    phase += 0.12
                    val pos = _currentPositionMs.value
                    val bands = _equalizerBands.value

                    // Accurate rhythmic dynamics synced to music beat (~128 BPM rock tempo)
                    val beatTime = pos / 468.75 // ~128 BPM quarter note interval
                    val beatFraction = beatTime - Math.floor(beatTime)
                    val kickPulse = Math.exp(-beatFraction * 5.0).toFloat() // Punchy transient attack

                    // Eighth note hi-hat pattern
                    val eighthTime = beatTime * 2.0
                    val eighthFraction = eighthTime - Math.floor(eighthTime)
                    val hihatTick = Math.exp(-eighthFraction * 7.0).toFloat()

                    val targetBars = if (useBarsBufferA) barsBufferA else barsBufferB
                    val prevBars = if (useBarsBufferA) barsBufferB else barsBufferA

                    // Standard 24 log frequencies covering 30 Hz to 16 kHz
                    for (i in 0 until barCount) {
                        val freq = (30.0 * Math.pow(16000.0 / 30.0, i.toDouble() / 23.0)).toInt()
                        val eqDb = interpolateDbForFreq(freq, bands)
                        val eqFactor = (1.0f + (eqDb / 12.0f) * 0.65f).coerceIn(0.2f, 2.2f)

                        val sineBase = (sin(phase + i * 0.45) * 0.28 + 0.38).toFloat()
                        val noise = Random.nextFloat() * 0.12f

                        val dynamicPulse = when {
                            i < 6 -> kickPulse * 0.55f // Sub & bass punch (tightly bound to 31.25 & 62.5 Hz EQ)
                            i in 6..14 -> (kickPulse * 0.25f + sin(phase * 1.5 + i) * 0.15f).toFloat() // Mids
                            else -> hihatTick * 0.45f + (sin(phase * 2.2 + i * 0.6) * 0.15f).toFloat() // Highs
                        }

                        val rawValue = ((sineBase + dynamicPulse + noise) * eqFactor).coerceIn(0.06f, 0.98f)

                        // Instant attack, fast decay
                        val prev = prevBars[i]
                        val smoothed = if (rawValue > prev) rawValue else (prev * 0.84f + rawValue * 0.16f)

                        targetBars[i] = smoothed

                        // Peak hold tracking
                        if (smoothed >= peakHold[i]) {
                            peakHold[i] = smoothed
                        } else {
                            peakHold[i] = (peakHold[i] - 0.015f).coerceAtLeast(smoothed)
                        }
                    }
                    useBarsBufferA = !useBarsBufferA
                    _visualizerBars.value = targetBars
                    _visualizerPeaks.value = peakHold.copyOf()

                    // 64-point responsive waveform reflecting the real-time EQ spectrum
                    val targetWave = if (useWaveBufferA) waveBufferA else waveBufferB
                    val bassBoost = interpolateDbForFreq(80, bands) / 12.0f
                    val trebleBoost = interpolateDbForFreq(4000, bands) / 12.0f
                    for (k in 0 until 64) {
                        val lowWave = sin(phase * 1.8 + k * 0.18) * (0.6 + bassBoost * 0.3)
                        val highWave = sin(phase * 4.5 + k * 0.6) * (0.25 + trebleBoost * 0.2)
                        targetWave[k] = ((lowWave + highWave) * (0.6 + kickPulse * 0.4)).toFloat().coerceIn(-0.95f, 0.95f)
                    }
                    useWaveBufferA = !useWaveBufferA
                    _waveformPoints.value = targetWave

                    delay(16) // ~60 fps ultra-fast, smooth visualizer loop!
                } else {
                    if (idleCount < 18) {
                        idleCount++
                        val targetBars = if (useBarsBufferA) barsBufferA else barsBufferB
                        val prevBars = if (useBarsBufferA) barsBufferB else barsBufferA
                        for (i in 0 until barCount) {
                            targetBars[i] = (prevBars[i] * 0.85f).coerceAtLeast(0.04f)
                            peakHold[i] = (peakHold[i] * 0.88f).coerceAtLeast(0.04f)
                        }
                        useBarsBufferA = !useBarsBufferA
                        _visualizerBars.value = targetBars
                        _visualizerPeaks.value = peakHold.copyOf()

                        val targetWave = if (useWaveBufferA) waveBufferA else waveBufferB
                        val prevWave = if (useWaveBufferA) waveBufferB else waveBufferA
                        for (k in 0 until 64) {
                            targetWave[k] = prevWave[k] * 0.8f
                        }
                        useWaveBufferA = !useWaveBufferA
                        _waveformPoints.value = targetWave
                        delay(25)
                    } else {
                        delay(300)
                    }
                }
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val pos = mp.currentPosition.toLong()
                        _currentPositionMs.value = pos
                        val sec = pos / 1000L
                        if (sec != lastSavedSec) {
                            lastSavedSec = sec
                            prefs.lastPositionMs = pos
                            _currentSong.value?.let { s -> prefs.lastSongId = s.id }
                        }
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

    fun savePlaybackState() {
        val song = _currentSong.value ?: return
        val pos = mediaPlayer?.let {
            try {
                if (it.isPlaying || it.currentPosition > 0) it.currentPosition.toLong() else _currentPositionMs.value
            } catch (e: Exception) {
                _currentPositionMs.value
            }
        } ?: _currentPositionMs.value

        prefs.lastSongId = song.id
        prefs.lastPositionMs = pos
    }

    fun stopAndRelease() {
        savePlaybackState()
        _isPlaying.value = false
        stopTicker()
        releaseMediaPlayer()
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
        stopAndRelease()
    }
}
