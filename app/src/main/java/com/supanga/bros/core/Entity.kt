package com.supanga.bros.core

import kotlin.math.floor

fun ftile(v: Float): Int = floor(v / TILE).toInt()

/** Base class of everything that moves. (x, y) is the top-left corner of the hitbox. */
abstract class Entity(var x: Float, var y: Float, var w: Int, var h: Int) {
    var vx = 0f
    var vy = 0f
    var dead = false
    var onGround = false

    val cx get() = x + w / 2f
    val bottom get() = y + h

    open fun update(g: Game) {}
    abstract fun render(s: Screen, camX: Int)

    fun overlaps(o: Entity) = x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y

    fun overlaps(ox: Float, oy: Float, ow: Float, oh: Float) = x < ox + ow && x + w > ox && y < oy + oh && y + h > oy

    protected open fun solid(lv: Level, tx: Int, ty: Int) = lv.solidAt(tx, ty)

    /** Moves horizontally, stops at walls. Returns true when blocked. */
    fun moveX(lv: Level, dx: Float): Boolean {
        if (dx == 0f) return false
        x += dx
        val top = ftile(y)
        val bot = ftile(y + h - 0.01f)
        if (dx > 0) {
            val tx = ftile(x + w - 0.01f)
            for (ty in top..bot) if (solid(lv, tx, ty)) { x = tx * TILE - w.toFloat(); return true }
        } else {
            val tx = ftile(x)
            for (ty in top..bot) if (solid(lv, tx, ty)) { x = (tx + 1f) * TILE; return true }
        }
        return false
    }

    /** Moves vertically, sets [onGround] when landing. Returns true when blocked. */
    fun moveY(lv: Level, dy: Float): Boolean {
        onGround = false
        if (dy == 0f) return false
        y += dy
        val left = ftile(x)
        val right = ftile(x + w - 0.01f)
        if (dy > 0) {
            val ty = ftile(y + h - 0.01f)
            for (tx in left..right) if (solid(lv, tx, ty)) {
                y = ty * TILE - h.toFloat()
                onGround = true
                return true
            }
        } else {
            val ty = ftile(y)
            for (tx in left..right) if (solid(lv, tx, ty)) {
                y = (ty + 1f) * TILE
                return true
            }
        }
        return false
    }

    /** True when there is no floor right in front of the entity (used by red turtles). */
    fun edgeAhead(lv: Level, dir: Int): Boolean {
        val probeX = if (dir > 0) x + w + 1 else x - 1
        return !lv.solidAt(ftile(probeX), ftile(y + h + 1))
    }
}

/** Short-lived visual effects that don't interact with anything. */
class ScorePopup(x: Float, y: Float, val text: String) : Entity(x, y, 0, 0) {
    private var t = 0
    override fun update(g: Game) {
        t++
        if (t < 30) y -= 1.2f
        if (t > 50) dead = true
    }

    override fun render(s: Screen, camX: Int) = s.text(text, x.toInt() - camX, y.toInt(), Colors.WHITE)
}

class Debris(x: Float, y: Float, vx0: Float, vy0: Float, private val img: Image) : Entity(x, y, 8, 8) {
    private var t = 0
    init { vx = vx0; vy = vy0 }

    override fun update(g: Game) {
        t++
        vy += 0.3f
        x += vx
        y += vy
        if (y > SCREEN_H + 16) dead = true
    }

    override fun render(s: Screen, camX: Int) = s.draw(img, x.toInt() - camX, y.toInt(), flipX = (t / 4) % 2 == 0)
}

class CoinPop(x: Float, y: Float) : Entity(x, y, 16, 16) {
    private var t = 0
    init { vy = -5.5f }

    override fun update(g: Game) {
        t++
        vy += 0.32f
        y += vy
        if (t >= 32) {
            dead = true
            g.addPopup(x, y, "200")
        }
    }

    override fun render(s: Screen, camX: Int) = s.draw(Sprites.coinSpin[(t / 2) % 4], x.toInt() - camX, y.toInt())
}

/** Small burst used when fireballs hit something and for the castle fireworks. */
class Puff(x: Float, y: Float, private val big: Boolean = false) : Entity(x, y, 0, 0) {
    private var t = 0
    override fun update(g: Game) {
        t++
        if (t > (if (big) 30 else 12)) dead = true
    }

    override fun render(s: Screen, camX: Int) {
        val r = if (big) 3 + t / 2 else 2 + t / 2
        val cx = x.toInt() - camX
        val cy = y.toInt()
        val colors = intArrayOf(Colors.WHITE, 0xFFFCD840.toInt(), 0xFFFC9838.toInt(), 0xFFE83818.toInt())
        val col = colors[minOf(3, t / (if (big) 8 else 3))]
        val n = if (big) 16 else 8
        for (i in 0 until n) {
            val a = i * Math.PI * 2 / n
            val px = cx + (kotlin.math.cos(a) * r).toInt()
            val py = cy + (kotlin.math.sin(a) * r).toInt()
            s.fillRect(px - 1, py - 1, 2, 2, col)
            if (big) s.fillRect(cx + (kotlin.math.cos(a) * r * 0.5).toInt(), cy + (kotlin.math.sin(a) * r * 0.5).toInt(), 2, 2, Colors.WHITE)
        }
    }
}
