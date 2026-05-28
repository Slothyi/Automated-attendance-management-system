package com.example.attendancepro.activities

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class WavyMeshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var animationTime = 0f

    // Wave Customization Properties
    private val gridRows = 24
    private val gridCols = 45
    private val dotSpacingX = 32f
    private val dotSpacingY = 12f
    private val maxWaveHeight = 70f

    // Breathing configuration parameters
    private val animationSpeed = 0.04f
    private val breathingSpeed = 0.025f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Cyber Cyan Gradient matching your reference image (#00E5FF to #0086FF)
        val gradientShader = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(Color.parseColor("#00E5FF"), Color.parseColor("#0086FF"), Color.parseColor("#00E5FF")),
            null, Shader.TileMode.CLAMP
        )
        meshPaint.shader = gradientShader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        animationTime += animationSpeed

        // Calculate dynamic breathing factors for scale and transparency
        val breathingFactor = sin(animationTime * breathingSpeed)
        val baseAlpha = 0.65f + (breathingFactor * 0.25f) // Fluctuates between 40% and 90% opacity
        val dotRadiusBonus = breathingFactor * 0.8f       // Subtle dot expansion/contraction

        val startX = (width - (gridCols * dotSpacingX)) / 2f
        val startY = height - (gridRows * dotSpacingY) - 20f

        for (row in 0 until gridRows) {
            // Fade out rows closer to the top edge to mimic the fog/glow effect
            val rowRatio = row.toFloat() / gridRows
            val alpha = (255 * baseAlpha * rowRatio).toInt()
            meshPaint.alpha = max(0, min(255, alpha))

            for (col in 0 until gridCols) {
                // Calculate position offsets using intersecting wave equations
                val x = startX + (col * dotSpacingX)

                // Primary undulating wave combined with a cross-cutting modifier
                val wave1 = sin((col * 0.25f) + animationTime)
                val wave2 = cos((row * 0.3f) - (animationTime * 0.6f))

                // Scale perspective height down for the background layers
                val currentWaveHeight = maxWaveHeight * rowRatio
                val yOffset = (wave1 + wave2) * currentWaveHeight

                val y = startY + (row * dotSpacingY) + yOffset

                // Dynamically size dots based on perspective depth and breathing pulses
                val baseRadius = 1.5f + (rowRatio * 2.5f)
                val finalRadius = max(1f, baseRadius + dotRadiusBonus)

                canvas.drawCircle(x, y, finalRadius, meshPaint)
            }
        }

        // Loop the redraw frame loop continuously
        invalidate()
    }
}