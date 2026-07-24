package com.example.musicparty.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.musicparty.model.Track;

public final class LinkOpener {

    private LinkOpener() {
    }

    public static void openTrack(Context context, Track track, long positionMs) {
        if (track == null) {
            return;
        }

        String url = track.getLinkUrl();
        if (url == null || url.isEmpty()) {
            url = track.getUri();
        }

        String videoId = LinkParser.extractYouTubeVideoId(url);
        if (!videoId.isEmpty()) {
            url = LinkParser.buildYouTubeWatchUrl(videoId, positionMs);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    }
}
