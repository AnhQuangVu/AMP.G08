package com.example.ampg08.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.example.ampg08.databinding.ActivitySplashBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;

public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding binding;
    private static final int SPLASH_DELAY = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();
        startAnimations();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Kiểm tra Firebase Auth – đã đăng nhập → Home, chưa → Login
            if (FirebaseAuthManager.getInstance().isLoggedIn()) {
                startActivity(new Intent(this, HomeActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DELAY);
    }

    private void startAnimations() {
        binding.logoLayout.setAlpha(0f);
        binding.logoLayout.setScaleX(0.8f);
        binding.logoLayout.setScaleY(0.8f);
        binding.logoLayout.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(1200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        binding.tvLoading.animate()
                .alpha(0.3f).setDuration(800).setStartDelay(500)
                .withEndAction(() -> binding.tvLoading.animate().alpha(1f).setDuration(800).start())
                .start();
    }
}