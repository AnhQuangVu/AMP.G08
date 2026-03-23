package com.example.ampg08.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ampg08.databinding.ItemScoreBinding;
import com.example.ampg08.model.ScoreModel;
import java.util.List;

public class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder> {

    private final List<ScoreModel> scores;

    public ScoreAdapter(List<ScoreModel> scores) {
        this.scores = scores;
    }

    @NonNull
    @Override
    public ScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScoreBinding binding = ItemScoreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ScoreViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreViewHolder holder, int position) {
        ScoreModel score = scores.get(position);
        holder.binding.tvRank.setText(String.valueOf(score.getRank()));
        holder.binding.tvUsername.setText(score.getUsername());
        holder.binding.tvMapName.setText(score.getMapName());
        holder.binding.tvScoreValue.setText(String.valueOf(score.getScore()));

        // Highlight top 3
        int color;
        switch (score.getRank()) {
            case 1: color = 0xFFFFD700; break; // Gold
            case 2: color = 0xFFC0C0C0; break; // Silver
            case 3: color = 0xFFCD7F32; break; // Bronze
            default: color = 0xFF00F5FF; break; // Default Cyan
        }
        holder.binding.tvRank.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return scores.size();
    }

    static class ScoreViewHolder extends RecyclerView.ViewHolder {
        ItemScoreBinding binding;
        ScoreViewHolder(ItemScoreBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}