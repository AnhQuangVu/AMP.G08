package com.example.ampg08.model;

public class PlayerState {
    private String uid;
    private String displayName;
    private float x;
    private float y;
    private long finishTime; // ms kể từ khi game bắt đầu, 0 nếu chưa về đích
    private boolean ready;
    private boolean frozen;     // đang bị đóng băng bởi skill
    private long frozenUntil;   // timestamp hết hiệu lực freeze

    public PlayerState() {}

    public PlayerState(String uid, String displayName) {
        this.uid = uid;
        this.displayName = displayName;
        this.x = 0;
        this.y = 0;
        this.finishTime = 0;
        this.ready = false;
        this.frozen = false;
        this.frozenUntil = 0;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public long getFinishTime() { return finishTime; }
    public void setFinishTime(long finishTime) { this.finishTime = finishTime; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    public long getFrozenUntil() { return frozenUntil; }
    public void setFrozenUntil(long frozenUntil) { this.frozenUntil = frozenUntil; }
}
