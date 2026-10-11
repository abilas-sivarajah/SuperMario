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
