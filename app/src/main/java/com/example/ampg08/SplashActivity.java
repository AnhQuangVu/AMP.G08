package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.example.ampg08.databinding.ActivitySplashBinding;

public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding binding;
    private static final int SPLASH_DELAY = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        startAnimations();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DELAY);
    }

    private void startAnimations() {
        // Logo Animation: Scale + Fade In
        binding.logoLayout.setAlpha(0f);
        binding.logoLayout.setScaleX(0.8f);
        binding.logoLayout.setScaleY(0.8f);

        binding.logoLayout.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Loading text pulse
        binding.tvLoading.animate()
                .alpha(0.3f)
                .setDuration(800)
                .setStartDelay(500)
                .withEndAction(() -> {
                    binding.tvLoading.animate().alpha(1f).setDuration(800).start();
                })
                .start();
    }
}