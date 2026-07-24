package com.example.musicparty;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicparty.adapter.ParticipantAdapter;
import com.example.musicparty.model.RoomState;
import com.example.musicparty.model.Track;
import com.example.musicparty.player.MusicPlayer;
import com.example.musicparty.repository.RoomListener;
import com.example.musicparty.repository.RoomManager;
import com.example.musicparty.ui.AmbientAtmosphere;
import com.example.musicparty.ui.AmbientGradientView;
import com.example.musicparty.util.TimeFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HostRoomActivity extends AppCompatActivity implements RoomListener {

    private static final long SYNC_INTERVAL_MS = 3000L;
    private static final long LINK_TICK_MS = 1000L;

    private String roomCode;
    private String userId;
    private RoomManager roomManager;
    private MusicPlayer musicPlayer;
    private ParticipantAdapter participantAdapter;

    private TextView roomCodeText;
    private TextView roomNameText;
    private TextView trackTypeText;
    private TextView trackTitleText;
    private TextView trackArtistText;
    private TextView playbackTimeText;
    private SeekBar seekBar;
    private FloatingActionButton playPauseButton;
    private AmbientAtmosphere ambientAtmosphere;

    private Track loadedTrack;
    private boolean isUserSeeking;
    private boolean linkPlaying;
    private long linkPositionMs;
    private long linkTickStartRealtime;
    private long linkTickStartPosition;

    private final Handler syncHandler = new Handler(Looper.getMainLooper());
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Handler linkHandler = new Handler(Looper.getMainLooper());

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            if (loadedTrack != null && loadedTrack.isLink() && linkPlaying) {
                roomManager.updatePlayback(roomCode, true, linkPositionMs);
            } else if (musicPlayer != null && musicPlayer.isPlaying()) {
                roomManager.updatePlayback(roomCode, true, musicPlayer.getCurrentPosition());
            }
            syncHandler.postDelayed(this, SYNC_INTERVAL_MS);
        }
    };

    private final Runnable linkTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (loadedTrack == null || !loadedTrack.isLink() || !linkPlaying) {
                return;
            }
            linkPositionMs = linkTickStartPosition
                    + (SystemClock.elapsedRealtime() - linkTickStartRealtime);
            updatePlaybackUi(linkPositionMs, 0L);
            roomManager.updatePlayback(roomCode, true, linkPositionMs);
            linkHandler.postDelayed(this, LINK_TICK_MS);
        }
    };

    private final ActivityResultLauncher<Intent> trackSourceLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Track track;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    track = result.getData().getSerializableExtra(TrackPickerActivity.EXTRA_TRACK, Track.class);
                } else {
                    track = (Track) result.getData().getSerializableExtra(TrackPickerActivity.EXTRA_TRACK);
                }
                if (track != null) {
                    selectTrack(track);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_room);

        roomCode = getIntent().getStringExtra(MainActivity.EXTRA_ROOM_CODE);
        userId = getIntent().getStringExtra(MainActivity.EXTRA_USER_ID);

        roomManager = RoomManager.getInstance();
        musicPlayer = new MusicPlayer(this);

        setupToolbar();
        bindViews();
        setupParticipantsList();
        setupPlayerControls();
        setupSourceButtons();
        setupBackNavigation();
        setupCloseRoomButton();

        roomCodeText.setText(roomCode);
        String roomName = getIntent().getStringExtra(MainActivity.EXTRA_ROOM_NAME);
        updateRoomNameUi(roomName);
        updateTrackUi(null);
    }

    private void updateRoomNameUi(String roomName) {
        if (TextUtils.isEmpty(roomName)) {
            RoomState roomState = roomManager.getRoom(roomCode);
            if (roomState != null) {
                roomName = roomState.getRoomName();
            }
        }
        if (TextUtils.isEmpty(roomName)) {
            roomNameText.setVisibility(View.GONE);
            return;
        }
        roomNameText.setText(roomName);
        roomNameText.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        roomManager.addListener(roomCode, this);
        syncHandler.postDelayed(syncRunnable, SYNC_INTERVAL_MS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        roomManager.removeListener(roomCode, this);
        syncHandler.removeCallbacks(syncRunnable);
        stopLinkTimer();
    }

    @Override
    protected void onDestroy() {
        if (ambientAtmosphere != null) {
            ambientAtmosphere.release();
        }
        musicPlayer.release();
        uiHandler.removeCallbacksAndMessages(null);
        linkHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onRoomUpdated(RoomState roomState) {
        runOnUiThread(() -> {
            participantAdapter.setParticipants(roomState.getParticipants());
            updateRoomNameUi(roomState.getRoomName());
        });
    }

    @Override
    public void onRoomClosed() {
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.room_closed, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> moveTaskToBack(true));
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    private void bindViews() {
        roomCodeText = findViewById(R.id.roomCodeText);
        roomNameText = findViewById(R.id.roomNameText);
        trackTypeText = findViewById(R.id.trackTypeText);
        trackTitleText = findViewById(R.id.trackTitleText);
        trackArtistText = findViewById(R.id.trackArtistText);
        playbackTimeText = findViewById(R.id.playbackTimeText);
        seekBar = findViewById(R.id.seekBar);
        playPauseButton = findViewById(R.id.playPauseButton);

        AmbientGradientView ambientBackground = findViewById(R.id.ambientBackground);
        ambientAtmosphere = new AmbientAtmosphere(this, ambientBackground);
    }

    private void setupParticipantsList() {
        RecyclerView participantsRecyclerView = findViewById(R.id.participantsRecyclerView);
        participantAdapter = new ParticipantAdapter();
        participantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantsRecyclerView.setAdapter(participantAdapter);

        RoomState roomState = roomManager.getRoom(roomCode);
        if (roomState != null) {
            participantAdapter.setParticipants(roomState.getParticipants());
        }
    }

    private void setupPlayerControls() {
        playPauseButton.setOnClickListener(v -> togglePlayback());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || loadedTrack == null) {
                    return;
                }
                if (loadedTrack.isLink()) {
                    linkPositionMs = progress * 1000L;
                    playbackTimeText.setText(TimeFormatter.formatDuration(linkPositionMs) + " / --:--");
                    return;
                }
                long duration = musicPlayer.getDuration();
                if (duration > 0) {
                    long position = (duration * progress) / 100L;
                    playbackTimeText.setText(
                            TimeFormatter.formatDuration(position) + " / "
                                    + TimeFormatter.formatDuration(duration)
                    );
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (loadedTrack == null) {
                    return;
                }
                if (loadedTrack.isLink()) {
                    linkPositionMs = seekBar.getProgress() * 1000L;
                    if (linkPlaying) {
                        linkTickStartRealtime = SystemClock.elapsedRealtime();
                        linkTickStartPosition = linkPositionMs;
                    }
                    roomManager.updatePlayback(roomCode, linkPlaying, linkPositionMs);
                    return;
                }
                long duration = musicPlayer.getDuration();
                if (duration <= 0) {
                    return;
                }
                long position = (duration * seekBar.getProgress()) / 100L;
                musicPlayer.seekTo(position);
                roomManager.updatePlayback(roomCode, musicPlayer.isPlaying(), position);
            }
        });

        musicPlayer.setListener(new MusicPlayer.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (loadedTrack != null && loadedTrack.isLink()) {
                    return;
                }
                updatePlayPauseButton(isPlaying);
                roomManager.updatePlayback(roomCode, isPlaying, musicPlayer.getCurrentPosition());
            }

            @Override
            public void onPositionChanged(long positionMs, long durationMs) {
                updatePlaybackUi(positionMs, durationMs);
            }
        });

        uiHandler.post(positionUpdateRunnable);
    }

    private final Runnable positionUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (loadedTrack != null && !loadedTrack.isLink() && !isUserSeeking) {
                musicPlayer.notifyPositionChanged();
            }
            uiHandler.postDelayed(this, 500L);
        }
    };

    private void setupSourceButtons() {
        MaterialButton localButton = findViewById(R.id.selectLocalButton);
        MaterialButton linkButton = findViewById(R.id.selectLinkButton);
        MaterialButton streamButton = findViewById(R.id.selectStreamButton);

        localButton.setOnClickListener(v ->
                trackSourceLauncher.launch(new Intent(this, TrackPickerActivity.class)));
        linkButton.setOnClickListener(v ->
                trackSourceLauncher.launch(new Intent(this, LinkInputActivity.class)));
        streamButton.setOnClickListener(v ->
                trackSourceLauncher.launch(new Intent(this, StreamPickerActivity.class)));
    }

    private void setupCloseRoomButton() {
        MaterialButton closeRoomButton = findViewById(R.id.closeRoomButton);
        closeRoomButton.setOnClickListener(v -> {
            roomManager.leaveRoom(roomCode, userId);
            finish();
        });
    }

    private void selectTrack(Track track) {
        stopLinkTimer();
        loadedTrack = track;
        linkPositionMs = 0L;
        linkPlaying = false;

        if (track.isLink()) {
            seekBar.setMax(600);
            seekBar.setProgress(0);
            updatePlayPauseButton(false);
        } else {
            seekBar.setMax(100);
            musicPlayer.loadTrack(track.getPlayableUrl());
            musicPlayer.pause();
            updatePlayPauseButton(false);
        }

        roomManager.setCurrentTrack(roomCode, track);
        updateTrackUi(track);
    }

    private void togglePlayback() {
        if (loadedTrack == null) {
            Toast.makeText(this, R.string.select_track_first, Toast.LENGTH_SHORT).show();
            return;
        }

        if (loadedTrack.isLink()) {
            if (linkPlaying) {
                pauseLinkPlayback();
            } else {
                startLinkPlayback();
            }
            return;
        }

        if (musicPlayer.isPlaying()) {
            musicPlayer.pause();
        } else {
            musicPlayer.play();
        }
    }

    private void startLinkPlayback() {
        linkPlaying = true;
        linkTickStartRealtime = SystemClock.elapsedRealtime();
        linkTickStartPosition = linkPositionMs;
        updatePlayPauseButton(true);
        roomManager.updatePlayback(roomCode, true, linkPositionMs);
        linkHandler.post(linkTimerRunnable);
    }

    private void pauseLinkPlayback() {
        linkPlaying = false;
        stopLinkTimer();
        updatePlayPauseButton(false);
        roomManager.updatePlayback(roomCode, false, linkPositionMs);
    }

    private void stopLinkTimer() {
        linkHandler.removeCallbacks(linkTimerRunnable);
    }

    private void updateTrackUi(Track track) {
        if (track == null) {
            trackTypeText.setVisibility(View.GONE);
            trackTitleText.setText(R.string.no_track_selected);
            trackArtistText.setText(R.string.select_track_hint);
            playbackTimeText.setText("0:00 / 0:00");
            seekBar.setProgress(0);
            if (ambientAtmosphere != null) {
                ambientAtmosphere.apply(null);
            }
            return;
        }

        trackTypeText.setVisibility(View.VISIBLE);
        trackTypeText.setText(track.getTypeLabel(this));
        trackTitleText.setText(track.getDisplayTitle(this));
        trackArtistText.setText(track.getDisplayArtist(this));
        if (ambientAtmosphere != null) {
            ambientAtmosphere.apply(track);
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        playPauseButton.setContentDescription(getString(isPlaying ? R.string.pause : R.string.play));
    }

    private void updatePlaybackUi(long positionMs, long durationMs) {
        if (isUserSeeking) {
            return;
        }

        if (loadedTrack != null && loadedTrack.isLink()) {
            seekBar.setProgress((int) (positionMs / 1000L));
            playbackTimeText.setText(TimeFormatter.formatDuration(positionMs) + " / --:--");
            return;
        }

        if (durationMs <= 0) {
            return;
        }
        int progress = (int) ((positionMs * 100L) / durationMs);
        seekBar.setProgress(progress);
        playbackTimeText.setText(
                TimeFormatter.formatDuration(positionMs) + " / "
                        + TimeFormatter.formatDuration(durationMs)
        );
    }
}
