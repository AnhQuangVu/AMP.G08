package com.example.ampg08.model;

public class Match {
    private String matchId;
    private String roomId;
    private String winnerUid;
    private String winnerName;
    private long winnerTime;
    private long createdAt;

    public Match() {}

    public Match(String matchId, String roomId, String winnerUid, String winnerName, long winnerTime) {
        this.matchId = matchId;
        this.roomId = roomId;
        this.winnerUid = winnerUid;
        this.winnerName = winnerName;
        this.winnerTime = winnerTime;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getMatchId() { return matchId; }
    public String getRoomId() { return roomId; }
    public String getWinnerUid() { return winnerUid; }
    public String getWinnerName() { return winnerName; }
    public long getWinnerTime() { return winnerTime; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setWinnerUid(String winnerUid) { this.winnerUid = winnerUid; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
    public void setWinnerTime(long winnerTime) { this.winnerTime = winnerTime; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}