package dev.aiko.music

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.media.AudioManager
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.session.*
import dev.aiko.music.data.Library
import dev.aiko.music.playback.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Runs only on an explicitly connected physical device; no emulator configuration. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MusicInstrumentation : Instrumentation() {
    private var navigationOnly = false
    private var nowPlayingOnly = false
    private var nowPlayingLayoutOnly = false
    private var pagerOnly = false
    private var seekOnly = false
    private var audioOnly = false
    private var bluetoothOnly = false
    private var screenOffPrepOnly = false
    private var trackControlsOnly = false
    private var soundUiOnly = false
    private var soundPersistencePhase: String? = null
    private var pullRefreshOnly = false
    override fun onCreate(arguments: Bundle?) {
        navigationOnly = arguments?.getString("navigationOnly") == "true"
        nowPlayingOnly = arguments?.getString("nowPlayingOnly") == "true"
        nowPlayingLayoutOnly = arguments?.getString("nowPlayingLayoutOnly") == "true"
        pagerOnly = arguments?.getString("pagerOnly") == "true"
        seekOnly = arguments?.getString("seekOnly") == "true"
        audioOnly = arguments?.getString("audioOnly") == "true"
        bluetoothOnly = arguments?.getString("bluetoothOnly") == "true"
        screenOffPrepOnly = arguments?.getString("screenOffPrepOnly") == "true"
        trackControlsOnly = arguments?.getString("trackControlsOnly") == "true"
        soundUiOnly = arguments?.getString("soundUiOnly") == "true"
        soundPersistencePhase = arguments?.getString("soundPersistencePhase")
        pullRefreshOnly = arguments?.getString("pullRefreshOnly") == "true"
        super.onCreate(arguments); start()
    }
    private fun <T> main(block: () -> T): T {
        val result = AtomicReference<T>()
        val error = AtomicReference<Throwable>()
        runOnMainSync { try { result.set(block()) } catch (t: Throwable) { error.set(t) } }
        error.get()?.let { throw it }
        return result.get()
    }
    private fun until(message: String, test: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) { if (main(test)) return; SystemClock.sleep(100) }
        error(message)
    }
    override fun onStart() {
        val report = StringBuilder()
        var controller: MediaController? = null
        val originalSound = PlaybackService.readSound(targetContext)
        fun pass(name: String) { report.appendLine("PASS $name"); sendStatus(0, Bundle().apply { putString("stream", "PASS $name\n") }) }
        var success = false
        try {
            check(!android.os.Build.FINGERPRINT.contains("generic")) { "Physical Android required" }
            val songs = Library.read(targetContext)
            check(songs.size >= 3) { "At least three local songs required" }
            pass("MediaStore: ${songs.size} local songs")
            if (pullRefreshOnly) {
                val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                PullRefreshDeviceChecks.run(this, activity, ::pass)
                success = true
                return
            }
            soundPersistencePhase?.let { phase ->
                val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                SoundPersistenceDeviceChecks.run(this, activity, phase, ::pass)
                success = true
                return
            }
            if (soundUiOnly) {
                val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                SoundSettingsDeviceChecks.run(this, activity, ::pass)
                success = true
                return
            }
            if (trackControlsOnly) {
                val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                TrackNavigationDeviceChecks.run(this, activity, songs, ::pass)
                success = true
                return
            }
            if (screenOffPrepOnly) {
                startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                ScreenOffReferenceChecks.prepare(this, songs, ::pass)
                success = true
                return
            }
            if (audioOnly || bluetoothOnly) {
                startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                AudioQualityDeviceChecks.run(this, songs, ::pass, bluetoothOnly)
                success = true
                return
            }
            if (seekOnly) {
                val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                SeekBarDeviceChecks.run(this, activity, ::pass)
                success = true
                return
            }
            if (pagerOnly) {
                val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                LibraryPagerDeviceChecks.run(this, activity, ::pass)
                success = true
                return
            }
            if (nowPlayingOnly || nowPlayingLayoutOnly) {
                val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                NowPlayingDeviceChecks.run(this, activity, songs, ::pass, nowPlayingLayoutOnly)
                success = true
                return
            }
            if (navigationOnly) {
                val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                LibraryNavigationDeviceChecks.run(this, activity, ::pass)
                success = true
                return
            }
            PlaylistDeviceChecks.run(targetContext, songs, ::pass)
            // Android 15 requires a visible Activity or an already foreground service to obtain focus.
            val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val future = main { MediaController.Builder(targetContext,
                SessionToken(targetContext, ComponentName(targetContext, PlaybackService::class.java))).buildAsync() }
            val p = future.get(15, TimeUnit.SECONDS)
            controller = p
            val original = main { p.sendCustomCommand(SessionCommand(PlaybackService.SOUND_COMMAND, Bundle.EMPTY), PlaybackService.soundBundle(SoundSettings())) }
            check(original.get(15, TimeUnit.SECONDS).resultCode == SessionResult.RESULT_SUCCESS)
            val tracks = songs.filter { it.duration > 60000 }.take(3)
            check(tracks.size == 3)
            main { p.setMediaItems(tracks.map { it.mediaItem() }); p.prepare(); p.play() }
            until("Original playback never became ready") { p.isPlaying && p.currentPosition > 1200 }
            check(main { p.playerError == null })
            pass("Local decode and advancing playback position")
            main { p.pause() }
            until("Pause failed") { !p.playWhenReady }
            val position = main { p.currentPosition }
            SystemClock.sleep(600)
            check(kotlin.math.abs(main { p.currentPosition } - position) < 120)
            pass("Pause holds position")
            main { p.seekTo(15000); p.play() }
            until("Seek failed") { p.isPlaying && p.currentPosition >= 15000 }
            main { p.seekToNextMediaItem() }
            until("Next failed") { p.currentMediaItemIndex == 1 && p.isPlaying }
            main { p.seekToPreviousMediaItem() }
            until("Previous failed") { p.currentMediaItemIndex == 0 && p.isPlaying }
            pass("Seek, next and previous")
            main {
                p.addMediaItem(tracks[0].mediaItem()); p.moveMediaItem(3, 1)
                check(p.mediaItemCount == 4 && p.getMediaItemAt(1).mediaId == tracks[0].uri.toString())
                p.removeMediaItem(1); p.shuffleModeEnabled = true; p.repeatMode = Player.REPEAT_MODE_ALL
            }
            until("Queue edit failed") { p.mediaItemCount == 3 && p.shuffleModeEnabled && p.repeatMode == Player.REPEAT_MODE_ALL }
            pass("Queue insert, reorder, remove; shuffle and repeat")
            fun sound(s: SoundSettings) {
                val f = main { p.sendCustomCommand(SessionCommand(PlaybackService.SOUND_COMMAND, Bundle.EMPTY), PlaybackService.soundBundle(s)) }
                check(f.get(15, TimeUnit.SECONDS).resultCode == SessionResult.RESULT_SUCCESS)
                until("Sound mode switch interrupted playback") { p.isPlaying && p.playerError == null && p.mediaItemCount == 3 }
            }
            val initialSamples = ToneProcessor.processedSamples
            sound(SoundSettings(enabled = true, bass = 6f, air = 6f))
            SystemClock.sleep(1500)
            check(main { p.playerError == null && p.isPlaying })
            check(ToneProcessor.processedSamples > initialSamples + 1000) { "DSP did not process PCM" }
            sound(SoundSettings(enabled = true, bass = 1f, air = 2f, trim = -3f))
            pass("Enhanced PCM processor and live tone adjustment")
            val recommendedFrames = RecommendedProcessor.processedFrames
            sound(SoundSettings(enabled = true, recommended = true))
            val savedIndex = main { p.currentMediaItemIndex }
            val recommendedPosition = main { p.currentPosition }
            SystemClock.sleep(8000)
            check(main { p.isPlaying && p.playerError == null && p.currentPosition > recommendedPosition + 6000 })
            check(RecommendedProcessor.processedFrames > recommendedFrames + 10000)
            check(PlaybackService.readSound(targetContext).mode == SoundMode.RECOMMENDED)
            pass("Recommended DSP active, persisted and advancing without playback error")
            val audioManager = targetContext.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
            fun key(code: Int) {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
            }
            key(KeyEvent.KEYCODE_MEDIA_PAUSE)
            until("Media pause failed") { !p.playWhenReady }
            key(KeyEvent.KEYCODE_MEDIA_PLAY)
            until("Media play failed") { p.isPlaying }
            key(KeyEvent.KEYCODE_MEDIA_NEXT)
            until("Media next failed") { p.currentMediaItemIndex != savedIndex }
            main { p.shuffleModeEnabled = false; p.seekToDefaultPosition(1) }
            until("Index setup failed") { p.currentMediaItemIndex == 1 }
            key(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            until("Media previous failed") { p.currentMediaItemIndex == 0 }
            pass("Android media keys: play, pause, next and previous")
            sound(SoundSettings(enabled = false))
            pass("Return to Original preserving queue and playback")
            uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()
            val backgroundPosition = main { p.currentPosition }
            SystemClock.sleep(3000)
            check(main { p.isPlaying && p.currentPosition > backgroundPosition + 2000 })
            pass("Service playback advances without Activity")
            uiAutomation.executeShellCommand("am start -n dev.aiko.music/.MainActivity").close()
            MiniPlayerDeviceChecks.run(this, activity, p, songs, ::pass)
            LibraryNavigationDeviceChecks.run(this, activity, ::pass)
            NowPlayingDeviceChecks.run(this, activity, songs, ::pass)
            LibraryPagerDeviceChecks.run(this, activity, ::pass)
            SeekBarDeviceChecks.run(this, activity, ::pass)
            main { p.pause(); p.shuffleModeEnabled = false; p.repeatMode = Player.REPEAT_MODE_OFF }
            success = true
        } catch (t: Throwable) {
            report.appendLine("FAIL ${t.stackTraceToString()}")
        } finally {
            controller?.let { p ->
                runCatching {
                    val f = main { p.pause(); p.sendCustomCommand(SessionCommand(PlaybackService.SOUND_COMMAND, Bundle.EMPTY), PlaybackService.soundBundle(originalSound)) }
                    f.get(10, TimeUnit.SECONDS)
                    main { p.release() }
                }
            }
            targetContext.filesDir.resolve("physical-results.txt").writeText(report.toString())
            finish(if (success) -1 else 0, Bundle().apply { putString("stream", report.toString()) })
        }
    }
}
