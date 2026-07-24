package com.example.musicparty.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import com.example.musicparty.R;
import com.example.musicparty.model.Track;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Drives {@link AmbientGradientView} colors from local track artwork when available,
 * or from a stable hash of the track metadata.
 */
public final class AmbientAtmosphere {

    private final Context appContext;
    private final AmbientGradientView ambientView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int applyGeneration;

    public AmbientAtmosphere(Context context, AmbientGradientView ambientView) {
        this.appContext = context.getApplicationContext();
        this.ambientView = ambientView;
        ambientView.setIdleAtmosphere();
    }

    public void apply(@Nullable Track track) {
        final int generation = ++applyGeneration;

        if (track == null) {
            ambientView.setIdleAtmosphere();
            return;
        }

        executor.execute(() -> {
            Bitmap artwork = loadArtwork(track);
            if (generation != applyGeneration) {
                if (artwork != null) {
                    artwork.recycle();
                }
                return;
            }

            final int[] colors;
            if (artwork != null) {
                try {
                    Palette palette = Palette.from(artwork).clearFilters().generate();
                    colors = colorsFromPalette(palette, track);
                } finally {
                    artwork.recycle();
                }
            } else {
                colors = colorsFromTrack(track);
            }

            mainHandler.post(() -> {
                if (generation != applyGeneration) {
                    return;
                }
                ambientView.setAtmosphereColors(colors[0], colors[1], colors[2], true);
            });
        });
    }

    public void release() {
        applyGeneration++;
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }

    @Nullable
    private Bitmap loadArtwork(Track track) {
        if (!track.isLocal()) {
            return null;
        }
        String uriString = track.getUri();
        if (uriString == null || uriString.isEmpty()) {
            return null;
        }
        try {
            return appContext.getContentResolver().loadThumbnail(
                    Uri.parse(uriString),
                    new Size(320, 320),
                    null
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    @ColorInt
    private int[] colorsFromPalette(Palette palette, Track track) {
        int fallbackTop = ContextCompat.getColor(appContext, R.color.glow_pink);
        int fallbackBottom = ContextCompat.getColor(appContext, R.color.glow_blue);
        int fallbackSide = ContextCompat.getColor(appContext, R.color.glow_orange);

        int vibrant = palette.getVibrantColor(0);
        int darkVibrant = palette.getDarkVibrantColor(0);
        int muted = palette.getMutedColor(0);
        int lightVibrant = palette.getLightVibrantColor(0);
        int dominant = palette.getDominantColor(0);

        int top = firstNonZero(vibrant, lightVibrant, dominant, fallbackTop);
        int bottom = firstNonZero(darkVibrant, muted, dominant, fallbackBottom);
        int side = firstNonZero(muted, lightVibrant, vibrant, fallbackSide);

        if (top == bottom) {
            int[] hashed = colorsFromTrack(track);
            bottom = hashed[1];
            side = hashed[2];
        }

        return new int[]{boostSaturation(top), boostSaturation(bottom), boostSaturation(side)};
    }

    @ColorInt
    private int[] colorsFromTrack(Track track) {
        String seed = track.getTitle() + "|" + track.getArtist() + "|" + track.getType();
        float hue = Math.abs(seed.hashCode() % 360);
        if (hue < 40) {
            hue = 18f + (hue % 24);
        } else if (hue < 120) {
            hue = 320f + (hue % 30);
        } else if (hue < 220) {
            hue = 210f + (hue % 25);
        } else {
            hue = 12f + (hue % 28);
        }

        int top = Color.HSVToColor(new float[]{hue, 0.72f, 0.95f});
        int bottom = Color.HSVToColor(new float[]{(hue + 48f) % 360f, 0.65f, 0.85f});
        int side = Color.HSVToColor(new float[]{(hue + 210f) % 360f, 0.55f, 0.9f});
        return new int[]{top, bottom, side};
    }

    @ColorInt
    private static int firstNonZero(@ColorInt int... colors) {
        for (int color : colors) {
            if (color != 0) {
                return color;
            }
        }
        return Color.GRAY;
    }

    @ColorInt
    private static int boostSaturation(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(1f, Math.max(0.45f, hsv[1] * 1.15f));
        hsv[2] = Math.min(1f, Math.max(0.55f, hsv[2]));
        return Color.HSVToColor(hsv);
    }
}
