package com.example.musicparty.util;

public final class TimeFormatter {

    private TimeFormatter() {
    }

    public static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0, durationMs / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
