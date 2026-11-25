package dev.aiko.music.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.aiko.music.LibraryOrder
import dev.aiko.music.LibraryPages
import dev.aiko.music.LibraryPlayback
import dev.aiko.music.LibraryUi
import dev.aiko.music.MusicViewModel
import dev.aiko.music.data.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private val categoryNames = listOf("Canciones", "Favoritos", "Playlists")
// Saving an integer target avoids restoring a half-dragged page after opening the full player.
private val categoryPagerSaver = Saver<PagerState, Int>(save = { it.targetPage },
    restore = { PagerState(currentPage = it.coerceIn(0, 2), pageCount = { 3 }) })

/** Playback position never invalidates these pages; adjacent filtered lists are retained by the VM. */
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun LibraryScreen(vm: MusicViewModel, granted: Boolean, asked: Boolean, requestPermission: () -> Unit,
    openSound: () -> Unit, modifier: Modifier = Modifier) {
    val library by vm.library.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val playback by vm.libraryPlayback.collectAsStateWithLifecycle()
    val pages by vm.libraryBrowser.pages.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var playlistQuery by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var createRequest by rememberSaveable { mutableIntStateOf(0) }
    val pager = rememberSaveable(saver = categoryPagerSaver) { PagerState(pageCount = { 3 }) }
    val tab by remember(pager) { derivedStateOf { pager.settledPage } }
    val context = LocalContext.current
    val libraryPrefs = remember(context) { context.getSharedPreferences("library_ui", android.content.Context.MODE_PRIVATE) }
    var order by remember {
        mutableStateOf(LibraryOrder.entries.firstOrNull { it.name == libraryPrefs.getString("sort_order", null) } ?: LibraryOrder.NEWEST)
    }
    LaunchedEffect(query, order, tab) { vm.libraryBrowser.setFilter(query, tab == 1, order) }
    val songList = rememberLazyListState()
    val favoriteList = rememberLazyListState()
    val playlistState = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    var selectionJob by remember { mutableStateOf<Job?>(null) }
    val motion = remember { spring<Float>(dampingRatio = 1f, stiffness = MusicStyle.LibraryPagerStiffness) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(pager) {
        snapshotFlow { pager.isScrollInProgress }.filter { it }.collect { keyboard?.hide() }
    }
    val originalConfiguration = LocalViewConfiguration.current
    val pagerConfiguration = remember(originalConfiguration) {
        object : ViewConfiguration by originalConfiguration {
            override val touchSlop = originalConfiguration.touchSlop * MusicStyle.LibraryPagerTouchSlop
        }
    }
    Column(modifier) {
        CompactLibraryHeader(tab, { page ->
            selectionJob?.cancel()
            selectionJob = scope.launch { pager.animateScrollToPage(page, animationSpec = motion) }
        }, searching, { searching = true }, {
            searching = false
            if (tab == 2) playlistQuery = "" else query = ""
        }, if (tab == 2) playlistQuery else query, { if (tab == 2) playlistQuery = it else query = it },
            openSound, { createRequest++ },
            pagePosition = { (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, 2f) })
        LibraryRefreshBox(library.refreshing, granted && !library.loading && !pager.isScrollInProgress,
            vm::refreshFromGesture, Modifier.fillMaxWidth().weight(1f)) {
          CompositionLocalProvider(LocalViewConfiguration provides pagerConfiguration) {
            HorizontalPager(pager, Modifier.fillMaxSize().semantics {
                contentDescription = "Categorías de biblioteca"
                stateDescription = if (pager.isScrollInProgress) "Cambiando categoría" else categoryNames[pager.settledPage]
            }, beyondViewportPageCount = 1, key = { categoryNames[it] },
                flingBehavior = PagerDefaults.flingBehavior(pager, pagerSnapDistance = PagerSnapDistance.atMost(1),
                    snapAnimationSpec = motion, snapPositionalThreshold = MusicStyle.LibraryPagerThreshold)) { page ->
                // The larger horizontal slop applies only to the pager, not to vertical lists or buttons.
                CompositionLocalProvider(LocalViewConfiguration provides originalConfiguration) {
                    if (page == 2) playlistState.SaveableStateProvider("playlists") {
                        PlaylistsScreen(vm, library.songs, granted, requestPermission, Modifier.fillMaxSize(),
                            searchQuery = playlistQuery, changeSearch = { playlistQuery = it },
                            createRequest = createRequest, active = tab == 2)
                    } else {
                        LibraryCategoryPage(library, pages, playback, favorites, page == 1,
                            if (page == 0) songList else favoriteList, order, { selected ->
                                order = selected
                                libraryPrefs.edit().putString("sort_order", selected.name).apply()
                            }, granted, asked, requestPermission, { query = "" }, vm)
                    }
                }
            }
          }
        }
        playback.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp)) }
        if (!playback.connected) Text("Conectando reproductor…", color = MusicStyle.Muted, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun LibraryCategoryPage(library: LibraryUi, pages: LibraryPages, playback: LibraryPlayback,
    favorites: Set<String>, favoritesOnly: Boolean, listState: LazyListState, order: LibraryOrder,
    changeOrder: (LibraryOrder) -> Unit, granted: Boolean, asked: Boolean, requestPermission: () -> Unit,
    clearQuery: () -> Unit, vm: MusicViewModel) {
    val context = LocalContext.current
    val songs = if (favoritesOnly) pages.favorites else pages.songs
    var orderMenu by remember { mutableStateOf(false) }
    val appliedFilter = "${pages.query}|${pages.order}"
    var previousFilter by rememberSaveable { mutableStateOf(appliedFilter) }
    LaunchedEffect(appliedFilter) {
        if (appliedFilter != previousFilter) { listState.scrollToItem(0); previousFilter = appliedFilter }
    }
    Column(Modifier.fillMaxSize()) {
        when {
            !granted -> LibraryEmptyState("Tu música empieza aquí", "Permite acceder a la música guardada en tu Android.",
                if (asked) "Revisar permiso" else "Elegir mi música", requestPermission, Modifier.weight(1f),
                if (asked) { { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) } } else null)
            (library.loading && library.songs.isEmpty() && !library.refreshing) || (library.songs.isNotEmpty() && pages.source != library.songs && songs.isEmpty()) ->
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MusicStyle.AccentText) }
            library.error != null -> LibraryEmptyState("No pudimos leer tu música", library.error!!, "Actualizar", vm::refresh, Modifier.weight(1f))
            songs.isEmpty() -> LibraryEmptyState(
                if (pages.query.isNotBlank()) "Sin coincidencias" else if (favoritesOnly) "Tus favoritas, más cerca" else "Todavía no hay canciones",
                if (pages.query.isNotBlank()) "Prueba con otro título, artista o álbum." else if (favoritesOnly) "Añade canciones a favoritos desde su menú o el reproductor." else "Copia música al teléfono y actualiza la biblioteca.",
                if (pages.query.isNotBlank()) "Borrar búsqueda" else "Actualizar", { if (pages.query.isNotBlank()) clearQuery() else vm.refresh() }, Modifier.weight(1f))
            else -> {
                Row(Modifier.fillMaxWidth().height(48.dp).padding(start = MusicStyle.PageMargin, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${songs.size} canciones", style = MaterialTheme.typography.bodySmall, color = MusicStyle.Muted, modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { orderMenu = true }, contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.semantics { contentDescription = "Ordenar canciones" }) {
                            Text(order.label, style = MaterialTheme.typography.bodySmall, color = MusicStyle.Muted)
                            Icon(Icons.Rounded.SwapVert, null, modifier = Modifier.size(16.dp), tint = MusicStyle.Muted)
                        }
                        DropdownMenu(orderMenu, { orderMenu = false }) {
                            LibraryOrder.entries.forEach { option ->
                                DropdownMenuItem(text = { Text(option.label) }, onClick = { changeOrder(option); orderMenu = false },
                                    trailingIcon = { if (order == option) Icon(Icons.Rounded.Check, "Seleccionado") })
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = MusicStyle.PageMargin, end = MusicStyle.PageMargin, bottom = 88.dp)) {
                        items(songs, key = Song::id, contentType = { "song" }) { song ->
                            SongRow(song, song.uri.toString() == playback.activeId, song.uri.toString() in favorites,
                                playback.connected, { vm.play(songs, song) }, { vm.favorite(song.uri.toString()) }, { vm.enqueue(song, it) })
                        }
                    }
                    GradientIconButton(Icons.Rounded.Shuffle, "Reproducir al azar", { vm.play(songs, songs.first(), true) },
                        Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp), enabled = playback.connected, floating = true, iconSize = 28)
                }
            }
        }
    }
}


