package dev.aiko.music

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.aiko.music.data.Song
import dev.aiko.music.playback.LoudnessAnalysis
import dev.aiko.music.playback.PlaybackService
import dev.aiko.music.playback.SoundMode
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Prepare real, calibrated references; screen-off observation then runs against shipping Benchmark. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object ScreenOffReferenceChecks {
    fun prepare(test: Instrumentation, songs: List<Song>, pass: (String) -> Unit) {
        val context = test.targetContext
        fun <T> main(block: () -> T): T {
            val result = AtomicReference<T>(); val failure = AtomicReference<Throwable>()
            test.runOnMainSync { try { result.set(block()) } catch (t: Throwable) { failure.set(t) } }
            failure.get()?.let { throw it }; return result.get()
        }
        fun until(message: String, seconds: Int = 70, predicate: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + seconds * 1000
            while (SystemClock.elapsedRealtime() < deadline) { if (main(predicate)) return; SystemClock.sleep(100) }
            error(message)
        }
        check(PlaybackService.readSound(context).mode == SoundMode.RECOMMENDED) { "Keep approved Recommended selected" }
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        check(audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        }) { "Bluetooth headphones required" }
        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val tracks = listOfNotNull(
            songs.firstOrNull { it.title == "MONTAGEM NO VENTO (Slowed)" && it.artist == "TRXVELER, TESSAI" },
            songs.firstOrNull { it.title == "MONTAGEM GENETICA" && it.artist == "KPHK, SCHWVFTY" },
            songs.firstOrNull { it.title == "SEM DIREÇÃO" && it.artist == "DJ FKU, NAOMI, Skorde" },
            songs.firstOrNull { it.title == "MONTAGEM CONTIGO" && it.artist == "Repsaj" },
            songs.firstOrNull { it.title == "DJ FKU, CHAXZER - DANÇAR NO SALÃO (SLOWED)" && it.artist == "DJ FKU, CHAXZER" },
            songs.firstOrNull { it.title == "PPPP (feat. Hatsune Miku, Kasane Teto)" && it.artist == "TAK, Hatsune Miku, Kasane Teto" },
            songs.firstOrNull { it.title == "A Thousand Years" && it.artist == "John Michael Howell, JVKE, ZVC" }
        )
        check(tracks.size == 7) { "Seven expected local references required; found ${tracks.size}" }
        pass("Seven real references selected; Recommended unchanged; Bluetooth music volume=$volume")
        val future = main { MediaController.Builder(context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync() }
        val player = future.get(15, TimeUnit.SECONDS)
        val results = JSONArray()
        try {
            main { player.pause(); player.shuffleModeEnabled = false; player.repeatMode = Player.REPEAT_MODE_OFF
                player.setMediaItems(tracks.map(Song::mediaItem)); player.prepare() }
            for ((index, song) in tracks.withIndex()) {
                main { player.seekToDefaultPosition(index) }
                until("Reference not selected: ${song.title}", 15) { player.currentMediaItem?.mediaId == song.uri.toString() }
                until("Analysis unavailable: ${song.title}") { LoudnessAnalysis.lastMeasured?.uri == song.uri.toString() }
                val level = requireNotNull(LoudnessAnalysis.lastMeasured).level
                check(level.lufs.isFinite() && level.peak.isFinite())
                results.put(JSONObject().put("title", song.title).put("artist", song.artist)
                    .put("uri", song.uri).put("durationMs", song.duration).put("balanceLufs", level.lufs)
                    .put("measuredPeak", level.peak).put("gainDb", level.gainDb)
                    .put("predictedCalibratedLufs", level.lufs + level.gainDb))
                pass("Reference saved: ${song.title}; gain=${level.gainDb} dB")
            }
            main { player.seekToDefaultPosition(0); player.pause() }
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == volume)
            context.filesDir.resolve("screenoff-references.json").writeText(results.toString(2))
            test.sendStatus(0, Bundle().apply { putString("screenoff_references_json", results.toString()) })
            pass("Reference queue saved paused; no DSP, system volume or sound-mode changes")
        } finally { main { player.pause(); player.release() } }
    }
}
