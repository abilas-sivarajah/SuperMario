package com.supanga.bros.core

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

enum class Song { NONE, OVERWORLD, UNDERGROUND, CASTLE, STAR, CLEAR, DEATH, GAME_OVER, CASTLE_CLEAR, HURRY }

enum class Sfx {
    JUMP_SMALL, JUMP_BIG, COIN, BUMP, BREAK, STOMP, KICK, POWER_APPEAR, POWER_UP, ONE_UP,
    FIREBALL, PIPE, FLAGPOLE, BEEP, FIREWORK, BOSS_FIRE, BOSS_FALL, PAUSE,
}

/** What the game needs from the audio system. */
interface Sound {
    fun play(s: Sfx)
    fun music(song: Song, fast: Boolean = false)
    fun stopMusic()
    /** True once a non-looping song (jingle) has played to its end. */
    val musicDone: Boolean
}

object NoSound : Sound {
    override fun play(s: Sfx) {}
    override fun music(song: Song, fast: Boolean) {}
    override fun stopMusic() {}
    override val musicDone = true
}

private enum class Wave { PULSE12, PULSE25, PULSE50, TRIANGLE, NOISE }

private class Note(val freq: Float, val steps: Int, val drum: Char = ' ')

private class Track(val wave: Wave, val vol: Float, val notes: List<Note>)

private class SongData(val stepSec: Float, val loop: Boolean, val tracks: List<Track>)

private fun noteFreq(name: String): Float {
    val letter = name[0].uppercaseChar()
    var i = 1
    var semi = when (letter) { 'C' -> 0; 'D' -> 2; 'E' -> 4; 'F' -> 5; 'G' -> 7; 'A' -> 9; 'B' -> 11; else -> error(name) }
    while (i < name.length && (name[i] == '#' || name[i] == 'b')) {
        semi += if (name[i] == '#') 1 else -1
        i++
    }
    val octave = name.substring(i).toInt()
    val midi = 12 * (octave + 1) + semi
    return (440.0 * 2.0.pow((midi - 69) / 12.0)).toFloat()
}

/** Parses "E5:2 -:1 C6:3 k:2" (note:steps, '-' is a rest, k/s/h are drums). */
private fun parse(s: String): List<Note> = s.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { tok ->
    val parts = tok.split(':')
    val steps = if (parts.size > 1) parts[1].toInt() else 1
    when (val n = parts[0]) {
        "-" -> Note(0f, steps)
        "k", "s", "h" -> Note(0f, steps, n[0])
        else -> Note(noteFreq(n), steps)
    }
}

private object Songs {
    private fun bass(r: String, f: String, o: String) = "$r:3 -:1 $f:2 $r:2 $o:3 -:1 $f:2 $r:2"
    private fun stabs(a: String, b: String = a) = "-:2 $a:2 -:2 $a:2 -:2 $b:2 -:2 $b:2"
    private const val BEAT = "k:2 h:2 s:2 h:2 k:2 h:2 s:2 h:1 h:1"

