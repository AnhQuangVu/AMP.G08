package com.example.ampg08;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.ampg08.databinding.ActivityGameBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.model.PlayerState;

public class GameActivity extends BaseActivity implements SensorEventListener {

    public static final String EXTRA_ROOM_ID  = "room_id";
    public static final String EXTRA_MAP_SEED = "map_seed";
    public static final String EXTRA_OFFLINE  = "offline";

    private ActivityGameBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor proximitySensor;

    private String roomId;
    private long mapSeed;
    private boolean offline;

    // Skill cooldown (proximity)
    private boolean skillOnCooldown = false;
    private static final long PROXIMITY_COOLDOWN = 5000;

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
                // Lưu kết quả và navigate Result
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
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    // ─── Sensor Events ───────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // event.values[0] = X (nghiêng trái/phải)
            // event.values[1] = Y (nghiêng trước/sau)
            binding.gameView.updateTilt(event.values[0], event.values[1]);

        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            // Khi che tay: giá trị < maximumRange → trigger skill
            if (event.values[0] < proximitySensor.getMaximumRange() && !skillOnCooldown) {
                skillOnCooldown = true;
                binding.gameView.onProximityTriggered();
                // Reset cooldown sau 5s
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> skillOnCooldown = false, PROXIMITY_COOLDOWN);
            }
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
        // Đơn giản: dialog pause
        new android.app.AlertDialog.Builder(this)
                .setTitle("PAUSE")
                .setMessage("Trò chơi đang tạm dừng")
                .setPositiveButton("Tiếp tục", null)
                .setNegativeButton("Thoát", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }
}