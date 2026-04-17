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

import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.game.Ball;
import com.example.ampg08.game.CollisionDetector;
import com.example.ampg08.game.MazeGenerator;
import com.example.ampg08.game.MazeRenderer;
import com.example.ampg08.game.SkillController;
import com.example.ampg08.model.PlayerState;
import com.example.ampg08.sync.PositionSyncManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // ─── Constants ───────────────────────────────────────────────────────
    private static final int   TARGET_FPS    = 60;
    private static final long  FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;
    private static final float MAX_DT        = 0.05f; // tránh physics jump
    private static final long REMOTE_PLAYER_STALE_MS = 1500L;
    private static final float MAP_COORD_INVALID = -1f;

    // ─── Game State ──────────────────────────────────────────────────────
    private Thread           gameThread;
    private volatile boolean isRunning  = false;
    private volatile boolean isPaused   = false;
    private volatile boolean isFinished = false;
    private final    Object  pauseLock  = new Object();

    // ─── Maze ────────────────────────────────────────────────────────────
    private MazeGenerator    mazeGenerator;
    private int[][]          maze;
    private MazeRenderer     mazeRenderer;
    private CollisionDetector collisionDetector;

    // ─── Ball & Physics ──────────────────────────────────────────────────
    private Ball  localBall;
    private float tiltX = 0f, tiltY = 0f;

    // ─── Layout ──────────────────────────────────────────────────────────
    private float cellSize;
    private float mazeOffsetX, mazeOffsetY;
    private float goalCenterX, goalCenterY;

    // ─── Multiplayer ─────────────────────────────────────────────────────
    private String  roomId;
    private String  localUid;
    private String  localDisplayName;
    private boolean offline = true;

    private final Map<String, PlayerState> remotePlayers    = new ConcurrentHashMap<>();
    private       ListenerRegistration     playersListener;
    private       PositionSyncManager      syncManager;

    // Guard: tránh ghi leaderboard nhiều lần
    private final AtomicBoolean finishWritten = new AtomicBoolean(false);

    // ─── Skill ───────────────────────────────────────────────────────────
    private SkillController skillController;

    // ─── Timing ──────────────────────────────────────────────────────────
    private long gameStartTimeMs;
    private long finishTimeMs;

    // ─── Paints (khởi tạo 1 lần) ────────────────────────────────────────
    private Paint ballPaint;
    private Paint remoteBallPaint;
    private Paint textPaint;
    private Paint hudTimerPaint;
    private Paint freezeOverlayPaint;
    private Paint boostGlowPaint;
    private Paint cooldownTrackPaint;
    private Paint cooldownArcPaint;
    private Paint cooldownReadyPaint;
    private Paint playerCountPaint;
    private Paint remoteNamePaint;

    // ─── Listener ────────────────────────────────────────────────────────
    private GameEventListener eventListener;

    public interface GameEventListener {
        void onGameFinished(long finishTimeMs);
        void onSkillUsed();
    }

    // ─── Constructors ────────────────────────────────────────────────────

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
        setLayerType(LAYER_TYPE_HARDWARE, null); // hardware acceleration cho SurfaceView
        initPaints();
        syncManager    = new PositionSyncManager();
        skillController = new SkillController();
    }

    private void initPaints() {
        ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ballPaint.setColor(0xFF00F5FF);
        ballPaint.setStyle(Paint.Style.FILL);
        ballPaint.setShadowLayer(16f, 0, 0, 0xFF00F5FF);

        remoteBallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        remoteBallPaint.setColor(0xFFFF6B00);
        remoteBallPaint.setStyle(Paint.Style.FILL);
        remoteBallPaint.setShadowLayer(12f, 0, 0, 0xFFFF6B00);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        hudTimerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudTimerPaint.setColor(0xFF00F5FF);
        hudTimerPaint.setTextSize(44f);
        hudTimerPaint.setTextAlign(Paint.Align.CENTER);
        hudTimerPaint.setFakeBoldText(true);

        freezeOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freezeOverlayPaint.setColor(0x5500BFFF);
        freezeOverlayPaint.setStyle(Paint.Style.FILL);

        boostGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boostGlowPaint.setColor(0xFFFFD700);
        boostGlowPaint.setStyle(Paint.Style.STROKE);
        boostGlowPaint.setStrokeWidth(4f);
        boostGlowPaint.setShadowLayer(20f, 0, 0, 0xFFFFD700);

        cooldownTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cooldownTrackPaint.setColor(0xFF333355);
        cooldownTrackPaint.setStyle(Paint.Style.STROKE);
        cooldownTrackPaint.setStrokeWidth(7f);

        cooldownArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cooldownArcPaint.setColor(0xFF00F5FF);
        cooldownArcPaint.setStyle(Paint.Style.STROKE);
        cooldownArcPaint.setStrokeWidth(7f);
        cooldownArcPaint.setStrokeCap(Paint.Cap.ROUND);

        cooldownReadyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cooldownReadyPaint.setColor(0xFF00FF88);
        cooldownReadyPaint.setTextSize(30f);
        cooldownReadyPaint.setTextAlign(Paint.Align.CENTER);

        playerCountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playerCountPaint.setColor(0xFFFFFFFF);
        playerCountPaint.setTextSize(30f);
        playerCountPaint.setTextAlign(Paint.Align.RIGHT);

        remoteNamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        remoteNamePaint.setColor(0xFFFF6B00);
        remoteNamePaint.setTextSize(22f);
        remoteNamePaint.setTextAlign(Paint.Align.CENTER);
    }

    // ─── Setup ──────────────────────────────────────────────────────────

    public void setupOffline(long seed) {
        this.offline  = true;
        this.roomId   = null;
        this.localUid = "local";
        this.localDisplayName = "Player";
        generateMaze(seed);
    }

    public void setupGame(long seed, String roomId, String uid, String displayName) {
        this.offline  = false;
        this.roomId   = roomId;
        this.localUid = uid;
        this.localDisplayName = displayName;
        generateMaze(seed);
        listenRemotePlayers();
    }

    private void generateMaze(long seed) {
        int mazeRows = 21;
        int mazeCols = 21;
        mazeGenerator     = new MazeGenerator(mazeRows, mazeCols, seed);
        maze              = mazeGenerator.generate();
        mazeRenderer      = new MazeRenderer(maze);
        collisionDetector = new CollisionDetector(maze);
        // Truyền vị trí goal cho renderer để vẽ nhấp nháy
        mazeRenderer.setGoal(mazeGenerator.getGoalRow(), mazeGenerator.getGoalCol());
    }

    public void computeLayout() {
        int width  = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0 || maze == null) return;

        int rows = mazeGenerator.getRows();
        int cols = mazeGenerator.getCols();

        float padding         = 40f;
        float hudHeight       = 100f;
        float availableWidth  = width  - 2 * padding;
        float availableHeight = height - 2 * padding - hudHeight;

        cellSize = Math.min(availableWidth / cols, availableHeight / rows);

        float mazeWidth  = cols * cellSize;
        float mazeHeight = rows * cellSize;
        mazeOffsetX = (width  - mazeWidth)  / 2f;
        mazeOffsetY = (height - mazeHeight) / 2f + hudHeight / 2f;

        float startX = mazeOffsetX + (mazeGenerator.getStartCol() + 0.5f) * cellSize;
        float startY = mazeOffsetY + (mazeGenerator.getStartRow() + 0.5f) * cellSize;
        float ballRadius = cellSize * 0.32f;

        localBall = new Ball(startX, startY, ballRadius);

        goalCenterX = mazeOffsetX + (mazeGenerator.getGoalCol() + 0.5f) * cellSize;
        goalCenterY = mazeOffsetY + (mazeGenerator.getGoalRow() + 0.5f) * cellSize;

        collisionDetector.setLayout(mazeOffsetX, mazeOffsetY, cellSize);

        gameStartTimeMs = System.currentTimeMillis();
        finishWritten.set(false);
    }

    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
    }

    // ─── Sensor Input ────────────────────────────────────────────────────

    public void updateTilt(float ax, float ay) {
        this.tiltX = ax;
        this.tiltY = ay;
    }

    /**
     * Gọi khi proximity sensor kích hoạt.
     * SkillController đã có cooldown — không cần guard thêm ở ngoài.
     */
    public void onProximityTriggered() {
        if (localBall == null || isFinished || isPaused) return;

        if (skillController.canUseSkill()) {
            long nowMs = System.currentTimeMillis();
            skillController.activateSkill();
            localBall.activateBoost(nowMs, skillController.getBoostDuration());

            if (!offline) {
                freezeNearestOpponent(nowMs);
            }

            if (eventListener != null) {
                post(() -> eventListener.onSkillUsed());
            }
        }
    }

    private void freezeNearestOpponent(long nowMs) {
        if (remotePlayers.isEmpty() || roomId == null || localBall == null) return;

        String nearestUid = null;
        float minDistSq = Float.MAX_VALUE;

        for (Map.Entry<String, PlayerState> entry : remotePlayers.entrySet()) {
            if (entry.getKey().equals(localUid)) continue;
            PlayerState ps = entry.getValue();
            if (ps == null) continue;
            if (ps.getFinishTime() > 0) continue; // Da ve dich.
            if (ps.getUpdatedAt() > 0 && nowMs - ps.getUpdatedAt() > REMOTE_PLAYER_STALE_MS) continue; // Du lieu stale.

            float rx = resolveRemoteX(ps);
            float ry = resolveRemoteY(ps);
            if (rx <= 0f || ry <= 0f) continue;

            float dx = rx - localBall.x;
            float dy = ry - localBall.y;
            float dSq = dx * dx + dy * dy;

            if (dSq < minDistSq) {
                minDistSq = dSq;
                nearestUid = entry.getKey();
            }
        }

        if (nearestUid != null) {
            FirestoreManager.getInstance().sendFreezeCommand(roomId, nearestUid);
        }
    }

    // ─── Multiplayer ────────────────────────────────────────────────────

    private void listenRemotePlayers() {
        if (roomId == null) return;

        playersListener = FirestoreManager.getInstance().listenPlayers(roomId, players -> {
            if (players == null) return;

            Map<String, PlayerState> latestRemote = new ConcurrentHashMap<>();
            for (PlayerState ps : players) {
                if (ps.getUid() == null) continue;

                if (!ps.getUid().equals(localUid)) {
                    latestRemote.put(ps.getUid(), ps);
                } else {
                    // Kiem tra freeze command cho local player.
                    if (ps.isFreezeRequested()) {
                        if (localBall != null) {
                            localBall.applyFreeze(System.currentTimeMillis(), skillController.getFreezeDuration());
                        }
                        FirestoreManager.getInstance().clearFreezeFlag(roomId, localUid);
                    }
                }
            }

            remotePlayers.clear();
            remotePlayers.putAll(latestRemote);
        });

        syncManager.start(roomId, localUid, new PositionSyncManager.PositionProvider() {
            @Override public float getX() { return localBall != null ? localBall.x : 0; }
            @Override public float getY() { return localBall != null ? localBall.y : 0; }
            @Override public float getMapX() { return localBall != null ? toMapX(localBall.x) : MAP_COORD_INVALID; }
            @Override public float getMapY() { return localBall != null ? toMapY(localBall.y) : MAP_COORD_INVALID; }
        }, offline);
    }

    // ─── Surface Callbacks ──────────────────────────────────────────────

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (maze == null) setupOffline(System.currentTimeMillis());
        computeLayout();
        isRunning = false; // reset trước khi start
        startGameThread();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        computeLayout();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        stopGameThread();
        syncManager.stop();
        if (playersListener != null) playersListener.remove();
    }

    private void startGameThread() {
        isRunning = true;
        isPaused  = false;
        gameThread = new Thread(this, "GameThread");
        gameThread.start();
    }

    private void stopGameThread() {
        isRunning = false;
        // Wake up nếu đang wait trong pauseLock
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        try {
            if (gameThread != null) gameThread.join(1000);
        } catch (InterruptedException ignored) {}
        gameThread = null;
    }

    /** Gọi từ Activity.onPause() */
    public void pauseGame() {
        isPaused = true;
    }

    /** Gọi từ Activity.onResume() */
    public void resumeGame() {
        isPaused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    // ─── Game Loop ──────────────────────────────────────────────────────

    @Override
    public void run() {
        long lastFrameTime = System.nanoTime();

        while (isRunning) {
            // Pause support
            synchronized (pauseLock) {
                while (isPaused && isRunning) {
                    try { pauseLock.wait(); } catch (InterruptedException ignored) {}
                }
            }
            if (!isRunning) break;

            long now = System.nanoTime();
            float dt = Math.min((now - lastFrameTime) / 1_000_000_000f, MAX_DT);
            lastFrameTime = now;

            if (!isFinished) {
                update(dt);
            }
            render();

            // Frame cap
            long elapsed   = System.nanoTime() - now;
            long sleepMs   = (FRAME_TIME_NS - elapsed) / 1_000_000;
            if (sleepMs > 1) {
                try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void update(float dt) {
        if (localBall == null) return;
        long nowMs = System.currentTimeMillis();

        localBall.update(tiltX, tiltY, dt, nowMs);
        collisionDetector.resolve(localBall);
        skillController.update(nowMs);

        // Check goal
        float dx   = localBall.x - goalCenterX;
        float dy   = localBall.y - goalCenterY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < cellSize * 0.45f) {
            onReachGoal();
        }
    }

    private void onReachGoal() {
        if (isFinished) return;
        isFinished  = true;
        finishTimeMs = System.currentTimeMillis() - gameStartTimeMs;

        // Ghi Firestore chỉ 1 lần (AtomicBoolean guard)
        if (!offline && roomId != null && localUid != null && finishWritten.compareAndSet(false, true)) {
            FirestoreManager.getInstance().updatePlayerFinish(
                    roomId, localUid, finishTimeMs,
                    new FirestoreManager.OnCompleteCallback() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String error) {}
                    });
        }

        if (eventListener != null) {
            post(() -> eventListener.onGameFinished(finishTimeMs));
        }
    }

    private void render() {
        Canvas canvas = null;
        SurfaceHolder holder = getHolder();
        try {
            canvas = holder.lockCanvas();
            if (canvas != null) draw(canvas);
        } finally {
            if (canvas != null) {
                try { holder.unlockCanvasAndPost(canvas); }
                catch (IllegalStateException ignored) {}
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawColor(0xFF0A0A14);

        if (maze == null || localBall == null) return;

        long nowMs = System.currentTimeMillis();

        // Maze (renderer xử lý goal nhấp nháy bên trong)
        mazeRenderer.draw(canvas, mazeOffsetX, mazeOffsetY, cellSize, nowMs);

        // Remote players
        for (PlayerState ps : remotePlayers.values()) {
            if (ps.getFinishTime() > 0) continue;
            float rx = resolveRemoteX(ps);
            float ry = resolveRemoteY(ps);
            if (rx > 0 && ry > 0) {
                canvas.drawCircle(rx, ry, localBall.radius * 0.9f, remoteBallPaint);
                // Tên đối thủ
                canvas.drawText(
                        ps.getDisplayName() != null ? ps.getDisplayName() : "?",
                        rx, ry - localBall.radius - 6f, remoteNamePaint);
            }
        }

        // Local ball
        if (localBall.isBoosted(nowMs)) {
            canvas.drawCircle(localBall.x, localBall.y, localBall.radius * 1.6f, boostGlowPaint);
        }
        canvas.drawCircle(localBall.x, localBall.y, localBall.radius, ballPaint);

        // HUD
        drawHUD(canvas, nowMs);

        // Freeze overlay
        if (localBall.isFrozen(nowMs)) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), freezeOverlayPaint);
            canvas.drawText("❄ FROZEN", getWidth() / 2f, getHeight() / 2f, textPaint);
        }
    }

    private void drawHUD(Canvas canvas, long nowMs) {
        // Timer
        long elapsed = isFinished ? finishTimeMs : (nowMs - gameStartTimeMs);
        canvas.drawText(formatTime(elapsed), getWidth() / 2f, 72, hudTimerPaint);

        // Skill cooldown ring
        float cx = 72f, cy = 72f, r = 28f;
        float progress = skillController.getCooldownProgress();
        canvas.drawCircle(cx, cy, r, cooldownTrackPaint);
        if (progress < 1f) {
            RectF arc = new RectF(cx - r, cy - r, cx + r, cy + r);
            canvas.drawArc(arc, -90f, 360f * progress, false, cooldownArcPaint);
        } else {
            canvas.drawText("Z", cx, cy + 10f, cooldownReadyPaint);
        }

        // Player count (multiplayer)
        if (!offline) {
            int total = remotePlayers.size() + 1;
            canvas.drawText("x" + total, getWidth() - 24f, 72f, playerCountPaint);
        }
    }

    private float toMapX(float screenX) {
        if (mazeGenerator == null || cellSize <= 0f) return MAP_COORD_INVALID;
        float mazeWidth = mazeGenerator.getCols() * cellSize;
        if (mazeWidth <= 0f) return MAP_COORD_INVALID;
        return clamp01((screenX - mazeOffsetX) / mazeWidth);
    }

    private float toMapY(float screenY) {
        if (mazeGenerator == null || cellSize <= 0f) return MAP_COORD_INVALID;
        float mazeHeight = mazeGenerator.getRows() * cellSize;
        if (mazeHeight <= 0f) return MAP_COORD_INVALID;
        return clamp01((screenY - mazeOffsetY) / mazeHeight);
    }

    private float fromMapX(float mapX) {
        if (mazeGenerator == null || cellSize <= 0f) return MAP_COORD_INVALID;
        float mazeWidth = mazeGenerator.getCols() * cellSize;
        if (mazeWidth <= 0f) return MAP_COORD_INVALID;
        return mazeOffsetX + clamp01(mapX) * mazeWidth;
    }

    private float fromMapY(float mapY) {
        if (mazeGenerator == null || cellSize <= 0f) return MAP_COORD_INVALID;
        float mazeHeight = mazeGenerator.getRows() * cellSize;
        if (mazeHeight <= 0f) return MAP_COORD_INVALID;
        return mazeOffsetY + clamp01(mapY) * mazeHeight;
    }

    private float resolveRemoteX(PlayerState ps) {
        if (ps.getMapX() >= 0f && ps.getMapX() <= 1f) {
            return fromMapX(ps.getMapX());
        }
        return ps.getX();
    }

    private float resolveRemoteY(PlayerState ps) {
        if (ps.getMapY() >= 0f && ps.getMapY() <= 1f) {
            return fromMapY(ps.getMapY());
        }
        return ps.getY();
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private String formatTime(long ms) {
        long s  = ms / 1000;
        long m  = s / 60;
        s       = s % 60;
        long cs = (ms % 1000) / 100;
        return String.format(java.util.Locale.US, "%02d:%02d.%01d", m, s, cs);
    }

    public void cleanup() {
        stopGameThread();
        syncManager.stop();
        if (playersListener != null) playersListener.remove();
    }
}
