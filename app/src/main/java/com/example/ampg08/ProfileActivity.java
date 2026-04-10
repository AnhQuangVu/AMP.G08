package com.example.ampg08;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.ampg08.databinding.ActivityProfileBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private final FirebaseAuthManager auth = FirebaseAuthManager.getInstance();
    private final FirestoreManager db = FirestoreManager.getInstance();
    private String localUid;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) uploadAvatar(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        localUid = auth.getCurrentUid();
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveName());

        // ✅ Chạm vào avatar để chọn ảnh mới
        binding.ivAvatar.setOnClickListener(v -> openImagePicker());

        loadProfile();
    }

    private void loadProfile() {
        if (localUid == null) return;
        db.getUser(localUid, user -> {
            if (user != null) {
                binding.etDisplayName.setText(user.getDisplayName());
                binding.tvEmail.setText(
                        auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "");
                binding.tvWins.setText(String.valueOf(user.getTotalWins()));
                binding.tvMatches.setText(String.valueOf(user.getTotalMatches()));
                binding.tvWinRate.setText(
                        String.format(java.util.Locale.US, "%.0f%%", user.getWinRate()));

                // ✅ Hiển thị avatar nếu có
                loadAvatar(user.getAvatarUrl());
            }
        });
    }

    /** Dùng Glide để load avatar theo URL, fallback về icon mặc định */
    private void loadAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(binding.ivAvatar);
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_profile);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        pickImageLauncher.launch(intent);
    }

    private void uploadAvatar(Uri imageUri) {
        if (localUid == null || localUid.trim().isEmpty()) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressAvatar.setVisibility(View.VISIBLE);
        binding.ivAvatar.setAlpha(0.5f);

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("avatars/" + localUid + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            String url = downloadUri.toString();
                            // ✅ Lưu URL vào Firestore
                            db.updateAvatarUrl(localUid, url, new FirestoreManager.OnCompleteCallback() {
                                @Override public void onSuccess() {
                                    binding.progressAvatar.setVisibility(View.GONE);
                                    binding.ivAvatar.setAlpha(1f);
                                    loadAvatar(url);
                                    Toast.makeText(ProfileActivity.this,
                                            "Đã cập nhật ảnh đại diện!", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onFailure(String error) {
                                    resetAvatarUI();
                                    Toast.makeText(ProfileActivity.this,
                                            "Lỗi lưu URL: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                )
                .addOnFailureListener(e -> {
                    resetAvatarUI();
                    Toast.makeText(this, "Upload thất bại: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void resetAvatarUI() {
        binding.progressAvatar.setVisibility(View.GONE);
        binding.ivAvatar.setAlpha(1f);
    }

    private void saveName() {
        if (localUid == null || localUid.trim().isEmpty()) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence nameValue = binding.etDisplayName.getText();
        String newName = nameValue != null ? nameValue.toString().trim() : "";
        if (newName.isEmpty()) {
            Toast.makeText(this, "Tên không được rỗng", Toast.LENGTH_SHORT).show();
            return;
        }
        db.updateDisplayName(localUid, newName, new FirestoreManager.OnCompleteCallback() {
            @Override public void onSuccess() {
                Toast.makeText(ProfileActivity.this, "Đã lưu!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
            }
            @Override public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}