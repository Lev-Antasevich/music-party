package com.example.musicparty.util;

public final class MusicSites {

    public static final Site[] SITES = {
            new Site("Muzofond", "https://muzofond.fm/"),
            new Site("DriveMusic", "https://ru.drivemusic.me/"),
            new Site("MP3Party", "https://mp3party.net/"),
            new Site("Pesni.me", "https://music.pesni.me/")
    };

    private MusicSites() {
    }

    public static final class Site {
        public final String name;
        public final String url;

        public Site(String name, String url) {
            this.name = name;
            this.url = url;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