    private val overworld = SongData(0.075f, true, listOf(
        Track(Wave.PULSE25, 0.11f, parse(
            "E5:2 G5:2 -:1 C6:3 B5:2 G5:2 E5:2 G5:2 " +
                "A5:3 G5:1 E5:2 C5:2 E5:2 A5:4 -:2 " +
                "F5:2 A5:2 C6:3 A5:1 G5:2 F5:2 A5:4 " +
                "G5:3 F5:1 D5:2 B4:2 D5:2 G5:4 -:2 " +
                "E5:2 G5:2 -:1 C6:3 D6:2 E6:2 D6:2 C6:2 " +
                "A5:3 C6:1 B5:2 A5:2 E5:2 C6:4 -:2 " +
                "F5:2 A5:2 D6:4 D5:2 G5:2 B5:4 " +
                "C6:4 G5:2 E5:2 C5:4 -:4 " +
                "A5:2 A5:2 -:2 A5:2 C6:3 A5:1 F5:4 " +
                "B5:2 B5:2 -:2 B5:2 D6:3 B5:1 G5:4 " +
                "G5:2 B5:2 E6:4 D6:2 B5:2 G5:4 " +
                "A5:2 C6:2 E6:4 D6:2 C6:2 A5:4 " +
                "F5:2 A5:2 C6:2 F6:2 E6:2 C6:2 A5:4 " +
                "G5:2 B5:2 D6:2 G6:2 F6:2 D6:2 B5:4 " +
                "C6:3 G5:1 E5:2 G5:2 C6:3 G5:1 E6:4 " +
                "D6:2 -:2 D6:2 -:2 D6:2 C6:2 B5:2 G5:2")),
        Track(Wave.PULSE50, 0.05f, parse(listOf(
            stabs("E4"), stabs("C5"), stabs("A4"), stabs("B4"), stabs("G4"), stabs("C5"), stabs("F4", "B4"), stabs("E4"),
            stabs("A4"), stabs("B4"), stabs("G4"), stabs("C5"), stabs("A4"), stabs("B4"), stabs("E4"), stabs("B4"),
        ).joinToString(" "))),
        Track(Wave.TRIANGLE, 0.22f, parse(listOf(
            bass("C3", "G2", "C4"), bass("A2", "E2", "A3"), bass("F2", "C3", "F3"), bass("G2", "D3", "G3"),
            bass("C3", "G2", "C4"), bass("A2", "E2", "A3"), "D3:3 -:1 A2:2 D3:2 G2:3 -:1 D3:2 G2:2", bass("C3", "G2", "C4"),
            bass("F2", "C3", "F3"), bass("G2", "D3", "G3"), bass("E2", "B2", "E3"), bass("A2", "E2", "A3"),
            bass("F2", "C3", "F3"), bass("G2", "D3", "G3"), bass("C3", "G2", "C4"), bass("G2", "D3", "G3"),
        ).joinToString(" "))),
        Track(Wave.NOISE, 0.07f, parse(List(16) { BEAT }.joinToString(" "))),
    ))

    private const val UG1 = "E4:1 -:1 E5:1 -:1 D5:1 -:1 B4:1 -:1 G4:1 -:1 A4:1 -:1 B4:1 -:3"
    private const val UG2 = "C4:1 -:1 C5:1 -:1 B4:1 -:1 G4:1 -:1 E4:1 -:1 F#4:1 -:1 G4:1 -:3"
    private const val UG3 = "A3:1 -:1 A4:1 -:1 G4:1 -:1 E4:1 -:1 C4:1 -:1 D4:1 -:1 E4:1 -:3"
    private const val UG4 = "B3:1 -:1 B4:1 -:1 A4:1 -:1 F#4:1 -:1 D#4:1 -:1 F#4:1 -:1 B4:1 -:3"
    private const val UG4B = "B3:1 -:1 B4:1 -:1 D#5:1 -:1 F#5:1 -:1 B5:2 -:6"
    private val underground = SongData(0.09f, true, listOf(
        Track(Wave.PULSE50, 0.10f, parse("$UG1 $UG2 $UG3 $UG4 $UG1 $UG2 $UG3 $UG4B -:16")),
        Track(Wave.TRIANGLE, 0.22f, parse(List(2) {
            "E2:2 -:6 E2:2 -:2 B2:2 -:2 C2:2 -:6 C2:2 -:2 G2:2 -:2 A2:2 -:6 A2:2 -:2 E2:2 -:2 B2:2 -:6 B2:2 -:2 F#2:2 -:2"
        }.joinToString(" ") + " -:16")),
    ))

