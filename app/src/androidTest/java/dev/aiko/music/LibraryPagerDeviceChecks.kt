package dev.aiko.music

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import java.util.concurrent.atomic.AtomicReference

/** Injects real touch paths through Android's input dispatcher on the connected physical phone. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object LibraryPagerDeviceChecks {
    fun run(test: Instrumentation, activity: Activity, pass: (String) -> Unit) {
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
        fun nodes() = owner.semanticsOwner.getAllSemanticsNodes(mergingEnabled = true).filter { it.boundsInRoot.width > 0 && it.boundsInRoot.height > 0 }
        fun SemanticsNode.texts() = config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()
        fun SemanticsNode.descriptions() = config.getOrNull(SemanticsProperties.ContentDescription) ?: emptyList()
        fun find(label: String) = nodes().firstOrNull { label in it.texts() || label in it.descriptions() }
        fun until(message: String, condition: () -> Boolean) {
            val end = SystemClock.elapsedRealtime() + 15000
            while (SystemClock.elapsedRealtime() < end) { if (main(condition)) return; SystemClock.sleep(25) }
            error(message)
        }
        fun click(label: String) {
            until("Missing $label") { nodes().any { (label in it.texts() || label in it.descriptions()) && it.config.contains(SemanticsActions.OnClick) } }
            main { check(nodes().first { (label in it.texts() || label in it.descriptions()) && it.config.contains(SemanticsActions.OnClick) }.config[SemanticsActions.OnClick].action!!.invoke()) }
        }
        fun settled(label: String) {
            until("Pager did not settle on $label") {
                find("Categorías de biblioteca")?.config?.getOrNull(SemanticsProperties.StateDescription) == label &&
                    nodes().any { label in it.texts() && it.config.getOrNull(SemanticsProperties.Selected) == true }
            }
        }
        fun category(label: String) { click(label); settled(label) }
        fun screenshot(name: String) {
            val bitmap = test.uiAutomation.takeScreenshot() ?: error("No screenshot")
            test.targetContext.filesDir.resolve(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        fun gesture(fromX: Float, fromY: Float, toX: Float, toY: Float, duration: Long = 420, hold: Long = 0, middle: (() -> Unit)? = null) {
            val down = SystemClock.uptimeMillis()
            fun event(action: Int, x: Float, y: Float) {
                val e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
                e.source = InputDevice.SOURCE_TOUCHSCREEN
                check(test.uiAutomation.injectInputEvent(e, true)); e.recycle()
            }
            event(MotionEvent.ACTION_DOWN, fromX, fromY)
            val steps = (duration / 16).toInt().coerceAtLeast(2)
            for (i in 1..steps) {
                SystemClock.sleep((down + duration * i / steps - SystemClock.uptimeMillis()).coerceAtLeast(0))
                val f = i.toFloat() / steps
                event(MotionEvent.ACTION_MOVE, fromX + (toX - fromX) * f, fromY + (toY - fromY) * f)
                if (i == steps / 2) middle?.invoke()
            }
            if (hold > 0) SystemClock.sleep(hold)
            event(MotionEvent.ACTION_UP, toX, toY)
        }
        fun swipe(left: Boolean, middle: (() -> Unit)? = null) {
            val body = main { find("Categorías de biblioteca")!!.boundsInWindow }
            val y = body.top + 24 * activity.resources.displayMetrics.density
            gesture(body.left + body.width * if (left) .86f else .14f, y,
                body.left + body.width * if (left) .14f else .86f, y, middle = middle)
        }
        fun rows() = nodes().flatMap { node -> node.descriptions().filter { it.startsWith("Opciones de ") && it != "Opciones de biblioteca" }.map { it to node.boundsInRoot.top } }
        val vm = main { ViewModelProvider(activity as ViewModelStoreOwner)[MusicViewModel::class.java] }
        until("Library not ready") { vm.libraryBrowser.pages.value.songs.isNotEmpty() && vm.playback.value.connected }
        click("Biblioteca"); category("Canciones")
        if (main { find("Cerrar búsqueda") != null }) click("Cerrar búsqueda")
        until("Search not clear") { vm.libraryBrowser.pages.value.query.isEmpty() }
        val tracks = main { vm.libraryBrowser.pages.value.songs.filter { it.duration > 90000 }.take(3) }
        check(tracks.size == 3)
        main { vm.play(tracks, tracks.first()) }
        until("Playback not advancing") { vm.playback.value.playing && vm.playback.value.position > 1000 }
        val queue = main { vm.playback.value.queue.map { it.mediaId } }
        val startPosition = main { vm.playback.value.position }
        val cached = main { vm.libraryBrowser.pages.value }
        try {
            swipe(true) {
                SystemClock.sleep(90)
                val progress = main {
                    val node = find("Categorías de biblioteca")!!
                    (node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)?.value?.invoke() ?: -1f) / node.boundsInRoot.width
                }
                check(progress > .03f && progress < .97f) { "Content did not follow the finger: offset=$progress pages" }
                screenshot("pager-drag.png")
            }
            settled("Favoritos")
            check(main { vm.libraryBrowser.pages.value === cached })
            swipe(true); settled("Playlists")
            swipe(true); settled("Playlists") // Last page never wraps.
            swipe(false); settled("Favoritos")
            swipe(false); settled("Canciones")
            swipe(false); settled("Canciones")
            pass("Pager: real drags follow the finger through Songs/Favorites/Playlists and back; one page per gesture; no wrapping or refiltering")

            val body = main { find("Categorías de biblioteca")!!.boundsInWindow }
            val density = activity.resources.displayMetrics.density
            val y = body.top + 24 * density
            gesture(body.center.x, y, body.center.x - 5 * density, y, 180, 180)
            SystemClock.sleep(120); settled("Canciones")
            gesture(body.left + body.width * .7f, y, body.left + body.width * .55f, y, 700, 250)
            SystemClock.sleep(120); settled("Canciones")
            gesture(body.center.x, body.top + body.height * .72f, body.center.x - 7 * density, body.top + body.height * .27f, 500)
            settled("Canciones"); SystemClock.sleep(500)
            val anchor = main { rows().first() }
            check(anchor.first != "Opciones de ${tracks.first().title}") { "Vertical list did not move" }
            swipe(true); settled("Favoritos"); swipe(false); settled("Canciones")
            check(main { rows().any { it.first == anchor.first && kotlin.math.abs(it.second - anchor.second) < 2f } }) { "Songs lost scroll position" }
            pass("Pager: 5 dp jitter and a short slow drag stay put; vertical/diagonal list scroll does not switch pages; scroll anchor survives tab changes")

            click("Abrir búsqueda")
            until("Search missing") { find("Buscar canciones") != null }
            val query = tracks.first().title.take(4)
            main { check(find("Buscar canciones")!!.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(query))) }
            until("Query not applied") { vm.libraryBrowser.pages.value.query == query }
            val filtered = main { vm.libraryBrowser.pages.value }
            swipe(true); settled("Favoritos")
            check(main { vm.libraryBrowser.pages.value === filtered })
            check(main { rows().all { row -> filtered.favorites.any { "Opciones de ${it.title}" == row.first } } }) { "Favorites shows Songs rows" }
            swipe(false); settled("Canciones")
            check(main { find("Buscar canciones")!!.config.getOrNull(SemanticsProperties.EditableText)?.text == query })
            click("Cerrar búsqueda")
            until("Query not cleared") { vm.libraryBrowser.pages.value.query.isEmpty() }
            category("Playlists")
            click("Abrir búsqueda")
            until("Playlist search missing") { find("Buscar playlists") != null }
            main { check(find("Buscar playlists")!!.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("lo"))) }
            swipe(false); settled("Favoritos")
            swipe(true); settled("Playlists")
            check(main { find("Buscar playlists")!!.config.getOrNull(SemanticsProperties.EditableText)?.text == "lo" })
            click("Cerrar búsqueda")
            category("Canciones")
            click("Reproduciendo")
            until("Full player missing") { find("Carátula circular") != null }
            click("Biblioteca"); settled("Canciones")
            check(main { vm.libraryBrowser.pages.value.songs.isNotEmpty() })
            check(main { vm.playback.value.playing && vm.playback.value.queue.map { it.mediaId } == queue && vm.playback.value.position > startPosition + 5000 })
            main { vm.togglePlay() }
            until("Final pause failed") { !vm.playback.value.playing }
            screenshot("pager-final.png")
            pass("Pager: tab taps, retained song/playlist searches, full-player return and uninterrupted real playback share the same queue")
        } catch (failure: Throwable) {
            screenshot("pager-failure.png"); throw failure
        } finally {
            main { if (vm.playback.value.playing) vm.togglePlay() }
        }
    }
}
