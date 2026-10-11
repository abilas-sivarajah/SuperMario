package com.supanga.bros

import com.supanga.bros.core.Game
import com.supanga.bros.core.Levels
import com.supanga.bros.core.SCREEN_H
import com.supanga.bros.core.SCREEN_W
import com.supanga.bros.core.Screen
import org.junit.Test
import java.io.File

/** Renders every level as a map (several screens per row) into build/screenshots/map_*.png. */
class LevelMapTest {
    @Test
    fun renderMaps() {
        val screen = Screen()
        for (i in 0 until Levels.COUNT) {
            val g = Game()
            g.debugLoad(i)
            val widthPx = g.level.width * 16
            val perRow = 4
            val screens = (widthPx + SCREEN_W - 1) / SCREEN_W
            val rows = (screens + perRow - 1) / perRow
            val outW = perRow * SCREEN_W
            val out = IntArray(outW * rows * SCREEN_H)
            for (k in 0 until screens) {
                g.debugCamera(k * SCREEN_W)
                g.render(screen)
                val ox = (k % perRow) * SCREEN_W
                val oy = (k / perRow) * SCREEN_H
                for (y in 0 until SCREEN_H) for (x in 0 until SCREEN_W) out[(oy + y) * outW + ox + x] = screen.pixels[y * SCREEN_W + x]
            }
            Png.write(File("build/screenshots/map_1-${i + 1}.png"), outW, rows * SCREEN_H, out, 1)
        }
    }
}
