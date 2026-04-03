package com.example.ampg08.game;

public class SkillController {

    private static final long COOLDOWN_MS = 5000L;  // 5 giây cooldown
    private static final long BOOST_DURATION_MS = 2000L;
    private static final long FREEZE_DURATION_MS = 3000L;

    private long lastUsedTimeMs = 0L;
    private boolean isOnCooldown = false;

    public SkillController() {
    }

    public boolean canUseSkill() {
        return !isOnCooldown;
    }

    public void activateSkill() {
        lastUsedTimeMs = System.currentTimeMillis();
        isOnCooldown = true;
    }

    public void update(long nowMs) {
        if (isOnCooldown && nowMs - lastUsedTimeMs >= COOLDOWN_MS) {
            isOnCooldown = false;
        }
    }

    public float getCooldownProgress() {
        if (!isOnCooldown) return 1f;

        long elapsed = System.currentTimeMillis() - lastUsedTimeMs;
        return Math.min(1f, (float) elapsed / COOLDOWN_MS);
    }

    public long getBoostDuration() {
        return BOOST_DURATION_MS;
    }

    public long getFreezeDuration() {
        return FREEZE_DURATION_MS;
    }

    public long getCooldownMs() {
        return COOLDOWN_MS;
    }
}