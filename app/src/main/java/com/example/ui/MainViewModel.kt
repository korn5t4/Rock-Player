package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.BuiltInRockAudio
import com.example.data.audio.EqualizerBandInfo
import com.example.data.audio.RockAudioEngine
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.model.VisualizerMode
import com.example.data.preferences.PlayerPreferences
import com.example.data.repository.MusicScanner
import com.example.data.repository.TrackCache
import com.example.ui.theme.PlayerSkinTheme
import com.example.ui.theme.PlayerSkins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    PLAYER,
    EQUALIZER,
    LIBRARY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PlayerPreferences(application)
    val audioEngine = RockAudioEngine.getInstance(application)

    // Current Screen
    private val _currentScreen = MutableStateFlow(AppScreen.PLAYER)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Library
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Remembered Folder State & Authorization
    private val _rememberedFolderUri = MutableStateFlow<String?>(prefs.savedFolderUri)
    val rememberedFolderUri: StateFlow<String?> = _rememberedFolderUri.asStateFlow()

    private val _rememberedFolderName = MutableStateFlow<String?>(prefs.savedFolderName)
    val rememberedFolderName: StateFlow<String?> = _rememberedFolderName.asStateFlow()

    private val _isFolderAuthorized = MutableStateFlow(
        prefs.savedFolderUri?.let { uriStr ->
            try {
                MusicScanner.isFolderPermissionGranted(application, Uri.parse(uriStr))
            } catch (e: Exception) {
                false
            }
        } ?: false
    )
    val isFolderAuthorized: StateFlow<Boolean> = _isFolderAuthorized.asStateFlow()

    // Filtered songs
    val filteredSongs: StateFlow<List<Song>> = combine(_songs, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true) ||
            (it.folderName?.contains(query, ignoreCase = true) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Visualizer Mode
    private val _visualizerMode = MutableStateFlow(VisualizerMode.SPECTRUM_BARS)
    val visualizerMode: StateFlow<VisualizerMode> = _visualizerMode.asStateFlow()

    // Skin Theme
    private val _currentSkin = MutableStateFlow(
        PlayerSkins.allSkins.firstOrNull { it.id == prefs.selectedThemeId } ?: PlayerSkins.ClassicSfondo
    )
    val currentSkin: StateFlow<PlayerSkinTheme> = _currentSkin.asStateFlow()

    // Settings
    private val _autoPlayOnStart = MutableStateFlow(prefs.autoPlayOnStart)
    val autoPlayOnStart: StateFlow<Boolean> = _autoPlayOnStart.asStateFlow()

    private val _lockScreenPlayerEnabled = MutableStateFlow(prefs.lockScreenPlayerEnabled)
    val lockScreenPlayerEnabled: StateFlow<Boolean> = _lockScreenPlayerEnabled.asStateFlow()

    private val _wakeScreenOnTrackChange = MutableStateFlow(prefs.wakeScreenOnTrackChange)
    val wakeScreenOnTrackChange: StateFlow<Boolean> = _wakeScreenOnTrackChange.asStateFlow()

    init {
        // Load initial playback preferences
        audioEngine.setShuffle(prefs.shuffleEnabled)
        audioEngine.setRepeatMode(prefs.repeatMode)

        loadInitialMusicLibrary()
    }

    private fun loadInitialMusicLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                // 1. Built-in rock demo songs
                val demoSongs = BuiltInRockAudio.getOrGenerateDemoTracks(getApplication())
                val currentList = demoSongs.toMutableList()

                // 2. INSTANT CACHE LOAD: load tracks cached from the remembered folder (0ms delay!)
                val savedFolderUriStr = prefs.savedFolderUri
                var folderName = prefs.savedFolderName
                if (!savedFolderUriStr.isNullOrBlank()) {
                    val treeUri = Uri.parse(savedFolderUriStr)
                    val isAuth = MusicScanner.isFolderPermissionGranted(getApplication(), treeUri)
                    _isFolderAuthorized.value = isAuth
                    if (folderName == null) {
                        folderName = MusicScanner.getFolderDisplayName(getApplication(), treeUri)
                        prefs.savedFolderName = folderName
                    }
                    _rememberedFolderName.value = folderName

                    // Load cached tracks immediately without waiting for disk/SAF traversal
                    val cachedTracks = TrackCache.loadCachedTracks(getApplication(), savedFolderUriStr)
                    if (cachedTracks.isNotEmpty()) {
                        val existingIds = currentList.map { it.id }.toSet()
                        val newTracks = cachedTracks.filter { it.id !in existingIds }
                        currentList.addAll(newTracks)
                        _statusMessage.value = "Loaded ${newTracks.size} tracks from authorized folder '$folderName'"
                    }
                }

                // 3. Fast device media store check
                try {
                    val mediaStoreSongs = MusicScanner.scanDeviceMediaStore(getApplication())
                    val existingIds = currentList.map { it.id }.toSet()
                    currentList.addAll(mediaStoreSongs.filter { it.id !in existingIds })
                } catch (e: Exception) {}

                // Set songs immediately so the library is ready in milliseconds!
                _songs.value = currentList
                audioEngine.setPlaylist(currentList, startIndex = 0, startPlaying = false)

                // 4. Background Fast Sync: if folder is remembered and authorized, check for additions/removals
                if (!savedFolderUriStr.isNullOrBlank()) {
                    val treeUri = Uri.parse(savedFolderUriStr)
                    launch(Dispatchers.IO) {
                        try {
                            val cachedMap = currentList.associateBy { it.id }
                            val freshScanned = MusicScanner.scanDocumentTreeUri(getApplication(), treeUri, cachedMap)
                            if (freshScanned.isNotEmpty()) {
                                val demoAndMedia = _songs.value.filter { it.isBuiltIn || it.id.startsWith("mediastore_") }
                                val merged = (demoAndMedia + freshScanned).distinctBy { it.id }
                                _songs.value = merged
                                audioEngine.updatePlaylist(merged)
                                TrackCache.saveCachedTracks(getApplication(), savedFolderUriStr, freshScanned)
                                prefs.savedFolderTrackCount = freshScanned.size
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Restore saved EQ levels
                val savedBands = prefs.getBandLevels()
                val preset = prefs.equalizerPreset
                audioEngine.applyPreset(preset)
                savedBands.forEachIndexed { index, level ->
                    audioEngine.setBandLevel(index.toShort(), level)
                }

                // Check auto-play on start
                if (_autoPlayOnStart.value && currentList.isNotEmpty()) {
                    val lastId = prefs.lastSongId
                    val targetSong = if (lastId != null) currentList.firstOrNull { it.id == lastId } ?: currentList[0] else currentList[0]
                    audioEngine.playSong(targetSong, autoStart = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun scanFolderUri(treeUri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            _statusMessage.value = "Scanning folder (Fast Scan)..."
            try {
                // Ensure and remember persistable permission across restarts
                val granted = MusicScanner.takeAndPersistFolderPermission(getApplication(), treeUri)
                _isFolderAuthorized.value = granted
                prefs.isFolderAuthorized = granted

                val folderName = MusicScanner.getFolderDisplayName(getApplication(), treeUri)

                // Persist folder in preferences so app remembers it on restart!
                prefs.savedFolderUri = treeUri.toString()
                prefs.savedFolderName = folderName
                _rememberedFolderUri.value = treeUri.toString()
                _rememberedFolderName.value = folderName

                // Execute ultra-fast scan
                val cachedMap = _songs.value.associateBy { it.id }
                val scanned = MusicScanner.scanDocumentTreeUri(getApplication(), treeUri, cachedMap)

                if (scanned.isNotEmpty()) {
                    // Cache tracks locally for instant access next time
                    TrackCache.saveCachedTracks(getApplication(), treeUri.toString(), scanned)
                    prefs.savedFolderTrackCount = scanned.size

                    val existing = _songs.value.toMutableList()
                    val existingIds = existing.map { it.id }.toSet()
                    val newTracks = scanned.filter { it.id !in existingIds }
                    existing.addAll(newTracks)
                    _songs.value = existing
                    audioEngine.updatePlaylist(existing)
                    _statusMessage.value = "Authorized & Remembered '$folderName' • Loaded ${scanned.size} tracks (Fast Scan)!"
                } else {
                    _statusMessage.value = "Authorized '$folderName' (no audio files found inside)."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error scanning folder: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun rescanRememberedFolder() {
        val uriStr = _rememberedFolderUri.value ?: return
        scanFolderUri(Uri.parse(uriStr))
    }

    fun clearRememberedFolder() {
        val uriStr = _rememberedFolderUri.value
        if (!uriStr.isNullOrBlank()) {
            try {
                getApplication<Application>().contentResolver.releasePersistableUriPermission(
                    Uri.parse(uriStr),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not held
            }
        }
        TrackCache.clearCache(getApplication())
        prefs.clearSavedFolder()
        _rememberedFolderUri.value = null
        _rememberedFolderName.value = null
        _isFolderAuthorized.value = false
        _statusMessage.value = "Remembered folder cleared."
        loadInitialMusicLibrary()
    }

    fun scanDeviceMediaStore() {
        viewModelScope.launch {
            _isScanning.value = true
            _statusMessage.value = "Scanning device music..."
            try {
                val scanned = MusicScanner.scanDeviceMediaStore(getApplication())
                val existing = _songs.value.toMutableList()
                val existingIds = existing.map { it.id }.toSet()
                val newTracks = scanned.filter { it.id !in existingIds }
                existing.addAll(newTracks)
                _songs.value = existing
                audioEngine.setPlaylist(existing, startIndex = 0, startPlaying = false)
                _statusMessage.value = "Synced ${newTracks.size} songs from device storage."
            } catch (e: Exception) {
                _statusMessage.value = "Scan error: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun playSong(song: Song) {
        audioEngine.playSong(song, autoStart = true)
        prefs.lastSongId = song.id

        // Lazily enrich metadata in background without blocking playback
            if (!song.isBuiltIn && !song.uriString.startsWith("content://media/external/audio/")) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val enriched = MusicScanner.enrichMetadata(getApplication(), song)
                        if (enriched != song) {
                            val updated = _songs.value.map { if (it.id == song.id) enriched else it }
                            _songs.value = updated
                            audioEngine.updatePlaylist(updated)
                            prefs.savedFolderUri?.let { uriStr ->
                                TrackCache.saveCachedTracks(
                                    getApplication(),
                                    uriStr,
                                    updated.filter { !it.isBuiltIn && !it.id.startsWith("mediastore_") }
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Keep fast values
                    }
                }
            }
    }

    fun togglePlayPause() {
        audioEngine.togglePlayPause()
        audioEngine.currentSong.value?.let {
            prefs.lastSongId = it.id
        }
    }

    fun playNext() {
        audioEngine.playNext()
        audioEngine.currentSong.value?.let {
            prefs.lastSongId = it.id
        }
    }

    fun playPrevious() {
        audioEngine.playPrevious()
        audioEngine.currentSong.value?.let {
            prefs.lastSongId = it.id
        }
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        val enabled = audioEngine.toggleShuffle()
        prefs.shuffleEnabled = enabled
    }

    fun cycleRepeatMode() {
        val mode = audioEngine.cycleRepeatMode()
        prefs.repeatMode = mode
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        _visualizerMode.value = mode
    }

    fun setSkinTheme(skin: PlayerSkinTheme) {
        _currentSkin.value = skin
        prefs.selectedThemeId = skin.id
    }

    fun setAutoPlayOnStart(enabled: Boolean) {
        _autoPlayOnStart.value = enabled
        prefs.autoPlayOnStart = enabled
    }

    fun setLockScreenPlayerEnabled(enabled: Boolean) {
        _lockScreenPlayerEnabled.value = enabled
        prefs.lockScreenPlayerEnabled = enabled
    }

    fun setWakeScreenOnTrackChange(enabled: Boolean) {
        _wakeScreenOnTrackChange.value = enabled
        prefs.wakeScreenOnTrackChange = enabled
    }

    fun launchLockScreenPlayerPreview() {
        com.example.ui.lockscreen.LockScreenPlayerActivity.launch(getApplication())
    }

    fun setEqualizerBand(bandIndex: Short, dbLevel: Int) {
        audioEngine.setBandLevel(bandIndex, dbLevel)
        val levels = audioEngine.equalizerBands.value.map { it.levelDb }
        prefs.saveBandLevels(levels)
        prefs.equalizerPreset = "Custom"
    }

    fun applyEqualizerPreset(presetName: String) {
        audioEngine.applyPreset(presetName)
        val levels = audioEngine.equalizerBands.value.map { it.levelDb }
        prefs.saveBandLevels(levels)
        prefs.equalizerPreset = presetName
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
