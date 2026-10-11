package com.supanga.bros.core

import kotlin.math.abs

interface Storage {
    fun loadTop(): Int
    fun saveTop(value: Int)
}

object NoStorage : Storage {
    override fun loadTop() = 0
    override fun saveTop(value: Int) {}
}

/**
 * The whole game: state machine, world simulation and rendering into a [Screen].
 * Runs at a fixed 60 updates per second; it has no dependency on Android.
 */
class Game(val sound: Sound = NoSound, private val storage: Storage = NoStorage) {

    enum class State { TITLE, INTRO, PLAYING, DYING, FLAG, COUNT, FIREWORKS, LEVEL_DONE, AXE, RESCUE, GAME_OVER }

    var state = State.TITLE
        private set
    private var timer = 0
    var frame = 0L
        private set
    var paused = false
        private set

    var score = 0
        private set
    var coins = 0
        private set
    var lives = 3
        private set
    var levelIndex = 0
        private set
    var top = storage.loadTop()
        private set

    private var savedPower = 0
    private var checkpoint = false
    private var timeUp = false

    lateinit var level: Level
        private set
    lateinit var player: Player
        private set
    val enemies = mutableListOf<Enemy>()
    val items = mutableListOf<PowerUp>()
    val fireballs = mutableListOf<Fireball>()
    val effects = mutableListOf<Entity>()
    val lifts = mutableListOf<Lift>()

    var camX = 0
        private set
    var time = 400
        private set
    private var timeTick = 0
    private var hurry = false
    private var hurryPending = false
    private var starMusic = false
    private var flagY = 40f
    private var castleFlag = 0
    private var fireworks = 0
    private var fireworkTimer = 0
    private var bridgeIdx = 0
    private var axeTaken = false
    private var bossFallPlayed = false
    private var rescueStep = 0

    private val cur = Buttons()
    private val prev = Buttons()

    init {
        loadLevel(0)
        state = State.TITLE
    }

    // ------------------------------------------------------------------ flow

    fun pause() {
        if (state != State.TITLE && state != State.INTRO && state != State.GAME_OVER) paused = true
    }

    fun togglePause() {
        if (state == State.TITLE || state == State.INTRO || state == State.GAME_OVER) return
        paused = !paused
        sound.play(Sfx.PAUSE)
    }

    private fun newGame() {
        score = 0
        coins = 0
        lives = 3
        levelIndex = 0
        savedPower = 0
        checkpoint = false
        timeUp = false
        hurry = false
        toIntro()
    }

    private fun toIntro() {
        state = State.INTRO
        timer = 0
        sound.stopMusic()
    }

    private fun toTitle() {
        saveTop()
        checkpoint = false
        loadLevel(0)
        state = State.TITLE
        timer = 0
        sound.stopMusic()
    }

    private fun saveTop() {
        if (score > top) {
            top = score
            storage.saveTop(top)
        }
    }

    private fun beginLevel() {
        loadLevel(levelIndex)
        state = State.PLAYING
        timer = 0
        timeUp = false
        playLevelMusic()
    }

    private fun groundRow(tx: Int): Int {
        for (r in 14 downTo 2) if (level.solidAt(tx, r) && !level.solidAt(tx, r - 1) && !level.solidAt(tx, r - 2)) return r
        return 13
    }

    private fun loadLevel(index: Int) {
        level = Levels.create(index)
        enemies.clear()
        items.clear()
        fireballs.clear()
        effects.clear()
        lifts.clear()
        level.lifts.forEach { lifts += Lift(it) }
        var sx = level.startX
        var feet = (level.startY + 1) * TILE
        if (checkpoint && level.checkpointX > 0) {
            sx = level.checkpointX
            feet = groundRow(sx) * TILE
        }
        player = Player(sx * 16f + 2, 0f)
        player.setPower(savedPower)
        player.y = feet - player.h.toFloat()
        val zone = level.zoneAt(sx)
        camX = (sx * TILE - 40).coerceIn(zone.x0 * TILE, maxOf(zone.x0 * TILE, zone.x1 * TILE - SCREEN_W))
        if (!checkpoint) camX = zone.x0 * TILE
        time = level.time
        timeTick = 0
        hurry = false
        hurryPending = false
        starMusic = false
        flagY = 40f
        castleFlag = 0
        fireworks = 0
        axeTaken = false
        bossFallPlayed = false
        bridgeIdx = level.bridgeX1
        rescueStep = 0
    }

    private fun currentSong(): Song {
        val zone = level.zoneAt(ftile(player.cx))
        return if (zone.theme == Theme.UNDERGROUND) Song.UNDERGROUND else level.music
    }

    private fun playLevelMusic() {
        starMusic = false
        sound.music(currentSong(), hurry)
    }

