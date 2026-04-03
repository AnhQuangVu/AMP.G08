package com.example.ampg08.game;

public class CollisionDetector {

    private final int[][] maze;
    private float offsetX, offsetY, cellSize;

    public CollisionDetector(int[][] maze) {
        this.maze = maze;
    }

    public void setLayout(float offsetX, float offsetY, float cellSize) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.cellSize = cellSize;
    }

    public void resolve(Ball ball) {
        if (maze == null || cellSize == 0) return;

        int rows = maze.length;
        int cols = maze[0].length;

        // Tính grid position của ball
        float localX = ball.x - offsetX;
        float localY = ball.y - offsetY;

        // Kiểm tra các ô xung quanh ball
        int minCol = Math.max(0, (int) ((localX - ball.radius) / cellSize));
        int maxCol = Math.min(cols - 1, (int) ((localX + ball.radius) / cellSize));
        int minRow = Math.max(0, (int) ((localY - ball.radius) / cellSize));
        int maxRow = Math.min(rows - 1, (int) ((localY + ball.radius) / cellSize));

        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                if (maze[r][c] == MazeGenerator.WALL) {
                    resolveWallCollision(ball, r, c);
                }
            }
        }

        // Boundary check - giữ ball trong maze
        float minX = offsetX + ball.radius;
        float maxX = offsetX + cols * cellSize - ball.radius;
        float minY = offsetY + ball.radius;
        float maxY = offsetY + rows * cellSize - ball.radius;

        if (ball.x < minX) { ball.x = minX; ball.vx = 0; }
        if (ball.x > maxX) { ball.x = maxX; ball.vx = 0; }
        if (ball.y < minY) { ball.y = minY; ball.vy = 0; }
        if (ball.y > maxY) { ball.y = maxY; ball.vy = 0; }
    }

    private void resolveWallCollision(Ball ball, int row, int col) {
        // Wall bounds
        float wallLeft = offsetX + col * cellSize;
        float wallTop = offsetY + row * cellSize;
        float wallRight = wallLeft + cellSize;
        float wallBottom = wallTop + cellSize;

        // Tìm điểm gần nhất trên wall đến center của ball
        float nearestX = clamp(ball.x, wallLeft, wallRight);
        float nearestY = clamp(ball.y, wallTop, wallBottom);

        // Khoảng cách từ ball center đến điểm gần nhất
        float dx = ball.x - nearestX;
        float dy = ball.y - nearestY;
        float distSq = dx * dx + dy * dy;
        float radiusSq = ball.radius * ball.radius;

        if (distSq < radiusSq && distSq > 0) {
            // Có va chạm - đẩy ball ra
            float dist = (float) Math.sqrt(distSq);
            float overlap = ball.radius - dist;

            // Normalize direction
            float nx = dx / dist;
            float ny = dy / dist;

            // Push ball out
            ball.x += nx * overlap;
            ball.y += ny * overlap;

            // Reflect velocity
            float dot = ball.vx * nx + ball.vy * ny;
            if (dot < 0) {
                ball.vx -= 2 * dot * nx * 0.5f; // 0.5 = bounce damping
                ball.vy -= 2 * dot * ny * 0.5f;
            }
        } else if (distSq == 0) {
            // Ball center inside wall - push to nearest edge
            float overlapLeft = ball.x - wallLeft;
            float overlapRight = wallRight - ball.x;
            float overlapTop = ball.y - wallTop;
            float overlapBottom = wallBottom - ball.y;

            float minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
                    Math.min(overlapTop, overlapBottom));

            if (minOverlap == overlapLeft) {
                ball.x = wallLeft - ball.radius;
                ball.vx = Math.min(0, ball.vx);
            } else if (minOverlap == overlapRight) {
                ball.x = wallRight + ball.radius;
                ball.vx = Math.max(0, ball.vx);
            } else if (minOverlap == overlapTop) {
                ball.y = wallTop - ball.radius;
                ball.vy = Math.min(0, ball.vy);
            } else {
                ball.y = wallBottom + ball.radius;
                ball.vy = Math.max(0, ball.vy);
            }
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}