package com.supanga.bros

import com.supanga.bros.core.Boss
import com.supanga.bros.core.Buttons
import com.supanga.bros.core.Game
import com.supanga.bros.core.PMode
import com.supanga.bros.core.Synth
import com.supanga.bros.core.Song
import com.supanga.bros.core.Tile
import com.supanga.bros.core.Walker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayTest {
    private val pad = Buttons()

    private fun Game.run(frames: Int, setup: Buttons.() -> Unit = {}) {
        pad.left = false; pad.right = false; pad.jump = false; pad.run = false; pad.down = false; pad.up = false; pad.start = false
        pad.setup()
        repeat(frames) { update(pad) }
        pad.left = false; pad.right = false; pad.jump = false; pad.run = false; pad.down = false; pad.up = false
    }

    @Test
    fun standingJumpReachesFourTiles() {
        val g = Game()
        g.debugLoad(0, 3, noEnemies = true)
        g.run(2)
        val startY = g.player.y
        var minY = startY
        pad.jump = true
        repeat(60) { g.update(pad); minY = minOf(minY, g.player.y) }
        val rise = startY - minY
        assertTrue("rise was $rise", rise in 58f..68f)
    }

    @Test
    fun runningJumpIsHigher() {
        val g = Game()
        g.debugLoad(0, 3, noEnemies = true)
        g.run(80) { right = true; run = true }
        val startY = g.player.y
        var minY = startY
        pad.right = true; pad.run = true; pad.jump = true
        repeat(50) { g.update(pad); minY = minOf(minY, g.player.y) }
        val rise = startY - minY
        assertTrue("rise was $rise", rise in 70f..84f)
    }

    @Test
    fun questionBlockGivesCoin() {
        val g = Game()
        g.debugLoad(0, 16, noEnemies = true)
        g.run(40) { jump = true }
        assertEquals(1, g.coins)
        assertEquals(Tile.USED, g.level.get(16, 9))
        assertEquals(200, g.score)
    }

    @Test
    fun mushroomMakesBig() {
        val g = Game()
        g.debugLoad(0, 21, noEnemies = true)
        g.run(30) { jump = true }
        assertEquals(1, g.items.size)
        g.run(400)
        // the mushroom slides off, bounces off the pipe and comes back to the player
        assertEquals(1, g.player.power)
    }

    @Test
    fun bigPlayerBreaksBricks() {
        val g = Game()
        g.debugLoad(0, 20, power = 1, noEnemies = true)
        g.run(40) { jump = true }
        assertEquals(Tile.EMPTY, g.level.get(20, 9))
    }

    @Test
    fun stompingWalkerScores() {
        val g = Game()
        g.debugLoad(0, 5, noEnemies = true)
        g.run(2)
        val w = Walker(5, 12, 0)
        w.vx = 0f
        g.enemies += w
        g.player.y = 100f
        g.run(60)
        assertEquals(100, g.score)
        assertTrue(w.inert)
        assertEquals(PMode.NORMAL, g.player.mode)
        assertEquals(Game.State.PLAYING, g.state)
    }

    @Test
    fun walkingIntoWalkerKillsSmallPlayer() {
        val g = Game()
        g.debugLoad(0, 5, noEnemies = true)
        g.enemies += Walker(9, 12, 0)
        g.run(120) { right = true }
        assertEquals(Game.State.DYING, g.state)
    }

    @Test
    fun pipeLeadsToBonusRoomAndBack() {
        val g = Game()
        g.debugLoad(0, 57, noEnemies = true)
        g.run(2)
        g.run(80) { down = true }
        assertTrue("camX ${g.camX}", g.camX >= 220 * 16)
        // walk from the floor next to the exit pipe into it
        g.debugLoad(0, 230, noEnemies = true)
        g.run(2)
        g.run(120) { right = true }
        g.run(120)
        assertTrue("x ${g.player.x}", g.player.x in 163 * 16f..170 * 16f)
        assertEquals(PMode.NORMAL, g.player.mode)
    }

    @Test
    fun flagpoleFinishesLevel() {
        val g = Game()
        g.debugLoad(0, 192, noEnemies = true)
        g.run(200) { right = true }
        assertTrue("state ${g.state}", g.state != Game.State.PLAYING)
        g.run(1500)
        assertEquals(1, g.levelIndex)
        assertTrue(g.score > 0)
    }

    @Test
    fun fallingIntoPitCostsLife() {
        val g = Game()
        g.debugLoad(0, 66, noEnemies = true)
        g.run(150) { right = true }
        assertEquals(Game.State.DYING, g.state)
        g.run(200)
        assertEquals(2, g.lives)
        assertEquals(Game.State.INTRO, g.state)
    }

    @Test
    fun axeEndsCastle() {
        val g = Game()
        g.debugLoad(3, 130, noEnemies = true)
        g.run(200) { right = true }
        assertTrue("state ${g.state}", g.state == Game.State.AXE || g.state == Game.State.RESCUE)
        g.run(900)
        assertEquals(Game.State.RESCUE, g.state)
    }

    @Test
    fun fireballsDefeatBoss() {
        val g = Game()
        g.debugLoad(3, 118, power = 2, noEnemies = true)
        val boss = Boss(127, 9, 121 * 16f, 131 * 16f)
        g.enemies += boss
        var frames = 0
        while (frames < 600 && !boss.knocked) {
            pad.run = frames % 20 < 2
            g.update(pad)
            frames++
        }
        assertTrue("boss not defeated", boss.knocked)
    }

    @Test
    fun warpZonePipeSkipsToCastle() {
        val g = Game()
        g.debugLoad(1, 176, noEnemies = true)
        g.run(2)
        g.run(80) { down = true }
        assertEquals(Game.State.INTRO, g.state)
        assertEquals(3, g.levelIndex)
    }

    @Test
    fun allLevelsBuildAndSongsStayInSync() {
        for (i in 0 until 4) {
            val g = Game()
            g.debugLoad(i)
            assertTrue(g.level.width > 100)
        }
        for (s in listOf(Song.OVERWORLD, Song.UNDERGROUND, Song.CASTLE, Song.STAR)) {
            val l = Synth.trackLengths(s)
            assertTrue("$s $l", l.distinct().size == 1)
        }
    }
}