    private fun trill(a: String, b: String, c: String, d: String) = List(4) { "$a:1 $b:1 $c:1 $d:1" }.joinToString(" ")
    private val castle = SongData(0.07f, true, listOf(
        Track(Wave.PULSE25, 0.08f, parse(
            trill("E5", "F5", "E5", "D#5") + " " + trill("G5", "G#5", "G5", "F#5") + " " +
                trill("C5", "C#5", "C5", "B4") + " " + trill("D#5", "E5", "D#5", "D5"))),
        Track(Wave.PULSE50, 0.05f, parse(
            trill("B4", "C5", "B4", "A#4") + " " + trill("D5", "D#5", "D5", "C#5") + " " +
                trill("G4", "G#4", "G4", "F#4") + " " + trill("A#4", "B4", "A#4", "A4"))),
        Track(Wave.TRIANGLE, 0.22f, parse(
            List(4) { "E2:2 E3:2" }.joinToString(" ") + " " + List(4) { "G2:2 G3:2" }.joinToString(" ") + " " +
                List(4) { "C2:2 C3:2" }.joinToString(" ") + " " + List(4) { "B1:2 B2:2" }.joinToString(" "))),
    ))

    private val star = SongData(0.065f, true, listOf(
        Track(Wave.PULSE25, 0.10f, parse(
            "G5:1 -:1 G5:1 -:1 E6:2 D6:2 C6:1 -:1 G5:1 -:1 C6:2 D6:2 " +
                "A5:1 -:1 A5:1 -:1 F6:2 E6:2 D6:1 -:1 A5:1 -:1 D6:2 E6:2")),
        Track(Wave.PULSE50, 0.05f, parse(
            "E5:1 -:1 E5:1 -:1 C6:2 B5:2 A5:1 -:1 E5:1 -:1 A5:2 B5:2 " +
                "F5:1 -:1 F5:1 -:1 D6:2 C6:2 B5:1 -:1 F5:1 -:1 B5:2 C6:2")),
        Track(Wave.TRIANGLE, 0.22f, parse(List(4) { "C3:2 G3:2" }.joinToString(" ") + " " + List(4) { "D3:2 A3:2" }.joinToString(" "))),
        Track(Wave.NOISE, 0.07f, parse(List(4) { "k:2 h:2 s:2 h:2" }.joinToString(" "))),
    ))

    private val clear = SongData(0.08f, false, listOf(
        Track(Wave.PULSE25, 0.11f, parse("C5:2 E5:2 G5:2 C6:4 B5:2 C6:2 D6:4 -:2 E6:2 D6:2 C6:2 G5:2 A5:2 B5:2 C6:12")),
        Track(Wave.PULSE50, 0.06f, parse("E4:2 G4:2 C5:2 E5:4 D5:2 E5:2 F5:4 -:2 G5:2 F5:2 E5:2 D5:2 F5:2 G5:2 E5:12")),
        Track(Wave.TRIANGLE, 0.22f, parse("C3:4 C3:4 F2:4 G2:4 -:2 C3:4 E2:4 F2:2 G2:4 C3:12")),
    ))

    private val death = SongData(0.07f, false, listOf(
        Track(Wave.PULSE25, 0.11f, parse("G5:2 F#5:2 F5:2 E5:4 -:2 C5:2 D5:2 B4:2 G4:4 -:2 E4:2 F4:2 D4:2 C4:8")),
        Track(Wave.TRIANGLE, 0.22f, parse("G2:4 -:4 C3:4 -:2 G2:4 -:4 C2:8")),
    ))

    private val gameOver = SongData(0.11f, false, listOf(
        Track(Wave.PULSE25, 0.10f, parse("E5:4 C5:4 A4:4 -:2 F4:4 G4:4 A4:2 G4:4 E4:4 D4:4 C4:12")),
        Track(Wave.TRIANGLE, 0.22f, parse("A2:12 -:2 F2:10 G2:8 C2:12")),
    ))

