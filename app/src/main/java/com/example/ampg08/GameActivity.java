package com.example.ampg08;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

public class GameActivity extends BaseActivity implements SensorEventListener {

    public static final String EXTRA_ROOM_ID  = "room_id";
    public static final String EXTRA_MAP_SEED = "map_seed";
    public static final String EXTRA_OFFLINE  = "offline";

    private ActivityGameBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor proximitySensor;
    private Dialog pauseDialog;
    private boolean keepPausedOnResume;
    private boolean proximityIsNear;

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

        setupGame();
        setupSensors();

        binding.btnPause.setOnClickListener(v -> showPause());
    }

    private void setupGame() {
        String uid  = FirebaseAuthManager.getInstance().getCurrentUid();
        String name = FirebaseAuthManager.getInstance().getCurrentDisplayName();

        if (offline) {
            binding.gameView.setupOffline(mapSeed);
        } else {
            binding.gameView.setupGame(mapSeed, roomId, uid != null ? uid : "local", name);
        }

        binding.gameView.setEventListener(new com.example.ampg08.view.GameView.GameEventListener() {
            @Override
            public void onGameFinished(long finishTimeMs) {
                navigateToResult(finishTimeMs);
            }

            @Override
            public void onSkillUsed() {
                Toast.makeText(GameActivity.this, "⚡ SKILL!", Toast.LENGTH_SHORT).show();
            }
        });
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
            binding.gameView.updateTilt(event.values[0], event.values[1]);

        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (proximitySensor == null) return;

            // Trigger skill once per FAR -> NEAR transition.
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