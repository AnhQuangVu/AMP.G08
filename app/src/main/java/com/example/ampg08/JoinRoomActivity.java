package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.ampg08.databinding.ActivityJoinRoomBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.PlayerState;

public class JoinRoomActivity extends BaseActivity {

    private ActivityJoinRoomBinding binding;
    private final FirebaseAuthManager auth = FirebaseAuthManager.getInstance();
    private final FirestoreManager db = FirestoreManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJoinRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnJoin.setOnClickListener(v -> joinRoom());
    }

    private void joinRoom() {
        String roomId = binding.etRoomCode.getText().toString().trim();
        if (TextUtils.isEmpty(roomId)) {
            Toast.makeText(this, "Nhập mã phòng", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUid();
        if (uid == null) { Toast.makeText(this, "Cần đăng nhập", Toast.LENGTH_SHORT).show(); return; }

        binding.btnJoin.setEnabled(false);
        db.getRoomByCode(roomId, room -> {
            if (room == null) {
                binding.btnJoin.setEnabled(true);
                Toast.makeText(this, "Không tìm thấy phòng", Toast.LENGTH_SHORT).show();
                return;
            }
            if ("playing".equals(room.getStatus()) || "ended".equals(room.getStatus())) {
                binding.btnJoin.setEnabled(true);
                Toast.makeText(this, "Phòng đã bắt đầu hoặc kết thúc", Toast.LENGTH_SHORT).show();
                return;
            }

            // Thêm uid vào danh sách players
            db.joinRoom(roomId, uid, new FirestoreManager.OnCompleteCallback() {
                @Override
                public void onSuccess() {
                    // Thêm PlayerState vào subcollection
                    PlayerState ps = new PlayerState(uid, auth.getCurrentDisplayName());
                    db.setPlayerState(roomId, ps, new FirestoreManager.OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            Intent intent = new Intent(JoinRoomActivity.this, LobbyActivity.class);
                            intent.putExtra(LobbyActivity.EXTRA_ROOM_ID, roomId);
                            intent.putExtra(LobbyActivity.EXTRA_IS_HOST, false);
                            startActivity(intent);
                            finish();
                        }
                        @Override
                        public void onFailure(String e) { binding.btnJoin.setEnabled(true); }
                    });
                }
                @Override
                public void onFailure(String error) {
                    binding.btnJoin.setEnabled(true);
                    Toast.makeText(JoinRoomActivity.this, "Lỗi vào phòng: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
