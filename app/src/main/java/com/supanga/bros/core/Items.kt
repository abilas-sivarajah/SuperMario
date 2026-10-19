package com.supanga.bros.core

import kotlin.math.sin

enum class ItemKind { MUSHROOM, FLOWER, STAR, ONE_UP }

/** Power-ups that come out of blocks. */
class PowerUp(x: Float, y: Float, val kind: ItemKind) : Entity(x, y, 16, 16) {
    var emerge = 32
        private set
    private var anim = 0

    override fun update(g: Game) {
        anim++
        if (emerge > 0) {
            emerge--
            y -= 0.5f
            if (emerge == 0 && kind != ItemKind.FLOWER) vx = 1f
            return
        }
        when (kind) {
            ItemKind.FLOWER -> {}
            ItemKind.STAR -> {
                vy += 0.25f
                if (moveX(g.level, vx)) vx = -vx
                moveY(g.level, vy)
                if (onGround) vy = -4.5f
            }
            else -> {
                vy = minOf(vy + 0.35f, 4f)
                if (moveX(g.level, vx)) vx = -vx
                moveY(g.level, vy)
                if (onGround) vy = 0f
            }
        }
        if (y > SCREEN_H + 16 || x + w < g.camX - 32) dead = true
    }

    /** The block below was hit: make it jump. */
    fun hop(dir: Int) {
        if (kind == ItemKind.FLOWER || emerge > 0) return
        vy = -3.5f
        vx = dir * 1f
    }

    override fun render(s: Screen, camX: Int) {
        val img = when (kind) {
            ItemKind.MUSHROOM -> Sprites.mushroom
            ItemKind.ONE_UP -> Sprites.oneUp
            ItemKind.FLOWER -> Sprites.flower[(anim / 4) % 4]
            ItemKind.STAR -> Sprites.star[(anim / 4) % 4]
        }
        s.draw(img, x.toInt() - camX, y.toInt())
    }
}

class Fireball(x: Float, y: Float, dir: Int) : Entity(x, y, 8, 8) {
    private var anim = 0

    init {
        vx = dir * 4f
        vy = 2.5f
    }

    override fun update(g: Game) {
        anim++
        vy = minOf(vy + 0.35f, 4f)
        if (moveX(g.level, vx)) {
            explode(g)
            return
        }
        moveY(g.level, vy)
        if (onGround) vy = -3.2f
        if (x < g.camX - 8 || x > g.camX + SCREEN_W + 8 || y > SCREEN_H) dead = true
    }

    fun explode(g: Game) {
        dead = true
        g.effects += Puff(x + 4, y + 4)
        g.sound.play(Sfx.BUMP)
    }

    override fun render(s: Screen, camX: Int) {
        val f = (anim / 2) % 4
        s.draw(Sprites.fireball, x.toInt() - camX, y.toInt(), flipX = f == 1 || f == 2, flipY = f >= 2)
    }
}

/** Moving platform. Only solid from above. */
class Lift(private val spec: LiftSpawn) : Entity(spec.x, spec.y, spec.widthTiles * 16, 8) {
    var dx = 0f
        private set
    var dy = 0f
        private set
    var ridden = false
    private var t = 0

    override fun update(g: Game) {
        val ox = x
        val oy = y
        t++
        when (spec.kind) {
            LiftKind.LOOP_UP -> { y -= 0.9f; if (y < -16f) y = SCREEN_H + 8f }
            LiftKind.LOOP_DOWN -> { y += 0.9f; if (y > SCREEN_H + 8f) y = -16f }
            LiftKind.SWING_X -> x = spec.x + sin(t * 0.02f) * spec.range
            LiftKind.SWING_Y -> y = spec.y + sin(t * 0.02f) * spec.range
            LiftKind.FALL -> if (ridden) { vy = minOf(vy + 0.12f, 3f); y += vy }
        }
        dx = x - ox
        dy = y - oy
        if (spec.kind == LiftKind.FALL && y > SCREEN_H + 16) dead = true
    }

    override fun render(s: Screen, camX: Int) {
        for (i in 0 until spec.widthTiles) s.draw(Sprites.lift, x.toInt() + i * 16 - camX, y.toInt())
    }
}

/**
 * Beanstalk that grows out of a block ([tx],[ty]) up past the top of the screen. The player can climb it.
 * Vines that are only scenery (the one in coin heaven) start fully grown and can't be grabbed.
 */
class Vine(val tx: Int, val ty: Int, val baseY: Float = ty * 16f, top: Float = baseY, val climbable: Boolean = true) :
    Entity(tx * 16f, top, 16, (baseY - top).toInt()) {

    override fun update(g: Game) {
        if (climbable && y > TOP) {
            y = maxOf(TOP, y - 1.25f)
            h = (baseY - y).toInt()
        }
    }

    /** True when the player is close enough to the stalk to hold on to it. */
    fun reaches(p: Player) = climbable && p.x + p.w > x + 3 && p.x < x + 13 && p.bottom > y + 4 && p.y < baseY

    override fun render(s: Screen, camX: Int) {
        val sx = x.toInt() - camX
        if (sx < -16 || sx > SCREEN_W) return
        s.draw(Sprites.vineTop, sx, y.toInt())
        var sy = y.toInt() + 16
        while (sy < baseY) {
            s.draw(Sprites.vine, sx, sy)
            sy += 16
        }
    }

    companion object {
        /** Grown vines reach this far above the screen. */
        const val TOP = -40f
    }
}

/** Springboard, two tiles high. Landing on it squeezes it; pressing jump while it squeezes gives a huge leap. */
class Spring(tx: Int, ty: Int) : Entity(tx * 16f, ty * 16f, 16, 32) {
    private var t = -1
    var press = 0
        private set
    /** Set when the rider pressed jump in time. */
    var boost = false

    val top get() = y + press

    /** Advances the squeeze by one frame while someone stands on it. Returns true when it throws the rider. */
    fun squeeze(): Boolean {
        t++
        if (t >= SQUEEZE.size) {
            t = -1
            press = 0
            return true
        }
        press = SQUEEZE[t]
        return false
    }

    override fun render(s: Screen, camX: Int) = s.draw(Sprites.spring[press], x.toInt() - camX, y.toInt())

    companion object {
        private val SQUEEZE = intArrayOf(3, 6, 9, 12, 14, 16, 16, 16, 16, 12, 8, 4)
    }
}
