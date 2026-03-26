package com.example.ampg08;

import android.os.Bundle;
import android.widget.Toast;

import com.example.ampg08.databinding.ActivityProfileBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.User;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private final FirebaseAuthManager auth = FirebaseAuthManager.getInstance();
    private final FirestoreManager db = FirestoreManager.getInstance();
    private String localUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        localUid = auth.getCurrentUid();
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveName());

        loadProfile();
    }

    private void loadProfile() {
        if (localUid == null) return;
        db.getUser(localUid, user -> {
            if (user != null) {
                binding.etDisplayName.setText(user.getDisplayName());
                binding.tvEmail.setText(auth.getCurrentUser() != null
                        ? auth.getCurrentUser().getEmail() : "");
                binding.tvWins.setText(String.valueOf(user.getTotalWins()));
                binding.tvMatches.setText(String.valueOf(user.getTotalMatches()));
                binding.tvWinRate.setText(String.format(java.util.Locale.US, "%.0f%%", user.getWinRate()));
            }
        });
    }

    private void saveName() {
        String newName = binding.etDisplayName.getText().toString().trim();
        if (newName.isEmpty()) { Toast.makeText(this, "Tên không được rỗng", Toast.LENGTH_SHORT).show(); return; }
        db.updateDisplayName(localUid, newName, new FirestoreManager.OnCompleteCallback() {
            @Override public void onSuccess() {
                Toast.makeText(ProfileActivity.this, "Đã lưu!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
