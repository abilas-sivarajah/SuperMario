package com.supanga.bros

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.supanga.bros.core.Storage
import com.supanga.bros.core.Synth

/** Streams the [Synth] output to an [AudioTrack] on its own thread. */
class AndroidAudio(private val synth: Synth, private val rate: Int) {
    @Volatile
    private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({
            val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(rate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, 4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()
            } catch (e: Exception) {
                running = false
                return@Thread
            }
            val buf = ShortArray(512)
            try {
                track.play()
                while (running) {
                    synth.render(buf, buf.size)
                    track.write(buf, 0, buf.size)
                }
            } catch (_: Exception) {
            } finally {
                try { track.stop() } catch (_: Exception) {}
                track.release()
            }
        }, "audio").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }
}

class PrefsStorage(context: Context) : Storage {
    private val prefs = context.getSharedPreferences("supa_nga_bros", Context.MODE_PRIVATE)
    override fun loadTop() = prefs.getInt("top", 0)
    override fun saveTop(value: Int) = prefs.edit().putInt("top", value).apply()
}
