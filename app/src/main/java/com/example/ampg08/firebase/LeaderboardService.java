package com.example.ampg08.firebase;

import com.example.ampg08.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardService {
    private static LeaderboardService instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static LeaderboardService getInstance() {
        if (instance == null) instance = new LeaderboardService();
        return instance;
    }

    public interface OnLeaderboard {
        void onSuccess(List<User> users);
        void onError(Exception e);
    }

    public void getTop20(OnLeaderboard cb) {
        db.collection("users")
                .orderBy("totalWins", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(qs -> {
                    List<User> list = new ArrayList<>();
                    qs.getDocuments().forEach(d -> {
                        User u = d.toObject(User.class);
                        if (u != null) list.add(u);
                    });
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }
}