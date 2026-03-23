package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import com.example.ampg08.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
        animateMenu();
    }

    private void setupClickListeners() {
        binding.btnPlay.setOnClickListener(v -> {
            startActivity(new Intent(this, PlaySetupActivity.class));
        });

        binding.btnScore.setOnClickListener(v -> {
            startActivity(new Intent(this, ScoreActivity.class));
        });

        binding.btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        binding.btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    private void animateMenu() {
        View[] buttons = {binding.btnPlay, binding.btnScore, binding.btnSettings, binding.btnProfile};
        long delay = 200;

        for (View button : buttons) {
            button.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(800)
                    .setStartDelay(delay)
                    .setInterpolator(new AnticipateOvershootInterpolator())
                    .start();
            delay += 150;
        }
    }
}