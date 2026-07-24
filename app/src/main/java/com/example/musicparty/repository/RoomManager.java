package com.example.musicparty.repository;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.musicparty.R;
import com.example.musicparty.model.RoomState;
import com.example.musicparty.model.Track;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RoomManager {

    private static final String TAG = "RoomManager";
    private static final int MAX_CREATE_ATTEMPTS = 8;

    private static RoomManager instance;

    private final DatabaseReference roomsRef;
    private final android.content.Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, RoomState> roomCache = new HashMap<>();
    private final Map<String, List<RoomListener>> listeners = new HashMap<>();
    private final Map<String, ValueEventListener> firebaseListeners = new HashMap<>();

    private RoomManager(DatabaseReference roomsRef, android.content.Context appContext) {
        this.roomsRef = roomsRef;
        this.appContext = appContext.getApplicationContext();
    }

    public static synchronized void init(@NonNull android.content.Context context) {
        if (instance == null) {
            instance = new RoomManager(
                    com.example.musicparty.FirebaseConfig.getDatabase(context).getReference("rooms"),
                    context
            );
        }
    }

    public static synchronized RoomManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RoomManager.init() must be called first");
        }
        return instance;
    }

    public void createRoom(String hostId, String hostName, String roomName, CreateRoomCallback callback) {
        attemptCreateRoom(hostId, hostName, roomName, callback, 0);
    }

    public RoomState getRoom(String roomCode) {
        return roomCache.get(roomCode);
    }

    public void checkRoomExists(String roomCode, RoomExistsCallback callback) {
        roomsRef.child(roomCode).get()
                .addOnSuccessListener(snapshot -> deliverExists(callback, snapshot.exists()))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "checkRoomExists failed", error);
                    deliverExists(callback, false);
                });
    }

    public void joinRoom(String roomCode, String guestId, String guestName, RoomActionCallback callback) {
        roomsRef.child(roomCode).child("participants").child(guestId)
                .setValue(FirebaseRoomMapper.participantData(guestName, false))
                .addOnSuccessListener(unused -> deliverSuccess(callback))
                .addOnFailureListener(error -> deliverError(callback, error.getMessage()));
    }

    public void leaveRoom(String roomCode, String participantId) {
        RoomState roomState = roomCache.get(roomCode);
        if (roomState != null && roomState.getHostId().equals(participantId)) {
            closeRoom(roomCode);
            return;
        }

        roomsRef.child(roomCode).child("participants").child(participantId).removeValue();
    }

    void closeRoom(String roomCode) {
        detachFirebaseListener(roomCode);
        roomCache.remove(roomCode);
        roomsRef.child(roomCode).removeValue();
    }

    public void setCurrentTrack(String roomCode, Track track) {
        roomsRef.child(roomCode).updateChildren(FirebaseRoomMapper.trackUpdate(track));
    }

    public void updatePlayback(String roomCode, boolean isPlaying, long positionMs) {
        roomsRef.child(roomCode).updateChildren(FirebaseRoomMapper.playbackUpdate(isPlaying, positionMs));
    }

    public void addListener(String roomCode, RoomListener listener) {
        List<RoomListener> roomListeners = listeners.computeIfAbsent(roomCode, key -> new ArrayList<>());
        if (!roomListeners.contains(listener)) {
            roomListeners.add(listener);
        }

        if (roomListeners.size() == 1) {
            attachFirebaseListener(roomCode);
        }

        RoomState cachedRoom = roomCache.get(roomCode);
        if (cachedRoom != null) {
            listener.onRoomUpdated(cachedRoom);
        }
    }

    public void removeListener(String roomCode, RoomListener listener) {
        List<RoomListener> roomListeners = listeners.get(roomCode);
        if (roomListeners == null) {
            return;
        }

        roomListeners.remove(listener);
        if (roomListeners.isEmpty()) {
            listeners.remove(roomCode);
            detachFirebaseListener(roomCode);
        }
    }

    private void attemptCreateRoom(
            String hostId,
            String hostName,
            String roomName,
            CreateRoomCallback callback,
            int attempt
    ) {
        if (attempt >= MAX_CREATE_ATTEMPTS) {
            deliverCreateError(callback, appContext.getString(R.string.create_room_failed));
            return;
        }

        String roomCode = generateRoomCode();
        roomsRef.child(roomCode)
                .setValue(FirebaseRoomMapper.createRoomData(hostId, hostName, roomName))
                .addOnSuccessListener(unused -> deliverCreateSuccess(callback, roomCode))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "createRoom failed for code " + roomCode, error);
                    String message = error.getMessage();
                    if (!TextUtils.isEmpty(message) && message.toLowerCase().contains("permission")) {
                        deliverCreateError(callback, message);
                        return;
                    }
                    attemptCreateRoom(hostId, hostName, roomName, callback, attempt + 1);
                });
    }

    private void attachFirebaseListener(String roomCode) {
        if (firebaseListeners.containsKey(roomCode)) {
            return;
        }

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    notifyRoomClosed(roomCode);
                    return;
                }

                RoomState roomState = FirebaseRoomMapper.fromSnapshot(snapshot);
                roomCache.put(roomCode, roomState);
                notifyRoomUpdated(roomCode, roomState);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Room listener cancelled: " + error.getMessage());
                notifyRoomClosed(roomCode);
            }
        };

        firebaseListeners.put(roomCode, valueEventListener);
        roomsRef.child(roomCode).addValueEventListener(valueEventListener);
    }

    private void detachFirebaseListener(String roomCode) {
        ValueEventListener valueEventListener = firebaseListeners.remove(roomCode);
        if (valueEventListener != null) {
            roomsRef.child(roomCode).removeEventListener(valueEventListener);
        }
    }

    private void notifyRoomUpdated(String roomCode, RoomState roomState) {
        List<RoomListener> roomListeners = listeners.get(roomCode);
        if (roomListeners == null) {
            return;
        }

        mainHandler.post(() -> {
            for (RoomListener listener : new ArrayList<>(roomListeners)) {
                listener.onRoomUpdated(roomState);
            }
        });
    }

    private void notifyRoomClosed(String roomCode) {
        detachFirebaseListener(roomCode);
        roomCache.remove(roomCode);

        List<RoomListener> roomListeners = listeners.remove(roomCode);
        if (roomListeners == null) {
            return;
        }

        mainHandler.post(() -> {
            for (RoomListener listener : new ArrayList<>(roomListeners)) {
                listener.onRoomClosed();
            }
        });
    }

    private void deliverCreateSuccess(CreateRoomCallback callback, String roomCode) {
        mainHandler.post(() -> callback.onSuccess(roomCode));
    }

    private void deliverCreateError(CreateRoomCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message != null ? message : "Unknown error"));
    }

    private void deliverSuccess(RoomActionCallback callback) {
        mainHandler.post(callback::onSuccess);
    }

    private void deliverError(RoomActionCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message != null ? message : "Unknown error"));
    }

    private void deliverExists(RoomExistsCallback callback, boolean exists) {
        mainHandler.post(() -> callback.onResult(exists));
    }

    private String generateRoomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