    private val castleClear = SongData(0.09f, false, listOf(
        Track(Wave.PULSE25, 0.11f, parse("C5:2 C5:1 C5:1 G5:4 E5:2 C5:2 G5:4 A5:2 B5:2 C6:4 B5:2 A5:2 G5:4 E5:2 F5:2 G5:4 A5:2 G5:2 C6:16")),
        Track(Wave.PULSE50, 0.06f, parse("E4:2 E4:1 E4:1 E5:4 C5:2 G4:2 E5:4 F5:2 G5:2 A5:4 G5:2 F5:2 E5:4 C5:2 D5:2 E5:4 F5:2 E5:2 E5:16")),
        Track(Wave.TRIANGLE, 0.22f, parse("C3:8 C3:8 F2:8 G2:8 C3:8 F2:4 G2:4 C3:16")),
    ))

    private val hurry = SongData(0.07f, false, listOf(
        Track(Wave.PULSE25, 0.11f, parse("E6:1 -:1 E6:1 -:1 E6:2 -:2 F6:1 -:1 F6:1 -:1 F6:2 -:2 G6:1 -:1 G6:1 -:1 G6:4")),
        Track(Wave.PULSE50, 0.06f, parse("C6:1 -:1 C6:1 -:1 C6:2 -:2 D6:1 -:1 D6:1 -:1 D6:2 -:2 E6:1 -:1 E6:1 -:1 E6:4")),
    ))

    fun get(s: Song): SongData? = when (s) {
        Song.NONE -> null
        Song.OVERWORLD -> overworld
        Song.UNDERGROUND -> underground
        Song.CASTLE -> castle
        Song.STAR -> star
        Song.CLEAR -> clear
        Song.DEATH -> death
        Song.GAME_OVER -> gameOver
        Song.CASTLE_CLEAR -> castleClear
        Song.HURRY -> hurry
    }

    /** Sanity check used by tests: every bar-based track of a looping song has the same length. */
    fun lengths(s: Song): List<Int> = get(s)?.tracks?.map { t -> t.notes.sumOf { it.steps } } ?: emptyList()
}

/** A sound effect segment: frequency glides from f0 to f1 while the volume goes from v0 to v1. */
private class Seg(val wave: Wave, val f0: Float, val f1: Float, val dur: Float, val v0: Float = 0.5f, val v1: Float = 0.3f)

private object Effects {
    private fun notes(names: String, dur: Float, wave: Wave = Wave.PULSE50, v: Float = 0.45f) =
        names.split(' ').map { val f = noteFreq(it); Seg(wave, f, f, dur, v, v * 0.7f) }

    fun get(s: Sfx): List<Seg> = when (s) {
        Sfx.JUMP_SMALL -> listOf(Seg(Wave.PULSE25, 260f, 820f, 0.16f, 0.5f, 0.25f))
        Sfx.JUMP_BIG -> listOf(Seg(Wave.PULSE25, 190f, 600f, 0.20f, 0.5f, 0.25f))
        Sfx.COIN -> listOf(Seg(Wave.PULSE50, 988f, 988f, 0.06f, 0.45f, 0.45f), Seg(Wave.PULSE50, 1319f, 1319f, 0.35f, 0.45f, 0.0f))
        Sfx.BUMP -> listOf(Seg(Wave.PULSE50, 180f, 70f, 0.09f, 0.6f, 0.2f))
        Sfx.BREAK -> listOf(Seg(Wave.NOISE, 3000f, 600f, 0.28f, 0.7f, 0.0f))
        Sfx.STOMP -> listOf(Seg(Wave.PULSE50, 700f, 160f, 0.07f, 0.55f, 0.3f), Seg(Wave.PULSE50, 320f, 140f, 0.06f, 0.4f, 0.0f))
        Sfx.KICK -> listOf(Seg(Wave.PULSE12, 900f, 260f, 0.06f, 0.55f, 0.1f))
        Sfx.POWER_APPEAR -> notes("C5 E5 G5 C6 E6 G6 D5 F#5 A5 D6 F#6 A6", 0.035f)
        Sfx.POWER_UP -> notes("C5 G5 C6 E5 B5 E6 G5 D6 G6 A5 E6 A6 C6 G6 C7", 0.035f)
        Sfx.ONE_UP -> notes("G5 C6 E6 G6 C7 G6", 0.08f, Wave.PULSE25)
        Sfx.FIREBALL -> listOf(Seg(Wave.PULSE12, 1300f, 380f, 0.06f, 0.4f, 0.1f))
        Sfx.PIPE -> List(3) { listOf(Seg(Wave.PULSE50, 420f, 190f, 0.07f, 0.5f, 0.3f), Seg(Wave.PULSE50, 1f, 1f, 0.04f, 0f, 0f)) }.flatten()
        Sfx.FLAGPOLE -> listOf(Seg(Wave.PULSE25, 1400f, 220f, 1.0f, 0.4f, 0.25f))
        Sfx.BEEP -> listOf(Seg(Wave.PULSE50, 1760f, 1760f, 0.025f, 0.3f, 0.3f))
        Sfx.FIREWORK -> listOf(Seg(Wave.NOISE, 1800f, 500f, 0.35f, 0.8f, 0.0f))
        Sfx.BOSS_FIRE -> listOf(Seg(Wave.NOISE, 1400f, 700f, 0.6f, 0.6f, 0.05f))
        Sfx.BOSS_FALL -> listOf(Seg(Wave.PULSE25, 700f, 50f, 1.3f, 0.5f, 0.1f))
        Sfx.PAUSE -> notes("E6 C6 E6 C6", 0.08f)
    }
}

