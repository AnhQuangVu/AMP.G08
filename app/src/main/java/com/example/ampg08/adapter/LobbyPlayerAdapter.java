package com.example.ampg08.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ampg08.R;
import com.example.ampg08.model.PlayerState;

import java.util.ArrayList;
import java.util.List;

public class LobbyPlayerAdapter extends RecyclerView.Adapter<LobbyPlayerAdapter.VH> {

    private List<PlayerState> players = new ArrayList<>();

    public void setPlayers(List<PlayerState> players) {
        this.players = players != null ? players : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lobby_player, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PlayerState p = players.get(position);
        holder.tvName.setText(p.getDisplayName() != null ? p.getDisplayName() : "Player");
        holder.tvStatus.setText(p.isReady() ? "✓ READY" : "⌛ WAITING");
        holder.tvStatus.setTextColor(p.isReady()
                ? 0xFF00FF88 : 0xFFFFAA00);
    }

    @Override
    public int getItemCount() { return players.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName   = itemView.findViewById(R.id.tvPlayerName);
            tvStatus = itemView.findViewById(R.id.tvPlayerStatus);
        }
    }
}