    /** Test helper: start [index] directly with the player at tile column [tx]. */
    internal fun debugLoad(index: Int, tx: Int = -1, power: Int = 0) {
        levelIndex = index
        savedPower = power
        checkpoint = false
        loadLevel(index)
        if (tx >= 0) {
            val zone = level.zoneAt(tx)
            player.x = tx * 16f + 2
            player.y = groundRow(tx) * 16f - player.h
            camX = (tx * TILE - 40).coerceIn(zone.x0 * TILE, maxOf(zone.x0 * TILE, zone.x1 * TILE - SCREEN_W))
        }
        state = State.PLAYING
        paused = false
    }

    /** Test helper: move the camera and spawn what would be visible there. */
    internal fun debugCamera(x: Int) {
        camX = x
        for (s in level.spawns) s.spawned = false
        enemies.clear()
        val keep = player.x
        player.x = x + 120f
        spawnEnemies()
        player.x = keep
    }

    // ------------------------------------------------------------------ update

    fun update(input: Buttons) {
        cur.copyFrom(input)
        val aPressed = cur.jump && !prev.jump
        val bPressed = cur.run && !prev.run
        val startPressed = cur.start && !prev.start
        prev.copyFrom(cur)

        if (startPressed && state != State.TITLE) {
            togglePause()
            return
        }
        if (paused) {
            if (aPressed) togglePause()
            return
        }
        frame++
        player.renderFrame = frame

        when (state) {
            State.TITLE -> {
                timer++
                if (aPressed || startPressed) newGame()
            }
            State.INTRO -> if (++timer > (if (timeUp) 280 else 160)) beginLevel()
            State.PLAYING -> updatePlaying(aPressed, bPressed)
            State.DYING -> updateDying()
            State.FLAG -> updateFlag()
            State.COUNT -> updateCount()
            State.FIREWORKS -> updateFireworks()
            State.LEVEL_DONE -> {
                updateEffects()
                if (++timer > 60 && (sound.musicDone || timer > 400)) {
                    savedPower = player.power
                    levelIndex = (levelIndex + 1) % Levels.COUNT
                    checkpoint = false
                    toIntro()
                }
            }
            State.AXE -> updateAxe()
            State.RESCUE -> updateRescue(aPressed)
            State.GAME_OVER -> if (++timer > 360 || (timer > 90 && aPressed)) toTitle()
        }
    }

    private fun updatePlaying(a: Boolean, b: Boolean) {
        val p = player
        when (p.mode) {
            PMode.GROW, PMode.SHRINK, PMode.FIRE_UP -> {
                if (++p.modeTimer >= 60) {
                    if (p.mode == PMode.SHRINK) p.invuln = 120
                    p.mode = PMode.NORMAL
                    p.modeTimer = 0
                }
                return
            }
            PMode.PIPE_DOWN -> {
                p.y += 0.6f
                if (++p.modeTimer >= 54) doWarp()
                return
            }
            PMode.PIPE_RIGHT -> {
                p.x += 0.6f
                if (++p.modeTimer >= 40) doWarp()
                return
            }
            PMode.PIPE_UP -> {
                p.y -= 0.6f
                val w = p.warp
                if (w == null || p.bottom <= w.destY * TILE) {
                    if (w != null) p.y = w.destY * TILE - p.h.toFloat()
                    p.mode = PMode.NORMAL
                    p.vy = 0f
                }
                return
            }
            else -> {}
        }

        // timer
        if (++timeTick >= 24) {
            timeTick = 0
            if (time > 0) time--
            if (time == 100 && !hurry) {
                hurry = true
                hurryPending = true
                sound.music(Song.HURRY)
            }
            if (time == 0) {
                timeUp = true
                killPlayer()
                return
            }
        }
        if (hurryPending && sound.musicDone) {
            hurryPending = false
            if (starMusic) sound.music(Song.STAR, false) else playLevelMusic()
        }

        level.update()

        // lifts move first and carry their rider
        for (l in lifts) l.update(this)
        lifts.removeAll { it.dead }
        p.riding?.let { r ->
            if (!r.dead && abs(r.dy) < 16f) {
                p.moveX(level, r.dx)
                p.y += r.dy
            }
        }

        p.prevBottom = p.bottom
        p.update(this, cur, a, b)
        landOnLifts()
        collectCoins()
        checkPipes()
        if (p.mode != PMode.NORMAL) return

        spawnEnemies()
        for (e in enemies) e.update(this)
        for (it in items) it.update(this)
        for (f in fireballs) f.update(this)
        updateEffects()
        interact()
        enemies.removeAll { it.dead || (!it.alwaysActive && (it.x + it.w < camX - 64)) }
        items.removeAll { it.dead }
        fireballs.removeAll { it.dead }

        updateCamera()
        if (level.checkpointX > 0 && ftile(p.x) >= level.checkpointX) checkpoint = true

        if (starMusic && p.star == 0) playLevelMusic()

        if (p.y > SCREEN_H + 8) {
            killPlayer()
            return
        }
        if (level.flagX >= 0 && p.x + p.w >= level.flagX * TILE + 6) {
            grabFlag()
            return
        }
        if (level.axeX >= 0 && !axeTaken && p.overlaps(level.axeX * 16f, level.axeY * 16f, 16f, 16f)) touchAxe()
    }

