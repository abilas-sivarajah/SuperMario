package com.supanga.bros

import com.supanga.bros.core.Image
import com.supanga.bros.core.Sprites
import org.junit.Test
import java.io.File

/** Renders every sprite onto one sheet (build/screenshots/sprites.png) so the art can be reviewed. */
class SpriteSheetTest {
    @Test
    fun renderSpriteSheet() {
        val all = mutableListOf<Image>()
        Sprites.small.forEach { all += it }
        Sprites.big.forEach { all += it }
        all += Sprites.walker + Sprites.walkerFlat
        Sprites.turtle.forEach { all += it }
        all += Sprites.wing
        all += Sprites.biter
        all += listOf(Sprites.mushroom, Sprites.oneUp) + Sprites.flower + Sprites.star + Sprites.coinSpin
        all += Sprites.fireball
        Sprites.tileSets.values.forEach { all += listOf(it.ground, it.brick, it.hard, it.used, it.debris) + it.question }
        all += listOf(Sprites.pipeTopLeft, Sprites.pipeTopRight, Sprites.pipeLeft, Sprites.pipeRight)
        all += listOf(Sprites.hpipeMouthTop, Sprites.hpipeMouthBottom, Sprites.hpipeTop, Sprites.hpipeBottom)
        all += listOf(Sprites.treeLeft, Sprites.treeMid, Sprites.treeRight, Sprites.trunk, Sprites.bridge, Sprites.chain)
        all += listOf(Sprites.lavaTop, Sprites.lava) + Sprites.axe + listOf(Sprites.lift, Sprites.flag, Sprites.poleTop)
        all += listOf(Sprites.bossClosed, Sprites.bossOpen, Sprites.bossFire, Sprites.podoboo, Sprites.retainer)
        all += Sprites.clouds + Sprites.bushes + listOf(Sprites.hillBig, Sprites.hillSmall, Sprites.castle)

        val sheetW = 520
        var x = 4
        var y = 4
        var rowH = 0
        val placed = mutableListOf<Triple<Image, Int, Int>>()
        for (img in all) {
            if (x + img.w + 4 > sheetW) { x = 4; y += rowH + 4; rowH = 0 }
            placed += Triple(img, x, y)
            x += img.w + 4
            rowH = maxOf(rowH, img.h)
        }
        val sheetH = y + rowH + 4
        val px = IntArray(sheetW * sheetH) { 0xFF5C94FC.toInt() }
        for ((img, ox, oy) in placed) {
            for (iy in 0 until img.h) for (ix in 0 until img.w) {
                val c = img.px[iy * img.w + ix]
                if (c ushr 24 != 0) px[(oy + iy) * sheetW + ox + ix] = c
            }
        }
        Png.write(File("build/screenshots/sprites.png"), sheetW, sheetH, px, 3)
    }
}
