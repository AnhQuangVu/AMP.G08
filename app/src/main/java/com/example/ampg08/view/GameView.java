package com.example.ampg08.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread gameThread;
    private boolean isRunning = false;
    private SurfaceHolder holder;
    private Paint wallPaint, playerPaint;
    
    // Game Entities (Simplified for now)
    private float playerX = 100, playerY = 100;
    private float playerRadius = 30;
    private float velX = 0, velY = 0;
    
    // Tilt values from Activity
    private float tiltX = 0, tiltY = 0;
    private float sensitivity = 2.0f;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        
        initPaints();
    }

    private void initPaints() {
        wallPaint = new Paint();
        wallPaint.setColor(Color.parseColor("#00F5FF")); // Cyan Neon
        wallPaint.setStrokeWidth(8f);
        wallPaint.setStyle(Paint.Style.STROKE);
        wallPaint.setShadowLayer(10, 0, 0, Color.parseColor("#00F5FF"));

        playerPaint = new Paint();
        playerPaint.setColor(Color.parseColor("#FF6B00")); // Orange Neon
        playerPaint.setAntiAlias(true);
        playerPaint.setShadowLayer(15, 0, 0, Color.parseColor("#FF6B00"));
    }

    public void updateTilt(float x, float y) {
        this.tiltX = x;
        this.tiltY = y;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        isRunning = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            if (!holder.getSurface().isValid()) continue;
            
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                updatePhysics();
                drawGame(canvas);
                holder.unlockCanvasAndPost(canvas);
            }
            
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updatePhysics() {
        // Apply tilt to velocity (Invert X because of sensor orientation)
        velX -= tiltX * sensitivity * 0.1f;
        velY += tiltY * sensitivity * 0.1f;
        
        // Apply friction
        velX *= 0.95f;
        velY *= 0.95f;
        
        playerX += velX;
        playerY += velY;
        
        // Simple screen boundary collision
        if (playerX < playerRadius) { playerX = playerRadius; velX = 0; }
        if (playerX > getWidth() - playerRadius) { playerX = getWidth() - playerRadius; velX = 0; }
        if (playerY < playerRadius) { playerY = playerRadius; velY = 0; }
        if (playerY > getHeight() - playerRadius) { playerY = getHeight() - playerRadius; velY = 0; }
    }

    private void drawGame(Canvas canvas) {
        canvas.drawColor(Color.parseColor("#0A0A0F")); // Background
        
        // Draw some "walls" (Grid)
        for (int i = 0; i < getWidth(); i += 100) {
            canvas.drawLine(i, 0, i, getHeight(), getGridPaint());
        }
        for (int i = 0; i < getHeight(); i += 100) {
            canvas.drawLine(0, i, getWidth(), i, getGridPaint());
        }

        // Draw Player
        canvas.drawCircle(playerX, playerY, playerRadius, playerPaint);
    }
    
    private Paint getGridPaint() {
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setAlpha(20);
        return p;
    }
}