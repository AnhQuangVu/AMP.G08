package com.example.ampg08;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ampg08.adapter.ScoreAdapter;
import com.example.ampg08.databinding.ActivityScoreBinding;
import com.example.ampg08.model.ScoreModel;
import java.util.ArrayList;
import java.util.List;

public class ScoreActivity extends BaseActivity {

    private ActivityScoreBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        setupScoreList();
    }

    private void setupScoreList() {
        List<ScoreModel> scores = new ArrayList<>();
        scores.add(new ScoreModel(1, "NeonKnight", 9850, "Cyber Grid"));
        scores.add(new ScoreModel(2, "TiltMaster", 8400, "Lava Rift"));
        scores.add(new ScoreModel(3, "VoidRunner", 7200, "Forest Run"));
        scores.add(new ScoreModel(4, "Guest_404", 5100, "Beginner Maze"));
        scores.add(new ScoreModel(5, "AlphaPilot", 4300, "Cyber Grid"));

        ScoreAdapter adapter = new ScoreAdapter(scores);
        binding.rvScores.setLayoutManager(new LinearLayoutManager(this));
        binding.rvScores.setAdapter(adapter);
    }
}