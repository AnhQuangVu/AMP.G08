package com.example.ampg08.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

public class MazeRenderer {

    private final int[][] maze;
    private final Paint wallPaint;
    private final Paint pathPaint;
    private final Paint gridPaint;

    public MazeRenderer(int[][] maze) {
        this.maze = maze;

        // Wall paint - Neon blue gradient
        wallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wallPaint.setColor(0xFF1A1A2E);
        wallPaint.setStyle(Paint.Style.FILL);

        // Path paint - Dark background
        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setColor(0xFF0D0D1A);
        pathPaint.setStyle(Paint.Style.FILL);

        // Grid lines - Subtle neon
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0x3300F5FF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
    }

    public void draw(Canvas canvas, float offsetX, float offsetY, float cellSize) {
        if (maze == null) return;

        int rows = maze.length;
        int cols = maze[0].length;

        // Draw cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float left = offsetX + c * cellSize;
                float top = offsetY + r * cellSize;
                float right = left + cellSize;
                float bottom = top + cellSize;

                if (maze[r][c] == MazeGenerator.WALL) {
                    // Wall cell
                    canvas.drawRect(left, top, right, bottom, wallPaint);

                    // Wall glow effect
                    Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    glowPaint.setStyle(Paint.Style.STROKE);
                    glowPaint.setStrokeWidth(2f);
                    glowPaint.setColor(0x4400F5FF);
                    canvas.drawRect(left + 1, top + 1, right - 1, bottom - 1, glowPaint);
                } else {
                    // Path cell
                    canvas.drawRect(left, top, right, bottom, pathPaint);
                }
            }
        }

        // Draw grid lines for visual effect
        for (int r = 0; r <= rows; r++) {
            float y = offsetY + r * cellSize;
            canvas.drawLine(offsetX, y, offsetX + cols * cellSize, y, gridPaint);
        }
        for (int c = 0; c <= cols; c++) {
            float x = offsetX + c * cellSize;
            canvas.drawLine(x, offsetY, x, offsetY + rows * cellSize, gridPaint);
        }
    }
}