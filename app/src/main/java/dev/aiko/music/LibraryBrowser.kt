package dev.aiko.music

import dev.aiko.music.data.Song
import dev.aiko.music.data.searchKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

enum class LibraryOrder(val label: String) {
    NEWEST("Más recientes"), OLDEST("Más antiguos"), TITLE("A–Z")
}

data class LibraryFilter(val query: String = "", val favoritesOnly: Boolean = false, val order: LibraryOrder = LibraryOrder.NEWEST)
data class LibraryResults(val songs: List<Song> = emptyList(), val filter: LibraryFilter? = null, val source: List<Song>? = null)
data class LibraryPages(val songs: List<Song> = emptyList(), val favorites: List<Song> = emptyList(),
    val query: String = "", val order: LibraryOrder = LibraryOrder.NEWEST, val source: List<Song>? = null)
private data class LibraryQuery(val query: String, val order: LibraryOrder)
private data class IndexedSong(val song: Song, val search: String)
private data class LibraryIndex(val source: List<Song>, val entries: List<IndexedSong>)
private data class FilterInput(val index: LibraryIndex, val query: LibraryQuery, val favorites: Set<String>)

/** Lives with MusicViewModel: navigating away never discards or rebuilds the current results. */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryBrowser(scope: CoroutineScope, library: StateFlow<LibraryUi>, favorites: StateFlow<Set<String>>, initialOrder: LibraryOrder) {
    private val filter = MutableStateFlow(LibraryFilter(order = initialOrder))
    private val index = library.map { it.songs }.distinctUntilChanged().mapLatest { songs ->
        withContext(Dispatchers.Default) {
            LibraryIndex(songs, songs.map { IndexedSong(it, "${it.title} ${it.artist} ${it.album}".searchKey()) })
        }
    }
    // Eager sharing retains the last snapshot even while no library composable is collecting.
    // A scan with unchanged songs, playback progress and navigation do not invalidate the index.
    private val query = filter.map { LibraryQuery(it.query, it.order) }.distinctUntilChanged()
    val pages: StateFlow<LibraryPages> = combine(index, query, favorites, ::FilterInput).mapLatest { input ->
        if (input.query.query.isNotBlank()) delay(100)
        withContext(Dispatchers.Default) {
            val words = input.query.query.searchKey().split("\\s+".toRegex()).filter(String::isNotEmpty)
            val songs = input.index.entries.asSequence().filter {
                words.all(it.search::contains)
            }.map(IndexedSong::song).toList().let { filtered ->
                when (input.query.order) {
                    LibraryOrder.NEWEST -> filtered.sortedWith(compareByDescending<Song> { it.added }.thenByDescending { it.id })
                    LibraryOrder.OLDEST -> filtered.sortedWith(compareBy<Song> { it.added }.thenBy { it.id })
                    LibraryOrder.TITLE -> filtered // MediaStore already returns title order.
                }
            }
            LibraryPages(songs, songs.filter { it.uri.toString() in input.favorites }, input.query.query, input.query.order, input.index.source)
        }
    }.stateIn(scope, SharingStarted.Eagerly, LibraryPages(order = initialOrder))
    // Selection changes only this cheap projection. Both neighboring lists are already ready.
    val results: StateFlow<LibraryResults> = combine(pages, filter) { pages, selection ->
        LibraryResults(if (selection.favoritesOnly) pages.favorites else pages.songs,
            LibraryFilter(pages.query, selection.favoritesOnly, pages.order), pages.source)
    }.stateIn(scope, SharingStarted.Eagerly, LibraryResults())

    fun setFilter(query: String, favoritesOnly: Boolean, order: LibraryOrder) {
        filter.value = LibraryFilter(query, favoritesOnly, order)
    }
}
