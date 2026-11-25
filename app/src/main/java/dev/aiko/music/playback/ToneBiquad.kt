package dev.aiko.music.playback

import kotlin.math.*

/** RBJ biquads: double-precision independent states, coefficients computed once per stream.
 * Formula reference: https://www.w3.org/TR/audio-eq-cookbook/
 */
class ToneBiquad private constructor(channels: Int, b0: Double, b1: Double, b2: Double,
    a0: Double, a1: Double, a2: Double) {
    private val b0 = b0 / a0; private val b1 = b1 / a0; private val b2 = b2 / a0
    private val a1 = a1 / a0; private val a2 = a2 / a0
    private val z1 = DoubleArray(channels); private val z2 = DoubleArray(channels)
    fun process(x: Double, channel: Int): Double {
        val y = b0 * x + z1[channel]
        z1[channel] = b1 * x - a1 * y + z2[channel]
        z2[channel] = b2 * x - a2 * y
        return y
    }
    companion object {
        private fun angle(rate: Int, hz: Double) = 2 * PI * hz.coerceAtMost(rate * .4) / rate
        fun highPass(rate: Int, channels: Int, hz: Double): ToneBiquad {
            val w = angle(rate, hz); val c = cos(w); val alpha = sin(w) / sqrt(2.0)
            return ToneBiquad(channels, (1 + c) / 2, -(1 + c), (1 + c) / 2,
                1 + alpha, -2 * c, 1 - alpha)
        }
        fun lowShelf(rate: Int, channels: Int, hz: Double, db: Double): ToneBiquad {
            val a = 10.0.pow(db / 40); val w = angle(rate, hz); val c = cos(w)
            val beta = sqrt(2 * a) * sin(w) // Shelf slope S=1.
            return ToneBiquad(channels,
                a * ((a + 1) - (a - 1) * c + beta), 2 * a * ((a - 1) - (a + 1) * c),
                a * ((a + 1) - (a - 1) * c - beta),
                (a + 1) + (a - 1) * c + beta, -2 * ((a - 1) + (a + 1) * c),
                (a + 1) + (a - 1) * c - beta)
        }
        fun peak(rate: Int, channels: Int, hz: Double, db: Double, q: Double): ToneBiquad {
            val a = 10.0.pow(db / 40); val w = angle(rate, hz); val c = cos(w); val alpha = sin(w) / (2 * q)
            return ToneBiquad(channels, 1 + alpha * a, -2 * c, 1 - alpha * a,
                1 + alpha / a, -2 * c, 1 - alpha / a)
        }
    }
}
