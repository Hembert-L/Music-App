package dev.aiko.music

import android.app.Activity
import android.app.Instrumentation
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/** Observes every library pre-draw, including the first frame after leaving the full player. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object LibraryNavigationDeviceChecks {
    fun run(test: Instrumentation, activity: Activity, pass: (String) -> Unit) {
        fun <T> main(block: () -> T): T {
            val result = AtomicReference<T>(); val error = AtomicReference<Throwable>()
            test.runOnMainSync { try { result.set(block()) } catch (t: Throwable) { error.set(t) } }
            error.get()?.let { throw it }
            return result.get()
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
        fun awaitCategory(label: String) {
            if (label !in listOf("Canciones", "Favoritos", "Playlists")) return
            val end = SystemClock.elapsedRealtime() + 10000
            while (SystemClock.elapsedRealtime() < end) {
                if (main { find("Categorías de biblioteca")?.config?.getOrNull(SemanticsProperties.StateDescription) == label }) return
                SystemClock.sleep(25)
            }
            error("Category did not settle: $label")
        }
        fun click(label: String) {
            val end = SystemClock.elapsedRealtime() + 10000
            while (SystemClock.elapsedRealtime() < end) {
                if (main { find(label)?.config?.getOrNull(SemanticsActions.OnClick)?.action?.invoke() == true }) { awaitCategory(label); return }
                // Dropdown menus own a separate Compose window. Use Android's tree for those.
                fun walk(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                    if (node.text?.toString() == label || node.contentDescription?.toString() == label) return node
                    for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it)?.let { found -> return found } }
                    return null
                }
                var node = test.uiAutomation.rootInActiveWindow?.let(::walk)
                while (node != null && !node.isClickable) node = node.parent
                if (node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { awaitCategory(label); return }
                SystemClock.sleep(20)
            }
            error("Cannot click $label")
        }
        fun until(message: String, condition: () -> Boolean) {
            val end = SystemClock.elapsedRealtime() + 15000
            while (SystemClock.elapsedRealtime() < end) { if (main(condition)) return; SystemClock.sleep(20) }
            error(message)
        }
        val vm = main { ViewModelProvider(activity as ViewModelStoreOwner)[MusicViewModel::class.java] }
        until("Library not ready") { vm.libraryBrowser.results.value.songs.isNotEmpty() && vm.playback.value.connected }
        click("Biblioteca")
        click("Canciones")
        until("Songs not applied") { vm.libraryBrowser.results.value.filter?.let { it.query.isEmpty() && !it.favoritesOnly } == true && find("Reproducir al azar") != null }
        main { val songs = vm.libraryBrowser.results.value.songs; vm.play(songs, songs.first()) }
        until("Playback not ready") { vm.playback.value.playing }
        main { vm.togglePlay() }
        // Scroll away from the top so a transient empty list would also lose a visible position.
        main { check(nodes().firstOrNull { it.config.contains(SemanticsProperties.VerticalScrollAxisRange) && it.config.contains(SemanticsActions.ScrollBy) }?.config?.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f, 700f) == true) }
        SystemClock.sleep(600)
        val snapshot = main { vm.libraryBrowser.results.value }
        fun visibleRows() = nodes().flatMap { node -> node.descriptions().filter { it.startsWith("Opciones de ") && it != "Opciones de biblioteca" }.map { it to node.boundsInRoot.top } }
        val anchor = main { visibleRows().first() }
        var armed = false
        var expectRows = true
        var frames = 0
        val failures = mutableListOf<String>()
        val observer = ViewTreeObserver.OnPreDrawListener {
            if (armed) {
                val current = nodes()
                if (current.any { "Canciones" in it.texts() } && current.none { "Carátula circular" in it.descriptions() }) {
                    frames++
                    if (current.any { it.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) == ProgressBarRangeInfo.Indeterminate }) failures.add("Loading spinner at frame $frames")
                    if (current.any { "Conectando reproductor…" in it.texts() }) failures.add("Fake reconnection at frame $frames")
                    if (expectRows && visibleRows().isEmpty()) failures.add("Empty songs at frame $frames")
                }
            }
            true
        }
        main { activity.window.decorView.viewTreeObserver.addOnPreDrawListener(observer) }
        fun roundTrip() {
            click("Reproduciendo")
            until("Player not opened") { find("Carátula circular") != null }
            main { armed = true }
            click("Biblioteca")
            until("Library not restored") { find("Canciones") != null }
            SystemClock.sleep(150)
            main { armed = false }
        }
        try {
            repeat(8) {
                roundTrip()
                check(main { vm.libraryBrowser.results.value === snapshot }) { "Navigation rebuilt results" }
                check(main { visibleRows().any { it.first == anchor.first && abs(it.second - anchor.second) < 2f } }) { "Scroll position changed" }
            }
            check(main { frames >= 8 && failures.isEmpty() }) { failures.joinToString() }
            pass("Library navigation: 8 player round trips retain results and scroll; no spinner, empty list or reconnection in $frames library pre-draws")

            // Real search, real order menu and navigation retain the same filtered snapshot.
            click("Abrir búsqueda")
            until("Search not opened") { find("Buscar canciones") != null }
            val query = snapshot.songs.first().title.take(5)
            main { check(find("Buscar canciones")?.config?.getOrNull(SemanticsActions.SetText)?.action?.invoke(AnnotatedString(query)) == true) }
            test.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
            until("Search not applied") { vm.libraryBrowser.results.value.filter?.query == query && vm.libraryBrowser.results.value.songs.isNotEmpty() }
            click("Ordenar canciones"); click("Más antiguos")
            until("Order not applied") { vm.libraryBrowser.results.value.filter?.order == LibraryOrder.OLDEST }
            val searched = main { vm.libraryBrowser.results.value }
            repeat(3) { roundTrip(); check(main { vm.libraryBrowser.results.value === searched && find("Más antiguos") != null }) }
            pass("Library navigation: real search and oldest order survive player round trips without recomputation")

            click("Cerrar búsqueda")
            until("Query not cleared") { vm.libraryBrowser.results.value.filter?.query == "" }
            click("Ordenar canciones"); click("Más recientes")
            until("Newest not applied") { vm.libraryBrowser.results.value.filter?.order == LibraryOrder.NEWEST }
            main { expectRows = false }
            click("Favoritos")
            until("Favorites not applied") { vm.libraryBrowser.results.value.filter?.favoritesOnly == true }
            val favorites = main { vm.libraryBrowser.results.value }
            roundTrip()
            check(main { vm.libraryBrowser.results.value === favorites })
            click("Playlists"); roundTrip()
            click("Canciones")
            until("Songs not restored") { vm.libraryBrowser.results.value.filter?.favoritesOnly == false }
            check(main { failures.isEmpty() }) { failures.joinToString() }
            pass("Library navigation: Favorites and Playlists return without false loading; Songs remains usable")

            val beforeRefresh = main { vm.libraryBrowser.results.value }
            main { vm.refresh() }
            until("Refresh did not complete") { !vm.library.value.loading }
            SystemClock.sleep(200)
            check(main { vm.libraryBrowser.results.value === beforeRefresh }) { "Unchanged scan rebuilt the search index/results" }
            pass("Library navigation: an unchanged MediaStore refresh retains the existing snapshot")
        } finally {
            main { armed = false; activity.window.decorView.viewTreeObserver.removeOnPreDrawListener(observer) }
        }
    }
}
