package dev.aiko.music.playback

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import kotlin.random.Random

class ToneEngineTest {
    @Test fun fullScaleSignalsRemainBelowReservedPeak() {
        var peak = 0f
        for (rate in listOf(8000, 44100, 48000, 96000)) for (channels in listOf(1, 2, 6)) {
            val engine = ToneEngine(rate, channels)
            val random = Random(47)
            repeat(rate * 2) { frame ->
                val settings = SoundSettings(true, if (frame % 97 < 40) 0f else 6f, if (frame % 53 < 20) 0f else 6f, if (frame % 173 < 25) -12f else 0f)
                repeat(channels) { channel ->
                    val input = when ((frame / (rate / 4)) % 4) {
                        0 -> 1f
                        1 -> if (frame % 2 == 0) -1f else 1f
                        2 -> random.nextFloat() * 2 - 1
                        else -> if (frame % 1024 == 0) 1f else 0f
                    }
                    val value = engine.process(input, channel, settings)
                    assertTrue(value.isFinite())
                    peak = max(peak, abs(value))
                }
            }
        }
        assertTrue("peak=$peak", peak <= 10.0.pow(-1.0 / 20).toFloat() + .00001f)
        println("Peak across full-scale DC/square/noise/impulses and live setting changes: $peak")
    }
    @Test fun toneControlsChangeFrequencyBalance() {
        fun rms(hz: Double, bass: Float, air: Float): Double {
            val engine = ToneEngine(48000, 2)
            val settings = SoundSettings(true, bass, air)
            var energy = 0.0
            repeat(48000) { i ->
                val x = (.5 * sin(2 * PI * hz * i / 48000)).toFloat()
                val y = engine.process(x, 0, settings)
                engine.process(x, 1, settings)
                if (i >= 24000) energy += y * y
            }
            return sqrt(energy / 24000)
        }
        assertTrue(rms(30.0, 6f, 0f) > rms(6000.0, 6f, 0f) * 1.8)
        assertTrue(rms(10000.0, 0f, 6f) > rms(30.0, 0f, 6f) * 1.5)
    }
    @Test fun channelsDoNotBleedIntoEachOther() {
        val engine = ToneEngine(48000, 2)
        repeat(48000) {
            engine.process(1f, 0, SoundSettings(true, 6f, 6f))
            assertEquals(0f, engine.process(0f, 1, SoundSettings(true, 6f, 6f)), 0f)
        }
    }
    @Test fun controlsAreBoundedAndHeadroomGrowsWithBoost() {
        assertEquals(SoundSettings(true, 6f, 0f, -12f), SoundSettings(true, 100f, -5f, -50f).safe())
        assertTrue(SoundSettings(bass = 6f, air = 6f).headroomDb < SoundSettings(bass = 0f, air = 0f).headroomDb)
        assertEquals(-1f, SoundSettings(bass = 0f, air = 0f).headroomDb, .0001f)
    }
}
