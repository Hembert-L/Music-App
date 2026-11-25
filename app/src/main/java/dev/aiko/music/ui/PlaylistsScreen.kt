@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.aiko.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aiko.music.MusicViewModel
import dev.aiko.music.PlaylistViewModel
import dev.aiko.music.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun PlaylistsScreen(music: MusicViewModel, library: List<Song>, granted: Boolean, requestPermission: () -> Unit,
    modifier: Modifier = Modifier, playlists: PlaylistViewModel = viewModel(), searchQuery: String = "",
    changeSearch: (String) -> Unit = {}, createRequest: Int = 0, active: Boolean = true) {
    val state by playlists.state.collectAsStateWithLifecycle()
    val activeId by playlists.activeId.collectAsStateWithLifecycle()
    val playback by music.playlistPlayback.collectAsStateWithLifecycle()
    val byUri = remember(library) { library.associateBy { it.uri.toString() } }
    val query = searchQuery
    var handledCreate by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var create by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var pickerId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(createRequest) {
        if (createRequest != handledCreate) { handledCreate = createRequest; playlists.clearError(); create = true }
    }
    LaunchedEffect(searchQuery) { if (searchQuery.isNotBlank()) selectedId = null }
    val selected = state.lists.firstOrNull { it.id == selectedId }
    BackHandler(active && selectedId != null && !create && renameId == null && deleteId == null && pickerId == null) { selectedId = null }
    fun play(list: Playlist, song: Song? = null, shuffle: Boolean = false) {
        val songs = list.resolve(byUri)
        if (songs.isNotEmpty() && playback.connected) {
            music.play(songs, song ?: songs.first(), shuffle)
            playlists.markPlaying(list.id)
        }
    }
    fun active(list: Playlist): Boolean = activeId == list.id && playback.itemId != null &&
        list.resolve(byUri).map { it.uri.toString() }.let { it.isNotEmpty() && it == playback.queue }

    Column(modifier) {
        if (selected == null) {
            val filtered = remember(state.lists, query) { state.lists.filter { query.searchKey() in it.name.searchKey() } }
            when {
                state.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.loadError -> PlaylistEmpty("No pudimos leer tus playlists", state.error ?: "Vuelve a intentarlo para recuperar tus listas.", "Reintentar", playlists::reload, Modifier.weight(1f))
                filtered.isEmpty() -> PlaylistEmpty(if (query.isNotBlank()) "Sin coincidencias" else "Tu música, en tus listas",
                    if (query.isNotBlank()) "No hay playlists con ese nombre." else "Crea una playlist y añade las canciones que quieras escuchar juntas.",
                    if (query.isNotBlank()) "Borrar búsqueda" else "Nueva playlist", { if (query.isNotBlank()) changeSearch("") else { playlists.clearError(); create = true } }, Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = MusicStyle.PageMargin, vertical = 12.dp)) {
                    items(filtered, key = Playlist::id, contentType = { "playlist" }) { list ->
                        val available = list.resolve(byUri)
                        PlaylistCard(list, available, active(list), playback.connected, { selectedId = list.id }, { play(list) },
                            { playlists.clearError(); renameId = list.id }, { selectedId = list.id; pickerId = list.id }, { deleteId = list.id })
                    }
                }
            }
        } else {
            val available = selected.resolve(byUri)
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedId = null }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver a playlists") }
                Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                    Text(selected.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(playlistCount(selected, available.size), color = MusicStyle.Muted, style = MaterialTheme.typography.bodySmall)
                }
                SmallPlaylistPlayButton("Reproducir playlist ${selected.name}", available.isNotEmpty() && playback.connected) { play(selected) }
                IconButton(onClick = { pickerId = selected.id }, enabled = !state.saving) { Icon(Icons.Rounded.Add, "Añadir canciones") }
            }
            if (selected.entries.isEmpty()) PlaylistEmpty("Esta playlist está esperando", "Añade canciones de tu biblioteca. El orden que elijas se conservará.",
                "Añadir canciones", { pickerId = selected.id }, Modifier.weight(1f))
            else {
                Row(Modifier.fillMaxWidth().padding(horizontal = MusicStyle.PageMargin), verticalAlignment = Alignment.CenterVertically) {
                    Text("Orden de la playlist", style = MaterialTheme.typography.bodySmall, color = MusicStyle.Muted, modifier = Modifier.weight(1f))
                    IconButton(onClick = { play(selected, shuffle = true) }, enabled = playback.connected && available.isNotEmpty()) { Icon(Icons.Rounded.Shuffle, "Reproducir playlist al azar") }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = MusicStyle.PageMargin, vertical = 4.dp)) {
                    items(selected.entries, key = PlaylistEntry::id) { entry ->
                        PlaylistMemberRow(entry, byUri[entry.uri], entry.uri == playback.itemId, !state.saving,
                            selected.entries.indexOf(entry), selected.entries.lastIndex,
                            { byUri[entry.uri]?.let { play(selected, it) } },
                            { playlists.move(selected.id, entry.id, it) }, { playlists.remove(selected.id, entry.id) })
                    }
                }
            }
        }
        if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.error != null && !create && renameId == null && !state.loadError) Text(state.error!!,
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
    }

    if (create || renameId != null) {
        val renaming = state.lists.firstOrNull { it.id == renameId }
        key(create, renameId) {
            PlaylistNameDialog(renaming?.name ?: "", create, state.saving, state.error,
                { create = false; renameId = null; playlists.clearError() }, { name ->
                    if (create) playlists.create(name) { id -> create = false; selectedId = id; pickerId = id }
                    else renaming?.let { playlists.rename(it.id, name) { renameId = null } }
                })
        }
    }
    deleteId?.let { id ->
        val deleting = state.lists.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { if (!state.saving) deleteId = null }, title = { Text("Eliminar playlist") },
            text = { Text("¿Eliminar «${deleting?.name ?: ""}»? Las canciones del teléfono se conservan.") },
            confirmButton = { TextButton(enabled = !state.saving, onClick = { playlists.delete(id) { deleteId = null; if (selectedId == id) selectedId = null } }) { Text("Eliminar") } },
            dismissButton = { TextButton(enabled = !state.saving, onClick = { deleteId = null }) { Text("Cancelar") } })
    }
    pickerId?.let { id ->
        val list = state.lists.firstOrNull { it.id == id }
        SongPicker(library, list?.entries?.map(PlaylistEntry::uri)?.toSet() ?: emptySet(), granted, requestPermission,
            state.saving, state.error, { if (!state.saving) pickerId = null }, { uris -> playlists.add(id, uris) { pickerId = null } })
    }
}

