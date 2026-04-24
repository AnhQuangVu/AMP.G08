package com.example.ampg08.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ampg08.R;
import com.example.ampg08.adapter.MapAdapter;
import com.example.ampg08.databinding.ActivityPlaySetupBinding;
import com.example.ampg08.model.MapModel;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PlaySetupActivity extends BaseActivity {

    private static final String STATE_SELECTED_MODE = "state_selected_mode";
    private static final String STATE_SELECTED_MAP_ID = "state_selected_map_id";
    private static final String STATE_PLAYER_COUNT = "state_player_count";
    private static final String STATE_AI_DIFFICULTY = "state_ai_difficulty";
    private static final String STATE_TIME_LIMIT_SECONDS = "state_time_limit_seconds";

    private ActivityPlaySetupBinding binding;
    private MapAdapter mapAdapter;
    private List<MapModel> maps;

    private MapModel selectedMap;
    private Mode selectedMode = Mode.VS_PLAYER;
    private int selectedPlayerCount = 2;
    private AiDifficulty selectedAiDifficulty = AiDifficulty.EASY;
    private int selectedTimeLimitSeconds = 180;

    private enum Mode {
        VS_PLAYER,
        VS_AI
    }

    private enum AiDifficulty {
        EASY,
        NORMAL,
        HARD
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        restoreState(savedInstanceState);
        setupUI();
        setupMapList(savedInstanceState != null ? savedInstanceState.getString(STATE_SELECTED_MAP_ID) : null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_MODE, selectedMode.name());
        outState.putString(STATE_SELECTED_MAP_ID, selectedMap != null ? selectedMap.getId() : null);
        outState.putInt(STATE_PLAYER_COUNT, selectedPlayerCount);
        outState.putString(STATE_AI_DIFFICULTY, selectedAiDifficulty.name());
        outState.putInt(STATE_TIME_LIMIT_SECONDS, selectedTimeLimitSeconds);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        selectedMode = parseEnum(savedInstanceState.getString(STATE_SELECTED_MODE), Mode.VS_PLAYER, Mode.class);
        selectedAiDifficulty = parseEnum(savedInstanceState.getString(STATE_AI_DIFFICULTY), AiDifficulty.EASY, AiDifficulty.class);
        selectedPlayerCount = savedInstanceState.getInt(STATE_PLAYER_COUNT, 2);
        selectedTimeLimitSeconds = savedInstanceState.getInt(STATE_TIME_LIMIT_SECONDS, 180);
    }

    private <T extends Enum<T>> T parseEnum(String rawValue, T fallback, Class<T> enumType) {
        if (rawValue == null) return fallback;
        try {
            return Enum.valueOf(enumType, rawValue);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.cardVsPlayer.setOnClickListener(v -> selectMode(Mode.VS_PLAYER));
        binding.cardVsAI.setOnClickListener(v -> selectMode(Mode.VS_AI));

        binding.btn2Players.setOnClickListener(v -> selectPlayerCount(2));
        binding.btn3Players.setOnClickListener(v -> selectPlayerCount(3));
        binding.btn4Players.setOnClickListener(v -> selectPlayerCount(4));

        binding.btnAiEasy.setOnClickListener(v -> selectAiDifficulty(AiDifficulty.EASY));
        binding.btnAiNormal.setOnClickListener(v -> selectAiDifficulty(AiDifficulty.NORMAL));
        binding.btnAiHard.setOnClickListener(v -> selectAiDifficulty(AiDifficulty.HARD));

        binding.rgTimeLimit.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb1Min) {
                selectedTimeLimitSeconds = 60;
            } else if (checkedId == R.id.rb3Min) {
                selectedTimeLimitSeconds = 180;
            } else if (checkedId == R.id.rb5Min) {
                selectedTimeLimitSeconds = 300;
            } else if (checkedId == R.id.rbNoLimit) {
                selectedTimeLimitSeconds = -1;
            }
            updateStepState();
        });

        binding.btnStart.setOnClickListener(v -> startMatch());

        applyMatchSelectionUI();
    }

    private void setupMapList(String restoredMapId) {
        maps = new ArrayList<>();
        maps.add(new MapModel("1", "Beginner Maze", 1, false, R.drawable.ic_launcher_background));
        maps.add(new MapModel("2", "Forest Run", 2, false, R.drawable.ic_launcher_background));
        maps.add(new MapModel("3", "Lava Rift", 4, true, R.drawable.ic_launcher_background));
        maps.add(new MapModel("4", "Cyber Grid", 5, true, R.drawable.ic_launcher_background));

        mapAdapter = new MapAdapter(maps, (map, position) -> {
            selectedMap = map;
            updateStepState();
            updateMapPreview();
        }, lockedMap -> Toast.makeText(this, R.string.map_locked_message, Toast.LENGTH_SHORT).show());

        binding.rvMaps.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvMaps.setAdapter(mapAdapter);

        int firstUnlockedIndex = -1;
        int restoredIndex = -1;
        for (int i = 0; i < maps.size(); i++) {
            MapModel map = maps.get(i);
            if (!map.isLocked() && firstUnlockedIndex == -1) {
                firstUnlockedIndex = i;
            }
            if (restoredMapId != null && restoredMapId.equals(map.getId()) && !map.isLocked()) {
                restoredIndex = i;
            }
        }

        int selectedIndex = restoredIndex != -1 ? restoredIndex : firstUnlockedIndex;
        if (selectedIndex != -1) {
            selectedMap = maps.get(selectedIndex);
            mapAdapter.setSelectedPosition(selectedIndex);
        }

        updateStepState();
        updateMapPreview();
    }

    private void selectMode(Mode mode) {
        selectedMode = mode;
        updateStepState();
        applyMatchSelectionUI();
    }

    private void selectPlayerCount(int count) {
        selectedPlayerCount = count;
        applyMatchSelectionUI();
        updateStepState();
    }

    private void selectAiDifficulty(AiDifficulty difficulty) {
        selectedAiDifficulty = difficulty;
        applyMatchSelectionUI();
        updateStepState();
    }

    private void updateStepState() {
        styleModeCard(binding.cardVsPlayer, selectedMode == Mode.VS_PLAYER);
        styleModeCard(binding.cardVsAI, selectedMode == Mode.VS_AI);

        binding.sectionPlayerCount.setVisibility(selectedMode == Mode.VS_PLAYER ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.sectionAiDifficulty.setVisibility(selectedMode == Mode.VS_AI ? android.view.View.VISIBLE : android.view.View.GONE);

        binding.step1.setBackgroundColor(getColor(R.color.color_primary));
        binding.step2.setBackgroundColor(selectedMap != null ? getColor(R.color.color_primary) : getColor(R.color.color_surface));
        binding.step3.setBackgroundColor(getColor(R.color.color_primary));

        binding.btnStart.setEnabled(selectedMap != null);
        binding.btnStart.setAlpha(selectedMap != null ? 1f : 0.6f);
    }

    private void applyMatchSelectionUI() {
        styleToggleButton(binding.btn2Players, selectedPlayerCount == 2);
        styleToggleButton(binding.btn3Players, selectedPlayerCount == 3);
        styleToggleButton(binding.btn4Players, selectedPlayerCount == 4);

        styleToggleButton(binding.btnAiEasy, selectedAiDifficulty == AiDifficulty.EASY);
        styleToggleButton(binding.btnAiNormal, selectedAiDifficulty == AiDifficulty.NORMAL);
        styleToggleButton(binding.btnAiHard, selectedAiDifficulty == AiDifficulty.HARD);

        if (selectedTimeLimitSeconds == 60) {
            binding.rgTimeLimit.check(R.id.rb1Min);
        } else if (selectedTimeLimitSeconds == 180) {
            binding.rgTimeLimit.check(R.id.rb3Min);
        } else if (selectedTimeLimitSeconds == 300) {
            binding.rgTimeLimit.check(R.id.rb5Min);
        } else {
            binding.rgTimeLimit.check(R.id.rbNoLimit);
        }
    }

    private void styleToggleButton(Button button, boolean selected) {
        button.animate()
                .scaleX(selected ? 1.04f : 1f)
                .scaleY(selected ? 1.04f : 1f)
                .setDuration(140)
                .start();
        button.setAlpha(selected ? 1f : 0.78f);
    }

    private void updateMapPreview() {
        if (selectedMap == null) {
            binding.tvSelectedMapName.setText(R.string.map_select_title);
            binding.tvSelectedMapDiff.setText("");
            return;
        }

        binding.tvSelectedMapName.setText(selectedMap.getName());
        binding.tvSelectedMapDiff.setText(getDifficultyLabel(selectedMap.getDifficulty()));
        binding.tvSelectedMapDiff.setTextColor(ContextCompat.getColor(this, getDifficultyColor(selectedMap.getDifficulty())));

        ImageView[] stars = {
                binding.star1, binding.star2, binding.star3, binding.star4, binding.star5
        };
        for (int i = 0; i < stars.length; i++) {
            stars[i].setImageResource(
                    i < selectedMap.getDifficulty() ? R.drawable.ic_star_filled : R.drawable.ic_star_empty
            );
        }
    }

    private String getDifficultyLabel(int difficulty) {
        if (difficulty <= 2) return getString(R.string.diff_easy);
        if (difficulty <= 4) return getString(R.string.diff_medium);
        return getString(R.string.diff_hard);
    }

    private int getDifficultyColor(int difficulty) {
        if (difficulty <= 2) return R.color.color_diff_easy;
        if (difficulty <= 4) return R.color.color_secondary;
        return R.color.color_diff_hard;
    }

    private void styleModeCard(MaterialCardView card, boolean selected) {
        card.animate().scaleX(selected ? 1.05f : 1f).scaleY(selected ? 1.05f : 1f).setDuration(180).start();
        card.setStrokeWidth(selected ? 3 : 1);
        card.setStrokeColor(getColor(selected ? R.color.color_primary : android.R.color.transparent));
    }

    private void startMatch() {
        if (selectedMap == null) {
            Toast.makeText(this, R.string.select_map_first, Toast.LENGTH_SHORT).show();
            return;
        }

        long mapSeed = buildMapSeed(selectedMap.getId());

        if (selectedMode == Mode.VS_AI) {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_OFFLINE, true);
            intent.putExtra(GameActivity.EXTRA_MAP_SEED, mapSeed);
            intent.putExtra(CreateRoomActivity.EXTRA_TIME_LIMIT_SECONDS, selectedTimeLimitSeconds);
            intent.putExtra(CreateRoomActivity.EXTRA_AI_DIFFICULTY, selectedAiDifficulty.name());
            intent.putExtra(CreateRoomActivity.EXTRA_MODE, selectedMode.name());
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, CreateRoomActivity.class);
            intent.putExtra(CreateRoomActivity.EXTRA_MAP_SEED, mapSeed);
            intent.putExtra(CreateRoomActivity.EXTRA_PLAYER_COUNT, selectedPlayerCount);
            intent.putExtra(CreateRoomActivity.EXTRA_TIME_LIMIT_SECONDS, selectedTimeLimitSeconds);
            intent.putExtra(CreateRoomActivity.EXTRA_AI_DIFFICULTY, selectedAiDifficulty.name());
            intent.putExtra(CreateRoomActivity.EXTRA_MODE, selectedMode.name());
            startActivity(intent);
        }
        finish();
    }

    private long buildMapSeed(String mapId) {
        return (mapId.hashCode() * 2654435761L) ^ System.currentTimeMillis();
    }
}