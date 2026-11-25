package dev.aiko.music

import android.content.Context
import android.util.AtomicFile
import dev.aiko.music.data.*
import java.io.File
import java.util.UUID

object PlaylistDeviceChecks {
    fun run(context: Context, songs: List<Song>, pass: (String) -> Unit) {
        val name = "playlist-check-${UUID.randomUUID()}.json"
        val file = File(context.filesDir, name)
        try {
            val store = PlaylistStore(context, name)
            check(store.read().isEmpty())
            val original = Playlist("test", "Verificación", emptyList()).add(songs.take(3).map { it.uri.toString() })
            store.write(listOf(original))
            check(PlaylistStore(context, name).read() == listOf(original))
            pass("Playlists: atomic storage survives a fresh repository instance")
            val edited = original.copy(name = "Verificación renombrada").move(original.entries[2].id, -1).remove(original.entries[0].id)
            store.write(listOf(edited))
            val reloaded = PlaylistStore(context, name).read().single()
            check(reloaded == edited)
            check(reloaded.resolve(songs.associateBy { it.uri.toString() }).map { it.uri.toString() } == reloaded.entries.map(PlaylistEntry::uri))
            check(reloaded.resolve(emptyMap()).isEmpty() && reloaded.entries.size == 2)
            pass("Playlists: rename, manual order, removal and missing-file references persist")
            val atomic = AtomicFile(file)
            val interrupted = atomic.startWrite()
            interrupted.write("incomplete".toByteArray())
            atomic.failWrite(interrupted)
            check(store.read() == listOf(edited))
            store.write(emptyList())
            check(PlaylistStore(context, name).read().isEmpty())
            pass("Playlists: interrupted write preserves data; deletion persists")
        } finally {
            file.delete()
            File(context.filesDir, "$name.new").delete()
            File(context.filesDir, "$name.bak").delete()
        }
    }
}
