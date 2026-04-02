package com.example.ampg08.firebase;

import com.example.ampg08.model.PlayerState;
import com.example.ampg08.model.Room;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.EventListener;

import java.util.*;

public class RoomService {
    private static RoomService instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static RoomService getInstance() {
        if (instance == null) instance = new RoomService();
        return instance;
    }

    private CollectionReference roomsRef() { return db.collection("rooms"); }

    public Task<Void> createRoom(Room room) {
        return roomsRef().document(room.getRoomId()).set(room);
    }

    public Task<Void> joinRoom(String roomId, PlayerState player) {
        DocumentReference playerDoc = roomsRef().document(roomId)
                .collection("players")
                .document(player.getUid());
        return playerDoc.set(player, SetOptions.merge());
    }

    public ListenerRegistration listenRoom(String roomId, EventListener<DocumentSnapshot> listener) {
        return roomsRef().document(roomId).addSnapshotListener(listener);
    }

    public ListenerRegistration listenPlayers(String roomId, EventListener<QuerySnapshot> listener) {
        return roomsRef().document(roomId).collection("players").addSnapshotListener(listener);
    }

    public Task<Void> setReady(String roomId, String uid, boolean ready) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("readyMap." + uid, ready);
        return roomsRef().document(roomId).update(updates);
    }

    public Task<Void> startGame(String roomId, long seed) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("started", true);
        updates.put("mapSeed", seed);
        updates.put("startedAt", FieldValue.serverTimestamp());
        return roomsRef().document(roomId).update(updates);
    }

    public Task<Void> updatePlayerPosition(String roomId, String uid, float x, float y) {
        Map<String, Object> m = new HashMap<>();
        m.put("x", x);
        m.put("y", y);
        m.put("updatedAt", FieldValue.serverTimestamp());
        return roomsRef().document(roomId).collection("players").document(uid).update(m);
    }

    public Task<Void> setFrozen(String roomId, String targetUid, long frozenUntilMs) {
        Map<String, Object> m = new HashMap<>();
        m.put("frozen", true);
        m.put("frozenUntilMs", frozenUntilMs);
        m.put("updatedAt", FieldValue.serverTimestamp());
        return roomsRef().document(roomId).collection("players").document(targetUid).update(m);
    }

    public Task<Void> finishPlayer(String roomId, String uid, long finishTimeMs, int rank) {
        Map<String, Object> m = new HashMap<>();
        m.put("finished", true);
        m.put("finishTimeMs", finishTimeMs);
        m.put("finishRank", rank);
        m.put("updatedAt", FieldValue.serverTimestamp());
        return roomsRef().document(roomId).collection("players").document(uid).update(m);
    }

    public Task<Integer> getNextRank(String roomId) {
        return roomsRef().document(roomId).collection("players")
                .whereEqualTo("finished", true)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return 1;
                    return task.getResult().size() + 1;
                });
    }
}