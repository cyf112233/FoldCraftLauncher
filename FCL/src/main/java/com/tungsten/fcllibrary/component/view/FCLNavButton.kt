package com.tungsten.fcllibrary.component.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tungsten.fcllibrary.R
import com.tungsten.fcllibrary.util.ConvertUtils

class FCLNavButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var iconView: ImageView
    private var textView: TextView
    private var backgroundDrawable: GradientDrawable
    
    private var iconResId: Int = 0
    private var text: String = ""
    private var iconColor: Int = Color.GRAY
    private var textColor: Int = Color.GRAY
    private var isSelected: Boolean = false
    
    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        
        // 创建背景
        backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = ConvertUtils.dip2px(context, 12f).toFloat()
            setColor(Color.TRANSPARENT)
        }
        background = backgroundDrawable
        
        // 创建图标
        iconView = ImageView(context).apply {
            layoutParams = LayoutParams(
                ConvertUtils.dip2px(context, 24f),
                ConvertUtils.dip2px(context, 24f)
            ).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        
        // 创建文字
        textView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = ConvertUtils.dip2px(context, 4f)
            }
            textSize = 10f
            gravity = Gravity.CENTER
            maxLines = 1
        }
        
        // 添加子视图
        addView(iconView)
        addView(textView)
        
        // 设置点击效果
        setOnClickListener {
            if (!isSelected) {
                setSelected(true)
            }
        }
        
        // 设置状态选择器
        updateState()
    }
    
    fun setIcon(resId: Int) {
        iconResId = resId
        iconView.setImageResource(resId)
    }
    
    fun setText(text: String) {
        this.text = text
        textView.text = text
    }
    
    fun setIconColor(color: Int) {
        iconColor = color
        updateState()
    }
    
    fun setTextColor(color: Int) {
        textColor = color
        updateState()
    }
    
    override fun setSelected(selected: Boolean) {
        if (isSelected != selected) {
            isSelected = selected
            updateState()
            
            // 播放动画
            if (selected) {
                val scaleAnimation = AnimationUtils.loadAnimation(context, R.anim.nav_button_select)
                startAnimation(scaleAnimation)
            }
        }
    }
    
    override fun isSelected(): Boolean {
        return isSelected
    }
    
    private fun updateState() {
        if (isSelected) {
            // 选中状态
            backgroundDrawable.apply {
                setColor(ContextCompat.getColor(context, R.color.nav_background_selected))
                setStroke(
                    ConvertUtils.dip2px(context, 2f),
                    ContextCompat.getColor(context, R.color.nav_border_selected)
                )
            }
            
            iconView.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.nav_icon_selected)
            )
            textView.setTextColor(
                ContextCompat.getColor(context, R.color.nav_text_selected)
            )
        } else {
            // 未选中状态
            backgroundDrawable.apply {
                setColor(Color.TRANSPARENT)
                setStroke(0, Color.TRANSPARENT)
            }
            
            iconView.imageTintList = ColorStateList.valueOf(iconColor)
            textView.setTextColor(textColor)
        }
    }
    
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.5f
    }
    
    // 设置涟漪效果
    override fun performClick(): Boolean {
        val rippleAnimation = AnimationUtils.loadAnimation(context, R.anim.button_ripple)
        startAnimation(rippleAnimation)
        return super.performClick()
    }
} 