package dev.aiko.music

import android.app.Activity
import android.app.Instrumentation
import android.app.NotificationManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.aiko.music.data.Song
import java.util.concurrent.atomic.AtomicReference

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object MiniPlayerDeviceChecks {
    fun run(test: Instrumentation, activity: Activity, controller: MediaController, songs: List<Song>, pass: (String) -> Unit) {
        fun <T> main(block: () -> T): T {
            val value = AtomicReference<T>(); val failure = AtomicReference<Throwable>()
            test.runOnMainSync { try { value.set(block()) } catch (t: Throwable) { failure.set(t) } }
            failure.get()?.let { throw it }
            return value.get()
        }
        fun until(message: String, condition: () -> Boolean) {
            val end = SystemClock.elapsedRealtime() + 25000
            while (SystemClock.elapsedRealtime() < end) { if (condition()) return; SystemClock.sleep(80) }
            error(message)
        }
        fun find(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
            fun walk(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if (predicate(node)) return node
                for (i in 0 until node.childCount) { node.getChild(i)?.let { walk(it)?.let { found -> return found } } }
                return null
            }
            return test.uiAutomation.rootInActiveWindow?.let(::walk)
        }
        fun label(value: String) = find { it.text?.toString() == value || it.contentDescription?.toString() == value }
        fun click(value: String) {
            until("Missing $value") { label(value) != null }
            var node = label(value)!!
            while (!node.isClickable) node = node.parent ?: error("Not clickable: $value")
            check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        val vm = main { ViewModelProvider(activity as ViewModelStoreOwner)[MusicViewModel::class.java] }
        until("ViewModel controller not connected") { main { vm.playback.value.connected } }
        val prefs = activity.getSharedPreferences("music", 0)
        val favorites = prefs.getStringSet("favorites", emptySet())!!.toSet()
        val playlistsFile = activity.filesDir.resolve("playlists.json")
        val playlistsBefore = if (playlistsFile.exists()) playlistsFile.readText() else null
        val long = songs.filter { it.title.length >= 45 && it.duration > 60000 }.minByOrNull { it.title.length } ?: error("No long real title")
        val short = songs.firstOrNull { it.title.length <= 8 && it.duration > 60000 } ?: error("No short real title")
        val tracks = listOf(long, short) + songs.first { it.id != long.id && it.id != short.id }
        main { vm.play(tracks, long) }
        until("Long title not presented") { main { vm.playback.value.playing && vm.miniTitleMotion.currentRun != null } }
        val token = main { vm.playback.value.titleToken }
        val run = main { vm.miniTitleMotion.currentRun!! }
        click("Pausar")
        until("Mini pause failed") { main { !vm.playback.value.playing } }
        check(main { vm.playback.value.titleToken == token })
        click("Reproduciendo")
        until("Full player not opened") { label("Carátula circular") != null }
        click("Biblioteca")
        until("Mini not restored") { label("Cerrar reproducción") != null }
        check(main { vm.miniTitleMotion.currentRun === run && vm.playback.value.titleToken == token })
        click("Favoritos"); click("Playlists"); click("Canciones")
        check(main { vm.miniTitleMotion.currentRun === run })
        until("Title run did not finish") { run.finished(SystemClock.elapsedRealtime()) }
        SystemClock.sleep(250)
        check(main { vm.miniTitleMotion.currentRun === run })
        pass("Mini title: long real title runs once; pause and screen/tab navigation preserve its timeline")
        click("Siguiente canción")
        until("Short title did not appear") { main { vm.playback.value.playing && vm.playback.value.item?.mediaId == short.uri.toString() && vm.playback.value.titleToken > token && vm.miniTitleMotion.currentRun == null } }
        click("Pausar")
        pass("Mini title: short real title remains static; no animation on pause/resume")
        click("Canción anterior")
        until("Previous failed") { main { vm.playback.value.item?.mediaId == long.uri.toString() && vm.playback.value.playing } }
        main { vm.play(tracks, long, true); controller.repeatMode = Player.REPEAT_MODE_ALL }
        until("Shuffle not active") { main { vm.playback.value.shuffle && vm.playback.value.playing } }
        val beforeNext = main { controller.currentMediaItemIndex }
        click("Siguiente canción")
        until("Shuffle next failed") { main { controller.currentMediaItemIndex != beforeNext } }
        click("Canción anterior")
        until("Shuffle previous failed") { main { controller.currentMediaItemIndex == beforeNext } }
        pass("Mini controls: play, pause, previous, next and shuffle order use the shared player")
        fun contentBottom(): Int = find { it.isScrollable }?.let { android.graphics.Rect().also(it::getBoundsInScreen).bottom } ?: 0
        val bottomWithMini = contentBottom()
        click("Cerrar reproducción")
        until("Close did not clear playback") { main { controller.mediaItemCount == 0 && controller.currentMediaItem == null && !controller.playWhenReady && controller.playbackState == Player.STATE_IDLE } }
        until("Mini stayed visible") { label("Cerrar reproducción") == null }
        check(prefs.getString("queue", null) == "[]" && prefs.getBoolean("playback_closed", false))
        until("Freed space not used") { contentBottom() > bottomWithMini + (40 * activity.resources.displayMetrics.density).toInt() }
        val notifications = activity.getSystemService(NotificationManager::class.java)
        until("Transport notification stayed visible") { notifications.activeNotifications.none { it.notification.category == android.app.Notification.CATEGORY_TRANSPORT } }
        click("Playlists"); click("Favoritos"); click("Canciones")
        SystemClock.sleep(500)
        check(label("Cerrar reproducción") == null && main { controller.currentMediaItem == null })
        check(prefs.getStringSet("favorites", emptySet()) == favorites)
        check((if (playlistsFile.exists()) playlistsFile.readText() else null) == playlistsBefore)
        pass("Mini X: stops, clears persistent queue, removes notification and frees space without deleting favorites/playlists")
        main { vm.play(tracks, short) }
        until("Explicit replay did not restore Mini") { label("Cerrar reproducción") != null && main { vm.playback.value.playing && controller.currentMediaItem?.mediaId == short.uri.toString() } }
        check(!prefs.getBoolean("playback_closed", true))
        main { controller.pause(); controller.shuffleModeEnabled = false; controller.repeatMode = Player.REPEAT_MODE_OFF }
        pass("Mini: explicit new playback restores the shared player after X")
    }
}