@Composable
private fun PlaylistCard(list: Playlist, songs: List<Song>, active: Boolean, connected: Boolean,
    open: () -> Unit, play: () -> Unit, rename: () -> Unit, add: () -> Unit, delete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(16.dp))
        .background(if (active) MusicStyle.SelectedGradient else MusicStyle.CardGradient)
        .border(1.dp, if (active) MusicStyle.Violet.copy(alpha = .5f) else MusicStyle.Divider.copy(alpha = .55f), RoundedCornerShape(16.dp))
        .clickable(role = Role.Button, onClick = open).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        MusicArtwork(songs.firstOrNull()?.artwork, list.name, Modifier.size(52.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(list.name, style = MaterialTheme.typography.titleMedium, color = if (active) MusicStyle.AccentText else MusicStyle.Text,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(playlistCount(list, songs.size), style = MaterialTheme.typography.bodySmall, color = MusicStyle.Muted, maxLines = 2)
        }
        SmallPlaylistPlayButton("Reproducir playlist ${list.name}", connected && songs.isNotEmpty(), play)
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Opciones de playlist ${list.name}", tint = MusicStyle.Muted, modifier = Modifier.size(22.dp)) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Renombrar") }, onClick = { menu = false; rename() })
                DropdownMenuItem(text = { Text("Añadir canciones") }, onClick = { menu = false; add() })
                DropdownMenuItem(text = { Text("Editar canciones y orden") }, onClick = { menu = false; open() })
                DropdownMenuItem(text = { Text("Eliminar playlist") }, onClick = { menu = false; delete() })
            }
        }
    }
}

