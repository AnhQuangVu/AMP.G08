package com.example.ampg08.game;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Vẽ mê cung theo phong cách Neon/Cyberpunk.
 * Paint được khởi tạo MỘT LẦN trong constructor — tránh GC pressure trong game loop.
 */
public class MazeRenderer {

    private final int[][] maze;

    // Paints (khởi tạo 1 lần duy nhất)
    private final Paint wallPaint;
    private final Paint pathPaint;
    private final Paint wallBorderPaint;
    private final Paint goalPaint;
    private final Paint goalGlowPaint;

    // Goal position (set từ ngoài)
    private int goalRow = -1, goalCol = -1;

    public MazeRenderer(int[][] maze) {
        this.maze = maze;

        wallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wallPaint.setColor(0xFF1A1A2E);
        wallPaint.setStyle(Paint.Style.FILL);

        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setColor(0xFF0D0D1A);
        pathPaint.setStyle(Paint.Style.FILL);

        // Viền neon mỏng cho tường — tạo 1 lần
        wallBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wallBorderPaint.setColor(0x5500F5FF);
        wallBorderPaint.setStyle(Paint.Style.STROKE);
        wallBorderPaint.setStrokeWidth(1f);

        // Goal fill
        goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goalPaint.setColor(0xFF00FF88);
        goalPaint.setStyle(Paint.Style.FILL);

        // Goal glow ring
        goalGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goalGlowPaint.setColor(0x5500FF88);
        goalGlowPaint.setStyle(Paint.Style.FILL);
    }

    public void setGoal(int row, int col) {
        this.goalRow = row;
        this.goalCol = col;
    }

    /**
     * Gọi trong game loop — KHÔNG tạo object mới ở đây.
     */
    public void draw(Canvas canvas, float offsetX, float offsetY, float cellSize, long nowMs) {
        if (maze == null) return;

        int rows = maze.length;
        int cols = maze[0].length;

        // Lấy clip bounds để chỉ vẽ ô nằm trong vùng hiển thị
        android.graphics.Rect clip = canvas.getClipBounds();
        int minCol = Math.max(0, (int) ((clip.left   - offsetX) / cellSize) - 1);
        int maxCol = Math.min(cols - 1, (int) ((clip.right  - offsetX) / cellSize) + 1);
        int minRow = Math.max(0, (int) ((clip.top    - offsetY) / cellSize) - 1);
        int maxRow = Math.min(rows - 1, (int) ((clip.bottom - offsetY) / cellSize) + 1);

        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                float left   = offsetX + c * cellSize;
                float top    = offsetY + r * cellSize;
                float right  = left + cellSize;
                float bottom = top  + cellSize;

                if (maze[r][c] == MazeGenerator.WALL) {
                    canvas.drawRect(left, top, right, bottom, wallPaint);
                    canvas.drawRect(left + 0.5f, top + 0.5f,
                            right - 0.5f, bottom - 0.5f, wallBorderPaint);
                } else {
                    canvas.drawRect(left, top, right, bottom, pathPaint);
                }
            }
        }

        // Vẽ goal với hiệu ứng nhấp nháy
        if (goalRow >= 0 && goalCol >= 0) {
            drawGoal(canvas, offsetX, offsetY, cellSize, nowMs);
        }
    }

    private void drawGoal(Canvas canvas, float offsetX, float offsetY, float cellSize, long nowMs) {
        float cx = offsetX + (goalCol + 0.5f) * cellSize;
        float cy = offsetY + (goalRow + 0.5f) * cellSize;
        float radius = cellSize * 0.38f;

        // Nhấp nháy sin wave
        float alpha = 0.5f + 0.5f * (float) Math.sin(nowMs * 0.006);

        goalPaint.setAlpha((int) (alpha * 210));
        canvas.drawCircle(cx, cy, radius, goalPaint);

        goalGlowPaint.setAlpha((int) (alpha * 100));
        canvas.drawCircle(cx, cy, radius * 1.4f, goalGlowPaint);

        // Viền neon
        goalPaint.setAlpha((int) (alpha * 255));
        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(2f);
        canvas.drawCircle(cx, cy, radius, goalPaint);
        goalPaint.setStyle(Paint.Style.FILL);
    }
}