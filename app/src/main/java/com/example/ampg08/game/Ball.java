package com.example.ampg08.game;

public class Ball {
    public float x, y;
    public float vx, vy;
    public float radius;

    private static final float BASE_SENSITIVITY = 200f;
    private static final float MAX_SPEED = 1200f;
    private static final float FRICTION_PER_FRAME_60FPS = 0.96f;
    private static final float BOOST_MULTIPLIER = 2.2f;

    private boolean boosted = false;
    private long boostUntilMs = 0L;

    private boolean frozen = false;
    private long frozenUntilMs = 0L;

    public Ball(float x, float y, float radius) {
        this.x = x; this.y = y; this.radius = radius;
        this.vx = 0f; this.vy = 0f;
    }
    public boolean isBoosted(long nowMs) {
        return boosted && nowMs < boostUntilMs;
    }
    public void activateBoost(long nowMs, long durationMs) {
        boosted = true;
        boostUntilMs = nowMs + Math.max(0L, durationMs);
    }

    public void applyFreeze(long nowMs, long durationMs) {
        frozen = true;
        frozenUntilMs = nowMs + Math.max(0L, durationMs);
    }

    public boolean isFrozen(long nowMs) {
        if (frozen && nowMs >= frozenUntilMs) frozen = false;
        return frozen;
    }

    public void update(float ax, float ay, float dt, long nowMs) {
        if (isFrozen(nowMs)) {
            vx = 0f; vy = 0f;
            return;
        }

        float boostMul = 1f;
        if (boosted && nowMs < boostUntilMs) boostMul = BOOST_MULTIPLIER;
        else boosted = false;

        vx += (-ax) * BASE_SENSITIVITY * boostMul * dt;
        vy += ( ay) * BASE_SENSITIVITY * boostMul * dt;

        float speed = (float) Math.sqrt(vx * vx + vy * vy);
        float max = MAX_SPEED * boostMul;
        if (speed > max) {
            vx = (vx / speed) * max;
            vy = (vy / speed) * max;
        }

        x += vx * dt;
        y += vy * dt;

        // convert friction from per-frame(60fps) to dt
        float frameScale = dt / (1f / 60f);
        float friction = (float) Math.pow(FRICTION_PER_FRAME_60FPS, frameScale);
        vx *= friction;
        vy *= friction;
    }
}