    private fun updateEffects() {
        for (e in effects) e.update(this)
        effects.removeAll { it.dead }
    }

    private fun landOnLifts() {
        val p = player
        p.riding = null
        for (l in lifts) l.ridden = false
        if (p.vy < 0f) return
        for (l in lifts) {
            if (p.x + p.w > l.x && p.x < l.x + l.w && p.bottom >= l.y && p.prevBottom <= l.y + 4f + abs(l.dy)) {
                p.landOn(l.y)
                p.riding = l
                l.ridden = true
                return
            }
        }
    }

    private fun collectCoins() {
        val p = player
        for (ty in ftile(p.y)..ftile(p.bottom - 0.01f)) for (tx in ftile(p.x)..ftile(p.x + p.w - 0.01f)) {
            if (level.get(tx, ty) == Tile.COIN) {
                level.set(tx, ty, Tile.EMPTY)
                addCoin()
                sound.play(Sfx.COIN)
            }
        }
    }

    private fun checkPipes() {
        val p = player
        if (!p.onGround || p.mode != PMode.NORMAL) return
        for (w in level.warps) {
            val enter = when (w.kind) {
                WarpKind.PIPE_DOWN -> cur.down && abs(p.bottom - w.ty * TILE) < 1f &&
                    p.x >= w.tx * TILE + 2 && p.x + p.w <= w.tx * TILE + 30
                WarpKind.PIPE_RIGHT -> cur.right && abs(p.x + p.w - w.tx * TILE) < 1.5f &&
                    abs(p.bottom - (w.ty + 2) * TILE) < 1f
            }
            if (enter) {
                p.mode = if (w.kind == WarpKind.PIPE_DOWN) PMode.PIPE_DOWN else PMode.PIPE_RIGHT
                p.modeTimer = 0
                p.warp = w
                p.vx = 0f
                p.vy = 0f
                p.ducking = false
                sound.play(Sfx.PIPE)
                return
            }
        }
    }

    private fun doWarp() {
        val p = player
        val w = p.warp ?: return
        enemies.clear()
        items.clear()
        fireballs.clear()
        p.riding = null
        when (w.exit) {
            ExitKind.DROP -> {
                p.x = w.destX * 16f + 2
                p.y = w.destY * 16f
                p.mode = PMode.NORMAL
            }
            ExitKind.PIPE_UP -> {
                p.x = w.destX * 16f + 10
                p.y = w.destY * 16f
                p.mode = PMode.PIPE_UP
                sound.play(Sfx.PIPE)
            }
        }
        p.facing = 1
        val zone = level.zoneAt(w.destX)
        camX = (p.x.toInt() - 40).coerceIn(zone.x0 * TILE, maxOf(zone.x0 * TILE, zone.x1 * TILE - SCREEN_W))
        if (!starMusic) playLevelMusic()
    }

    private fun spawnEnemies() {
        val zone = level.zoneAt(ftile(player.cx))
        val right = camX + SCREEN_W + 32
        for (s in level.spawns) {
            if (s.spawned || s.tx < zone.x0 || s.tx >= zone.x1) continue
            if (s.tx * TILE > right) continue
            s.spawned = true
            if (s.tx * TILE < camX - 32) continue
            val pal = when (zone.theme) { Theme.OVERWORLD -> 0; Theme.UNDERGROUND -> 1; Theme.CASTLE -> 2 }
            enemies += when (s.kind) {
                EnemyKind.WALKER -> Walker(s.tx, s.ty, pal)
                EnemyKind.TURTLE -> Turtle(s.tx, s.ty, red = false, winged = false)
                EnemyKind.RED_TURTLE -> Turtle(s.tx, s.ty, red = true, winged = false)
                EnemyKind.FLY_TURTLE -> Turtle(s.tx, s.ty, red = false, winged = true)
                EnemyKind.BITER -> Biter(s.tx, s.ty)
                EnemyKind.PODOBOO -> Podoboo(s.tx, s.param)
                EnemyKind.FIRE_BAR -> FireBar(s.tx, s.ty, s.param)
                EnemyKind.BOSS -> Boss(s.tx, s.ty, (level.bridgeX0 + 3) * 16f, (level.bridgeX1 - 1) * 16f)
            }
        }
    }

    private fun updateCamera() {
        val zone = level.zoneAt(ftile(player.cx))
        val target = player.x.toInt() - 112
        if (target > camX) camX = target
        camX = camX.coerceIn(zone.x0 * TILE, maxOf(zone.x0 * TILE, zone.x1 * TILE - SCREEN_W))
    }

