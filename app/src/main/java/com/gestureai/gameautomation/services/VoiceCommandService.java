package com.gestureai.gameautomation.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class VoiceCommandService extends Service {
    private static final String TAG = "VoiceCommandService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Voice Command Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Voice Command Service started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Voice Command Service destroyed");
    }
}