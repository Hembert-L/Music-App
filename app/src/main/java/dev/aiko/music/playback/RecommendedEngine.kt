package dev.aiko.music.playback

import kotlin.math.*

/** Restrained bass and vocal presence. No allocations, ambience delays or feedback reverb.
 * Stereo side enhancement leaves the mid (voices/mono fold) intact before linked limiting.
 * The linked limiter anticipates 5 ms and reserves 1.5 dB below digital full scale.
 */
class RecommendedEngine(val sampleRate: Int, val channels: Int,
    private val analysisOnly: Boolean = false, private val initialGainDb: Float = 0f) {
    init { require(sampleRate > 0 && channels > 0) }
    val delayFrames = max(1, (sampleRate * .005).roundToInt())
    val tailFrames = delayFrames + (sampleRate * .10).roundToInt()
    private var sideLow = 0f
    private val rumbleCut = ToneBiquad.highPass(sampleRate, channels, 28.0)
    private val bass = ToneBiquad.lowShelf(sampleRate, channels, 70.0, .4)
    private val lowMid = ToneBiquad.peak(sampleRate, channels, 240.0, -1.5, .85)
    private val presence = ToneBiquad.peak(sampleRate, channels, 2400.0, 1.4, .75)
    private val shaped = FloatArray(channels)
    private val delay = FloatArray((delayFrames + 1) * channels)
    private val peakValues = FloatArray(delayFrames + 2)
    private val peakTimes = LongArray(delayFrames + 2)
    private var head = 0
    private var tail = 0
    private var time = 0L
    private var gain = 1f
    private var fade = 0f
    private val trackGain = TrackGain(sampleRate, initialGainDb)
    private val sideAlpha = alpha(1000.0)
    private val fadeAlpha = (1 - exp(-1.0 / (.015 * sampleRate))).toFloat()
    private val attack = (1 - exp(-1.0 / (.0003 * sampleRate))).toFloat()
    private val release = (1 - exp(-1.0 / (.100 * sampleRate))).toFloat()
    private val preGain = 10.0.pow(-3.0 / 20).toFloat()
    private fun alpha(hz: Double) = (1 - exp(-2 * PI * hz / sampleRate)).toFloat()

    fun processFrame(input: FloatArray, output: FloatArray, trackGainDb: Float = initialGainDb) {
        fade += fadeAlpha * (1f - fade)
        for (c in 0 until channels) {
            val raw = if (input[c].isFinite()) input[c].coerceIn(-1f, 1f) else 0f
            val x = rumbleCut.process(raw.toDouble(), c)
            shaped[c] = presence.process(lowMid.process(bass.process(x, c), c), c).toFloat()
        }
        if (channels == 2) {
            val mid = (shaped[0] + shaped[1]) * .5f
            val side = (shaped[0] - shaped[1]) * .5f
            sideLow += sideAlpha * (side - sideLow)
            val highSide = side - sideLow
            val wideSide = side + .16f * highSide
            shaped[0] = mid + wideSide
            shaped[1] = mid - wideSide
        }
        var peak = 0f
        // Normalize BEFORE the limiter so a mastered loud track keeps its transients.
        val level = trackGain.nextGain(trackGainDb)
        val write = (time % (delayFrames + 1)).toInt() * channels
        for (c in 0 until channels) {
            val value = shaped[c] * preGain * fade * level
            delay[write + c] = value
            peak = max(peak, abs(value))
        }
        if (analysisOnly) {
            // Silent measurement needs the same waveform/delay, without limiting or its peak queue.
            if (time >= delayFrames) {
                val read = ((time - delayFrames) % (delayFrames + 1)).toInt() * channels
                for (c in 0 until channels) output[c] = delay[read + c]
            } else output.fill(0f)
            time++
            return
        }
        val capacity = peakValues.size
        while (head != tail && peakTimes[head] < time - delayFrames) head = (head + 1) % capacity
        while (head != tail) {
            val previous = (tail - 1 + capacity) % capacity
            if (peakValues[previous] > peak) break
            tail = previous
        }
        peakValues[tail] = peak
        peakTimes[tail] = time
        tail = (tail + 1) % capacity
        val target = min(1f, CEILING / peakValues[head].coerceAtLeast(.000001f))
        gain += (if (target < gain) attack else release) * (target - gain)
        if (time >= delayFrames) {
            val read = ((time - delayFrames) % (delayFrames + 1)).toInt() * channels
            var outputPeak = 0f
            for (c in 0 until channels) outputPeak = max(outputPeak, abs(delay[read + c]))
            // A linked final guard covers attack rounding; never independently clips L/R.
            val applied = min(gain, CEILING / outputPeak.coerceAtLeast(.000001f))
            for (c in 0 until channels) output[c] = delay[read + c] * applied
        } else output.fill(0f)
        time++
    }
    companion object { val CEILING = 10.0.pow(-1.5 / 20).toFloat() }
}
