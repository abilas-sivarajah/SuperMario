package com.supanga.bros.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

abstract class Enemy(x: Float, y: Float, w: Int, h: Int) : Entity(x, y, w, h) {
    var knocked = false
    var facing = -1
    protected var anim = 0

    open val stompable = true
    open val fireproof = false
    open val starproof = false
    open val behindTiles = false
    open val bumpsOthers = true
    open val alwaysActive = false
    /** No interaction at all (e.g. a squashed walker). */
    open val inert = false
    open val points = 100

    override fun update(g: Game) {
        anim++
        if (knocked) {
            vy += 0.3f
            x += vx
            y += vy
            if (y > SCREEN_H + 32) dead = true
            return
        }
        act(g)
        if (y > SCREEN_H + 48) dead = true
    }

    abstract fun act(g: Game)

    open fun touchesPlayer(p: Player) = overlaps(p)

    /** Player landed on top. */
    open fun stomped(g: Game) {}

    /** Player touched from the side / below. */
    open fun touched(g: Game) = g.hurtPlayer()

    /** Knocked off the screen (fireball, shell, star, block hit from below). */
    open fun knockOut(dir: Int) {
        knocked = true
        vy = -3.5f
        vx = dir * 0.8f
    }

    /** Returns true if the fireball is used up. */
    open fun fireHit(g: Game, dir: Int): Boolean {
        knockOut(dir)
        g.addScore(points * 2, x, y)
        g.sound.play(Sfx.KICK)
        return true
    }

    protected fun fall() {
        vy += 0.35f
        if (vy > 4f) vy = 4f
    }

    protected fun walk(g: Game) {
        fall()
        if (moveX(g.level, vx)) vx = -vx
        moveY(g.level, vy)
        if (onGround) vy = 0f
        facing = if (vx < 0) -1 else 1
    }
}

class Walker(tx: Int, ty: Int, private val pal: Int) : Enemy(tx * 16f + 1, ty * 16f + 2, 14, 14) {
    private var flat = 0
    override val inert get() = flat > 0 || knocked
    override val stompable get() = flat == 0

    init { vx = -0.5f }

    override fun act(g: Game) {
        if (flat > 0) {
            if (++flat > 30) dead = true
            return
        }
        walk(g)
    }

    override fun stomped(g: Game) {
        flat = 1
        g.sound.play(Sfx.STOMP)
    }

    override fun render(s: Screen, camX: Int) {
        val sx = x.toInt() - 1 - camX
        val sy = bottom.toInt() - 16
        when {
            flat > 0 -> s.draw(Sprites.walkerFlat[pal], sx, sy)
            knocked -> s.draw(Sprites.walker[pal], sx, sy, flipY = true)
            else -> s.draw(Sprites.walker[pal], sx, sy, flipX = (anim / 10) % 2 == 0)
        }
    }
}

class Turtle(tx: Int, ty: Int, private val red: Boolean, private var winged: Boolean) :
    Enemy(tx * 16f + 1, ty * 16f + 2, 14, 14) {

    enum class S { WALK, SHELL, SLIDE }

    var state = S.WALK
        private set
    private var shellTimer = 0
    var kickGrace = 0
        private set
    var combo = 0

    override val points get() = 100

    init { vx = -0.5f }

    override fun act(g: Game) {
        if (kickGrace > 0) kickGrace--
        when (state) {
            S.WALK -> {
                if (red && onGround && edgeAhead(g.level, if (vx < 0) -1 else 1)) vx = -vx
                walk(g)
                if (winged && onGround) vy = -3.6f
                if (winged) { vy -= 0.12f } // floatier hops
            }
            S.SHELL -> {
                fall()
                moveY(g.level, vy)
                if (onGround) vy = 0f
                shellTimer++
                if (shellTimer > 420) {
                    state = S.WALK
                    vx = if (g.player.cx < cx) -0.5f else 0.5f
                }
            }
            S.SLIDE -> {
                fall()
                if (x > g.camX + SCREEN_W + 48) dead = true
                if (moveX(g.level, vx)) {
                    vx = -vx
                    if (x > g.camX - 16 && x < g.camX + SCREEN_W) g.sound.play(Sfx.BUMP)
                }
                moveY(g.level, vy)
                if (onGround) vy = 0f
            }
        }
    }

    fun kick(g: Game) {
        val dir = if (g.player.cx < cx) 1 else -1
        state = S.SLIDE
        vx = dir * 3f
        kickGrace = 10
        combo = 0
        g.sound.play(Sfx.KICK)
        g.addScore(400, x, y)
    }

    override fun stomped(g: Game) {
        when (state) {
            S.WALK -> {
                if (winged) {
                    winged = false
                    vy = 0f
                } else {
                    state = S.SHELL
                    vx = 0f
                    shellTimer = 0
                }
                g.sound.play(Sfx.STOMP)
            }
            S.SHELL -> kick(g)
            S.SLIDE -> {
                state = S.SHELL
                vx = 0f
                shellTimer = 0
                g.sound.play(Sfx.STOMP)
            }
        }
    }

    override fun touched(g: Game) {
        when (state) {
            S.SHELL -> kick(g)
            S.SLIDE -> if (kickGrace == 0) g.hurtPlayer()
            S.WALK -> g.hurtPlayer()
        }
    }

    override fun render(s: Screen, camX: Int) {
        val set = Sprites.turtle[if (red) 1 else 0]
        val sx = x.toInt() - 1 - camX
        val sy = bottom.toInt() - 16
        if (knocked) {
            s.draw(set[2], sx, sy, flipY = true)
            return
        }
        when (state) {
            S.WALK -> {
                val flip = vx > 0
                s.draw(set[(anim / 10) % 2], sx, sy, flipX = flip)
                if (winged) {
                    val wy = sy + 2 + if ((anim / 6) % 2 == 0) 0 else 2
                    s.draw(Sprites.wing, if (flip) sx + 1 else sx + 8, wy, flipX = flip, flipY = (anim / 6) % 2 == 1)
                }
            }
            S.SHELL -> {
                val waking = shellTimer > 330 && (anim / 4) % 2 == 0
                s.draw(set[if (waking) 3 else 2], sx, sy)
            }
            S.SLIDE -> s.draw(set[2], sx, sy, flipX = (anim / 3) % 2 == 0)
        }
    }
}

