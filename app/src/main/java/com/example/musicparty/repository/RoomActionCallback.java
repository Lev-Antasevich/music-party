package com.example.musicparty.repository;

public interface RoomActionCallback {
    void onSuccess();

    void onError(String message);
}
