package com.example.musicparty;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.musicparty.auth.AuthManager;
import com.example.musicparty.auth.AuthValidator;
import com.example.musicparty.repository.CreateRoomCallback;
import com.example.musicparty.repository.RoomActionCallback;
import com.example.musicparty.repository.RoomExistsCallback;
import com.example.musicparty.repository.RoomManager;
import com.example.musicparty.ui.AmbientGradientView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final long OPERATION_TIMEOUT_MS = 15000L;

    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USER_NAME = "extra_user_name";
    public static final String EXTRA_ROOM_CODE = "extra_room_code";
    public static final String EXTRA_ROOM_NAME = "extra_room_name";

    private TextView welcomeText;
    private TextInputLayout roomNameInputLayout;
    private TextInputEditText roomNameInput;
    private TextInputEditText roomCodeInput;
    private MaterialButton createRoomButton;
    private MaterialButton joinRoomButton;
    private MaterialButton signOutButton;
    private ProgressBar loadingProgress;

    private String userId;
    private String userName;
    private RoomManager roomManager;
    private AuthManager authManager;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingTimeout;

    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = AuthManager.getInstance(this);
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            openLoginScreen();
            return;
        }
        userId = user.getUid();
        userName = authManager.getDisplayName();

        setContentView(R.layout.activity_main);

        roomManager = RoomManager.getInstance();

        AmbientGradientView ambientBackground = findViewById(R.id.ambientBackground);
        ambientBackground.setIdleAtmosphere();

        welcomeText = findViewById(R.id.welcomeText);
        roomNameInputLayout = findViewById(R.id.roomNameInputLayout);
        roomNameInput = findViewById(R.id.roomNameInput);
        roomCodeInput = findViewById(R.id.roomCodeInput);
        createRoomButton = findViewById(R.id.createRoomButton);
        joinRoomButton = findViewById(R.id.joinRoomButton);
        signOutButton = findViewById(R.id.signOutButton);
        loadingProgress = findViewById(R.id.loadingProgress);

        welcomeText.setText(getString(R.string.welcome_user, userName));

        createRoomButton.setOnClickListener(v -> createRoom());
        joinRoomButton.setOnClickListener(v -> joinRoom());
        signOutButton.setOnClickListener(v -> signOut());

        requestPermissionsIfNeeded();
    }

    private void createRoom() {
        roomNameInputLayout.setError(null);
        String roomName = roomNameInput.getText() != null
                ? roomNameInput.getText().toString().trim()
                : "";

        if (!AuthValidator.isValidRoomName(roomName)) {
            roomNameInputLayout.setError(getString(R.string.invalid_room_name));
            roomNameInputLayout.requestFocus();
            return;
        }

        startLoading();
        final String finalRoomName = roomName;
        roomManager.createRoom(userId, userName, roomName, new CreateRoomCallback() {
            @Override
            public void onSuccess(String roomCode) {
                stopLoading();
                openHostRoom(roomCode, finalRoomName);
            }

            @Override
            public void onError(String message) {
                stopLoading();
                showFirebaseError(message);
            }
        });
    }

    private void joinRoom() {
        String roomCode = roomCodeInput.getText() != null
                ? roomCodeInput.getText().toString().trim()
                : "";

        if (roomCode.length() != 6) {
            Toast.makeText(this, R.string.invalid_room_code, Toast.LENGTH_SHORT).show();
            return;
        }

        startLoading();
        roomManager.checkRoomExists(roomCode, new RoomExistsCallback() {
            @Override
            public void onResult(boolean exists) {
                if (!exists) {
                    stopLoading();
                    Toast.makeText(MainActivity.this, R.string.room_not_found, Toast.LENGTH_SHORT).show();
                    return;
                }

                roomManager.joinRoom(roomCode, userId, userName, new RoomActionCallback() {
                    @Override
                    public void onSuccess() {
                        stopLoading();
                        openGuestRoom(roomCode);
                    }

                    @Override
                    public void onError(String message) {
                        stopLoading();
                        Toast.makeText(
                                MainActivity.this,
                                getString(R.string.join_failed),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }
        });
    }

    private void signOut() {
        authManager.signOut(this);
        openLoginScreen();
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openHostRoom(String roomCode, String roomName) {
        Intent intent = new Intent(this, HostRoomActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USER_NAME, userName);
        intent.putExtra(EXTRA_ROOM_CODE, roomCode);
        intent.putExtra(EXTRA_ROOM_NAME, roomName);
        startActivity(intent);
    }

    private void openGuestRoom(String roomCode) {
        Intent intent = new Intent(this, GuestRoomActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USER_NAME, userName);
        intent.putExtra(EXTRA_ROOM_CODE, roomCode);
        startActivity(intent);
    }

    private void startLoading() {
        createRoomButton.setEnabled(false);
        joinRoomButton.setEnabled(false);
        signOutButton.setEnabled(false);
        loadingProgress.setVisibility(View.VISIBLE);

        if (pendingTimeout != null) {
            timeoutHandler.removeCallbacks(pendingTimeout);
        }
        pendingTimeout = () -> {
            stopLoading();
            Toast.makeText(this, R.string.operation_timeout, Toast.LENGTH_LONG).show();
        };
        timeoutHandler.postDelayed(pendingTimeout, OPERATION_TIMEOUT_MS);
    }

    private void stopLoading() {
        createRoomButton.setEnabled(true);
        joinRoomButton.setEnabled(true);
        signOutButton.setEnabled(true);
        loadingProgress.setVisibility(View.GONE);

        if (pendingTimeout != null) {
            timeoutHandler.removeCallbacks(pendingTimeout);
            pendingTimeout = null;
        }
    }

    private void showFirebaseError(String message) {
        String safeMessage = TextUtils.isEmpty(message) ? getString(R.string.unknown_error) : message;
        Toast.makeText(
                this,
                getString(R.string.firebase_error, safeMessage),
                Toast.LENGTH_LONG
        ).show();
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                audioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
            }
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingTimeout != null) {
            timeoutHandler.removeCallbacks(pendingTimeout);
        }
        super.onDestroy();
    }
}
