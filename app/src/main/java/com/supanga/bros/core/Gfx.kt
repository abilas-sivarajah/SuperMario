package com.supanga.bros.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val SCREEN_W = 256
const val SCREEN_H = 240
const val TILE = 16

/** A small ARGB image. Fully transparent pixels (alpha == 0) are skipped when drawing. */
class Image(val w: Int, val h: Int, val px: IntArray) {

    fun flippedX(): Image {
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) out[y * w + x] = px[y * w + (w - 1 - x)]
        return Image(w, h, out)
    }

    fun flippedY(): Image {
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) out[y * w + x] = px[(h - 1 - y) * w + x]
        return Image(w, h, out)
    }

    /** Swap rows and columns (used to turn vertical pipe pieces into horizontal ones). */
    fun transposed(): Image {
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) out[x * h + y] = px[y * w + x]
        return Image(h, w, out)
    }

    /** Horizontal squash around the center, used for the spinning coin. */
    fun scaledX(f: Float): Image {
        val out = IntArray(w * h)
        val cx = w / 2f
        for (y in 0 until h) for (x in 0 until w) {
            val sx = ((x + 0.5f - cx) / f + cx).toInt()
            if (sx in 0 until w) out[y * w + x] = px[y * w + sx]
        }
        return Image(w, h, out)
    }

    fun sub(x0: Int, y0: Int, sw: Int, sh: Int): Image {
        val out = IntArray(sw * sh)
        for (y in 0 until sh) for (x in 0 until sw) out[y * sw + x] = px[(y0 + y) * w + x0 + x]
        return Image(sw, sh, out)
    }

    fun recolored(map: Map<Int, Int>): Image = Image(w, h, IntArray(px.size) { map[px[it]] ?: px[it] })

    companion object {
        /** Builds an image from ASCII art. '.' and ' ' are transparent, every other char is looked up in [pal]. */
        fun art(rows: List<String>, pal: Map<Char, Int>): Image {
            val h = rows.size
            val w = rows[0].length
            val px = IntArray(w * h)
            for (y in 0 until h) {
                val r = rows[y]
                require(r.length == w) { "Art row $y has length ${r.length}, expected $w: '$r'" }
                for (x in 0 until w) {
                    val c = r[x]
                    px[y * w + x] = if (c == '.' || c == ' ') 0 else (pal[c] ?: error("No color for '$c' in row $y: '$r'"))
                }
            }
            return Image(w, h, px)
        }

        fun blank(w: Int, h: Int) = Image(w, h, IntArray(w * h))
    }
}

/** Builds a bigger image out of smaller ones. */
class Canvas(w: Int, h: Int) {
    val img = Image.blank(w, h)
    fun draw(src: Image, x: Int, y: Int) {
        for (sy in 0 until src.h) for (sx in 0 until src.w) {
            val c = src.px[sy * src.w + sx]
            val dx = x + sx
            val dy = y + sy
            if (c ushr 24 != 0 && dx in 0 until img.w && dy in 0 until img.h) img.px[dy * img.w + dx] = c
        }
    }

    fun fill(x: Int, y: Int, w: Int, h: Int, color: Int) {
        for (yy in max(0, y) until min(img.h, y + h)) for (xx in max(0, x) until min(img.w, x + w)) img.px[yy * img.w + xx] = color
    }
}

/** The 256x240 software frame buffer everything is rendered into. */
class Screen(val w: Int = SCREEN_W, val h: Int = SCREEN_H) {
    val pixels = IntArray(w * h)

    fun clear(color: Int) = pixels.fill(color)

    fun fillRect(x: Int, y: Int, rw: Int, rh: Int, color: Int) {
        val x0 = max(0, x)
        val y0 = max(0, y)
        val x1 = min(w, x + rw)
        val y1 = min(h, y + rh)
        for (yy in y0 until y1) {
            val row = yy * w
            for (xx in x0 until x1) pixels[row + xx] = color
        }
    }

    fun draw(img: Image, x: Int, y: Int, flipX: Boolean = false, flipY: Boolean = false) {
        if (x >= w || y >= h || x + img.w <= 0 || y + img.h <= 0) return
        val sx0 = max(0, -x)
        val sy0 = max(0, -y)
        val sx1 = min(img.w, w - x)
        val sy1 = min(img.h, h - y)
        val src = img.px
        for (sy in sy0 until sy1) {
            val srcRow = (if (flipY) img.h - 1 - sy else sy) * img.w
            val dstRow = (y + sy) * w + x
            for (sx in sx0 until sx1) {
                val c = src[srcRow + if (flipX) img.w - 1 - sx else sx]
                if (c ushr 24 != 0) pixels[dstRow + sx] = c
            }
        }
    }

