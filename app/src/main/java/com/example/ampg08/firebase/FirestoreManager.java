package com.example.ampg08.firebase;

import android.util.Log;

import com.example.ampg08.model.Match;
import com.example.ampg08.model.PlayerState;
import com.example.ampg08.model.Room;
import com.example.ampg08.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FirestoreManager {

    private static final String TAG = "FirestoreManager";

    private static FirestoreManager instance;
    private final FirebaseFirestore db;

    public static final String COL_USERS       = "users";
    public static final String COL_ROOMS       = "rooms";
    public static final String COL_PLAYERS     = "players";
    public static final String COL_MATCHES     = "matches";
    public static final String COL_LEADERBOARD = "leaderboard";
    public static final String COL_FRIENDS     = "friends";

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();

        // Bật offline persistence (giúp app tiếp tục hoạt động khi mất mạng ngắn)
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
    }

    public static FirestoreManager getInstance() {
        if (instance == null) instance = new FirestoreManager();
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    // ─── USERS ──────────────────────────────────────────────────────────

    public void createUser(User user, OnCompleteCallback callback) {
        db.collection(COL_USERS).document(user.getUid())
                .set(user)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createUser failed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void getUser(String uid, OnUserCallback callback) {
        db.collection(COL_USERS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    callback.onUser(doc.exists() ? doc.toObject(User.class) : null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "getUser failed", e);
                    callback.onUser(null);
                });
    }

    public void updateDisplayName(String uid, String displayName, OnCompleteCallback callback) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("uid", uid);
        patch.put("displayName", displayName);

        // Dùng merge để vừa hỗ trợ user doc cũ thiếu uid, vừa tự tạo doc nếu chưa có.
        db.collection(COL_USERS).document(uid)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    // Best-effort update leaderboard display name
                    db.collection(COL_LEADERBOARD).document(uid)
                            .update("displayName", displayName)
                            .addOnFailureListener(e -> Log.w(TAG, "leaderboard name update skipped"));
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateDisplayName failed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void updateAvatarUrl(String uid, String avatarUrl, OnCompleteCallback callback) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("uid", uid);
        patch.put("avatarUrl", avatarUrl);

        db.collection(COL_USERS).document(uid)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getFriendUids(String uid, OnFriendUidsCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        Set<String> merged = new HashSet<>();
        AtomicInteger pending = new AtomicInteger(2);

        db.collection(COL_USERS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    mergeFriendArrayField(doc, "friends", merged);
                    mergeFriendArrayField(doc, "friendUids", merged);
                    if (pending.decrementAndGet() == 0) {
                        callback.onResult(new ArrayList<>(merged));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "getFriendUids users doc failed", e);
                    if (pending.decrementAndGet() == 0) {
                        callback.onResult(new ArrayList<>(merged));
                    }
                });

        db.collection(COL_USERS).document(uid)
                .collection(COL_FRIENDS)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot friendDoc : snap.getDocuments()) {
                        String friendUid = friendDoc.getString("uid");
                        if (friendUid == null || friendUid.trim().isEmpty()) {
                            friendUid = friendDoc.getId();
                        }
                        if (friendUid != null && !friendUid.trim().isEmpty()) {
                            merged.add(friendUid);
                        }
                    }
                    if (pending.decrementAndGet() == 0) {
                        callback.onResult(new ArrayList<>(merged));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "getFriendUids subcollection failed", e);
                    if (pending.decrementAndGet() == 0) {
                        callback.onResult(new ArrayList<>(merged));
                    }
                });
    }

    private void mergeFriendArrayField(DocumentSnapshot doc, String field, Set<String> out) {
        Object value = doc.get(field);
        if (!(value instanceof List<?>)) return;

        for (Object item : (List<?>) value) {
            if (item instanceof String) {
                String uid = ((String) item).trim();
                if (!uid.isEmpty()) out.add(uid);
            }
        }
    }

    // ─── ROOMS ──────────────────────────────────────────────────────────

    public void createRoom(Room room, OnRoomCallback callback) {
        DocumentReference ref = db.collection(COL_ROOMS).document();
        room.setRoomId(ref.getId());
        ref.set(room)
                .addOnSuccessListener(v -> callback.onRoom(room))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createRoom failed", e);
                    callback.onRoom(null);
                });
    }

    public void getRoomByCode(String roomId, OnRoomCallback callback) {
        db.collection(COL_ROOMS).document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    callback.onRoom(doc.exists() ? doc.toObject(Room.class) : null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "getRoomByCode failed", e);
                    callback.onRoom(null);
                });
    }

    public void joinRoom(String roomId, String uid, OnCompleteCallback callback) {
        db.collection(COL_ROOMS).document(roomId)
                .update("players", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "joinRoom failed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void setRoomStatus(String roomId, String status, OnCompleteCallback callback) {
        db.collection(COL_ROOMS).document(roomId)
                .update("status", status)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "setRoomStatus failed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void leaveRoom(String roomId, String uid, OnCompleteCallback callback) {
        if (roomId == null || uid == null) return;
        // 1. Remove UID from 'players' array in room document
        db.collection(COL_ROOMS).document(roomId)
                .update("players", com.google.firebase.firestore.FieldValue.arrayRemove(uid))
                .addOnCompleteListener(task -> {
                    // 2. Delete player state sub-document
                    db.collection(COL_ROOMS).document(roomId)
                            .collection(COL_PLAYERS).document(uid)
                            .delete();
                    
                    if (callback != null) {
                        if (task.isSuccessful()) callback.onSuccess();
                        else callback.onFailure("Failed to leave room");
                    }
                });
    }

    public ListenerRegistration listenRoom(String roomId, OnRoomCallback callback) {
        return db.collection(COL_ROOMS).document(roomId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { Log.w(TAG, "listenRoom error", e); return; }
                    if (snap != null && snap.exists()) callback.onRoom(snap.toObject(Room.class));
                });
    }

    // ─── PLAYER STATES ──────────────────────────────────────────────────

    public void setPlayerState(String roomId, PlayerState state, OnCompleteCallback callback) {
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(state.getUid())
                .set(state)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "setPlayerState failed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Cập nhật vị trí bóng — không có callback vì được gọi 10 lần/giây.
     * Lỗi chỉ log cảnh báo để tránh crash từ callback bất đồng bộ.
     */
    public void updatePlayerPosition(String roomId, String uid, float x, float y, float mapX, float mapY) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", x);
        data.put("y", y);
        data.put("mapX", mapX);
        data.put("mapY", mapY);
        data.put("updatedAt", System.currentTimeMillis());
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(uid)
                .update(data)
                .addOnFailureListener(e -> Log.w(TAG, "updatePlayerPosition failed", e));
    }

    public void updatePlayerFinish(String roomId, String uid, long finishTime,
                                   OnCompleteCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("finishTime", finishTime);
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(uid)
                .update(data)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updatePlayerFinish failed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void setPlayerReady(String roomId, String uid, boolean ready) {
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(uid)
                .update("ready", ready)
                .addOnFailureListener(e -> Log.w(TAG, "setPlayerReady failed", e));
    }

    public ListenerRegistration listenPlayers(String roomId, OnPlayersCallback callback) {
        return db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { Log.w(TAG, "listenPlayers error", e); return; }
                    if (snap == null) return;

                    List<PlayerState> players = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        PlayerState ps = doc.toObject(PlayerState.class);
                        if (ps != null) players.add(ps);
                    }
                    callback.onPlayers(players);
                });
    }

    // ─── FREEZE ─────────────────────────────────────────────────────────

    public void sendFreezeCommand(String roomId, String targetUid) {
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(targetUid)
                .update("freezeRequested", true)
                .addOnFailureListener(e -> Log.w(TAG, "sendFreezeCommand failed", e));
    }

    public void clearFreezeFlag(String roomId, String uid) {
        db.collection(COL_ROOMS).document(roomId)
                .collection(COL_PLAYERS).document(uid)
                .update("freezeRequested", false)
                .addOnFailureListener(e -> Log.w(TAG, "clearFreezeFlag failed", e));
    }

    // ─── MATCHES ────────────────────────────────────────────────────────

    public void saveMatch(Match match, OnCompleteCallback callback) {
        db.collection(COL_MATCHES).document(match.getMatchId())
                .set(match)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveMatch failed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // ─── LEADERBOARD ────────────────────────────────────────────────────

    public void getLeaderboard(OnLeaderboardCallback callback) {
        db.collection(COL_LEADERBOARD)
                .orderBy("wins", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snap -> callback.onResult(snap.getDocuments()))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "getLeaderboard failed", e);
                    callback.onResult(null);
                });
    }

    public void getLeaderboardByUids(List<String> uids, OnLeaderboardCallback callback) {
        if (uids == null || uids.isEmpty()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        List<String> deduped = new ArrayList<>(new HashSet<>(uids));
        final int chunkSize = 10;
        final int totalChunks = (deduped.size() + chunkSize - 1) / chunkSize;

        List<DocumentSnapshot> collected = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger done = new AtomicInteger(0);
        for (int start = 0; start < deduped.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, deduped.size());
            List<String> chunk = deduped.subList(start, end);

            db.collection(COL_LEADERBOARD)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(snap -> {
                        for (QueryDocumentSnapshot doc : snap) {
                            collected.add(doc);
                        }
                        if (done.incrementAndGet() == totalChunks) {
                            callback.onResult(sortLeaderboardDocs(collected));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "getLeaderboardByUids chunk failed", e);
                        if (done.incrementAndGet() == totalChunks) {
                            callback.onResult(sortLeaderboardDocs(collected));
                        }
                    });
        }
    }

    private List<DocumentSnapshot> sortLeaderboardDocs(List<DocumentSnapshot> docs) {
        List<DocumentSnapshot> sorted = new ArrayList<>(docs);
        sorted.sort((a, b) -> {
            Long awObj = a.getLong("wins");
            Long bwObj = b.getLong("wins");
            long aw = awObj != null ? awObj : 0L;
            long bw = bwObj != null ? bwObj : 0L;
            if (bw != aw) return Long.compare(bw, aw);

            Long amObj = a.getLong("totalMatches");
            Long bmObj = b.getLong("totalMatches");
            long am = amObj != null ? amObj : 0L;
            long bm = bmObj != null ? bmObj : 0L;
            return Long.compare(bm, am);
        });
        return sorted;
    }

    public void incrementWins(String uid, String displayName) {
        DocumentReference ref = db.collection(COL_LEADERBOARD).document(uid);
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(ref);
            if (snap.exists()) {
                Long winsObj = snap.getLong("wins");
                Long totalObj = snap.getLong("totalMatches");
                long wins  = winsObj != null ? winsObj : 0L;
                long total = totalObj != null ? totalObj : 0L;
                transaction.update(ref, "wins", wins + 1,
                        "totalMatches", total + 1,
                        "displayName", displayName); // sync tên mới nhất
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("uid", uid);
                data.put("displayName", displayName);
                data.put("wins", 1L);
                data.put("totalMatches", 1L);
                transaction.set(ref, data);
            }
            return null;
        }).addOnFailureListener(e -> Log.e(TAG, "incrementWins transaction failed", e));

        // Cập nhật users collection song song
        db.collection(COL_USERS).document(uid)
                .update("totalWins",    com.google.firebase.firestore.FieldValue.increment(1),
                        "totalMatches", com.google.firebase.firestore.FieldValue.increment(1))
                .addOnFailureListener(e -> Log.w(TAG, "incrementWins users update failed", e));
    }

    public void incrementTotalMatches(String uid) {
        db.collection(COL_LEADERBOARD).document(uid)
                .update("totalMatches", com.google.firebase.firestore.FieldValue.increment(1))
                .addOnFailureListener(e -> Log.w(TAG, "incrementTotalMatches leaderboard failed", e));

        db.collection(COL_USERS).document(uid)
                .update("totalMatches", com.google.firebase.firestore.FieldValue.increment(1))
                .addOnFailureListener(e -> Log.w(TAG, "incrementTotalMatches users failed", e));
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

    // ─── MATCHMAKING ────────────────────────────────────────────────────

    private static final String COL_MATCHMAKING = "matchmaking";
    private static final String DOC_POOL        = "pool";

    /**
     * Tham gia hàng chờ ghép trận.
     * - Nếu pool trống: ghi uid vào, chờ người thứ 2.
     * - Nếu pool đã có người: lấy uid đó, tạo phòng cho cả 2, ghi roomId vào pool.
     * Dùng transaction để tránh race condition.
     */
    public void joinMatchmakingPool(String uid, String displayName, OnMatchmakingCallback callback) {
        DocumentReference poolRef = db.collection(COL_MATCHMAKING).document(DOC_POOL);

        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentSnapshot snap = tx.get(poolRef);

            String waitingUid     = snap.getString("waitingUid");
            String waitingName    = snap.getString("waitingName");

            if (waitingUid == null || waitingUid.isEmpty() || waitingUid.equals(uid)) {
                // Pool trống hoặc chính mình → ghi vào chờ và RESET sạch sẽ
                Map<String, Object> data = new HashMap<>();
                data.put("waitingUid", uid);
                data.put("waitingName", displayName);
                data.put("roomId", "");
                data.put("player1", "");
                data.put("player2", "");
                data.put("player1Name", "");
                data.put("player2Name", "");
                data.put("mapSeed", 0L);
                data.put("updatedAt", System.currentTimeMillis());
                tx.set(poolRef, data);
            } else {
                // Có người chờ → tạo phòng và match
                long seed = System.currentTimeMillis();
                DocumentReference roomRef = db.collection(COL_ROOMS).document();
                String newRoomId = roomRef.getId();

                Room room = new Room(newRoomId, uid, seed);
                room.getPlayers().add(waitingUid);
                tx.set(roomRef, room);

                Map<String, Object> poolData = new HashMap<>();
                poolData.put("roomId", newRoomId);
                poolData.put("mapSeed", seed);
                poolData.put("player1", waitingUid);
                poolData.put("player1Name", waitingName);
                poolData.put("player2", uid);
                poolData.put("player2Name", displayName);
                poolData.put("waitingUid", "");
                // Dùng thời gian hiện tại của hệ thống để khớp với searchStartTime
                poolData.put("updatedAt", System.currentTimeMillis());
                tx.set(poolRef, poolData);
            }
            return null;
        }).addOnSuccessListener(v -> callback.onJoined())
          .addOnFailureListener(e -> {
              Log.e(TAG, "joinMatchmakingPool failed", e);
              callback.onError(e.getMessage());
          });
    }

    /** Lắng nghe pool để biết khi nào được match. */
    public ListenerRegistration listenMatchmakingPool(OnMatchmakingPoolCallback callback) {
        return db.collection(COL_MATCHMAKING).document(DOC_POOL)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    String roomId  = snap.getString("roomId");
                    String p1      = snap.getString("player1");
                    String p2      = snap.getString("player2");
                    long   mapSeed = snap.getLong("mapSeed") != null ? snap.getLong("mapSeed") : 0L;
                    long updatedAt = snap.getLong("updatedAt") != null ? snap.getLong("updatedAt") : 0L;
                    callback.onUpdate(roomId, p1, p2, mapSeed, updatedAt);
                });
    }

    /** Rời hàng chờ (khi hủy). */
    public void leaveMatchmakingPool(String uid) {
        DocumentReference poolRef = db.collection(COL_MATCHMAKING).document(DOC_POOL);
        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentSnapshot snap = tx.get(poolRef);
            String waitingUid = snap.getString("waitingUid");
            if (uid.equals(waitingUid)) {
                Map<String, Object> data = new HashMap<>();
                data.put("waitingUid", "");
                data.put("waitingName", "");
                data.put("roomId", "");
                tx.set(poolRef, data);
            }
            return null;
        }).addOnFailureListener(e -> Log.w(TAG, "leaveMatchmakingPool failed", e));
    }

    public interface OnMatchmakingCallback {
        void onJoined();
        void onError(String error);
    }

    public interface OnMatchmakingPoolCallback {
        void onUpdate(String roomId, String p1, String p2, long mapSeed, long updatedAt);
    }

    public interface OnLeaderboardCallback {
        void onResult(List<DocumentSnapshot> docs);
    }

    public interface OnFriendUidsCallback {
        void onResult(List<String> uids);
    }
}