    // ------------------------------------------------------------------ interactions

    private val stompPoints = intArrayOf(100, 200, 400, 500, 800, 1000, 2000, 4000, 5000, 8000)
    private val shellPoints = intArrayOf(500, 800, 1000, 2000, 4000, 5000, 8000)

    private fun interact() {
        val p = player
        // player vs enemies
        for (e in enemies) {
            if (p.mode != PMode.NORMAL) break
            if (e.dead || e.knocked || e.inert) continue
            if (!e.touchesPlayer(p)) continue
            if (p.star > 0) {
                if (!e.starproof) {
                    e.knockOut(if (e.cx < p.cx) -1 else 1)
                    sound.play(Sfx.KICK)
                    award(stompPoints, p.stompCombo++, e.x, e.y)
                }
                continue
            }
            val stomp = e.stompable && p.vy > 0f && p.prevBottom <= e.y + 6f
            if (stomp) {
                val counts = !(e is Turtle && e.state == Turtle.S.SHELL)
                e.stomped(this)
                p.y = e.y - p.h
                p.bounce(cur.jump)
                if (counts) award(stompPoints, p.stompCombo++, e.x, e.y)
            } else {
                e.touched(this)
            }
        }
        // fireballs vs enemies
        for (f in fireballs) {
            if (f.dead) continue
            for (e in enemies) {
                if (e.dead || e.knocked || e.inert || e.fireproof) continue
                if (!f.overlaps(e)) continue
                if (e.fireHit(this, if (f.vx > 0) 1 else -1)) {
                    f.explode(this)
                    break
                }
            }
        }
        // sliding shells vs enemies
        for (s in enemies) {
            if (s !is Turtle || s.state != Turtle.S.SLIDE || s.knocked || s.dead) continue
            for (e in enemies) {
                if (e === s || e.dead || e.knocked || e.inert || e.starproof || !s.overlaps(e)) continue
                e.knockOut(if (s.vx > 0) 1 else -1)
                sound.play(Sfx.KICK)
                award(shellPoints, s.combo++, e.x, e.y)
                if (e is Turtle && e.state == Turtle.S.SLIDE) {
                    s.knockOut(if (s.vx > 0) -1 else 1)
                    break
                }
            }
        }
        // enemies turn around when they bump into each other
        for (i in enemies.indices) {
            val a = enemies[i]
            if (!a.bumpsOthers || a.knocked || a.inert || (a is Turtle && a.state == Turtle.S.SLIDE)) continue
            for (j in i + 1 until enemies.size) {
                val b = enemies[j]
                if (!b.bumpsOthers || b.knocked || b.inert || (b is Turtle && b.state == Turtle.S.SLIDE)) continue
                if (!a.overlaps(b)) continue
                if (a.cx < b.cx) { a.vx = -abs(a.vx); b.vx = abs(b.vx) } else { a.vx = abs(a.vx); b.vx = -abs(b.vx) }
            }
        }
        // items
        for (it in items) {
            if (it.dead || it.emerge > 0 || !p.overlaps(it)) continue
            it.dead = true
            collect(it)
        }
    }

    private fun award(table: IntArray, idx: Int, x: Float, y: Float) {
        if (idx >= table.size) oneUp(x, y) else addScore(table[idx], x, y)
    }

    private fun collect(it: PowerUp) {
        val p = player
        when (it.kind) {
            ItemKind.MUSHROOM -> {
                addScore(1000, it.x, it.y)
                sound.play(Sfx.POWER_UP)
                if (p.power == 0) grow()
            }
            ItemKind.FLOWER -> {
                addScore(1000, it.x, it.y)
                sound.play(Sfx.POWER_UP)
                when (p.power) {
                    0 -> grow()
                    1 -> { p.setPower(2); p.mode = PMode.FIRE_UP; p.modeTimer = 0 }
                }
            }
            ItemKind.STAR -> {
                addScore(1000, it.x, it.y)
                p.star = 720
                starMusic = true
                sound.music(Song.STAR)
            }
            ItemKind.ONE_UP -> oneUp(it.x, it.y)
        }
    }

    private fun grow() {
        player.setPower(1)
        player.mode = PMode.GROW
        player.modeTimer = 0
    }

    fun hurtPlayer() {
        val p = player
        if (p.invuln > 0 || p.star > 0 || p.mode != PMode.NORMAL) return
        if (p.power > 0) {
            p.setPower(0)
            p.mode = PMode.SHRINK
            p.modeTimer = 0
            sound.play(Sfx.PIPE)
        } else {
            killPlayer()
        }
    }

    private fun killPlayer() {
        player.die()
        player.riding = null
        savedPower = 0
        state = State.DYING
        timer = 0
        sound.music(Song.DEATH)
    }

