package com.supanga.bros.core

/** Small DSL used to describe the levels. Ground surface is row 13, the player stands in row 12. */
class LevelBuilder(width: Int) {
    val lv = Level(width)

    fun fill(x0: Int, x1: Int, y0: Int, y1: Int, t: Int) {
        for (x in x0..x1) for (y in y0..y1) lv.set(x, y, t)
    }

    fun ground(x0: Int, x1: Int, top: Int = 13) = fill(x0, x1, top, 14, Tile.GROUND)

    /**
     * Places a horizontal run of blocks starting at ([x],[y]):
     * B brick, ? coin block, M power-up block, U used, H hard, o coin, h hidden 1-UP,
     * i hidden coin, c multi-coin brick, S star brick, L 1-UP brick, V vine brick, C cloud, G ground, ' ' skip.
     */
    fun row(x: Int, y: Int, s: String) {
        s.forEachIndexed { i, ch ->
            val tx = x + i
            when (ch) {
                'B' -> lv.set(tx, y, Tile.BRICK)
                '?' -> block(tx, y, Tile.QUESTION, Content.COIN)
                'M' -> block(tx, y, Tile.QUESTION, Content.POWER)
                'U' -> lv.set(tx, y, Tile.USED)
                'H' -> lv.set(tx, y, Tile.HARD)
                'G' -> lv.set(tx, y, Tile.GROUND)
                'o' -> lv.set(tx, y, Tile.COIN)
                'h' -> block(tx, y, Tile.HIDDEN, Content.ONE_UP)
                'i' -> block(tx, y, Tile.HIDDEN, Content.COIN)
                'c' -> { block(tx, y, Tile.BRICK, Content.MULTI_COIN); lv.multiCoinLeft[lv.idx(tx, y)] = 10 }
                'S' -> block(tx, y, Tile.BRICK, Content.STAR)
                'L' -> block(tx, y, Tile.BRICK, Content.ONE_UP)
                'P' -> block(tx, y, Tile.BRICK, Content.POWER)
                'V' -> block(tx, y, Tile.BRICK, Content.VINE)
                'C' -> lv.set(tx, y, Tile.CLOUD)
                ' ' -> Unit
                else -> error("unknown block '$ch'")
            }
        }
    }

    private fun block(x: Int, y: Int, t: Int, c: Content) {
        lv.set(x, y, t)
        lv.contents[lv.idx(x, y)] = c
    }

    fun column(x: Int, y0: Int, y1: Int, t: Int) = fill(x, x, y0, y1, t)

    /** Vertical pipe [h] tiles high standing on row [base]. Returns the row of the pipe top. */
    fun pipe(x: Int, h: Int, base: Int = 13, biter: Boolean = false): Int {
        val top = base - h
        lv.set(x, top, Tile.PIPE_TL)
        lv.set(x + 1, top, Tile.PIPE_TR)
        for (y in top + 1 until base) {
            lv.set(x, y, Tile.PIPE_L)
            lv.set(x + 1, y, Tile.PIPE_R)
        }
        if (biter) lv.spawns += Spawn(EnemyKind.BITER, x, top)
        return top
    }

    fun stairsUp(x: Int, n: Int, base: Int = 13, maxH: Int = n) {
        for (i in 0 until n) column(x + i, base - minOf(i + 1, maxH), base - 1, Tile.HARD)
    }

    fun stairsDown(x: Int, n: Int, base: Int = 13) {
        for (i in 0 until n) column(x + i, base - (n - i), base - 1, Tile.HARD)
    }

    fun tree(x: Int, w: Int, top: Int) {
        lv.set(x, top, Tile.TREE_L)
        for (i in 1 until w - 1) lv.set(x + i, top, Tile.TREE_M)
        lv.set(x + w - 1, top, Tile.TREE_R)
        for (i in 1 until w - 1) for (y in top + 1..14) if (lv.get(x + i, y) == Tile.EMPTY) lv.set(x + i, y, Tile.TRUNK)
    }

    fun lava(x0: Int, x1: Int, top: Int = 12) {
        fill(x0, x1, top, top, Tile.LAVA_TOP)
        fill(x0, x1, top + 1, 14, Tile.LAVA)
    }

    fun enemy(kind: EnemyKind, x: Int, y: Int = 12, param: Int = 0) {
        lv.spawns += Spawn(kind, x, y, param)
    }

    fun walkers(y: Int = 12, vararg xs: Int) = xs.forEach { enemy(EnemyKind.WALKER, it, y) }

    fun flag(x: Int) {
        lv.flagX = x
        lv.set(x, 12, Tile.HARD)
    }

