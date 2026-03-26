package com.example.ampg08.game;

public class CollisionDetector {

    /**
     * Kiểm tra và giải quyết va chạm giữa ball và toàn bộ tường trong maze.
     *
     * @param ball     Đối tượng bóng
     * @param maze     Ma trận maze (1=tường)
     * @param cellSize Kích thước pixel mỗi ô
     * @param offsetX  Offset X của maze trên canvas
     * @param offsetY  Offset Y của maze trên canvas
     */
    public static void resolve(Ball ball, int[][] maze, float cellSize, float offsetX, float offsetY) {
        int rows = maze.length;
        int cols = maze[0].length;

        // Tính vùng ô mà bóng đang tiếp xúc (mở rộng 1 ô xung quanh)
        int minRow = Math.max(0, (int) ((ball.y - ball.radius - offsetY) / cellSize) - 1);
        int maxRow = Math.min(rows - 1, (int) ((ball.y + ball.radius - offsetY) / cellSize) + 1);
        int minCol = Math.max(0, (int) ((ball.x - ball.radius - offsetX) / cellSize) - 1);
        int maxCol = Math.min(cols - 1, (int) ((ball.x + ball.radius - offsetX) / cellSize) + 1);

        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                if (maze[r][c] == MazeGenerator.WALL) {
                    float wallLeft   = offsetX + c * cellSize;
                    float wallTop    = offsetY + r * cellSize;
                    float wallRight  = wallLeft + cellSize;
                    float wallBottom = wallTop  + cellSize;

                    // Tìm điểm gần nhất trên AABB với tâm bóng
                    float nearestX = clamp(ball.x, wallLeft,  wallRight);
                    float nearestY = clamp(ball.y, wallTop,   wallBottom);

                    float dx = ball.x - nearestX;
                    float dy = ball.y - nearestY;
                    float distSq = dx * dx + dy * dy;

                    if (distSq < ball.radius * ball.radius && distSq > 0) {
                        float dist = (float) Math.sqrt(distSq);
                        float overlap = ball.radius - dist;

                        // Đẩy bóng ra khỏi tường theo hướng pháp tuyến
                        float nx = dx / dist;
                        float ny = dy / dist;
                        ball.x += nx * overlap;
                        ball.y += ny * overlap;

                        // Phản chiếu vận tốc (bật lại, tắt dần)
                        float dot = ball.vx * nx + ball.vy * ny;
                        if (dot < 0) {
                            ball.vx -= 1.4f * dot * nx;
                            ball.vy -= 1.4f * dot * ny;
                            // Giảm năng lượng sau va chạm
                            ball.vx *= 0.75f;
                            ball.vy *= 0.75f;
                        }
                    }
                }
            }
        }
    }

    /**
     * Kiểm tra bóng có chạm vào ô goal (finish) không.
     */
    public static boolean checkGoal(Ball ball, int goalRow, int goalCol,
                                    float cellSize, float offsetX, float offsetY) {
        float goalCenterX = offsetX + goalCol * cellSize + cellSize / 2f;
        float goalCenterY = offsetY + goalRow * cellSize + cellSize / 2f;
        float dx = ball.x - goalCenterX;
        float dy = ball.y - goalCenterY;
        float threshold = cellSize * 0.6f;
        return dx * dx + dy * dy < threshold * threshold;
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
