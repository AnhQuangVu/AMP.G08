package com.example.ampg08.model;

public class User {
    private String uid;
    private String displayName;
    private String avatar;
    private int totalWins;
    private int totalMatches;

    public User() {} // Firestore cần constructor rỗng

    public User(String uid, String displayName, String avatar, int totalWins, int totalMatches) {
        this.uid = uid;
        this.displayName = displayName;
        this.avatar = avatar;
        this.totalWins = totalWins;
        this.totalMatches = totalMatches;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }

    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }

    public float getWinRate() {
        if (totalMatches == 0) return 0f;
        return (float) totalWins / totalMatches * 100f;
    }
}
