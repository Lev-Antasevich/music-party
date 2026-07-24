package com.example.musicparty;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public final class FirebaseConfig {

    private static final String TAG = "FirebaseConfig";

    private FirebaseConfig() {
    }

    public static FirebaseDatabase getDatabase(Context context) {
        String databaseUrl = resolveDatabaseUrl(context);
        if (TextUtils.isEmpty(databaseUrl)) {
            Log.w(TAG, "Database URL is missing. Add firebase_database_url in strings.xml");
            return FirebaseDatabase.getInstance();
        }

        Log.i(TAG, "Using Realtime Database URL: " + databaseUrl);
        return FirebaseDatabase.getInstance(databaseUrl);
    }

    private static String resolveDatabaseUrl(Context context) {
        FirebaseOptions options = FirebaseApp.getInstance().getOptions();
        String optionsUrl = options.getDatabaseUrl();
        if (!TextUtils.isEmpty(optionsUrl)) {
            return optionsUrl;
        }

        String configuredUrl = context.getString(R.string.firebase_database_url).trim();
        if (!TextUtils.isEmpty(configuredUrl) && !configuredUrl.startsWith("REPLACE_")) {
            return configuredUrl;
        }

        return "";
    }
}
