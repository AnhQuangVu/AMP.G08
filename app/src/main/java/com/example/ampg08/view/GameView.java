package com.example.ampg08.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.game.Ball;
import com.example.ampg08.game.CollisionDetector;
import com.example.ampg08.game.MazeGenerator;
import com.example.ampg08.game.MazeRenderer;
import com.example.ampg08.model.PlayerState;

import java.util.List;

/**
 * GameView nâng cấp hoàn chỉnh:
 * - SurfaceView + Thread riêng ~60fps
 * - Mê cung DFS, vật lý AABB
 * - Sync vị trí lên Firestore realtime
 * - Vẽ opponent từ Firestore
 * - Skill boost (từ proximity) + freeze opponent
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // ── Game Loop ───────────────────────────────────────────────────────
    private Thread gameThread;
    private volatile boolean isRunning = false;
    private static final long TARGET_FPS = 60;
    private static final long FRAME_TIME_MS = 1000L / TARGET_FPS;

    // ── Maze ────────────────────────────────────────────────────────────
    private int[][] maze;
    private MazeGenerator mazeGenerator;
    private MazeRenderer renderer;
    private float cellSize;
    private float offsetX, offsetY;
    private int goalRow, goalCol;

    // ── Physics ─────────────────────────────────────────────────────────
    private Ball localBall;
    private volatile float tiltX = 0, tiltY = 0;
    private long gameStartTime;
    private boolean gameFinished = false;

    // ── Firebase Sync ───────────────────────────────────────────────────
    private String roomId;
    private String localUid;
    private String localDisplayName;
    private List<PlayerState> opponents;
    private com.google.firebase.firestore.ListenerRegistration playersListener;
    private static final long SYNC_INTERVAL_MS = 100; // sync lên Firebase mỗi 100ms
    private long lastSyncTime = 0;

    // ── Callback ────────────────────────────────────────────────────────
    private GameEventListener eventListener;

    // ── Skill frozen ────────────────────────────────────────────────────
    private volatile boolean isFrozen = false;
    private volatile long frozenUntil = 0;

    // ── UI Paints ───────────────────────────────────────────────────────
    private Paint hudPaint;
    private Paint hudBgPaint;
    private Paint skillReadyPaint;
    private Paint skillCooldownPaint;
    private boolean skillReady = true;
    private long skillCooldownUntil = 0;
    private static final long SKILL_COOLDOWN_MS = 5000;

    public interface GameEventListener {
        void onGameFinished(long finishTimeMs);
        void onSkillUsed();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        initHudPaints();
    }

    private void initHudPaints() {
        hudPaint = new Paint();
        hudPaint.setColor(Color.WHITE);
        hudPaint.setAntiAlias(true);
        hudPaint.setTextSize(36f);
        hudPaint.setFakeBoldText(true);

        hudBgPaint = new Paint();
        hudBgPaint.setColor(Color.parseColor("#AA000000"));

        skillReadyPaint = new Paint();
        skillReadyPaint.setColor(Color.parseColor("#FF6B00"));
        skillReadyPaint.setAntiAlias(true);
        skillReadyPaint.setTextSize(28f);

        skillCooldownPaint = new Paint();
        skillCooldownPaint.setColor(Color.parseColor("#AA888888"));
        skillCooldownPaint.setAntiAlias(true);
        skillCooldownPaint.setTextSize(24f);
    }

    // ────────────────────────────────────────────────────────────────────
    // PUBLIC SETUP API
    // ────────────────────────────────────────────────────────────────────

    public void setupGame(long mapSeed, String roomId, String localUid, String displayName) {
        this.roomId = roomId;
        this.localUid = localUid;
        this.localDisplayName = displayName;
        this.mazeGenerator = new MazeGenerator(21, 15, mapSeed);
        this.maze = mazeGenerator.generate();
        this.goalRow = mazeGenerator.getGoalRow();
        this.goalCol = mazeGenerator.getGoalCol();
        this.renderer = new MazeRenderer();
        this.gameStartTime = System.currentTimeMillis();
        this.gameFinished = false;

        // Lắng nghe các player khác trong room
        if (roomId != null) {
            listenToOpponents();
        }
    }

    /** Setup cho chế độ offline (không cần roomId) */
    public void setupOffline(long mapSeed) {
        setupGame(mapSeed, null, "local", "You");
    }

    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
    }

    // ────────────────────────────────────────────────────────────────────
    // SURFACE CALLBACKS
    // ────────────────────────────────────────────────────────────────────

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (maze == null) {
            // Fallback nếu setupGame chưa được gọi
            setupOffline(System.currentTimeMillis());
        }
        computeLayout();
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        computeLayout();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        isRunning = false;
        stopListening();
        try { gameThread.join(2000); } catch (InterruptedException ignored) {}
    }

    /** Tính toán cellSize và offset để maze vừa màn hình */
    private void computeLayout() {
        if (maze == null) return;
        int w = getWidth();
        int h = getHeight();
        float cellW = (float) w / maze[0].length;
        float cellH = (float) h / maze.length;
        cellSize = Math.min(cellW, cellH);
        offsetX = (w - cellSize * maze[0].length) / 2f;
        offsetY = (h - cellSize * maze.length) / 2f;

        // Khởi tạo bóng tại ô start
        float startX = offsetX + mazeGenerator.getStartCol() * cellSize + cellSize / 2f;
        float startY = offsetY + mazeGenerator.getStartRow() * cellSize + cellSize / 2f;
        float ballRadius = cellSize * 0.3f;
        localBall = new Ball(startX, startY, ballRadius);
    }

    @Override
    public void run() {
        long lastTimeNs = System.nanoTime();
        while (isRunning) {
            long nowNs = System.nanoTime();
            float dt = Math.min((nowNs - lastTimeNs) / 1_000_000_000f, 0.05f);
            lastTimeNs = nowNs;

            if (!gameFinished) {
                update(dt);
            }

            Canvas canvas = null;
            try {
                canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    synchronized (getHolder()) {
                        renderFrame(canvas);
                    }
                }
            } finally {
                if (canvas != null) getHolder().unlockCanvasAndPost(canvas);
            }

            // Giữ ~60fps
            long elapsed = (System.nanoTime() - nowNs) / 1_000_000L;
            long sleep = FRAME_TIME_MS - elapsed;
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void update(float dt) {
        if (localBall == null || maze == null) return;

        // Kiểm tra frozen (bị đóng băng bởi skill của opponent)
        if (System.currentTimeMillis() > frozenUntil) {
            isFrozen = false;
        }

        if (!isFrozen) {
            localBall.update(tiltX, tiltY, dt);
            CollisionDetector.resolve(localBall, maze, cellSize, offsetX, offsetY);
        }

        // Kiểm tra đến goal
        if (CollisionDetector.checkGoal(localBall, goalRow, goalCol, cellSize, offsetX, offsetY)) {
            gameFinished = true;
            long elapsed = System.currentTimeMillis() - gameStartTime;
            syncFinish(elapsed);
            if (eventListener != null) {
                post(() -> eventListener.onGameFinished(elapsed));
            }
        }

        // Sync Firebase
        long nowMs = System.currentTimeMillis();
        if (roomId != null && nowMs - lastSyncTime > SYNC_INTERVAL_MS) {
            lastSyncTime = nowMs;
            FirestoreManager.getInstance().updatePlayerPosition(roomId, localUid, localBall.x, localBall.y);
        }

        // Cập nhật cooldown skill
        if (!skillReady && nowMs > skillCooldownUntil) {
            skillReady = true;
        }
    }

    private void renderFrame(Canvas canvas) {
        if (renderer == null || maze == null) {
            canvas.drawColor(Color.BLACK);
            return;
        }

        // Vẽ maze
        renderer.drawMaze(canvas, maze, cellSize, offsetX, offsetY, goalRow, goalCol);

        // Vẽ opponent
        renderer.drawOpponentBalls(canvas, opponents, localUid, cellSize, offsetX, offsetY);

        // Vẽ local player
        if (localBall != null) {
            renderer.drawLocalBall(canvas, localBall);
        }

        // HUD: timer
        drawHUD(canvas);
    }

    private void drawHUD(Canvas canvas) {
        long elapsed = System.currentTimeMillis() - gameStartTime;
        String timeStr = formatTime(elapsed);

        // Timer background
        hudBgPaint.setColor(Color.parseColor("#AA000000"));
        canvas.drawRoundRect(8, 8, 240, 64, 12, 12, hudBgPaint);
        hudPaint.setColor(Color.parseColor("#00F5FF"));
        hudPaint.setTextSize(34f);
        canvas.drawText("⏱ " + timeStr, 20, 52, hudPaint);

        // Skill indicator
        drawSkillHUD(canvas);

        // Frozen overlay
        if (isFrozen) {
            Paint frozenOverlay = new Paint();
            frozenOverlay.setColor(Color.parseColor("#5000BFFF"));
            canvas.drawRect(0, 0, getWidth(), getHeight(), frozenOverlay);
            hudPaint.setColor(Color.parseColor("#00BFFF"));
            hudPaint.setTextSize(40f);
            hudPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("❄ FROZEN ❄", getWidth() / 2f, getHeight() / 2f, hudPaint);
            hudPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawSkillHUD(Canvas canvas) {
        float btnSize = 80f;
        float btnX = getWidth() - btnSize - 12;
        float btnY = getHeight() - btnSize - 12;

        hudBgPaint.setColor(skillReady ? Color.parseColor("#AAFF6B00") : Color.parseColor("#AA444444"));
        canvas.drawRoundRect(btnX, btnY, btnX + btnSize, btnY + btnSize, 16, 16, hudBgPaint);

        Paint icon = skillReady ? skillReadyPaint : skillCooldownPaint;
        icon.setTextAlign(Paint.Align.CENTER);
        icon.setTextSize(32f);
        canvas.drawText("⚡", btnX + btnSize / 2, btnY + btnSize / 2 + 12, icon);

        if (!skillReady) {
            long remaining = (skillCooldownUntil - System.currentTimeMillis()) / 1000 + 1;
            if (remaining > 0) {
                skillCooldownPaint.setTextSize(22f);
                canvas.drawText(remaining + "s", btnX + btnSize / 2, btnY + btnSize - 8, skillCooldownPaint);
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // SENSOR INPUTS
    // ────────────────────────────────────────────────────────────────────

    /** Nhận dữ liệu Accelerometer từ Activity */
    public void updateTilt(float x, float y) {
        this.tiltX = x;
        this.tiltY = y;
    }

    /** Nhận sự kiện Proximity – kích hoạt skill */
    public void onProximityTriggered() {
        if (!skillReady) return;
        skillReady = false;
        skillCooldownUntil = System.currentTimeMillis() + SKILL_COOLDOWN_MS;

        // Kích hoạt boost cho bản thân
        if (localBall != null) {
            localBall.activateBoost();
        }

        // Đóng băng opponent đầu tiên tìm được
        if (roomId != null && opponents != null && !opponents.isEmpty()) {
            for (PlayerState p : opponents) {
                if (!p.getUid().equals(localUid)) {
                    long freezeEnd = System.currentTimeMillis() + 2000;
                    FirestoreManager.getInstance().freezePlayer(roomId, p.getUid(), freezeEnd);
                    break;
                }
            }
        }

        if (eventListener != null) post(eventListener::onSkillUsed);
    }

    // ────────────────────────────────────────────────────────────────────
    // FIREBASE SYNC
    // ────────────────────────────────────────────────────────────────────

    private void listenToOpponents() {
        playersListener = FirestoreManager.getInstance().listenPlayers(roomId, players -> {
            this.opponents = players;
            // Kiểm tra mình có bị freeze không
            if (players != null) {
                for (PlayerState p : players) {
                    if (p.getUid().equals(localUid)) {
                        if (p.isFrozen() && p.getFrozenUntil() > System.currentTimeMillis()) {
                            isFrozen = true;
                            frozenUntil = p.getFrozenUntil();
                        }
                        break;
                    }
                }
            }
        });
    }

    private void syncFinish(long elapsed) {
        if (roomId == null) return;
        FirestoreManager.getInstance().updatePlayerFinish(roomId, localUid, elapsed, new FirestoreManager.OnCompleteCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(String error) {}
        });
    }

    private void stopListening() {
        if (playersListener != null) {
            playersListener.remove();
            playersListener = null;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String formatTime(long ms) {
        long s = ms / 1000;
        long m = s / 60;
        s = s % 60;
        return String.format(java.util.Locale.US, "%02d:%02d.%01d", m, s, (ms % 1000) / 100);
    }
}