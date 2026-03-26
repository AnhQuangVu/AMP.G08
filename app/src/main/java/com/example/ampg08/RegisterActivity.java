package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.ampg08.databinding.ActivityRegisterBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.User;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;
    private final FirebaseAuthManager authManager = FirebaseAuthManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v -> doRegister());
        binding.tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void doRegister() {
        String name  = binding.etDisplayName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String pass  = binding.etPassword.getText().toString();
        String pass2 = binding.etPasswordConfirm.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(pass2)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        authManager.registerWithEmail(email, pass)
                .addOnSuccessListener(result -> {
                    FirebaseUser fbUser = result.getUser();
                    if (fbUser == null) { setLoading(false); return; }

                    // Cập nhật displayName trong Firebase Auth
                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build();
                    fbUser.updateProfile(profileUpdate);

                    // Tạo document trong Firestore
                    User user = new User(fbUser.getUid(), name, "", 0, 0);
                    FirestoreManager.getInstance().createUser(user, new FirestoreManager.OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            startActivity(new Intent(RegisterActivity.this, HomeActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        }
                        @Override
                        public void onFailure(String error) {
                            setLoading(false);
                            Toast.makeText(RegisterActivity.this, "Lỗi tạo hồ sơ: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
    }
}
