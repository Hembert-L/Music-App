package dev.aiko.music.playback

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import kotlin.random.Random

class RecommendedEngineTest {
    @Test fun linkedLimiterBoundsFullScaleAndPathologicalInputs() {
        var peak = 0f
        for (rate in listOf(8000, 44100, 48000, 96000)) for (channels in listOf(1, 2, 6)) {
            val engine = RecommendedEngine(rate, channels)
            val input = FloatArray(channels)
            val output = FloatArray(channels)
            val random = Random(37)
            repeat(rate) { frame ->
                for (c in input.indices) input[c] = when ((frame / (rate / 8)) % 8) {
                    0 -> 1f
                    1 -> if (frame % 2 == 0) -1f else 1f
                    2 -> random.nextFloat() * 2 - 1
                    3 -> sin(2 * PI * 40 * frame / rate).toFloat()
                    4 -> if (c % 2 == 0) 1f else -1f
                    5 -> if (frame % 17 == 0) 1f else 0f
                    6 -> Float.NaN
                    else -> Float.POSITIVE_INFINITY
                }
                engine.processFrame(input, output)
                for (y in output) {
                    assertTrue("non-finite", y.isFinite())
                    assertTrue("peak=$y", abs(y) <= RecommendedEngine.CEILING + .000002f)
                    peak = max(peak, abs(y))
                }
            }
        }
        println("Recommended peak in full-scale stress test: $peak; ceiling=${RecommendedEngine.CEILING}")
    }
    @Test fun monoVocalsStayCentredAndAgreeWithMonoPath() {
        val stereo = RecommendedEngine(48000, 2)
        val mono = RecommendedEngine(48000, 1)
        val input = FloatArray(2); val output = FloatArray(2)
        val mi = FloatArray(1); val mo = FloatArray(1)
        repeat(48000) { i ->
            val x = (.2 * sin(2 * PI * 441 * i / 48000)).toFloat()
            input.fill(x); mi[0] = x
            stereo.processFrame(input, output); mono.processFrame(mi, mo)
            assertEquals(output[0], output[1], .000001f)
            assertEquals(mo[0], output[0], .000001f)
        }
    }
    @Test fun spatialProcessingPreservesMonoFoldWithoutDelayedReflections() {
        val engine = RecommendedEngine(48000, 2)
        val mono = RecommendedEngine(48000, 1)
        val input = FloatArray(2); val output = FloatArray(2)
        val mi = FloatArray(1); val mo = FloatArray(1)
        var sideLow = 0f
        val alpha = (1 - exp(-2 * PI * 1000 / 48000)).toFloat()
        var extraPeak = 0f
        repeat(56000) { i ->
            input[0] = if (i == 48000) .1f else 0f
            input[1] = -input[0]
            mi[0] = input[0]
            engine.processFrame(input, output)
            mono.processFrame(mi, mo)
            sideLow += alpha * (mo[0] - sideLow)
            // Direct side enhancement only: no 9/13 ms copies or room-delay buffer.
            assertEquals(mo[0] + .16f * (mo[0] - sideLow), output[0], .000002f)
            assertEquals(0f, output[0] + output[1], .000001f)
            extraPeak = max(extraPeak, abs(output[0] - mo[0]))
        }
        assertTrue("Width should remain subtle but present", extraPeak > 0.001f && extraPeak < 0.015f)
    }
    @Test fun lowEndIsSoftAndPresentRelativeToMids() {
        fun rms(hz: Int): Double {
            val engine = RecommendedEngine(48000, 2)
            val input = FloatArray(2); val output = FloatArray(2)
            var sum = 0.0
            repeat(48000) { i ->
                input.fill((.1 * sin(2 * PI * hz * i / 48000)).toFloat())
                engine.processFrame(input, output)
                if (i > 24000) sum += output[0] * output[0]
            }
            return sqrt(sum / 24000)
        }
        val low = rms(60); val mid = rms(1000)
        assertTrue("bass/mid=${low/mid}", low / mid in .9..1.05)
        val voice = rms(2400); val muddy = rms(240)
        val clarity = 20 * log10(voice / muddy)
        assertTrue("presence/low-mid=$clarity dB", clarity in 2.4..3.6)
        assertTrue(mid < .1 / sqrt(2.0))
        println("Clear profile: 60Hz/1kHz=${20 * log10(low/mid)} dB; 2.4kHz/240Hz=$clarity dB")
    }
    @Test fun subsonicRumbleIsReducedAndBassStaysBelowMidsInStereoWidth() {
        fun rms(hz: Int, opposite: Boolean): Double {
            val engine = RecommendedEngine(48000, 2)
            val input = FloatArray(2); val output = FloatArray(2)
            var energy = 0.0
            repeat(96000) { i ->
                input[0] = (.05 * sin(2 * PI * hz * i / 48000)).toFloat()
                input[1] = input[0] * if (opposite) -1 else 1
                engine.processFrame(input, output)
                if (i >= 48000) energy += output[0] * output[0]
            }
            return sqrt(energy / 48000)
        }
        assertTrue(rms(5, false) < rms(60, false) * .06)
        val bassWidth = rms(50, true) / rms(50, false)
        val midWidth = rms(2000, true) / rms(2000, false)
        assertTrue("bass=$bassWidth mid=$midWidth", bassWidth < midWidth)
        assertTrue(midWidth in 1.08..1.18)
        assertTrue(bassWidth < 1.02)
    }
    @Test fun independentChannelsDoNotLeakInMultichannelProcessing() {
        val engine = RecommendedEngine(48000, 6, initialGainDb = -9f)
        val input = FloatArray(6); val output = FloatArray(6)
        repeat(48000) { i ->
            input[0] = (.8 * sin(2 * PI * 80 * i / 48000)).toFloat()
            engine.processFrame(input, output)
            for (c in 1..5) assertEquals(0f, output[c], 0f)
        }
    }
    @Test fun lookaheadAndTailPreserveTheEndImpulse() {
        val engine = RecommendedEngine(48000, 1)
        val input = floatArrayOf(0f); val output = FloatArray(1)
        repeat(4800) { engine.processFrame(input, output) }
        input[0] = .1f; engine.processFrame(input, output)
        input[0] = 0f
        var peak = 0f
        repeat(engine.tailFrames) { engine.processFrame(input, output); peak = max(peak, abs(output[0])) }
        assertTrue(peak > .02f)
        assertEquals(240, engine.delayFrames)
    }
    @Test fun presetDoesNotOverwriteManualValuesAndOriginalRemainsDefault() {
        val manual = SoundSettings(true, 5f, 3f, -4f)
        val preset = manual.copy(recommended = true)
        assertEquals(SoundMode.RECOMMENDED, preset.mode)
        assertEquals(manual, preset.copy(recommended = false))
        assertEquals(SoundMode.ORIGINAL, SoundSettings().mode)
        assertEquals(SoundMode.ORIGINAL, preset.copy(enabled = false).mode)
    }
}
