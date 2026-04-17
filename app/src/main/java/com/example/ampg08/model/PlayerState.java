package com.example.ampg08.model;

public class PlayerState {
    private String uid;
    private String displayName;
    private float x;
    private float y;
    private float mapX;
    private float mapY;
    private boolean ready;
    private long finishTime;
    private boolean freezeRequested;
    private long updatedAt;

    public PlayerState() {}

    public PlayerState(String uid, String displayName) {
        this.uid = uid;
        this.displayName = displayName;
        this.x = 0;
        this.y = 0;
        this.mapX = -1f;
        this.mapY = -1f;
        this.ready = false;
        this.finishTime = 0;
        this.freezeRequested = false;
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters
    public String getUid() { return uid; }
    public String getDisplayName() { return displayName; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getMapX() { return mapX; }
    public float getMapY() { return mapY; }
    public boolean isReady() { return ready; }
    public long getFinishTime() { return finishTime; }
    public boolean isFreezeRequested() { return freezeRequested; }
    public long getUpdatedAt() { return updatedAt; }

    // Setters
    public void setUid(String uid) { this.uid = uid; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setMapX(float mapX) { this.mapX = mapX; }
    public void setMapY(float mapY) { this.mapY = mapY; }
    public void setReady(boolean ready) { this.ready = ready; }
    public void setFinishTime(long finishTime) { this.finishTime = finishTime; }
    public void setFreezeRequested(boolean freezeRequested) { this.freezeRequested = freezeRequested; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}