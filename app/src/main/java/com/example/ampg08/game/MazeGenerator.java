package com.example.ampg08.game;

import java.util.Random;
import java.util.Stack;

/**
 * Sinh mê cung bằng thuật toán Recursive DFS (Iterative stack version).
 * Maze lưu dạng int[rows][cols]:
 *   1 = tường
 *   0 = đường đi
 * Kích thước phải là lẻ để DFS hoạt động đúng.
 */
public class MazeGenerator {

    public static final int WALL = 1;
    public static final int PATH = 0;

    private final int rows;
    private final int cols;
    private final int[][] maze;
    private final Random random;

    // Vị trí start và goal
    private int startRow, startCol;
    private int goalRow, goalCol;

    public MazeGenerator(int rows, int cols, long seed) {
        // Đảm bảo kích thước lẻ
        this.rows = (rows % 2 == 0) ? rows + 1 : rows;
        this.cols = (cols % 2 == 0) ? cols + 1 : cols;
        this.maze = new int[this.rows][this.cols];
        this.random = new Random(seed);
    }

    public int[][] generate() {
        // Điền toàn bộ là tường
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                maze[r][c] = WALL;

        // DFS từ (1,1)
        startRow = 1;
        startCol = 1;
        maze[startRow][startCol] = PATH;

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{startRow, startCol});

        int[] dr = {-2, 2, 0, 0};
        int[] dc = {0, 0, -2, 2};

        while (!stack.isEmpty()) {
            int[] curr = stack.peek();
            int r = curr[0];
            int c = curr[1];

            // Tìm các hàng xóm chưa thăm (chưa có đường)
            java.util.List<Integer> neighbors = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];
                if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && maze[nr][nc] == WALL) {
                    neighbors.add(i);
                }
            }

            if (!neighbors.isEmpty()) {
                int dir = neighbors.get(random.nextInt(neighbors.size()));
                int nr = r + dr[dir];
                int nc = c + dc[dir];
                // Đục tường ở giữa
                maze[r + dr[dir] / 2][c + dc[dir] / 2] = PATH;
                maze[nr][nc] = PATH;
                stack.push(new int[]{nr, nc});
            } else {
                stack.pop();
            }
        }

        // Đặt goal ở góc dưới phải (gần nhất là ô đường)
        goalRow = rows - 2;
        goalCol = cols - 2;
        maze[goalRow][goalCol] = PATH;

        return maze;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }

    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }

    public int getGoalRow() { return goalRow; }
    public int getGoalCol() { return goalCol; }
}
