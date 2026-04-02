package com.example.ampg08.game;

import com.example.ampg08.model.PlayerState;

import java.util.List;

public class SkillController {
    private static final long COOLDOWN_MS = 5000L;
    private static final long FREEZE_MS = 3000L;

    private long skillReadyAt = 0L;

    public boolean canUse(long nowMs) {
        return nowMs >= skillReadyAt;
    }

    public void markUsed(long nowMs) {
        skillReadyAt = nowMs + COOLDOWN_MS;
    }

    public long getCooldownRemaining(long nowMs) {
        return Math.max(0, skillReadyAt - nowMs);
    }

    public PlayerState findNearestOpponent(PlayerState me, List<PlayerState> all) {
        PlayerState nearest = null;
        double best = Double.MAX_VALUE;
        for (PlayerState p : all) {
            if (p.getUid().equals(me.getUid()) || p.isFinished()) continue;
            double dx = p.getX() - me.getX();
            double dy = p.getY() - me.getY();
            double d2 = dx * dx + dy * dy;
            if (d2 < best) {
                best = d2;
                nearest = p;
            }
        }
        return nearest;
    }

    public long calcFrozenUntil(long nowMs) {
        return nowMs + FREEZE_MS;
    }
}