    private fun updateDying() {
        player.updateDead()
        timer++
        if (timer > 150 && (sound.musicDone || timer > 400)) {
            lives--
            if (lives <= 0) {
                state = State.GAME_OVER
                timer = 0
                saveTop()
                sound.music(Song.GAME_OVER)
            } else {
                toIntro()
            }
        }
    }

    fun addScore(n: Int, x: Float, y: Float) {
        score += n
        effects += ScorePopup(x, y, n.toString())
    }

    private fun oneUp(x: Float, y: Float) {
        lives++
        sound.play(Sfx.ONE_UP)
        effects += ScorePopup(x, y, "1UP")
    }

    fun addPopup(x: Float, y: Float, text: String) {
        effects += ScorePopup(x, y, text)
    }

    private fun addCoin() {
        coins++
        score += 200
        if (coins >= 100) {
            coins -= 100
            lives++
            sound.play(Sfx.ONE_UP)
        }
    }

    // ------------------------------------------------------------------ blocks

    fun throwFireball(p: Player): Boolean {
        if (fireballs.size >= 2) return false
        val fx = if (p.facing > 0) p.x + p.w else p.x - 8
        fireballs += Fireball(fx, p.y + 4, p.facing)
        sound.play(Sfx.FIREBALL)
        return true
    }

    fun hitBlock(tx: Int, ty: Int, p: Player) {
        val t = level.get(tx, ty)
        val c = level.contentAt(tx, ty)
        val idx = level.idx(tx, ty)
        when {
            t == Tile.QUESTION || t == Tile.HIDDEN -> {
                level.set(tx, ty, Tile.USED)
                startBump(tx, ty)
                release(c, tx, ty)
            }
            t == Tile.BRICK && c == Content.MULTI_COIN -> {
                startBump(tx, ty)
                release(Content.COIN, tx, ty)
                val left = (level.multiCoinLeft[idx] ?: 1) - 1
                level.multiCoinLeft[idx] = left
                if (left <= 0) level.set(tx, ty, Tile.USED)
            }
            t == Tile.BRICK && c != Content.NONE -> {
                level.set(tx, ty, Tile.USED)
                startBump(tx, ty)
                release(c, tx, ty)
            }
            t == Tile.BRICK -> {
                if (p.big) {
                    breakBrick(tx, ty)
                } else {
                    startBump(tx, ty)
                    sound.play(Sfx.BUMP)
                }
            }
            else -> sound.play(Sfx.BUMP)
        }
        bumpAbove(tx, ty)
    }

    private fun startBump(tx: Int, ty: Int) {
        level.bumpX = tx
        level.bumpY = ty
        level.bumpTimer = 10
    }

    private fun release(c: Content, tx: Int, ty: Int) {
        val bx = tx * 16f
        val by = ty * 16f
        when (c) {
            Content.COIN, Content.MULTI_COIN -> {
                effects += CoinPop(bx, by - 16)
                addCoin()
                sound.play(Sfx.COIN)
            }
            Content.POWER -> {
                items += PowerUp(bx, by, if (player.power == 0) ItemKind.MUSHROOM else ItemKind.FLOWER)
                sound.play(Sfx.POWER_APPEAR)
            }
            Content.STAR -> {
                items += PowerUp(bx, by, ItemKind.STAR)
                sound.play(Sfx.POWER_APPEAR)
            }
            Content.ONE_UP -> {
                items += PowerUp(bx, by, ItemKind.ONE_UP)
                sound.play(Sfx.POWER_APPEAR)
            }
            Content.NONE -> sound.play(Sfx.BUMP)
        }
    }

    private fun breakBrick(tx: Int, ty: Int) {
        level.set(tx, ty, Tile.EMPTY)
        level.contents.remove(level.idx(tx, ty))
        val img = Sprites.tileSets.getValue(level.zoneAt(tx).theme).debris
        val bx = tx * 16f
        val by = ty * 16f
        effects += Debris(bx, by, -1f, -5f, img)
        effects += Debris(bx + 8, by, 1f, -5f, img)
        effects += Debris(bx, by + 8, -1f, -3f, img)
        effects += Debris(bx + 8, by + 8, 1f, -3f, img)
        score += 50
        sound.play(Sfx.BREAK)
    }

    private fun bumpAbove(tx: Int, ty: Int) {
        val ax = tx * 16f
        val ay = (ty - 1) * 16f
        for (e in enemies) {
            if (e.dead || e.knocked || e.inert || e.starproof || e is Biter) continue
            if (e.overlaps(ax, ay, 16f, 16f)) {
                e.knockOut(if (e.cx < ax + 8) -1 else 1)
                sound.play(Sfx.KICK)
                addScore(100, e.x, e.y)
            }
        }
        for (it in items) if (it.overlaps(ax, ay, 16f, 16f)) it.hop(if (it.cx < ax + 8) -1 else 1)
        if (level.get(tx, ty - 1) == Tile.COIN) {
            level.set(tx, ty - 1, Tile.EMPTY)
            effects += CoinPop(ax, ay - 16)
            addCoin()
            sound.play(Sfx.COIN)
        }
    }

