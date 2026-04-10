package com.example.ampg08.model;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String roomId;
    private String hostUid;
    private long mapSeed;
    private int playerLimit;
    private int timeLimitSeconds;
    private String aiDifficulty;
    private String mode;
    private String status; // "waiting", "playing", "ended"
    private List<String> players;
    private long createdAt;

    public Room() {
        this.players = new ArrayList<>();
        this.playerLimit = 2;
        this.timeLimitSeconds = 180;
        this.aiDifficulty = "EASY";
        this.mode = "VS_PLAYER";
    }

    public Room(String roomId, String hostUid, long mapSeed) {
        this.roomId = roomId;
        this.hostUid = hostUid;
        this.mapSeed = mapSeed;
        this.playerLimit = 2;
        this.timeLimitSeconds = 180;
        this.aiDifficulty = "EASY";
        this.mode = "VS_PLAYER";
        this.status = "waiting";
        this.players = new ArrayList<>();
        this.players.add(hostUid);
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getRoomId() { return roomId; }
    public String getHostUid() { return hostUid; }
    public long getMapSeed() { return mapSeed; }
    public int getPlayerLimit() { return playerLimit; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public String getAiDifficulty() { return aiDifficulty; }
    public String getMode() { return mode; }
    public String getStatus() { return status; }
    public List<String> getPlayers() { return players; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setHostUid(String hostUid) { this.hostUid = hostUid; }
    public void setMapSeed(long mapSeed) { this.mapSeed = mapSeed; }
    public void setPlayerLimit(int playerLimit) { this.playerLimit = playerLimit; }
    public void setTimeLimitSeconds(int timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }
    public void setAiDifficulty(String aiDifficulty) { this.aiDifficulty = aiDifficulty; }
    public void setMode(String mode) { this.mode = mode; }
    public void setStatus(String status) { this.status = status; }
    public void setPlayers(List<String> players) { this.players = players; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}