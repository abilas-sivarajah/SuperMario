package com.supanga.bros

import com.supanga.bros.core.Buttons
import com.supanga.bros.core.Game
import com.supanga.bros.core.Screen
import org.junit.Test
import java.io.File

/** Drives the game headless and saves frames to build/screenshots for visual review. */
class ScreenshotTest {
    private val screen = Screen()
    private val pad = Buttons()

    private fun shot(g: Game, name: String) {
        g.render(screen)
        Png.write(File("build/screenshots/$name.png"), screen.w, screen.h, screen.pixels, 2)
    }

    private fun run(g: Game, frames: Int, setup: Buttons.() -> Unit = {}) {
        pad.setup()
        repeat(frames) { g.update(pad) }
        pad.left = false; pad.right = false; pad.jump = false; pad.run = false; pad.down = false; pad.start = false
    }

    @Test
    fun screenshots() {
        val g = Game()
        run(g, 5)
        shot(g, "01_title")
        run(g, 1) { jump = true }
        run(g, 2)
        shot(g, "02_intro")
        run(g, 170)
        shot(g, "03_level_start")
        run(g, 100) { right = true }
        shot(g, "04_walk")
        run(g, 30) { right = true; jump = true }
        shot(g, "05_jump")
        run(g, 200) { right = true; run = true }
        shot(g, "06_run")
    }

    @Test
    fun scenes() {
        // warp zone
        var g = Game()
        g.debugLoad(1, 170, noEnemies = true)
        run(g, 20)
        shot(g, "10_warpzone")
        // big hero next to blocks, then fire hero throwing
        g = Game()
        g.debugLoad(0, 19, power = 1, noEnemies = true)
        run(g, 3)
        shot(g, "11_big")
        g = Game()
        g.debugLoad(0, 30, power = 2)
        run(g, 30) { right = true }
        run(g, 3) { run = true }
        run(g, 6)
        shot(g, "12_fire")
        // boss fight
        g = Game()
        g.debugLoad(3, 120, power = 2)
        run(g, 2)
        run(g, 12) { right = true }
        run(g, 3) { run = true }
        run(g, 160)
        shot(g, "13_boss")
        // underground start
        g = Game()
        g.debugLoad(1, 30)
        run(g, 60)
        shot(g, "14_underground")
        // castle fire bars
        g = Game()
        g.debugLoad(3, 18)
        run(g, 40)
        shot(g, "15_castle")
        // flag pole
        g = Game()
        g.debugLoad(0, 186, noEnemies = true)
        run(g, 2)
        run(g, 40) { right = true; run = true }
        run(g, 40) { right = true; run = true; jump = true }
        run(g, 20)
        shot(g, "16_flag")
        run(g, 200)
        shot(g, "17_castle_walk")
        // rescue
        g = Game()
        g.debugLoad(3, 131, noEnemies = true)
        run(g, 3)
        run(g, 60) { right = true }
        run(g, 900)
        shot(g, "18_rescue")
        // tree tops
        g = Game()
        g.debugLoad(2, 30)
        run(g, 30)
        shot(g, "19_trees")
        // world 2-1 starts in front of the castle of world 1
        g = Game()
        g.debugLoad(4)
        run(g, 3)
        shot(g, "20_world2")
        // the vine grows out of the brick bridge, the hero climbs it into coin heaven
        g = Game()
        g.debugLoad(4, 51, power = 1, noEnemies = true)
        run(g, 2)
        run(g, 30) { jump = true }
        run(g, 60)
        g.player.y = 9 * 16f - g.player.h
        run(g, 2)
        run(g, 60) { up = true }
        shot(g, "21_vine")
        run(g, 260) { up = true }
        run(g, 60) { right = true }
        run(g, 12) { right = true; jump = true }
        shot(g, "22_coin_heaven")
        // springboard squeezed, then a timed jump up to the road in the sky
        g = Game()
        g.debugLoad(4, 126, noEnemies = true)
        run(g, 2)
        g.player.x = 132 * 16f + 2
        g.player.y = 120f
        run(g, 14)
        shot(g, "23_spring")
        run(g, 100) { jump = true; right = true; run = true }
        run(g, 30) { right = true }
        shot(g, "24_sky_road")
    }
}
