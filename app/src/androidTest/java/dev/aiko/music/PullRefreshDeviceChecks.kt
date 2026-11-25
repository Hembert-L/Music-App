package dev.aiko.music

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dev.aiko.music.playback.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

/** Real touch regression for vertical refresh and the existing horizontal library pager. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object PullRefreshDeviceChecks {
    fun run(test: Instrumentation, activity: Activity, pass: (String) -> Unit) {
        fun <T> main(block: () -> T): T {
            val result = AtomicReference<T>(); val failure = AtomicReference<Throwable>()
            test.runOnMainSync { try { result.set(block()) } catch (t: Throwable) { failure.set(t) } }
            failure.get()?.let { throw it }; return result.get()
        }
        fun until(message: String, predicate: () -> Boolean) {
            val end = SystemClock.elapsedRealtime() + 15000
            while (SystemClock.elapsedRealtime() < end) { if (main(predicate)) return; SystemClock.sleep(15) }
            error(message)
        }
        fun root(view: View): ViewRootForTest? {
            if (view is ViewRootForTest) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) root(view.getChildAt(i))?.let { return it }
            return null
        }
        val owner = main { root(activity.window.decorView) ?: error("No Compose root") }
        fun nodes(merged: Boolean = true) = owner.semanticsOwner.getAllSemanticsNodes(mergingEnabled = merged)
        fun SemanticsNode.labels() = (config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()) +
            (config.getOrNull(SemanticsProperties.ContentDescription) ?: emptyList())
        fun find(label: String) = nodes().firstOrNull { label in it.labels() }
        fun tag(value: String) = nodes(false).firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == value }
        fun click(label: String) {
            until("Missing $label") { nodes().any { label in it.labels() && it.config.contains(SemanticsActions.OnClick) } }
            main { check(nodes().first { label in it.labels() && it.config.contains(SemanticsActions.OnClick) }.config[SemanticsActions.OnClick].action!!.invoke()) }
        }
        fun nativeText(node: AccessibilityNodeInfo, text: String): Boolean {
            if (node.text?.toString() == text) return true
            for (i in 0 until node.childCount) if (node.getChild(i)?.let { nativeText(it, text) } == true) return true
            return false
        }
        fun category(label: String) {
            click(label)
            until("Category not settled: $label") { find("Categorías de biblioteca")?.config?.getOrNull(SemanticsProperties.StateDescription) == label }
            SystemClock.sleep(150)
        }
        fun gesture(x: Float, fromY: Float, toY: Float, toX: Float = x, duration: Long = 450) {
            val down = SystemClock.uptimeMillis()
            fun event(action: Int, px: Float, py: Float) {
                val e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, px, py, 0)
                e.source = InputDevice.SOURCE_TOUCHSCREEN
                try { check(test.uiAutomation.injectInputEvent(e, true)) } finally { e.recycle() }
            }
            event(MotionEvent.ACTION_DOWN, x, fromY)
            try {
                for (i in 1..30) {
                    SystemClock.sleep((down + duration * i / 30 - SystemClock.uptimeMillis()).coerceAtLeast(0))
                    event(MotionEvent.ACTION_MOVE, x + (toX - x) * i / 30, fromY + (toY - fromY) * i / 30)
                }
            } finally { event(MotionEvent.ACTION_UP, toX, toY) }
        }
        val vm = main { ViewModelProvider(activity as ViewModelStoreOwner)[MusicViewModel::class.java] }
        until("Library not ready") { vm.playback.value.connected && !vm.library.value.loading && vm.libraryBrowser.pages.value.songs.isNotEmpty() }
        val soundBefore = PlaybackService.readSound(activity)
        val queueBefore = main { vm.playback.value.queue.map { it.mediaId } }
        val favoritesBefore = main { vm.favorites.value }
        val playlistsFile = activity.filesDir.resolve("playlists.json")
        val playlistsBefore = if (playlistsFile.exists()) playlistsFile.readText() else null
        val orderBefore = main { vm.libraryBrowser.pages.value.order }
        val playingBefore = main { vm.playback.value.playing }
        val audio = activity.getSystemService(AudioManager::class.java)
        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var refreshCount = 0
        main { scope.launch { vm.library.map { it.refreshing }.distinctUntilChanged().collect { if (it) refreshCount++ } } }
        val density = activity.resources.displayMetrics.density
        val screenshots = activity.getExternalFilesDir(null)!!.resolve("pull-refresh").apply { mkdirs() }
        fun screenshot(name: String) {
            val bitmap = requireNotNull(test.uiAutomation.takeScreenshot())
            screenshots.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        fun body() = main { find("Categorías de biblioteca")!!.boundsInWindow }
        fun indexedList() = nodes().firstOrNull {
            val rect = it.boundsInWindow
            val viewport = find("Categorías de biblioteca")!!.boundsInWindow
            it.config.contains(SemanticsProperties.VerticalScrollAxisRange) && it.config.contains(SemanticsActions.ScrollToIndex) &&
                rect.width > 0 && rect.height > 0 && rect.center.x in viewport.left..viewport.right
        }
        fun pull(distanceDp: Float) {
            val b = body(); val y = b.top + 116 * density
            gesture(b.center.x, y, y + distanceDp * density)
        }
        fun top() {
            main { indexedList()?.config?.get(SemanticsActions.ScrollToIndex)?.action?.invoke(0) }
            SystemClock.sleep(180)
        }
        fun refresh(name: String, action: () -> Unit) {
            val before = main { vm.library.value.songs }
            val playbackBefore = main { vm.playback.value }
            val count = main { refreshCount }
            action()
            until("Refresh did not start: $name") { vm.library.value.refreshing && tag("libraryRefreshIndicator") != null }
            check(main { vm.library.value.songs === before && vm.library.value.songs.isNotEmpty() }) { "Refresh cleared cached songs" }
            SystemClock.sleep(120)
            screenshot(name)
            until("Refresh did not finish: $name") { !vm.library.value.loading && !vm.library.value.refreshing }
            until("Indicator stayed visible") { tag("libraryRefreshIndicator") == null }
            check(main { refreshCount == count + 1 && vm.library.value.songs !== before }) { "Not exactly one real library read: $name" }
            check(main { vm.libraryBrowser.pages.value.order == orderBefore })
            if (!playbackBefore.playing || playbackBefore.duration - playbackBefore.position > 2500)
                check(main { vm.playback.value.item?.mediaId == playbackBefore.item?.mediaId && vm.playback.value.playing == playbackBefore.playing }) { "Refresh changed the active track or play/pause state" }
            pass("$name: one real read, wheel shown/hidden, cached rows and order retained")
        }
        try {
            if (test.uiAutomation.rootInActiveWindow?.let { nativeText(it, "Ajustes de sonido") } == true) test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            click("Biblioteca"); category("Canciones")
            if (main { find("Cerrar búsqueda") != null }) click("Cerrar búsqueda")
            if (main { vm.playback.value.playing }) { main { vm.togglePlay() }; until("Pause not confirmed") { !vm.playback.value.playing } }
            click("Opciones de biblioteca")
            SystemClock.sleep(300)
            val menu = requireNotNull(test.uiAutomation.rootInActiveWindow)
            check(nativeText(menu, "Sonido") && !nativeText(menu, "Actualizar biblioteca"))
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            pass("Overflow menu keeps Sound and removes Update library")
            top()
            val countBeforeShort = main { refreshCount }
            val pausedTrack = main { vm.playback.value.item?.mediaId }
            pull(42f); SystemClock.sleep(500)
            check(main { refreshCount == countBeforeShort && !vm.library.value.refreshing && !vm.playback.value.playing && vm.playback.value.item?.mediaId == pausedTrack })
            pass("Short top pull retracts without refreshing")
            refresh("canciones") { pull(240f) }
            check(main { !vm.playback.value.playing && vm.playback.value.item?.mediaId == pausedTrack })
            pass("Short/full pulls over song rows do not select a song or start paused playback")
            if (queueBefore.isNotEmpty()) { main { vm.togglePlay() }; until("Playback did not start") { vm.playback.value.playing } }

            main { requireNotNull(indexedList()).config[SemanticsActions.ScrollToIndex].action!!.invoke(20) }
            SystemClock.sleep(200)
            val countBeforeScroll = main { refreshCount }
            pull(58f); SystemClock.sleep(350)
            check(main { refreshCount == countBeforeScroll })
            pass("Scrolling down inside the list does not refresh before reaching its top")
            top()
            val b = body(); val y = b.top + 24 * density
            gesture(b.right - 32 * density, y, y, toX = b.left + 32 * density)
            until("Horizontal swipe failed") { find("Categorías de biblioteca")?.config?.getOrNull(SemanticsProperties.StateDescription) == "Favoritos" }
            check(main { refreshCount == countBeforeScroll })
            refresh("favoritos") { pull(240f) }
            category("Playlists")
            refresh("playlists") { pull(240f) }
            category("Canciones"); top()

            click("Abrir búsqueda")
            until("No search field") { find("Buscar canciones") != null }
            val query = main { vm.library.value.songs.first().title.take(6) }
            main { check(find("Buscar canciones")!!.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(query)))
                activity.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(activity.window.decorView.windowToken, 0) }
            until("Query not applied") { vm.libraryBrowser.pages.value.query == query }
            refresh("busqueda") { pull(210f) }
            check(main { vm.libraryBrowser.pages.value.query == query })
            main { check(find("Buscar canciones")!!.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("__sin_coincidencias_music__"))) }
            until("Empty results not displayed") { vm.libraryBrowser.pages.value.songs.isEmpty() && find("Sin coincidencias") != null }
            refresh("sin-resultados") { pull(210f) }
            click("Cerrar búsqueda")
            until("Search did not clear") { vm.libraryBrowser.pages.value.query.isEmpty() }
            top()
            refresh("accesibilidad") {
                main { check(tag("libraryRefreshContainer")!!.config[SemanticsActions.CustomActions].first { it.label == "Actualizar biblioteca" }.action()) }
            }
            if (queueBefore.isNotEmpty()) {
                check(main { vm.playback.value.playing })
                click("Reproduciendo"); click("Biblioteca")
                SystemClock.sleep(200)
                check(main { !vm.library.value.refreshing && tag("libraryRefreshIndicator") == null })
                pass("Playback continues; returning from Now Playing does not show a manual refresh wheel")
            }
            check(main { vm.playback.value.queue.map { it.mediaId } == queueBefore && vm.favorites.value == favoritesBefore })
            check((if (playlistsFile.exists()) playlistsFile.readText() else null) == playlistsBefore)
            check(PlaybackService.readSound(activity) == soundBefore && audio.getStreamVolume(AudioManager.STREAM_MUSIC) == volume)
            val report = JSONObject().put("manualRefreshes", main { refreshCount }).put("songs", main { vm.library.value.songs.size })
                .put("systemAnimatorsEnabled", android.animation.ValueAnimator.areAnimatorsEnabled())
                .put("queueUnchanged", true).put("favoritesUnchanged", true).put("playlistsUnchanged", true)
                .put("soundUnchanged", true).put("volume", volume).put("order", orderBefore.name)
            test.sendStatus(0, Bundle().apply { putString("pull_refresh_json", report.toString()) })
            pass("Queue, favorites, playlists, search/order behavior and sound remain intact")
        } catch (failure: Throwable) {
            runCatching { screenshot("fallo") }; throw failure
        } finally {
            main { scope.cancel(); if (vm.playback.value.playing != playingBefore) vm.togglePlay() }
        }
    }
}
