package com.example.musicparty;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicparty.auth.AuthManager;
import com.example.musicparty.auth.AuthValidator;
import com.example.musicparty.ui.AmbientGradientView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private AuthManager authManager;

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private TextInputLayout guestNameInputLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private TextInputEditText guestNameInput;
    private MaterialButton emailAuthButton;
    private MaterialButton toggleModeButton;
    private MaterialButton googleSignInButton;
    private MaterialButton guestSignInButton;
    private ProgressBar loadingProgress;

    private boolean registerMode;
    private boolean guestMode;
    private boolean googleFlowInProgress;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                googleFlowInProgress = false;
                if (result.getResultCode() != RESULT_OK) {
                    stopLoading();
                    if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(this, R.string.google_sign_in_canceled, Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                startLoading();
                authManager.handleGoogleSignInResult(result.getData(), new AuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        stopLoading();
                        openMainScreen();
                    }

                    @Override
                    public void onError(String message) {
                        stopLoading();
                        showServerAuthError(message);
                    }
                });
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = AuthManager.getInstance(this);
        if (authManager.isLoggedIn()) {
            openMainScreen();
            return;
        }

        setContentView(R.layout.activity_login);
        bindViews();
        setupListeners();
        updateModeUi();
    }

    private void bindViews() {
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        guestNameInputLayout = findViewById(R.id.guestNameInputLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        guestNameInput = findViewById(R.id.guestNameInput);
        emailAuthButton = findViewById(R.id.emailAuthButton);
        toggleModeButton = findViewById(R.id.toggleModeButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        guestSignInButton = findViewById(R.id.guestSignInButton);
        loadingProgress = findViewById(R.id.loadingProgress);

        AmbientGradientView ambientBackground = findViewById(R.id.ambientBackground);
        ambientBackground.setIdleAtmosphere();
    }

    private void setupListeners() {
        emailAuthButton.setOnClickListener(v -> submitEmailAuth());
        toggleModeButton.setOnClickListener(v -> {
            registerMode = !registerMode;
            guestMode = false;
            clearFieldErrors();
            updateModeUi();
        });
        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());
        guestSignInButton.setOnClickListener(v -> startGuestSignIn());
    }

    private void updateModeUi() {
        confirmPasswordInputLayout.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        passwordInputLayout.setHelperText(
                registerMode ? getString(R.string.password_helper_register) : null
        );
        emailAuthButton.setText(registerMode ? R.string.sign_up : R.string.sign_in);
        toggleModeButton.setText(registerMode ? R.string.have_account : R.string.need_account);

        guestNameInputLayout.setVisibility(guestMode ? View.VISIBLE : View.GONE);
        guestSignInButton.setText(guestMode ? R.string.continue_as_guest : R.string.sign_in_as_guest);
    }

    private void submitEmailAuth() {
        clearFieldErrors();
        guestMode = false;
        updateModeUi();

        String email = AuthValidator.normalizeEmail(textOf(emailInput));
        String password = rawTextOf(passwordInput);
        String confirmPassword = rawTextOf(confirmPasswordInput);

        AuthValidator.ValidationResult validation = registerMode
                ? AuthValidator.validateRegistration(email, password, confirmPassword)
                : AuthValidator.validateLogin(email, password);

        if (!validation.ok) {
            showFieldError(validation.field, validation.errorResKey);
            return;
        }

        startLoading();
        AuthManager.AuthCallback callback = new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                stopLoading();
                openMainScreen();
            }

            @Override
            public void onError(String message) {
                stopLoading();
                showServerAuthError(message);
            }
        };

        if (registerMode) {
            authManager.registerWithEmail(email, password, callback);
        } else {
            authManager.signInWithEmail(email, password, callback);
        }
    }

    private void startGuestSignIn() {
        clearFieldErrors();

        if (!guestMode) {
            guestMode = true;
            updateModeUi();
            guestNameInputLayout.requestFocus();
            return;
        }

        String guestName = textOf(guestNameInput);
        AuthValidator.ValidationResult validation = AuthValidator.validateGuestName(guestName);
        if (!validation.ok) {
            guestNameInputLayout.setError(getString(R.string.invalid_name));
            guestNameInputLayout.requestFocus();
            return;
        }

        startLoading();
        authManager.signInAsGuest(guestName, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                stopLoading();
                openMainScreen();
            }

            @Override
            public void onError(String message) {
                stopLoading();
                showServerAuthError(message);
            }
        });
    }

    private void showFieldError(AuthValidator.Field field, String errorResKey) {
        int messageRes = resolveValidationMessage(errorResKey);
        TextInputLayout target = layoutForField(field);
        if (target != null) {
            target.setError(getString(messageRes));
            target.requestFocus();
        } else {
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
        }
    }

    private TextInputLayout layoutForField(AuthValidator.Field field) {
        if (field == null) {
            return null;
        }
        switch (field) {
            case EMAIL:
                return emailInputLayout;
            case PASSWORD:
                return passwordInputLayout;
            case CONFIRM_PASSWORD:
                return confirmPasswordInputLayout;
            case NAME:
                return guestNameInputLayout;
            default:
                return null;
        }
    }

    private int resolveValidationMessage(String errorResKey) {
        if ("invalid_email".equals(errorResKey)) {
            return R.string.invalid_email;
        }
        if ("invalid_password".equals(errorResKey)) {
            return R.string.invalid_password;
        }
        if ("weak_password".equals(errorResKey)) {
            return R.string.weak_password;
        }
        if ("passwords_mismatch".equals(errorResKey)) {
            return R.string.passwords_mismatch;
        }
        if ("invalid_name".equals(errorResKey)) {
            return R.string.invalid_name;
        }
        return R.string.unknown_error;
    }

    private void showServerAuthError(String codeOrMessage) {
        if ("EMAIL_ALREADY_IN_USE".equals(codeOrMessage)) {
            emailInputLayout.setError(getString(R.string.email_already_in_use));
            emailInputLayout.requestFocus();
            return;
        }
        if ("INVALID_EMAIL".equals(codeOrMessage)) {
            emailInputLayout.setError(getString(R.string.invalid_email));
            emailInputLayout.requestFocus();
            return;
        }
        if ("WEAK_PASSWORD".equals(codeOrMessage)) {
            passwordInputLayout.setError(getString(R.string.weak_password));
            passwordInputLayout.requestFocus();
            return;
        }
        if ("WRONG_PASSWORD".equals(codeOrMessage)) {
            passwordInputLayout.setError(getString(R.string.wrong_password));
            passwordInputLayout.requestFocus();
            return;
        }
        if ("USER_NOT_FOUND".equals(codeOrMessage)) {
            emailInputLayout.setError(getString(R.string.user_not_found));
            emailInputLayout.requestFocus();
            return;
        }
        if ("USER_DISABLED".equals(codeOrMessage)) {
            Toast.makeText(this, R.string.user_disabled, Toast.LENGTH_LONG).show();
            return;
        }
        if ("TOO_MANY_REQUESTS".equals(codeOrMessage)) {
            Toast.makeText(this, R.string.too_many_requests, Toast.LENGTH_LONG).show();
            return;
        }
        if ("NETWORK_ERROR".equals(codeOrMessage)) {
            Toast.makeText(this, R.string.network_error, Toast.LENGTH_LONG).show();
            return;
        }

        String safeMessage = TextUtils.isEmpty(codeOrMessage)
                ? getString(R.string.unknown_error)
                : codeOrMessage;
        Toast.makeText(this, getString(R.string.auth_error, safeMessage), Toast.LENGTH_LONG).show();
    }

    private void clearFieldErrors() {
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
        guestNameInputLayout.setError(null);
    }

    private void startGoogleSignIn() {
        guestMode = false;
        updateModeUi();

        if (!authManager.isGoogleSignInAvailable()) {
            Toast.makeText(this, R.string.google_not_configured, Toast.LENGTH_LONG).show();
            return;
        }
        if (googleFlowInProgress) {
            return;
        }

        googleFlowInProgress = true;
        googleSignInButton.setEnabled(false);
        authManager.prepareGoogleSignInIntent(this, new AuthManager.IntentCallback() {
            @Override
            public void onIntentReady(Intent intent) {
                googleSignInLauncher.launch(intent);
            }

            @Override
            public void onError(String message) {
                googleFlowInProgress = false;
                googleSignInButton.setEnabled(true);
                showServerAuthError(message);
            }
        });
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startLoading() {
        emailAuthButton.setEnabled(false);
        toggleModeButton.setEnabled(false);
        googleSignInButton.setEnabled(false);
        guestSignInButton.setEnabled(false);
        loadingProgress.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        emailAuthButton.setEnabled(true);
        toggleModeButton.setEnabled(true);
        googleSignInButton.setEnabled(true);
        guestSignInButton.setEnabled(true);
        loadingProgress.setVisibility(View.GONE);
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private static String rawTextOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
