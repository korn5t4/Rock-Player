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
import com.example.ui.theme.PlayerSkinTheme
import com.example.ui.theme.PlayerSkins
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
    val audioEngine = RockAudioEngine(application)

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

    // Remembered Folder State
    private val _rememberedFolderUri = MutableStateFlow<String?>(prefs.savedFolderUri)
    val rememberedFolderUri: StateFlow<String?> = _rememberedFolderUri.asStateFlow()

    private val _rememberedFolderName = MutableStateFlow<String?>(prefs.savedFolderName)
    val rememberedFolderName: StateFlow<String?> = _rememberedFolderName.asStateFlow()

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

                // 2. Also try device media store
                try {
                    val mediaStoreSongs = MusicScanner.scanDeviceMediaStore(getApplication())
                    currentList.addAll(mediaStoreSongs)
                } catch (e: Exception) {}

                // 3. Automatically restore & scan REMEMBERED FOLDER if previously selected!
                val savedFolderUriStr = prefs.savedFolderUri
                if (!savedFolderUriStr.isNullOrBlank()) {
                    try {
                        val treeUri = Uri.parse(savedFolderUriStr)
                        val folderSongs = MusicScanner.scanDocumentTreeUri(getApplication(), treeUri)
                        if (folderSongs.isNotEmpty()) {
                            val existingIds = currentList.map { it.id }.toSet()
                            val newTracks = folderSongs.filter { it.id !in existingIds }
                            currentList.addAll(newTracks)
                            val folderName = prefs.savedFolderName ?: MusicScanner.getFolderDisplayName(getApplication(), treeUri)
                            _rememberedFolderName.value = folderName
                            _statusMessage.value = "Loaded ${newTracks.size} tracks from remembered folder '$folderName'"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                _songs.value = currentList
                audioEngine.setPlaylist(currentList, startIndex = 0, startPlaying = false)

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
                    val index = if (lastId != null) currentList.indexOfFirst { it.id == lastId }.coerceAtLeast(0) else 0
                    audioEngine.playSongAtIndex(index, autoStart = true)
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
            _statusMessage.value = "Scanning folder and subfolders..."
            try {
                // Ensure persistable permission is granted
                try {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(
                        treeUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val folderName = MusicScanner.getFolderDisplayName(getApplication(), treeUri)

                // Persist folder in preferences so app remembers it on restart!
                prefs.savedFolderUri = treeUri.toString()
                prefs.savedFolderName = folderName
                _rememberedFolderUri.value = treeUri.toString()
                _rememberedFolderName.value = folderName

                val scanned = MusicScanner.scanDocumentTreeUri(getApplication(), treeUri)
                if (scanned.isNotEmpty()) {
                    val existing = _songs.value.toMutableList()
                    val existingIds = existing.map { it.id }.toSet()
                    val newTracks = scanned.filter { it.id !in existingIds }
                    existing.addAll(newTracks)
                    _songs.value = existing
                    audioEngine.setPlaylist(existing, startIndex = 0, startPlaying = false)
                    _statusMessage.value = "Remembered '$folderName' • Added ${newTracks.size} tracks!"
                } else {
                    _statusMessage.value = "Remembered '$folderName' (no audio files found inside)."
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
        prefs.clearSavedFolder()
        _rememberedFolderUri.value = null
        _rememberedFolderName.value = null
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
        val index = _songs.value.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            audioEngine.playSongAtIndex(index, autoStart = true)
            prefs.lastSongId = song.id
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
