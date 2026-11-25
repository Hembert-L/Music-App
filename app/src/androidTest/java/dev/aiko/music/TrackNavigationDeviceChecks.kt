package dev.aiko.music

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.*
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.aiko.music.data.Song
import dev.aiko.music.playback.PlaybackService
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/** Regression checks for explicit skips on the connected physical Android. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object TrackNavigationDeviceChecks {
    fun run(test: Instrumentation, activity: Activity, songs: List<Song>, pass: (String) -> Unit) {
        fun <T> main(block: () -> T): T {
            val result = AtomicReference<T>(); val failure = AtomicReference<Throwable>()
            test.runOnMainSync { try { result.set(block()) } catch (t: Throwable) { failure.set(t) } }
            failure.get()?.let { throw it }; return result.get()
        }
        fun until(message: String, predicate: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + 15000
            while (SystemClock.elapsedRealtime() < deadline) { if (main(predicate)) return; SystemClock.sleep(40) }
            error(message)
        }
        fun root(view: View): ViewRootForTest? {
            if (view is ViewRootForTest) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) root(view.getChildAt(i))?.let { return it }
            return null
        }
        val owner = main { root(activity.window.decorView) ?: error("No Compose root") }
        fun click(label: String) {
            fun node() = owner.semanticsOwner.getAllSemanticsNodes(mergingEnabled = true).firstOrNull {
                it.boundsInRoot.width > 0 && it.boundsInRoot.height > 0 && it.config.contains(SemanticsActions.OnClick) &&
                    (label in (it.config.getOrNull(SemanticsProperties.ContentDescription) ?: emptyList()) ||
                        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true)
            }
            until("Missing $label") { node() != null }
            main { check(node()!!.config[SemanticsActions.OnClick].action!!.invoke()) }
        }
        val context = test.targetContext
        val audio = context.getSystemService(AudioManager::class.java)
        val volumeBefore = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val soundBefore = PlaybackService.readSound(context)
        val bluetooth = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        }
        val future = main { MediaController.Builder(context, SessionToken(context,
            ComponentName(context, PlaybackService::class.java))).buildAsync() }
        val player = future.get(15, TimeUnit.SECONDS)
        val observations = JSONArray()
        val savedItems = main { (0 until player.mediaItemCount).map(player::getMediaItemAt) }
        val savedIndex = main { player.currentMediaItemIndex }
        val savedPosition = main { player.currentPosition }
        val savedShuffle = main { player.shuffleModeEnabled }
        val savedRepeat = main { player.repeatMode }
        val savedPlaying = main { player.playWhenReady }
        val tracks = songs.filter { it.duration > 60000 }.take(4)
        check(tracks.size == 4)
        fun setup(index: Int, paused: Boolean = true) {
            main { player.pause(); player.seekTo(index, 17000L); player.prepare() }
            until("Setup index=$index") { player.currentMediaItemIndex == index &&
                player.playbackState == Player.STATE_READY && !player.playWhenReady && abs(player.currentPosition - 17000) < 500 }
            if (!paused) { main { player.play() }; until("Setup play") { player.isPlaying } }
        }
        fun selected(name: String, index: Int) {
            until("$name did not select/play index=$index") { player.currentMediaItemIndex == index && player.isPlaying }
            SystemClock.sleep(220)
            val position = main { player.currentPosition }
            check(position in 0..2500) { "$name inherited position=$position" }
            observations.put(JSONObject().put("check", name).put("index", index).put("positionMs", position)
                .put("playing", main { player.isPlaying }))
            pass("$name: actual destination starts at zero and plays")
        }
        fun key(code: Int) = main {
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
        try {
            main { player.pause(); player.shuffleModeEnabled = false; player.repeatMode = Player.REPEAT_MODE_OFF
                player.setMediaItems(tracks.map(Song::mediaItem)); player.prepare() }
            setup(1); click("Biblioteca"); click("Canción anterior"); selected("Mini previous after paused 17s", 0)
            setup(0); click("Siguiente canción"); selected("Mini next while paused", 1)
            click("Reproduciendo")
            setup(2, paused = false); click("Canción anterior"); selected("Full player previous after 17s playing", 1)
            setup(1); click("Canción siguiente"); selected("Full player next while paused", 2)

            setup(2); main { player.seekToPrevious() }; selected("Session previous while paused after 17s", 1)
            setup(1); main { player.seekToPreviousMediaItem() }; selected("Session previous-item while paused", 0)
            setup(0); main { player.seekToNext() }; selected("Session next while paused", 1)
            setup(1); main { player.seekToNextMediaItem() }; selected("Session next-item while paused", 2)

            main { player.shuffleModeEnabled = true; player.repeatMode = Player.REPEAT_MODE_ALL }
            setup(1)
            val shuffledNext = main { player.nextMediaItemIndex }
            main { player.seekToNext() }; selected("Shuffle next while paused", shuffledNext)
            main { player.pause(); player.seekTo(17000L) }
            main { player.seekToPrevious() }; selected("Shuffle previous follows the same queue history", 1)
            main { player.shuffleModeEnabled = false; player.repeatMode = Player.REPEAT_MODE_ONE }
            setup(1); main { player.seekToNext() }; selected("Manual next with repeat-one", 2)
            setup(2); main { player.seekToPrevious() }; selected("Manual previous with repeat-one", 1)
            main { player.repeatMode = Player.REPEAT_MODE_ALL }
            setup(0); main { player.seekToPrevious() }; selected("Repeat-all previous wraps", tracks.lastIndex)
            main { player.pause() }; main { player.seekToNext() }; selected("Repeat-all next wraps", 0)

            main { player.repeatMode = Player.REPEAT_MODE_OFF }
            setup(0); main { player.seekToPrevious() }; SystemClock.sleep(250)
            check(main { player.currentMediaItemIndex == 0 && !player.playWhenReady && abs(player.currentPosition - 17000) < 500 })
            setup(tracks.lastIndex); main { player.seekToNext() }; SystemClock.sleep(250)
            check(main { player.currentMediaItemIndex == tracks.lastIndex && !player.playWhenReady && abs(player.currentPosition - 17000) < 500 })
            pass("Queue boundaries without repeat: no restart, no unexpected playback")
            setup(1); main { player.seekTo(9000L) }
            until("Ordinary seek changed pause") { !player.playWhenReady && abs(player.currentPosition - 9000) < 500 }
            pass("Progress seeking keeps pause; only explicit track skips start playback")

            setup(2); key(KeyEvent.KEYCODE_MEDIA_PREVIOUS); selected("Android media previous while paused after 17s", 1)
            setup(1); key(KeyEvent.KEYCODE_MEDIA_NEXT); selected("Android media next while paused", 2)
            setup(1)
            test.uiAutomation.executeShellCommand("input keyevent KEYCODE_SLEEP").use {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { input -> input.readBytes() }
            }
            key(KeyEvent.KEYCODE_MEDIA_PREVIOUS); selected("Screen-off media previous while paused", 0)
            main { player.pause() }; key(KeyEvent.KEYCODE_MEDIA_NEXT); selected("Screen-off media next while paused", 1)
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == volumeBefore)
            check(PlaybackService.readSound(context) == soundBefore)
            val report = JSONObject().put("version", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
                .put("bluetoothConnected", bluetooth).put("soundUnchanged", true).put("volumeBefore", volumeBefore)
                .put("volumeAfter", audio.getStreamVolume(AudioManager.STREAM_MUSIC)).put("checks", observations)
            context.filesDir.resolve("track-controls.json").writeText(report.toString(2))
            test.sendStatus(0, Bundle().apply { putString("track_controls_json", report.toString()) })
            pass("Sound settings and system volume preserved; prior queue restored afterward")
        } finally {
            main {
                player.pause(); player.shuffleModeEnabled = savedShuffle; player.repeatMode = savedRepeat
                if (savedItems.isNotEmpty()) {
                    player.setMediaItems(savedItems, savedIndex.coerceIn(savedItems.indices), savedPosition.coerceAtLeast(0))
                    player.prepare(); player.playWhenReady = savedPlaying
                } else player.clearMediaItems()
                player.release()
            }
        }
    }
}
