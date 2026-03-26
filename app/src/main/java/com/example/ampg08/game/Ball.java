package com.example.ampg08.game;

public class Ball {

    public float x, y;         // Vị trí tâm bóng (pixel)
    public float vx, vy;       // Vận tốc (pixel/s)
    public float radius;       // Bán kính (pixel)
    public boolean boosted;    // Đang skill tăng tốc
    public long boostUntil;    // Timestamp kết thúc boost

    private static final float FRICTION = 1f;
    private static final float MAX_SPEED = 1200f;   // pixel/s
    private static final float BOOST_MULTIPLIER = 2.2f;

    public Ball(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.vx = 0;
        this.vy = 0;
        this.boosted = false;
        this.boostUntil = 0;
    }

    public void update(float ax, float ay, float dt) {
        // Kiểm tra boost còn hiệu lực không
        float multiplier = 1f;
        if (boosted && System.currentTimeMillis() < boostUntil) {
            multiplier = BOOST_MULTIPLIER;
        } else {
            boosted = false;
        }
        float sensitivity = 200f * multiplier;
        vx += -ax * sensitivity * dt;
        vy +=  ay * sensitivity * dt;

        // Giới hạn tốc độ tối đa
        float speed = (float) Math.sqrt(vx * vx + vy * vy);
        if (speed > MAX_SPEED * multiplier) {
            vx = vx / speed * MAX_SPEED * multiplier;
            vy = vy / speed * MAX_SPEED * multiplier;
        }

        // Di chuyển
        x += vx * dt;
        y += vy * dt;

        // Ma sát
        vx *= FRICTION;
        vy *= FRICTION;
    }

    /** Kích hoạt boost 2 giây */
    public void activateBoost() {
        boosted = true;
        boostUntil = System.currentTimeMillis() + 2000;
    }

    public void resolveWallCollision(float wallLeft, float wallTop, float wallRight, float wallBottom) {
        // Trái/Phải
        if (x - radius < wallLeft) {
            x = wallLeft + radius;
            vx = Math.abs(vx) * 0.4f;
        } else if (x + radius > wallRight) {
            x = wallRight - radius;
            vx = -Math.abs(vx) * 0.4f;
        }
        // Trên/Dưới
        if (y - radius < wallTop) {
            y = wallTop + radius;
            vy = Math.abs(vy) * 0.4f;
        } else if (y + radius > wallBottom) {
            y = wallBottom - radius;
            vy = -Math.abs(vy) * 0.4f;
        }
    }
}
