package com.supanga.bros.core

import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class Theme(val bg: Int) {
    OVERWORLD(Colors.SKY),
    UNDERGROUND(Colors.BLACK),
    CASTLE(Colors.BLACK),
}

/** Builds every [Image] the game uses from [Art] and a set of palettes. */
object Sprites {
    private fun c(v: Long) = v.toInt()

    // Player frame indices
    const val STAND = 0
    const val WALK1 = 1
    const val WALK2 = 2
    const val WALK3 = 3
    const val JUMP = 4
    const val SKID = 5
    const val SPECIAL = 6 // small: dead, big: duck
    const val CLIMB1 = 7
    const val CLIMB2 = 8

    private val heroBase = mapOf(
        'K' to c(0xFF101010), 'W' to c(0xFFFCFCFC), 'S' to c(0xFFFCBC8C), 'H' to c(0xFF6C3008),
        'Y' to c(0xFFFCD840), 'C' to c(0xFF1888E8), 'O' to c(0xFFE04818),
    )
    private val heroPalettes = listOf(
        heroBase,
        heroBase + mapOf('C' to c(0xFFF4F4F4), 'W' to c(0xFFE82818), 'O' to c(0xFFE82818)), // fire
        heroBase + mapOf('C' to c(0xFF00A800), 'O' to c(0xFF101010), 'W' to c(0xFFFCD840)), // star cycle
        heroBase + mapOf('C' to c(0xFFE82818), 'O' to c(0xFFFC9838), 'W' to c(0xFFFCFCFC)),
        heroBase + mapOf('C' to c(0xFF101010), 'O' to c(0xFF00A8A8), 'W' to c(0xFFE82818), 'S' to c(0xFFFCD8A8)),
    )
    const val PAL_NORMAL = 0
    const val PAL_FIRE = 1
    const val PAL_STAR = 2 // 2..4

    val small: List<List<Image>> = heroPalettes.map { p ->
        val climb = Image.art(Art.smallClimb, p)
        listOf(Art.smallStand, Art.smallWalk1, Art.smallWalk2, Art.smallWalk3, Art.smallJump, Art.smallSkid, Art.smallDead)
            .map { Image.art(it, p) } + listOf(climb, climb.flippedX())
    }
    val big: List<List<Image>> = heroPalettes.map { p ->
        val climb = Image.art(Art.bigClimb, p)
        listOf(Art.bigStand, Art.bigWalk1, Art.bigWalk2, Art.bigWalk3, Art.bigJump, Art.bigSkid, Art.bigDuck)
            .map { Image.art(it, p) } + listOf(climb, climb.flippedX())
    }

    // ---------------------------------------------------------------- enemies
    private val walkerPal = listOf(
        mapOf('A' to c(0xFFA83808), 'a' to c(0xFFE07038), 'T' to c(0xFFFCD8A8), 'F' to c(0xFF381800)),
        mapOf('A' to c(0xFF005888), 'a' to c(0xFF3CBCFC), 'T' to c(0xFFBCE8FC), 'F' to c(0xFF002848)),
        mapOf('A' to c(0xFF686868), 'a' to c(0xFFB0B0B0), 'T' to c(0xFFE0E0E0), 'F' to c(0xFF202020)),
    ).map { it + mapOf('K' to c(0xFF101010), 'W' to Colors.WHITE) }

    val walker = walkerPal.map { Image.art(Art.walker, it) }
    val walkerFlat = walkerPal.map { Image.art(Art.walkerFlat, it) }

    private val turtleGreen = mapOf(
        'G' to c(0xFF00A800), 'L' to c(0xFF80D010), 'S' to c(0xFFFCD890), 'Y' to c(0xFFFCFCB0),
        'W' to Colors.WHITE, 'K' to c(0xFF101010),
    )
    private val turtleRed = turtleGreen + mapOf('G' to c(0xFFC82800), 'L' to c(0xFFFC9838))

    /** [red][frame]: frames 0,1 walk, 2 shell, 3 waking shell */
    val turtle: List<List<Image>> = listOf(turtleGreen, turtleRed).map { p ->
        listOf(Art.turtle1, Art.turtle2, Art.shell, Art.shellWaking).map { Image.art(it, p) }
    }
    val wing = Image.art(Art.wing, mapOf('K' to c(0xFF101010), 'W' to Colors.WHITE))

    private val biterPal = mapOf(
        'R' to c(0xFFE83818), 'W' to Colors.WHITE, 'T' to Colors.WHITE,
        'G' to c(0xFF00A800), 'L' to c(0xFF80D010), 'K' to c(0xFF101010),
    )
    val biter = listOf(Image.art(Art.biterOpen, biterPal), Image.art(Art.biterClosed, biterPal))