/**
 * Tiny chiptune synthesizer: 2 pulse channels + triangle + noise for music and two effect voices.
 * [render] is called from the audio thread; the control methods may be called from any thread.
 */
class Synth(private val rate: Int = 22050) : Sound {
    private sealed class Cmd {
        class Music(val song: Song, val fast: Boolean) : Cmd()
        class Effect(val sfx: Sfx) : Cmd()
        object Stop : Cmd()
    }

    private val queue = ConcurrentLinkedQueue<Cmd>()

    @Volatile
    private var done = true
    override val musicDone get() = done

    override fun play(s: Sfx) { queue.add(Cmd.Effect(s)) }
    override fun music(song: Song, fast: Boolean) { done = false; queue.add(Cmd.Music(song, fast)) }
    override fun stopMusic() { queue.add(Cmd.Stop) }

    private class Voice {
        var phase = 0f
        var freq = 0f
        var vol = 0f
        var lfsr = 1
        var noiseOut = 1f
        var noiseAcc = 0f
    }

    private inner class Player(val data: SongData, fast: Boolean) {
        val stepSamples = data.stepSec / (if (fast) 1.4f else 1f) * rate
        val noteIdx = IntArray(data.tracks.size)
        val left = FloatArray(data.tracks.size) // samples left in current note
        val len = FloatArray(data.tracks.size)
        val voices = Array(data.tracks.size) { Voice() }
        val drum = CharArray(data.tracks.size) { ' ' }
        val finished = BooleanArray(data.tracks.size)

        init {
            for (i in data.tracks.indices) start(i)
        }

        fun start(i: Int) {
            val t = data.tracks[i]
            if (noteIdx[i] >= t.notes.size) {
                if (data.loop) noteIdx[i] = 0 else { finished[i] = true; voices[i].vol = 0f; return }
            }
            val n = t.notes[noteIdx[i]]
            len[i] = n.steps * stepSamples
            left[i] += len[i]
            voices[i].freq = n.freq
            drum[i] = n.drum
            voices[i].vol = if (n.freq > 0f || n.drum != ' ') t.vol else 0f
        }

        fun sample(): Float {
            var out = 0f
            for (i in data.tracks.indices) {
                if (finished[i]) continue
                val t = data.tracks[i]
                val v = voices[i]
                if (v.vol > 0f) {
                    val elapsed = (len[i] - left[i]) / rate
                    val gate = left[i] > len[i] * 0.12f || t.wave == Wave.TRIANGLE && left[i] > 40
                    val env = when {
                        t.wave == Wave.NOISE -> when (drum[i]) {
                            'k' -> exp(-elapsed / 0.06f)
                            's' -> exp(-elapsed / 0.09f)
                            else -> exp(-elapsed / 0.025f)
                        }
                        !gate -> 0f
                        t.wave == Wave.TRIANGLE -> 1f
                        else -> 0.75f + 0.25f * exp(-elapsed / 0.08f)
                    }
                    val rateHz = when (drum[i]) { 'k' -> 1600f; 's' -> 7000f; 'h' -> 20000f; else -> v.freq }
                    out += osc(t.wave, v, rateHz) * v.vol * env
                }
                left[i] -= 1f
                if (left[i] <= 0f) {
                    noteIdx[i]++
                    start(i)
                }
            }
            if (!data.loop && finished.all { it }) done = true
            return out
        }
    }

