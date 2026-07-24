package com.example.musicparty.util;

import android.text.TextUtils;

public final class StreamUrlHelper {

    private StreamUrlHelper() {
    }

    public static boolean isValidStreamUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        return lower.contains(".mp3")
                || lower.contains(".m4a")
                || lower.contains(".aac")
                || lower.contains(".ogg")
                || lower.contains(".wav")
                || lower.contains("audio")
                || lower.contains("stream");
    }

    public static boolean isAudioResourceUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        String lower = url.toLowerCase();
        if (lower.contains(".mp3") || lower.contains(".m4a") || lower.contains(".aac")
                || lower.contains(".ogg") || lower.contains(".wav")) {
            return true;
        }
        return lower.contains("mime=audio") || lower.contains("content-type=audio");
    }

    public static String fileNameFromUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return "Stream track";
        }
        int slashIndex = url.lastIndexOf('/');
        int queryIndex = url.indexOf('?', slashIndex >= 0 ? slashIndex : 0);
        String filePart;
        if (slashIndex >= 0) {
            filePart = queryIndex > slashIndex
                    ? url.substring(slashIndex + 1, queryIndex)
                    : url.substring(slashIndex + 1);
        } else {
            filePart = url;
        }
        if (filePart.isEmpty()) {
            return "Stream track";
        }
        return filePart.replace("%20", " ");
    }
}
