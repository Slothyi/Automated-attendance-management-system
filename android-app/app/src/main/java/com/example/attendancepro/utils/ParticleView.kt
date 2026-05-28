package com.example.attendancepro.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val particles = mutableListOf<Particle>()

    private val paint = Paint().apply {
        color = 0x88FFFFFF.toInt()
        isAntiAlias = true
    }

    private var viewWidth = 0
    private var viewHeight = 0

    init {

        // IMPORTANT
        isClickable = false
        isFocusable = false
        isFocusableInTouchMode = false

        // PARTICLE COUNT
        repeat(40) {
            particles.add(Particle())
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        viewWidth = w
        viewHeight = h

        particles.forEach {
            it.reset(true)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        particles.forEach { p ->

            // PARTICLE VISIBILITY
            paint.alpha = p.alpha

            canvas.drawCircle(
                p.x,
                p.y,
                p.radius,
                paint
            )

            // MOVEMENT
            p.y -= p.speed
            p.x += p.drift

            // RESET
            if (p.y < -20f) {
                p.reset(false)
            }
        }

        // CONTINUOUS ANIMATION
        postInvalidateOnAnimation()
    }

    inner class Particle {

        var x = 0f
        var y = 0f

        var radius = 0f

        var speed = 0f
        var drift = 0f

        var alpha = 255

        fun reset(initial: Boolean) {

            x = Random.nextFloat() * viewWidth

            y =
                if (initial)
                    Random.nextFloat() * viewHeight
                else
                    viewHeight.toFloat()

            radius = Random.nextFloat() * 6f + 2f

            // SLOWER CINEMATIC SPEED
            speed = Random.nextFloat() * 0.7f + 0.2f

            // SIDE DRIFT
            drift = Random.nextFloat() * 0.5f - 0.25f

            // RANDOM VISIBILITY
            alpha = Random.nextInt(60, 180)
        }
    }
}