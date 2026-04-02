package com.example.ampg08.model;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Room {
    private String roomId;
    private String hostUid;
    private long mapSeed;
    private boolean started;
    private Timestamp createdAt;
    private Timestamp startedAt;

    // uid -> ready
    private Map<String, Boolean> readyMap = new HashMap<>();

    public Room() {}

    public Room(String roomId, String hostUid, long mapSeed) {
        this.roomId = roomId;
        this.hostUid = hostUid;
        this.mapSeed = mapSeed;
        this.started = false;
        this.createdAt = Timestamp.now();
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getHostUid() { return hostUid; }
    public void setHostUid(String hostUid) { this.hostUid = hostUid; }

    public long getMapSeed() { return mapSeed; }
    public void setMapSeed(long mapSeed) { this.mapSeed = mapSeed; }

    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }

    public Map<String, Boolean> getReadyMap() { return readyMap; }
    public void setReadyMap(Map<String, Boolean> readyMap) { this.readyMap = readyMap; }
}