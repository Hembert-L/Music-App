package dev.aiko.music.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media as AudioMedia
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.text.Normalizer
import java.util.Locale

data class Song(
    val id: Long, val title: String, val artist: String, val album: String,
    val albumId: Long, val duration: Long, val added: Long, val uri: Uri
) {
    // Track-specific art avoids borrowing another song's image for an "unknown" album.
    val artwork: Uri get() = Uri.withAppendedPath(uri, "albumart")
    fun mediaItem(): MediaItem = MediaItem.Builder().setMediaId(uri.toString()).setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist)
            .setAlbumTitle(album).setArtworkUri(artwork).build()).build()
}

object Library {
    fun read(context: Context): List<Song> {
        val columns = arrayOf(AudioMedia._ID, AudioMedia.TITLE, AudioMedia.ARTIST, AudioMedia.ALBUM, AudioMedia.ALBUM_ID, AudioMedia.DURATION, AudioMedia.DATE_ADDED)
        return buildList {
            context.contentResolver.query(AudioMedia.EXTERNAL_CONTENT_URI, columns,
                "${AudioMedia.IS_MUSIC} != 0 AND ${AudioMedia.DURATION} > 0", null, "${AudioMedia.TITLE} COLLATE NOCASE ASC")?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    add(Song(id, c.getString(1)?.ifBlank { "Sin título" } ?: "Sin título",
                        c.getString(2)?.takeUnless { it == "<unknown>" } ?: "Artista desconocido",
                        c.getString(3)?.takeUnless { it == "<unknown>" } ?: "Sin álbum",
                        c.getLong(4), c.getLong(5), c.getLong(6), ContentUris.withAppendedId(AudioMedia.EXTERNAL_CONTENT_URI, id)))
                }
            }
        }
    }
}

fun String.searchKey(): String = Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "").trim()

fun List<Song>.search(query: String): List<Song> {
    val words = query.searchKey().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return if (words.isEmpty()) this else filter { song ->
        val text = "${song.title} ${song.artist} ${song.album}".searchKey()
        words.all { it in text }
    }
}
