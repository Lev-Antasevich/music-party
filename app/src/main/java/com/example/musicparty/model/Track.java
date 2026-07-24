package com.example.musicparty.model;

import android.content.Context;
import android.text.TextUtils;

import com.example.musicparty.R;
import com.example.musicparty.util.TimeFormatter;

import java.io.Serializable;

public class Track implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_LOCAL = "local";
    public static final String TYPE_LINK = "link";
    public static final String TYPE_STREAM = "stream";

    private final String type;
    private final long id;
    private final String title;
    private final String artist;
    private final String uri;
    private final String linkUrl;
    private final long durationMs;

    private Track(
            String type,
            long id,
            String title,
            String artist,
            String uri,
            String linkUrl,
            long durationMs
    ) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.linkUrl = linkUrl;
        this.durationMs = durationMs;
    }

    public static Track local(long id, String title, String artist, String uri, long durationMs) {
        return new Track(TYPE_LOCAL, id, title, artist, uri, "", durationMs);
    }

    public static Track link(String title, String linkUrl, String videoId) {
        return new Track(TYPE_LINK, videoId.hashCode(), title, "", videoId, linkUrl, 0L);
    }

    public static Track linkGeneric(String title, String linkUrl) {
        return new Track(TYPE_LINK, linkUrl.hashCode(), title, "", linkUrl, linkUrl, 0L);
    }

    public static Track stream(String title, String artist, String streamUrl, long durationMs) {
        return new Track(TYPE_STREAM, streamUrl.hashCode(), title, artist, streamUrl, streamUrl, durationMs);
    }

    public String getType() {
        return type;
    }

    public boolean isLocal() {
        return TYPE_LOCAL.equals(type);
    }

    public boolean isLink() {
        return TYPE_LINK.equals(type);
    }

    public boolean isStream() {
        return TYPE_STREAM.equals(type);
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getUri() {
        return uri;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public String getPlayableUrl() {
        if (isLink()) {
            return linkUrl;
        }
        return uri;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getDisplayTitle(Context context) {
        if (TextUtils.isEmpty(title)) {
            return context.getString(R.string.untitled_track);
        }
        return title;
    }

    public String getDisplayArtist(Context context) {
        if (isLink()) {
            if (!TextUtils.isEmpty(artist)) {
                return artist;
            }
            return context.getString(R.string.track_type_youtube);
        }
        if (TextUtils.isEmpty(artist)) {
            return context.getString(R.string.unknown_artist);
        }
        return artist;
    }

    public String getFormattedDuration() {
        if (durationMs <= 0) {
            return "--:--";
        }
        return TimeFormatter.formatDuration(durationMs);
    }

    public String getTypeLabel(Context context) {
        if (isLink()) {
            return context.getString(R.string.track_type_link);
        }
        if (isStream()) {
            return context.getString(R.string.track_type_stream);
        }
        return context.getString(R.string.track_type_local);
    }

    public boolean matches(Track other) {
        if (other == null || !type.equals(other.type)) {
            return false;
        }
        if (isLink()) {
            return linkUrl.equals(other.linkUrl);
        }
        return uri.equals(other.uri);
    }
}