    // ------------------------------------------------------------------ end of level

    private fun grabFlag() {
        val p = player
        val poleX = level.flagX * 16f + 8
        val pts = when {
            p.bottom < 64 -> 5000
            p.bottom < 104 -> 2000
            p.bottom < 144 -> 800
            p.bottom < 176 -> 400
            else -> 100
        }
        addScore(pts, poleX + 4, p.y)
        p.mode = PMode.FLAG_SLIDE
        p.vx = 0f
        p.vy = 0f
        p.star = 0
        p.ducking = false
        p.x = poleX - p.w + 1
        p.facing = 1
        p.riding = null
        sound.stopMusic()
        sound.play(Sfx.FLAGPOLE)
        state = State.FLAG
        timer = 0
        val d = time % 10
        fireworks = if (d == 1 || d == 3 || d == 6) d else 0
    }

    private fun updateFlag() {
        val p = player
        val base = 12 * 16f
        val poleX = level.flagX * 16f + 8
        if (flagY < base - 16) flagY = minOf(base - 16, flagY + 2f)
        when (p.mode) {
            PMode.FLAG_SLIDE -> {
                if (p.bottom < base) {
                    p.y = minOf(base - p.h, p.y + 2f)
                } else if (flagY >= base - 16) {
                    p.mode = PMode.FLAG_WAIT
                    p.modeTimer = 0
                    p.facing = -1
                    p.x = poleX + 1
                }
            }
            PMode.FLAG_WAIT -> if (++p.modeTimer > 25) {
                p.mode = PMode.AUTO_WALK
                sound.music(Song.CLEAR)
            }
            PMode.AUTO_WALK -> {
                p.autoWalk(this)
                if (p.cx >= level.castleX * TILE + 40) {
                    p.mode = PMode.HIDDEN
                    state = State.COUNT
                    timer = 0
                }
            }
            else -> {}
        }
        updateEffects()
        updateCamera()
    }

    private fun updateCount() {
        updateEffects()
        if (time > 0) {
            val step = minOf(time, 2)
            time -= step
            score += 50 * step
            if (frame % 4 == 0L) sound.play(Sfx.BEEP)
        } else {
            if (castleFlag < 16) castleFlag++ else {
                state = if (fireworks > 0) State.FIREWORKS else State.LEVEL_DONE
                timer = 0
                fireworkTimer = 0
            }
        }
    }

    private fun updateFireworks() {
        updateEffects()
        if (fireworks > 0 && ++fireworkTimer >= 36) {
            fireworkTimer = 0
            fireworks--
            val cx = level.castleX * 16f + 40
            val fx = cx - 48 + ((frame * 37) % 96)
            val fy = 40f + ((frame * 23) % 60)
            effects += Puff(fx, fy, big = true)
            sound.play(Sfx.FIREWORK)
            score += 500
        }
        if (fireworks == 0 && effects.none { it is Puff }) {
            state = State.LEVEL_DONE
            timer = 0
        }
    }

    private fun touchAxe() {
        axeTaken = true
        state = State.AXE
        timer = 0
        sound.stopMusic()
        player.vx = 0f
        player.vy = 0f
        player.star = 0
        for (e in enemies) {
            if (e is Boss) e.frozen = true
            if (e is BossFire) e.dead = true
        }
        level.set(level.bridgeX1, level.bridgeY - 1, Tile.EMPTY) // chain snaps
        bridgeIdx = level.bridgeX1
    }

    private fun updateAxe() {
        timer++
        if (timer > 24 && timer % 3 == 0 && bridgeIdx >= level.bridgeX0) {
            level.set(bridgeIdx, level.bridgeY, Tile.EMPTY)
            bridgeIdx--
            sound.play(Sfx.BUMP)
        }
        for (e in enemies) if (e is Boss) {
            e.update(this)
            if (!bossFallPlayed && !e.onGround && e.vy > 1f) {
                bossFallPlayed = true
                sound.play(Sfx.BOSS_FALL)
            }
        }
        enemies.removeAll { it.dead || it !is Boss }
        updateEffects()
        if (bridgeIdx < level.bridgeX0 && enemies.isEmpty() && timer > 90) {
            state = State.RESCUE
            timer = 0
            rescueStep = 0
            player.mode = PMode.AUTO_WALK
            sound.music(Song.CASTLE_CLEAR)
        }
    }

    private val retainerX get() = (level.width - 18) * 16f