/** Plant that rises out of a pipe. */
class Biter(tx: Int, private val pipeTop: Int) : Enemy(tx * 16f + 8, pipeTop * 16f, 16, 24) {
    private val hiddenY = pipeTop * 16f
    private val shownY = pipeTop * 16f - 24
    private var phase = 0 // 0 hidden, 1 rising, 2 shown, 3 sinking
    private var timer = 60

    override val stompable = false
    override val behindTiles = true
    override val bumpsOthers = false
    override val points = 200
    override val inert get() = knocked || y >= hiddenY - 2

    override fun act(g: Game) {
        when (phase) {
            0 -> {
                y = hiddenY
                val near = abs(g.player.cx - (x + 8)) < 28
                if (--timer <= 0 && !near) phase = 1
                if (near && timer <= 0) timer = 1
            }
            1 -> { y -= 0.5f; if (y <= shownY) { y = shownY; phase = 2; timer = 60 } }
            2 -> if (--timer <= 0) phase = 3
            3 -> { y += 0.5f; if (y >= hiddenY) { phase = 0; timer = 70 } }
        }
    }

    override fun touchesPlayer(p: Player) = p.overlaps(x + 3, y + 2, 10f, hiddenY - y - 2)

    override fun knockOut(dir: Int) {
        // plants just disappear in a puff
        dead = true
    }

    override fun fireHit(g: Game, dir: Int): Boolean {
        g.effects += Puff(x + 8, y + 8, false)
        return super.fireHit(g, dir)
    }

    override fun render(s: Screen, camX: Int) {
        if (y >= hiddenY) return
        s.draw(Sprites.biter[(anim / 8) % 2], x.toInt() - camX, y.toInt())
    }
}

/** Lava bubble that jumps out of a pit. */
class Podoboo(tx: Int, delay: Int) : Enemy(tx * 16f + 3, 250f, 10, 12) {
    private var wait = 30 + delay * 70

    override val stompable = false
    override val fireproof = true
    override val starproof = true
    override val bumpsOthers = false
    override val inert get() = wait > 0

    override fun act(g: Game) {
        if (wait > 0) {
            if (--wait == 0) vy = -6.2f
            return
        }
        vy += 0.18f
        y += vy
        if (y > 250f && vy > 0f) {
            y = 250f
            wait = 80 + (anim * 7) % 60
        }
    }

    override fun fireHit(g: Game, dir: Int) = false
    override fun knockOut(dir: Int) {}

    override fun render(s: Screen, camX: Int) {
        if (wait > 0) return
        s.draw(Sprites.podoboo, x.toInt() - 3 - camX, y.toInt() - 2, flipY = vy > 0f)
    }
}

/** Chain of fireballs rotating around a block. */
class FireBar(tx: Int, ty: Int, idx: Int) : Enemy(tx * 16f, ty * 16f, 16, 16) {
    private val ox = tx * 16f + 8
    private val oy = ty * 16f + 8
    private var angle = idx * 1.7
    private val speed = if (idx % 2 == 0) 0.034 else -0.034
    private val balls = 6

