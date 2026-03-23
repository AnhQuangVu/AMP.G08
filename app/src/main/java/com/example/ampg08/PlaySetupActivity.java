package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ampg08.adapter.MapAdapter;
import com.example.ampg08.databinding.ActivityPlaySetupBinding;
import com.example.ampg08.model.MapModel;
import java.util.ArrayList;
import java.util.List;

public class PlaySetupActivity extends BaseActivity {

    private ActivityPlaySetupBinding binding;
    private MapAdapter mapAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        setupMapList();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Mode Selection
        binding.cardVsPlayer.setOnClickListener(v -> selectMode(binding.cardVsPlayer, binding.cardVsAI));
        binding.cardVsAI.setOnClickListener(v -> selectMode(binding.cardVsAI, binding.cardVsPlayer));

        binding.btnStart.setOnClickListener(v -> {
            startActivity(new Intent(this, GameActivity.class));
            finish();
        });
    }

    private void setupMapList() {
        List<MapModel> maps = new ArrayList<>();
        maps.add(new MapModel("1", "Beginner Maze", 1, false, R.drawable.ic_launcher_background));
        maps.add(new MapModel("2", "Forest Run", 2, false, R.drawable.ic_launcher_background));
        maps.add(new MapModel("3", "Lava Rift", 4, true, R.drawable.ic_launcher_background));
        maps.add(new MapModel("4", "Cyber Grid", 5, true, R.drawable.ic_launcher_background));

        mapAdapter = new MapAdapter(maps, map -> {
            // Handle map selection
        });

        binding.rvMaps.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvMaps.setAdapter(mapAdapter);
    }

    private void selectMode(View selected, View unselected) {
        selected.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
        unselected.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
        
        // In real app, update stroke/background colors to reflect selection
    }
}