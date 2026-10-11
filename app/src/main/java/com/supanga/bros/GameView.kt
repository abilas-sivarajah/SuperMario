package com.supanga.bros

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.supanga.bros.core.Buttons
import com.supanga.bros.core.Game
import com.supanga.bros.core.SCREEN_H
import com.supanga.bros.core.SCREEN_W
import com.supanga.bros.core.Screen
import com.supanga.bros.core.Synth

/** Hosts the game loop: fixed 60 Hz updates, renders the 256x240 frame scaled to the display. */
@SuppressLint("ViewConstructor")
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private val synth = Synth(SAMPLE_RATE)
    private val audio = AndroidAudio(synth, SAMPLE_RATE)
    private val game = Game(synth, PrefsStorage(context))
    private val screen = Screen()
    private val frame = Bitmap.createBitmap(SCREEN_W, SCREEN_H, Bitmap.Config.ARGB_8888)
    private val touch = TouchControls()
    private val keys = Buttons()
    private val stick = Buttons()
    private val input = Buttons()
    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val dst = Rect()
    private val gameRect = RectF()

    @Volatile private var running = false
    @Volatile private var surfaceReady = false
    @Volatile private var backPause = false
    @Volatile private var gamepadActive = false
    private var thread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val scale = minOf(height / SCREEN_H.toFloat(), width / SCREEN_W.toFloat())
        val gw = SCREEN_W * scale
        val gh = SCREEN_H * scale
        val left = (width - gw) / 2f
        val top = (height - gh) / 2f
        gameRect.set(left, top, left + gw, top + gh)
        dst.set(left.toInt(), top.toInt(), (left + gw).toInt(), (top + gh).toInt())
        touch.layout(width, height, gameRect)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        stopLoop()
    }

    fun resume() {
        audio.start()
        startLoop()
    }

    fun pause() {
        backPause = true
        stopLoop()
        audio.stop()
    }

    /** Back button: pauses the game while playing. Returns false to let the activity close. */
    fun onBack(): Boolean {
        if (game.state == Game.State.TITLE || game.paused) return false
        backPause = true
        return true
    }

    private fun startLoop() {
        if (running || !surfaceReady) return
        running = true
        thread = Thread(this, "game").also { it.start() }
    }

    private fun stopLoop() {
        running = false
        thread?.join(1000)
        thread = null
    }

    override fun run() {
        val step = 1_000_000_000L / 60
        var last = System.nanoTime()
        var acc = 0L
        while (running) {
            val now = System.nanoTime()
            acc += now - last
            last = now
            if (acc > step * 4) acc = step * 4
            var updated = false
            while (acc >= step) {
                gatherInput()
                game.update(input)
                acc -= step
                updated = true
            }
            if (!updated) {
                Thread.sleep(1)
                continue
            }
            game.render(screen)
            frame.setPixels(screen.pixels, 0, SCREEN_W, 0, 0, SCREEN_W, SCREEN_H)
            present()
        }
    }

    private fun present() {
        // The surface can disappear at any moment (app sent to background); never let that kill the thread.
        val canvas = try {
            holder.lockHardwareCanvas()
        } catch (e: Exception) {
            try { holder.lockCanvas() } catch (e2: Exception) { null }
        } ?: return
        try {
            drawFrame(canvas)
        } finally {
            try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
        }
    }

    private fun gatherInput() {
        touch.menuMode = game.state == Game.State.TITLE || game.state == Game.State.GAME_OVER ||
            game.state == Game.State.RESCUE || game.paused
        input.left = touch.left || keys.left || stick.left
        input.right = touch.right || keys.right || stick.right
        input.up = touch.up || keys.up || stick.up
        input.down = touch.down || keys.down || stick.down
        input.jump = touch.a || keys.jump
        input.run = touch.b || keys.run
        input.start = keys.start
        // pause requests come from the UI thread; apply them here on the game thread
        if (touch.pauseRequested) {
            touch.pauseRequested = false
            game.togglePause()
        }
        if (backPause) {
            backPause = false
            game.pause()
        }
    }

    private fun drawFrame(c: Canvas) {
        c.drawColor(Color.BLACK)
        c.drawBitmap(frame, null, dst, paint)
        val hideTouch = gamepadActive && System.currentTimeMillis() - touch.lastUsed > 3000
        if (!hideTouch) touch.draw(c, showPauseOnly = touch.menuMode)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = touch.onTouch(event)

    fun onKey(keyCode: Int, down: Boolean): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> keys.left = down
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> keys.right = down
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> keys.up = down
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> keys.down = down
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_DPAD_CENTER -> keys.jump = down
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> keys.run = down
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_P -> keys.start = down
            else -> return false
        }
        gamepadActive = true
        return true
    }

    fun onJoystick(ev: MotionEvent): Boolean {
        if (ev.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK || ev.action != MotionEvent.ACTION_MOVE) return false
        val x = ev.getAxisValue(MotionEvent.AXIS_X)
        val y = ev.getAxisValue(MotionEvent.AXIS_Y)
        val hx = ev.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hy = ev.getAxisValue(MotionEvent.AXIS_HAT_Y)
        stick.left = x < -0.5f || hx < -0.5f
        stick.right = x > 0.5f || hx > 0.5f
        stick.up = y < -0.6f || hy < -0.5f
        stick.down = y > 0.6f || hy > 0.5f
        gamepadActive = true
        return true
    }

    companion object {
        private const val SAMPLE_RATE = 22050
    }
}
