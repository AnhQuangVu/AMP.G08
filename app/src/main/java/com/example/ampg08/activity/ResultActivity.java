package com.example.ampg08.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.ampg08.databinding.ActivityResultBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.PlayerState;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResultActivity extends BaseActivity {

    public static final String EXTRA_FINISH_TIME = "finish_time";
    public static final String EXTRA_ROOM_ID     = "room_id";
    public static final String EXTRA_OFFLINE     = "offline";

    private ActivityResultBinding binding;
    private final FirebaseAuthManager auth = FirebaseAuthManager.getInstance();
    private final FirestoreManager    db   = FirestoreManager.getInstance();

    private ListenerRegistration playersListener;
    private String  roomId;
    private long    myFinishTime;
    private boolean offline;

    // Guard: cập nhật leaderboard chỉ 1 lần dù snapshot thay đổi nhiều lần
    private final AtomicBoolean leaderboardUpdated = new AtomicBoolean(false);
    private final AtomicBoolean roomLeft = new AtomicBoolean(false);
    private volatile boolean isResultWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        myFinishTime = getIntent().getLongExtra(EXTRA_FINISH_TIME, 0);
        roomId       = getIntent().getStringExtra(EXTRA_ROOM_ID);
        offline      = getIntent().getBooleanExtra(EXTRA_OFFLINE, true);

        binding.btnHome.setOnClickListener(v -> goHome());
        binding.btnPlayAgain.setOnClickListener(v -> goHome());

        if (offline) {
            showOfflineResult();
        } else {
            showOnlineResult();
        }
    }

    // ─── Offline ─────────────────────────────────────────────────────────

    private void showOfflineResult() {
        binding.tvWinner.setText("FINISHED!");
        binding.tvTime.setText(formatTime(myFinishTime));
        binding.tvSubtitle.setText("Chế độ luyện tập – kết quả không được lưu");
        binding.groupOnline.setVisibility(View.GONE);
    }

    // ─── Online ──────────────────────────────────────────────────────────

    private void showOnlineResult() {
        if (roomId == null) { showOfflineResult(); return; }

        binding.tvSubtitle.setText("Online Match");
        binding.tvTime.setText(formatTime(myFinishTime));

        String localUid = auth.getCurrentUid();
        if (localUid != null) {
            db.getRoomByCode(roomId, room -> {
                isResultWriter = room != null && localUid.equals(room.getHostUid());
                listenPlayersForResult();
            });
        } else {
            listenPlayersForResult();
        }

        // Đóng room sau 15 giây (tăng từ 10s để mọi người đều thấy kết quả)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            db.setRoomStatus(roomId, "ended", new FirestoreManager.OnCompleteCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String e) {}
            });
        }, 15_000);
    }

    private void listenPlayersForResult() {
        playersListener = db.listenPlayers(roomId, players -> {
            if (players == null || players.isEmpty()) return;

            // Sắp xếp: người có finishTime > 0 lên trước (nhỏ hơn → thắng)
            // Người chưa về (finishTime == 0) xuống cuối
            List<PlayerState> sorted = new ArrayList<>(players);
            sorted.sort((a, b) -> {
                boolean aFinished = a.getFinishTime() > 0;
                boolean bFinished = b.getFinishTime() > 0;
                if (aFinished && bFinished) return Long.compare(a.getFinishTime(), b.getFinishTime());
                if (aFinished) return -1;
                if (bFinished) return 1;
                return 0;
            });

            PlayerState winner = sorted.get(0);
            if (winner.getFinishTime() > 0) {
                binding.tvWinner.setText("🏆 " + winner.getDisplayName());
                // Chi host duoc ghi ket qua, va chi ghi 1 lan.
                if (isResultWriter && leaderboardUpdated.compareAndSet(false, true)) {
                    saveMatchResult(winner, sorted);
                }
            } else {
                binding.tvWinner.setText("⏳ Đang chờ...");
            }

            // Ranking board
            StringBuilder sb = new StringBuilder();
            int rank = 1;
            for (PlayerState p : sorted) {
                String rankEmoji = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : rank + ".";
                String time      = p.getFinishTime() > 0 ? formatTime(p.getFinishTime()) : "...";
                sb.append(rankEmoji).append(" ").append(p.getDisplayName())
                        .append("  ").append(time).append("\n");
                rank++;
            }
            binding.tvRanking.setText(sb.toString().trim());
            binding.groupOnline.setVisibility(View.VISIBLE);
        });
    }

    private void saveMatchResult(PlayerState winner, List<PlayerState> sorted) {
        List<PlayerState> finishedPlayers = new ArrayList<>();
        for (PlayerState p : sorted) {
            if (p.getFinishTime() == 0) break;
            finishedPlayers.add(p);
        }

        db.recordMatchAndLeaderboardOnce(roomId, winner, finishedPlayers, new FirestoreManager.OnMatchRecordedCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(String e) {}
            @Override public void onSkipped() {}
        });
    }

    private void goHome() {
        leaveRoomOnce();
        startActivity(new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void leaveRoomOnce() {
        if (offline || roomId == null || !roomLeft.compareAndSet(false, true)) return;
        String uid = auth.getCurrentUid();
        if (uid != null) {
            db.leaveRoom(roomId, uid, null);
        }
    }

    private String formatTime(long ms) {
        long s  = ms / 1000;
        long m  = s / 60;
        s       = s % 60;
        long cs = (ms % 1000) / 100;
        return String.format(java.util.Locale.US, "%02d:%02d.%01d", m, s, cs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playersListener != null) playersListener.remove();
        leaveRoomOnce();
    }
}