@Composable
private fun SmallPlaylistPlayButton(description: String, enabled: Boolean, play: () -> Unit) {
    Box(Modifier.size(48.dp).clickable(enabled = enabled, role = Role.Button, onClick = play)
        .semantics {
            contentDescription = description
            if (!enabled) { disabled(); stateDescription = "Sin canciones disponibles" }
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (enabled) MusicStyle.Accent else MusicStyle.CardGradient), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, null, tint = if (enabled) MusicStyle.Text else MusicStyle.Muted.copy(alpha = .5f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PlaylistMemberRow(entry: PlaylistEntry, song: Song?, active: Boolean, editable: Boolean,
    index: Int, lastIndex: Int, play: () -> Unit, move: (Int) -> Unit, remove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).clickable(enabled = song != null, onClick = play), verticalAlignment = Alignment.CenterVertically) {
            MusicArtwork(song?.artwork, song?.title ?: "Archivo no disponible", Modifier.size(50.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(song?.title ?: "Archivo no disponible", style = MaterialTheme.typography.titleMedium,
                    color = if (active) MusicStyle.AccentText else MusicStyle.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song?.artist ?: "La referencia se conserva en la playlist", style = MaterialTheme.typography.bodySmall, color = MusicStyle.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Editar canción ${index + 1}") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Subir una posición") }, enabled = editable && index > 0, onClick = { menu = false; move(-1) })
                    DropdownMenuItem(text = { Text("Bajar una posición") }, enabled = editable && index < lastIndex, onClick = { menu = false; move(1) })
                    DropdownMenuItem(text = { Text("Quitar de esta playlist") }, enabled = editable, onClick = { menu = false; remove() })
                }
            }
        }
        HorizontalDivider(Modifier.padding(start = 62.dp), color = MusicStyle.Divider, thickness = .5.dp)
    }
}

@Composable
private fun PlaylistNameDialog(initial: String, creating: Boolean, busy: Boolean, error: String?, dismiss: () -> Unit, save: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text(if (creating) "Nueva playlist" else "Renombrar playlist") },
        text = { Column {
            OutlinedTextField(name, { if (it.length <= PlaylistNames.MAX_LENGTH) name = it }, label = { Text("Nombre") }, singleLine = true, enabled = !busy)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
        } }, confirmButton = { TextButton(enabled = name.isNotBlank() && !busy, onClick = { save(name) }) { Text(if (creating) "Crear" else "Guardar") } },
        dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Cancelar") } })
}

@Composable
private fun SongPicker(library: List<Song>, existing: Set<String>, granted: Boolean, permission: () -> Unit,
    busy: Boolean, error: String?, dismiss: () -> Unit, add: (List<String>) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val candidates by produceState(emptyList<Song>(), library, existing, query) {
        value = withContext(Dispatchers.Default) { library.filterNot { it.uri.toString() in existing }.search(query) }
    }
    Dialog(onDismissRequest = { if (!busy) dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.95f).fillMaxHeight(.90f), color = MusicStyle.Secondary, shape = MusicStyle.CardShape) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Añadir canciones", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = dismiss, enabled = !busy) { Icon(Icons.Rounded.Close, "Cerrar selección") }
                }
                MusicSearchField(query, { query = it })
                Text("${selected.size} seleccionadas", color = MusicStyle.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
                when {
                    !granted -> PlaylistEmpty("Permite acceder a tu música", "Solo se añadirán referencias a los archivos de tu teléfono.", "Dar permiso", permission, Modifier.weight(1f))
                    candidates.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (query.isNotBlank()) "Sin coincidencias" else "No hay más canciones para añadir.", color = MusicStyle.Muted)
                    }
                    else -> LazyColumn(Modifier.weight(1f)) {
                        items(candidates, key = Song::id) { song ->
                            val uri = song.uri.toString()
                            val checked = uri in selected
                            fun toggle() { selected = ArrayList(selected).apply { if (!remove(uri)) add(uri) } }
                            Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).clickable(enabled = !busy, onClick = ::toggle), verticalAlignment = Alignment.CenterVertically) {
                                MusicArtwork(song.artwork, song.title, Modifier.size(46.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MusicStyle.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Checkbox(checked, onCheckedChange = { toggle() }, enabled = !busy, modifier = Modifier.semantics { contentDescription = "Seleccionar ${song.title}" })
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = { add(selected.toList()) }, enabled = selected.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) { Text("Añadir ${selected.size} canciones") }
            }
        }
    }
}

@Composable
private fun PlaylistEmpty(title: String, text: String, action: String, click: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = MusicStyle.AccentText, modifier = Modifier.size(36.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MusicStyle.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
        TextButton(onClick = click) { Text(action) }
    }
}

private fun playlistCount(list: Playlist, available: Int): String {
    val missing = list.entries.size - available
    return "${list.entries.size} canciones" + if (missing > 0) " · $missing no disponibles" else ""
}
