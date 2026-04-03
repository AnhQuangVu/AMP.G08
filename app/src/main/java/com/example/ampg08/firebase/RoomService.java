package com.example.ampg08.firebase;

public class RoomService {

    private static RoomService instance;

    private RoomService() {}

    public static synchronized RoomService getInstance() {
        if (instance == null) {
            instance = new RoomService();
        }
        return instance;
    }

    public void updatePlayerPosition(String roomId, String uid, float x, float y) {
        FirestoreManager.getInstance().updatePlayerPosition(roomId, uid, x, y);
    }

    public void updatePlayerFinished(String roomId, String uid, long finishTime,
                                     FirestoreManager.OnCompleteCallback callback) {
        FirestoreManager.getInstance().updatePlayerFinish(roomId, uid, finishTime, callback);
    }
}