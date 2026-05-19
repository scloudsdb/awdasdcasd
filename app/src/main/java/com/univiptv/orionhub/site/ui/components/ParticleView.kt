package com.univiptv.orionhub.site.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var alpha: Int,
        var speedX: Float,
        var speedY: Float,
        var color: Int
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
    }

    private val particleColors = intArrayOf(
        0xFF6750A4.toInt(),
        0xFF7C6BBF.toInt(),
        0xFF9B8ED8.toInt(),
        0xFF625B71.toInt(),
        0xFFCCC2DC.toInt()
    )

    private var animator: ValueAnimator? = null
    private val connectionDistance = 150f
    private val particleCount = 40

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initParticles()
        startAnimation()
    }

    private fun initParticles() {
        particles.clear()
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        repeat(particleCount) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    radius = Random.nextFloat() * 3f + 1.5f,
                    alpha = Random.nextInt(60, 180),
                    speedX = (Random.nextFloat() - 0.5f) * 1.2f,
                    speedY = (Random.nextFloat() - 0.5f) * 1.2f,
                    color = particleColors[Random.nextInt(particleColors.size)]
                )
            )
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    private fun updateParticles() {
        val w = width.toFloat()
        val h = height.toFloat()
        for (p in particles) {
            p.x += p.speedX
            p.y += p.speedY

            if (p.x < 0) { p.x = 0f; p.speedX *= -1 }
            if (p.x > w) { p.x = w; p.speedX *= -1 }
            if (p.y < 0) { p.y = 0f; p.speedY *= -1 }
            if (p.y > h) { p.y = h; p.speedY *= -1 }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (i in particles.indices) {
            for (j in i + 1 until particles.size) {
                val p1 = particles[i]
                val p2 = particles[j]
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist < connectionDistance) {
                    val lineAlpha = ((1f - dist / connectionDistance) * 40).toInt()
                    linePaint.color = p1.color
                    linePaint.alpha = lineAlpha
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
                }
            }
        }

        for (p in particles) {
            paint.color = p.color
            paint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.radius, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
