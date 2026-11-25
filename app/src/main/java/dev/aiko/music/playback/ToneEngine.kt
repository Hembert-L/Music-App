package dev.aiko.music.playback

import kotlin.math.*

enum class SoundMode { ORIGINAL, CUSTOM, RECOMMENDED }

data class SoundSettings(val enabled: Boolean = false, val bass: Float = 2f, val air: Float = 1f, val trim: Float = 0f, val recommended: Boolean = false) {
    val mode: SoundMode get() = if (!enabled) SoundMode.ORIGINAL else if (recommended) SoundMode.RECOMMENDED else SoundMode.CUSTOM
    fun safe() = copy(bass = (if (bass.isFinite()) bass else 0f).coerceIn(0f, 6f), air = (if (air.isFinite()) air else 0f).coerceIn(0f, 6f), trim = (if (trim.isFinite()) trim else -12f).coerceIn(-12f, 0f))
    val headroomDb: Float get() = (-1.0 - 20 * log10(1 + (10.0.pow(bass / 20.0) - 1) + 2 * (10.0.pow(air / 20.0) - 1)) + trim).toFloat()
}

/** Bounded, modest tone shaping. Low-pass states are convex combinations of input.
 * |x + b*low + a*(x-highLow)| <= 1+b+2a. Compensation reserves an extra 1 dB.
 * Gains are smoothed and compensation follows the SAME instantaneous gains.
 */
class ToneEngine(private val sampleRate: Int, channels: Int) {
    private val low = FloatArray(channels)
    private val highLow = FloatArray(channels)
    private val lowAlpha = (1 - exp(-2 * PI * 180 / sampleRate)).toFloat()
    private val highAlpha = (1 - exp(-2 * PI * 4500 / sampleRate)).toFloat()
    private val smooth = (1 - exp(-1.0 / (.015 * sampleRate))).toFloat()
    private var bass = 0f
    private var air = 0f
    private var level = 0f
    private var gain = 0f
    private var lastSettings: SoundSettings? = null
    private var targetBass = 0f
    private var targetAir = 0f
    private var targetLevel = 0f
    fun process(input: Float, channel: Int, settings: SoundSettings): Float {
        if (channel == 0) {
            if (settings != lastSettings) {
                lastSettings = settings
                targetBass = 10.0.pow(settings.bass / 20.0).toFloat() - 1
                targetAir = 10.0.pow(settings.air / 20.0).toFloat() - 1
                targetLevel = 10.0.pow((-1 + settings.trim) / 20.0).toFloat()
            }
            bass += smooth * (targetBass - bass)
            air += smooth * (targetAir - air)
            level += smooth * (targetLevel - level)
            gain = level / (1 + bass + 2 * air)
        }
        val x = if (input.isFinite()) input.coerceIn(-1f, 1f) else 0f
        low[channel] += lowAlpha * (x - low[channel])
        highLow[channel] += highAlpha * (x - highLow[channel])
        return ((x + bass * low[channel] + air * (x - highLow[channel])) * gain).coerceIn(-1f, 1f)
    }
}