    override val stompable = false
    override val fireproof = true
    override val starproof = true
    override val bumpsOthers = false

    override fun act(g: Game) { angle += speed }

    override fun fireHit(g: Game, dir: Int) = false
    override fun knockOut(dir: Int) {}

    override fun touchesPlayer(p: Player): Boolean {
        for (i in 0 until balls) {
            val bx = ox + (cos(angle) * i * 8).toFloat()
            val by = oy + (sin(angle) * i * 8).toFloat()
            if (p.overlaps(bx - 3, by - 3, 6f, 6f)) return true
        }
        return false
    }

    override fun render(s: Screen, camX: Int) {
        for (i in 0 until balls) {
            val bx = ox + (cos(angle) * i * 8).toFloat()
            val by = oy + (sin(angle) * i * 8).toFloat()
            val f = (anim / 3) % 4
            s.draw(Sprites.fireball, bx.toInt() - 4 - camX, by.toInt() - 4, flipX = f == 1 || f == 2, flipY = f >= 2)
        }
    }
}

/** The fire-breathing castle boss, Krawallo. */
class Boss(tx: Int, ty: Int, private val minX: Float, private val maxX: Float) :
    Enemy(tx * 16f + 4, (ty + 1) * 16f - 28, 24, 28) {
    private var hp = 5
    private var fireTimer = 150
    private var mouth = 0
    private var moveTimer = 40
    private var flash = 0
    var frozen = false

    override val stompable = false
    override val starproof = true
    override val bumpsOthers = false
    override val alwaysActive = true
    override val points = 2500

    override fun act(g: Game) {
        if (flash > 0) flash--
        if (frozen) {
            fall()
            moveY(g.level, vy)
            if (onGround) vy = 0f
            if (y > SCREEN_H + 40) dead = true
            return
        }
        facing = if (g.player.cx < cx) -1 else 1
        if (--moveTimer <= 0) {
            vx = when ((anim / 7) % 3) { 0 -> -0.5f; 1 -> 0.5f; else -> 0f }
            moveTimer = 40 + (anim * 13) % 70
        }
        if (x < minX) vx = abs(vx)
        if (x > maxX) vx = -abs(vx)
        fall()
        moveX(g.level, vx)
        moveY(g.level, vy)
        if (onGround) {
            vy = 0f
            if ((anim % 150) == 75) vy = -3.6f
        }
        val onScreen = x < g.camX + SCREEN_W && x + w > g.camX
        if (mouth > 0) mouth--
        if (onScreen && --fireTimer <= 0) {
            val p = g.player
            val targetY = (ftile(p.y + p.h - 8) * 16f + 4).coerceAtMost(y + 20)
            g.enemies += BossFire(x - 20, y + 6, targetY)
            g.sound.play(Sfx.BOSS_FIRE)
            fireTimer = 140 + (anim * 17) % 90
        }
        if (fireTimer == 30) mouth = 40
        if (y > SCREEN_H + 40) dead = true
    }

    override fun fireHit(g: Game, dir: Int): Boolean {
        if (frozen) return true
        flash = 8
        g.effects += Puff(x + 12, y + 12)
        if (--hp <= 0) {
            knockOut(dir)
            g.addScore(5000, x, y)
            g.sound.play(Sfx.BOSS_FALL)
        }
        return true
    }

    override fun knockOut(dir: Int) {
        knocked = true
        vy = -3f
        vx = 0f
    }

    override fun render(s: Screen, camX: Int) {
        val img = if (mouth > 0) Sprites.bossOpen else Sprites.bossClosed
        val sx = x.toInt() - 4 - camX
        val sy = bottom.toInt() - 31
        if (flash > 0 && flash % 2 == 0) s.drawTinted(img, sx, sy, Colors.WHITE, flipX = facing > 0)
        else s.draw(img, sx, sy, flipX = facing > 0, flipY = knocked)
    }
}

class BossFire(x: Float, y: Float, private val targetY: Float) : Enemy(x, y + 2, 24, 4) {
    override val stompable = false
    override val fireproof = true
    override val starproof = true
    override val bumpsOthers = false

    init { vx = -1.7f }

    override fun act(g: Game) {
        x += vx
        if (y < targetY) y = minOf(targetY, y + 0.6f) else if (y > targetY) y = maxOf(targetY, y - 0.6f)
        if (x + w < g.camX - 8) dead = true
    }

    override fun fireHit(g: Game, dir: Int) = false
    override fun knockOut(dir: Int) {}

    override fun render(s: Screen, camX: Int) =
        s.draw(Sprites.bossFire, x.toInt() - camX, y.toInt() - 2, flipY = (anim / 4) % 2 == 0)
}
