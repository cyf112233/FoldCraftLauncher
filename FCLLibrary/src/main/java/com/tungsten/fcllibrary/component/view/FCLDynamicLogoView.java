package com.tungsten.fcllibrary.component.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

public class FCLDynamicLogoView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float animValue = 0f;
    private ValueAnimator animator;

    public FCLDynamicLogoView(Context context) { super(context); init(); }
    public FCLDynamicLogoView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public FCLDynamicLogoView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.CYAN);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void startAnimation() {
        if (animator != null && animator.isRunning()) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            animValue = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float r = Math.min(w, h) * 0.4f;
        float cx = w / 2f;
        float cy = h / 2f;
        // 动态圆环
        paint.setColor(Color.CYAN);
        paint.setStrokeWidth(8f);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(Color.MAGENTA);
        paint.setStrokeWidth(4f);
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, 360f * animValue, 120f, false, paint);
        // 中心F字母
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(10f);
        canvas.drawLine(cx - r/2, cy - r/2, cx + r/2, cy - r/2, paint);
        canvas.drawLine(cx - r/2, cy - r/2, cx - r/2, cy + r/2, paint);
        canvas.drawLine(cx - r/2, cy, cx + r/4, cy, paint);
    }
} 