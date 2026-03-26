package com.example.ampg08.model;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String roomId;
    private String hostUid;
    private String status; // "waiting" | "playing" | "ended"
    private long mapSeed;
    private List<String> players;
    private long createdAt;

    public Room() { players = new ArrayList<>(); }

    public Room(String roomId, String hostUid, long mapSeed) {
        this.roomId = roomId;
        this.hostUid = hostUid;
        this.status = "waiting";
        this.mapSeed = mapSeed;
        this.players = new ArrayList<>();
        this.players.add(hostUid);
        this.createdAt = System.currentTimeMillis();
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getHostUid() { return hostUid; }
    public void setHostUid(String hostUid) { this.hostUid = hostUid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getMapSeed() { return mapSeed; }
    public void setMapSeed(long mapSeed) { this.mapSeed = mapSeed; }

    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
