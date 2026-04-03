package com.example.ampg08.firebase;

import com.example.ampg08.model.Match;
import com.example.ampg08.model.PlayerState;
import com.example.ampg08.model.Room;
import com.example.ampg08.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {

    private static FirestoreManager instance;
    private final FirebaseFirestore db;

    public static final String COL_USERS       = "users";
    public static final String COL_ROOMS       = "rooms";
    public static final String COL_PLAYERS     = "players";
    public static final String COL_MATCHES     = "matches";
    public static final String COL_LEADERBOARD = "leaderboard";

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirestoreManager getInstance() {
        if (instance == null) instance = new FirestoreManager();
        return instance;
    }

    // ─── USERS ──────────────────────────────────────────────────────────

    public void createUser(User user, OnCompleteCallback callback) {
        db.collection(COL_USERS).document(user.getUid())
                .set(user)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getUser(String uid, OnUserCallback callback) {
        db.collection(COL_USERS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) callback.onUser(doc.toObject(User.class));
                    else callback.onUser(null);
                })
                .addOnFailureListener(e -> callback.onUser(null));
    }

    public void updateDisplayName(String uid, String displayName, OnCompleteCallback callback) {
        db.collection(COL_USERS).document(uid)
                .update("displayName", displayName)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ─── ROOMS ──────────────────────────────────────────────────────────

    public void createRoom(Room room, OnRoomCallback callback) {
        DocumentReference ref = db.collection(COL_ROOMS).document();
        room.setRoomId(ref.getId());
        ref.set(room)
                .addOnSuccessListener(v -> callback.onRoom(room))
                .addOnFailureListener(e -> callback.onRoom(null));
    }

    public void getRoomByCode(String roomId, OnRoomCallback callback) {
        db.collection(COL_ROOMS).document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) callback.onRoom(doc.toObject(Room.class));
                    else callback.onRoom(null);
                })
                .addOnFailureListener(e -> callback.onRoom(null));
    }

    public void joinRoom(String roomId, String uid, OnCompleteCallback callback) {
        db.collection(COL_ROOMS).document(roomId)
                .update("players", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void setRoomStatus(String roomId, String status, OnCompleteCallback callback) {
        db.collection(COL_ROOMS).document(roomId)
                .update("status", status)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public ListenerRegistration listenRoom(String roomId, OnRoomCallback callback) {
        return db.collection(COL_ROOMS).document(roomId)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null && snap.exists()) callback.onRoom(snap.toObject(Room.class));
                });
    }

    // ─── PLAYER STATES trong room ────────────────────────────────────────

    public void setPlayerState(String roomId, PlayerState state, OnCompleteCallback callback) {
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(state.getUid())
                .set(state)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updatePlayerPosition(String roomId, String uid, float x, float y) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", x);
        data.put("y", y);
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(uid)
                .update(data);
    }

    public void updatePlayerFinish(String roomId, String uid, long finishTime, OnCompleteCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("finishTime", finishTime);
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(uid)
                .update(data)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void setPlayerReady(String roomId, String uid, boolean ready) {
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(uid)
                .update("ready", ready);
    }

    public ListenerRegistration listenPlayers(String roomId, OnPlayersCallback callback) {
        return db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) {
                        List<PlayerState> players = new ArrayList<>();
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            PlayerState ps = doc.toObject(PlayerState.class);
                            if (ps != null) players.add(ps);
                        }
                        callback.onPlayers(players);
                    }
                });
    }

    // ─── FREEZE COMMAND ─────────────────────────────────────────────────

    public void sendFreezeCommand(String roomId, String targetUid) {
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(targetUid)
                .update("freezeRequested", true);
    }

    public void clearFreezeFlag(String roomId, String uid) {
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(uid)
                .update("freezeRequested", false);
    }

    // ─── MATCHES ────────────────────────────────────────────────────────

    public void saveMatch(Match match, OnCompleteCallback callback) {
        db.collection(COL_MATCHES).document(match.getMatchId())
                .set(match)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ─── LEADERBOARD ────────────────────────────────────────────────────

    public void getLeaderboard(OnLeaderboardCallback callback) {
        db.collection(COL_LEADERBOARD)
                .orderBy("wins", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snap -> callback.onResult(snap.getDocuments()))
                .addOnFailureListener(e -> callback.onResult(null));
    }

    public void incrementWins(String uid, String displayName) {
        DocumentReference ref = db.collection(COL_LEADERBOARD).document(uid);
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(ref);
            if (snap.exists()) {
                long wins = snap.getLong("wins") != null ? snap.getLong("wins") : 0;
                long total = snap.getLong("totalMatches") != null ? snap.getLong("totalMatches") : 0;
                transaction.update(ref, "wins", wins + 1, "totalMatches", total + 1);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("uid", uid);
                data.put("displayName", displayName);
                data.put("wins", 1);
                data.put("totalMatches", 1);
                transaction.set(ref, data);
            }
            return null;
        });

        // Update user collection too
        db.collection(COL_USERS).document(uid)
                .update("totalWins", com.google.firebase.firestore.FieldValue.increment(1),
                        "totalMatches", com.google.firebase.firestore.FieldValue.increment(1));
    }

    public void incrementTotalMatches(String uid) {
        db.collection(COL_LEADERBOARD).document(uid)
                .update("totalMatches", com.google.firebase.firestore.FieldValue.increment(1));

        db.collection(COL_USERS).document(uid)
                .update("totalMatches", com.google.firebase.firestore.FieldValue.increment(1));
    }

    // ─── CALLBACKS ──────────────────────────────────────────────────────

    public interface OnCompleteCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnUserCallback {
        void onUser(User user);
    }

    public interface OnRoomCallback {
        void onRoom(Room room);
    }

    public interface OnPlayersCallback {
        void onPlayers(List<PlayerState> players);
    }

    public interface OnLeaderboardCallback {
        void onResult(List<DocumentSnapshot> docs);
    }
}
