package com.example.ampg08;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.ampg08.databinding.ActivityLoginBinding;
import com.example.ampg08.firebase.FirebaseAuthManager;
import com.example.ampg08.firebase.FirestoreManager;
import com.example.ampg08.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        authManager = FirebaseAuthManager.getInstance();
        authManager.initGoogleSignIn(this, getString(R.string.default_web_client_id));

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
        Intent signInIntent = authManager.getGoogleSignInIntent();
        startActivityForResult(signInIntent, FirebaseAuthManager.RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FirebaseAuthManager.RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                setLoading(true);
                authManager.firebaseAuthWithGoogle(account)
                        .addOnSuccessListener(result -> {
                            ensureUserDoc(result.getUser());
                            goHome();
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(this, "Google thất bại", Toast.LENGTH_SHORT).show();
                        });
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In lỗi", Toast.LENGTH_SHORT).show();
            }
        }
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