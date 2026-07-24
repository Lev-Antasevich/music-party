package com.example.musicparty.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.musicparty.R;
import com.example.musicparty.model.Track;

import java.util.ArrayList;
import java.util.List;

public final class MediaStoreHelper {

    private MediaStoreHelper() {
    }

    public static List<Track> loadAudioTracks(Context context) {
        List<Track> tracks = new ArrayList<>();

        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                selection,
                null,
                sortOrder
        )) {
            if (cursor == null) {
                return tracks;
            }

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                long duration = cursor.getLong(durationColumn);
                Uri contentUri = ContentUris.withAppendedId(collection, id);

                tracks.add(Track.local(
                        id,
                        title == null || title.isEmpty()
                                ? context.getString(R.string.untitled_track)
                                : title,
                        artist,
                        contentUri.toString(),
                        duration
                ));
            }
        }

        return tracks;
    }

    public static Track resolveLocalTrack(android.content.Context context, Track remoteTrack) {
        if (remoteTrack == null) {
            return null;
        }

        for (Track localTrack : loadAudioTracks(context)) {
            if (localTrack.getTitle().equalsIgnoreCase(remoteTrack.getTitle())
                    && artistMatches(localTrack.getArtist(), remoteTrack.getArtist())) {
                return localTrack;
            }
        }

        return remoteTrack;
    }

    private static boolean artistMatches(String left, String right) {
        if (left == null) {
            left = "";
        }
        if (right == null) {
            right = "";
        }
        return left.equalsIgnoreCase(right);
    }
}
