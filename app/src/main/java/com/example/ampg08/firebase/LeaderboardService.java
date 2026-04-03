package com.example.ampg08.firebase;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class LeaderboardService {

    private static LeaderboardService instance;

    private LeaderboardService() {}

    public static synchronized LeaderboardService getInstance() {
        if (instance == null) {
            instance = new LeaderboardService();
        }
        return instance;
    }

    public interface LeaderboardCallback {
        void onResult(List<DocumentSnapshot> docs);
    }

    public void getTop20(LeaderboardCallback callback) {
        FirestoreManager.getInstance().getLeaderboard(callback::onResult);
    }

    public void incrementWins(String uid, String displayName) {
        FirestoreManager.getInstance().incrementWins(uid, displayName);
    }

    public void incrementTotalMatches(String uid) {
        FirestoreManager.getInstance().incrementTotalMatches(uid);
    }
}