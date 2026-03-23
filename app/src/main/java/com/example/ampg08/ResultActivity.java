package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.OvershootInterpolator;
import com.example.ampg08.databinding.ActivityResultBinding;

public class ResultActivity extends BaseActivity {

    private ActivityResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        animateResults();

        binding.btnPlayAgain.setOnClickListener(v -> {
            startActivity(new Intent(this, PlaySetupActivity.class));
            finish();
        });

        binding.btnMainMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity();
        });
    }

    private void animateResults() {
        binding.tvVictory.setAlpha(0f);
        binding.tvVictory.setTranslationY(-50f);
        binding.tvVictory.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .start();

        binding.cardRank1.setAlpha(0f);
        binding.cardRank1.setTranslationX(-100f);
        binding.cardRank1.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(1000)
                .setStartDelay(500)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }
}