    fun castle(x: Int) {
        lv.castleX = x
        lv.decorations += Decoration(Deco.CASTLE, x * 16, 13 * 16 - 80)
    }

    /** The castle of the previous world, behind the start position. */
    fun startCastle(x: Int = 0) {
        lv.decorations += Decoration(Deco.CASTLE, x * 16, 13 * 16 - 80)
    }

    /** Springboard standing on row [base] (it fills the two rows above). */
    fun spring(x: Int, base: Int = 13) {
        lv.springs += x to base - 2
    }

    fun zone(x0: Int, x1: Int, theme: Theme, fallTo: Int = -1) {
        lv.zones += Zone(x0, x1, theme, fallTo = fallTo)
    }

    fun warp(kind: WarpKind, tx: Int, ty: Int, destX: Int, destY: Int, exit: ExitKind, toLevel: Int = -1) {
        lv.warps += Warp(kind, tx, ty, destX, destY, exit, toLevel)
    }

    /** Horizontal pipe whose mouth (facing left) is at column [x], rows [y] and [y]+1, going up to row 0 at [x]+2. */
    fun sidePipe(x: Int, y: Int, topRow: Int = 0) {
        lv.set(x, y, Tile.HPIPE_MOUTH_T)
        lv.set(x, y + 1, Tile.HPIPE_MOUTH_B)
        lv.set(x + 1, y, Tile.HPIPE_T)
        lv.set(x + 1, y + 1, Tile.HPIPE_B)
        for (yy in topRow..y + 1) {
            lv.set(x + 2, yy, Tile.PIPE_L)
            lv.set(x + 3, yy, Tile.PIPE_R)
        }
    }

    /** Hills, bushes and clouds repeating every 48 tiles, like a classic overworld. */
    fun scenery(x0: Int, x1: Int, hills: Boolean = true) {
        var base = x0 - (x0 % 48)
        while (base < x1) {
            fun at(dx: Int, kind: Deco, y: Int) {
                val tx = base + dx
                if (tx in x0 until x1) lv.decorations += Decoration(kind, tx * 16, y)
            }
            if (hills) {
                at(0, Deco.HILL_BIG, 13 * 16 - 36)
                at(16, Deco.HILL_SMALL, 13 * 16 - 20)
                at(11, Deco.BUSH3, 13 * 16 - 24)
                at(23, Deco.BUSH1, 13 * 16 - 24)
                at(41, Deco.BUSH2, 13 * 16 - 24)
            }
            at(8, Deco.CLOUD1, 3 * 16)
            at(19, Deco.CLOUD3, 2 * 16)
            at(27, Deco.CLOUD2, 3 * 16 + 8)
            at(36, Deco.CLOUD1, 2 * 16)
            base += 48
        }
    }

    fun build(): Level {
        // Scenery must not be drawn through solid tiles that are part of the foreground; nothing to do here,
        // but zones are required.
        if (lv.zones.isEmpty()) lv.zones += Zone(0, lv.width, Theme.OVERWORLD)
        return lv
    }
}

object Levels {
    /** Level names in the order they are played. */
    val names = listOf("1-1", "1-2", "1-3", "1-4", "2-1")
    val COUNT = names.size

    fun create(index: Int): Level = when (index) {
        0 -> level11()
        1 -> level12()
        2 -> level13()
        3 -> level14()
        else -> level21()
    }

