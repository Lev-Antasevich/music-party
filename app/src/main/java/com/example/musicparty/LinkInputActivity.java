package com.example.musicparty;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicparty.model.Track;
import com.example.musicparty.ui.AmbientGradientView;
import com.example.musicparty.util.LinkParser;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LinkInputActivity extends AppCompatActivity {

    private TextInputEditText urlInput;
    private TextInputEditText titleInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_input);

        AmbientGradientView ambientBackground = findViewById(R.id.ambientBackground);
        ambientBackground.setIdleAtmosphere();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        urlInput = findViewById(R.id.urlInput);
        titleInput = findViewById(R.id.titleInput);
        MaterialButton confirmButton = findViewById(R.id.confirmLinkButton);
        confirmButton.setOnClickListener(v -> confirmLink());
    }

    private void confirmLink() {
        String url = urlInput.getText() != null ? urlInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, R.string.enter_link, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(title)) {
            title = LinkParser.suggestTitle(this, url);
        }

        Track track;
        String videoId = LinkParser.extractYouTubeVideoId(url);
        if (!videoId.isEmpty()) {
            track = Track.link(title, url, videoId);
        } else {
            track = Track.linkGeneric(title, url);
        }

        Intent data = new Intent();
        data.putExtra(TrackPickerActivity.EXTRA_TRACK, track);
        setResult(RESULT_OK, data);
        finish();
    }
}
