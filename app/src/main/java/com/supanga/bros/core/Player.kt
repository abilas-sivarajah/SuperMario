package com.supanga.bros.core

import kotlin.math.abs
import kotlin.math.sign

/** Controller state, written by the platform layer and read once per frame by the game. */
class Buttons {
    var left = false
    var right = false
    var up = false
    var down = false
    var jump = false
    var run = false
    var start = false

    fun copyFrom(o: Buttons) {
        left = o.left; right = o.right; up = o.up; down = o.down; jump = o.jump; run = o.run; start = o.start
    }
}

enum class PMode { NORMAL, GROW, SHRINK, FIRE_UP, DEAD, PIPE_DOWN, PIPE_RIGHT, PIPE_UP, FLAG_SLIDE, FLAG_WAIT, AUTO_WALK, HIDDEN }

/** The hero, Nga. Movement values follow the feel of classic 8-bit platformers (pixels per frame at 60 fps). */
class Player(x: Float, y: Float) : Entity(x, y, 12, 14) {
    companion object {
        const val WALK_ACC = 0.0371f
        const val RUN_ACC = 0.0557f
        const val RELEASE_DEC = 0.0508f
        const val SKID_DEC = 0.1016f
        const val MIN_WALK = 0.0742f
        const val MAX_WALK = 1.5625f
        const val MAX_RUN = 2.5625f
        const val MAX_FALL = 4.5f
    }

    var power = 0 // 0 small, 1 big, 2 fire
        private set
    var facing = 1
    var ducking = false
    var skidding = false
    var mode = PMode.NORMAL
    var modeTimer = 0
    var invuln = 0
    var star = 0
    var stompCombo = 0
    var riding: Lift? = null
    var prevBottom = 0f
    var warp: Warp? = null
    var walkTargetX = 0f

    private var holdG = 0.125f
    private var fallG = 0.4375f
    private var airMax = MAX_WALK
    private var takeoffSpeed = 0f
    private var jumped = false
    private var animTick = 0f
    private var walkFrame = 0
    private var throwTimer = 0
    private var deadTimer = 0

    val big get() = power > 0

    fun setPower(p: Int) {
        val b = bottom
        power = p
        ducking = false
        h = if (p > 0) 28 else 14
        y = b - h
    }

    private fun setDuck(d: Boolean) {
        if (d == ducking) return
        val b = bottom
        ducking = d
        h = if (d || power == 0) 14 else 28
        y = b - h
    }

    override fun solid(lv: Level, tx: Int, ty: Int): Boolean {
        val t = lv.get(tx, ty)
        // The level's left edge is a wall, but the right end is open (so the player can walk into the castle).
        if (tx >= lv.width) return false
        return Tile.solid(t)
    }

    fun startJump(g: Game, run: Boolean) {
        val spd = abs(vx)
        when {
            spd < 1f -> { vy = -4.1f; holdG = 0.125f; fallG = 0.4375f }
            spd < 2.3125f -> { vy = -4.1f; holdG = 0.1172f; fallG = 0.375f }
            else -> { vy = -5f; holdG = 0.15625f; fallG = 0.5625f }
        }
        takeoffSpeed = spd
        airMax = if (run || spd > MAX_WALK + 0.01f) MAX_RUN else MAX_WALK
        jumped = true
        onGround = false
        riding = null
        g.sound.play(if (big) Sfx.JUMP_BIG else Sfx.JUMP_SMALL)
    }

    fun landOn(top: Float) {
        y = top - h
        vy = 0f
        onGround = true
        jumped = false
        stompCombo = 0
    }

    fun bounce(jumpHeld: Boolean) {
        vy = -4f
        holdG = 0.125f
        fallG = 0.4375f
        jumped = true
    }

    fun update(g: Game, b: Buttons, aPressed: Boolean, bPressed: Boolean) {
        if (invuln > 0) invuln--
        if (star > 0) star--
        if (throwTimer > 0) throwTimer--
        val lv = g.level
        val dir = (if (b.right) 1 else 0) - (if (b.left) 1 else 0)

        if (onGround) {
            setDuck(power > 0 && b.down)
            val maxSpd = if (b.run) MAX_RUN else MAX_WALK
            if (ducking || dir == 0) {
                skidding = false
                decelerate(RELEASE_DEC)
            } else if (vx != 0f && sign(vx).toInt() != dir) {
                skidding = true
                vx += dir * SKID_DEC
                if (sign(vx).toInt() == dir || abs(vx) < SKID_DEC) {
                    vx = 0f
                    skidding = false
                    facing = dir
                }
            } else {
                skidding = false
                facing = dir
                if (abs(vx) < MIN_WALK) vx = dir * MIN_WALK
                val acc = if (b.run) RUN_ACC else WALK_ACC
                if (abs(vx) < maxSpd) {
                    vx += dir * acc
                    if (abs(vx) > maxSpd) vx = dir * maxSpd
                } else {
                    vx -= dir * RELEASE_DEC
                    if (abs(vx) < maxSpd) vx = dir * maxSpd
                }
            }
            if (aPressed) startJump(g, b.run)
        } else if (dir != 0) {
            val spd = abs(vx)
            if (vx == 0f || sign(vx).toInt() == dir) {
                vx += dir * (if (spd >= MAX_WALK) RUN_ACC else WALK_ACC)
                if (abs(vx) > airMax) vx = dir * airMax
            } else {
                vx += dir * when {
                    spd >= MAX_WALK -> RUN_ACC
                    takeoffSpeed >= 1.8125f -> RELEASE_DEC
                    else -> WALK_ACC
                }
            }
        }

        vy += if (b.jump && vy < 0f) holdG else fallG
        if (vy > MAX_FALL) vy = MAX_FALL

        if (bPressed && power == 2 && !ducking && g.throwFireball(this)) throwTimer = 8

        if (moveX(lv, vx)) vx = 0f

        val oldTop = y
        val wasRising = vy < 0f
        if (moveY(lv, vy)) {
            if (wasRising) {
                headBump(g, oldTop)
            } else {
                vy = 0f
                jumped = false
                stompCombo = 0
                holdG = 0.125f
                fallG = 0.4375f
            }
        } else if (wasRising) {
            checkHidden(g, oldTop)
        }

        // keep inside the camera / zone
        val minX = g.camX.toFloat()
        if (x < minX) { x = minX; if (vx < 0) vx = 0f }
        val zone = lv.zoneAt(ftile(cx))
        val maxX = zone.x1 * TILE - w.toFloat()
        if (x > maxX && g.state == Game.State.PLAYING) { x = maxX; if (vx > 0) vx = 0f }

        animate()
    }