    /** World 1-1: classic overworld with a hidden bonus room. */
    private fun level11(): Level = LevelBuilder(240).run {
        lv.name = "1-1"
        lv.time = 400
        lv.music = Song.OVERWORLD
        lv.checkpointX = 82
        zone(0, 212, Theme.OVERWORLD)
        zone(220, 236, Theme.UNDERGROUND)
        scenery(0, 212)

        ground(0, 68)
        row(16, 9, "?")
        row(20, 9, "BMB?B")
        row(22, 5, "?")
        walkers(12, 22)
        pipe(28, 2)
        pipe(38, 3)
        walkers(12, 41)
        pipe(46, 4)
        walkers(12, 51, 53)
        val bonusPipe = pipe(57, 4)
        warp(WarpKind.PIPE_DOWN, 57, bonusPipe, 222, 1, ExitKind.DROP)
        row(64, 8, "h")

        ground(71, 85)
        row(77, 9, "BMB")
        row(80, 5, "BBBBBBBB")
        walkers(4, 81, 83)

        ground(89, 152)
        row(91, 5, "BBB?")
        row(94, 9, "c")
        walkers(12, 97, 99)
        row(100, 9, "BS")
        row(106, 9, "?")
        row(109, 9, "?")
        row(109, 5, "M")
        row(112, 9, "?")
        enemy(EnemyKind.TURTLE, 107)
        row(118, 9, "B")
        row(121, 5, "BBB")
        walkers(12, 123, 125)
        row(128, 5, "B??B")
        row(129, 9, "BB")
        walkers(12, 130, 132)
        stairsUp(134, 4)
        stairsDown(140, 4)
        stairsUp(148, 4)
        column(152, 9, 12, Tile.HARD)

        ground(155, 211)
        stairsDown(155, 4)
        val exitPipe = pipe(163, 2)
        row(168, 9, "BB?B")
        walkers(12, 174, 176)
        pipe(179, 2)
        stairsUp(181, 8)
        column(189, 5, 12, Tile.HARD)
        flag(198)
        castle(202)

        // Bonus room
        ground(220, 235)
        column(220, 0, 12, Tile.BRICK)
        row(223, 0, "BBBBBBB")
        row(223, 10, "BBBBBBB")
        row(223, 11, "BBBBBBB")
        row(223, 12, "BBBBBBB")
        row(223, 8, "ooooooo")
        row(224, 6, "ooooo")
        row(223, 4, "ooooooo")
        sidePipe(232, 11)
        warp(WarpKind.PIPE_RIGHT, 232, 11, 163, exitPipe, ExitKind.PIPE_UP)
        build()
    }

    /** World 1-2: underground with lifts, ending in a pipe to the outside. A warp zone hides above the ceiling. */
    private fun level12(): Level = LevelBuilder(224).run {
        lv.name = "1-2"
        lv.time = 400
        lv.music = Song.UNDERGROUND
        lv.checkpointX = 98
        lv.startX = 3
        lv.startY = 12
        zone(0, 182, Theme.UNDERGROUND)
        zone(184, 224, Theme.OVERWORLD)

        ground(0, 79)
        column(0, 2, 12, Tile.BRICK)
        row(6, 2, "B".repeat(121))
        row(10, 9, "M????")
        walkers(12, 16, 18)
        column(19, 12, 12, Tile.HARD)
        column(21, 11, 12, Tile.HARD)
        column(23, 10, 12, Tile.HARD)
        column(25, 9, 12, Tile.HARD)
        column(27, 9, 12, Tile.HARD)
        column(29, 10, 12, Tile.HARD)
        enemy(EnemyKind.WALKER, 31)
        row(34, 5, "BBBBBB")
        column(34, 6, 8, Tile.BRICK)
        column(39, 6, 8, Tile.BRICK)
        row(35, 7, "oooo")
        row(34, 9, "BBBcBB")
        enemy(EnemyKind.TURTLE, 41)
        row(44, 8, "BBBBB")
        row(44, 4, "BBBBB")
        row(45, 6, "ooo")
        walkers(12, 48, 50)
        row(53, 9, "BBSBB")
        row(58, 6, "BBBB")
        row(58, 5, "BBBB")
        row(58, 7, "oooo")
        row(63, 9, "BBMBB")
        enemy(EnemyKind.TURTLE, 66)
        enemy(EnemyKind.TURTLE, 68)
        row(70, 5, "BBBBBBBBB")
        row(71, 4, "ooooooo")
        walkers(12, 74, 76)

        ground(82, 95)
        pipe(84, 3, biter = true)
        walkers(12, 88)
        pipe(91, 4, biter = true)

        ground(98, 127)
        pipe(100, 2)
        row(104, 9, "BBBLBB")
        row(104, 5, "oooooo")
        pipe(111, 3, biter = true)
        enemy(EnemyKind.RED_TURTLE, 116)
        enemy(EnemyKind.TURTLE, 119)
        row(116, 9, "BBMBBB")
        stairsUp(123, 4)
        column(127, 9, 12, Tile.HARD)

        // Lift section: no ceiling above the pit
        lv.lifts += LiftSpawn(LiftKind.LOOP_UP, 129f * 16, 6f * 16, 3)
        lv.lifts += LiftSpawn(LiftKind.LOOP_UP, 129f * 16, 14f * 16, 3)
        ground(133, 138)
        lv.lifts += LiftSpawn(LiftKind.LOOP_DOWN, 140f * 16, 2f * 16, 3)
        lv.lifts += LiftSpawn(LiftKind.LOOP_DOWN, 140f * 16, 10f * 16, 3)

        ground(144, 181)
        row(144, 2, "B".repeat(24))
        column(163, 3, 12, Tile.BRICK)
        row(146, 9, "BBBBBB")
        row(146, 8, "oooooo")
        lv.camLimitX = 164 * 16 - SCREEN_W

        // Warp zone, only reachable over the ceiling
        column(181, 0, 12, Tile.BRICK)
        val w3 = pipe(170, 3)
        val w4 = pipe(176, 3)
        warp(WarpKind.PIPE_DOWN, 170, w3, 0, 0, ExitKind.DROP, toLevel = 2)
        warp(WarpKind.PIPE_DOWN, 176, w4, 0, 0, ExitKind.DROP, toLevel = 3)
        lv.warpZoneX = 167
        lv.warpLabels += 170 to "3"
        lv.warpLabels += 176 to "4"

        // Outside
        ground(184, 223)
        val exitTop = pipe(188, 2)
        sidePipe(157, 11, topRow = 3)
        warp(WarpKind.PIPE_RIGHT, 157, 11, 188, exitTop, ExitKind.PIPE_UP)
        stairsUp(192, 8)
        column(200, 5, 12, Tile.HARD)
        flag(209)
        castle(213)
        scenery(184, 224)
        build()
    }

