package dev.aiko.music

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import dev.aiko.music.data.*
import dev.aiko.music.playback.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

data class PlaybackUi(
    val connected: Boolean = false, val item: MediaItem? = null, val playing: Boolean = false,
    val loading: Boolean = false, val position: Long = 0, val duration: Long = 0,
    val queue: List<MediaItem> = emptyList(), val index: Int = -1, val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF, val error: String? = null, val titleToken: Long = 0
)
data class LibraryUi(val songs: List<Song> = emptyList(), val loading: Boolean = false, val error: String? = null,
    val refreshing: Boolean = false)
data class LibraryPlayback(val connected: Boolean = false, val activeId: String? = null, val error: String? = null)
data class PlaylistPlayback(val connected: Boolean = false, val itemId: String? = null, val queue: List<String> = emptyList())

@UnstableApi
class MusicViewModel(app: Application) : AndroidViewModel(app) {
    val permission: String get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    fun hasPermission() = ContextCompat.checkSelfPermission(getApplication(), permission) == PackageManager.PERMISSION_GRANTED
    private val _library = MutableStateFlow(LibraryUi())
    val library = _library.asStateFlow()
    private val _playback = MutableStateFlow(PlaybackUi())
    val playback = _playback.asStateFlow()
    val libraryPlayback = playback.map { LibraryPlayback(it.connected, it.item?.mediaId, it.error) }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, LibraryPlayback())
    val playlistPlayback = playback.map { PlaylistPlayback(it.connected, it.item?.mediaId, it.queue.map { item -> item.mediaId }) }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaylistPlayback())
    private val prefs = app.getSharedPreferences("music", 0)
    val favorites = MutableStateFlow(prefs.getStringSet("favorites", emptySet())!!.toSet())
    val libraryBrowser = LibraryBrowser(viewModelScope, library, favorites,
        LibraryOrder.entries.firstOrNull { it.name == app.getSharedPreferences("library_ui", 0).getString("sort_order", null) } ?: LibraryOrder.NEWEST)
    val sound = MutableStateFlow(PlaybackService.readSound(app))
    private var controller: MediaController? = null
    val miniTitleMotion = MiniTitleMotion()
    private var titleToken = 0L
    private var titlePending = false
    private var observedMediaId: String? = null
    private var closingPlayback = false
    private var loadJob: Job? = null
    private val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) titlePending = true
        }
        override fun onEvents(player: Player, events: Player.Events) { sync() }
        override fun onPlayerError(error: PlaybackException) {
            _playback.value = _playback.value.copy(error = "No se pudo reproducir este archivo. Comprueba que siga en el dispositivo o elige otra canción.")
        }
    }
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { refresh() }
    }
    init {
        future.addListener({
            runCatching { future.get() }.onSuccess {
                controller = it
                it.addListener(listener)
                sync()
            }.onFailure { _playback.value = _playback.value.copy(error = "No se pudo conectar el reproductor. Cierra y vuelve a abrir Music.") }
        }, ContextCompat.getMainExecutor(app))
        app.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        viewModelScope.launch {
            while (isActive) { sync(); delay(400) }
        }
        refresh()
    }
    fun refresh() = loadLibrary(manual = false)
    fun refreshFromGesture() {
        if (loadJob?.isActive != true) loadLibrary(manual = true)
    }
    private fun loadLibrary(manual: Boolean) {
        val showIndicator = manual || _library.value.refreshing
        loadJob?.cancel()
        if (!hasPermission()) { _library.value = LibraryUi(); return }
        loadJob = viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            _library.value = _library.value.copy(loading = true, error = null, refreshing = showIndicator)
            val result = try {
                val songs = withContext(Dispatchers.IO) { Library.read(getApplication()) }
                LibraryUi(songs)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { LibraryUi(error = "No se pudo leer la biblioteca. Revisa el permiso de música y vuelve a actualizar.") }
            // Keep a fast manual scan perceptible, without delaying automatic/lifecycle refreshes.
            if (showIndicator) delay((450L - (SystemClock.elapsedRealtime() - started)).coerceAtLeast(0L))
            _library.value = result
        }
    }
    private fun sync() {
        val p = controller ?: return
        if (closingPlayback) {
            if (p.mediaItemCount == 0) closingPlayback = false
            else { _playback.value = PlaybackUi(connected = true); return }
        }
        val id = p.currentMediaItem?.mediaId
        if (id != observedMediaId) { observedMediaId = id; titlePending = id != null }
        if (titlePending && p.isPlaying) { titleToken++; titlePending = false }
        _playback.value = PlaybackUi(true, p.currentMediaItem, p.isPlaying,
            p.playbackState == Player.STATE_BUFFERING, p.currentPosition.coerceAtLeast(0),
            p.duration.takeIf { it > 0 } ?: 0, (0 until p.mediaItemCount).map(p::getMediaItemAt),
            p.currentMediaItemIndex, p.shuffleModeEnabled, p.repeatMode,
            if (p.playerError != null) _playback.value.error else null, if (titlePending || id == null) 0 else titleToken)
    }
    fun play(songs: List<Song>, song: Song, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        controller?.run {
            titlePending = true
            shuffleModeEnabled = shuffle
            val start = if (shuffle) songs.indices.random() else songs.indexOf(song).coerceAtLeast(0)
            setMediaItems(songs.map(Song::mediaItem), start, 0)
            prepare(); play()
        }
    }
    fun togglePlay() { controller?.run { if (playWhenReady) pause() else { prepare(); play() } } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun closePlayback() {
        val p = controller ?: return
        closingPlayback = true
        _playback.value = PlaybackUi(connected = true)
        val result = p.sendCustomCommand(SessionCommand(PlaybackService.CLOSE_COMMAND, android.os.Bundle.EMPTY), android.os.Bundle.EMPTY)
        result.addListener({
            runCatching { result.get() }.onSuccess {
                if (it.resultCode != SessionResult.RESULT_SUCCESS) closingPlayback = false
            }.onFailure { closingPlayback = false }
            sync()
        }, ContextCompat.getMainExecutor(getApplication()))
    }
    fun seek(position: Long) {
        controller?.run {
            val target = position.coerceAtLeast(0)
            if (isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) seekTo(target)
            else if (currentMediaItemIndex >= 0 && isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM)) {
                // Restored, unprepared items expose indexed seeking before their timeline is seekable.
                // Keep the same item and paused state; prepare/play still requires the user's action.
                seekTo(currentMediaItemIndex, target)
            }
        }
    }
    fun shuffle() { controller?.run { shuffleModeEnabled = !shuffleModeEnabled } }
    fun repeat() { controller?.run { repeatMode = when (repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF } } }
    fun enqueue(song: Song, next: Boolean) {
        controller?.run {
            addMediaItem(if (next) (currentMediaItemIndex + 1).coerceAtLeast(0) else mediaItemCount, song.mediaItem())
        }
    }
    fun selectQueue(index: Int) { controller?.run { titlePending = true; seekToDefaultPosition(index); prepare(); play() } }
    fun remove(index: Int) { controller?.removeMediaItem(index) }
    fun move(index: Int, to: Int) { controller?.run { if (to in 0 until mediaItemCount) moveMediaItem(index, to) } }
    fun favorite(id: String) {
        favorites.value = favorites.value.toMutableSet().apply { if (!add(id)) remove(id) }
        prefs.edit().putStringSet("favorites", favorites.value).apply()
    }
    fun setSound(settings: SoundSettings) {
        val p = controller ?: return
        val safe = settings.safe()
        val result = p.sendCustomCommand(SessionCommand(PlaybackService.SOUND_COMMAND, android.os.Bundle.EMPTY), PlaybackService.soundBundle(safe))
        result.addListener({
            runCatching { result.get() }.onSuccess {
                if (it.resultCode == SessionResult.RESULT_SUCCESS) sound.value = safe
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }
    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(observer)
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
    }
}