    /** Draws [img] with every opaque pixel replaced by [color] (used for flashes and shadows). */
    fun drawTinted(img: Image, x: Int, y: Int, color: Int, flipX: Boolean = false) {
        for (sy in 0 until img.h) {
            val dy = y + sy
            if (dy !in 0 until h) continue
            for (sx in 0 until img.w) {
                val dx = x + sx
                if (dx !in 0 until w) continue
                val c = img.px[sy * img.w + if (flipX) img.w - 1 - sx else sx]
                if (c ushr 24 != 0) pixels[dy * w + dx] = color
            }
        }
    }

    fun text(s: String, x: Int, y: Int, color: Int = Colors.WHITE, scale: Int = 1) =
        Font.draw(this, s, x, y, color, scale)

    fun textCentered(s: String, y: Int, color: Int = Colors.WHITE, scale: Int = 1) =
        text(s, (w - s.length * 8 * scale) / 2, y, color, scale)

    fun plot(x: Int, y: Int, color: Int) {
        if (x in 0 until w && y in 0 until h) pixels[y * w + x] = color
    }
}

object Colors {
    const val WHITE = 0xFFFCFCFC.toInt()
    const val BLACK = 0xFF000000.toInt()
    const val SKY = 0xFF5C94FC.toInt()
    const val NIGHT = 0xFF000000.toInt()
    const val GOLD = 0xFFFCC838.toInt()
    const val RED = 0xFFE83818.toInt()
    const val GRAY = 0xFFA8A8A8.toInt()

    fun lerp(a: Int, b: Int, t: Float): Int {
        val ar = (a shr 16) and 255; val ag = (a shr 8) and 255; val ab = a and 255
        val br = (b shr 16) and 255; val bg = (b shr 8) and 255; val bb = b and 255
        val r = (ar + (br - ar) * t).roundToInt()
        val g = (ag + (bg - ag) * t).roundToInt()
        val bl = (ab + (bb - ab) * t).roundToInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }
}

/**
 * Bitmap font: 5x7 glyphs that are drawn "smeared" one pixel to the right,
 * which gives the thick vertical strokes typical for 8-bit fonts.
 */
object Font {
    private val glyphs = HashMap<Char, IntArray>()

    private fun def(c: Char, vararg rows: String) {
        require(rows.size == 7 && rows.all { it.length == 5 }) { "bad glyph $c" }
        glyphs[c] = IntArray(7) { r -> rows[r].foldIndexed(0) { i, acc, ch -> if (ch == '#') acc or (1 shl (4 - i)) else acc } }
    }

    init {
        def('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#")
        def('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.")
        def('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.")
        def('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.")
        def('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####")
        def('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....")
        def('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####")
        def('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#")
        def('I', ".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###.")
        def('J', "..###", "...#.", "...#.", "...#.", "#..#.", "#..#.", ".##..")
        def('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#")
        def('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####")
        def('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#")
        def('N', "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#")
        def('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.")
        def('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....")
        def('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#")
        def('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#")
        def('S', ".###.", "#...#", "#....", ".###.", "....#", "#...#", ".###.")
        def('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..")
        def('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.")
        def('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..")
        def('W', "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#")
        def('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#")
        def('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..")
        def('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####")
        def('0', ".###.", "#..##", "#.#.#", "#.#.#", "#.#.#", "##..#", ".###.")
        def('1', "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###.")
        def('2', ".###.", "#...#", "....#", "..##.", ".#...", "#....", "#####")
        def('3', ".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###.")
        def('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.")
        def('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.")
        def('6', "..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###.")
        def('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...")
        def('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.")
        def('9', ".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##..")
        def('-', ".....", ".....", ".....", ".###.", ".....", ".....", ".....")
        def('!', "..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#..")
        def('.', ".....", ".....", ".....", ".....", ".....", ".....", "..#..")
        def(',', ".....", ".....", ".....", ".....", ".....", "..#..", ".#...")
        def('?', ".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#..")
        def(':', ".....", "..#..", ".....", ".....", ".....", "..#..", ".....")
        def('\'', "..#..", "..#..", ".....", ".....", ".....", ".....", ".....")
        def('/', "....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#....")
        def('*', ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", ".....") // "x" as in "x 3"
        def('(', "...#.", "..#..", ".#...", ".#...", ".#...", "..#..", "...#.")
        def(')', ".#...", "..#..", "...#.", "...#.", "...#.", "..#..", ".#...")
        def('>', ".#...", "..#..", "...#.", "....#", "...#.", "..#..", ".#...")
        def('<', "...#.", "..#..", ".#...", "#....", ".#...", "..#..", "...#.")
    }

    fun draw(screen: Screen, s: String, x: Int, y: Int, color: Int, scale: Int = 1) {
        var cx = x
        for (ch in s) {
            val g = glyphs[ch.uppercaseChar()]
            if (g != null) {
                for (r in 0 until 7) {
                    val bits = g[r]
                    if (bits == 0) continue
                    for (c in 0 until 5) {
                        if (bits and (1 shl (4 - c)) != 0) {
                            screen.fillRect(cx + c * scale, y + r * scale, 2 * scale, scale, color)
                        }
                    }
                }
            }
            cx += 8 * scale
        }
    }
}
