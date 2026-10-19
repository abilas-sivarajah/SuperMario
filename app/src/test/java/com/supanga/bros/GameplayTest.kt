package com.supanga.bros

import com.supanga.bros.core.Boss
import com.supanga.bros.core.Buttons
import com.supanga.bros.core.Game
import com.supanga.bros.core.Levels
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
    fun castleLeadsToWorldTwo() {
        val g = Game()
        g.debugLoad(3, 130, noEnemies = true)
        g.run(200) { right = true }
        g.run(1000)
        assertEquals(Game.State.RESCUE, g.state)
        g.run(1) { jump = true }
        assertEquals(Game.State.INTRO, g.state)
        assertEquals("2-1", Levels.names[g.levelIndex])
    }

    /** Drops the hero onto the springboard of 2-1 and returns the highest point of the feet after the throw. */
    private fun springApex(boost: Boolean, steer: Boolean = false): Game {
        val g = Game()
        g.debugLoad(4, 128, noEnemies = true)
        g.run(2)
        g.player.x = 132 * 16f + 2
        g.player.y = 120f
        var f = 0
        while (g.player.spring == null && f++ < 60) g.update(pad)
        assertTrue("didn't land on the spring", g.player.spring != null)
        pad.jump = boost
        pad.right = steer
        pad.run = steer
        return g
    }

    @Test
    fun springboardBounces() {
        val g = springApex(boost = false)
        var top = g.player.bottom
        repeat(90) { g.update(pad); top = minOf(top, g.player.bottom) }
        // a plain bounce goes about five tiles up, more than a normal jump
        assertTrue("apex $top", top in 90f..112f)
        assertEquals(Game.State.PLAYING, g.state)
    }

    @Test
    fun timedSpringJumpReachesSkyRoad() {
        val g = springApex(boost = true, steer = true)
        var top = g.player.bottom
        repeat(110) { g.update(pad); top = minOf(top, g.player.bottom) }
        pad.jump = false; pad.right = false; pad.run = false
        assertTrue("apex $top", top < 20f)
        // landed on the clouds in the sky
        g.run(20)
        assertEquals(4 * 16f, g.player.bottom, 0.01f)
        assertTrue("x ${g.player.x / 16}", g.player.x / 16 in 135f..148f)
    }

    @Test
    fun springboardSidesAreSolid() {
        val g = Game()
        g.debugLoad(4, 128, noEnemies = true)
        g.run(2)
        g.run(120) { right = true }
        assertTrue("x ${g.player.x}", g.player.x + g.player.w <= 132 * 16f + 0.01f)
        assertTrue(g.player.spring == null)
    }

    @Test
    fun vineLeadsToCoinHeavenAndBack() {
        val g = Game()
        g.debugLoad(4, 51, noEnemies = true)
        g.run(2)
        g.run(30) { jump = true }
        assertEquals(Tile.USED, g.level.get(51, 9))
        assertEquals(1, g.vines.size)
        g.run(100)
        // stand on the brick bridge above the vine block and climb up
        g.player.y = 9 * 16f - g.player.h
        g.run(2)
        g.run(30) { up = true }
        assertEquals(PMode.CLIMB, g.player.mode)
        g.run(300) { up = true }
        assertTrue("x ${g.player.x / 16}", g.player.x > 222 * 16f)
        assertEquals(PMode.NORMAL, g.player.mode)
        assertEquals(11 * 16f, g.player.bottom, 0.01f)
        // walking off the end of the clouds drops the hero back into the level, alive
        var f = 0
        pad.right = true
        while (g.player.x > 200 * 16f && f++ < 800) g.update(pad)
        g.run(120)
        assertEquals(Game.State.PLAYING, g.state)
        assertEquals(3, g.lives)
        assertTrue("x ${g.player.x / 16}", g.player.x / 16 in 128f..132f)
        assertTrue(g.player.onGround)
    }

    @Test
    fun climbingDownLeavesTheVine() {
        val g = Game()
        g.debugLoad(4, 51, noEnemies = true)
        g.run(2)
        g.run(30) { jump = true }
        g.run(100)
        g.player.y = 9 * 16f - g.player.h
        g.run(2)
        g.run(40) { up = true }
        g.run(60) { down = true }
        assertEquals(PMode.NORMAL, g.player.mode)
        assertEquals(9 * 16f, g.player.bottom, 0.01f)
    }

    @Test
    fun allLevelsBuildAndSongsStayInSync() {
        for (i in 0 until Levels.COUNT) {
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
