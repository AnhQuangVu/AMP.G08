package com.example.ampg08.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.ampg08.databinding.ActivityCreateRoomBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.PlayerState;
import com.example.ampg08.model.Room;

public class CreateRoomActivity extends BaseActivity {

    public static final String EXTRA_MAP_SEED = "extra_map_seed";
    public static final String EXTRA_PLAYER_COUNT = "extra_player_count";
    public static final String EXTRA_TIME_LIMIT_SECONDS = "extra_time_limit_seconds";
    public static final String EXTRA_AI_DIFFICULTY = "extra_ai_difficulty";
    public static final String EXTRA_MODE = "extra_mode";

    private ActivityCreateRoomBinding binding;
    private final FirebaseAuthManager auth = FirebaseAuthManager.getInstance();
    private final FirestoreManager db = FirestoreManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnCreate.setOnClickListener(v -> createRoom());
    }

    private void createRoom() {
        long seed = getIntent().getLongExtra(EXTRA_MAP_SEED, System.currentTimeMillis());
        int playerCount = getIntent().getIntExtra(EXTRA_PLAYER_COUNT, 2);
        int timeLimitSeconds = getIntent().getIntExtra(EXTRA_TIME_LIMIT_SECONDS, 180);
        String aiDifficulty = getIntent().getStringExtra(EXTRA_AI_DIFFICULTY);
        String mode = getIntent().getStringExtra(EXTRA_MODE);

        String hostUid = auth.getCurrentUid();
        if (hostUid == null) {
            Toast.makeText(this, "Cần đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnCreate.setEnabled(false);
        Room room = new Room("", hostUid, seed);
        room.setPlayerLimit(Math.max(2, Math.min(4, playerCount)));
        room.setTimeLimitSeconds(timeLimitSeconds);
        room.setAiDifficulty(aiDifficulty != null ? aiDifficulty : "EASY");
        room.setMode(mode != null ? mode : "VS_PLAYER");

        db.createRoom(room, createdRoom -> {
            if (createdRoom == null) {
                binding.btnCreate.setEnabled(true);
                Toast.makeText(this, "Tạo phòng thất bại", Toast.LENGTH_SHORT).show();
                return;
            }
            // Thêm host vào subcollection players
            PlayerState ps = new PlayerState(hostUid, auth.getCurrentDisplayName());
            db.setPlayerState(createdRoom.getRoomId(), ps, new FirestoreManager.OnCompleteCallback() {
                @Override
                public void onSuccess() {
                    // Hiển thị Room Code
                    binding.tvRoomCode.setText(createdRoom.getRoomId());
                    binding.tvRoomCode.setVisibility(android.view.View.VISIBLE);
                    binding.tvHintCopy.setVisibility(android.view.View.VISIBLE);
                    binding.btnCopy.setVisibility(android.view.View.VISIBLE);
                    binding.btnGoLobby.setVisibility(android.view.View.VISIBLE);

                    binding.btnCopy.setOnClickListener(v -> {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("RoomCode", createdRoom.getRoomId()));
                        Toast.makeText(CreateRoomActivity.this, "Đã copy mã phòng!", Toast.LENGTH_SHORT).show();
                    });

                    binding.btnGoLobby.setOnClickListener(v -> {
                        Intent intent = new Intent(CreateRoomActivity.this, LobbyActivity.class);
                        intent.putExtra(LobbyActivity.EXTRA_ROOM_ID, createdRoom.getRoomId());
                        intent.putExtra(LobbyActivity.EXTRA_IS_HOST, true);
                        intent.putExtra(LobbyActivity.EXTRA_PLAYER_LIMIT, createdRoom.getPlayerLimit());
                        intent.putExtra(LobbyActivity.EXTRA_TIME_LIMIT_SECONDS, createdRoom.getTimeLimitSeconds());
                        startActivity(intent);
                        finish();
                    });
                }
                @Override
                public void onFailure(String error) {
                    binding.btnCreate.setEnabled(true);
                    Toast.makeText(CreateRoomActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
