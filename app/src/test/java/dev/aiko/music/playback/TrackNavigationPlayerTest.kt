package dev.aiko.music.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class TrackNavigationPlayerTest {
    private class RecordingPlayer(val previous: Int, val next: Int) {
        val calls = mutableListOf<String>()
        val player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, args ->
            when (method.name) {
                "getPreviousMediaItemIndex" -> previous
                "getNextMediaItemIndex" -> next
                "seekTo" -> { calls += "seek:${args!![0]}:${args[1]}"; null }
                "prepare", "play", "pause" -> { calls += method.name; null }
                else -> error("Unexpected Player method: ${method.name}")
            }
        } as Player
    }

    @Test fun bothPreviousCommandsStartTheRealPredecessorAtZero() {
        for (command in listOf(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)) {
            val raw = RecordingPlayer(previous = 7, next = 2)
            assertTrue(handleTrackSkip(raw.player, command))
            assertEquals(listOf("seek:7:0", "prepare", "play"), raw.calls)
        }
    }

    @Test fun bothNextCommandsStartTheRealSuccessorAtZero() {
        for (command in listOf(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)) {
            val raw = RecordingPlayer(previous = 7, next = 2)
            assertTrue(handleTrackSkip(raw.player, command))
            assertEquals(listOf("seek:2:0", "prepare", "play"), raw.calls)
        }
    }

    @Test fun missingDestinationDoesNotRestartOrUnpauseTheCurrentTrack() {
        val raw = RecordingPlayer(C.INDEX_UNSET, C.INDEX_UNSET)
        for (command in listOf(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
            assertTrue(handleTrackSkip(raw.player, command))
        assertEquals(emptyList<String>(), raw.calls)
    }

    @Test fun ordinarySeekingAndPauseAreNotHandledAsSkips() {
        val raw = RecordingPlayer(0, 2)
        assertFalse(handleTrackSkip(raw.player, Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
        assertFalse(handleTrackSkip(raw.player, Player.COMMAND_SEEK_TO_MEDIA_ITEM))
        assertFalse(handleTrackSkip(raw.player, Player.COMMAND_PLAY_PAUSE))
        assertEquals(emptyList<String>(), raw.calls)
    }
}
