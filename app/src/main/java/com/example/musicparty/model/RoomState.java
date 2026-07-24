package com.example.musicparty.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoomState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String hostId;
    private final String hostName;
    private String roomName;
    private Track currentTrack;
    private boolean isPlaying;
    private long positionMs;
    private long updatedAt;
    private final List<Participant> participants;

    public RoomState(String hostId, String hostName) {
        this.hostId = hostId;
        this.hostName = hostName;
        this.participants = new ArrayList<>();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public String getRoomName() {
        return roomName != null ? roomName : "";
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public void setCurrentTrack(Track currentTrack) {
        this.currentTrack = currentTrack;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public long getPositionMs() {
        return positionMs;
    }

    public void setPositionMs(long positionMs) {
        this.positionMs = positionMs;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Participant> getParticipants() {
        return new ArrayList<>(participants);
    }

    public void addParticipant(Participant participant) {
        for (Participant existing : participants) {
            if (existing.getId().equals(participant.getId())) {
                return;
            }
        }
        participants.add(participant);
    }

    public long getEstimatedPositionMs() {
        if (!isPlaying) {
            return positionMs;
        }
        return positionMs + (System.currentTimeMillis() - updatedAt);
    }
}
