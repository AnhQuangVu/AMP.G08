package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.ampg08.databinding.ActivityLoginBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private FirebaseAuthManager authManager;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleGoogleSignInResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        authManager = FirebaseAuthManager.getInstance();
        try {
            authManager.initGoogleSignIn(this, getString(R.string.default_web_client_id));
        } catch (Exception e) {
            Log.e(TAG, "Google Sign-In init failed", e);
            Toast.makeText(this,
                    "Google Sign-In chưa cấu hình đúng. Kiểm tra google-services.json và SHA-1.",
                    Toast.LENGTH_LONG).show();
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnLogin.setOnClickListener(v -> doEmailLogin());
        binding.btnGoogle.setOnClickListener(v -> doGoogleSignIn());
        binding.tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        String prefillEmail = getIntent().getStringExtra("prefill_email");
        if (!TextUtils.isEmpty(prefillEmail)) {
            binding.etEmail.setText(prefillEmail);
            binding.etPassword.requestFocus();
        }
    }

    private void doEmailLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String pass  = binding.etPassword.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        authManager.loginWithEmail(email, pass)
                .addOnSuccessListener(result -> goHome())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void doGoogleSignIn() {
        if (!authManager.isGooglePlayServicesAvailable(this)) {
            showGooglePlayServicesError(authManager.getGooglePlayServicesStatus(this));
            return;
        }
        try {
            setLoading(true);
            googleSignInLauncher.launch(authManager.getGoogleSignInIntent());
        } catch (Exception e) {
            setLoading(false);
            Log.e(TAG, "Cannot launch Google Sign-In", e);
            Toast.makeText(this,
                    "Không thể mở Google Sign-In. Kiểm tra cấu hình và thử lại.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            setLoading(false);
            if (result.getData() != null) {
                Task<GoogleSignInAccount> t = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try { t.getResult(ApiException.class); }
                catch (ApiException ex) {
                    Toast.makeText(this, "GG error code=" + ex.getStatusCode(), Toast.LENGTH_LONG).show();
                    return;
                }
            }
            Toast.makeText(this, "GG canceled (no data)", Toast.LENGTH_LONG).show();
            return;
        }

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null || account.getIdToken() == null) {
                setLoading(false);
                Toast.makeText(this,
                        "Không nhận được token Google. Vui lòng thử lại.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            setLoading(true);
            authManager.firebaseAuthWithGoogle(account)
                    .addOnSuccessListener(r -> {
                        ensureUserDoc(r.getUser());
                        goHome();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Log.e(TAG, "Firebase auth with Google failed", e);
                        Toast.makeText(this,
                                authManager.mapFirebaseGoogleAuthError(e),
                                Toast.LENGTH_LONG).show();
                    });
        } catch (ApiException e) {
            setLoading(false);
            Log.e(TAG, "Google Sign-In failed, code=" + e.getStatusCode(), e);
            Toast.makeText(this, "ApiException code=" + e.getStatusCode(), Toast.LENGTH_LONG).show();
        }
    }

    private void showGooglePlayServicesError(int status) {
        if (status == ConnectionResult.SERVICE_INVALID
                || status == ConnectionResult.SERVICE_MISSING
                || status == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED
                || status == ConnectionResult.SERVICE_DISABLED) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, status, 9002).show();
            return;
        }
        Toast.makeText(this,
                "Thiết bị chưa hỗ trợ Google Play Services (code " + status + ")",
                Toast.LENGTH_LONG).show();
    }

    private void ensureUserDoc(FirebaseUser fbUser) {
        if (fbUser == null) return;
        FirestoreManager.getInstance().getUser(fbUser.getUid(), user -> {
            if (user == null) {
                String name = fbUser.getDisplayName() != null ? fbUser.getDisplayName() : fbUser.getEmail();
                User newUser = new User(fbUser.getUid(), name, "", 0, 0);
                FirestoreManager.getInstance().createUser(newUser, new FirestoreManager.OnCompleteCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String error) {}
                });
            }
        });
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogle.setEnabled(!loading);
    }
}
