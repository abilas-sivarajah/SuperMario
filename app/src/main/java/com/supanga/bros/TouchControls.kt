package com.supanga.bros

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot

/**
 * On-screen controls: a d-pad on the left, B (run / fire) and A (jump) on the right, pause at the top.
 * Placed in the black bars next to the 256x240 picture when there is room, otherwise drawn on top of it.
 */
class TouchControls {
    @Volatile var left = false
    @Volatile var right = false
    @Volatile var up = false
    @Volatile var down = false
    @Volatile var a = false
    @Volatile var b = false
    @Volatile var pauseRequested = false
    /** Any touch counts as A (title screen, game over, ...). */
    @Volatile var menuMode = false
    @Volatile var lastUsed = 0L

    private var width = 1
    private var height = 1
    private var unit = 1f
    private var dpadX = 0f
    private var dpadY = 0f
    private var ax = 0f
    private var ay = 0f
    private var bx = 0f
    private var by = 0f
    private var radius = 1f
    private val pauseRect = RectF()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val arrow = Path()

    fun layout(w: Int, h: Int, gameRect: RectF) {
        width = w
        height = h
        unit = h / 10f
        val side = gameRect.left
        val roomy = side >= unit * 3.4f
        dpadX = if (roomy) side / 2f else unit * 2.4f
        dpadY = h - unit * 2.8f
        val rcx = if (roomy) w - side / 2f else w - unit * 2.4f
        radius = unit * 0.95f
        ax = rcx + unit * 1.05f
        ay = dpadY - unit * 0.55f
        bx = rcx - unit * 1.05f
        by = dpadY + unit * 0.55f
        if (!roomy) {
            ax = minOf(ax, w - radius - unit * 0.2f)
        }
        val ps = unit * 1.1f
        pauseRect.set(unit * 0.3f, unit * 0.3f, unit * 0.3f + ps, unit * 0.3f + ps)
        stroke.strokeWidth = unit * 0.06f
        label.textSize = unit * 0.7f
    }

    fun onTouch(ev: MotionEvent): Boolean {
        lastUsed = System.currentTimeMillis()
        val action = ev.actionMasked
        val upIndex = if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) ev.actionIndex else -1
        var l = false; var r = false; var u = false; var d = false; var pa = false; var pb = false
        for (i in 0 until ev.pointerCount) {
            if (i == upIndex || action == MotionEvent.ACTION_CANCEL) continue
            val x = ev.getX(i)
            val y = ev.getY(i)
            if (pauseRect.contains(x, y)) {
                if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) && ev.actionIndex == i) pauseRequested = true
                continue
            }
            if (menuMode) { pa = true; continue }
            if (x < width / 2f) {
                val dx = x - dpadX
                val dy = y - dpadY
                if (dx < -unit * 0.35f) l = true
                if (dx > unit * 0.35f) r = true
                if (dy > unit * 0.85f && abs(dx) < unit * 1.6f) d = true
                if (dy < -unit * 0.85f && abs(dx) < unit * 1.6f) u = true
            } else {
                val da = hypot(x - ax, y - ay)
                val db = hypot(x - bx, y - by)
                if (da < radius * 1.7f && db < radius * 1.7f && abs(da - db) < unit * 0.45f) {
                    pa = true; pb = true
                } else if (da <= db) pa = true else pb = true
            }
        }
        left = l; right = r; up = u; down = d; a = pa; b = pb
        return true
    }

    fun draw(c: Canvas, showPauseOnly: Boolean) {
        drawPause(c)
        if (showPauseOnly) return
        // d-pad
        val s = unit * 0.95f
        fill.color = 0x40FFFFFF
        stroke.color = 0x90FFFFFF.toInt()
        c.drawCircle(dpadX, dpadY, unit * 1.9f, fill)
        c.drawCircle(dpadX, dpadY, unit * 1.9f, stroke)
        fill.color = if (left) 0xC0FFFFFF.toInt() else 0x80FFFFFF.toInt()
        drawArrow(c, dpadX - s * 1.2f, dpadY, -1f, 0f)
        fill.color = if (right) 0xC0FFFFFF.toInt() else 0x80FFFFFF.toInt()
        drawArrow(c, dpadX + s * 1.2f, dpadY, 1f, 0f)
        fill.color = if (up) 0xC0FFFFFF.toInt() else 0x60FFFFFF
        drawArrow(c, dpadX, dpadY - s * 1.2f, 0f, -1f)
        fill.color = if (down) 0xC0FFFFFF.toInt() else 0x80FFFFFF.toInt()
        drawArrow(c, dpadX, dpadY + s * 1.2f, 0f, 1f)
        // buttons
        drawButton(c, bx, by, "B", b)
        drawButton(c, ax, ay, "A", a)
    }

    private fun drawPause(c: Canvas) {
        fill.color = 0x50FFFFFF
        c.drawRoundRect(pauseRect, unit * 0.2f, unit * 0.2f, fill)
        fill.color = 0xC0FFFFFF.toInt()
        val w = pauseRect.width()
        val bw = w * 0.15f
        c.drawRect(pauseRect.left + w * 0.3f, pauseRect.top + w * 0.25f, pauseRect.left + w * 0.3f + bw, pauseRect.bottom - w * 0.25f, fill)
        c.drawRect(pauseRect.right - w * 0.3f - bw, pauseRect.top + w * 0.25f, pauseRect.right - w * 0.3f, pauseRect.bottom - w * 0.25f, fill)
    }

    private fun drawArrow(c: Canvas, x: Float, y: Float, dx: Float, dy: Float) {
        val len = unit * 0.45f
        val wid = unit * 0.4f
        arrow.reset()
        arrow.moveTo(x + dx * len, y + dy * len)
        arrow.lineTo(x - dx * len * 0.6f - dy * wid, y - dy * len * 0.6f + dx * wid)
        arrow.lineTo(x - dx * len * 0.6f + dy * wid, y - dy * len * 0.6f - dx * wid)
        arrow.close()
        c.drawPath(arrow, fill)
    }

    private fun drawButton(c: Canvas, x: Float, y: Float, text: String, pressed: Boolean) {
        fill.color = if (pressed) 0xE0E83818.toInt() else 0x90C83010.toInt()
        c.drawCircle(x, y, radius, fill)
        stroke.color = 0xB0FFFFFF.toInt()
        c.drawCircle(x, y, radius, stroke)
        label.color = 0xE0FFFFFF.toInt()
        c.drawText(text, x, y + label.textSize * 0.36f, label)
    }
}
