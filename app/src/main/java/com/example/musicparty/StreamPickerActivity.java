package com.example.musicparty;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicparty.model.Track;
import com.example.musicparty.ui.AmbientGradientView;
import com.example.musicparty.util.MusicSites;
import com.example.musicparty.util.StreamUrlHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class StreamPickerActivity extends AppCompatActivity {

    private TextInputEditText streamUrlInput;
    private TextView capturedStreamText;
    private WebView musicWebView;

    private String capturedStreamUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_picker);

        AmbientGradientView ambientBackground = findViewById(R.id.ambientBackground);
        ambientBackground.setIdleAtmosphere();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        streamUrlInput = findViewById(R.id.streamUrlInput);
        capturedStreamText = findViewById(R.id.capturedStreamText);
        musicWebView = findViewById(R.id.musicWebView);
        Spinner siteSpinner = findViewById(R.id.siteSpinner);
        MaterialButton pasteConfirmButton = findViewById(R.id.pasteConfirmButton);
        MaterialButton useCapturedButton = findViewById(R.id.useCapturedButton);

        ArrayAdapter<MusicSites.Site> siteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                MusicSites.SITES
        );
        siteSpinner.setAdapter(siteAdapter);
        siteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                musicWebView.loadUrl(MusicSites.SITES[position].url);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setupWebView();

        pasteConfirmButton.setOnClickListener(v -> confirmPastedUrl());
        useCapturedButton.setOnClickListener(v -> useCapturedStream());

        siteSpinner.setSelection(0);
    }

    private void setupWebView() {
        WebSettings settings = musicWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        musicWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                captureStreamIfNeeded(url);
            }
        });
    }

    private void captureStreamIfNeeded(String url) {
        if (!StreamUrlHelper.isAudioResourceUrl(url)) {
            return;
        }
        capturedStreamUrl = url;
        runOnUiThread(() -> {
            capturedStreamText.setText(getString(R.string.captured_stream, StreamUrlHelper.fileNameFromUrl(url)));
            capturedStreamText.setVisibility(View.VISIBLE);
        });
    }

    private void confirmPastedUrl() {
        String url = streamUrlInput.getText() != null ? streamUrlInput.getText().toString().trim() : "";
        if (!StreamUrlHelper.isValidStreamUrl(url)) {
            Toast.makeText(this, R.string.invalid_stream_url, Toast.LENGTH_SHORT).show();
            return;
        }
        returnStreamTrack(url, StreamUrlHelper.fileNameFromUrl(url));
    }

    private void useCapturedStream() {
        if (TextUtils.isEmpty(capturedStreamUrl)) {
            Toast.makeText(this, R.string.no_stream_captured, Toast.LENGTH_SHORT).show();
            return;
        }
        returnStreamTrack(capturedStreamUrl, StreamUrlHelper.fileNameFromUrl(capturedStreamUrl));
    }

    private void returnStreamTrack(String streamUrl, String title) {
        Track track = Track.stream(title, "Stream", streamUrl, 0L);
        Intent data = new Intent();
        data.putExtra(TrackPickerActivity.EXTRA_TRACK, track);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onDestroy() {
        musicWebView.destroy();
        super.onDestroy();
    }
}
