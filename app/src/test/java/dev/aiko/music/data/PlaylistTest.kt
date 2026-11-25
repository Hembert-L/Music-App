package dev.aiko.music.data

import org.junit.Assert.*
import org.junit.Test

class PlaylistTest {
    private val a = PlaylistEntry("a", "content://media/external/audio/media/1")
    private val b = PlaylistEntry("b", "content://media/external/audio/media/2")
    private val c = PlaylistEntry("c", "content://media/external/audio/media/3")
    @Test fun namesTrimAndRejectBlankOrAmbiguousDuplicates() {
        assertEquals("Viaje", PlaylistNames.validate(" Viaje ", emptyList()))
        assertThrows(IllegalArgumentException::class.java) { PlaylistNames.validate("  ", emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { PlaylistNames.validate("RELAX", listOf(Playlist("x", "Rélax"))) }
        assertEquals("Rélax", PlaylistNames.validate("Rélax", listOf(Playlist("x", "Rélax")), "x"))
    }
    @Test fun namesHaveAnExplicitLengthLimit() {
        assertEquals(60, PlaylistNames.validate("a".repeat(60), emptyList()).length)
        assertThrows(IllegalArgumentException::class.java) { PlaylistNames.validate("a".repeat(61), emptyList()) }
    }
    @Test fun addingKeepsExistingOrderAndStableIdentities() {
        val before = Playlist("list", "Viaje", listOf(a, b))
        val after = before.add(listOf(c.uri, a.uri, c.uri))
        assertEquals(listOf(a, b), after.entries.take(2))
        assertEquals(listOf(a.uri, b.uri, c.uri), after.entries.map(PlaylistEntry::uri))
        assertEquals(3, after.entries.map(PlaylistEntry::id).distinct().size)
        assertEquals(2, before.entries.size)
    }
    @Test fun reorderingMovesExactlyOneEntryWithoutLosingTracks() {
        val before = Playlist("list", "Viaje", listOf(a, b, c))
        val after = before.move("c", -1)
        assertEquals(listOf(a, c, b), after.entries)
        assertEquals(before, before.move("a", -1))
        assertEquals(before, before.move("missing", 1))
        assertEquals(before, before.move("c", 1))
    }
    @Test fun removingUsesEntryIdentityAndDoesNotMutateOriginal() {
        val duplicateReference = PlaylistEntry("another-a", a.uri)
        val before = Playlist("list", "Viaje", listOf(a, b, duplicateReference))
        assertEquals(listOf(b, duplicateReference), before.remove("a").entries)
        assertEquals(3, before.entries.size)
    }
}
