package com.example.musicparty.repository;

import androidx.annotation.NonNull;

import com.example.musicparty.model.Participant;
import com.example.musicparty.model.RoomState;
import com.example.musicparty.model.Track;
import com.example.musicparty.util.LinkParser;
import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

final class FirebaseRoomMapper {

    private FirebaseRoomMapper() {
    }

    static Map<String, Object> createRoomData(String hostId, String hostName, String roomName) {
        Map<String, Object> hostParticipant = new HashMap<>();
        hostParticipant.put("name", hostName);
        hostParticipant.put("host", true);

        Map<String, Object> participants = new HashMap<>();
        participants.put(hostId, hostParticipant);

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("hostId", hostId);
        roomData.put("hostName", hostName);
        roomData.put("roomName", roomName);
        roomData.put("isPlaying", false);
        roomData.put("positionMs", 0L);
        roomData.put("updatedAt", System.currentTimeMillis());
        roomData.put("participants", participants);
        return roomData;
    }

    static Map<String, Object> participantData(String name, boolean host) {
        Map<String, Object> participant = new HashMap<>();
        participant.put("name", name);
        participant.put("host", host);
        return participant;
    }

    static Map<String, Object> trackData(Track track) {
        Map<String, Object> trackData = new HashMap<>();
        trackData.put("type", track.getType());
        trackData.put("id", track.getId());
        trackData.put("title", track.getTitle());
        trackData.put("artist", track.getArtist());
        trackData.put("uri", track.getUri());
        trackData.put("linkUrl", track.getLinkUrl());
        trackData.put("durationMs", track.getDurationMs());
        return trackData;
    }

    static Map<String, Object> playbackUpdate(boolean isPlaying, long positionMs) {
        Map<String, Object> update = new HashMap<>();
        update.put("isPlaying", isPlaying);
        update.put("positionMs", positionMs);
        update.put("updatedAt", System.currentTimeMillis());
        return update;
    }

    static Map<String, Object> trackUpdate(Track track) {
        Map<String, Object> update = new HashMap<>();
        update.put("currentTrack", trackData(track));
        update.put("isPlaying", false);
        update.put("positionMs", 0L);
        update.put("updatedAt", System.currentTimeMillis());
        return update;
    }

    static RoomState fromSnapshot(@NonNull DataSnapshot snapshot) {
        String hostId = valueAsString(snapshot.child("hostId"));
        String hostName = valueAsString(snapshot.child("hostName"));

        RoomState roomState = new RoomState(hostId, hostName);
        roomState.setRoomName(valueAsString(snapshot.child("roomName")));

        DataSnapshot trackSnapshot = snapshot.child("currentTrack");
        if (trackSnapshot.exists()) {
            roomState.setCurrentTrack(parseTrack(trackSnapshot));
        }

        Boolean isPlaying = snapshot.child("isPlaying").getValue(Boolean.class);
        roomState.setPlaying(isPlaying != null && isPlaying);

        Long positionMs = snapshot.child("positionMs").getValue(Long.class);
        roomState.setPositionMs(positionMs != null ? positionMs : 0L);

        Long updatedAt = snapshot.child("updatedAt").getValue(Long.class);
        roomState.setUpdatedAt(updatedAt != null ? updatedAt : System.currentTimeMillis());

        DataSnapshot participantsSnapshot = snapshot.child("participants");
        for (DataSnapshot participantSnapshot : participantsSnapshot.getChildren()) {
            String participantId = participantSnapshot.getKey();
            if (participantId == null) {
                continue;
            }
            String name = valueAsString(participantSnapshot.child("name"));
            Boolean host = participantSnapshot.child("host").getValue(Boolean.class);
            roomState.addParticipant(new Participant(
                    participantId,
                    name,
                    host != null && host
            ));
        }

        return roomState;
    }

    private static Track parseTrack(DataSnapshot snapshot) {
        String type = valueAsString(snapshot.child("type"));
        if (type.isEmpty()) {
            type = Track.TYPE_LOCAL;
        }

        Long id = snapshot.child("id").getValue(Long.class);
        String title = valueAsString(snapshot.child("title"));
        String artist = snapshot.child("artist").getValue(String.class);
        String uri = valueAsString(snapshot.child("uri"));
        String linkUrl = valueAsString(snapshot.child("linkUrl"));
        Long durationMs = snapshot.child("durationMs").getValue(Long.class);

        if (title.isEmpty()) {
            title = "";
        }

        if (Track.TYPE_LINK.equals(type)) {
            if (linkUrl.isEmpty()) {
                linkUrl = uri;
            }
            String videoId = LinkParser.extractYouTubeVideoId(linkUrl);
            if (!videoId.isEmpty()) {
                return Track.link(title, linkUrl, videoId);
            }
            return Track.linkGeneric(title, linkUrl);
        }

        if (Track.TYPE_STREAM.equals(type)) {
            return Track.stream(
                    title,
                    artist == null ? "" : artist,
                    uri,
                    durationMs != null ? durationMs : 0L
            );
        }

        return Track.local(
                id != null ? id : 0L,
                title,
                artist,
                uri,
                durationMs != null ? durationMs : 0L
        );
    }

    private static String valueAsString(DataSnapshot snapshot) {
        String value = snapshot.getValue(String.class);
        return value != null ? value : "";
    }
}
