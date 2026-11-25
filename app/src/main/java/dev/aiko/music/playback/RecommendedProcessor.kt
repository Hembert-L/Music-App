package dev.aiko.music.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

@UnstableApi
class RecommendedProcessor : BaseAudioProcessor() {
    private var engine: RecommendedEngine? = null
    private var input = FloatArray(0)
    private var output = FloatArray(0)
    @Volatile var trackGainDb = TrackLoudness.UNCALIBRATED_GAIN_DB
    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) throw UnhandledAudioFormatException(inputAudioFormat)
        return inputAudioFormat
    }
    override fun onFlush() {
        engine = if (inputAudioFormat.sampleRate > 0 && inputAudioFormat.channelCount > 0)
            RecommendedEngine(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, initialGainDb = trackGainDb) else null
        input = FloatArray(inputAudioFormat.channelCount.coerceAtLeast(0))
        output = FloatArray(input.size)
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val dsp = engine ?: return
        val selectedGain = trackGainDb
        if (lastProcessedGainDb != selectedGain) gainChanges++
        lastProcessedGainDb = selectedGain
        val buffer = replaceOutputBuffer(inputBuffer.remaining())
        val frames = inputBuffer.remaining() / (input.size * 2)
        repeat(frames) {
            for (c in input.indices) input[c] = inputBuffer.short / 32768f
            dsp.processFrame(input, output, selectedGain)
            writeFrame(buffer)
        }
        processedFrames += frames
        buffer.flip()
    }
    private fun writeFrame(buffer: ByteBuffer) {
        for (c in output.indices) buffer.putShort((output[c] * 32768).toInt().coerceIn(-32768, 32767).toShort())
    }
    override fun onQueueEndOfStream() {
        val dsp = engine ?: return
        val buffer = replaceOutputBuffer(dsp.tailFrames * input.size * 2)
        input.fill(0f)
        repeat(dsp.tailFrames) { dsp.processFrame(input, output, trackGainDb); writeFrame(buffer) }
        buffer.flip()
    }
    companion object {
        @Volatile var processedFrames: Long = 0
            private set
        @Volatile var lastProcessedGainDb = TrackLoudness.UNCALIBRATED_GAIN_DB
            private set
        @Volatile var gainChanges = 0L
            private set
    }
}
