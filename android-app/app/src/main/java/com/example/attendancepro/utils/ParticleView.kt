package com.example.attendancepro.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.random.Random

class ParticleView(context: Context) : View(context) {

    private val particles = mutableListOf<Particle>()

    private val paint = Paint().apply {
        color = 0x88FFFFFF.toInt()   // 🔥 bright white (visible)
        isAntiAlias = true
    }

    private var viewWidth = 0
    private var viewHeight = 0

    init {
        repeat(25) {   // more particles = visible
            particles.add(Particle())
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        viewWidth = w
        viewHeight = h

        particles.forEach { it.reset(true) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        particles.forEach { p ->
            canvas.drawCircle(p.x, p.y, p.radius, paint)

            p.y -= p.speed
            p.x += p.drift

            if (p.y < 0) {
                p.reset(false)
            }
        }

        invalidate()
    }

    inner class Particle {
        var x = 0f
        var y = 0f
        var radius = 0f
        var speed = 0f
        var drift = 0f

        fun reset(initial: Boolean) {
            x = Random.nextFloat() * viewWidth
            y = if (initial) Random.nextFloat() * viewHeight else viewHeight.toFloat()

            radius = Random.nextFloat() * 5f + 2f
            speed = Random.nextFloat() * 0.8f + 0.3f
            drift = Random.nextFloat() * 0.6f - 0.3f
        }
    }
}