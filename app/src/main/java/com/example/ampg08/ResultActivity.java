package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.ampg08.databinding.ActivityResultBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.Match;
import com.example.ampg08.model.PlayerState;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ResultActivity extends BaseActivity {

    public static final String EXTRA_FINISH_TIME = "finish_time";
    public static final String EXTRA_ROOM_ID     = "room_id";
    public static final String EXTRA_OFFLINE     = "offline";

    private ActivityResultBinding binding;
    private final FirebaseAuthManager auth = FirebaseAuthManager.getInstance();
    private final FirestoreManager db = FirestoreManager.getInstance();

    private ListenerRegistration playersListener;
    private String roomId;
    private long myFinishTime;
    private boolean offline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        myFinishTime = getIntent().getLongExtra(EXTRA_FINISH_TIME, 0);
        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        offline = getIntent().getBooleanExtra(EXTRA_OFFLINE, true);

        binding.btnHome.setOnClickListener(v -> goHome());
        binding.btnPlayAgain.setOnClickListener(v -> goHome());

        if (offline) {
            showOfflineResult();
        } else {
            showOnlineResult();
        }
    }

    private void showOfflineResult() {
        binding.tvWinner.setText("🏆 FINISHED!");
        binding.tvTime.setText(formatTime(myFinishTime));
        binding.tvSubtitle.setText("Chế độ luyện tập");
        binding.groupOnline.setVisibility(View.GONE);
    }

    private void showOnlineResult() {
        if (roomId == null) { showOfflineResult(); return; }

        binding.tvSubtitle.setText("Online Match");
        binding.tvTime.setText(formatTime(myFinishTime));

        // Nghe kết quả tất cả players
        playersListener = db.listenPlayers(roomId, players -> {
            if (players == null || players.isEmpty()) return;

            // Sắp xếp theo finishTime (0 = chưa về đích, đẩy xuống cuối)
            List<PlayerState> sorted = new ArrayList<>(players);
            sorted.sort((a, b) -> {
                if (a.getFinishTime() == 0 && b.getFinishTime() == 0) return 0;
                if (a.getFinishTime() == 0) return 1;
                if (b.getFinishTime() == 0) return -1;
                return Long.compare(a.getFinishTime(), b.getFinishTime());
            });

            // Winner là người finish đầu tiên
            PlayerState winner = sorted.get(0);
            if (winner.getFinishTime() > 0) {
                binding.tvWinner.setText("🏆 " + winner.getDisplayName());
                updateLeaderboard(winner, sorted.size());
            } else {
                binding.tvWinner.setText("⏳ Đang chờ...");
            }

            // Hiển thị ranking
            StringBuilder sb = new StringBuilder();
            int rank = 1;
            for (PlayerState p : sorted) {
                String rankEmoji = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : rank + ".";
                String time = p.getFinishTime() > 0 ? formatTime(p.getFinishTime()) : "...";
                sb.append(rankEmoji).append(" ").append(p.getDisplayName())
                        .append("  ").append(time).append("\n");
                rank++;
            }
            binding.tvRanking.setText(sb.toString().trim());
            binding.groupOnline.setVisibility(View.VISIBLE);
        });

        // Đặt roomId ended sau 10 giây
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            db.setRoomStatus(roomId, "ended", new FirestoreManager.OnCompleteCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String e) {}
            });
        }, 10000);
    }

    private void updateLeaderboard(PlayerState winner, int totalPlayers) {
        String localUid = auth.getCurrentUid();
        if (localUid == null) return;

        // Lưu match
        String matchId = UUID.randomUUID().toString();
        Match match = new Match(matchId, roomId, winner.getUid(), winner.getDisplayName(), winner.getFinishTime());
        db.saveMatch(match, new FirestoreManager.OnCompleteCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(String e) {}
        });

        // Cập nhật leaderboard
        if (localUid.equals(winner.getUid())) {
            db.incrementWins(localUid, auth.getCurrentDisplayName());
        } else {
            db.incrementTotalMatches(localUid);
        }
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    private String formatTime(long ms) {
        long s = ms / 1000;
        long m = s / 60;
        s = s % 60;
        return String.format(java.util.Locale.US, "%02d:%02d.%01d", m, s, (ms % 1000) / 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playersListener != null) playersListener.remove();
    }
}