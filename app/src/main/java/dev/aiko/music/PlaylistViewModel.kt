package dev.aiko.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.aiko.music.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

data class PlaylistsUi(val lists: List<Playlist> = emptyList(), val loading: Boolean = true, val saving: Boolean = false,
    val loadError: Boolean = false, val error: String? = null)

class PlaylistViewModel(app: Application) : AndroidViewModel(app) {
    private val store = PlaylistStore(app)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(PlaylistsUi())
    val state = _state.asStateFlow()
    private val prefs = app.getSharedPreferences("playlists_ui", 0)
    val activeId = MutableStateFlow(prefs.getString("active", null))
    init { reload() }
    fun reload() {
        viewModelScope.launch { mutex.withLock {
            _state.value = _state.value.copy(loading = true, error = null)
            try { _state.value = PlaylistsUi(lists = withContext(Dispatchers.IO) { store.read() }, loading = false) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.value = _state.value.copy(loading = false, loadError = true,
                error = "No se pudieron leer tus playlists. No se ha sobrescrito el archivo guardado.") }
        } }
    }
    private fun update(change: (List<Playlist>) -> List<Playlist>, complete: () -> Unit = {}) {
        viewModelScope.launch { mutex.withLock {
            if (_state.value.loadError || _state.value.loading) return@withLock
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                val next = change(_state.value.lists)
                withContext(Dispatchers.IO) { store.write(next) }
                _state.value = PlaylistsUi(next, loading = false)
                complete()
            } catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) {
                _state.value = _state.value.copy(saving = false, error = e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = "No se pudo guardar. Tus playlists anteriores siguen disponibles.")
            }
        } }
    }
    fun create(name: String, complete: (String) -> Unit) {
        val id = UUID.randomUUID().toString()
        update({ lists -> lists + Playlist(id, PlaylistNames.validate(name, lists)) }, { complete(id) })
    }
    fun rename(id: String, name: String, complete: () -> Unit) = update({ lists ->
        val valid = PlaylistNames.validate(name, lists, id)
        lists.map { if (it.id == id) it.copy(name = valid) else it }
    }, complete)
    fun delete(id: String, complete: () -> Unit) = update({ it.filterNot { list -> list.id == id } }, {
        if (activeId.value == id) markPlaying(null)
        complete()
    })
    fun add(id: String, uris: List<String>, complete: () -> Unit) = update({ lists -> lists.map { if (it.id == id) it.add(uris) else it } }, complete)
    fun remove(id: String, entryId: String) = update({ lists -> lists.map { if (it.id == id) it.remove(entryId) else it } })
    fun move(id: String, entryId: String, direction: Int) = update({ lists -> lists.map { if (it.id == id) it.move(entryId, direction) else it } })
    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun markPlaying(id: String?) { activeId.value = id; prefs.edit().putString("active", id).apply() }
}
