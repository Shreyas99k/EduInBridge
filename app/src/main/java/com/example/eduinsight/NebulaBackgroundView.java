package com.example.eduinsight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NebulaBackgroundView extends View {
    private Paint cloudPaint, bgPaint;
    private List<Cloud> clouds;
    private final int CLOUD_COUNT = 6;
    private Random random = new Random();

    public NebulaBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clouds = new ArrayList<>();
    }

    private void initClouds(int w, int h) {
        if (!clouds.isEmpty()) return;
        // Nebula 1: Top Right (Cyan/Blue)
        clouds.add(new Cloud(w * 0.8f, h * 0.2f, 800, "#1A38BDF8")); 
        // Nebula 2: Bottom Left (Indigo/Purple)
        clouds.add(new Cloud(w * 0.2f, h * 0.8f, 1000, "#1A818CF8"));
        // Nebula 3: Center (Darker Blue)
        clouds.add(new Cloud(w * 0.5f, h * 0.5f, 1200, "#122DD4BF"));
        
        // Random drift clouds
        for (int i = 0; i < 3; i++) {
            clouds.add(new Cloud(w, h));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Deep Space Gradient Background
        LinearGradient bgGradient = new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#0F172A"), Color.parseColor("#1E293B")},
                null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bgGradient);
        canvas.drawRect(0, 0, w, h, bgPaint);

        // 2. Render Nebula Clouds
        initClouds(w, h);
        for (Cloud cloud : clouds) {
            cloud.update(w, h);
            
            RadialGradient gradient = new RadialGradient(
                cloud.x, cloud.y, cloud.radius,
                new int[]{Color.parseColor(cloud.hexColor), Color.TRANSPARENT},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
            );
            cloudPaint.setShader(gradient);
            canvas.drawCircle(cloud.x, cloud.y, cloud.radius, cloudPaint);
        }
        
        // Use a lower frame rate for background to save battery
        postInvalidateDelayed(30); 
    }

    private class Cloud {
        float x, y, radius, speedX, speedY;
        String hexColor;

        // For fixed initial clouds
        Cloud(float x, float y, float radius, String color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.hexColor = color;
            this.speedX = (random.nextFloat() - 0.5f) * 0.2f;
            this.speedY = (random.nextFloat() - 0.5f) * 0.2f;
        }

        // For random drift clouds
        Cloud(int w, int h) {
            radius = 600 + random.nextFloat() * 600;
            x = random.nextFloat() * w;
            y = random.nextFloat() * h;
            speedX = (random.nextFloat() - 0.5f) * 0.4f;
            speedY = (random.nextFloat() - 0.5f) * 0.4f;
            hexColor = random.nextBoolean() ? "#1538BDF8" : "#15818CF8";
        }

        void update(int w, int h) {
            x += speedX;
            y += speedY;
            
            // Soft boundary bouncing
            if (x < -radius) speedX = Math.abs(speedX);
            if (x > w + radius) speedX = -Math.abs(speedX);
            if (y < -radius) speedY = Math.abs(speedY);
            if (y > h + radius) speedY = -Math.abs(speedY);
        }
    }
}
