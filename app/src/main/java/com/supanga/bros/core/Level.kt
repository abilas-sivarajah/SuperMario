package com.supanga.bros.core

object Tile {
    const val EMPTY = 0
    const val GROUND = 1
    const val BRICK = 2
    const val QUESTION = 3
    const val USED = 4
    const val HARD = 5
    const val PIPE_TL = 6
    const val PIPE_TR = 7
    const val PIPE_L = 8
    const val PIPE_R = 9
    const val HIDDEN = 10
    const val COIN = 11
    const val TREE_L = 12
    const val TREE_M = 13
    const val TREE_R = 14
    const val TRUNK = 15
    const val BRIDGE = 16
    const val CHAIN = 17
    const val LAVA_TOP = 18
    const val LAVA = 19
    const val HPIPE_MOUTH_T = 20
    const val HPIPE_MOUTH_B = 21
    const val HPIPE_T = 22
    const val HPIPE_B = 23
    const val CLOUD = 24

    fun solid(t: Int) = when (t) {
        GROUND, BRICK, QUESTION, USED, HARD, PIPE_TL, PIPE_TR, PIPE_L, PIPE_R,
        TREE_L, TREE_M, TREE_R, BRIDGE, HPIPE_MOUTH_T, HPIPE_MOUTH_B, HPIPE_T, HPIPE_B, CLOUD -> true
        else -> false
    }

    /** Tiles that react when hit from below. */
    fun bumpable(t: Int) = t == BRICK || t == QUESTION || t == HIDDEN
}

enum class Content { NONE, COIN, POWER, ONE_UP, STAR, MULTI_COIN, VINE }

enum class EnemyKind { WALKER, TURTLE, RED_TURTLE, FLY_TURTLE, BITER, PODOBOO, FIRE_BAR, BOSS }

class Spawn(val kind: EnemyKind, val tx: Int, val ty: Int, val param: Int = 0) {
    var spawned = false
}

/**
 * A horizontal stretch of the map with its own look and camera bounds (main area, bonus room, ...).
 * Falling out of a zone with [fallTo] >= 0 is not deadly: the player drops back in at that column (coin heaven).
 */
class Zone(val x0: Int, val x1: Int, val theme: Theme, val bg: Int = theme.bg, val fallTo: Int = -1)

enum class WarpKind { PIPE_DOWN, PIPE_RIGHT, VINE }
enum class ExitKind { PIPE_UP, DROP, VINE_UP }

/**
 * Entering pipe tile ([tx],[ty]) moves the player to ([destX],[destY]) (tile coordinates).
 * For [WarpKind.VINE] ([tx],[ty]) is the block the vine grows out of; climbing off the top of the screen warps.
 */
class Warp(val kind: WarpKind, val tx: Int, val ty: Int, val destX: Int, val destY: Int, val exit: ExitKind, val toLevel: Int = -1)

enum class LiftKind { LOOP_UP, LOOP_DOWN, SWING_X, SWING_Y, FALL }

class LiftSpawn(val kind: LiftKind, val x: Float, val y: Float, val widthTiles: Int, val range: Float = 0f)

enum class Deco { CLOUD1, CLOUD2, CLOUD3, BUSH1, BUSH2, BUSH3, HILL_BIG, HILL_SMALL, CASTLE }

class Decoration(val kind: Deco, val x: Int, val y: Int)

class Level(val width: Int, val height: Int = 15) {
    val tiles = IntArray(width * height)
    val contents = HashMap<Int, Content>()
    val multiCoinLeft = HashMap<Int, Int>()
    val spawns = mutableListOf<Spawn>()
    val lifts = mutableListOf<LiftSpawn>()
    val warps = mutableListOf<Warp>()
    val zones = mutableListOf<Zone>()
    val decorations = mutableListOf<Decoration>()
    /** Springboards, by the tile column and the upper of the two rows they fill. */
    val springs = mutableListOf<Pair<Int, Int>>()

    var name = "1-1"
    var time = 400
    var music = Song.OVERWORLD
    var startX = 2
    var startY = 12
    var startFromPipe = false
    var checkpointX = -1
    var flagX = -1
    var castleX = -1
    var castleLevel = false
    var axeX = -1
    var axeY = -1
    var bridgeX0 = -1
    var bridgeX1 = -1
    var bridgeY = -1
    /** While the player is below the ceiling the camera may not scroll past this (hides the warp zone). */
    var camLimitX = -1
    var warpZoneX = -1
    val warpLabels = mutableListOf<Pair<Int, String>>()

    // Block bump animation (a block hit from below moves up a few pixels and back).
    var bumpX = -1
    var bumpY = -1
    var bumpTimer = 0

    fun idx(x: Int, y: Int) = y * width + x

    fun get(x: Int, y: Int): Int {
        if (x < 0 || x >= width) return Tile.HARD
        if (y < 0 || y >= height) return Tile.EMPTY
        return tiles[idx(x, y)]
    }

    fun set(x: Int, y: Int, t: Int) {
        if (x in 0 until width && y in 0 until height) tiles[idx(x, y)] = t
    }

    fun solidAt(x: Int, y: Int) = Tile.solid(get(x, y))

    fun contentAt(x: Int, y: Int) = contents[idx(x, y)] ?: Content.NONE

    fun zoneAt(tx: Int): Zone = zones.firstOrNull { tx >= it.x0 && tx < it.x1 } ?: zones.first()

    fun bumpOffset(x: Int, y: Int): Int {
        if (x != bumpX || y != bumpY || bumpTimer <= 0) return 0
        val t = 10 - bumpTimer
        return -(if (t < 5) t else 10 - t)
    }

    fun update() {
        if (bumpTimer > 0) bumpTimer--
    }
}
