package dev.aiko.music.playback

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import kotlin.random.Random

class TrackLoudnessTest {
    private fun tone(rate: Int, channels: Int, amplitude: Double, seconds: Int = 5, silence: Boolean = false): TrackLoudness {
        val meter = TrackLoudnessMeter(rate, channels)
        val frame = FloatArray(channels)
        repeat(rate * seconds) { i ->
            frame.fill((amplitude * sin(2 * PI * 1000 * i / rate)).toFloat())
            meter.add(frame)
        }
        if (silence) { frame.fill(0f); repeat(rate * 10) { meter.add(frame) } }
        return meter.result()
    }
    @Test fun calibratedStereoOneKhzAndDifferentRatesAgree() {
        for (rate in listOf(44100, 48000, 96000)) {
            val level = tone(rate, 2, .1)
            assertEquals("$rate Hz calibration", -20.0, level.lufs, .12)
            assertEquals(.1, level.peak, .0001)
        }
        assertEquals(3.0103, tone(48000, 2, .1).lufs - tone(48000, 1, .1).lufs, .001)
    }
    @Test fun levelChangesAreMeasuredInDbAndSilenceDoesNotBoostTracks() {
        val quiet = tone(48000, 2, .04)
        val loud = tone(48000, 2, .4)
        assertEquals(20.0, loud.lufs - quiet.lufs, .001)
        assertEquals(loud.lufs, tone(48000, 2, .4, silence = true).lufs, .3)
        val silence = TrackLoudnessMeter(48000, 2)
        repeat(48000) { silence.add(floatArrayOf(0f, Float.NaN)) }
        assertEquals(0f, silence.result().gainDb, 0f)
    }
    @Test fun fixedTrackGainMatchesTargetWithoutSqueezingQuietSections() {
        val loud = tone(48000, 2, .4)
        val quiet = tone(48000, 2, .1)
        assertEquals(-16.0, loud.lufs + loud.gainDb, .001)
        assertEquals(-16.0, quiet.lufs + quiet.gainDb, .001)
        val highCrest = TrackLoudness(-24.0, .8)
        assertTrue(highCrest.gainDb < 0f)
        assertEquals(-2.0, 20 * log10(highCrest.peak) + highCrest.gainDb, .0001)
        assertEquals(6f, TrackLoudness(-50.0, .001).gainDb, 0f)
        assertEquals(-18f, TrackLoudness(6.0, .8).gainDb, 0f)
        // Within a track the same fixed multiplier retains the original 20 dB contrast.
        val gain = TrackGain(48000, -8f)
        val a = .1f * gain.nextGain(-8f); val b = .01f * gain.nextGain(-8f)
        assertEquals(10f, a / b, .0001f)
    }
    @Test fun gainTransitionsBoundAllChannelsAndDoNotClickOrLeak() {
        for (rate in listOf(8000, 44100, 48000, 96000)) for (channels in listOf(1, 2, 6)) {
            val gain = TrackGain(rate, -18f)
            var previous = 0f
            repeat(rate * 2) { i ->
                val current = .2f * gain.nextGain(if (i < rate) -18f else 6f)
                assertTrue(abs(current - previous) < .03f)
                previous = current
            }
            val engine = RecommendedEngine(rate, channels, initialGainDb = 6f)
            val frame = FloatArray(channels); val output = FloatArray(channels)
            val random = Random(17)
            repeat(rate) { i ->
                for (c in frame.indices) frame[c] = when (i % 100) {
                    0 -> Float.NaN; 1 -> Float.POSITIVE_INFINITY; else -> random.nextFloat() * 2 - 1
                }
                engine.processFrame(frame, output, if (i < rate / 2) 6f else -18f)
                for (x in output) assertTrue(x.isFinite() && abs(x) <= RecommendedEngine.CEILING + .000002f)
            }
        }
    }
    @Test fun normalizationBeforeLimitingPreservesTransientShapeAndChannelRatio() {
        val rate = 48000
        val reference = RecommendedEngine(rate, 2, analysisOnly = true)
        val meter = TrackLoudnessMeter(rate, 2)
        val pcm = FloatArray(rate * 4)
        val input = FloatArray(2); val output = FloatArray(2)
        repeat(pcm.size) { i ->
            pcm[i] = (.9 * sin(2 * PI * 55 * i / rate) + .09 * sin(2 * PI * 1500 * i / rate)).toFloat()
            input[0] = pcm[i]; input[1] = pcm[i] * .6f
            reference.processFrame(input, output); meter.add(output)
        }
        val level = meter.result()
        assertTrue("Healthy transient source: ${level.peak}", level.peak > .5)
        val normalized = RecommendedEngine(rate, 2, initialGainDb = level.gainDb)
        val freshReference = RecommendedEngine(rate, 2, analysisOnly = true)
        val linear = 10.0.pow(level.gainDb / 20.0).toFloat()
        val expected = FloatArray(2)
        repeat(pcm.size) { i ->
            input[0] = pcm[i]; input[1] = pcm[i] * .6f
            normalized.processFrame(input, output)
            freshReference.processFrame(input, expected)
            for (c in output.indices) assertEquals(expected[c] * linear, output[c], .000002f)
        }
    }
    @Test fun selectedGainIsExactAtStartupAndDeClicksSettleBeforeFiftyMs() {
        for (rate in listOf(8000, 44100, 48000, 96000)) {
            val gain = TrackGain(rate, TrackLoudness.UNCALIBRATED_GAIN_DB)
            val chosen = 10.0.pow(-9.0 / 20).toFloat()
            repeat(rate * 3) { assertEquals(chosen, gain.nextGain(-9f), .000001f) }
            repeat(rate / 20) { gain.nextGain(-12f) }
            assertEquals(10.0.pow(-12.0 / 20).toFloat(), gain.nextGain(-12f), .00003f)
        }
    }
}
