package com.example.attendancepro.utils

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class CyberParticlesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val particles = mutableListOf<Particle>()

    private val random = Random.Default

    private var initialized = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        style = Paint.Style.FILL

        // Glow effect
        maskFilter = BlurMaskFilter(
            10f,
            BlurMaskFilter.Blur.NORMAL
        )
    }

    data class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speed: Float,
        var alpha: Int,
        var upward: Boolean,
        var drift: Float
    )

    private fun initializeParticles() {

        particles.clear()

        // MUCH MORE PARTICLES
        repeat(140) {

            val upward = it % 2 == 0

            particles.add(
                Particle(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height,

                    // BIGGER PARTICLES
                    radius = random.nextFloat() * 6f + 3f,

                    // Faster movement
                    speed = random.nextFloat() * 1.2f + 1f,

                    // More visible
                    alpha = random.nextInt(80, 180),

                    upward = upward,

                    drift = random.nextFloat() * 10f
                )
            )
        }

        initialized = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!initialized && width > 0 && height > 0) {
            initializeParticles()
        }

        particles.forEach { particle ->

            // Brighter futuristic colors
            paint.color = if (particle.upward) {

                Color.parseColor("#A8F1FF")

            } else {

                Color.parseColor("#5EDFFF")
            }

            paint.alpha = particle.alpha

            // Draw particle
            canvas.drawCircle(
                particle.x,
                particle.y,
                particle.radius,
                paint
            )

            // Floating horizontal drift
            particle.x += sin(
                (particle.y * 0.01f) + particle.drift
            ) * 0.22f

            // UPWARD FLOW
            if (particle.upward) {

                particle.y -= particle.speed

                if (particle.y < -30f) {

                    particle.y = height + 30f
                    particle.x = random.nextFloat() * width
                }

            } else {

                // DOWNWARD FLOW
                particle.y += particle.speed

                if (particle.y > height + 30f) {

                    particle.y = -30f
                    particle.x = random.nextFloat() * width
                }
            }
        }

        // Smooth animation
        postInvalidateOnAnimation()
    }
}