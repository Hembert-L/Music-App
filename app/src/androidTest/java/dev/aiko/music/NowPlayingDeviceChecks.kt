package dev.aiko.music

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.media3.common.Player
import dev.aiko.music.data.Song
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object NowPlayingDeviceChecks {
    fun run(test: Instrumentation, activity: Activity, songs: List<Song>, pass: (String) -> Unit, layoutOnly: Boolean = false) {
        fun <T> main(block: () -> T): T {
            val result = AtomicReference<T>(); val failure = AtomicReference<Throwable>()
            test.runOnMainSync { try { result.set(block()) } catch (t: Throwable) { failure.set(t) } }
            failure.get()?.let { throw it }; return result.get()
        }
        fun root(view: View): ViewRootForTest? {
            if (view is ViewRootForTest) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) root(view.getChildAt(i))?.let { return it }
            return null
        }
        val owner = main { root(activity.window.decorView) ?: error("No Compose root") }
        fun nodes() = owner.semanticsOwner.getAllSemanticsNodes(mergingEnabled = true)
        fun SemanticsNode.texts() = config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()
        fun SemanticsNode.descriptions() = config.getOrNull(SemanticsProperties.ContentDescription) ?: emptyList()
        fun find(label: String) = nodes().firstOrNull { label in it.descriptions() || label in it.texts() }
        fun until(message: String, check: () -> Boolean) {
            val end = SystemClock.elapsedRealtime() + 15000
            while (SystemClock.elapsedRealtime() < end) { if (main(check)) return; SystemClock.sleep(30) }
            error(message)
        }
        fun click(label: String) {
            until("Missing button $label") { nodes().any { (label in it.descriptions() || label in it.texts()) && it.config.contains(SemanticsActions.OnClick) } }
            main { check(nodes().first { (label in it.descriptions() || label in it.texts()) && it.config.contains(SemanticsActions.OnClick) }.config[SemanticsActions.OnClick].action!!.invoke()) }
        }
        fun shell(command: String) { test.uiAutomation.executeShellCommand(command).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { input -> input.readBytes() } } }
        fun waitForSheet(label: String, visible: Boolean) {
            fun walk(node: AccessibilityNodeInfo): Boolean {
                if (node.text?.toString() == label) return true
                for (i in 0 until node.childCount) if (node.getChild(i)?.let(::walk) == true) return true
                return false
            }
            val end = SystemClock.elapsedRealtime() + 10000
            while (SystemClock.elapsedRealtime() < end) {
                if ((test.uiAutomation.rootInActiveWindow?.let(::walk) == true) == visible) return
                SystemClock.sleep(60)
            }
            error("Sheet $label visible != $visible")
        }
        fun screenshot(name: String) {
            val bitmap = test.uiAutomation.takeScreenshot() ?: error("No screenshot")
            test.targetContext.filesDir.resolve(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        fun artworkPixels(): IntArray {
            val bounds = main { find("Carátula circular")!!.boundsInWindow }
            val bitmap = test.uiAutomation.takeScreenshot() ?: error("No screenshot")
            val w = bounds.width.toInt(); val h = bounds.height.toInt()
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, bounds.left.toInt(), bounds.top.toInt(), w, h)
            bitmap.recycle(); return pixels
        }
        val vm = main { ViewModelProvider(activity as ViewModelStoreOwner)[MusicViewModel::class.java] }
        until("Not connected") { vm.playback.value.connected }
        val featured = songs.firstOrNull { it.title.startsWith("PPPP") } ?: songs.first { it.duration > 60000 }
        val long = songs.filter { it.id != featured.id && it.duration > 60000 }.maxBy { it.title.length + it.artist.length }
        val fallback = songs.firstOrNull { it.title.startsWith("AUD-") } ?: songs.first { it.id != featured.id && it.id != long.id }
        val tracks = listOf(featured, long, fallback)
        val favoriteBefore = featured.uri.toString() in vm.favorites.value
        main { vm.play(tracks, featured) }
        until("Featured track not playing") { vm.playback.value.playing && vm.playback.value.item?.mediaId == featured.uri.toString() }
        click("Reproduciendo")
        until("Full player not visible") { find("Carátula circular") != null && find("Progreso de reproducción") != null }

        fun assertLayout() = main {
            val density = activity.resources.displayMetrics.density
            val cover = find("Carátula circular")!!.boundsInRoot
            check(abs(cover.width - cover.height) <= 1) { "Artwork is not square/circular" }
            val labels = listOf(if (vm.playback.value.shuffle) "Desactivar aleatorio" else "Activar aleatorio", "Canción anterior",
                if (vm.playback.value.playing) "Pausar" else "Reproducir", "Canción siguiente",
                "Repetir: ${when (vm.playback.value.repeat) { Player.REPEAT_MODE_ALL -> "toda la cola"; Player.REPEAT_MODE_ONE -> "una canción"; else -> "desactivado" }}")
            val controls = labels.map { find(it)!!.boundsInRoot }
            check(abs(controls[2].width - controls[2].height) < 2 && controls[2].height >= 68 * density - 1) { "Primary circle clipped or undersized" }
            controls.zipWithNext().forEach { (a, b) -> check(a.right <= b.left + 1) { "Controls overlap" } }
            controls.forEach { check(it.width >= 52 * density - 1 && it.height >= 52 * density - 1) { "Touch target smaller than 52 dp" } }
            val nav = nodes().first { "Reproduciendo" in it.texts() && it.config.getOrNull(SemanticsProperties.Role) == Role.Tab }
            check(nav.config.getOrNull(SemanticsProperties.Selected) == true)
            val headerText = nodes().first { "Reproduciendo" in it.texts() && it.config.getOrNull(SemanticsProperties.Role) != Role.Tab }
            val headerLayout = mutableListOf<TextLayoutResult>()
            headerText.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(headerLayout)
            check(headerLayout.none { it.didOverflowHeight }) { "Header text clipped vertically" }
            check(controls.all { it.bottom <= nav.boundsInRoot.top + 1 }) { "Controls clipped by bottom navigation" }
            check(nodes().none { it.texts().any { text -> text.startsWith("Cola de reproducción ·") } || "Cerrar reproducción" in it.descriptions() })
            val size = owner.view.width.toFloat()
            check(abs(cover.center.x - size / 2) < 2 || size > owner.view.height) { "Artwork not horizontally centered" }
            "play=${(controls[2].width / density).toInt()} dp; secondary=${(controls.first().width / density).toInt()} dp"
        }
        try {
            SystemClock.sleep(1200)
            val dimensions = assertLayout()
            if (layoutOnly) {
                click("Pausar")
                until("Pause failed") { !vm.playback.value.playing }
                SystemClock.sleep(250)
                screenshot("now-playing-layout.png")
                pass("Now Playing layout: $dimensions; five controls ordered, complete and visible; shared navigation selected; no duplicate queue or Mini Player")
                return
            }
            val playingPixels = artworkPixels()
            SystemClock.sleep(700)
            check(playingPixels.contentEquals(artworkPixels())) { "Artwork moved while playing" }
            click("Pausar")
            until("Pause failed") { !vm.playback.value.playing }
            SystemClock.sleep(400)
            check(playingPixels.contentEquals(artworkPixels())) { "Artwork moved when paused" }
            pass("Now Playing: circular artwork pixels are identical during playback and pause; five controls fit; no redundant queue/Mini Player")

            click(if (favoriteBefore) "Quitar de favoritos" else "Añadir a favoritos")
            until("Favorite did not change") { (featured.uri.toString() in vm.favorites.value) != favoriteBefore }
            click(if (favoriteBefore) "Añadir a favoritos" else "Quitar de favoritos")
            until("Favorite not restored") { (featured.uri.toString() in vm.favorites.value) == favoriteBefore }
            main { check(find("Progreso de reproducción")!!.config[SemanticsActions.SetProgress].action!!.invoke(60000f)) }
            until("Seek failed") { abs(vm.playback.value.position - 60000) < 1500 }
            click("Reproducir")
            until("Play failed") { vm.playback.value.playing }
            pass("Now Playing: favorite toggle and restore, accessible slider seek, play/pause use the real shared session")

            click("Activar aleatorio"); until("Shuffle failed") { vm.playback.value.shuffle }
            click("Desactivar aleatorio"); until("Shuffle off failed") { !vm.playback.value.shuffle }
            click("Repetir: desactivado"); until("Repeat all failed") { vm.playback.value.repeat == Player.REPEAT_MODE_ALL }
            click("Repetir: toda la cola"); until("Repeat one failed") { vm.playback.value.repeat == Player.REPEAT_MODE_ONE }
            click("Repetir: una canción"); until("Repeat off failed") { vm.playback.value.repeat == Player.REPEAT_MODE_OFF }
            click("Canción siguiente")
            until("Next metadata did not update") { vm.playback.value.item?.mediaId == long.uri.toString() && find(long.title) != null }
            click("Canción anterior")
            until("Previous failed") { vm.playback.value.item?.mediaId == featured.uri.toString() }
            pass("Now Playing: next/previous, shuffle and all repeat modes update the same player")

            click("Opciones de sonido")
            waitForSheet("El sonido, a tu gusto.", true)
            shell("input keyevent KEYCODE_BACK")
            waitForSheet("El sonido, a tu gusto.", false)
            click("Cola")
            waitForSheet("A continuación", true)
            shell("input keyevent KEYCODE_BACK")
            waitForSheet("A continuación", false)
            main { vm.selectQueue(1) }
            until("Long metadata missing") { find(long.title) != null && find(long.artist) != null }
            assertLayout()
            screenshot("now-playing-long.png")
            main { vm.selectQueue(2) }
            until("Fallback track missing") { find(fallback.title) != null }
            SystemClock.sleep(600)
            screenshot("now-playing-fallback.png")
            pass("Now Playing: real long title/artist and fallback track; sound and bottom queue remain accessible")

            click("Volver a la biblioteca")
            until("Mini Player not visible") { find("Cerrar reproducción") != null && find(fallback.title) != null }
            click("Reproduciendo")
            until("Full player not restored") { find("Carátula circular") != null && find(fallback.title) != null }
            main { vm.selectQueue(0) }
            until("Featured track not restored") { find(featured.title) != null && vm.playback.value.playing }
            click("Pausar")
            until("Final pause failed") { !vm.playback.value.playing }
            SystemClock.sleep(250)
            screenshot("now-playing-final.png")
            pass("Now Playing: library/Mini Player/full screen stay synchronized; final track paused")
        } catch (failure: Throwable) {
            screenshot("now-playing-failure.png")
            throw failure
        } finally {
            main { if (vm.playback.value.playing) vm.togglePlay(); if ((featured.uri.toString() in vm.favorites.value) != favoriteBefore) vm.favorite(featured.uri.toString()) }
        }
    }
}
