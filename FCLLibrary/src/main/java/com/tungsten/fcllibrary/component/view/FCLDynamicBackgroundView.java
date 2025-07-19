package com.tungsten.fcllibrary.component.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class FCLDynamicBackgroundView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int width, height;
    private float anim = 0f;
    private Handler handler = new Handler();
    private final int particleCount = 32;
    private float[] px = new float[particleCount];
    private float[] py = new float[particleCount];
    private float[] pvx = new float[particleCount];
    private float[] pvy = new float[particleCount];
    private int[] pcolor = new int[particleCount];
    private Random random = new Random();

    public FCLDynamicBackgroundView(Context context) { super(context); init(); }
    public FCLDynamicBackgroundView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public FCLDynamicBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        for (int i = 0; i < particleCount; i++) {
            px[i] = random.nextFloat();
            py[i] = random.nextFloat();
            pvx[i] = (random.nextFloat() - 0.5f) * 0.002f;
            pvy[i] = (random.nextFloat() - 0.5f) * 0.002f;
            pcolor[i] = Color.HSVToColor(new float[]{random.nextFloat() * 360, 0.7f, 1f});
        }
        startAnim();
    }

    private void startAnim() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                anim += 0.008f;
                updateParticles();
                invalidate();
                handler.postDelayed(this, 16);
            }
        }, 16);
    }

    private void updateParticles() {
        for (int i = 0; i < particleCount; i++) {
            px[i] += pvx[i];
            py[i] += pvy[i];
            if (px[i] < 0 || px[i] > 1) pvx[i] = -pvx[i];
            if (py[i] < 0 || py[i] > 1) pvy[i] = -pvy[i];
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        width = getWidth();
        height = getHeight();
        // 绘制动态渐变背景
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = Math.max(width, height) * 0.8f;
        int[] colors = new int[] {
                Color.HSVToColor(new float[]{(anim*60)%360, 0.5f, 1f}),
                Color.HSVToColor(new float[]{(anim*60+60)%360, 0.5f, 1f}),
                Color.HSVToColor(new float[]{(anim*60+120)%360, 0.5f, 1f}),
                Color.HSVToColor(new float[]{(anim*60+180)%360, 0.5f, 1f})
        };
        float[] pos = new float[] {0f, 0.4f, 0.7f, 1f};
        RadialGradient gradient = new RadialGradient(centerX, centerY, radius, colors, pos, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
        // 绘制粒子
        for (int i = 0; i < particleCount; i++) {
            paint.setColor(pcolor[i]);
            paint.setAlpha(120);
            float x = px[i] * width;
            float y = py[i] * height;
            canvas.drawCircle(x, y, 18 + 10 * (float)Math.sin(anim*2 + i), paint);
        }
    }
} 