    /** World 1-3: tree tops and moving platforms high above the ground. */
    private fun level13(): Level = LevelBuilder(176).run {
        lv.name = "1-3"
        lv.time = 300
        lv.music = Song.OVERWORLD
        lv.checkpointX = 72
        zone(0, 176, Theme.OVERWORLD)
        scenery(0, 176, hills = false)

        ground(0, 15)
        tree(18, 4, 11)
        tree(24, 6, 8)
        row(25, 7, "oooo")
        tree(30, 3, 5)
        enemy(EnemyKind.WALKER, 27, 7)
        tree(35, 8, 9)
        enemy(EnemyKind.RED_TURTLE, 39, 8)
        row(37, 5, "M")
        tree(46, 5, 6)
        row(47, 3, "ooo")
        lv.lifts += LiftSpawn(LiftKind.SWING_Y, 53f * 16, 8f * 16, 3, 64f)
        tree(58, 7, 10)
        enemy(EnemyKind.FLY_TURTLE, 63, 7)
        tree(66, 4, 7)
        row(66, 4, "oooo")
        tree(71, 9, 11)
        walkers(10, 75, 77)
        lv.lifts += LiftSpawn(LiftKind.SWING_X, 82f * 16, 9f * 16, 3, 48f)
        tree(88, 5, 8)
        enemy(EnemyKind.RED_TURTLE, 90, 7)
        tree(95, 3, 5)
        row(95, 2, "ooo")
        tree(100, 6, 9)
        lv.lifts += LiftSpawn(LiftKind.FALL, 108f * 16, 8f * 16, 3)
        lv.lifts += LiftSpawn(LiftKind.FALL, 113f * 16, 7f * 16, 3)
        tree(118, 4, 7)
        enemy(EnemyKind.FLY_TURTLE, 121, 5)
        tree(124, 8, 10)
        row(126, 6, "ooo")
        enemy(EnemyKind.RED_TURTLE, 128, 9)
        lv.lifts += LiftSpawn(LiftKind.SWING_X, 134f * 16, 8f * 16, 3, 40f)
        tree(140, 4, 9)

        ground(146, 175)
        stairsUp(148, 4)
        column(152, 9, 12, Tile.HARD)
        flag(160)
        castle(164)
        build()
    }