    private fun decelerate(d: Float) {
        if (abs(vx) <= d) vx = 0f else vx -= sign(vx) * d
    }

    private fun headBump(g: Game, oldTop: Float) {
        val lv = g.level
        val ty = ftile(y - 1)
        val center = ftile(cx)
        val left = ftile(x)
        val right = ftile(x + w - 0.01f)
        val tx = when {
            lv.solidAt(center, ty) -> center
            lv.solidAt(left, ty) -> left
            else -> right
        }
        vy = 1f
        g.hitBlock(tx, ty, this)
    }

    private fun checkHidden(g: Game, oldTop: Float) {
        val lv = g.level
        val tx = ftile(cx)
        val ty = ftile(y)
        if (lv.get(tx, ty) == Tile.HIDDEN && oldTop >= (ty + 1) * TILE - 0.01f) {
            y = (ty + 1f) * TILE
            vy = 1f
            g.hitBlock(tx, ty, this)
        }
    }

    private fun animate() {
        val spd = abs(vx)
        if (onGround && spd > 0.05f) {
            animTick += spd * 0.11f + 0.04f
            if (animTick >= 1f) {
                animTick = 0f
                walkFrame = (walkFrame + 1) % 3
            }
        }
    }

    /** Walks automatically (after the flag pole and in the castle). */
    fun autoWalk(g: Game) {
        vx = 1.25f
        facing = 1
        vy = minOf(vy + 0.4375f, MAX_FALL)
        moveX(g.level, vx)
        moveY(g.level, vy)
        if (onGround) vy = 0f
        animTick += 0.25f
        if (animTick >= 1f) { animTick = 0f; walkFrame = (walkFrame + 1) % 3 }
    }

    fun updateDead() {
        deadTimer++
        if (deadTimer == 30) vy = -4.2f
        if (deadTimer > 30) {
            vy += 0.25f
            y += vy
        }
    }

    fun die() {
        mode = PMode.DEAD
        deadTimer = 0
        vx = 0f
        vy = 0f
        star = 0
        setPower(0)
    }

    private fun frameIndex(): Int = when {
        mode == PMode.DEAD -> Sprites.SPECIAL
        mode == PMode.FLAG_SLIDE || mode == PMode.FLAG_WAIT -> Sprites.JUMP
        mode == PMode.PIPE_DOWN || mode == PMode.PIPE_UP -> Sprites.STAND
        ducking -> Sprites.SPECIAL
        !onGround && jumped -> Sprites.JUMP
        throwTimer > 0 -> Sprites.WALK3
        skidding -> Sprites.SKID
        mode == PMode.AUTO_WALK || mode == PMode.PIPE_RIGHT || (onGround && abs(vx) > 0.05f) -> Sprites.WALK1 + walkFrame
        !onGround -> Sprites.WALK1 + walkFrame
        else -> Sprites.STAND
    }

    fun palette(frame: Long): Int = when {
        star > 0 -> Sprites.PAL_STAR + ((frame / (if (star < 120) 4 else 2)) % 3).toInt()
        mode == PMode.FIRE_UP -> if ((modeTimer / 4) % 2 == 0) Sprites.PAL_FIRE else Sprites.PAL_NORMAL
        power == 2 -> Sprites.PAL_FIRE
        else -> Sprites.PAL_NORMAL
    }

    var renderFrame = 0L

    override fun render(s: Screen, camX: Int) {
        if (mode == PMode.HIDDEN) return
        if (invuln > 0 && mode == PMode.NORMAL && (renderFrame / 2) % 2 == 0L) return
        val pal = palette(renderFrame)
        val sx = x.toInt() - 2 - camX
        val flip = facing < 0
        when (mode) {
            PMode.GROW, PMode.SHRINK -> {
                val showBig = ((modeTimer / 4) % 2 == 0) == (mode == PMode.GROW)
                val b = bottom.toInt()
                if (showBig) s.draw(Sprites.big[pal][Sprites.STAND], sx, b - 32, flip)
                else s.draw(Sprites.small[pal][Sprites.STAND], sx, b - 16, flip)
                return
            }
            else -> {}
        }
        val f = frameIndex()
        val b = bottom.toInt()
        if (power == 0 || mode == PMode.DEAD) s.draw(Sprites.small[pal][f], sx, b - 16, flip)
        else s.draw(Sprites.big[pal][f], sx, b - 32, flip)
    }
}
