package com.example.ampg08.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.example.ampg08.model.PlayerState;

import java.util.List;

/**
 * Vẽ mê cung và bóng lên Canvas theo phong cách Neon/Cyberpunk.
 */
public class MazeRenderer {

    private final Paint wallPaint;
    private final Paint wallGlowPaint;
    private final Paint pathPaint;
    private final Paint goalPaint;
    private final Paint goalGlowPaint;
    private final Paint playerPaint;
    private final Paint playerGlowPaint;
    private final Paint opponentPaint;
    private final Paint frozenPaint;
    private final Paint boostPaint;
    private final Paint bgPaint;
    private final Paint textPaint;
    private final RectF rectBuffer = new RectF();

    public MazeRenderer() {
        // Nền maze
        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#0D0D1A"));

        // Tường – Neon Cyan
        wallPaint = new Paint();
        wallPaint.setColor(Color.parseColor("#00C8D4"));
        wallPaint.setStyle(Paint.Style.FILL);

        wallGlowPaint = new Paint();
        wallGlowPaint.setColor(Color.parseColor("#4000F5FF"));
        wallGlowPaint.setStyle(Paint.Style.FILL);
        wallGlowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(8f,
                android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Đường đi
        pathPaint = new Paint();
        pathPaint.setColor(Color.parseColor("#0A0A15"));

        // Goal – Neon Green
        goalPaint = new Paint();
        goalPaint.setColor(Color.parseColor("#00FF88"));
        goalGlowPaint = new Paint();
        goalGlowPaint.setColor(Color.parseColor("#8000FF88"));
        goalGlowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(16f,
                android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Player – Neon Orange
        playerPaint = new Paint();
        playerPaint.setColor(Color.parseColor("#FF6B00"));
        playerPaint.setAntiAlias(true);
        playerGlowPaint = new Paint();
        playerGlowPaint.setColor(Color.parseColor("#80FF6B00"));
        playerGlowPaint.setAntiAlias(true);
        playerGlowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(12f,
                android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Opponent – Neon Purple
        opponentPaint = new Paint();
        opponentPaint.setColor(Color.parseColor("#9B59B6"));
        opponentPaint.setAntiAlias(true);

        // Frozen – Neon Blue
        frozenPaint = new Paint();
        frozenPaint.setColor(Color.parseColor("#00BFFF"));
        frozenPaint.setAntiAlias(true);
        frozenPaint.setAlpha(200);

        // Boost aura
        boostPaint = new Paint();
        boostPaint.setColor(Color.parseColor("#FFAA00"));
        boostPaint.setAntiAlias(true);
        boostPaint.setMaskFilter(new android.graphics.BlurMaskFilter(20f,
                android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Text trên bóng
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(20f);
        textPaint.setFakeBoldText(true);
    }

    /**
     * Vẽ toàn bộ maze lên canvas.
     */
    public void drawMaze(Canvas canvas, int[][] maze, float cellSize, float offsetX, float offsetY,
                         int goalRow, int goalCol) {
        int rows = maze.length;
        int cols = maze[0].length;

        // Vẽ nền
        canvas.drawColor(Color.parseColor("#0A0A0F"));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float left   = offsetX + c * cellSize;
                float top    = offsetY + r * cellSize;
                float right  = left + cellSize;
                float bottom = top + cellSize;
                rectBuffer.set(left, top, right, bottom);

                if (maze[r][c] == MazeGenerator.WALL) {
                    canvas.drawRect(rectBuffer, wallPaint);
                } else {
                    canvas.drawRect(rectBuffer, pathPaint);
                }
            }
        }

        // Vẽ Goal (nhấp nháy theo thời gian)
        float pulse = (float) (0.7f + 0.3f * Math.sin(System.currentTimeMillis() * 0.005));
        goalGlowPaint.setAlpha((int) (180 * pulse));
        float gLeft   = offsetX + goalCol * cellSize + 2;
        float gTop    = offsetY + goalRow * cellSize + 2;
        float gRight  = gLeft + cellSize - 4;
        float gBottom = gTop + cellSize - 4;
        rectBuffer.set(gLeft - 4, gTop - 4, gRight + 4, gBottom + 4);
        canvas.drawRoundRect(rectBuffer, 8, 8, goalGlowPaint);
        rectBuffer.set(gLeft, gTop, gRight, gBottom);
        canvas.drawRoundRect(rectBuffer, 6, 6, goalPaint);
    }

    /**
     * Vẽ bóng của bản thân (local player).
     */
    public void drawLocalBall(Canvas canvas, Ball ball) {
        // Boost aura
        if (ball.boosted && System.currentTimeMillis() < ball.boostUntil) {
            canvas.drawCircle(ball.x, ball.y, ball.radius * 2.0f, boostPaint);
        }
        // Glow
        playerGlowPaint.setAlpha(150);
        canvas.drawCircle(ball.x, ball.y, ball.radius + 6, playerGlowPaint);
        // Bóng chính
        canvas.drawCircle(ball.x, ball.y, ball.radius, playerPaint);
    }

    /**
     * Vẽ bóng của các opponent từ Firestore.
     */
    public void drawOpponentBalls(Canvas canvas, List<PlayerState> opponents,
                                  String localUid, float cellSize, float offsetX, float offsetY) {
        if (opponents == null) return;
        for (PlayerState p : opponents) {
            if (p.getUid().equals(localUid)) continue;
            // x, y trong PlayerState là pixel relative game canvas
            float px = p.getX();
            float py = p.getY();

            Paint paint = p.isFrozen() ? frozenPaint : opponentPaint;
            canvas.drawCircle(px, py, cellSize * 0.35f, paint);

            // Tên
            String initials = p.getDisplayName() != null && !p.getDisplayName().isEmpty()
                    ? String.valueOf(p.getDisplayName().charAt(0)).toUpperCase() : "?";
            canvas.drawText(initials, px, py + 7, textPaint);
        }
    }
}
