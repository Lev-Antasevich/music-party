package com.example.musicparty.util;

import android.content.Context;
import android.text.TextUtils;

import com.example.musicparty.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LinkParser {

    private static final Pattern YOUTUBE_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?|shorts)/|.*[?&]v=)|youtu\\.be/)([A-Za-z0-9_-]{11})"
    );

    private LinkParser() {
    }

    public static boolean isYouTubeUrl(String url) {
        return !TextUtils.isEmpty(extractYouTubeVideoId(url));
    }

    public static String extractYouTubeVideoId(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        Matcher matcher = YOUTUBE_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static String buildYouTubeWatchUrl(String videoId, long positionMs) {
        long seconds = Math.max(0L, positionMs / 1000L);
        return "https://www.youtube.com/watch?v=" + videoId + "&t=" + seconds + "s";
    }

    public static String suggestTitle(Context context, String url) {
        if (isYouTubeUrl(url)) {
            return context.getString(R.string.link_youtube_video);
        }
        return context.getString(R.string.link_generic);
    }
}
