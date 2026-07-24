package com.example.musicparty.player;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class MusicPlayer {

    public interface Listener {
        void onIsPlayingChanged(boolean isPlaying);

        void onPositionChanged(long positionMs, long durationMs);
    }

    private final ExoPlayer exoPlayer;
    private Listener listener;

    public MusicPlayer(@NonNull Context context) {
        exoPlayer = new ExoPlayer.Builder(context).build();
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (listener != null) {
                    listener.onIsPlayingChanged(isPlaying);
                }
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void loadTrack(@NonNull String uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uri)));
        exoPlayer.prepare();
    }

    public void play() {
        exoPlayer.play();
    }

    public void pause() {
        exoPlayer.pause();
    }

    public void seekTo(long positionMs) {
        exoPlayer.seekTo(positionMs);
    }

    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }

    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }

    public long getDuration() {
        long duration = exoPlayer.getDuration();
        return duration > 0 ? duration : 0;
    }

    public void notifyPositionChanged() {
        if (listener != null) {
            listener.onPositionChanged(getCurrentPosition(), getDuration());
        }
    }

    public void release() {
        exoPlayer.release();
    }
}
