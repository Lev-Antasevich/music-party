package com.example.musicparty.repository;

import com.example.musicparty.model.RoomState;

public interface RoomListener {
    void onRoomUpdated(RoomState roomState);

    void onRoomClosed();
}
