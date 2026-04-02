package com.example.ampg08;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.ampg08.databinding.ActivityRegisterBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.User;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Locale;

public class RegisterActivity extends BaseActivity {

    private static final String TAG = "RegisterActivity";
    private static final int MAX_REGISTER_ATTEMPTS = 2;

    private ActivityRegisterBinding binding;
    private final FirebaseAuthManager authManager = FirebaseAuthManager.getInstance();
    private int registerAttempt;

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
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Không có kết nối mạng. Vui lòng kiểm tra Internet rồi thử lại.", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        registerAttempt = 0;
        attemptRegister(email, pass, name);
    }

    private void attemptRegister(String email, String pass, String name) {
        registerAttempt++;
        authManager.registerWithEmail(email, pass)
                .addOnSuccessListener(result -> {
                    FirebaseUser fbUser = result.getUser();
                    if (fbUser == null) {
                        setLoading(false);
                        Toast.makeText(this, "Không thể tạo tài khoản, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build();
                    fbUser.updateProfile(profileUpdate);

                    User user = new User(fbUser.getUid(), name, "", 0, 0);
                    FirestoreManager.getInstance().createUser(user, new FirestoreManager.OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            completeRegistrationSuccess(email, false);
                        }

                        @Override
                        public void onFailure(String error) {
                            FirebaseUser currentUser = authManager.getCurrentUser();
                            boolean authAlreadyCreated = currentUser != null
                                    && currentUser.getEmail() != null
                                    && currentUser.getEmail().equalsIgnoreCase(email);

                            if (authAlreadyCreated) {
                                Log.w(TAG, "Profile creation failed but auth account already created: " + error);
                                completeRegistrationSuccess(email, true);
                                return;
                            }

                            setLoading(false);
                            Toast.makeText(RegisterActivity.this, "Lỗi tạo hồ sơ: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (shouldRetryRecaptchaFailure(e) && registerAttempt < MAX_REGISTER_ATTEMPTS) {
                        binding.getRoot().postDelayed(() -> attemptRegister(email, pass, name), 1200L);
                        return;
                    }

                    logAuthFailure(e, email);
                    setLoading(false);
                    Toast.makeText(this, mapRegisterError(e), Toast.LENGTH_LONG).show();
                });
    }

    private boolean shouldRetryRecaptchaFailure(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.US);
        return lower.contains("recaptcha") && lower.contains("network error");
    }

    private String mapRegisterError(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return "Mạng đang không ổn định. Vui lòng thử lại sau ít phút.";
        }

        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) {
                return "Email này đã được đăng ký.";
            }
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "Email không hợp lệ.";
            }
            if ("ERROR_WEAK_PASSWORD".equals(code)) {
                return "Mật khẩu quá yếu, vui lòng dùng mật khẩu mạnh hơn.";
            }
            if ("ERROR_TOO_MANY_REQUESTS".equals(code)) {
                return "Bạn thao tác quá nhanh. Vui lòng đợi một chút rồi thử lại.";
            }
        }

        if (shouldRetryRecaptchaFailure(e)) {
            return "Không kết nối được dịch vụ xác minh Google (reCAPTCHA).\n"
                    + "Hãy tắt VPN/Proxy, kiểm tra ngày giờ thiết bị, cập nhật Google Play Services rồi thử lại.";
        }

        return "Đăng ký thất bại: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định");
    }

    private void logAuthFailure(Exception e, String email) {
        String code = (e instanceof FirebaseAuthException)
                ? ((FirebaseAuthException) e).getErrorCode()
                : "N/A";
        Log.e(TAG, "register failed - email=" + email + ", code=" + code + ", attempt=" + registerAttempt, e);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }

        //noinspection deprecation
        android.net.NetworkInfo info = cm.getActiveNetworkInfo();
        //noinspection deprecation
        return info != null && info.isConnected();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
    }

    private void completeRegistrationSuccess(String email, boolean profilePending) {
        setLoading(false);
        authManager.signOut(this);

        String message = profilePending
                ? "Đăng ký thành công. Hồ sơ sẽ được hoàn tất khi bạn đăng nhập lại."
                : "Đăng ký thành công. Mời bạn đăng nhập.";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("prefill_email", email);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
