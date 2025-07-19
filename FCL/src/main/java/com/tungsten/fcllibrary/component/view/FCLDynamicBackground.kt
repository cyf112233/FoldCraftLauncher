package com.tungsten.fcllibrary.component.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import com.tungsten.fcllibrary.component.theme.Theme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FCLDynamicBackground @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentTheme: Theme? = null
    private var particles = mutableListOf<Particle>()
    private var paint = Paint()
    private var backgroundPaint = Paint()
    private var gradientPaint = Paint()
    
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    
    private var rotationAnimation: RotateAnimation? = null
    private var isAnimating = false
    
    // 粒子数量
    private val particleCount = 50
    
    // 动画相关
    private var time = 0f
    private val animationSpeed = 0.02f
    
    init {
        initPaints()
        initParticles()
    }
    
    private fun initPaints() {
        paint.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        backgroundPaint.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        gradientPaint.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }
    
    private fun initParticles() {
        particles.clear()
        repeat(particleCount) {
            particles.add(createRandomParticle())
        }
    }
    
    private fun createRandomParticle(): Particle {
        return Particle(
            x = Random.nextFloat() * 1000f,
            y = Random.nextFloat() * 1000f,
            radius = Random.nextFloat() * 3f + 1f,
            speed = Random.nextFloat() * 2f + 0.5f,
            angle = Random.nextFloat() * 360f,
            alpha = Random.nextFloat() * 0.5f + 0.3f,
            color = getRandomColor()
        )
    }
    
    private fun getRandomColor(): Int {
        val colors = listOf(
            Color.parseColor("#FF6B6B"), // 红色
            Color.parseColor("#4ECDC4"), // 青色
            Color.parseColor("#45B7D1"), // 蓝色
            Color.parseColor("#96CEB4"), // 绿色
            Color.parseColor("#FFEAA7"), // 黄色
            Color.parseColor("#DDA0DD"), // 紫色
            Color.parseColor("#FFB6C1")  // 粉色
        )
        return colors[Random.nextInt(colors.size)]
    }
    
    fun setTheme(theme: Theme) {
        currentTheme = theme
        updateBackgroundGradient()
        invalidate()
    }
    
    private fun updateBackgroundGradient() {
        val theme = currentTheme ?: return
        
        val colors = if (theme.isDarkTheme()) {
            intArrayOf(
                Color.parseColor("#1a1a2e"),
                Color.parseColor("#16213e"),
                Color.parseColor("#0f3460")
            )
        } else {
            intArrayOf(
                Color.parseColor("#f8f9fa"),
                Color.parseColor("#e9ecef"),
                Color.parseColor("#dee2e6")
            )
        }
        
        gradientPaint.shader = LinearGradient(
            0f, 0f,
            width.toFloat(), height.toFloat(),
            colors,
            null,
            Shader.TileMode.CLAMP
        )
    }
    
    fun startAnimation() {
        if (isAnimating) return
        
        isAnimating = true
        
        // 创建旋转动画
        rotationAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 30000 // 30秒一圈
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        
        startAnimation(rotationAnimation)
        
        // 启动粒子动画
        post(object : Runnable {
            override fun run() {
                if (isAnimating) {
                    updateParticles()
                    invalidate()
                    postDelayed(this, 16) // 60 FPS
                }
            }
        })
    }
    
    fun stopAnimation() {
        isAnimating = false
        rotationAnimation?.cancel()
        clearAnimation()
    }
    
    private fun updateParticles() {
        time += animationSpeed
        
        particles.forEach { particle ->
            // 更新粒子位置
            particle.x += cos(Math.toRadians(particle.angle.toDouble())).toFloat() * particle.speed
            particle.y += sin(Math.toRadians(particle.angle.toDouble())).toFloat() * particle.speed
            
            // 边界检查
            if (particle.x < -particle.radius) particle.x = width + particle.radius
            if (particle.x > width + particle.radius) particle.x = -particle.radius
            if (particle.y < -particle.radius) particle.y = height + particle.radius
            if (particle.y > height + particle.radius) particle.y = -particle.radius
            
            // 添加一些随机性
            particle.angle += Random.nextFloat() * 2f - 1f
            particle.alpha = (0.3f + 0.4f * sin(time + particle.x * 0.01f)).coerceIn(0.1f, 0.8f)
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = minOf(w, h) / 2f
        
        updateBackgroundGradient()
        
        // 重新初始化粒子位置
        particles.forEach { particle ->
            particle.x = Random.nextFloat() * w
            particle.y = Random.nextFloat() * h
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制背景渐变
        if (gradientPaint.shader != null) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
        }
        
        // 绘制动态圆形
        drawDynamicCircles(canvas)
        
        // 绘制粒子
        drawParticles(canvas)
        
        // 绘制连接线
        drawConnectionLines(canvas)
    }
    
    private fun drawDynamicCircles(canvas: Canvas) {
        val theme = currentTheme ?: return
        
        val circleCount = 3
        val baseRadius = radius * 0.3f
        
        for (i in 0 until circleCount) {
            val angle = (time * 20 + i * 120) * Math.PI / 180
            val x = centerX + cos(angle).toFloat() * baseRadius * 0.5f
            val y = centerY + sin(angle).toFloat() * baseRadius * 0.5f
            val circleRadius = baseRadius * (0.5f + 0.3f * sin(time * 2 + i).toFloat())
            
            paint.apply {
                color = if (theme.isDarkTheme()) {
                    Color.argb(30, 255, 255, 255)
                } else {
                    Color.argb(20, 0, 0, 0)
                }
                style = Paint.Style.FILL
            }
            
            canvas.drawCircle(x, y, circleRadius, paint)
            
            // 绘制边框
            paint.apply {
                color = if (theme.isDarkTheme()) {
                    Color.argb(50, 255, 255, 255)
                } else {
                    Color.argb(30, 0, 0, 0)
                }
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            
            canvas.drawCircle(x, y, circleRadius, paint)
        }
    }
    
    private fun drawParticles(canvas: Canvas) {
        particles.forEach { particle ->
            paint.apply {
                color = particle.color
                alpha = (particle.alpha * 255).toInt()
                style = Paint.Style.FILL
            }
            
            canvas.drawCircle(particle.x, particle.y, particle.radius, paint)
            
            // 绘制光晕效果
            paint.apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                alpha = (particle.alpha * 100).toInt()
            }
            
            canvas.drawCircle(particle.x, particle.y, particle.radius * 2, paint)
        }
    }
    
    private fun drawConnectionLines(canvas: Canvas) {
        val theme = currentTheme ?: return
        val maxDistance = 150f
        
        paint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = if (theme.isDarkTheme()) {
                Color.argb(30, 255, 255, 255)
            } else {
                Color.argb(20, 0, 0, 0)
            }
        }
        
        for (i in particles.indices) {
            for (j in i + 1 until particles.size) {
                val p1 = particles[i]
                val p2 = particles[j]
                
                val distance = kotlin.math.sqrt(
                    (p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)
                )
                
                if (distance < maxDistance) {
                    val alpha = ((maxDistance - distance) / maxDistance * 100).toInt()
                    paint.alpha = alpha
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                }
            }
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
    
    data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val speed: Float,
        var angle: Float,
        var alpha: Float,
        val color: Int
    )
} 