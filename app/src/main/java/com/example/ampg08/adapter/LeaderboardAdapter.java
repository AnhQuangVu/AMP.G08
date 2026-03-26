package com.example.ampg08.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ampg08.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {

    private final List<DocumentSnapshot> data = new ArrayList<>();

    public void setData(List<DocumentSnapshot> docs) {
        List<DocumentSnapshot> newData = docs != null ? docs : Collections.emptyList();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new LeaderboardDiffCallback(data, newData));
        data.clear();
        data.addAll(newData);
        diffResult.dispatchUpdatesTo(this);
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
        String rankText = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "#" + rank;

        String name = doc.getString("displayName");
        if (name == null || name.trim().isEmpty()) {
            name = holder.itemView.getContext().getString(R.string.leaderboard_unknown_name);
        }

        int wins = safeLongToInt(doc.getLong("wins"));
        int total = safeLongToInt(doc.getLong("totalMatches"));

        holder.tvRank.setText(rankText);
        holder.tvName.setText(name);
        holder.tvWins.setText(holder.itemView.getContext().getString(R.string.leaderboard_wins_format, wins));
        holder.tvMatches.setText(holder.itemView.getContext().getString(R.string.leaderboard_matches_format, total));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private int safeLongToInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvWins, tvMatches;

        VH(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvName = itemView.findViewById(R.id.tvName);
            tvWins = itemView.findViewById(R.id.tvWins);
            tvMatches = itemView.findViewById(R.id.tvMatches);
        }
    }

    private static class LeaderboardDiffCallback extends DiffUtil.Callback {
        private final List<DocumentSnapshot> oldList;
        private final List<DocumentSnapshot> newList;

        LeaderboardDiffCallback(List<DocumentSnapshot> oldList, List<DocumentSnapshot> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            DocumentSnapshot oldDoc = oldList.get(oldItemPosition);
            DocumentSnapshot newDoc = newList.get(newItemPosition);

            String oldName = oldDoc.getString("displayName");
            String newName = newDoc.getString("displayName");
            Long oldWins = oldDoc.getLong("wins");
            Long newWins = newDoc.getLong("wins");
            Long oldTotal = oldDoc.getLong("totalMatches");
            Long newTotal = newDoc.getLong("totalMatches");

            return equalsNullable(oldName, newName)
                    && equalsNullable(oldWins, newWins)
                    && equalsNullable(oldTotal, newTotal);
        }

        private boolean equalsNullable(Object a, Object b) {
            return a == b || (a != null && a.equals(b));
        }
    }
}
