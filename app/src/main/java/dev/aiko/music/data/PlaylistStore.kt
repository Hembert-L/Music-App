package dev.aiko.music.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Private, versioned, atomic storage. Never writes to MediaStore or to audio files. */
class PlaylistStore(context: Context, fileName: String = "playlists.json") {
    private val file = AtomicFile(File(context.filesDir, fileName))

    @Synchronized fun read(): List<Playlist> {
        val bytes = try { file.readFully() } catch (e: java.io.FileNotFoundException) { return emptyList() }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        check(root.getInt("version") == 1) { "Unsupported playlist storage version" }
        val lists = root.getJSONArray("playlists")
        return (0 until lists.length()).map { i ->
            val item = lists.getJSONObject(i)
            val entries = item.getJSONArray("entries")
            Playlist(item.getString("id"), item.getString("name"), (0 until entries.length()).map { n ->
                entries.getJSONObject(n).let { PlaylistEntry(it.getString("id"), it.getString("uri")) }
            })
        }.also { playlists ->
            check(playlists.map(Playlist::id).distinct().size == playlists.size)
            playlists.forEach { check(it.entries.map(PlaylistEntry::id).distinct().size == it.entries.size) }
        }
    }

    @Synchronized fun write(playlists: List<Playlist>) {
        val lists = JSONArray()
        playlists.forEach { playlist ->
            val entries = JSONArray()
            playlist.entries.forEach { entries.put(JSONObject().put("id", it.id).put("uri", it.uri)) }
            lists.put(JSONObject().put("id", playlist.id).put("name", playlist.name).put("entries", entries))
        }
        val bytes = JSONObject().put("version", 1).put("playlists", lists).toString().toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
}
