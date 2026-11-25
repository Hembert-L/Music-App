package dev.aiko.music

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dev.aiko.music.playback.PlaybackService
import dev.aiko.music.playback.SoundMode
import dev.aiko.music.playback.SoundSettings
import dev.aiko.music.ui.MusicStyle
import dev.aiko.music.ui.MusicTheme
import dev.aiko.music.ui.SoundSettingsSheet
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/** Real UI and preferences on the physical phone, with no changes to audio implementation. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object SoundSettingsDeviceChecks {
    fun run(test: Instrumentation, activity: Activity, pass: (String) -> Unit) {
        fun <T> main(block: () -> T): T {
            val result = AtomicReference<T>(); val failure = AtomicReference<Throwable>()
            test.runOnMainSync { try { result.set(block()) } catch (t: Throwable) { failure.set(t) } }
            failure.get()?.let { throw it }; return result.get()
        }
        fun until(message: String, predicate: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + 15000
            while (SystemClock.elapsedRealtime() < deadline) { if (predicate()) return; SystemClock.sleep(60) }
            error(message)
        }
        fun walk(node: AccessibilityNodeInfo, label: String, slider: Boolean): AccessibilityNodeInfo? {
            if (node.isVisibleToUser && slider && node.rangeInfo != null && walk(node, label, false) != null) return node
            if (node.isVisibleToUser && !slider && (node.text?.toString() == label || node.contentDescription?.toString() == label)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, label, slider)?.let { found -> return found } }
            return null
        }
        fun find(label: String, slider: Boolean = false) = test.uiAutomation.rootInActiveWindow?.let { walk(it, label, slider) }
        fun click(label: String) {
            until("Missing $label") { find(label) != null }
            var node = find(label)!!
            while (!node.isClickable) node = node.parent ?: error("Not clickable: $label")
            check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            SystemClock.sleep(220)
        }
        fun bounds(label: String) = Rect().also { requireNotNull(find(label)) { "Missing bounds for $label" }.getBoundsInScreen(it) }
        fun back() {
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            SystemClock.sleep(220)
        }
        fun tap(x: Float, y: Float) {
            val down = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                try { check(test.uiAutomation.injectInputEvent(event, true)) } finally { event.recycle() }
            }
            SystemClock.sleep(250)
        }
        val context = test.targetContext
        val vm = main { ViewModelProvider(activity as ViewModelStoreOwner)[MusicViewModel::class.java] }
        until("No player connection") { main { vm.playback.value.connected } }
        val before = main { vm.sound.value }
        val initialPlaying = main { vm.playback.value.playing }
        val queueBefore = main { vm.playback.value.queue.map { it.mediaId } }
        val audio = context.getSystemService(AudioManager::class.java)
        val initialVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val screenshots = context.getExternalFilesDir(null)!!.resolve("sound-settings").apply { mkdirs() }
        val observations = JSONArray()
        fun screenshot(name: String) {
            SystemClock.sleep(350)
            val bitmap = requireNotNull(test.uiAutomation.takeScreenshot())
            screenshots.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        fun selected(mode: SoundMode) {
            until("Mode not confirmed: $mode") { main { vm.sound.value.mode == mode } && PlaybackService.readSound(context).mode == mode }
            val labels = listOf("Original", "Recomendado", "Personalizado")
            val selectedLabel = when(mode) { SoundMode.ORIGINAL -> "Original"; SoundMode.RECOMMENDED -> "Recomendado"; else -> "Personalizado" }
            until("Visual mode did not settle: $mode") {
                labels.count { find("Modo $it")?.isChecked == true } == 1 && find("Modo $selectedLabel")?.isChecked == true &&
                    (find("Graves") != null) == (mode == SoundMode.CUSTOM)
            }
            SystemClock.sleep(350)
        }
        fun setProgress(label: String, value: Float) {
            until("No slider $label") { find(label, true) != null }
            check(find(label, true)!!.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
                Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, value) }))
        }
        try {
            if (queueBefore.isNotEmpty() && !initialPlaying) {
                main { vm.togglePlay() }
                until("Saved track did not start") { main { vm.playback.value.playing } }
            }
            if (find("Ajustes de sonido") != null) back()
            if (find("Opciones de biblioteca") == null) click("Biblioteca")
            click("Opciones de biblioteca"); click("Sonido")
            until("No sound panel") { find("Ajustes de sonido") != null }
            click("Modo Original"); selected(SoundMode.ORIGINAL)
            val compactTop = bounds("Ajustes de sonido").top
            screenshot("original")
            click("Modo Original"); selected(SoundMode.ORIGINAL)
            check(find("Controles de auriculares") == null)
            pass("Original: compact panel, exactly one active switch, active switch cannot leave all modes off")

            val settingBeforeInfo = main { vm.sound.value }
            click("Información de Recomendado")
            until("Info not visible") { find("Cerrar información") != null }
            screenshot("informacion-recomendado")
            check(main { vm.sound.value == settingBeforeInfo })
            click("Cerrar información")
            until("Info did not close") { find("Ajustes de sonido") != null }
            check(abs(bounds("Ajustes de sonido").top - compactTop) <= 2)
            click("Información de Original"); back()
            until("Back did not close info") { find("Ajustes de sonido") != null }
            click("Información de Recomendado")
            tap(20f, (activity.resources.displayMetrics.heightPixels * .25f))
            until("Outside tap closed the wrong panel") { find("Ajustes de sonido") != null && find("Cerrar información") == null }
            check(main { vm.sound.value == settingBeforeInfo })
            pass("Info: independent of selection; close button, outside tap and Back; no sheet reflow")

            click("Modo Recomendado"); selected(SoundMode.RECOMMENDED)
            screenshot("recomendado")
            val recommendedTop = bounds("Ajustes de sonido").top
            click("Modo Personalizado"); selected(SoundMode.CUSTOM)
            check(bounds("Ajustes de sonido").top < recommendedTop - 150)
            val switchBounds = listOf("Original", "Recomendado", "Personalizado").map { bounds("Modo $it") }
            check(switchBounds.maxOf { it.right } - switchBounds.minOf { it.right } <= 2)
            check(switchBounds.all { it.height() >= 47 * activity.resources.displayMetrics.density })
            screenshot("personalizado")
            pass("Modes: shared row/switch alignment; Custom expands, Recommended hides all adjustments")

            setProgress("Graves", 4f); until("Bass not persisted") { PlaybackService.readSound(context).bass == 4f }
            val sliderBounds = Rect().also { find("Graves", true)!!.getBoundsInScreen(it) }
            tap(sliderBounds.left + sliderBounds.width() * .8f, sliderBounds.exactCenterY())
            until("Slider touch did not persist") { PlaybackService.readSound(context).bass in 4.2f..6f }
            setProgress("Graves", 4f); until("Bass restore failed") { PlaybackService.readSound(context).bass == 4f }
            setProgress("Brillo", 3f); until("Air not persisted") { PlaybackService.readSound(context).air == 3f }
            setProgress("Nivel adicional", -5f); until("Trim not persisted") { PlaybackService.readSound(context).trim == -5f }
            val customValues = main { vm.sound.value }
            check(abs(customValues.headroomDb - SoundSettings(true, 4f, 3f, -5f).headroomDb) < .0001f)
            until("Headroom value not updated") { find("Margen automático: %.1f dB".format(customValues.headroomDb)) != null }
            click("Información de Margen automático"); screenshot("informacion-margen"); click("Cerrar información")
            click("Modo Recomendado"); selected(SoundMode.RECOMMENDED)
            click("Modo Personalizado"); selected(SoundMode.CUSTOM)
            check(main { vm.sound.value.bass == 4f && vm.sound.value.air == 3f && vm.sound.value.trim == -5f })
            pass("All three sliders persist; actual headroom updates; values survive mode changes")
            click("Restablecer mejora")
            until("Reset semantics changed") { main { vm.sound.value == SoundSettings(enabled = true) } }
            pass("Reset retains original defaults and active Custom mode")
            click("Modo Recomendado"); selected(SoundMode.RECOMMENDED)
            back(); click("Opciones de biblioteca"); click("Sonido"); selected(SoundMode.RECOMMENDED)
            check(PlaybackService.readSound(context) == main { vm.sound.value })
            pass("Confirmed preferences survive closing/reopening the sheet")

            // Exercise the real reusable content in a constrained physical-device host.
            main {
                (activity as ComponentActivity).setContent {
                    val sound by vm.sound.collectAsState()
                    MusicTheme {
                        val density = LocalDensity.current.density
                        CompositionLocalProvider(LocalDensity provides Density(density, 1.3f)) {
                            Box(Modifier.fillMaxSize().background(MusicStyle.Background), contentAlignment = Alignment.Center) {
                                Box(Modifier.requiredSize(320.dp, 400.dp).background(MusicStyle.SoundPanel)) {
                                    SoundSettingsSheet(sound, true, vm::setSound)
                                }
                            }
                        }
                    }
                }
            }
            until("Small content did not mount") { find("Ajustes de sonido") != null }
            selected(SoundMode.RECOMMENDED)
            screenshot("compacto-320-fuente130")
            click("Modo Personalizado")
            until("Small custom mode not active") { main { vm.sound.value.mode == SoundMode.CUSTOM } }
            fun scrollToReset() {
                fun scrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                    if (node.isScrollable) return node
                    for (i in 0 until node.childCount) node.getChild(i)?.let { scrollable(it)?.let { found -> return found } }
                    return null
                }
                repeat(5) {
                    if (find("Restablecer mejora") != null) return
                    test.uiAutomation.rootInActiveWindow?.let(::scrollable)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    SystemClock.sleep(250)
                }
                error("Small Custom panel cannot reach reset")
            }
            scrollToReset(); screenshot("compacto-personalizado-scroll")
            check(bounds("Restablecer mejora").width() > 0)
            pass("320×400 dp physical host, 130% font: compact modes fit; expanded Custom scrolls to every setting")
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == initialVolume)
            check(main { vm.playback.value.queue.map { it.mediaId } } == queueBefore)
            if (queueBefore.isNotEmpty()) until("Playback did not continue") { main { vm.playback.value.playing } }
            observations.put(JSONObject().put("compactTitleTopPx", compactTop).put("recommendedTitleTopPx", recommendedTop)
                .put("volumeBefore", initialVolume).put("volumeAfter", audio.getStreamVolume(AudioManager.STREAM_MUSIC))
                .put("queueUnchanged", true).put("sourceSound", before.toString()))
            test.sendStatus(0, Bundle().apply { putString("sound_settings_json", observations.toString())
                putString("screenshots", screenshots.absolutePath) })
            pass("Queue and system volume unchanged; original sound settings restored after test")
        } catch (failure: Throwable) {
            runCatching { screenshot("fallo-prueba") }
            throw failure
        } finally {
            main { vm.setSound(before) }
            until("Original sound not restored") { PlaybackService.readSound(context) == before }
            main { if (vm.playback.value.playing != initialPlaying) vm.togglePlay() }
        }
    }
}
