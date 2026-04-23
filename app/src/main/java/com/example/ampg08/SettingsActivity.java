package com.example.ampg08;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.example.ampg08.databinding.ActivitySettingsBinding;

public class SettingsActivity extends BaseActivity {

    private static final String PREFS_NAME = "tilt_arena_settings";
    private static final String KEY_SENSITIVITY = "sensor_sensitivity";
    private static final String KEY_VIBRATION = "vibration";

    private static final int DEFAULT_SENSITIVITY = 5;

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        binding.btnBack.setOnClickListener(v -> finish());

        loadSettings();
        setupListeners();
    }

    private void loadSettings() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        binding.sbSensitivity.setProgress(prefs.getInt(KEY_SENSITIVITY, DEFAULT_SENSITIVITY));
        binding.swVibration.setChecked(prefs.getBoolean(KEY_VIBRATION, true));
    }

    private void setupListeners() {
        binding.sbSensitivity.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                saveInt(KEY_SENSITIVITY, progress);
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                // no-op
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                android.widget.Toast.makeText(SettingsActivity.this, R.string.settings_saved, android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        binding.swVibration.setOnCheckedChangeListener((buttonView, isChecked) -> saveBoolean(KEY_VIBRATION, isChecked));

        binding.btnReset.setOnClickListener(v -> showResetDialog());
    }

    private void showResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_reset_confirm_title)
                .setMessage(R.string.settings_reset_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    binding.sbSensitivity.setProgress(DEFAULT_SENSITIVITY);
                    binding.swVibration.setChecked(true);

                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putInt(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)
                            .putBoolean(KEY_VIBRATION, true)
                            .apply();

                    android.widget.Toast.makeText(this, R.string.settings_reset_done, android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void saveInt(String key, int value) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    private void saveBoolean(String key, boolean value) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }
}