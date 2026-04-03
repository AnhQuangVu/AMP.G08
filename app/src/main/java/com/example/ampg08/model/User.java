package com.example.ampg08.model;

public class User {
    private String uid;
    private String displayName;
    private String avatarUrl;
    private int totalWins;
    private int totalMatches;

    public User() {}

    public User(String uid, String displayName, String avatarUrl, int totalWins, int totalMatches) {
        this.uid = uid;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.totalWins = totalWins;
        this.totalMatches = totalMatches;
    }

    // Getters
    public String getUid() { return uid; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public int getTotalWins() { return totalWins; }
    public int getTotalMatches() { return totalMatches; }

    // Setters
    public void setUid(String uid) { this.uid = uid; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }

    public float getWinRate() {
        if (totalMatches == 0) return 0f;
        return (float) totalWins / totalMatches * 100f;
    }
}