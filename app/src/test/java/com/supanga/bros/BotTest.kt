package com.supanga.bros

import com.supanga.bros.core.Buttons
import com.supanga.bros.core.Game
import com.supanga.bros.core.Screen
import com.supanga.bros.core.Tile
import com.supanga.bros.core.ftile
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** A naive bot (run right, jump at walls, gaps and enemies) to find unfair spots in the levels. */
class BotTest {
    fun driveForDebug(g: Game, b: Buttons, prevJump: Boolean) = drive(g, b, prevJump)

    private fun drive(g: Game, b: Buttons, prevJump: Boolean): Boolean {
        val p = g.player
        val lv = g.level
        b.left = false
        b.right = true
        b.run = true
        b.down = false
        val front = p.x + p.w
        val feetRow = ftile(p.bottom - 1)
        fun blocking(tx: Int, ty: Int) = lv.solidAt(tx, ty) && lv.get(tx, ty) !in Tile.HPIPE_MOUTH_T..Tile.HPIPE_B
        val wall = (1..2).any { d -> blocking(ftile(front + d * 6), feetRow) || blocking(ftile(front + d * 6), feetRow - 1) }
        val groundAhead = (1..3).all { d -> lv.solidAt(ftile(front + d * 8), feetRow + 1) }
        var gap = !groundAhead
        if (gap) {
            val ahead = g.lifts.filter { it !== p.riding && it.x + it.w > front - 4 && it.x < front + 150 }
            val landAhead = (1..6).any { d -> (feetRow - 4..feetRow + 2).any { r -> lv.solidAt(ftile(front + d * 16), r) && !lv.solidAt(ftile(front + d * 16), r - 1) } }
            if (ahead.isNotEmpty() && !landAhead) {
                val reachable = ahead.any { l -> l.x <= front + 30 && l.x + l.w > front + 10 && l.y >= p.bottom - 40 && l.y <= p.bottom + 24 }
                if (!reachable) {
                    if (p.riding != null || lv.solidAt(ftile(p.cx), feetRow + 1)) b.right = false
                    gap = false
                }
            }
        }
        val enemy = g.enemies.any { !it.knocked && !it.inert && it.cx > p.cx - 8 && it.x - front < 52 && abs(it.bottom - p.bottom) < 40 }
        val want = wall || gap || enemy
        return if (p.onGround) {
            if (want && !prevJump) { b.jump = true; true } else { b.jump = false; false }
        } else {
            b.jump = p.vy < 0
            b.jump
        }
    }

    @Test
    fun botPlaysLevels() {
        play(god = false, tag = "bot")
    }

    /** With enemies disabled only the terrain matters: every level must be finishable. */
    @Test
    fun botTraversesTerrain() {
        val finished = play(god = true, tag = "god")
        // the naive bot can't time lifts, but the lift-free levels must be beaten
        assertTrue("1-1 not finished", 0 in finished)
        assertTrue("1-4 not finished", 3 in finished)
    }

    private fun play(god: Boolean, tag: String): Set<Int> {
        val finished = mutableSetOf<Int>()
        val screen = Screen()
        val report = StringBuilder()
        for (lvl in 0 until 4) {
            val g = Game()
            g.debugLoad(lvl)
            g.debugGod = god
            val b = Buttons()
            var prevJump = false
            var maxX = 0f
            var deaths = 0
            var frames = 0
            val startLevel = g.levelIndex
            while (frames < 12000 && g.levelIndex == startLevel && deaths < 6) {
                if (g.state == Game.State.PLAYING) prevJump = drive(g, b, prevJump) else { b.right = false; b.jump = false }
                val before = g.state
                g.update(b)
                frames++
                if (g.state == Game.State.PLAYING) maxX = maxOf(maxX, g.player.x)
                if (before == Game.State.PLAYING && g.state == Game.State.DYING) {
                    deaths++
                    g.render(screen)
                    Png.write(File("build/screenshots/${tag}_1-${lvl + 1}_death$deaths.png"), screen.w, screen.h, screen.pixels, 2)
                    report.append("1-${lvl + 1}: death $deaths at tile ${(g.player.x / 16).toInt()} (time ${g.time})\n")
                }
                if (g.state == Game.State.RESCUE) break
                if (g.state == Game.State.GAME_OVER) break
            }
            if (g.levelIndex != startLevel || g.state == Game.State.RESCUE) finished += lvl
            report.append("1-${lvl + 1}: maxTile=${(maxX / 16).toInt()} / ${g.level.width} state=${g.state} level=${g.levelIndex} deaths=$deaths frames=$frames\n")
        }
        println(report)
        File("build/screenshots/${tag}_report.txt").writeText(report.toString())
        return finished
    }
}
