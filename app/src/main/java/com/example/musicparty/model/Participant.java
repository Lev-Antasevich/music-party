package com.example.musicparty.model;

import java.io.Serializable;

public class Participant implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final boolean host;

    public Participant(String id, String name, boolean host) {
        this.id = id;
        this.name = name;
        this.host = host;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isHost() {
        return host;
    }
}