    private inner class EffectPlayer(val segs: List<Seg>) {
        var seg = 0
        var pos = 0f
        val voice = Voice()
        val active get() = seg < segs.size

        fun sample(): Float {
            if (!active) return 0f
            val s = segs[seg]
            val t = pos / (s.dur * rate)
            // exponential glide sounds more natural than linear
            val f = if (s.f0 > 0f && s.f1 > 0f) (s.f0 * exp(ln(s.f1 / s.f0) * t)) else s.f0
            val v = s.v0 + (s.v1 - s.v0) * t
            val out = osc(s.wave, voice, f) * v * 0.32f
            pos++
            if (pos >= s.dur * rate) { seg++; pos = 0f }
            return out
        }
    }

    private var player: Player? = null
    private var tone: EffectPlayer? = null
    private var noise: EffectPlayer? = null

    private fun osc(w: Wave, v: Voice, f: Float): Float {
        if (w == Wave.NOISE) {
            v.noiseAcc += f / rate
            while (v.noiseAcc >= 1f) {
                v.noiseAcc -= 1f
                val bit = (v.lfsr xor (v.lfsr shr 1)) and 1
                v.lfsr = (v.lfsr shr 1) or (bit shl 14)
                v.noiseOut = if (v.lfsr and 1 == 0) 1f else -1f
            }
            return v.noiseOut
        }
        v.phase += f / rate
        if (v.phase >= 1f) v.phase -= v.phase.toInt()
        val p = v.phase
        return when (w) {
            Wave.PULSE12 -> if (p < 0.125f) 1f else -1f
            Wave.PULSE25 -> if (p < 0.25f) 1f else -1f
            Wave.PULSE50 -> if (p < 0.5f) 1f else -1f
            Wave.TRIANGLE -> 4f * kotlin.math.abs(p - 0.5f) - 1f
            Wave.NOISE -> 0f
        }
    }

    private fun handleCommands() {
        while (true) {
            when (val c = queue.poll() ?: break) {
                is Cmd.Music -> {
                    val d = Songs.get(c.song)
                    player = d?.let { Player(it, c.fast) }
                    if (d == null) done = true
                }
                is Cmd.Stop -> { player = null; done = true }
                is Cmd.Effect -> {
                    val segs = Effects.get(c.sfx)
                    val p = EffectPlayer(segs)
                    if (segs.first().wave == Wave.NOISE) noise = p else tone = p
                }
            }
        }
    }

    /** Fills [out] with [n] mono 16-bit samples. */
    fun render(out: ShortArray, n: Int) {
        handleCommands()
        val p = player
        val t = tone
        val z = noise
        for (i in 0 until n) {
            var s = 0f
            if (p != null) s += p.sample() * (if (t != null && t.active) 0.6f else 1f)
            if (t != null) s += t.sample()
            if (z != null) s += z.sample()
            val v = (s * 32767f * 0.8f).toInt().coerceIn(-32768, 32767)
            out[i] = v.toShort()
        }
        if (t != null && !t.active) tone = null
        if (z != null && !z.active) noise = null
    }

    companion object {
        fun trackLengths(s: Song): List<Int> = Songs.lengths(s)
    }
}