    private fun updateRescue(a: Boolean) {
        val p = player
        timer++
        updateEffects()
        when (rescueStep) {
            0 -> {
                p.autoWalk(this)
                updateCamera()
                if (p.x >= retainerX - 28) {
                    p.mode = PMode.NORMAL
                    p.vx = 0f
                    rescueStep = 1
                    timer = 0
                }
            }
            1 -> {
                // time bonus
                if (time > 0) {
                    val step = minOf(time, 2)
                    time -= step
                    score += 50 * step
                    if (frame % 4 == 0L) sound.play(Sfx.BEEP)
                } else if (timer > 60) {
                    rescueStep = 2
                    timer = 0
                    saveTop()
                }
            }
            2 -> if (timer > 240 && a) {
                // a new, harder lap: keep score and lives
                levelIndex = 0
                checkpoint = false
                savedPower = player.power
                toIntro()
            }
        }
    }

    // ------------------------------------------------------------------ rendering

    fun render(s: Screen) {
        when (state) {
            State.INTRO -> { renderIntro(s); return }
            State.GAME_OVER -> { renderGameOver(s); return }
            else -> {}
        }
        val zone = level.zoneAt((camX + 128) / TILE)
        s.clear(zone.bg)
        renderDecorations(s)
        for (e in enemies) if (e.behindTiles) e.render(s, camX)
        for (it in items) if (it.emerge > 0) it.render(s, camX)
        val inPipe = player.mode == PMode.PIPE_DOWN || player.mode == PMode.PIPE_RIGHT || player.mode == PMode.PIPE_UP
        if (inPipe) player.render(s, camX)
        renderTiles(s, zone.theme)
        renderFlag(s)
        if (level.castleLevel) renderCastleProps(s)
        for (l in lifts) l.render(s, camX)
        for (it in items) if (it.emerge == 0) it.render(s, camX)
        for (e in enemies) if (!e.behindTiles) e.render(s, camX)
        if (!inPipe) player.render(s, camX)
        for (f in fireballs) f.render(s, camX)
        for (e in effects) e.render(s, camX)
        renderHud(s, showTime = state != State.TITLE)
        if (state == State.TITLE) renderTitle(s)
        if (state == State.RESCUE) renderRescueText(s)
        if (paused) {
            s.fillRect(88, 100, 80, 24, Colors.BLACK)
            s.textCentered("PAUSE", 108)
        }
    }

    private fun renderDecorations(s: Screen) {
        for (d in level.decorations) {
            val img = when (d.kind) {
                Deco.CLOUD1 -> Sprites.clouds[0]
                Deco.CLOUD2 -> Sprites.clouds[1]
                Deco.CLOUD3 -> Sprites.clouds[2]
                Deco.BUSH1 -> Sprites.bushes[0]
                Deco.BUSH2 -> Sprites.bushes[1]
                Deco.BUSH3 -> Sprites.bushes[2]
                Deco.HILL_BIG -> Sprites.hillBig
                Deco.HILL_SMALL -> Sprites.hillSmall
                Deco.CASTLE -> Sprites.castle
            }
            val x = d.x - camX
            if (x + img.w < 0 || x >= SCREEN_W) continue
            s.draw(img, x, d.y)
            if (d.kind == Deco.CASTLE && castleFlag > 0) {
                val fx = x + 36
                val fy = d.y - castleFlag
                s.fillRect(fx + 6, fy, 1, castleFlag, Colors.BLACK)
                s.draw(Sprites.poleTop, fx + 3, fy - 6)
                s.fillRect(fx - 4, fy + 1, 10, 6, 0xFFE83818.toInt())
            }
        }
    }

    private val qSeq = intArrayOf(0, 0, 0, 1, 2, 1)

    private fun renderTiles(s: Screen, theme: Theme) {
        val ts = Sprites.tileSets.getValue(theme)
        val q = qSeq[((frame / 8) % 6).toInt()]
        val tx0 = camX / TILE
        val tx1 = minOf(level.width - 1, (camX + SCREEN_W) / TILE)
        for (ty in 0 until level.height) for (tx in tx0..tx1) {
            val img = when (level.get(tx, ty)) {
                Tile.GROUND -> ts.ground
                Tile.BRICK -> ts.brick
                Tile.QUESTION -> ts.question[q]
                Tile.USED -> ts.used
                Tile.HARD -> ts.hard
                Tile.PIPE_TL -> Sprites.pipeTopLeft
                Tile.PIPE_TR -> Sprites.pipeTopRight
                Tile.PIPE_L -> Sprites.pipeLeft
                Tile.PIPE_R -> Sprites.pipeRight
                Tile.COIN -> Sprites.coinShimmer[q]
                Tile.TREE_L -> Sprites.treeLeft
                Tile.TREE_M -> Sprites.treeMid
                Tile.TREE_R -> Sprites.treeRight
                Tile.TRUNK -> Sprites.trunk
                Tile.BRIDGE -> Sprites.bridge
                Tile.CHAIN -> Sprites.chain
                Tile.LAVA_TOP -> Sprites.lavaTop
                Tile.LAVA -> Sprites.lava
                Tile.HPIPE_MOUTH_T -> Sprites.hpipeMouthTop
                Tile.HPIPE_MOUTH_B -> Sprites.hpipeMouthBottom
                Tile.HPIPE_T -> Sprites.hpipeTop
                Tile.HPIPE_B -> Sprites.hpipeBottom
                else -> null
            } ?: continue
            s.draw(img, tx * TILE - camX, ty * TILE + level.bumpOffset(tx, ty))
        }
    }

