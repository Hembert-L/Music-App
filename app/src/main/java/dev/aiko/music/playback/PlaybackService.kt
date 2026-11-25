package dev.aiko.music.playback

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.session.*
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.aiko.music.MainActivity
import org.json.JSONArray
import org.json.JSONObject

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val processor = ToneProcessor()
    private val recommendedProcessor = RecommendedProcessor()
    private lateinit var sound: SoundSettings
    private val handler = Handler(Looper.getMainLooper())
    private val loudness by lazy { LoudnessAnalysis(this, handler) }
    private val prefs by lazy { getSharedPreferences("music", Context.MODE_PRIVATE) }
    private val saveLoop = object : Runnable {
        override fun run() { saveQueue(); handler.postDelayed(this, 5000) }
    }
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { saveQueue() }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { updateTrackLevel() }
    }

    override fun onCreate() {
        super.onCreate()
        sound = readSound(this)
        player = createPlayer()
        val activity = PendingIntent.getActivity(this, 0, android.content.Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, TrackNavigationPlayer(player)).setSessionActivity(activity).setCallback(object : MediaSession.Callback {
            override fun onPlaybackResumption(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                if (player.mediaItemCount == 0) restoreQueue()
                val items = (0 until player.mediaItemCount).map(player::getMediaItemAt)
                if (items.isEmpty()) return Futures.immediateFailedFuture(IllegalStateException("No saved queue"))
                return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(items,
                    player.currentMediaItemIndex.coerceAtLeast(0), player.currentPosition.coerceAtLeast(0)))
            }
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                val base = super.onConnect(session, controller)
                val commands = base.availableSessionCommands.buildUpon()
                if (controller.packageName == packageName) {
                    commands.add(SessionCommand(SOUND_COMMAND, Bundle.EMPTY))
                    commands.add(SessionCommand(CLOSE_COMMAND, Bundle.EMPTY))
                }
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(commands.build()).build()
            }
            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                if (customCommand.customAction == CLOSE_COMMAND && controller.packageName == packageName) {
                    player.pause()
                    player.stop()
                    player.clearMediaItems()
                    val saved = prefs.edit().putString("queue", "[]").putInt("index", 0).putLong("position", 0)
                        .putBoolean("playback_closed", true).commit()
                    pauseAllPlayersAndStopSelf()
                    return Futures.immediateFuture(SessionResult(if (saved) SessionResult.RESULT_SUCCESS else SessionError.ERROR_IO))
                }
                if (customCommand.customAction != SOUND_COMMAND || controller.packageName != packageName)
                    return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
                val next = SoundSettings(args.getBoolean("enabled"), args.getFloat("bass", 2f),
                    args.getFloat("air", 1f), args.getFloat("trim", 0f), args.getBoolean("recommended", false)).safe()
                val rebuild = next.mode != sound.mode
                sound = next
                processor.settings = next
                prefs.edit().putBoolean("enhanced", next.enabled).putFloat("bass", next.bass)
                    .putFloat("air", next.air).putFloat("trim", next.trim).putBoolean("recommended", next.recommended).apply()
                if (rebuild) replacePlayer()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }).build()
        restoreQueue()
        player.addListener(listener)
        updateTrackLevel()
        handler.post(saveLoop)
    }

    private fun createPlayer(): ExoPlayer {
        processor.settings = sound
        val renderers = if (sound.enabled) object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context).setAudioCapabilities(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES)
                    .setAudioProcessors(if (sound.mode == SoundMode.RECOMMENDED) arrayOf(recommendedProcessor) else arrayOf(processor)).build()
        } else DefaultRenderersFactory(this).setEnableAudioFloatOutput(true)
        return ExoPlayer.Builder(this, renderers).build().apply {
            if (sound.enabled) {
                trackSelectionParameters = trackSelectionParameters.buildUpon().setAudioOffloadPreferences(
                    TrackSelectionParameters.AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED).build()).build()
            }
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
    }

    private fun replacePlayer() {
        val old = player
        val items = (0 until old.mediaItemCount).map(old::getMediaItemAt)
        val index = old.currentMediaItemIndex.coerceAtLeast(0)
        val position = old.currentPosition.coerceAtLeast(0)
        val playing = old.playWhenReady
        old.removeListener(listener)
        // Release the previous AudioTrack before preparing the replacement. A short gap is expected.
        old.pause()
        loudness.cancel()
        player = createPlayer().apply {
            repeatMode = old.repeatMode
            shuffleModeEnabled = old.shuffleModeEnabled
            setMediaItems(items, index.coerceAtMost((items.size - 1).coerceAtLeast(0)), position)
        }
        session!!.setPlayer(TrackNavigationPlayer(player))
        old.release()
        player.addListener(listener)
        updateTrackLevel()
        if (items.isNotEmpty()) { player.prepare(); player.playWhenReady = playing }
        saveQueue()
    }

    private fun updateTrackLevel() {
        loudness.cancel()
        if (sound.mode != SoundMode.RECOMMENDED) return
        val current = player.currentMediaItem?.localConfiguration?.uri ?: return
        recommendedProcessor.trackGainDb = loudness.select(current).gainDb
        val nextIndex = player.nextMediaItemIndex
        val next = if (nextIndex in 0 until player.mediaItemCount)
            player.getMediaItemAt(nextIndex).localConfiguration?.uri else null
        loudness.request(current, next)
    }

    private fun saveQueue() {
        if (!::player.isInitialized) return
        val queue = JSONArray()
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            queue.put(JSONObject().put("uri", item.mediaId).put("title", item.mediaMetadata.title)
                .put("artist", item.mediaMetadata.artist).put("album", item.mediaMetadata.albumTitle)
                .put("art", item.mediaMetadata.artworkUri?.toString()))
        }
        prefs.edit().putString("queue", queue.toString()).putInt("index", player.currentMediaItemIndex.coerceAtLeast(0))
            .putBoolean("playback_closed", player.mediaItemCount == 0)
            .putLong("position", player.currentPosition.coerceAtLeast(0)).putInt("repeat", player.repeatMode)
            .putBoolean("shuffle", player.shuffleModeEnabled).apply()
    }

    private fun restoreQueue() {
        if (prefs.getBoolean("playback_closed", false)) return
        runCatching {
            val array = JSONArray(prefs.getString("queue", "[]"))
            val items = (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                MediaItem.Builder().setMediaId(o.getString("uri")).setUri(o.getString("uri"))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(o.optString("title"))
                        .setArtist(o.optString("artist")).setAlbumTitle(o.optString("album"))
                        .setArtworkUri(o.optString("art").takeIf { it.startsWith("content:") }?.let(android.net.Uri::parse)).build()).build()
            }
            if (items.isNotEmpty()) player.setMediaItems(items, prefs.getInt("index", 0).coerceIn(items.indices), prefs.getLong("position", 0))
            player.repeatMode = prefs.getInt("repeat", Player.REPEAT_MODE_OFF)
            player.shuffleModeEnabled = prefs.getBoolean("shuffle", false)
            // Restore paused. Opening Music must never unexpectedly start audio.
        }.onFailure { prefs.edit().remove("queue").apply() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() {
        loudness.close()
        handler.removeCallbacksAndMessages(null)
        saveQueue()
        player.removeListener(listener)
        session?.release()
        player.release()
        super.onDestroy()
    }

    companion object {
        const val SOUND_COMMAND = "dev.aiko.music.SOUND"
        const val CLOSE_COMMAND = "dev.aiko.music.CLOSE_PLAYBACK"
        fun readSound(context: Context): SoundSettings = context.getSharedPreferences("music", Context.MODE_PRIVATE).let {
            SoundSettings(it.getBoolean("enhanced", false), it.getFloat("bass", 2f), it.getFloat("air", 1f), it.getFloat("trim", 0f), it.getBoolean("recommended", false)).safe()
        }
        fun soundBundle(s: SoundSettings) = Bundle().apply {
            putBoolean("enabled", s.enabled); putFloat("bass", s.bass); putFloat("air", s.air); putFloat("trim", s.trim); putBoolean("recommended", s.recommended)
        }
    }
}
