package com.example.ampg08.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ampg08.databinding.ItemMapBinding;
import com.example.ampg08.model.MapModel;
import java.util.List;

public class MapAdapter extends RecyclerView.Adapter<MapAdapter.MapViewHolder> {

    private final List<MapModel> maps;
    private final OnMapClickListener listener;
    private int selectedPosition = 0;

    public interface OnMapClickListener {
        void onMapClick(MapModel map);
    }

    public MapAdapter(List<MapModel> maps, OnMapClickListener listener) {
        this.maps = maps;
        this.listener = listener;
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
        holder.binding.ivLock.setVisibility(map.isLocked() ? View.VISIBLE : View.GONE);
        
        // Highlight selected map
        if (position == selectedPosition) {
            holder.binding.getRoot().setStrokeColor(holder.binding.getRoot().getContext().getColor(com.example.ampg08.R.color.color_primary));
            holder.binding.getRoot().setStrokeWidth(4);
        } else {
            holder.binding.getRoot().setStrokeColor(0x33FFFFFF);
            holder.binding.getRoot().setStrokeWidth(2);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onMapClick(map);
        });
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