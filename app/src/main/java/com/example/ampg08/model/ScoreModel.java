package com.example.ampg08.model;

public class ScoreModel {
    private String uid;
    private int rank;
    private String username;
    private int score;
    private String mapName;

    public ScoreModel(String uid, int rank, String username, int score, String mapName) {
        this.uid = uid;
        this.rank = rank;
        this.username = username;
        this.score = score;
        this.mapName = mapName;
    }

    public ScoreModel(int rank, String username, int score, String mapName) {
        this.uid = "";
        this.rank = rank;
        this.username = username;
        this.score = score;
        this.mapName = mapName;
    }

    public String getUid() { return uid; }
    public int getRank() { return rank; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
    public String getMapName() { return mapName; }
}