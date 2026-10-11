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
}
