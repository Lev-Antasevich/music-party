package com.example.musicparty;

import android.app.Application;

import com.example.musicparty.repository.RoomManager;
import com.google.firebase.FirebaseApp;

public class MusicPartyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        RoomManager.init(this);
    }
}
