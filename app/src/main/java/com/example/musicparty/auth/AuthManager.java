package com.example.musicparty.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.musicparty.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

public final class AuthManager {

    private static final String TAG = "AuthManager";

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);

        void onError(String message);
    }

    public interface IntentCallback {
        void onIntentReady(Intent intent);

        void onError(String message);
    }

    private static AuthManager instance;

    private final Context appContext;
    private final FirebaseAuth firebaseAuth;

    private AuthManager(Context context) {
        appContext = context.getApplicationContext();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void signInWithEmail(String email, String password, AuthCallback callback) {
        String normalizedEmail = AuthValidator.normalizeEmail(email);
        firebaseAuth.signInWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(error -> callback.onError(mapAuthException(error)));
    }

    public void registerWithEmail(String email, String password, AuthCallback callback) {
        String normalizedEmail = AuthValidator.normalizeEmail(email);
        firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError(appContext.getString(R.string.user_not_created));
                        return;
                    }
                    String resolvedName = normalizedEmail.split("@")[0];
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(resolvedName)
                            .build();
                    user.updateProfile(profile)
                            .addOnCompleteListener(task -> callback.onSuccess(user));
                })
                .addOnFailureListener(error -> callback.onError(mapAuthException(error)));
    }

    public void signInAsGuest(String displayName, AuthCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null && currentUser.isAnonymous()) {
            updateGuestDisplayName(currentUser, displayName, callback);
            return;
        }

        if (currentUser != null) {
            firebaseAuth.signOut();
        }

        firebaseAuth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError(appContext.getString(R.string.guest_sign_in_failed));
                        return;
                    }
                    updateGuestDisplayName(user, displayName, callback);
                })
                .addOnFailureListener(error -> callback.onError(mapAuthException(error)));
    }

    private void updateGuestDisplayName(FirebaseUser user, String displayName, AuthCallback callback) {
        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build();
        user.updateProfile(profile)
                .addOnCompleteListener(task -> callback.onSuccess(user));
    }

    public String getDisplayName() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return "";
        }
        if (!TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName().trim();
        }
        if (!TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail().split("@")[0];
        }
        return user.isAnonymous()
                ? appContext.getString(R.string.user_guest)
                : appContext.getString(R.string.user_default);
    }

    public boolean isGoogleSignInAvailable() {
        return !TextUtils.isEmpty(resolveWebClientId());
    }

    /**
     * Clears any stuck Google session, then returns a fresh sign-in intent.
     */
    public void prepareGoogleSignInIntent(Activity activity, IntentCallback callback) {
        String webClientId = resolveWebClientId();
        if (TextUtils.isEmpty(webClientId)) {
            callback.onError(appContext.getString(R.string.google_sign_in_not_configured));
            return;
        }

        GoogleSignInClient client = googleClient(activity, webClientId);
        client.signOut().addOnCompleteListener(unused -> {
            Intent intent = client.getSignInIntent();
            if (intent == null) {
                callback.onError(appContext.getString(R.string.google_sign_in_open_failed));
                return;
            }
            callback.onIntentReady(intent);
        });
    }

    public void handleGoogleSignInResult(@Nullable Intent data, AuthCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null || TextUtils.isEmpty(account.getIdToken())) {
                callback.onError(appContext.getString(R.string.google_token_missing));
                return;
            }
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                    .addOnFailureListener(error -> callback.onError(mapAuthException(error)));
        } catch (ApiException error) {
            Log.e(TAG, "Google sign-in failed, status=" + error.getStatusCode(), error);
            callback.onError(googleErrorMessage(error.getStatusCode(), error.getMessage()));
        }
    }

    public void signOut(Activity activity) {
        firebaseAuth.signOut();
        String webClientId = resolveWebClientId();
        if (!TextUtils.isEmpty(webClientId)) {
            googleClient(activity, webClientId).signOut();
        }
    }

    private GoogleSignInClient googleClient(Activity activity, String webClientId) {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(activity, options);
    }

    @Nullable
    private String resolveWebClientId() {
        int generatedResId = appContext.getResources().getIdentifier(
                "default_web_client_id",
                "string",
                appContext.getPackageName()
        );
        if (generatedResId != 0) {
            String value = appContext.getString(generatedResId).trim();
            if (!TextUtils.isEmpty(value) && !value.startsWith("REPLACE_")) {
                return value;
            }
        }

        int manualResId = appContext.getResources().getIdentifier(
                "google_web_client_id",
                "string",
                appContext.getPackageName()
        );
        if (manualResId != 0) {
            String value = appContext.getString(manualResId).trim();
            if (!TextUtils.isEmpty(value) && !value.startsWith("REPLACE_")) {
                return value;
            }
        }
        return null;
    }

    private String googleErrorMessage(int statusCode, @Nullable String message) {
        switch (statusCode) {
            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                return appContext.getString(R.string.google_sign_in_canceled);
            case GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS:
                return appContext.getString(R.string.google_sign_in_in_progress);
            case CommonStatusCodes.NETWORK_ERROR:
                return appContext.getString(R.string.google_network_error);
            case CommonStatusCodes.DEVELOPER_ERROR:
            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                return appContext.getString(R.string.google_config_error, statusCode);
            default:
                return appContext.getString(
                        R.string.google_sign_in_error,
                        statusCode,
                        TextUtils.isEmpty(message)
                                ? appContext.getString(R.string.unknown)
                                : message
                );
        }
    }

    private String mapAuthException(Exception error) {
        if (error instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            return "EMAIL_ALREADY_IN_USE";
        }
        if (error instanceof com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            return "WEAK_PASSWORD";
        }
        if (error instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            return "WRONG_PASSWORD";
        }
        if (error instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            String code = ((com.google.firebase.auth.FirebaseAuthInvalidUserException) error).getErrorCode();
            if ("ERROR_USER_DISABLED".equals(code) || "user-disabled".equals(code)) {
                return "USER_DISABLED";
            }
            return "USER_NOT_FOUND";
        }

        if (error instanceof com.google.firebase.auth.FirebaseAuthException) {
            String code = ((com.google.firebase.auth.FirebaseAuthException) error).getErrorCode();
            if (code != null) {
                String normalized = code.toLowerCase(java.util.Locale.ROOT);
                if (normalized.contains("email_already_in_use") || normalized.contains("email-already-in-use")) {
                    return "EMAIL_ALREADY_IN_USE";
                }
                if (normalized.contains("invalid_email") || normalized.contains("invalid-email")) {
                    return "INVALID_EMAIL";
                }
                if (normalized.contains("weak_password") || normalized.contains("weak-password")) {
                    return "WEAK_PASSWORD";
                }
                if (normalized.contains("wrong_password")
                        || normalized.contains("invalid_credential")
                        || normalized.contains("invalid-credential")) {
                    return "WRONG_PASSWORD";
                }
                if (normalized.contains("user_not_found") || normalized.contains("user-not-found")) {
                    return "USER_NOT_FOUND";
                }
                if (normalized.contains("user_disabled") || normalized.contains("user-disabled")) {
                    return "USER_DISABLED";
                }
                if (normalized.contains("too_many_requests") || normalized.contains("too-many-requests")) {
                    return "TOO_MANY_REQUESTS";
                }
                if (normalized.contains("network")) {
                    return "NETWORK_ERROR";
                }
            }
        }

        String message = error.getMessage();
        if (message != null) {
            String lower = message.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("already in use") || lower.contains("email-already-in-use")) {
                return "EMAIL_ALREADY_IN_USE";
            }
        }

        return readableError(message);
    }

    private String readableError(@Nullable String message) {
        if (TextUtils.isEmpty(message)) {
            return appContext.getString(R.string.unknown_error);
        }
        return message;
    }
}
