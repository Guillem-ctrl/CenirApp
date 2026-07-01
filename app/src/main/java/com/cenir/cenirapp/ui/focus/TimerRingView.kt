package com.cenir.cenirapp.ui.focus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.cenir.cenirapp.R

class TimerRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // 0f = vacío, 1f = lleno
    var progress: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
    }
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Colores (se resuelven aquí para respetar el tema claro/oscuro)
        trackPaint.color = ContextCompat.getColor(context, R.color.chalk)
        progressPaint.color = ContextCompat.getColor(context, R.color.amber)

        val stroke = trackPaint.strokeWidth
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - stroke

        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Pista de fondo (círculo completo)
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)

        // Arco de progreso — empieza arriba (-90°) y barre en sentido horario
        val sweep = 360f * progress
        canvas.drawArc(oval, -90f, sweep, false, progressPaint)
    }
}
