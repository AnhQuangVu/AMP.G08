package com.example.ampg08.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.game.Ball;
import com.example.ampg08.game.CollisionDetector;
import com.example.ampg08.game.MazeGenerator;
import com.example.ampg08.game.MazeRenderer;
import com.example.ampg08.game.SkillController;
import com.example.ampg08.model.PlayerState;
import com.example.ampg08.sync.PositionSyncManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // ─── Constants ───────────────────────────────────────────────────────
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;

    // ─── Game State ──────────────────────────────────────────────────────
    private Thread gameThread;
    private volatile boolean isRunning = false;
    private volatile boolean isFinished = false;

    // ─── Maze ────────────────────────────────────────────────────────────
    private MazeGenerator mazeGenerator;
    private int[][] maze;
    private MazeRenderer mazeRenderer;
    private CollisionDetector collisionDetector;

    // ─── Ball & Physics ──────────────────────────────────────────────────
    private Ball localBall;
    private float tiltX = 0f, tiltY = 0f;

    // ─── Layout ──────────────────────────────────────────────────────────
    private float cellSize;
    private float mazeOffsetX, mazeOffsetY;
    private float goalCenterX, goalCenterY;

    // ─── Multiplayer ─────────────────────────────────────────────────────
    private String roomId;
    private String localUid;
    private String localDisplayName;
    private boolean offline = true;
    private final Map<String, PlayerState> remotePlayers = new ConcurrentHashMap<>();
    private ListenerRegistration playersListener;
    private PositionSyncManager syncManager;

    // ─── Skill ───────────────────────────────────────────────────────────
    private SkillController skillController;

    // ─── Timing ──────────────────────────────────────────────────────────
    private long gameStartTimeMs;
    private long finishTimeMs;

    // ─── Paints ──────────────────────────────────────────────────────────
    private Paint ballPaint;
    private Paint remoteBallPaint;
    private Paint goalPaint;
    private Paint textPaint;
    private Paint freezeOverlayPaint;
    private Paint boostGlowPaint;

    // ─── Listener ────────────────────────────────────────────────────────
    private GameEventListener eventListener;

    public interface GameEventListener {
        void onGameFinished(long finishTimeMs);
        void onSkillUsed();
    }

    // ─── Constructor ─────────────────────────────────────────────────────

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
        initPaints();
        syncManager = new PositionSyncManager();
        skillController = new SkillController();
    }

    private void initPaints() {
        // Local ball - Neon Cyan
        ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ballPaint.setColor(0xFF00F5FF);
        ballPaint.setStyle(Paint.Style.FILL);
        ballPaint.setShadowLayer(20f, 0, 0, 0xFF00F5FF);

        // Remote ball - Neon Orange
        remoteBallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        remoteBallPaint.setColor(0xFFFF6B00);
        remoteBallPaint.setStyle(Paint.Style.FILL);
        remoteBallPaint.setShadowLayer(15f, 0, 0, 0xFFFF6B00);

        // Goal - Green glow
        goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goalPaint.setColor(0xFF00FF88);
        goalPaint.setStyle(Paint.Style.FILL);

        // Text
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Freeze overlay
        freezeOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freezeOverlayPaint.setColor(0x4400BFFF);
        freezeOverlayPaint.setStyle(Paint.Style.FILL);

        // Boost glow
        boostGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boostGlowPaint.setColor(0xFFFFD700);
        boostGlowPaint.setStyle(Paint.Style.STROKE);
        boostGlowPaint.setStrokeWidth(4f);
        boostGlowPaint.setShadowLayer(25f, 0, 0, 0xFFFFD700);
    }

    // ─── Setup Methods ───────────────────────────────────────────────────

    public void setupOffline(long seed) {
        this.offline = true;
        this.roomId = null;
        this.localUid = "local";
        this.localDisplayName = "Player";
        generateMaze(seed);
    }

    public void setupGame(long seed, String roomId, String uid, String displayName) {
        this.offline = false;
        this.roomId = roomId;
        this.localUid = uid;
        this.localDisplayName = displayName;
        generateMaze(seed);
        listenRemotePlayers();
    }

    private void generateMaze(long seed) {
        // Maze size 21x21 cells (ensures perfect maze)
        int mazeRows = 21;
        int mazeCols = 21;
        mazeGenerator = new MazeGenerator(mazeRows, mazeCols, seed);
        maze = mazeGenerator.generate();
        mazeRenderer = new MazeRenderer(maze);
        collisionDetector = new CollisionDetector(maze);
    }

    public void computeLayout() {
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0 || maze == null) return;

        int rows = mazeGenerator.getRows();
        int cols = mazeGenerator.getCols();

        // Tính cellSize sao cho maze vừa màn hình (có padding)
        float padding = 40f;
        float availableWidth = width - 2 * padding;
        float availableHeight = height - 2 * padding - 100; // 100 cho HUD

        cellSize = Math.min(availableWidth / cols, availableHeight / rows);

        // Center maze
        float mazeWidth = cols * cellSize;
        float mazeHeight = rows * cellSize;
        mazeOffsetX = (width - mazeWidth) / 2f;
        mazeOffsetY = (height - mazeHeight) / 2f + 50; // Offset for HUD

        // Init ball at start position
        float startX = mazeOffsetX + (mazeGenerator.getStartCol() + 0.5f) * cellSize;
        float startY = mazeOffsetY + (mazeGenerator.getStartRow() + 0.5f) * cellSize;
        float ballRadius = cellSize * 0.35f;

        localBall = new Ball(startX, startY, ballRadius);

        // Goal center
        goalCenterX = mazeOffsetX + (mazeGenerator.getGoalCol() + 0.5f) * cellSize;
        goalCenterY = mazeOffsetY + (mazeGenerator.getGoalRow() + 0.5f) * cellSize;

        // Cập nhật collision detector với layout mới
        collisionDetector.setLayout(mazeOffsetX, mazeOffsetY, cellSize);

        gameStartTimeMs = System.currentTimeMillis();
    }

    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
    }

    // ─── Sensor Input ────────────────────────────────────────────────────

    public void updateTilt(float ax, float ay) {
        this.tiltX = ax;
        this.tiltY = ay;
    }

    public void onProximityTriggered() {
        if (skillController.canUseSkill()) {
            skillController.activateSkill();

            // Boost bản thân
            localBall.activateBoost(System.currentTimeMillis());

            // Freeze đối thủ gần nhất (nếu multiplayer)
            if (!offline) {
                freezeNearestOpponent();
            }

            if (eventListener != null) {
                eventListener.onSkillUsed();
            }
        }
    }

    private void freezeNearestOpponent() {
        if (remotePlayers.isEmpty()) return;

        String nearestUid = null;
        float minDist = Float.MAX_VALUE;

        for (Map.Entry<String, PlayerState> entry : remotePlayers.entrySet()) {
            if (entry.getKey().equals(localUid)) continue;

            PlayerState ps = entry.getValue();
            float dx = ps.getX() - localBall.x;
            float dy = ps.getY() - localBall.y;
            float dist = dx * dx + dy * dy;

            if (dist < minDist) {
                minDist = dist;
                nearestUid = entry.getKey();
            }
        }

        if (nearestUid != null && roomId != null) {
            // Gửi freeze command qua Firestore
            FirestoreManager.getInstance().sendFreezeCommand(roomId, nearestUid);
        }
    }

    public void applyFreezeFromRemote() {
        localBall.applyFreeze(System.currentTimeMillis());
    }

    // ─── Multiplayer Sync ────────────────────────────────────────────────

    private void listenRemotePlayers() {
        if (roomId == null) return;

        playersListener = FirestoreManager.getInstance().listenPlayers(roomId, players -> {
            if (players == null) return;

            for (PlayerState ps : players) {
                if (!ps.getUid().equals(localUid)) {
                    remotePlayers.put(ps.getUid(), ps);
                }

                // Kiểm tra freeze command cho local player
                if (ps.getUid().equals(localUid) && ps.isFreezeRequested()) {
                    applyFreezeFromRemote();
                    // Clear freeze flag
                    FirestoreManager.getInstance().clearFreezeFlag(roomId, localUid);
                }
            }
        });

        // Start position sync
        syncManager.start(roomId, localUid, new PositionSyncManager.PositionProvider() {
            @Override
            public float getX() { return localBall != null ? localBall.x : 0; }
            @Override
            public float getY() { return localBall != null ? localBall.y : 0; }
        }, offline);
    }

    // ─── SurfaceView Callbacks ───────────────────────────────────────────

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (maze == null) {
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
        syncManager.stop();
        if (playersListener != null) {
            playersListener.remove();
        }
        try {
            if (gameThread != null) {
                gameThread.join(500);
            }
        } catch (InterruptedException ignored) {}
    }

    // ─── Game Loop ───────────────────────────────────────────────────────

    @Override
    public void run() {
        long lastFrameTime = System.nanoTime();

        while (isRunning) {
            long now = System.nanoTime();
            float dt = (now - lastFrameTime) / 1_000_000_000f;
            lastFrameTime = now;

            // Giới hạn dt để tránh physics jump
            dt = Math.min(dt, 0.05f);

            if (!isFinished) {
                update(dt);
            }
            render();

            // Frame rate control
            long elapsed = System.nanoTime() - now;
            long sleepTime = (FRAME_TIME_NS - elapsed) / 1_000_000;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void update(float dt) {
        if (localBall == null) return;

        long nowMs = System.currentTimeMillis();

        // Update ball physics
        localBall.update(tiltX, tiltY, dt, nowMs);

        // Collision detection với tường
        collisionDetector.resolve(localBall);

        // Check goal
        float dx = localBall.x - goalCenterX;
        float dy = localBall.y - goalCenterY;
        float distToGoal = (float) Math.sqrt(dx * dx + dy * dy);

        if (distToGoal < cellSize * 0.4f) {
            onReachGoal();
        }

        // Update skill cooldown
        skillController.update(nowMs);
    }

    private void onReachGoal() {
        if (isFinished) return;

        isFinished = true;
        finishTimeMs = System.currentTimeMillis() - gameStartTimeMs;

        // Lưu finish time lên Firestore (multiplayer)
        if (!offline && roomId != null && localUid != null) {
            FirestoreManager.getInstance().updatePlayerFinish(roomId, localUid, finishTimeMs,
                    new FirestoreManager.OnCompleteCallback() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String error) {}
                    });
        }

        // Notify Activity
        if (eventListener != null) {
            post(() -> eventListener.onGameFinished(finishTimeMs));
        }
    }

    private void render() {
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas != null) {
                draw(canvas);
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Background
        canvas.drawColor(0xFF0A0A14);

        if (maze == null || localBall == null) return;

        // Draw maze
        mazeRenderer.draw(canvas, mazeOffsetX, mazeOffsetY, cellSize);

        // Draw goal (blinking effect)
        drawGoal(canvas);

        // Draw remote players
        drawRemotePlayers(canvas);

        // Draw local ball
        drawLocalBall(canvas);

        // Draw HUD
        drawHUD(canvas);

        // Draw freeze overlay nếu đang bị freeze
        if (localBall.isFrozen(System.currentTimeMillis())) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), freezeOverlayPaint);
            canvas.drawText("❄️ FROZEN", getWidth() / 2f, getHeight() / 2f, textPaint);
        }
    }

    private void drawGoal(Canvas canvas) {
        // Blinking effect
        long time = System.currentTimeMillis();
        float alpha = (float) (0.5 + 0.5 * Math.sin(time * 0.006));
        goalPaint.setAlpha((int) (alpha * 180));

        float goalRadius = cellSize * 0.45f;
        canvas.drawCircle(goalCenterX, goalCenterY, goalRadius, goalPaint);

        // Goal glow
        goalPaint.setAlpha((int) (alpha * 80));
        canvas.drawCircle(goalCenterX, goalCenterY, goalRadius * 1.3f, goalPaint);
    }

    private void drawRemotePlayers(Canvas canvas) {
        for (PlayerState ps : remotePlayers.values()) {
            if (ps.getFinishTime() > 0) continue; // Đã về đích

            float rx = ps.getX();
            float ry = ps.getY();

            // Validate position
            if (rx > 0 && ry > 0) {
                canvas.drawCircle(rx, ry, localBall.radius * 0.9f, remoteBallPaint);
            }
        }
    }

    private void drawLocalBall(Canvas canvas) {
        long nowMs = System.currentTimeMillis();

        // Boost glow effect
        if (localBall.isBoosted(nowMs)) {
            canvas.drawCircle(localBall.x, localBall.y, localBall.radius * 1.5f, boostGlowPaint);
        }

        canvas.drawCircle(localBall.x, localBall.y, localBall.radius, ballPaint);
    }

    private void drawHUD(Canvas canvas) {
        // Timer
        long elapsed = isFinished ? finishTimeMs : (System.currentTimeMillis() - gameStartTimeMs);
        String timeStr = formatTime(elapsed);
        canvas.drawText(timeStr, getWidth() / 2f, 80, textPaint);

        // Skill cooldown indicator
        float cooldownProgress = skillController.getCooldownProgress();
        if (cooldownProgress < 1f) {
            Paint cooldownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            cooldownPaint.setColor(0xFF666666);
            cooldownPaint.setStyle(Paint.Style.STROKE);
            cooldownPaint.setStrokeWidth(8f);

            float cx = 80, cy = 80, radius = 30;
            canvas.drawCircle(cx, cy, radius, cooldownPaint);

            cooldownPaint.setColor(0xFF00F5FF);
            RectF arcRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
            canvas.drawArc(arcRect, -90, 360 * cooldownProgress, false, cooldownPaint);
        } else {
            // Skill ready indicator
            Paint readyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            readyPaint.setColor(0xFF00FF88);
            readyPaint.setTextSize(32f);
            readyPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("⚡", 80, 90, readyPaint);
        }

        // Player count (multiplayer)
        if (!offline) {
            Paint playerCountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            playerCountPaint.setColor(0xFFFFFFFF);
            playerCountPaint.setTextSize(32f);
            playerCountPaint.setTextAlign(Paint.Align.RIGHT);
            int count = remotePlayers.size() + 1;
            canvas.drawText("👥 " + count, getWidth() - 40, 80, playerCountPaint);
        }
    }

    private String formatTime(long ms) {
        long s = ms / 1000;
        long m = s / 60;
        s = s % 60;
        long millis = (ms % 1000) / 100;
        return String.format(java.util.Locale.US, "%02d:%02d.%01d", m, s, millis);
    }

    // ─── Cleanup ─────────────────────────────────────────────────────────

    public void cleanup() {
        isRunning = false;
        syncManager.stop();
        if (playersListener != null) {
            playersListener.remove();
        }
    }
}