package dev.aiko.music

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dev.aiko.music.playback.PlaybackService
import dev.aiko.music.playback.SoundMode
import dev.aiko.music.playback.SoundSettings
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

/** Separate invocations intentionally preserve test values across a real process stop/relaunch. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object SoundPersistenceDeviceChecks {
    fun run(test: Instrumentation, activity: Activity, phase: String, pass: (String) -> Unit) {
        fun <T> main(block: () -> T): T {
            val value = AtomicReference<T>(); val failure = AtomicReference<Throwable>()
            test.runOnMainSync { try { value.set(block()) } catch (t: Throwable) { failure.set(t) } }
            failure.get()?.let { throw it }; return value.get()
        }
        fun until(message: String, predicate: () -> Boolean) {
            val end = SystemClock.elapsedRealtime() + 15000
            while (SystemClock.elapsedRealtime() < end) { if (predicate()) return; SystemClock.sleep(70) }
            error(message)
        }
        fun findIn(node: AccessibilityNodeInfo, label: String, slider: Boolean): AccessibilityNodeInfo? {
            if (node.isVisibleToUser && slider && node.rangeInfo != null && findIn(node, label, false) != null) return node
            if (node.isVisibleToUser && !slider && (node.text?.toString() == label || node.contentDescription?.toString() == label)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { findIn(it, label, slider)?.let { found -> return found } }
            return null
        }
        fun find(label: String, slider: Boolean = false) = test.uiAutomation.rootInActiveWindow?.let { findIn(it, label, slider) }
        fun click(label: String) {
            until("Missing $label") { find(label) != null }
            var node = find(label)!!
            while (!node.isClickable) node = node.parent ?: error("Not clickable: $label")
            check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        val context = test.targetContext
        val vm = main { ViewModelProvider(activity as ViewModelStoreOwner)[MusicViewModel::class.java] }
        until("No player connection") { main { vm.playback.value.connected } }
        val audio = context.getSystemService(AudioManager::class.java)
        val stateFile = context.filesDir.resolve("custom-persistence-baseline.json")
        fun soundJson(s: SoundSettings) = JSONObject().put("enabled", s.enabled).put("recommended", s.recommended)
            .put("bass", s.bass).put("air", s.air).put("trim", s.trim)
        if (phase == "prepare") {
            val sound = main { vm.sound.value }
            JSONObject().put("sound", soundJson(sound)).put("playing", main { vm.playback.value.playing })
                .put("volume", audio.getStreamVolume(AudioManager.STREAM_MUSIC))
                .put("queue", JSONArray(main { vm.playback.value.queue.map { it.mediaId } }))
                .toString(2).let(stateFile::writeText)
        }
        check(stateFile.exists()) { "Prepare phase required before verification" }
        val baseline = JSONObject(stateFile.readText())
        val original = baseline.getJSONObject("sound").let {
            SoundSettings(it.getBoolean("enabled"), it.getDouble("bass").toFloat(), it.getDouble("air").toFloat(),
                it.getDouble("trim").toFloat(), it.getBoolean("recommended"))
        }
        val seeded = SoundSettings(true, 4f, 3f, -5f, false)
        fun values(s: SoundSettings, expected: SoundSettings) = s.bass == expected.bass && s.air == expected.air && s.trim == expected.trim
        fun assertStored(expected: SoundSettings, mode: SoundMode) {
            until("Stored or ViewModel values differ: $phase / $mode") {
                val persisted = PlaybackService.readSound(context)
                val shown = main { vm.sound.value }
                persisted.mode == mode && shown.mode == mode && values(persisted, expected) && values(shown, expected)
            }
        }
        fun selected(mode: SoundMode) {
            val label = when(mode) { SoundMode.ORIGINAL -> "Original"; SoundMode.RECOMMENDED -> "Recomendado"; else -> "Personalizado" }
            until("Switch state did not settle: $label") { find("Modo $label")?.isChecked == true }
            until("Panel expansion did not settle") { (find("Graves") != null) == (mode == SoundMode.CUSTOM) }
        }
        fun assertSliders(expected: SoundSettings) {
            for ((name, value) in listOf("Graves" to expected.bass, "Brillo" to expected.air, "Nivel adicional" to expected.trim)) {
                until("Visible slider/value missing: $name / $value") { find(name, true) != null && find("%.1f dB".format(value)) != null }
                val range = requireNotNull(find(name, true)?.rangeInfo)
                test.sendStatus(0, Bundle().apply { putString("slider_native", "$name: min=${range.min}, max=${range.max}, current=${range.current}; visible=$value dB") })
            }
        }
        fun change(name: String, value: Float) {
            fun field(s: SoundSettings) = when(name) { "Graves" -> s.bass; "Brillo" -> s.air; else -> s.trim }
            if (field(PlaybackService.readSound(context)) == value && main { field(vm.sound.value) == value }) return
            until("Missing slider $name") { find(name, true) != null }
            check(find(name, true)!!.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
                Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, value) }))
            until("Slider did not save: $name") {
                field(PlaybackService.readSound(context)) == value
            }
        }
        fun screenshot() {
            SystemClock.sleep(400)
            val dir = context.getExternalFilesDir(null)!!.resolve("sound-persistence").apply { mkdirs() }
            val bitmap = requireNotNull(test.uiAutomation.takeScreenshot())
            dir.resolve("$phase.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }

        // Check cold-start preferences BEFORE clicking any mode or changing any setting.
        when(phase) {
            "afterCustomRestart" -> assertStored(seeded, SoundMode.CUSTOM)
            "afterRecommendedRestart" -> assertStored(seeded, SoundMode.RECOMMENDED)
            "finalVerify" -> assertStored(original, SoundMode.RECOMMENDED)
            "prepare", "restore" -> Unit
            else -> error("Unknown persistence phase: $phase")
        }
        if (main { vm.playback.value.playing }) main { vm.togglePlay() }
        if (find("Ajustes de sonido") != null) test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        if (find("Opciones de biblioteca") == null) click("Biblioteca")
        click("Opciones de biblioteca"); click("Sonido")
        until("No sound settings") { find("Ajustes de sonido") != null }
        when(phase) {
            "prepare" -> {
                click("Modo Personalizado"); selected(SoundMode.CUSTOM)
                change("Graves", seeded.bass); change("Brillo", seeded.air); change("Nivel adicional", seeded.trim)
                assertStored(seeded, SoundMode.CUSTOM); assertSliders(seeded)
                pass("Distinct Custom values 4/3/-5 dB saved from the real sliders")
            }
            "afterCustomRestart" -> {
                selected(SoundMode.CUSTOM); assertSliders(seeded)
                pass("Cold start: Custom mode and all three slider values restored before any edits")
                for ((label, mode) in listOf("Original" to SoundMode.ORIGINAL, "Recomendado" to SoundMode.RECOMMENDED)) {
                    click("Modo $label"); assertStored(seeded, mode); selected(mode)
                    click("Modo Personalizado"); assertStored(seeded, SoundMode.CUSTOM); selected(SoundMode.CUSTOM); assertSliders(seeded)
                    pass("Custom → $label → Custom preserves all three values and their visible controls")
                }
                click("Modo Recomendado"); assertStored(seeded, SoundMode.RECOMMENDED); selected(SoundMode.RECOMMENDED)
            }
            "afterRecommendedRestart" -> {
                selected(SoundMode.RECOMMENDED)
                pass("Cold start: Recommended restored, with distinct Custom values still saved and hidden")
                click("Modo Personalizado"); assertStored(seeded, SoundMode.CUSTOM); selected(SoundMode.CUSTOM); assertSliders(seeded)
                pass("Custom values visible and correct after restarting in Recommended")
            }
            "restore" -> {
                click("Modo Personalizado"); selected(SoundMode.CUSTOM)
                change("Graves", original.bass); change("Brillo", original.air); change("Nivel adicional", original.trim)
                click("Modo Recomendado"); assertStored(original, SoundMode.RECOMMENDED); selected(SoundMode.RECOMMENDED)
                pass("Original personal values restored from baseline; Recommended selected")
            }
            "finalVerify" -> {
                selected(SoundMode.RECOMMENDED)
                pass("Final cold start: original personal values retained and Recommended selected")
            }
        }
        check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == baseline.getInt("volume")) { "System volume changed during test" }
        check(main { vm.playback.value.queue.map { it.mediaId } } ==
            baseline.getJSONArray("queue").let { q -> (0 until q.length()).map(q::getString) }) { "Queue changed" }
        screenshot()
        val report = JSONObject().put("phase", phase).put("pid", Process.myPid())
            .put("version", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            .put("sound", soundJson(PlaybackService.readSound(context))).put("baselineSound", soundJson(original))
            .put("volume", audio.getStreamVolume(AudioManager.STREAM_MUSIC)).put("queueUnchanged", true)
        test.sendStatus(0, Bundle().apply { putString("custom_persistence_json", report.toString()) })
        if (phase == "finalVerify" && baseline.getBoolean("playing")) main { vm.togglePlay() }
    }
}
