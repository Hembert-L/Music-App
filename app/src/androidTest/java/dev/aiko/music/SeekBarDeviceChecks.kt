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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object SeekBarDeviceChecks {
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
        fun nodes(merged: Boolean = true) = owner.semanticsOwner.getAllSemanticsNodes(mergingEnabled = merged)
            .filter { it.boundsInRoot.width > 0 && it.boundsInRoot.height > 0 }
        fun SemanticsNode.labels() = (config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()) +
            (config.getOrNull(SemanticsProperties.ContentDescription) ?: emptyList())
        fun find(label: String) = nodes().firstOrNull { label in it.labels() }
        fun track() = nodes(false).first { it.config.getOrNull(SemanticsProperties.TestTag) == "playerSeekTrack" }.boundsInWindow
        fun until(message: String, condition: () -> Boolean) {
            val end = SystemClock.elapsedRealtime() + 12000
            while (SystemClock.elapsedRealtime() < end) { if (main(condition)) return; SystemClock.sleep(25) }
            error(message)
        }
        fun click(label: String) {
            until("Missing $label") { nodes().any { label in it.labels() && it.config.contains(SemanticsActions.OnClick) } }
            main { check(nodes().first { label in it.labels() && it.config.contains(SemanticsActions.OnClick) }.config[SemanticsActions.OnClick].action!!.invoke()) }
        }
        fun screenshot(name: String) {
            val bitmap = test.uiAutomation.takeScreenshot() ?: error("No screenshot")
            test.targetContext.filesDir.resolve(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        fun touch(from: Float, to: Float = from, yOffsetDp: Float = 0f, cancel: Boolean = false, beforeUp: (() -> Unit)? = null) {
            val bounds = main { track() }
            val y = bounds.center.y + yOffsetDp * activity.resources.displayMetrics.density
            val startX = bounds.left + bounds.width * from
            val endX = bounds.left + bounds.width * to
            val down = SystemClock.uptimeMillis()
            fun event(action: Int, x: Float) {
                val motion = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
                motion.source = InputDevice.SOURCE_TOUCHSCREEN
                try { check(test.uiAutomation.injectInputEvent(motion, true)) } finally { motion.recycle() }
            }
            event(MotionEvent.ACTION_DOWN, startX)
            var ended = false
            try {
                if (from != to) for (i in 1..24) {
                    SystemClock.sleep(16)
                    event(MotionEvent.ACTION_MOVE, startX + (endX - startX) * i / 24)
                }
                SystemClock.sleep(90)
                beforeUp?.invoke()
                event(if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP, endX)
                ended = true
            } finally { if (!ended) event(MotionEvent.ACTION_CANCEL, endX) }
        }
        val vm = main { ViewModelProvider(activity as ViewModelStoreOwner)[MusicViewModel::class.java] }
        until("Library not ready") { vm.library.value.songs.size >= 3 && vm.playback.value.connected }
        val songs = main { vm.library.value.songs }
        val first = songs.firstOrNull { it.title.startsWith("PPPP") && it.duration > 120000 } ?: songs.first { it.duration > 120000 }
        val tracks = listOf(first) + songs.filter { it.id != first.id && it.duration > 60000 }.take(2)
        main { vm.play(tracks, first) }
        until("Track not playing") { vm.playback.value.playing && vm.playback.value.duration > 0 }
        click("Reproduciendo")
        until("Seek bar not drawn") { find("Progreso de reproducción") != null }
        val duration = main { vm.playback.value.duration }
        val queue = main { vm.playback.value.queue.map { it.mediaId } }
        fun uiPosition() = find("Progreso de reproducción")!!.config[SemanticsProperties.ProgressBarRangeInfo].current.toLong()
        fun matches(fraction: Float, playing: Boolean) {
            val target = (duration * fraction).toLong()
            until("Seek $fraction not confirmed (playing=$playing)") {
                val p = vm.playback.value
                p.playing == playing && abs(p.position - target) <= (if (playing) 1600 else 900) && p.error == null
            }
            until("Seek bar did not match Media3") { abs(uiPosition() - vm.playback.value.position) < 1300 }
        }
        fun accessible(value: Float) = main { check(find("Progreso de reproducción")!!.config[SemanticsActions.SetProgress].action!!.invoke(value)) }
        try {
            main {
                val density = activity.resources.displayMetrics.density
                check(abs(track().height / density - 6f) < 1)
                val thumb = nodes(false).first { it.config.getOrNull(SemanticsProperties.TestTag) == "playerSeekThumb" }.boundsInRoot
                check(abs(thumb.width / density - 18f) < 1 && thumb.height >= 48 * density - 1)
                check(find("Progreso de reproducción")!!.boundsInRoot.height >= 48 * density - 1)
            }
            pass("Seek bar: native track 6 dp, visible thumb 18 dp and direct touch surface 48 dp")
            click("Pausar")
            until("Pause failed") { !vm.playback.value.playing && find("Reproducir") != null }
            touch(.75f, yOffsetDp = 14f); matches(.75f, false)
            val before = main { vm.playback.value.position }
            touch(.75f, .20f, beforeUp = {
                screenshot("seek-preview.png")
                val preview = main { uiPosition() }
                val actual = main { vm.playback.value.position }
                check(abs(preview - duration * .20f) < 1300 && abs(actual - before) < 400) {
                    "Drag preview=$preview, expected=${duration * .20f}, Media3=$actual, before=$before"
                }
            })
            matches(.20f, false)
            touch(.60f, yOffsetDp = -14f); matches(.60f, false)
            pass("Seek bar: paused forward tap, backward drag with live preview, and taps above/below the visible line land within 900 ms")

            for (fraction in listOf(.80f, .25f, .65f, .15f)) touch(fraction)
            matches(.15f, false)
            SystemClock.sleep(1800)
            check(main { abs(vm.playback.value.position - duration * .15f) < 900 && abs(uiPosition() - vm.playback.value.position) < 900 })
            pass("Seek bar: rapid alternate forward/back seeks keep the last destination without reverting after confirmation")

            click("Reproducir")
            until("Play failed") { vm.playback.value.playing }
            touch(.70f); matches(.70f, true)
            touch(.70f, .30f); matches(.30f, true)
            touch(.30f, .55f, cancel = true)
            SystemClock.sleep(700)
            until("Cancelled drag left a frozen preview") { abs(uiPosition() - vm.playback.value.position) < 1300 && vm.playback.value.playing }
            check(main { vm.playback.value.queue.map { it.mediaId } == queue && vm.playback.value.item?.mediaId == first.uri.toString() })
            pass("Seek bar: real forward/back gestures during playback preserve play state and queue; cancellation resynchronizes the preview")

            click("Pausar")
            until("Pause failed") { !vm.playback.value.playing }
            touch(0f); matches(0f, false)
            touch(1f); matches(1f, false)
            accessible(duration * .42f); matches(.42f, false)
            accessible(-5000f); matches(0f, false)
            accessible(duration * 1.2f); matches(1f, false)
            accessible(duration * .35f); matches(.35f, false)
            click("Canción siguiente")
            until("Next failed") { vm.playback.value.item?.mediaId == tracks[1].uri.toString() }
            click("Canción anterior")
            until("Previous failed") { vm.playback.value.item?.mediaId == first.uri.toString() }
            until("New item progress not reset") { abs(uiPosition() - vm.playback.value.position) < 1300 }
            accessible(duration * .35f); matches(.35f, false)
            SystemClock.sleep(250)
            screenshot("seek-final.png")
            pass("Seek bar: endpoints and accessibility clamp correctly; next/previous reset progress and keep metadata synchronized")
        } catch (failure: Throwable) {
            screenshot("seek-failure.png"); throw failure
        } finally { main { if (vm.playback.value.playing) vm.togglePlay() } }
    }
}
