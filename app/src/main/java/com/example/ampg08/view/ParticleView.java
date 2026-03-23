package com.example.ampg08.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.example.ampg08.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleView extends View {
    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint();
    private final Random random = new Random();
    private static final int PARTICLE_COUNT = 40;

    public ParticleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(context.getColor(R.color.color_primary));
        paint.setStrokeWidth(2f);
        paint.setAlpha(100);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(random.nextInt(w), random.nextInt(h)));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Particle p : particles) {
            p.move(getWidth(), getHeight());
            canvas.drawCircle(p.x, p.y, p.radius, paint);
        }
        invalidate(); // Animate
    }

    private class Particle {
        float x, y, radius, vx, vy;
        Particle(float x, float y) {
            this.x = x; this.y = y;
            this.radius = random.nextFloat() * 4 + 2;
            this.vx = (random.nextFloat() - 0.5f) * 2;
            this.vy = (random.nextFloat() - 0.5f) * 2;
        }
        void move(int w, int h) {
            x += vx; y += vy;
            if (x < 0 || x > w) vx *= -1;
            if (y < 0 || y > h) vy *= -1;
        }
    }
}