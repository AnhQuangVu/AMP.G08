package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;

import com.example.ampg08.databinding.ActivityHomeBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding binding;
    private final FirebaseAuthManager authManager = FirebaseAuthManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        loadUserInfo();
        setupClickListeners();
        animateMenu();
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }
    private void loadUserInfo() {
        String uid = authManager.getCurrentUid();
        if (uid == null) return;
        FirestoreManager.getInstance().getUser(uid, user -> {
            if (user != null) {
                binding.tvUsername.setText(user.getDisplayName());
                binding.tvStats.setText(getString(R.string.home_stats_format, user.getTotalWins(), user.getTotalMatches()));
            } else {
                binding.tvUsername.setText(authManager.getCurrentDisplayName());
                binding.tvStats.setText(R.string.home_stats_default);
            }
        });
    }

    private void setupClickListeners() {
        // Quick Match
        binding.btnQuickMatch.setOnClickListener(v ->
                startActivity(new Intent(this, MatchmakingActivity.class)));

        // Online Multiplayer
        binding.btnCreate.setOnClickListener(v ->
                startActivity(new Intent(this, CreateRoomActivity.class)));

        binding.btnJoin.setOnClickListener(v ->
                startActivity(new Intent(this, JoinRoomActivity.class)));

        // Match setup flow
        binding.btnMatchSetup.setOnClickListener(v ->
                startActivity(new Intent(this, PlaySetupActivity.class)));

        // Offline Practice
        binding.btnPractice.setOnClickListener(v -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_OFFLINE, true);
            intent.putExtra(GameActivity.EXTRA_MAP_SEED, System.currentTimeMillis());
            startActivity(intent);
        });

        // Nhánh phụ
        binding.btnScore.setOnClickListener(v ->
                startActivity(new Intent(this, ScoreActivity.class)));

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        binding.btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        binding.btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        // Sign Out
        binding.btnLogout.setOnClickListener(v -> {
            binding.btnLogout.setEnabled(false);
            authManager.signOut(this, getString(R.string.default_web_client_id), () -> {
                startActivity(new Intent(this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            });
        });
    }

    private void animateMenu() {
        View[] buttons = {
                binding.btnQuickMatch, binding.btnCreate, binding.btnJoin, binding.btnMatchSetup, binding.btnPractice,
                binding.btnScore, binding.btnSettings,
                binding.btnLeaderboard, binding.btnProfile
        };
        long delay = 150;
        for (View btn : buttons) {
            btn.setAlpha(0f);
            btn.setTranslationX(80f);
            btn.animate().alpha(1f).translationX(0f)
                    .setDuration(700)
                    .setStartDelay(delay)
                    .setInterpolator(new AnticipateOvershootInterpolator())
                    .start();
            delay += 120;
        }
    }
}