    // ---------------------------------------------------------------- items
    private val shroomBase = mapOf('K' to c(0xFF101010), 'W' to Colors.WHITE, 'T' to c(0xFFFCE0B0))
    val mushroom = Image.art(Art.mushroom, shroomBase + ('M' to c(0xFFF07800)))
    val oneUp = Image.art(Art.mushroom, shroomBase + ('M' to c(0xFF00A800)))

    val flower = listOf(
        c(0xFFE83818) to c(0xFFFC9838),
        c(0xFFFC9838) to c(0xFFFCFCFC),
        c(0xFFFCD840) to c(0xFFE83818),
        c(0xFF00A8A8) to c(0xFFFCD840),
    ).map { (p, y) ->
        Image.art(Art.flower, mapOf('P' to p, 'Y' to y, 'W' to Colors.WHITE, 'G' to c(0xFF00A800), 'L' to c(0xFF80D010), 'K' to c(0xFF101010)))
    }

    val star = listOf(c(0xFFFCD840), c(0xFFFC9838), c(0xFFFCFCFC), c(0xFF80D010))
        .map { Image.art(Art.star, mapOf('S' to it, 'K' to c(0xFF101010))) }

    private val coinPal = mapOf('Y' to c(0xFFFCC838), 'W' to c(0xFFFCFCE0), 'D' to c(0xFFC87818), 'K' to c(0xFF101010))
    private val coinBase = Image.art(Art.coin, coinPal)
    /** Spinning coin frames. */
    val coinSpin = listOf(1f, 0.55f, 0.2f, 0.55f).map { if (it == 1f) coinBase else coinBase.scaledX(it) }
    /** Coin tiles that sit in the level shimmer instead of spinning. */
    val coinShimmer = listOf(c(0xFFFCC838), c(0xFFE8A020), c(0xFFC87818)).map {
        Image.art(Art.coin, coinPal + ('Y' to it))
    }
    val hudCoin = listOf(c(0xFFFCC838), c(0xFFE8A020), c(0xFFC87818)).map {
        Image.art(Art.hudCoin, coinPal + ('Y' to it))
    }

    val fireball: Image = Image.art(Art.fireball, mapOf('R' to c(0xFFE83818), 'Y' to c(0xFFFC9838), 'W' to c(0xFFFCFC80)))

    // ---------------------------------------------------------------- tiles
    private class ThemePal(val main: Int, val light: Int, val dark: Int, val used: Int)

    private val themePal = mapOf(
        Theme.OVERWORLD to ThemePal(c(0xFFC8580C), c(0xFFFCA048), c(0xFF702800), c(0xFF9C4810)),
        Theme.UNDERGROUND to ThemePal(c(0xFF0070C8), c(0xFF40B8FC), c(0xFF003070), c(0xFF0058A0)),
        Theme.CASTLE to ThemePal(c(0xFF787878), c(0xFFC0C0C0), c(0xFF383838), c(0xFF606060)),
    )

    class TileSet(
        val ground: Image, val brick: Image, val hard: Image, val used: Image,
        val question: List<Image>, val debris: Image,
    )

    val tileSets: Map<Theme, TileSet> = Theme.entries.associateWith { t ->
        val p = themePal.getValue(t)
        val shade = mapOf('A' to p.main, 'L' to p.light, 'D' to p.dark, 'K' to Colors.BLACK)
        TileSet(
            ground = Image.art(Art.ground, shade),
            brick = Image.art(Art.brick, shade),
            hard = Image.art(Art.hard, shade),
            used = Image.art(Art.used, mapOf('A' to p.used, 'K' to Colors.BLACK)),
            question = listOf(c(0xFFFCB818), c(0xFFE89810), c(0xFFC87808)).map {
                Image.art(Art.question, mapOf('Q' to it, 'D' to c(0xFF8C3800), 'W' to c(0xFFFCF0C8), 'K' to Colors.BLACK))
            },
            debris = Image.art(Art.debris, shade),
        )
    }

    private val pipePal = mapOf('G' to c(0xFF00A800), 'L' to c(0xFF98E848), 'D' to c(0xFF005800), 'K' to Colors.BLACK)

    private fun pipeImage(row: String, outlineTopBottom: Boolean): Image {
        val rows = List(16) { y ->
            if (outlineTopBottom && (y == 0 || y == 15)) row.map { if (it == '.') '.' else 'K' }.joinToString("") else row
        }
        return Image.art(rows, pipePal)
    }

    private val lip = pipeImage(Art.pipeLipRow, true)
    private val body = pipeImage(Art.pipeBodyRow, false)
    val pipeTopLeft = lip.sub(0, 0, 16, 16)
    val pipeTopRight = lip.sub(16, 0, 16, 16)
    val pipeLeft = body.sub(0, 0, 16, 16)
    val pipeRight = body.sub(16, 0, 16, 16)