    /** World 1-4: castle with fire bars, lava and the boss on the bridge. */
    private fun level14(): Level = LevelBuilder(160).run {
        lv.name = "1-4"
        lv.time = 300
        lv.music = Song.CASTLE
        lv.castleLevel = true
        lv.startX = 2
        lv.startY = 7
        zone(0, 160, Theme.CASTLE)
        val stone = Tile.GROUND

        // ceiling
        fill(0, 159, 0, 2, stone)
        // A: entry platform
        fill(0, 12, 8, 14, stone)
        lava(13, 15)
        enemy(EnemyKind.PODOBOO, 14, 12)
        // B
        fill(16, 30, 11, 14, stone)
        fill(16, 30, 3, 3, stone)
        row(22, 7, "U")
        enemy(EnemyKind.FIRE_BAR, 22, 7, 0)
        row(28, 7, "U")
        enemy(EnemyKind.FIRE_BAR, 28, 7, 1)
        lava(31, 34)
        enemy(EnemyKind.PODOBOO, 32, 12)
        // C
        fill(35, 60, 10, 14, stone)
        row(40, 6, "M")
        row(46, 6, "U")
        enemy(EnemyKind.FIRE_BAR, 46, 6, 2)
        fill(52, 54, 3, 5, stone)
        row(56, 6, "U")
        enemy(EnemyKind.FIRE_BAR, 56, 6, 3)
        lava(61, 63)
        enemy(EnemyKind.PODOBOO, 62, 12)
        // D: corridor with low ceiling
        fill(64, 90, 11, 14, stone)
        fill(64, 90, 3, 5, stone)
        row(70, 10, "U")
        enemy(EnemyKind.FIRE_BAR, 70, 10, 4)
        row(78, 6, "U")
        enemy(EnemyKind.FIRE_BAR, 78, 6, 5)
        row(84, 10, "U")
        enemy(EnemyKind.FIRE_BAR, 84, 10, 6)
        lava(91, 94)
        enemy(EnemyKind.PODOBOO, 92, 12)
        enemy(EnemyKind.PODOBOO, 93, 12, 1)
        // E
        fill(95, 117, 10, 14, stone)
        row(100, 6, "?")
        row(101, 6, "?")
        row(102, 6, "M")
        row(108, 9, "U")
        enemy(EnemyKind.FIRE_BAR, 108, 9, 7)
        // Bridge over lava with the boss
        lava(118, 132)
        fill(118, 132, 10, 10, Tile.BRIDGE)
        lv.bridgeX0 = 118
        lv.bridgeX1 = 132
        lv.bridgeY = 10
        lv.set(132, 9, Tile.CHAIN)
        enemy(EnemyKind.BOSS, 128, 9)
        fill(133, 159, 10, 14, stone)
        fill(133, 135, 3, 5, stone)
        lv.axeX = 133
        lv.axeY = 9
        build()
    }

    /**
     * World 2-1: overworld behind the castle of world 1, with marching walker trios, paratroopas,
     * a vine hidden under a brick bridge that leads up to coin heaven, and a springboard to a road in the sky.
     */
    private fun level21(): Level = LevelBuilder(288).run {
        lv.name = "2-1"
        lv.time = 400
        lv.music = Song.OVERWORLD
        lv.checkpointX = 88
        zone(0, 213, Theme.OVERWORLD)
        zone(222, 288, Theme.OVERWORLD, fallTo = 128)
        scenery(0, 213)
        startCastle()

        // A: meadow in front of the castle
        ground(0, 39)
        row(10, 9, "?")
        row(14, 9, "BMB?B")
        row(16, 5, "B?B")
        enemy(EnemyKind.TURTLE, 20) // kick its shell into the trio behind it
        walkers(12, 23, 25, 27)
        row(29, 8, "h")
        pipe(32, 3, biter = true)

        // B: stairs up to a brick bridge; one brick of it holds the vine (hit it from below)
        ground(42, 80)
        stairsUp(44, 4)
        row(48, 9, "BBBVBB")
        enemy(EnemyKind.RED_TURTLE, 52, 8)
        walkers(12, 57, 59)
        pipe(62, 2)
        row(66, 9, "?B?")
        row(67, 5, "c")
        pipe(71, 4, biter = true)
        stairsUp(76, 4)
        column(80, 9, 12, Tile.HARD)

        // C: paratroopas hopping around the pits
        ground(83, 100)
        stairsDown(83, 4)
        row(91, 9, "B?M?B")
        enemy(EnemyKind.FLY_TURTLE, 96)
        ground(104, 139)
        enemy(EnemyKind.FLY_TURTLE, 107)
        row(110, 9, "BBBB")
        row(110, 5, "BSBB")
        walkers(12, 114, 116, 118)
        pipe(122, 3, biter = true)

        // D: springboard. A normal bounce collects the coins above it, a well-timed one reaches the sky road.
        spring(132)
        column(132, 6, 8, Tile.COIN)
        row(135, 4, "C".repeat(14))
        row(136, 3, "o".repeat(12))
        ground(143, 165)
        walkers(12, 146, 148, 150)
        enemy(EnemyKind.TURTLE, 155)
        row(157, 9, "B?B")
        pipe(162, 2)

        // E: last stretch, staircase, flag and castle
        ground(169, 212)
        walkers(12, 172, 174, 176)
        row(172, 9, "?B?")
        pipe(178, 2, biter = true)
        stairsUp(182, 8)
        column(190, 5, 12, Tile.HARD)
        flag(199)
        castle(203)

        // Coin heaven: the vine comes up through the first cloud, walking off the end drops back down at column 128
        warp(WarpKind.VINE, 51, 9, 225, 11, ExitKind.VINE_UP)
        scenery(222, 288, hills = false)
        row(224, 11, "C".repeat(44))
        row(229, 7, "oooooooo")
        row(239, 9, "oooooo")
        row(247, 7, "oooooooo")
        row(257, 9, "oooooo")
        build()
    }
}
