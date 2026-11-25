package dev.aiko.music.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

@UnstableApi
class ToneProcessor : BaseAudioProcessor() {
    @Volatile var settings = SoundSettings(enabled = true)
    private var engine: ToneEngine? = null
    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) throw UnhandledAudioFormatException(inputAudioFormat)
        return inputAudioFormat
    }
    override fun onFlush() { engine = if (inputAudioFormat.sampleRate > 0 && inputAudioFormat.channelCount > 0)
        ToneEngine(inputAudioFormat.sampleRate, inputAudioFormat.channelCount) else null }
    override fun queueInput(inputBuffer: ByteBuffer) {
        val sampleCount = inputBuffer.remaining() / 2
        val out = replaceOutputBuffer(inputBuffer.remaining())
        val s = settings.safe()
        var channel = 0
        while (inputBuffer.remaining() >= 2) {
            val x = inputBuffer.short / 32768f
            val y = engine!!.process(x, channel, s)
            out.putShort((y * 32768).toInt().coerceIn(-32768, 32767).toShort())
            channel = (channel + 1) % inputAudioFormat.channelCount
        }
        out.flip()
        processedSamples += sampleCount
    }
    companion object {
        @Volatile var processedSamples: Long = 0
            private set
    }
}
