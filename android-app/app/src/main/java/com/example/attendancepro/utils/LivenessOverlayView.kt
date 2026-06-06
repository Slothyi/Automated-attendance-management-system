package com.example.attendancepro.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class LivenessOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class LivenessDirection {
        NONE, LEFT, RIGHT, BLINK
    }

    var activeDirection: LivenessDirection = LivenessDirection.NONE
        set(value) {
            field = value
            invalidate()
        }

    // Paint objects
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D9000000") // 85% black
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF") // Semi-transparent white
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
    }

    private val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66BB6A") // Soft green
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        pathEffect = DashPathEffect(floatArrayOf(dpToPx(4f), dpToPx(4f)), 0f)
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.5f)
    }

    private val activeTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66BB6A")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2.5f)
    }

    private val ovalRect = RectF()
    private var cx = 0f
    private var cy = 0f
    private var rx = 0f
    private var ry = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2.3f // Positioned slightly above absolute center to make space for instruction box

        val ovalWidth = w * 0.62f
        val ovalHeight = ovalWidth * 1.35f

        rx = ovalWidth / 2f
        ry = ovalHeight / 2f

        ovalRect.set(cx - rx, cy - ry, cx + rx, cy + ry)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background overlay using software layer for PorterDuff clearing
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 1. Fill entire screen with semi-transparent overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // 2. Clear the oval face area
        canvas.drawOval(ovalRect, eraserPaint)

        canvas.restoreToCount(layerId)

        // 3. Draw crosshairs inside the oval
        // Vertical line (strictly within oval)
        canvas.drawLine(cx, cy - ry, cx, cy + ry, crosshairPaint)
        // Horizontal line (strictly within oval)
        canvas.drawLine(cx - rx, cy, cx + rx, cy, crosshairPaint)

        // 4. Draw static white/gray oval border
        canvas.drawOval(ovalRect, borderPaint)

        // 5. Draw highlighted green border arcs if requested
        if (activeDirection == LivenessDirection.RIGHT) {
            // Draw right arc: angle goes from -75 to 75 degrees
            canvas.drawArc(ovalRect, -75f, 150f, false, activeBorderPaint)
        } else if (activeDirection == LivenessDirection.LEFT) {
            // Draw left arc: angle goes from 105 to 255 degrees
            canvas.drawArc(ovalRect, 105f, 150f, false, activeBorderPaint)
        }

        // 6. Draw circular ring of tick marks around the oval
        val totalTicks = 90
        val gap = dpToPx(8f)
        val tickLen = dpToPx(6f)

        for (i in 0 until totalTicks) {
            val angleDeg = (i * 360f / totalTicks)
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()

            // Calculate start and end points of the tick line
            val startX = cx + (rx + gap) * cosA
            val startY = cy + (ry + gap) * sinA
            val endX = cx + (rx + gap + tickLen) * cosA
            val endY = cy + (ry + gap + tickLen) * sinA

            // Determine if tick should be highlighted in green
            val isRightTick = (angleDeg in 0f..75f || angleDeg in 285f..360f)
            val isLeftTick = (angleDeg in 105f..255f)

            val paint = when {
                activeDirection == LivenessDirection.RIGHT && isRightTick -> activeTickPaint
                activeDirection == LivenessDirection.LEFT && isLeftTick -> activeTickPaint
                else -> tickPaint
            }

            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    fun getOvalRect(): RectF {
        return ovalRect
    }
}
