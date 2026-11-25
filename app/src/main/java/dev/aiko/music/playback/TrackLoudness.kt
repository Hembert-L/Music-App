package dev.aiko.music.playback

import kotlin.math.*

data class TrackLoudness(val lufs: Double, val peak: Double) {
    /** Fixed track gain: retain dynamics, bound boosts, reserve 2 dB sample-peak headroom. */
    val gainDb: Float get() {
        if (!lufs.isFinite() || !peak.isFinite() || peak <= 0.0) return 0f
        return minOf(TARGET_LUFS - lufs, -2.0 - 20 * log10(peak), 6.0).coerceAtLeast(-18.0).toFloat()
    }
    companion object {
        const val TARGET_LUFS = -16.0
        const val UNCALIBRATED_GAIN_DB = -9f
    }
}

/** Integrated K-weighted loudness for mono/stereo. 400 ms, 75% overlap, two gates.
 * Measures the complete *processed* track, not an intro or a changing playback RMS.
 * This is not a certified meter, nor an inter-sample/true-peak measurement.
 */
class TrackLoudnessMeter(private val sampleRate: Int, private val channels: Int) {
    init { require(sampleRate >= 8000 && channels in 1..2) }
    private class Filter(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double) {
        var z1 = 0.0; var z2 = 0.0
        fun process(x: Double): Double {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            return y
        }
    }
    private fun shelf(): Filter {
        val k = tan(PI * 1681.974450955533 / sampleRate)
        val q = .7071752369554196
        val vh = 10.0.pow(3.999843853973347 / 20)
        val vb = vh.pow(.4996667741545416)
        val a0 = 1 + k / q + k * k
        return Filter((vh + vb * k / q + k * k) / a0, 2 * (k * k - vh) / a0,
            (vh - vb * k / q + k * k) / a0, 2 * (k * k - 1) / a0, (1 - k / q + k * k) / a0)
    }
    private fun highPass(): Filter {
        val k = tan(PI * 38.13547087602444 / sampleRate)
        val q = .5003270373238773
        val a0 = 1 + k / q + k * k
        // BS.1770 RLB numerator is intentionally NOT divided by a0.
        return Filter(1.0, -2.0, 1.0, 2 * (k * k - 1) / a0, (1 - k / q + k * k) / a0)
    }
    private val shelves = Array(channels) { shelf() }
    private val highPasses = Array(channels) { highPass() }
    private val blockFrames = (sampleRate * .4).roundToInt()
    private val stepFrames = (sampleRate * .1).roundToInt()
    private val ring = DoubleArray(blockFrames)
    private val blocks = ArrayList<Double>()
    private var frames = 0L
    private var ringIndex = 0
    private var nextBlockAt = blockFrames.toLong()
    private var sum = 0.0
    private var peak = 0.0
    fun add(frame: FloatArray) {
        var energy = 0.0
        for (c in 0 until channels) {
            val x = if (frame[c].isFinite()) frame[c].toDouble() else 0.0
            peak = max(peak, abs(x))
            val y = highPasses[c].process(shelves[c].process(x))
            energy += y * y
        }
        sum += energy - ring[ringIndex]
        ring[ringIndex] = energy
        ringIndex++
        if (ringIndex == blockFrames) ringIndex = 0
        frames++
        if (frames == nextBlockAt) {
            blocks.add(max(0.0, sum / blockFrames))
            nextBlockAt += stepFrames
        }
    }
    fun result(): TrackLoudness {
        val absolute = 10.0.pow((-70 + .691) / 10)
        val audible = blocks.filter { it > absolute }
        if (audible.isEmpty()) return TrackLoudness(Double.NEGATIVE_INFINITY, peak)
        val relative = audible.average() * .1
        val gated = audible.filter { it > relative }
        return TrackLoudness(-.691 + 10 * log10(gated.average()), peak)
    }
}

/** Gain changes only on track/analysis changes, never on beats. Engine limits after this gain. */
class TrackGain(sampleRate: Int, initialDb: Float) {
    private var gain = linear(initialDb)
    // De-click only at a track boundary; never a seconds-long volume sweep.
    private val smoothing = (1 - exp(-1.0 / (.005 * sampleRate))).toFloat()
    private var lastDb = initialDb
    private var target = gain
    fun nextGain(db: Float): Float {
        if (db != lastDb) { lastDb = db; target = linear(db) }
        gain += smoothing * (target - gain)
        return gain
    }
    private fun linear(db: Float) = 10.0.pow((if (db.isFinite()) db else 0f).coerceIn(-18f, 6f) / 20.0).toFloat()
}
