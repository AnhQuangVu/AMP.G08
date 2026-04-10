package com.example.ampg08;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ampg08.adapter.ScoreAdapter;
import com.example.ampg08.databinding.ActivityScoreBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.ScoreModel;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ScoreActivity extends BaseActivity {

    private ActivityScoreBinding binding;
    private ScoreAdapter scoreAdapter;
    private final List<ScoreModel> allScores = new ArrayList<>();
    private final List<ScoreModel> friendScores = new ArrayList<>();
    private int selectedTabIndex = 0;
    private boolean friendScoresLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        binding.btnBack.setOnClickListener(v -> finish());
        setupRecyclerView();
        setupTabs();
        loadScoresFromFirestore();
    }

    private void setupRecyclerView() {
        scoreAdapter = new ScoreAdapter(new ArrayList<>());
        binding.rvScores.setLayoutManager(new LinearLayoutManager(this));
        binding.rvScores.setAdapter(scoreAdapter);
    }

    private void loadScoresFromFirestore() {
        showLoading(true);
        FirestoreManager.getInstance().getLeaderboard(docs -> {
            allScores.clear();

            if (docs != null) {
                allScores.addAll(mapDocsToScores(docs));
            }

            showLoading(false);
            applyTabFilter(selectedTabIndex);
        });
    }

    private List<ScoreModel> mapDocsToScores(List<DocumentSnapshot> docs) {
        List<ScoreModel> mapped = new ArrayList<>();

        for (int i = 0; i < docs.size(); i++) {
            DocumentSnapshot doc = docs.get(i);
            String uid = doc.getString("uid");
            if (uid == null || uid.trim().isEmpty()) {
                uid = doc.getId();
            }

            String displayName = doc.getString("displayName");
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = getString(R.string.leaderboard_unknown_name);
            }

            long winsLong = doc.getLong("wins") != null ? doc.getLong("wins") : 0L;
            long matchesLong = doc.getLong("totalMatches") != null ? doc.getLong("totalMatches") : 0L;

            int wins = (int) Math.max(0L, winsLong);
            String subtitle = getString(R.string.score_matches_format, matchesLong);
            mapped.add(new ScoreModel(uid, i + 1, displayName, wins, subtitle));
        }

        return mapped;
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabIndex = tab.getPosition();
                if (selectedTabIndex == 1 && !friendScoresLoaded) {
                    loadFriendScores();
                    return;
                }
                applyTabFilter(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // no-op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                selectedTabIndex = tab.getPosition();
                if (selectedTabIndex == 1 && !friendScoresLoaded) {
                    loadFriendScores();
                    return;
                }
                applyTabFilter(tab.getPosition());
            }
        });
    }

    private void loadFriendScores() {
        String myUid = FirebaseAuthManager.getInstance().getCurrentUid();
        if (myUid == null || myUid.trim().isEmpty()) {
            friendScores.clear();
            friendScoresLoaded = true;
            applyTabFilter(1);
            return;
        }

        showLoading(true);
        FirestoreManager.getInstance().getFriendUids(myUid, uids -> {
            List<String> targetUids = new ArrayList<>(uids != null ? uids : new ArrayList<>());
            if (!targetUids.contains(myUid)) {
                targetUids.add(myUid);
            }

            FirestoreManager.getInstance().getLeaderboardByUids(targetUids, docs -> {
                friendScores.clear();
                if (docs != null) {
                    friendScores.addAll(mapDocsToScores(docs));
                }

                friendScoresLoaded = true;
                showLoading(false);
                if (selectedTabIndex == 1) {
                    applyTabFilter(1);
                }
            });
        });
    }

    private void applyTabFilter(int tabIndex) {
        List<ScoreModel> filtered = new ArrayList<>();
        String myName = FirebaseAuthManager.getInstance().getCurrentDisplayName();
        String myUid = FirebaseAuthManager.getInstance().getCurrentUid();

        if (tabIndex == 0) {
            filtered.addAll(allScores);
        } else if (tabIndex == 1) {
            filtered.addAll(friendScores);
        } else {
            boolean hasMine = false;
            for (ScoreModel score : allScores) {
                if (myUid != null && myUid.equals(score.getUid())) {
                    filtered.add(score);
                    hasMine = true;
                }
            }
            if (!hasMine) {
                filtered.add(new ScoreModel(myUid != null ? myUid : "", 1, myName, 0, getString(R.string.score_no_match_data)));
            }
        }

        List<ScoreModel> ranked = reRank(filtered);
        scoreAdapter.submitList(ranked);
        binding.tvEmpty.setText(getEmptyTextForTab(tabIndex));
        binding.tvEmpty.setVisibility(ranked.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private String getEmptyTextForTab(int tabIndex) {
        if (tabIndex == 1) {
            return getString(R.string.score_empty_friends);
        }
        if (tabIndex == 2) {
            return getString(R.string.score_empty_mine);
        }
        return getString(R.string.score_empty);
    }

    private List<ScoreModel> reRank(List<ScoreModel> source) {
        List<ScoreModel> result = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            ScoreModel score = source.get(i);
            result.add(new ScoreModel(score.getUid(), i + 1, score.getUsername(), score.getScore(), score.getMapName()));
        }
        return result;
    }

    private void showLoading(boolean loading) {
        binding.progressScores.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.rvScores.setVisibility(loading ? android.view.View.INVISIBLE : android.view.View.VISIBLE);
        binding.tvEmpty.setVisibility(android.view.View.GONE);
    }
}