    // Horizontal pipes are the vertical ones transposed: the mouth faces left.
    val hpipeMouthTop = pipeTopLeft.transposed()
    val hpipeMouthBottom = pipeTopRight.transposed()
    val hpipeTop = pipeLeft.transposed()
    val hpipeBottom = pipeRight.transposed()

    private val treePal = mapOf('G' to c(0xFF00A800), 'L' to c(0xFF80D010), 'K' to Colors.BLACK)
    val treeLeft = Image.art(Art.treeLeft, treePal)
    val treeMid = Image.art(Art.treeMid, treePal)
    val treeRight = treeLeft.flippedX()
    val trunk = Image.art(Art.trunk, mapOf('A' to c(0xFFB06018), 'D' to c(0xFF603008), 'K' to Colors.BLACK))

    val bridge = Image.art(Art.bridge, mapOf('L' to c(0xFFFCD8A8), 'A' to c(0xFFC8580C), 'D' to c(0xFF702800), 'K' to Colors.BLACK))
    val chain = Image.art(Art.chain, mapOf('G' to c(0xFFB0B0B0), 'K' to Colors.BLACK))
    private val lavaPal = mapOf('R' to c(0xFFE83800), 'O' to c(0xFFFCA048))
    val lavaTop = Image.art(Art.lavaTop, lavaPal)
    val lava = Image.art(Art.lava, lavaPal)
    val axe = listOf(c(0xFFFCFCFC), c(0xFFFCD840)).map {
        Image.art(Art.axe, mapOf('W' to it, 'L' to c(0xFFA8A8A8), 'H' to c(0xFF8C4000), 'K' to Colors.BLACK))
    }
    val lift = Image.art(Art.liftSegment, mapOf('A' to c(0xFFE89060), 'L' to c(0xFFFCD0B0), 'K' to c(0xFF702800)))
    val flag = Image.art(Art.flag, mapOf('W' to Colors.WHITE, 'R' to c(0xFFE83818), 'K' to c(0xFF00A800)))
    val poleTop = Image.art(Art.poleTop, mapOf('G' to c(0xFF00A800), 'L' to c(0xFF80D010), 'K' to Colors.BLACK))

    // ---------------------------------------------------------------- world 2
    private val vinePal = mapOf('G' to c(0xFF00A800), 'L' to c(0xFF80D010), 'K' to Colors.BLACK)
    val vine = Image.art(Art.vine, vinePal)
    val vineTop = Image.art(Art.vineTop, vinePal)
    val cloudBlock = Image.art(Art.cloudBlock, mapOf('W' to Colors.WHITE, 'B' to c(0xFFA8D8FC), 'K' to c(0xFF101010)))

    /** Springboard squeezed by 0..16 pixels: plates with a coil of rings between them, 16x32. */
    val spring: List<Image> = run {
        val pal = mapOf(
            'K' to c(0xFF101010), 'R' to c(0xFFE83818), 'Y' to c(0xFFFC9838), 'D' to c(0xFF881400),
            'W' to Colors.WHITE, 'G' to c(0xFFA8A8A8),
        )
        val plate = Image.art(Art.springPlate, pal)
        val ring = Image.art(listOf("..KWWWWWWWWWWK..", "...KGGGGGGGGK..."), pal)
        val posts = Image.art(listOf(".....KG..GK....."), pal)
        (0..16).map { press ->
            val cv = Canvas(16, 32)
            val coilTop = press + 6
            val coilH = 20 - press
            for (y in coilTop until 26) cv.draw(posts, 0, y)
            for (i in 0 until 5) cv.draw(ring, 0, coilTop + ((i + 0.5f) * coilH / 5).roundToInt() - 1)
            cv.draw(plate, 0, press)
            cv.draw(plate, 0, 26)
            cv.img
        }
    }

    // ---------------------------------------------------------------- castle level
    private val bossPal = mapOf(
        'K' to c(0xFF101010), 'G' to c(0xFF30A040), 'Y' to c(0xFFF8D878), 'O' to c(0xFFE86818),
        'W' to Colors.WHITE, 'R' to c(0xFFE81818), 'r' to c(0xFF801010),
    )
    val bossClosed = Image.art(Art.boss, bossPal)
    val bossOpen = Image.art(Art.boss.take(12) + Art.bossMouthOpen + Art.boss.drop(17), bossPal)
    val bossFire = Image.art(Art.bossFire, mapOf('R' to c(0xFFE83818), 'O' to c(0xFFFC9838), 'Y' to c(0xFFFCD840), 'W' to c(0xFFFCFCE0)))
    val podoboo = Image.art(Art.podoboo, mapOf('R' to c(0xFFE83818), 'O' to c(0xFFFC9838), 'Y' to c(0xFFFCD840), 'W' to c(0xFFFCFCE0)))
    val retainer = Image.art(Art.retainer, mapOf(
        'K' to c(0xFF101010), 'P' to c(0xFF8838C8), 'W' to Colors.WHITE, 'S' to c(0xFFFCBC8C),
        'B' to c(0xFF1888E8), 'Y' to c(0xFFFCD840), 'H' to c(0xFF6C3008),
    ))

