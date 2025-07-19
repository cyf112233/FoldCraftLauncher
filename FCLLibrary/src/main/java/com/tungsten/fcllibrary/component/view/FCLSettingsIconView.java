package com.tungsten.fcllibrary.component.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

public class FCLSettingsIconView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float rotate = 0f;
    private ValueAnimator animator;

    public FCLSettingsIconView(Context context) { super(context); init(); }
    public FCLSettingsIconView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public FCLSettingsIconView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.WHITE);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        startAnim();
    }

    private void startAnim() {
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(3000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            rotate = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float r = Math.min(w, h) * 0.35f;
        float cx = w / 2f;
        float cy = h / 2f;
        canvas.save();
        canvas.rotate(rotate, cx, cy);
        // 齿轮外圈
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(5f);
        canvas.drawCircle(cx, cy, r, paint);
        // 齿轮齿
        paint.setStrokeWidth(7f);
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI / 4);
            float x1 = (float) (cx + Math.cos(angle) * (r + 4));
            float y1 = (float) (cy + Math.sin(angle) * (r + 4));
            float x2 = (float) (cx + Math.cos(angle) * (r + 18));
            float y2 = (float) (cy + Math.sin(angle) * (r + 18));
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
        // 齿轮内圈
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(5f);
        canvas.drawCircle(cx, cy, r * 0.6f, paint);
        canvas.restore();
    }
} 