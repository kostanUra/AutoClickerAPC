package com.example.autoclicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.min

class FloatingButtonView(context: Context) : View(context) {

    var isClicking = false
        set(value) { field = value; invalidate() }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF")
        style = Paint.Style.FILL
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    override fun onDraw(canvas: Canvas) {
        val r = min(width, height) / 2f - 10f
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, r, fill)
        ring.color = if (isClicking) Color.parseColor("#FF4CAF50")
                     else Color.parseColor("#FF2196F3")
        canvas.drawCircle(cx, cy, r, ring)
    }
}
