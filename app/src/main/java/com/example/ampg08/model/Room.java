package com.example.ampg08.model;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String roomId;
    private String hostUid;
    private long mapSeed;
    private String status; // "waiting", "playing", "ended"
    private List<String> players;
    private long createdAt;

    public Room() {
        this.players = new ArrayList<>();
    }

    public Room(String roomId, String hostUid, long mapSeed) {
        this.roomId = roomId;
        this.hostUid = hostUid;
        this.mapSeed = mapSeed;
        this.status = "waiting";
        this.players = new ArrayList<>();
        this.players.add(hostUid);
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getRoomId() { return roomId; }
    public String getHostUid() { return hostUid; }
    public long getMapSeed() { return mapSeed; }
    public String getStatus() { return status; }
    public List<String> getPlayers() { return players; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setHostUid(String hostUid) { this.hostUid = hostUid; }
    public void setMapSeed(long mapSeed) { this.mapSeed = mapSeed; }
    public void setStatus(String status) { this.status = status; }
    public void setPlayers(List<String> players) { this.players = players; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}