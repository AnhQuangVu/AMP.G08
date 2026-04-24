package com.example.ampg08.activity;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ampg08.adapter.LeaderboardAdapter;
import com.example.ampg08.databinding.ActivityLeaderboardBinding;
import com.example.ampg08.firebase.FirestoreManager;

public class LeaderboardActivity extends BaseActivity {

    private ActivityLeaderboardBinding binding;
    private LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new LeaderboardAdapter();
        binding.rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLeaderboard.setAdapter(adapter);

        loadLeaderboard();
    }

    private void loadLeaderboard() {
        binding.progressBar.setVisibility(View.VISIBLE);
        FirestoreManager.getInstance().getLeaderboard(docs -> {
            binding.progressBar.setVisibility(View.GONE);
            if (docs != null) {
                adapter.setData(docs);
            }
        });
    }
}
