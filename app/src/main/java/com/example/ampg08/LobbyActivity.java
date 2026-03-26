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

public class LobbyActivity extends BaseActivity {

    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_IS_HOST = "is_host";

    private ActivityLobbyBinding binding;
    private final FirebaseAuthManager auth = FirebaseAuthManager.getInstance();
    private final FirestoreManager db = FirestoreManager.getInstance();

    private String roomId;
    private boolean isHost;
    private String localUid;
    private long mapSeed;

    private ListenerRegistration roomListener;
    private ListenerRegistration playersListener;
    private LobbyPlayerAdapter playerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLobbyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        roomId  = getIntent().getStringExtra(EXTRA_ROOM_ID);
        isHost  = getIntent().getBooleanExtra(EXTRA_IS_HOST, false);
        localUid = auth.getCurrentUid();

        binding.tvRoomCode.setText("Room: " + (roomId != null ? roomId.substring(0, 8).toUpperCase() : "?"));

        // RecyclerView players
        playerAdapter = new LobbyPlayerAdapter();
        binding.rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPlayers.setAdapter(playerAdapter);

        // Host mới thấy nút Start
        binding.btnStart.setVisibility(isHost ? View.VISIBLE : View.GONE);
        binding.btnStart.setEnabled(false);
        binding.btnStart.setOnClickListener(v -> startGame());

        binding.btnReady.setOnClickListener(v -> toggleReady());
        binding.btnBack.setOnClickListener(v -> { leaveRoom(); finish(); });

        listenRoom();
        listenPlayers();
    }

    private boolean isReady = false;

    private void toggleReady() {
        isReady = !isReady;
        db.setPlayerReady(roomId, localUid, isReady);
        binding.btnReady.setText(isReady ? "CANCEL" : "READY");
        binding.btnReady.setAlpha(isReady ? 0.6f : 1f);
    }

    private void listenRoom() {
        roomListener = db.listenRoom(roomId, room -> {
            if (room == null) return;
            mapSeed = room.getMapSeed();
            if ("playing".equals(room.getStatus())) {
                // Host đã start, vào game
                goToGame();
            }
        });
    }

    private void listenPlayers() {
        playersListener = db.listenPlayers(roomId, players -> {
            playerAdapter.setPlayers(players);
            // Host có thể start nếu tất cả đều ready (ít nhất 1 người)
            if (isHost && players != null && !players.isEmpty()) {
                boolean allReady = true;
                for (PlayerState p : players) {
                    if (!p.isReady() && !p.getUid().equals(localUid)) {
                        allReady = false; break;
                    }
                }
                binding.btnStart.setEnabled(players.size() >= 1);
                binding.tvWaiting.setText(allReady ? "Tất cả sẵn sàng! ✓" : "Chờ mọi người ready...");
            }
        });
    }

    private void startGame() {
        // Chỉ host được set status → playing (security rules enforce này)
        db.setRoomStatus(roomId, "playing", new FirestoreManager.OnCompleteCallback() {
            @Override public void onSuccess() { goToGame(); }
            @Override public void onFailure(String error) {
                Toast.makeText(LobbyActivity.this, "Lỗi start: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToGame() {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_ROOM_ID, roomId);
        intent.putExtra(GameActivity.EXTRA_MAP_SEED, mapSeed);
        intent.putExtra(GameActivity.EXTRA_OFFLINE, false);
        startActivity(intent);
        finish();
    }

    private void leaveRoom() {
        if (playersListener != null) playersListener.remove();
        if (roomListener != null) roomListener.remove();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveRoom();
    }
}
