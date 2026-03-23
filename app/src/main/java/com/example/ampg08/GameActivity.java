package com.example.ampg08;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import com.example.ampg08.databinding.ActivityGameBinding;
import com.example.ampg08.databinding.PauseOverlayBinding;

public class GameActivity extends BaseActivity implements SensorEventListener {

    private ActivityGameBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hideSystemUI();
        setupSensors();
        setupClickListeners();
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void setupClickListeners() {
        binding.btnPause.setOnClickListener(v -> showPauseDialog());
    }

    private void showPauseDialog() {
        PauseOverlayBinding pauseBinding = PauseOverlayBinding.inflate(LayoutInflater.from(this));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(pauseBinding.getRoot())
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        pauseBinding.btnResume.setOnClickListener(v -> dialog.dismiss());
        pauseBinding.btnRestart.setOnClickListener(v -> {
            dialog.dismiss();
            recreate();
        });
        pauseBinding.btnQuit.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float tiltX = event.values[0];
            float tiltY = event.values[1];

            // Update Game View
            binding.gameView.updateTilt(tiltX, tiltY);

            // Update UI indicator
            binding.tiltIndicator.setRotation((float) Math.toDegrees(Math.atan2(tiltY, tiltX)) + 90);
            float alpha = Math.min(1.0f, (Math.abs(tiltX) + Math.abs(tiltY)) / 10f);
            binding.tiltIndicator.setAlpha(Math.max(0.2f, alpha));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}