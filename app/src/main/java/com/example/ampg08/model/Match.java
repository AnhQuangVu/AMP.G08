package com.example.ampg08.model;

public class Match {
    private String matchId;
    private String roomId;
    private String winnerUid;
    private String winnerName;
    private long duration; // ms
    private long startedAt;
    private long endedAt;

    public Match() {}

    public Match(String matchId, String roomId, String winnerUid, String winnerName, long duration) {
        this.matchId = matchId;
        this.roomId = roomId;
        this.winnerUid = winnerUid;
        this.winnerName = winnerName;
        this.duration = duration;
        this.endedAt = System.currentTimeMillis();
        this.startedAt = endedAt - duration;
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getWinnerUid() { return winnerUid; }
    public void setWinnerUid(String winnerUid) { this.winnerUid = winnerUid; }

    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public long getEndedAt() { return endedAt; }
    public void setEndedAt(long endedAt) { this.endedAt = endedAt; }
}
