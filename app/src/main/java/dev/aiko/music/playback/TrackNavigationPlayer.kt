package dev.aiko.music.playback

import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** One skip policy for Compose, notifications and Bluetooth media commands. */
@UnstableApi
class TrackNavigationPlayer(private val playback: Player) : ForwardingSimpleBasePlayer(playback) {
    override fun getState(): State {
        val state = super.getState()
        val commands = state.availableCommands.buildUpon()
        // Keep command availability and controller position predictions consistent with real skips.
        if (!state.availableCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
            commands.remove(Player.COMMAND_SEEK_TO_PREVIOUS)
        if (!state.availableCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
            commands.remove(Player.COMMAND_SEEK_TO_NEXT)
        return state.buildUpon().setAvailableCommands(commands.build())
            .setMaxSeekToPreviousPositionMs(Long.MAX_VALUE).build()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> =
        if (handleTrackSkip(playback, seekCommand)) Futures.immediateVoidFuture()
        else super.handleSeek(mediaItemIndex, positionMs, seekCommand)
}

@UnstableApi
internal fun handleTrackSkip(playback: Player, command: Int): Boolean {
    val forward = when (command) {
        Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> false
        Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> true
        else -> return false
    }
    // Media3 supplies the real queue order, including shuffle and repeat-all wrapping.
    val destination = if (forward) playback.nextMediaItemIndex else playback.previousMediaItemIndex
    if (destination != C.INDEX_UNSET) {
        playback.seekTo(destination, 0L)
        playback.prepare()
        playback.play()
    }
    return true
}
