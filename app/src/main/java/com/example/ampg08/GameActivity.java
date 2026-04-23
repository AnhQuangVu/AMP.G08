package com.example.ampg08;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.ViewGroup;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Dialog;
import android.widget.Toast;

import com.example.ampg08.databinding.ActivityGameBinding;
import com.example.ampg08.databinding.PauseOverlayBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;

public class GameActivity extends BaseActivity implements SensorEventListener {

    public static final String EXTRA_ROOM_ID  = "room_id";
    public static final String EXTRA_MAP_SEED = "map_seed";
    public static final String EXTRA_OFFLINE  = "offline";

    private static final String PREFS_NAME        = "tilt_arena_settings";
    private static final String KEY_SENSITIVITY   = "sensor_sensitivity";
    private static final String KEY_VIBRATION     = "vibration";
    private static final int    DEFAULT_SENSITIVITY = 5;

    private ActivityGameBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor proximitySensor;
    private Vibrator vibrator;
    private Dialog pauseDialog;
    private boolean keepPausedOnResume;
    private boolean proximityIsNear;
    private float sensitivityMultiplier = 1f;
    private boolean vibrationEnabled = true;

    private String  roomId;
    private long    mapSeed;
    private boolean offline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        roomId  = getIntent().getStringExtra(EXTRA_ROOM_ID);
        mapSeed = getIntent().getLongExtra(EXTRA_MAP_SEED, System.currentTimeMillis());
        offline = getIntent().getBooleanExtra(EXTRA_OFFLINE, true);

        loadSettings();
        setupGame();
        setupSensors();

        binding.btnPause.setOnClickListener(v -> showPause());
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int sensitivity = prefs.getInt(KEY_SENSITIVITY, DEFAULT_SENSITIVITY);
        // Slider 0–10, map sang multiplier 0.2x–2.0x
        sensitivityMultiplier = 0.2f + (sensitivity / 10f) * 1.8f;
        vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void setupGame() {
        String uid  = FirebaseAuthManager.getInstance().getCurrentUid();
        String fallbackName = FirebaseAuthManager.getInstance().getCurrentDisplayName();

        binding.gameView.setEventListener(new com.example.ampg08.view.GameView.GameEventListener() {
            @Override
            public void onGameFinished(long finishTimeMs) {
                navigateToResult(finishTimeMs);
            }

            @Override
            public void onSkillUsed() {
                Toast.makeText(GameActivity.this, "⚡ SKILL!", Toast.LENGTH_SHORT).show();
                vibrate(100);
            }
        });

        if (offline) {
            binding.gameView.setupOffline(mapSeed);
        } else {
            // Lấy tên từ Firestore để đồng bộ với tên đã đổi trong Profile
            FirestoreManager.getInstance().getUser(uid, user -> {
                String name = (user != null && user.getDisplayName() != null)
                        ? user.getDisplayName() : fallbackName;
                binding.gameView.setupGame(mapSeed, roomId, uid != null ? uid : "local", name);
            });
        }
    }

    private void vibrate(long ms) {
        if (!vibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(ms);
        }
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings(); // re-read nếu vừa từ Settings về
        if (keepPausedOnResume) {
            keepPausedOnResume = false;
            showPause();
        } else {
            setGamePaused(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        proximityIsNear = false;
        setGamePaused(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pauseDialog != null && pauseDialog.isShowing()) {
            pauseDialog.dismiss();
        }
        binding.gameView.cleanup();
    }

    // ─── Sensor Events ───────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            binding.gameView.updateTilt(
                    event.values[0] * sensitivityMultiplier,
                    event.values[1] * sensitivityMultiplier);

        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (proximitySensor == null) return;

            boolean isNear = event.values[0] < proximitySensor.getMaximumRange();
            if (isNear && !proximityIsNear) {
                binding.gameView.onProximityTriggered();
            }
            proximityIsNear = isNear;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ─── Navigation ──────────────────────────────────────────────────────

    private void navigateToResult(long finishTimeMs) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_FINISH_TIME, finishTimeMs);
        intent.putExtra(ResultActivity.EXTRA_ROOM_ID, roomId);
        intent.putExtra(ResultActivity.EXTRA_OFFLINE, offline);
        startActivity(intent);
        finish();
    }

    private void showPause() {
        if (pauseDialog != null && pauseDialog.isShowing()) {
            return;
        }

        setGamePaused(true);

        PauseOverlayBinding pauseBinding = PauseOverlayBinding.inflate(getLayoutInflater());
        pauseDialog = new Dialog(this);
        pauseDialog.setContentView(pauseBinding.getRoot());
        pauseDialog.setCancelable(false);
        if (pauseDialog.getWindow() != null) {
            pauseDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            pauseDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        pauseBinding.btnResume.setOnClickListener(v -> {
            pauseDialog.dismiss();
            setGamePaused(false);
        });
        pauseBinding.btnRestart.setOnClickListener(v -> {
            pauseDialog.dismiss();
            recreate();
        });
        pauseBinding.btnSettings.setOnClickListener(v -> {
            keepPausedOnResume = true;
            pauseDialog.dismiss();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        pauseBinding.btnQuit.setOnClickListener(v -> {
            pauseDialog.dismiss();
            finish();
        });

        pauseDialog.show();
    }

    private void setGamePaused(boolean paused) {
        if (paused) {
            binding.gameView.pauseGame();
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } else {
            binding.gameView.resumeGame();
            if (sensorManager != null) {
                if (accelerometer != null) {
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                }
                if (proximitySensor != null) {
                    sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        }
    }
}
