package dev.aiko.music

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.session.*
import dev.aiko.music.data.Song
import dev.aiko.music.playback.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.log10

/** Real files + real Android decoder, followed by live speaker/Bluetooth session checks. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object AudioQualityDeviceChecks {
    fun run(test: Instrumentation, songs: List<Song>, pass: (String) -> Unit, bluetoothPhase: Boolean = false) {
        val context = test.targetContext
        fun <T> main(block: () -> T): T {
            val result = AtomicReference<T>(); val failure = AtomicReference<Throwable>()
            test.runOnMainSync { try { result.set(block()) } catch (t: Throwable) { failure.set(t) } }
            failure.get()?.let { throw it }; return result.get()
        }
        fun until(message: String, seconds: Int = 20, block: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + seconds * 1000
            while (SystemClock.elapsedRealtime() < deadline) { if (main(block)) return; SystemClock.sleep(50) }
            error(message)
        }
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val externalTypes = setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET)
        val bluetoothTypes = setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET)
        if (bluetoothPhase) {
            val headphones = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { it.type in bluetoothTypes }
            check(headphones.isNotEmpty()) { "Connect Bluetooth headphones for this phase" }
            pass("Bluetooth phase: ${headphones.joinToString { it.productName.toString() }}; STREAM_MUSIC=$volume (unchanged)")
        } else {
            check(audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).none { it.type in externalTypes }) { "Disconnect headphones for speaker phase" }
            pass("Speaker phase: no connected headphones; STREAM_MUSIC=$volume (unchanged)")
        }
        fun verifyBluetoothRoute() {
            if (!bluetoothPhase) return
            val dump = ParcelFileDescriptor.AutoCloseInputStream(test.uiAutomation.executeShellCommand("dumpsys audio")).bufferedReader().use { it.readText() }
            val active = dump.lineSequence().firstOrNull {
                "u/pid:${context.applicationInfo.uid}/" in it && "state:started" in it
            } ?: error("No started AudioTrack for Music")
            val id = Regex("deviceId:(\\d+)").find(active)?.groupValues?.get(1)?.toInt() ?: error("No routed device")
            val device = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.id == id }
                ?: error("Routed device not available")
            check(device.type in bluetoothTypes && "USAGE_MEDIA" in active && "CONTENT_TYPE_MUSIC" in active) { "Music is not routed to media Bluetooth: $active" }
            context.filesDir.resolve("audio-bluetooth-route.json").writeText(JSONObject()
                .put("deviceId", id).put("deviceName", device.productName).put("deviceType", device.type)
                .put("musicVolume", audio.getStreamVolume(AudioManager.STREAM_MUSIC)).put("audioTrack", active.trim()).toString(2))
        }
        val originalSound = PlaybackService.readSound(context)
        val future = main { MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync() }
        val p = future.get(15, TimeUnit.SECONDS)
        fun sound(settings: SoundSettings) {
            val result = main { p.sendCustomCommand(SessionCommand(PlaybackService.SOUND_COMMAND, Bundle.EMPTY), PlaybackService.soundBundle(settings)) }
            check(result.get(15, TimeUnit.SECONDS).resultCode == SessionResult.RESULT_SUCCESS)
        }
        try {
            main { p.pause() }
            sound(originalSound.copy(enabled = false)) // Cancel background work during offline reference measurements.
            // Public playlist references matched by real metadata + duration, avoiding extended copies.
            val tracks = listOfNotNull(
                songs.firstOrNull { it.title == "MONTAGEM POUCO (Slowed)" && it.artist == "Repsaj" && abs(it.duration - 79741) < 1000 },
                songs.firstOrNull { it.title == "MONTAGEM HIKARI" && it.artist == "BellyJay" && abs(it.duration - 89985) < 1000 },
                songs.firstOrNull { it.title == "MONTAGEM NO VENTO (Slowed)" && it.artist == "TRXVELER, TESSAI" && abs(it.duration - 105682) < 1000 }
            )
            check(tracks.size >= 3)
            val results = JSONArray()
            for (song in tracks) {
                val started = SystemClock.elapsedRealtime()
                val before = LoudnessAnalysis.measure(context, song.uri)
                val after = LoudnessAnalysis.measure(context, song.uri, before.gainDb)
                check(before.lufs.isFinite() && after.lufs.isFinite())
                check(abs(after.lufs - (before.lufs + before.gainDb)) < .12) { "Track gain mismatch ${song.title}" }
                check(after.peak <= RecommendedEngine.CEILING + .000002) { "Peak overflow ${song.title}" }
                check(after.peak <= .7945) { "2 dB margin missing ${song.title}" }
                results.put(JSONObject().put("title", song.title).put("uri", song.uri)
                    .put("recommendedBeforeLufs", before.lufs).put("trackGainDb", before.gainDb)
                    .put("normalizedLufs", after.lufs).put("normalizedSamplePeakDbfs", 20 * log10(after.peak))
                    .put("twoPassAnalysisMs", SystemClock.elapsedRealtime() - started))
                context.filesDir.resolve("audio-reference-results.json").writeText(results.toString(2))
                pass("Complete real-file analysis: ${song.title}: ${"%.2f".format(before.lufs)} → ${"%.2f".format(after.lufs)} LUFS; gain ${"%.2f".format(before.gainDb)} dB")
            }
            context.filesDir.resolve("audio-reference-results.json").writeText(results.toString(2))
            sound(originalSound.copy(enabled = true, recommended = true))
            // Fresh URI for the SAME local file: exercise a cold cache without deleting user data.
            val coldUri = tracks[0].uri.buildUpon().appendQueryParameter("music_level_test", SystemClock.elapsedRealtime().toString()).build()
            val coldItem = tracks[0].mediaItem().buildUpon().setMediaId(coldUri.toString()).setUri(coldUri).build()
            main { p.setMediaItems(listOf(coldItem)); p.prepare(); p.play() }
            until("Cold playback did not start") { p.isPlaying && p.currentPosition > 1200 && LoudnessAnalysis.lastApplied?.uri == coldUri.toString() }
            verifyBluetoothRoute()
            val coldSelection = requireNotNull(LoudnessAnalysis.lastApplied)
            check(!coldSelection.cached && coldSelection.gainDb == TrackLoudness.UNCALIBRATED_GAIN_DB)
            val initialChanges = RecommendedProcessor.gainChanges
            val initialPosition = main { p.currentPosition }
            until("Cold analysis never finished", 60) { LoudnessAnalysis.lastMeasured?.uri == coldUri.toString() }
            SystemClock.sleep(1500)
            check(main { p.isPlaying && p.currentPosition > initialPosition && p.playerError == null })
            check(LoudnessAnalysis.lastApplied == coldSelection) { "Late analysis changed selected gain" }
            check(RecommendedProcessor.lastProcessedGainDb == coldSelection.gainDb && RecommendedProcessor.gainChanges == initialChanges) { "Late analysis changed PCM gain" }
            context.filesDir.resolve("audio-startup-results.json").writeText(JSONObject()
                .put("uri", coldUri).put("initialGainDb", coldSelection.gainDb)
                .put("afterAnalysisGainDb", RecommendedProcessor.lastProcessedGainDb)
                .put("gainChangesWhilePlaying", RecommendedProcessor.gainChanges - initialChanges)
                .put("measuredGainForFuturePlaybackDb", requireNotNull(LoudnessAnalysis.lastMeasured).level.gainDb).toString(2))
            pass("Cold start: gain fixed from first playback through completed analysis; zero late PCM gain changes")
            main { p.pause(); p.setMediaItems(listOf(tracks[1].mediaItem())) }
            until("Intermediate selection failed") { LoudnessAnalysis.lastApplied?.uri == tracks[1].uri.toString() }
            main { p.setMediaItems(listOf(coldItem)); p.prepare(); p.play() }
            until("Warm start not using cached level") { p.isPlaying && LoudnessAnalysis.lastApplied?.let { it.uri == coldUri.toString() && it.cached && it.gainDb == RecommendedProcessor.lastProcessedGainDb } == true }
            val warmSelection = requireNotNull(LoudnessAnalysis.lastApplied)
            check(abs(warmSelection.gainDb - results.getJSONObject(0).getDouble("trackGainDb")) < .05)
            val warmChanges = RecommendedProcessor.gainChanges
            SystemClock.sleep(3000)
            check(RecommendedProcessor.gainChanges == warmChanges && RecommendedProcessor.lastProcessedGainDb == warmSelection.gainDb)
            pass("Warm start: saved level applied at playback start and remains steady")
            val frames = RecommendedProcessor.processedFrames
            main { p.setMediaItems(tracks.map(Song::mediaItem)); p.prepare(); p.play() }
            until("Recommended did not play") { p.isPlaying && p.currentPosition > 1500 && p.playerError == null }
            until("No selected level") { LoudnessAnalysis.lastApplied?.uri == tracks[0].uri.toString() }
            val live = requireNotNull(LoudnessAnalysis.lastApplied)
            until("Full analysis did not finish", 60) { LoudnessAnalysis.lastMeasured?.uri == tracks[0].uri.toString() }
            check(RecommendedProcessor.processedFrames > frames + 10000)
            check(LoudnessAnalysis.lastApplied == live && RecommendedProcessor.lastProcessedGainDb == live.gainDb)
            pass("Recommended real PCM advances without applying analysis results mid-song")
            main { p.pause() }
            val position = main { p.currentPosition }
            SystemClock.sleep(600)
            check(abs(main { p.currentPosition } - position) < 100)
            main { p.seekTo(25000); p.play() }
            until("Seek failed") { p.isPlaying && p.currentPosition >= 25000 }
            main { p.seekToNextMediaItem() }
            until("Next failed") { p.currentMediaItemIndex == 1 && p.isPlaying }
            until("New track gain missing") { LoudnessAnalysis.lastApplied?.uri == tracks[1].uri.toString() }
            main { p.seekToPreviousMediaItem() }
            until("Previous failed") { p.currentMediaItemIndex == 0 && p.isPlaying }
            until("Cached gain not reused") { LoudnessAnalysis.lastApplied?.let { it.uri == tracks[0].uri.toString() && it.cached } == true }
            pass("Pause, seek, next/previous and complete-track cache reuse")
            main { p.shuffleModeEnabled = true; p.repeatMode = Player.REPEAT_MODE_ALL; p.seekToNextMediaItem() }
            until("Shuffle failed") { p.isPlaying && p.currentMediaItemIndex != 0 }
            until("Shuffle gain not updated", 60) { LoudnessAnalysis.lastApplied?.uri == p.currentMediaItem?.mediaId }
            val shuffleIndex = main { p.currentMediaItemIndex }
            sound(originalSound.copy(enabled = false))
            until("Original switch failed") { p.isPlaying && p.currentMediaItemIndex == shuffleIndex && p.shuffleModeEnabled }
            sound(originalSound.copy(enabled = true, recommended = false))
            until("Custom switch failed") { p.isPlaying && p.playerError == null }
            sound(originalSound.copy(enabled = true, recommended = true))
            until("Recommended switch failed") { p.isPlaying && p.mediaItemCount == tracks.size }
            pass("Shuffle/repeat and Original/Custom/Recommended transitions preserve queue and playback")
            fun key(code: Int) {
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
            }
            key(KeyEvent.KEYCODE_MEDIA_PAUSE); until("Remote pause failed") { !p.playWhenReady }
            key(KeyEvent.KEYCODE_MEDIA_PLAY); until("Remote play failed") { p.isPlaying }
            main { p.shuffleModeEnabled = false; p.repeatMode = Player.REPEAT_MODE_OFF }
            if (bluetoothPhase) {
                main { p.seekToDefaultPosition(0) }
                until("Remote next setup failed") { p.currentMediaItemIndex == 0 && p.isPlaying }
                key(KeyEvent.KEYCODE_MEDIA_NEXT); until("Remote next failed") { p.currentMediaItemIndex == 1 }
                key(KeyEvent.KEYCODE_MEDIA_PREVIOUS); until("Remote previous failed") { p.currentMediaItemIndex == 0 }
                pass("Android media keys over Bluetooth playback: pause/play/next/previous")
            }
            context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val background = main { p.currentPosition }
            val backgroundMs = if (bluetoothPhase) 12000L else 5000L
            SystemClock.sleep(backgroundMs)
            check(main { p.isPlaying && p.playerError == null && p.currentPosition > background + backgroundMs - 1500 })
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == volume)
            verifyBluetoothRoute()
            pass("Android media keys + ${backgroundMs / 1000} s background playback; system volume unchanged")
            if (bluetoothPhase) {
                for (index in listOf(1, 2)) {
                    main { p.pause(); p.seekToDefaultPosition(index) }
                    until("Reference selection missing") { LoudnessAnalysis.lastApplied?.uri == tracks[index].uri.toString() }
                    until("Reference analysis missing", 60) { LoudnessAnalysis.lastMeasured?.uri == tracks[index].uri.toString() }
                }
                pass("All three local references have saved levels for a calibrated listening comparison")
            }
        } finally {
            main { p.pause(); p.shuffleModeEnabled = false; p.repeatMode = Player.REPEAT_MODE_OFF }
            sound(originalSound)
            main { p.release() }
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
