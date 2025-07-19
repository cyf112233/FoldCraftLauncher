package com.tungsten.fcllibrary.component.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.animation.ValueAnimator;
import android.view.animation.OvershootInterpolator;

public class FCLStartButtonView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float ripple = 0f;
    private ValueAnimator animator;
    private boolean pressed = false;
    private OnClickListener clickListener;

    public FCLStartButtonView(Context context) { super(context); init(); }
    public FCLStartButtonView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public FCLStartButtonView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setClickable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float r = Math.min(w, h) / 2f;
        // 渐变背景
        LinearGradient gradient = new LinearGradient(0, 0, w, h, Color.CYAN, Color.MAGENTA, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL);
        RectF rect = new RectF(0, 0, w, h);
        canvas.drawRoundRect(rect, h/2f, h/2f, paint);
        paint.setShader(null);
        // 涟漪动画
        if (ripple > 0f) {
            paint.setColor(Color.argb((int)(120 * (1 - ripple)), 255,255,255));
            canvas.drawCircle(w/2f, h/2f, r * ripple, paint);
        }
        // 文字
        paint.setColor(Color.WHITE);
        paint.setTextSize(h * 0.4f);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textY = h/2f - (fm.ascent + fm.descent)/2;
        canvas.drawText("启动游戏", w/2f, textY, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                startRipple();
                return true;
            case MotionEvent.ACTION_UP:
                if (pressed && clickListener != null) clickListener.onClick(this);
                pressed = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void startRipple() {
        if (animator != null && animator.isRunning()) animator.cancel();
        ripple = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(400);
        animator.setInterpolator(new OvershootInterpolator());
        animator.addUpdateListener(a -> {
            ripple = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setOnClickListener(OnClickListener l) {
        this.clickListener = l;
    }
} 