    private fun renderFlag(s: Screen) {
        if (level.flagX < 0) return
        val px = level.flagX * TILE - camX
        if (px < -32 || px > SCREEN_W + 16) return
        s.fillRect(px + 7, 40, 2, 152, 0xFF80D010.toInt())
        s.fillRect(px + 8, 40, 1, 152, 0xFF00A800.toInt())
        s.draw(Sprites.poleTop, px + 4, 32)
        s.draw(Sprites.flag, px + 7 - 16, flagY.toInt())
    }

    private fun renderCastleProps(s: Screen) {
        if (!axeTaken && level.axeX >= 0) {
            s.draw(Sprites.axe[((frame / 10) % 2).toInt()], level.axeX * TILE - camX, level.axeY * TILE)
        }
        val rx = retainerX.toInt() - camX
        if (rx > -16 && rx < SCREEN_W) s.draw(Sprites.retainer, rx, 10 * TILE - 24)
    }

    private fun renderHud(s: Screen, showTime: Boolean) {
        s.text("NGA", 24, 16)
        s.text(score.toString().padStart(6, '0'), 24, 24)
        s.draw(Sprites.hudCoin[qSeq[((frame / 8) % 6).toInt()]], 88, 24)
        s.text("*" + coins.toString().padStart(2, '0'), 96, 24)
        s.text("WORLD", 144, 16)
        s.text(levelName(), 152, 24)
        s.text("TIME", 200, 16)
        if (showTime && state != State.INTRO && state != State.GAME_OVER) s.text(time.toString().padStart(3, '0'), 208, 24)
    }

    private fun levelName() = "1-${levelIndex + 1}"

    private fun renderIntro(s: Screen) {
        s.clear(Colors.BLACK)
        renderHud(s, showTime = false)
        if (timeUp && timer < 120) {
            s.textCentered("TIME UP", 112)
            return
        }
        s.textCentered("WORLD " + levelName(), 80)
        s.draw(Sprites.small[0][Sprites.STAND], 96, 106)
        s.text("* $lives", 128, 112)
    }

    private fun renderGameOver(s: Screen) {
        s.clear(Colors.BLACK)
        renderHud(s, showTime = false)
        s.textCentered("GAME OVER", 112)
    }

    private fun renderTitle(s: Screen) {
        val bx = 24
        val by = 36
        val bw = 208
        val bh = 88
        val main = 0xFFC8580C.toInt()
        val light = 0xFFFCA048.toInt()
        val dark = 0xFF702800.toInt()
        s.fillRect(bx - 1, by - 1, bw + 2, bh + 2, Colors.BLACK)
        s.fillRect(bx, by, bw, bh, main)
        s.fillRect(bx, by, bw, 2, light)
        s.fillRect(bx, by, 2, bh, light)
        s.fillRect(bx, by + bh - 2, bw, 2, dark)
        s.fillRect(bx + bw - 2, by, 2, bh, dark)
        for ((rx, ry) in listOf(bx + 5 to by + 5, bx + bw - 9 to by + 5, bx + 5 to by + bh - 9, bx + bw - 9 to by + bh - 9)) {
            s.fillRect(rx, ry, 4, 4, dark)
            s.fillRect(rx, ry, 2, 2, light)
        }
        val cream = 0xFFFCE0B0.toInt()
        fun logo(text: String, y: Int) {
            val x = (SCREEN_W - text.length * 24) / 2
            s.text(text, x + 2, y + 2, Colors.BLACK, 3)
            s.text(text, x, y, cream, 3)
        }
        logo("SUPA", by + 10)
        logo("NGA BROS", by + 44)
        s.text("(C)2026 NGA STUDIOS", bx + bw - 19 * 8, by + bh + 6, cream)
        s.text("1 PLAYER GAME", 80, 148)
        if ((timer / 16) % 2 == 0) s.text(">", 64, 148)
        s.text("TOP- " + top.toString().padStart(6, '0'), 88, 166)
        if ((timer / 30) % 2 == 0) s.textCentered("TAP TO START", 184)
    }

    private fun renderRescueText(s: Screen) {
        if (rescueStep >= 1) s.textCentered("THANK YOU NGA!", 72)
        if (rescueStep >= 2 && timer > 60) {
            s.textCentered("THE KINGDOM IS SAFE", 96)
            s.textCentered("AGAIN. FOR NOW...", 112)
        }
        if (rescueStep >= 2 && timer > 240 && (timer / 30) % 2 == 0) s.textCentered("PRESS A FOR A NEW QUEST", 144)
    }
}
