package dev.aiko.music.data

import java.util.UUID

data class PlaylistEntry(val id: String, val uri: String)
data class Playlist(val id: String, val name: String, val entries: List<PlaylistEntry> = emptyList()) {
    fun add(uris: List<String>): Playlist {
        val existing = entries.map(PlaylistEntry::uri).toSet()
        return copy(entries = entries + uris.distinct().filter { it !in existing }
            .map { PlaylistEntry(UUID.randomUUID().toString(), it) })
    }
    fun move(entryId: String, direction: Int): Playlist {
        val from = entries.indexOfFirst { it.id == entryId }
        val to = from + direction
        if (from < 0 || to !in entries.indices) return this
        return copy(entries = entries.toMutableList().apply { add(to, removeAt(from)) })
    }
    fun remove(entryId: String) = copy(entries = entries.filterNot { it.id == entryId })
    fun resolve(songsByUri: Map<String, Song>): List<Song> = entries.mapNotNull { songsByUri[it.uri] }
}

object PlaylistNames {
    const val MAX_LENGTH = 60
    fun validate(raw: String, playlists: List<Playlist>, exceptId: String? = null): String {
        val name = raw.trim()
        require(name.isNotEmpty()) { "Escribe un nombre para la playlist." }
        require(name.length <= MAX_LENGTH) { "El nombre puede tener hasta $MAX_LENGTH caracteres." }
        require(playlists.none { it.id != exceptId && it.name.searchKey() == name.searchKey() }) { "Ya existe una playlist con ese nombre." }
        return name
    }
}
