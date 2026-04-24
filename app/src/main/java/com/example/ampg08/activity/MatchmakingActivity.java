package com.example.ampg08.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.ampg08.databinding.ActivityMatchmakingBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.concurrent.atomic.AtomicBoolean;

public class MatchmakingActivity extends BaseActivity {

    private ActivityMatchmakingBinding binding;
    private FirestoreManager db;
    private FirebaseAuthManager auth;

    private ListenerRegistration poolListener;
    private final AtomicBoolean navigated = new AtomicBoolean(false);
    private boolean joined = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMatchmakingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        db   = FirestoreManager.getInstance();
        auth = FirebaseAuthManager.getInstance();

        binding.btnCancel.setOnClickListener(v -> {
            cancelAndLeave();
            finish();
        });

        startMatchmaking();
    }

    private long searchStartTime;

    private void startMatchmaking() {
        String uid  = auth.getCurrentUid();
        String name = auth.getCurrentDisplayName();
        if (uid == null) { finish(); return; }

        searchStartTime = System.currentTimeMillis();
        setStatus("Đang tìm trận...");
        db.joinMatchmakingPool(uid, name, new FirestoreManager.OnMatchmakingCallback() {
            @Override
            public void onJoined() {
                joined = true;
                setStatus("Đang chờ đối thủ...");
                listenForMatch();
            }
            @Override
            public void onError(String error) {
                showError(error);
            }
        });
    }

    private void listenForMatch() {
        String myUid = auth.getCurrentUid();
        poolListener = db.listenMatchmakingPool((roomId, p1, p2, mapSeed, updatedAt) -> {
            // CHỈ VÀO TRẬN KHI:
            // 1. Đầy đủ thông tin: roomId, p1, p2, mapSeed > 0
            // 2. Quan trọng: updatedAt phải lớn hơn hoặc bằng searchStartTime (tránh đọc cache cũ)
            // 3. Mình là 1 trong 2 người
            if (roomId != null && !roomId.isEmpty() && 
                p1 != null && !p1.isEmpty() && 
                p2 != null && !p2.isEmpty() &&
                mapSeed > 0 && updatedAt >= searchStartTime) {
                
                if (myUid != null && (myUid.equals(p1) || myUid.equals(p2))) {
                    setupAndLaunch(roomId, mapSeed);
                }
            }
        });
    }

    private void setupAndLaunch(String roomId, long mapSeed) {
        if (!navigated.compareAndSet(false, true)) return;
        cleanup();
        String uid  = auth.getCurrentUid();
        String name = auth.getCurrentDisplayName();
        if (uid == null) { finish(); return; }

        // Mỗi player tự tạo PlayerState của mình (đúng rule: auth.uid == uid)
        com.example.ampg08.model.PlayerState ps = new com.example.ampg08.model.PlayerState(uid, name);
        db.setPlayerState(roomId, ps, new FirestoreManager.OnCompleteCallback() {
            @Override public void onSuccess() { launchGame(roomId, mapSeed); }
            @Override public void onFailure(String e) { launchGame(roomId, mapSeed); } // vẫn launch nếu lỗi nhỏ
        });
    }

    private void launchGame(String roomId, long mapSeed) {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_ROOM_ID, roomId);
            intent.putExtra(GameActivity.EXTRA_MAP_SEED, mapSeed);
            intent.putExtra(GameActivity.EXTRA_OFFLINE, false);
            startActivity(intent);
            finish();
        });
    }

    private void cancelAndLeave() {
        cleanup();
        if (joined && !navigated.get()) {
            String uid = auth.getCurrentUid();
            if (uid != null) db.leaveMatchmakingPool(uid);
        }
    }

    private void cleanup() {
        if (poolListener != null) { poolListener.remove(); poolListener = null; }
    }

    private void setStatus(String msg) {
        runOnUiThread(() -> binding.tvStatus.setText(msg));
    }

    private void showError(String msg) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Lỗi: " + msg, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAndLeave();
    }
}
