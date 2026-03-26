package com.example.ampg08.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ampg08.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {

    private List<DocumentSnapshot> data = new ArrayList<>();

    public void setData(List<DocumentSnapshot> docs) {
        this.data = docs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentSnapshot doc = data.get(position);
        int rank = position + 1;
        String rankEmoji = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "#" + rank;
        holder.tvRank.setText(rankEmoji);
        holder.tvName.setText(doc.getString("displayName"));
        Long wins = doc.getLong("wins");
        Long total = doc.getLong("totalMatches");
        holder.tvWins.setText((wins != null ? wins : 0) + " W");
        holder.tvMatches.setText((total != null ? total : 0) + " games");
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvWins, tvMatches;
        VH(@NonNull View itemView) {
            super(itemView);
            tvRank    = itemView.findViewById(R.id.tvRank);
            tvName    = itemView.findViewById(R.id.tvName);
            tvWins    = itemView.findViewById(R.id.tvWins);
            tvMatches = itemView.findViewById(R.id.tvMatches);
        }
    }
}
