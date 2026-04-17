package com.example.ampg08.firebase;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Locale;

public class FirebaseAuthManager {

    public static final int RC_SIGN_IN = 9001;
    private static FirebaseAuthManager instance;
    private final FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private String cachedWebClientId;

    public interface AuthOperationCallback {
        void onComplete();
    }

    private FirebaseAuthManager() {
        auth = FirebaseAuth.getInstance();
    }

    public static FirebaseAuthManager getInstance() {
        if (instance == null) instance = new FirebaseAuthManager();
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public Task<com.google.firebase.auth.AuthResult> loginWithEmail(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<com.google.firebase.auth.AuthResult> registerWithEmail(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    public void initGoogleSignIn(Activity activity, String webClientId) {
        googleSignInClient = ensureGoogleSignInClient(activity, webClientId);
    }

    private GoogleSignInClient ensureGoogleSignInClient(Activity activity, String webClientId) {
        String resolvedWebClientId = !TextUtils.isEmpty(webClientId) ? webClientId : cachedWebClientId;
        if (TextUtils.isEmpty(resolvedWebClientId) || resolvedWebClientId.contains("YOUR_WEB_CLIENT_ID")) {
            throw new IllegalArgumentException("default_web_client_id is invalid or not configured");
        }

        cachedWebClientId = resolvedWebClientId;
        if (googleSignInClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(resolvedWebClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(activity, gso);
        }
        return googleSignInClient;
    }

    public boolean isGooglePlayServicesAvailable(Activity activity) {
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
        return status == ConnectionResult.SUCCESS;
    }

    public int getGooglePlayServicesStatus(Activity activity) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
    }

    public Intent getGoogleSignInIntent() {
        if (googleSignInClient == null) {
            throw new IllegalStateException("GoogleSignInClient is not initialized");
        }
        return googleSignInClient.getSignInIntent();
    }

    /**
     * Clear Google session before launching sign-in so account picker can show another account.
     */
    public void clearGoogleSessionForPicker(Activity activity, String webClientId, AuthOperationCallback callback) {
        GoogleSignInClient client = ensureGoogleSignInClient(activity, webClientId);
        client.signOut().addOnCompleteListener(activity, task -> {
            if (callback != null) callback.onComplete();
        });
    }

    public Task<com.google.firebase.auth.AuthResult> firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        return auth.signInWithCredential(credential);
    }

    public Task<GoogleSignInAccount> getSignedInGoogleAccount(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        return task;
    }

    public void signOut(Activity activity) {
        auth.signOut();

        GoogleSignInClient client = googleSignInClient;
        if (client == null) {
            client = GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN);
        }
        client.signOut();
    }

    public void signOut(Activity activity, String webClientId, AuthOperationCallback callback) {
        auth.signOut();
        GoogleSignInClient client = ensureGoogleSignInClient(activity, webClientId);
        client.signOut().addOnCompleteListener(activity, task -> {
            if (callback != null) callback.onComplete();
        });
    }

    public String mapGoogleSignInError(ApiException e) {
        int code = e.getStatusCode();
        if (code == 10 || code == 12500) {
            return "Google Sign-In chưa cấu hình đúng (SHA-1/SHA-256 hoặc OAuth).";
        }
        if (code == 7) {
            return "Không có kết nối mạng ổn định. Vui lòng thử lại.";
        }
        if (code == 12501) {
            return "Bạn đã hủy đăng nhập Google.";
        }
        return "Google Sign-In lỗi (code " + code + ").";
    }

    public String mapFirebaseGoogleAuthError(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return "Mạng đang không ổn định. Vui lòng thử lại.";
        }

        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            if ("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL".equals(code)) {
                return "Email đã liên kết với phương thức đăng nhập khác.";
            }
            if ("ERROR_INVALID_CREDENTIAL".equals(code)) {
                return "Thông tin Google không hợp lệ. Vui lòng đăng nhập lại.";
            }
            if ("ERROR_TOO_MANY_REQUESTS".equals(code)) {
                return "Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút.";
            }
        }

        String msg = e.getMessage();
        if (msg != null && msg.toLowerCase(Locale.US).contains("developer_error")) {
            return "Google Sign-In chưa cấu hình đúng (SHA-1/SHA-256 hoặc OAuth).";
        }
        return "Đăng nhập Google thất bại.";
    }

    public String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCurrentDisplayName() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? (user.getDisplayName() != null ? user.getDisplayName() : user.getEmail()) : "Guest";
    }
}
