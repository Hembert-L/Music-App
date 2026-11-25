package dev.aiko.music

import org.junit.Assert.*
import org.junit.Test

class MiniTitleMotionTest {
    @Test fun delayForwardHoldReturnAndFinishAreFinite() {
        val run = TitleRun(100, 280f, 10000)
        assertEquals(0f, run.offset(1099), 0f)
        assertEquals(140f, run.offset(6100), .001f)
        assertEquals(280f, run.offset(11300), 0f)
        assertTrue(run.offset(12500) in 0f..280f)
        assertTrue(run.finished(100 + run.totalMs))
        assertEquals(0f, run.offset(100 + run.totalMs + 10000), 0f)
    }
    @Test fun pauseProgressAndNavigationCannotRestartAClaimedRun() {
        val store = MiniTitleMotion()
        val first = store.claim(1, 120f, 2f, 100)!!
        assertSame(first, store.claim(1, 120f, 2f, 2000))
        assertSame(first, store.claim(1, 200f, 2f, 100000))
        assertTrue(store.claim(1, 120f, 2f, 100000)!!.finished(100000))
    }
    @Test fun shortTitlesDoNotAnimateEvenIfLayoutLaterChanges() {
        val store = MiniTitleMotion()
        assertNull(store.claim(1, 0f, 2f, 100))
        assertNull(store.claim(1, 120f, 2f, 10000))
    }
    @Test fun newPlaybackStartsFreshAndStalePlaybackCannotReturn() {
        val store = MiniTitleMotion()
        val first = store.claim(1, 100f, 1f, 100)!!
        val second = store.claim(2, 100f, 1f, 1000)!!
        assertNotSame(first, second)
        assertEquals(1000L, second.startMs)
        assertNull(store.claim(1, 100f, 1f, 2000))
        assertNull(store.claim(0, 100f, 1f, 2000))
        assertSame(second, store.currentRun)
    }
}
