package com.example.ampg08.model;

import com.google.firebase.Timestamp;

public class MatchResult {
    private String roomId;
    private String uid;
    private String displayName;
    private int rank;
    private long finishTimeMs;
    private boolean offline;
    private Timestamp createdAt;

    public MatchResult() {}

    public MatchResult(String roomId, String uid, String displayName, int rank, long finishTimeMs, boolean offline) {
        this.roomId = roomId;
        this.uid = uid;
        this.displayName = displayName;
        this.rank = rank;
        this.finishTimeMs = finishTimeMs;
        this.offline = offline;
        this.createdAt = Timestamp.now();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public long getFinishTimeMs() {
        return finishTimeMs;
    }

    public void setFinishTimeMs(long finishTimeMs) {
        this.finishTimeMs = finishTimeMs;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}