@Composable
fun MusicSearchField(query: String, change: (String) -> Unit, modifier: Modifier = Modifier,
    placeholder: String = "Buscar canciones...", description: String = "Buscar canciones", visualHeight: Dp = 48.dp) {
    val keyboard = LocalSoftwareKeyboardController.current
    BasicTextField(value = query, onValueChange = change, singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MusicStyle.Text), cursorBrush = MusicStyle.Accent,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = description },
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Box(Modifier.fillMaxWidth().height(visualHeight).clip(MusicStyle.CapsuleShape).background(MusicStyle.CardGradient)
                  .border(1.dp, MusicStyle.Divider, MusicStyle.CapsuleShape))
              Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = MusicStyle.Muted, modifier = Modifier.padding(start = 16.dp, end = 12.dp).size(23.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(placeholder, color = MusicStyle.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    inner()
                }
                if (query.isNotEmpty()) IconButton(onClick = { change("") }, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Close, "Borrar búsqueda", tint = MusicStyle.Muted) }
                else Spacer(Modifier.width(16.dp))
              }
            }
        })
}

@Composable
private fun SongRow(song: Song, active: Boolean, favorite: Boolean, enabled: Boolean, play: () -> Unit,
    toggleFavorite: () -> Unit, enqueue: (Boolean) -> Unit) {
    var menu by remember(song.id) { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = MusicStyle.RowHeight)
            .background(if (active) MusicStyle.Purple.copy(alpha = .09f) else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = play), verticalAlignment = Alignment.CenterVertically) {
            MusicArtwork(song.artwork, song.title, Modifier.size(MusicStyle.ArtworkSize))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, color = if (active) MusicStyle.AccentText else MusicStyle.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MusicStyle.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            Text("%d:%02d".format(song.duration / 60000, song.duration / 1000 % 60), style = MaterialTheme.typography.bodySmall, color = MusicStyle.Muted)
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.MoreVert, "Opciones de ${song.title}", tint = MusicStyle.Text, modifier = Modifier.size(22.dp)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(if (favorite) "Quitar de favoritos" else "Añadir a favoritos") }, onClick = { toggleFavorite(); menu = false },
                        leadingIcon = { Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) })
                    DropdownMenuItem(text = { Text("Reproducir después") }, enabled = enabled, onClick = { enqueue(true); menu = false })
                    DropdownMenuItem(text = { Text("Añadir al final de la cola") }, enabled = enabled, onClick = { enqueue(false); menu = false })
                }
            }
        }
        HorizontalDivider(Modifier.padding(start = MusicStyle.ArtworkSize + 12.dp), thickness = .5.dp, color = MusicStyle.Divider)
    }
}

@Composable
private fun LibraryEmptyState(title: String, text: String, button: String, action: () -> Unit,
    modifier: Modifier, secondary: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.LibraryMusic, null, tint = MusicStyle.AccentText, modifier = Modifier.size(36.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        Text(text, color = MusicStyle.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
        Button(onClick = action) { Text(button) }
        secondary?.let { TextButton(onClick = it) { Text("Abrir ajustes de Android") } }
    }
}
