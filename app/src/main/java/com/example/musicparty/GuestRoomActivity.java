package com.example.musicparty;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.musicparty.util.LinkOpener;
import com.example.musicparty.util.MediaStoreHelper;
import com.example.musicparty.util.TimeFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class GuestRoomActivity extends AppCompatActivity implements RoomListener {

    private static final long SYNC_THRESHOLD_MS = 500L;
    private static final long SYNC_CHECK_INTERVAL_MS = 2000L;

    private String roomCode;
    private String userId;
    private RoomManager roomManager;
    private MusicPlayer musicPlayer;
    private ParticipantAdapter participantAdapter;

    private TextView roomCodeText;
    private TextView roomNameText;
    private TextView hostNameText;
    private TextView trackTypeText;
    private TextView trackTitleText;
    private TextView trackArtistText;
    private TextView syncStatusText;
    private TextView playbackTimeText;
    private SeekBar seekBar;
    private MaterialButton openLinkButton;
    private AmbientAtmosphere ambientAtmosphere;

    private Track loadedTrack;
    private long lastLinkPositionMs;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final Runnable syncCheckRunnable = new Runnable() {
        @Override
        public void run() {
            RoomState roomState = roomManager.getRoom(roomCode);
            if (roomState != null) {
                applyRoomState(roomState, false);
            }
            uiHandler.postDelayed(this, SYNC_CHECK_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_room);

        roomCode = getIntent().getStringExtra(MainActivity.EXTRA_ROOM_CODE);
        userId = getIntent().getStringExtra(MainActivity.EXTRA_USER_ID);
        String userName = getIntent().getStringExtra(MainActivity.EXTRA_USER_NAME);

        roomManager = RoomManager.getInstance();
        musicPlayer = new MusicPlayer(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setSubtitle(userName);

        roomCodeText = findViewById(R.id.roomCodeText);
        roomNameText = findViewById(R.id.roomNameText);
        hostNameText = findViewById(R.id.hostNameText);
        trackTypeText = findViewById(R.id.trackTypeText);
        trackTitleText = findViewById(R.id.trackTitleText);
        trackArtistText = findViewById(R.id.trackArtistText);
        syncStatusText = findViewById(R.id.syncStatusText);
        playbackTimeText = findViewById(R.id.playbackTimeText);
        seekBar = findViewById(R.id.seekBar);
        openLinkButton = findViewById(R.id.openLinkButton);

        AmbientGradientView ambientBackground = findViewById(R.id.ambientBackground);
        ambientAtmosphere = new AmbientAtmosphere(this, ambientBackground);

        openLinkButton.setOnClickListener(v -> {
            if (loadedTrack != null && loadedTrack.isLink()) {
                LinkOpener.openTrack(this, loadedTrack, lastLinkPositionMs);
            }
        });

        roomCodeText.setText(roomCode);

        RecyclerView participantsRecyclerView = findViewById(R.id.participantsRecyclerView);
        participantAdapter = new ParticipantAdapter();
        participantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantsRecyclerView.setAdapter(participantAdapter);

        RoomState roomState = roomManager.getRoom(roomCode);
        if (roomState != null) {
            updateRoomHeader(roomState);
            participantAdapter.setParticipants(roomState.getParticipants());
            applyRoomState(roomState, true);
        }

        uiHandler.post(positionUpdateRunnable);
    }

    private final Runnable positionUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (loadedTrack != null && !loadedTrack.isLink()) {
                updatePlaybackUi(musicPlayer.getCurrentPosition(), musicPlayer.getDuration());
            }
            uiHandler.postDelayed(this, 500L);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        roomManager.addListener(roomCode, this);
        uiHandler.postDelayed(syncCheckRunnable, SYNC_CHECK_INTERVAL_MS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        roomManager.removeListener(roomCode, this);
        uiHandler.removeCallbacks(syncCheckRunnable);
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            roomManager.leaveRoom(roomCode, userId);
        }
        if (ambientAtmosphere != null) {
            ambientAtmosphere.release();
        }
        musicPlayer.release();
        uiHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onRoomUpdated(RoomState roomState) {
        runOnUiThread(() -> {
            updateRoomHeader(roomState);
            participantAdapter.setParticipants(roomState.getParticipants());
            applyRoomState(roomState, true);
        });
    }

    private void updateRoomHeader(RoomState roomState) {
        if (!TextUtils.isEmpty(roomState.getRoomName())) {
            roomNameText.setText(roomState.getRoomName());
            roomNameText.setVisibility(View.VISIBLE);
        } else {
            roomNameText.setVisibility(View.GONE);
        }
        hostNameText.setText(getString(R.string.host_label, roomState.getHostName()));
    }

    @Override
    public void onRoomClosed() {
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.room_closed, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void applyRoomState(RoomState roomState, boolean forceTrackReload) {
        Track track = roomState.getCurrentTrack();
        if (track == null) {
            openLinkButton.setVisibility(View.GONE);
            trackTypeText.setVisibility(View.GONE);
            trackTitleText.setText(R.string.waiting_for_track);
            trackArtistText.setText(R.string.host_will_select_track);
            syncStatusText.setText(R.string.sync_waiting);
            if (ambientAtmosphere != null) {
                ambientAtmosphere.apply(null);
            }
            return;
        }

        trackTypeText.setVisibility(View.VISIBLE);
        trackTypeText.setText(track.getTypeLabel(this));
        trackTitleText.setText(track.getDisplayTitle(this));
        trackArtistText.setText(track.getDisplayArtist(this));

        boolean trackChanged = loadedTrack == null || !loadedTrack.matches(track);
        if (trackChanged && ambientAtmosphere != null) {
            ambientAtmosphere.apply(track);
        }

        long targetPosition = roomState.getEstimatedPositionMs();
        lastLinkPositionMs = targetPosition;

        if (track.isLink()) {
            loadedTrack = track;
            openLinkButton.setVisibility(View.VISIBLE);
            seekBar.setEnabled(false);
            seekBar.setProgress((int) (targetPosition / 1000L));
            playbackTimeText.setText(TimeFormatter.formatDuration(targetPosition) + " / --:--");
            syncStatusText.setText(getString(
                    roomState.isPlaying() ? R.string.link_sync_playing : R.string.link_sync_paused,
                    TimeFormatter.formatDuration(targetPosition)
            ));
            return;
        }

        openLinkButton.setVisibility(View.GONE);
        seekBar.setEnabled(false);

        if (forceTrackReload || loadedTrack == null || !loadedTrack.matches(track)) {
            Track playableTrack = track.isLocal()
                    ? MediaStoreHelper.resolveLocalTrack(this, track)
                    : track;
            loadedTrack = playableTrack;
            musicPlayer.loadTrack(playableTrack.getPlayableUrl());
        }

        long currentPosition = musicPlayer.getCurrentPosition();
        if (Math.abs(currentPosition - targetPosition) > SYNC_THRESHOLD_MS) {
            musicPlayer.seekTo(targetPosition);
        }

        if (roomState.isPlaying() && !musicPlayer.isPlaying()) {
            musicPlayer.play();
            syncStatusText.setText(R.string.sync_playing);
        } else if (!roomState.isPlaying() && musicPlayer.isPlaying()) {
            musicPlayer.pause();
            syncStatusText.setText(R.string.sync_paused);
        } else {
            syncStatusText.setText(roomState.isPlaying() ? R.string.sync_playing : R.string.sync_paused);
        }

        updatePlaybackUi(targetPosition, track.getDurationMs());
    }

    private void updatePlaybackUi(long positionMs, long durationMs) {
        if (loadedTrack != null && loadedTrack.isLink()) {
            return;
        }

        if (durationMs <= 0) {
            durationMs = musicPlayer.getDuration();
        }
        if (durationMs <= 0) {
            playbackTimeText.setText("0:00 / 0:00");
            seekBar.setProgress(0);
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
