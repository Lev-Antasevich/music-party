package com.example.musicparty.repository;

public interface CreateRoomCallback {
    void onSuccess(String roomCode);

    void onError(String message);
}
