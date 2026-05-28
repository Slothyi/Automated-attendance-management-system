package com.example.attendancepro.activities

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class TechBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#08AEEBFF")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#18C8F1FF")
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22C8F1FF")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private var phase = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // SOFT TOP GLOW
        canvas.drawCircle(
            w * 0.5f,
            h * 0.18f,
            240f,
            glowPaint
        )

        // GRID
        val spacing = 120

        for (x in 0..width step spacing) {
            canvas.drawLine(
                x.toFloat(),
                0f,
                x.toFloat(),
                height.toFloat(),
                gridPaint
            )
        }

        for (y in 0..height step spacing) {
            canvas.drawLine(
                0f,
                y.toFloat(),
                width.toFloat(),
                y.toFloat(),
                gridPaint
            )
        }

        // FLOATING TECH LINES
        val path = Path()

        path.moveTo(0f, h * 0.35f)

        for (x in 0..width step 20) {

            val y = (
                    h * 0.35f +
                            sin((x * 0.01f) + phase) * 18f
                    ).toFloat()

            path.lineTo(x.toFloat(), y)
        }

        canvas.drawPath(path, linePaint)

        // SECOND LINE
        val path2 = Path()

        path2.moveTo(0f, h * 0.55f)

        for (x in 0..width step 20) {

            val y = (
                    h * 0.55f +
                            sin((x * 0.012f) + phase + 1.5f) * 14f
                    ).toFloat()

            path2.lineTo(x.toFloat(), y)
        }

        canvas.drawPath(path2, linePaint)

        // Animation
        phase += 0.03f

        invalidate()
    }
}