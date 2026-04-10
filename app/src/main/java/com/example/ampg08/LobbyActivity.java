package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ampg08.adapter.LobbyPlayerAdapter;
import com.example.ampg08.databinding.ActivityLobbyBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.PlayerState;
import com.example.ampg08.model.Room;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LobbyActivity extends BaseActivity {

    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_IS_HOST = "is_host";
    public static final String EXTRA_PLAYER_LIMIT = "player_limit";
    public static final String EXTRA_TIME_LIMIT_SECONDS = "time_limit_seconds";

    private ActivityLobbyBinding binding;
    private final FirebaseAuthManager auth = FirebaseAuthManager.getInstance();
    private final FirestoreManager    db   = FirestoreManager.getInstance();

    private String  roomId;
    private boolean isHost;
    private String  localUid;
    private long    mapSeed;
    private int     playerLimit = 2;
    private int     timeLimitSeconds = 180;

    private ListenerRegistration roomListener;
    private ListenerRegistration playersListener;
    private LobbyPlayerAdapter   playerAdapter;

    private boolean isReady = false;

    // Guard: tránh navigate game 2 lần khi cả room listener lẫn btnStart đều kích hoạt
    private final AtomicBoolean navigatedToGame = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding  = ActivityLobbyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        roomId   = getIntent().getStringExtra(EXTRA_ROOM_ID);
        isHost   = getIntent().getBooleanExtra(EXTRA_IS_HOST, false);
        localUid = auth.getCurrentUid();
        playerLimit = getIntent().getIntExtra(EXTRA_PLAYER_LIMIT, 2);
        timeLimitSeconds = getIntent().getIntExtra(EXTRA_TIME_LIMIT_SECONDS, 180);

        // Hiển thị code ngắn gọn (8 ký tự đầu, uppercase)
        String displayCode = roomId != null
                ? roomId.substring(0, Math.min(8, roomId.length())).toUpperCase()
                : "?";
        binding.tvRoomCode.setText("ROOM: " + displayCode);

        playerAdapter = new LobbyPlayerAdapter();
        binding.rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPlayers.setAdapter(playerAdapter);

        // Host thấy nút Start, non-host thấy nút Ready
        if (isHost) {
            binding.btnStart.setVisibility(View.VISIBLE);
            binding.btnStart.setEnabled(false);
            binding.btnReady.setVisibility(View.GONE); // host không cần ready
        } else {
            binding.btnStart.setVisibility(View.GONE);
        }

        binding.btnStart.setOnClickListener(v -> startGame());
        binding.btnReady.setOnClickListener(v -> toggleReady());
        binding.btnBack.setOnClickListener(v -> {
            leaveRoom();
            finish();
        });

        // Host tự động được đánh dấu ready
        if (isHost && localUid != null) {
            db.setPlayerReady(roomId, localUid, true);
        }

        listenRoom();
        listenPlayers();
    }

    private void toggleReady() {
        isReady = !isReady;
        db.setPlayerReady(roomId, localUid, isReady);
        binding.btnReady.setText(isReady ? "✓ READY" : "READY");
        binding.btnReady.setAlpha(isReady ? 0.65f : 1f);
    }

    private void listenRoom() {
        roomListener = db.listenRoom(roomId, room -> {
            if (room == null) return;
            mapSeed = room.getMapSeed();
            if (room.getPlayerLimit() > 0) {
                playerLimit = room.getPlayerLimit();
            }
            timeLimitSeconds = room.getTimeLimitSeconds();

            // Khi host set status → "playing", tất cả vào game
            if ("playing".equals(room.getStatus())) {
                goToGame();
            }
        });
    }

    private void listenPlayers() {
        playersListener = db.listenPlayers(roomId, players -> {
            if (players == null) return;
            playerAdapter.setPlayers(players);

            if (!isHost) return;

            // Điều kiện Start: ít nhất 2 người VÀ tất cả đều ready
            boolean allReady = true;
            int     count    = 0;

            for (PlayerState p : players) {
                count++;
                if (!p.isReady()) {
                    allReady = false;
                }
            }

            int requiredPlayers = Math.max(2, playerLimit);
            boolean canStart = count >= requiredPlayers && allReady;
            binding.btnStart.setEnabled(canStart);
            binding.tvWaiting.setText(
                    canStart ? "Tất cả sẵn sàng! ✓"
                            : count < requiredPlayers ? "Chờ đủ người chơi..."
                            : "Chờ mọi người ready...");
        });
    }

    private void startGame() {
        binding.btnStart.setEnabled(false);
        db.setRoomStatus(roomId, "playing", new FirestoreManager.OnCompleteCallback() {
            @Override
            public void onSuccess() {
                goToGame(); // host navigate ngay, listener cũng trigger nhưng guard sẽ chặn
            }

            @Override
            public void onFailure(String error) {
                binding.btnStart.setEnabled(true);
                Toast.makeText(LobbyActivity.this, "Lỗi start: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToGame() {
        if (!navigatedToGame.compareAndSet(false, true)) return; // đã navigate rồi

        leaveRoom();
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_ROOM_ID, roomId);
        intent.putExtra(GameActivity.EXTRA_MAP_SEED, mapSeed);
        intent.putExtra(GameActivity.EXTRA_OFFLINE, false);
        intent.putExtra(CreateRoomActivity.EXTRA_TIME_LIMIT_SECONDS, timeLimitSeconds);
        startActivity(intent);
        finish();
    }

    private void leaveRoom() {
        if (playersListener != null) { playersListener.remove(); playersListener = null; }
        if (roomListener    != null) { roomListener.remove();    roomListener    = null; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveRoom();
    }
}