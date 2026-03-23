package com.example.ampg08.model;

public class MapModel {
    private String id;
    private String name;
    private int difficulty; // 1 to 5
    private boolean isLocked;
    private int thumbnailResId;

    public MapModel(String id, String name, int difficulty, boolean isLocked, int thumbnailResId) {
        this.id = id;
        this.name = name;
        this.difficulty = difficulty;
        this.isLocked = isLocked;
        this.thumbnailResId = thumbnailResId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getDifficulty() { return difficulty; }
    public boolean isLocked() { return isLocked; }
    public int getThumbnailResId() { return thumbnailResId; }
}