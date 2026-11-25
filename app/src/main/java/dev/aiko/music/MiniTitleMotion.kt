package dev.aiko.music

import kotlin.math.ceil

/** Owned by the ViewModel, independent of pause/progress and composable lifetime. */
class MiniTitleMotion {
    private var claimedToken = -1L
    private var run: TitleRun? = null
    val currentRun: TitleRun? get() = run
    fun claim(token: Long, overflowPx: Float, density: Float, nowMs: Long): TitleRun? {
        if (token <= 0 || token < claimedToken) return null
        if (claimedToken != token) {
            claimedToken = token
            run = if (overflowPx > 1f) TitleRun(nowMs, overflowPx,
                ceil(overflowPx / (28f * density) * 1000).toLong().coerceAtLeast(900)) else null
        }
        return run
    }
}

data class TitleRun(val startMs: Long, val distance: Float, val forwardMs: Long) {
    val returnMs = (forwardMs / 3).coerceIn(600, 2400)
    val totalMs = 1000 + forwardMs + 650 + returnMs
    fun finished(nowMs: Long) = nowMs - startMs >= totalMs
    fun offset(nowMs: Long): Float {
        val t = (nowMs - startMs).coerceAtLeast(0)
        return when {
            t < 1000 -> 0f
            t < 1000 + forwardMs -> distance * smooth((t - 1000).toFloat() / forwardMs)
            t < 1000 + forwardMs + 650 -> distance
            t < totalMs -> distance * (1 - smooth((t - 1000 - forwardMs - 650).toFloat() / returnMs))
            else -> 0f
        }
    }
    private fun smooth(x: Float) = x * x * (3 - 2 * x)
}
