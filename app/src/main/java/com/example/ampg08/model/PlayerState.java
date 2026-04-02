package com.example.ampg08.model;

import com.google.firebase.Timestamp;

public class PlayerState {
    private String uid;
    private String displayName;
    private float x;
    private float y;
    private boolean finished;
    private long finishTimeMs;
    private int finishRank; // 1,2,3...
    private Timestamp updatedAt;

    // freeze state
    private boolean frozen;
    private long frozenUntilMs;

    public PlayerState() {}

    public PlayerState(String uid, String displayName, float x, float y) {
        this.uid = uid;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.finished = false;
        this.finishTimeMs = 0;
        this.finishRank = 0;
        this.frozen = false;
        this.frozenUntilMs = 0;
        this.updatedAt = Timestamp.now();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }

    public long getFinishTimeMs() { return finishTimeMs; }
    public void setFinishTimeMs(long finishTimeMs) { this.finishTimeMs = finishTimeMs; }

    public int getFinishRank() { return finishRank; }
    public void setFinishRank(int finishRank) { this.finishRank = finishRank; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    public long getFrozenUntilMs() { return frozenUntilMs; }
    public void setFrozenUntilMs(long frozenUntilMs) { this.frozenUntilMs = frozenUntilMs; }
}