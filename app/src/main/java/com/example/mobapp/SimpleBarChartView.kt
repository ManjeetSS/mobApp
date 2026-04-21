package com.example.mobapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Minimal dependency-free bar chart — one rounded bar per entry with a day label
 * underneath and the value tag above. Designed for 7–14 data points; scales bar
 * heights against the max value in the set.
 */
class SimpleBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Entry(val label: String, val value: Float, val valueLabel: String)

    private var entries: List<Entry> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(11f)
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    var barColor: Int = 0xFF3F51B5.toInt()
        set(value) {
            field = value
            barPaint.color = value
            invalidate()
        }

    var trackColor: Int = 0x22888888
        set(value) {
            field = value
            trackPaint.color = value
            invalidate()
        }

    var labelColor: Int = 0xDD000000.toInt()
        set(value) {
            field = value
            labelPaint.color = value
            valuePaint.color = value
            invalidate()
        }

    init {
        barPaint.color = barColor
        trackPaint.color = trackColor
        labelPaint.color = labelColor
        valuePaint.color = labelColor
    }

    fun setEntries(newEntries: List<Entry>) {
        entries = newEntries
        invalidate()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val defaultHeight = dp(140f).toInt()
        val w = resolveSize(dp(280f).toInt(), widthSpec)
        val h = resolveSize(defaultHeight, heightSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        if (entries.isEmpty()) return
        val padStart = paddingLeft.toFloat()
        val padEnd = paddingRight.toFloat()
        val padTop = paddingTop.toFloat()
        val padBottom = paddingBottom.toFloat()

        val w = width.toFloat()
        val h = height.toFloat()

        val labelArea = sp(14f)
        val valueArea = sp(12f) + dp(2f)
        val chartTop = padTop + valueArea
        val chartBottom = h - padBottom - labelArea
        val chartH = (chartBottom - chartTop).coerceAtLeast(1f)

        val n = entries.size
        val slot = (w - padStart - padEnd) / n
        val barW = (slot * 0.55f).coerceAtMost(dp(28f))
        val radius = dp(4f)

        val maxV = (entries.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)

        entries.forEachIndexed { i, e ->
            val cx = padStart + slot * (i + 0.5f)

            // Track (subtle full-height line so empty days still read as "a day").
            rect.set(cx - barW / 2, chartTop, cx + barW / 2, chartBottom)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)

            // Bar proper.
            if (e.value > 0f) {
                val barH = chartH * (e.value / maxV)
                rect.set(cx - barW / 2, chartBottom - barH, cx + barW / 2, chartBottom)
                canvas.drawRoundRect(rect, radius, radius, barPaint)
                // Value label above the bar.
                val valueY = (chartBottom - barH - dp(3f)).coerceAtLeast(padTop + valuePaint.textSize)
                canvas.drawText(e.valueLabel, cx, valueY, valuePaint)
            }

            // Day label under chart.
            canvas.drawText(e.label, cx, h - padBottom - dp(1f), labelPaint)
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity
}

/** Resolves a theme color attribute to an ARGB int. */
internal fun android.content.Context.resolveThemeColor(attr: Int, fallback: Int = Color.GRAY): Int {
    val arr = obtainStyledAttributes(intArrayOf(attr))
    return try {
        arr.getColor(0, fallback)
    } finally {
        arr.recycle()
    }
}
