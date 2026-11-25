package dev.aiko.music.playback

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.Process
import android.os.SystemClock
import android.provider.MediaStore
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Private, bounded cache of complete-track measurements. Never modifies music files.
 * One background decoder, current + next only; canceled on track/mode changes or service exit.
 */
class LoudnessAnalysis(private val context: Context, private val handler: Handler) : AutoCloseable {
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); runnable.run() }, "Music-track-level")
    }
    private val file = AtomicFile(File(context.filesDir, "track-levels-v1.json"))
    private val cacheLock = Any()
    private val cache by lazy {
        runCatching { JSONObject(file.openRead().bufferedReader().use { it.readText() }) }.getOrElse { JSONObject() }
    }
    private var job: Future<*>? = null
    private var generation = 0L
    fun cancel() { generation++; job?.cancel(true); job = null }
    /** Pick ONCE at a playback boundary. A completed analysis never changes this selection. */
    fun select(current: Uri): AppliedLevel {
        val measured = runCatching {
            val key = fingerprint(current) ?: return@runCatching null
            synchronized(cacheLock) { cache.optJSONObject(key) }?.let {
                TrackLoudness(it.getDouble("lufs"), it.getDouble("peak"))
            }
        }.getOrNull()
        return AppliedLevel(current.toString(), measured?.gainDb ?: TrackLoudness.UNCALIBRATED_GAIN_DB,
            measured != null).also { lastApplied = it }
    }
    fun request(current: Uri, next: Uri?) {
        cancel()
        val token = generation
        job = worker.submit {
            for (uri in listOfNotNull(current, next).distinct()) {
                if (Thread.currentThread().isInterrupted) break
                try {
                    val key = fingerprint(uri) ?: continue
                    val saved = synchronized(cacheLock) { cache.optJSONObject(key) }
                    val level = if (saved != null) TrackLoudness(saved.getDouble("lufs"), saved.getDouble("peak"))
                        else measure(context, uri).also { measured ->
                            if (measured.lufs.isFinite()) {
                                // Limit storage to 384 tiny measurement records; not PCM or artwork.
                                val json = synchronized(cacheLock) {
                                    if (cache.length() >= 384) cache.keys().asSequence().firstOrNull()?.let(cache::remove)
                                    cache.put(key, JSONObject().put("lufs", measured.lufs).put("peak", measured.peak))
                                    cache.toString()
                                }
                                val stream = file.startWrite()
                                try { stream.write(json.toByteArray()); file.finishWrite(stream) }
                                catch (t: Throwable) { file.failWrite(stream); throw t }
                            }
                        }
                    if (uri == current) handler.post {
                        if (token == generation) {
                            // Save for a FUTURE playback; never sweep the current song's volume.
                            lastMeasured = MeasuredLevel(uri.toString(), level, saved != null)
                        }
                    }
                } catch (_: Exception) {
                    // Unsupported/revoked/canceled files keep conservative playback; never block it.
                    if (Thread.currentThread().isInterrupted) break
                }
            }
        }
    }
    private fun fingerprint(uri: Uri): String? {
        if (uri.scheme != "content") return null
        return context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED), null, null, null)?.use {
            if (it.moveToFirst()) "clear-bt-v1|$uri|${it.getLong(0)}|${it.getLong(1)}" else null
        }
    }
    override fun close() { cancel(); worker.shutdownNow() }
    data class AppliedLevel(val uri: String, val gainDb: Float, val cached: Boolean)
    data class MeasuredLevel(val uri: String, val level: TrackLoudness, val cached: Boolean)
    companion object {
        @Volatile var lastApplied: AppliedLevel? = null
            private set
        @Volatile var lastMeasured: MeasuredLevel? = null
            private set

        /** Offline PCM decode: no AudioTrack, microphone, network, or audible output. */
        fun measure(context: Context, uri: Uri, gainDb: Float? = null): TrackLoudness {
            val extractor = MediaExtractor()
            var codec: MediaCodec? = null
            try {
                extractor.setDataSource(context, uri, null)
                val track = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                } ?: error("No audio track")
                extractor.selectTrack(track)
                val format = extractor.getTrackFormat(track)
                format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                val decoder = MediaCodec.createDecoderByType(requireNotNull(format.getString(MediaFormat.KEY_MIME)))
                codec = decoder
                decoder.configure(format, null, null, 0)
                decoder.start()
                val info = MediaCodec.BufferInfo()
                var inputEnded = false
                var outputEnded = false
                var engine: RecommendedEngine? = null
                var meter: TrackLoudnessMeter? = null
                var input = FloatArray(0)
                var output = FloatArray(0)
                var encoding = AudioFormat.ENCODING_PCM_16BIT
                var rate = 0
                var channels = 0
                fun configure(actual: MediaFormat) {
                    val actualRate = actual.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val actualChannels = actual.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    if (engine == null) {
                        rate = actualRate; channels = actualChannels
                        require(channels in 1..2 && rate >= 8000)
                        engine = RecommendedEngine(rate, channels, analysisOnly = gainDb == null, initialGainDb = gainDb ?: 0f)
                        meter = TrackLoudnessMeter(rate, channels)
                        input = FloatArray(channels); output = FloatArray(channels)
                        encoding = if (actual.containsKey(MediaFormat.KEY_PCM_ENCODING)) actual.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                        require(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT)
                    }
                    check(rate == actualRate && channels == actualChannels) { "Changing PCM format" }
                }
                fun collectFrame() {
                    if (gainDb != null) for (c in output.indices)
                        output[c] = (output[c] * 32768).toInt().coerceIn(-32768, 32767) / 32768f
                    meter!!.add(output)
                }
                val deadline = SystemClock.elapsedRealtime() + 60000
                while (!outputEnded) {
                    if (Thread.currentThread().isInterrupted) throw InterruptedException()
                    check(SystemClock.elapsedRealtime() < deadline) { "Analysis time budget" }
                    if (!inputEnded) {
                        val index = decoder.dequeueInputBuffer(1000)
                        if (index >= 0) {
                            val buffer = requireNotNull(decoder.getInputBuffer(index))
                            buffer.clear()
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val index = decoder.dequeueOutputBuffer(info, 1000)
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) configure(decoder.outputFormat)
                    if (index >= 0) {
                        try {
                            if (info.size > 0) {
                                if (engine == null) configure(decoder.outputFormat)
                                val buffer = requireNotNull(decoder.getOutputBuffer(index)).order(ByteOrder.nativeOrder())
                                buffer.position(info.offset); buffer.limit(info.offset + info.size)
                                val bytes = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                                while (buffer.remaining() >= channels * bytes) {
                                    for (c in input.indices) input[c] = if (bytes == 4) buffer.float else buffer.short / 32768f
                                    requireNotNull(engine).processFrame(input, output)
                                    collectFrame()
                                }
                            }
                            outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        } finally { decoder.releaseOutputBuffer(index, false) }
                    }
                }
                val dsp = requireNotNull(engine)
                input.fill(0f)
                repeat(dsp.tailFrames) { dsp.processFrame(input, output); collectFrame() }
                return requireNotNull(meter).result()
            } finally {
                codec?.let { runCatching { it.stop() }; it.release() }
                extractor.release()
            }
        }
    }
}
