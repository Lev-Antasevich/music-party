package com.example.musicparty;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicparty.adapter.TrackAdapter;
import com.example.musicparty.model.Track;
import com.example.musicparty.ui.AmbientGradientView;
import com.example.musicparty.util.MediaStoreHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class TrackPickerActivity extends AppCompatActivity {

    public static final String EXTRA_TRACK = "extra_track";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_picker);

        AmbientGradientView ambientBackground = findViewById(R.id.ambientBackground);
        ambientBackground.setIdleAtmosphere();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView tracksRecyclerView = findViewById(R.id.tracksRecyclerView);
        TextView emptyStateText = findViewById(R.id.emptyStateText);

        TrackAdapter trackAdapter = new TrackAdapter();
        tracksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tracksRecyclerView.setAdapter(trackAdapter);

        List<Track> tracks = MediaStoreHelper.loadAudioTracks(this);
        trackAdapter.setTracks(tracks);
        trackAdapter.setOnTrackClickListener(this::returnTrack);

        if (tracks.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            tracksRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            tracksRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void returnTrack(Track track) {
        Intent data = new Intent();
        data.putExtra(EXTRA_TRACK, track);
        setResult(RESULT_OK, data);
        finish();
    }
}
