package com.example.ampg08.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ampg08.R;
import com.example.ampg08.databinding.ItemMapBinding;
import com.example.ampg08.model.MapModel;
import java.util.List;

public class MapAdapter extends RecyclerView.Adapter<MapAdapter.MapViewHolder> {

    private final List<MapModel> maps;
    private final OnMapClickListener listener;
    private final OnLockedMapClickListener lockedMapListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnMapClickListener {
        void onMapClick(MapModel map, int position);
    }

    public interface OnLockedMapClickListener {
        void onLockedMapClick(MapModel map);
    }

    public MapAdapter(List<MapModel> maps,
                      OnMapClickListener listener,
                      OnLockedMapClickListener lockedMapListener) {
        this.maps = maps;
        this.listener = listener;
        this.lockedMapListener = lockedMapListener;
    }

    @NonNull
    @Override
    public MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMapBinding binding = ItemMapBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MapViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MapViewHolder holder, int position) {
        MapModel map = maps.get(position);
        holder.binding.tvMapName.setText(map.getName());
        holder.binding.ivThumbnail.setImageResource(map.getThumbnailResId());
        holder.binding.ivLock.setVisibility(map.isLocked() ? View.VISIBLE : View.GONE);
        holder.binding.getRoot().setAlpha(map.isLocked() ? 0.65f : 1f);
        bindDifficultyStars(holder.binding, map.getDifficulty());

        // Highlight selected map
        if (position == selectedPosition) {
            holder.binding.getRoot().setStrokeColor(ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.color_primary));
            holder.binding.getRoot().setStrokeWidth(4);
        } else {
            holder.binding.getRoot().setStrokeColor(0x33FFFFFF);
            holder.binding.getRoot().setStrokeWidth(2);
        }

        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) return;
            if (map.isLocked()) {
                if (lockedMapListener != null) {
                    lockedMapListener.onLockedMapClick(map);
                }
                return;
            }
            int oldPos = selectedPosition;
            selectedPosition = clickedPosition;
            if (oldPos != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPos);
            }
            notifyItemChanged(selectedPosition);
            listener.onMapClick(map, selectedPosition);
        });
    }

    private void bindDifficultyStars(ItemMapBinding binding, int difficulty) {
        binding.difficultyContainer.removeAllViews();
        int clampedDifficulty = Math.max(1, Math.min(5, difficulty));
        for (int i = 1; i <= 5; i++) {
            ImageView star = new ImageView(binding.getRoot().getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(12, binding),
                    dpToPx(12, binding)
            );
            if (i > 1) {
                params.setMarginStart(dpToPx(2, binding));
            }
            star.setLayoutParams(params);
            star.setImageDrawable(AppCompatResources.getDrawable(
                    binding.getRoot().getContext(),
                    i <= clampedDifficulty ? R.drawable.ic_star_filled : R.drawable.ic_star_empty
            ));
            binding.difficultyContainer.addView(star);
        }
    }

    private int dpToPx(int dp, ItemMapBinding binding) {
        float density = binding.getRoot().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return maps.size();
    }

    static class MapViewHolder extends RecyclerView.ViewHolder {
        ItemMapBinding binding;
        MapViewHolder(ItemMapBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}