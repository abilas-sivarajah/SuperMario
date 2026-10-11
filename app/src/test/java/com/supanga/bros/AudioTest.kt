package com.supanga.bros

import com.supanga.bros.core.Sfx
import com.supanga.bros.core.Song
import com.supanga.bros.core.Synth
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataOutputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/** Renders every song and effect offline, checks levels and writes WAVs to build/audio for listening. */
class AudioTest {
    private val rate = 22050

    private fun render(seconds: Float, setup: (Synth) -> Unit): ShortArray {
        val synth = Synth(rate)
        setup(synth)
        val out = ShortArray((seconds * rate).toInt())
        val buf = ShortArray(512)
        var pos = 0
        while (pos < out.size) {
            synth.render(buf, buf.size)
            val n = minOf(buf.size, out.size - pos)
            System.arraycopy(buf, 0, out, pos, n)
            pos += n
        }
        return out
    }

    private fun wav(name: String, data: ShortArray) {
        val f = File("build/audio/$name.wav")
        f.parentFile.mkdirs()
        DataOutputStream(f.outputStream().buffered()).use { o ->
            fun le32(v: Int) { o.write(v and 255); o.write((v shr 8) and 255); o.write((v shr 16) and 255); o.write((v shr 24) and 255) }
            fun le16(v: Int) { o.write(v and 255); o.write((v shr 8) and 255) }
            o.writeBytes("RIFF"); le32(36 + data.size * 2); o.writeBytes("WAVEfmt ")
            le32(16); le16(1); le16(1); le32(rate); le32(rate * 2); le16(2); le16(16)
            o.writeBytes("data"); le32(data.size * 2)
            for (s in data) le16(s.toInt())
        }
    }

    @Test
    fun songsHaveSaneLevels() {
        for (song in Song.entries.filter { it != Song.NONE }) {
            val d = render(12f) { it.music(song) }
            val peak = d.maxOf { abs(it.toInt()) }
            val rms = sqrt(d.map { it.toDouble() * it }.average())
            println("$song peak=$peak rms=${rms.toInt()}")
            assertTrue("$song too quiet", rms > 500)
            assertTrue("$song clips", peak < 32000)
            wav("song_${song.name.lowercase()}", d)
        }
    }

    @Test
    fun effectsHaveSaneLevels() {
        for (sfx in Sfx.entries) {
            val d = render(1.5f) { it.play(sfx) }
            val peak = d.maxOf { abs(it.toInt()) }
            println("$sfx peak=$peak")
            assertTrue("$sfx silent", peak > 1000)
            assertTrue("$sfx clips", peak < 32000)
            wav("sfx_${sfx.name.lowercase()}", d)
        }
    }

    @Test
    fun jingleFinishes() {
        val synth = Synth(rate)
        synth.music(Song.DEATH)
        val buf = ShortArray(512)
        repeat((5 * rate) / 512) { synth.render(buf, buf.size) }
        assertTrue(synth.musicDone)
    }
}