    // ---------------------------------------------------------------- scenery (procedural)

    private fun blob(w: Int, h: Int, fill: Int, outline: Int, shade: Int, inside: (Int, Int) -> Boolean,
                     isShade: (Int, Int) -> Boolean = { _, _ -> false }): Image {
        val m = Array(h) { y -> BooleanArray(w) { x -> inside(x, y) } }
        val px = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            if (!m[y][x]) continue
            val edge = (x == 0 || !m[y][x - 1]) || (x == w - 1 || !m[y][x + 1]) || (y == 0 || !m[y - 1][x]) ||
                (y < h - 1 && !m[y + 1][x])
            px[y * w + x] = when {
                edge -> outline
                isShade(x, y) -> shade
                else -> fill
            }
        }
        return Image(w, h, px)
    }

    private fun inCircle(x: Int, y: Int, cx: Float, cy: Float, r: Float): Boolean {
        val dx = x + 0.5f - cx
        val dy = y + 0.5f - cy
        return dx * dx + dy * dy <= r * r
    }

    private fun puffShape(w: Int, h: Int): (Int, Int) -> Boolean {
        val bumps = (w - 16) / 16 + 1
        return { x, y ->
            var inside = x in 6 until w - 6 && y in 12 until h - 1
            for (i in 0..bumps) {
                val cx = 9f + i * (w - 18f) / bumps
                val edge = i == 0 || i == bumps
                val cy = if (edge) h - 9f else 10f
                val r = if (edge) 8.5f else 10f
                if (inCircle(x, y, cx, cy, r)) inside = true
            }
            inside
        }
    }

    /** Clouds 1..3 bumps wide. */
    val clouds = (1..3).map { n ->
        val w = 32 + 16 * n
        blob(w, 24, Colors.WHITE, c(0xFF101010), c(0xFFA8D8FC), puffShape(w, 24), { x, y -> y >= 17 && (x + y) % 3 != 0 })
    }
    val bushes = (1..3).map { n ->
        val w = 32 + 16 * n
        blob(w, 24, c(0xFF80D010), c(0xFF005800), c(0xFF00A800), puffShape(w, 24), { _, y -> y >= 18 })
    }

    private fun hill(w: Int, h: Int): Image {
        val cx = w / 2f
        val spots = listOf(cx - 12f to h * 0.55f, cx + 8f to h * 0.4f, cx + 14f to h * 0.75f, cx - 4f to h * 0.85f)
        return blob(w, h, c(0xFF00A800), c(0xFF101010), c(0xFF005800), { x, y ->
            val t = (x + 0.5f - cx) / cx
            val top = h * (1f - sqrt((1f - t * t).coerceAtLeast(0f)))
            y >= top
        }, { x, y ->
            spots.any { (sx, sy) -> val dx = (x - sx) / 3f; val dy = (y - sy) / 5f; dx * dx + dy * dy <= 1f }
        })
    }

    val hillBig = hill(80, 36)
    val hillSmall = hill(48, 20)

    /** Small 5x5 tile castle (80x80). Door is the middle column, bottom two tiles. */
    val castle: Image = run {
        val ts = tileSets.getValue(Theme.OVERWORLD)
        val cv = Canvas(80, 80)
        fun merlons(tx: Int, ty: Int) {
            cv.draw(ts.brick, tx * 16, ty * 16)
            cv.fill(tx * 16 + 5, ty * 16, 6, 8, 0)
        }
        for (tx in 1..3) merlons(tx, 0)
        for (tx in 1..3) cv.draw(ts.brick, tx * 16, 16)
        cv.fill(16 + 4, 16 + 2, 8, 12, Colors.BLACK)
        cv.fill(48 + 4, 16 + 2, 8, 12, Colors.BLACK)
        for (tx in 0..4) merlons(tx, 2)
        for (ty in 3..4) for (tx in 0..4) cv.draw(ts.brick, tx * 16, ty * 16)
        // door with arch
        cv.fill(32 + 2, 56, 12, 24, Colors.BLACK)
        cv.fill(32 + 4, 52, 8, 4, Colors.BLACK)
        cv.img
    }
}
