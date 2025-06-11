package com.gestureai.gameautomation.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gestureai.gameautomation.services.VoiceCommandService;

/**
 * Broadcast receiver for handling voice commands from fragments
 */
public class VoiceCommandReceiver extends BroadcastReceiver {
    private static final String TAG = "VoiceCommandReceiver";
    
    public static final String ACTION_VOICE_COMMAND = "com.gestureai.gameautomation.VOICE_COMMAND";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_CONFIDENCE = "confidence";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_VOICE_COMMAND.equals(intent.getAction())) {
            String command = intent.getStringExtra(EXTRA_COMMAND);
            float confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0.0f);
            
            Log.d(TAG, "Received voice command: " + command + ", confidence: " + confidence);
            
            try {
                processVoiceCommand(context, command, confidence);
            } catch (Exception e) {
                Log.e(TAG, "Error processing voice command", e);
            }
        }
    }
    
    private void processVoiceCommand(Context context, String command, float confidence) {
        if (command == null || confidence < 0.5f) {
            Log.w(TAG, "Voice command ignored - low confidence or null command");
            return;
        }
        
        // Process navigation commands
        if (command.toLowerCase().contains("dashboard")) {
            UINavigationReceiver.sendNavigationCommand(context, "0");
        } else if (command.toLowerCase().contains("autoplay") || command.toLowerCase().contains("auto play")) {
            UINavigationReceiver.sendNavigationCommand(context, "1");
        } else if (command.toLowerCase().contains("ai") || command.toLowerCase().contains("artificial intelligence")) {
            UINavigationReceiver.sendNavigationCommand(context, "2");
        } else if (command.toLowerCase().contains("tools")) {
            UINavigationReceiver.sendNavigationCommand(context, "3");
        } else if (command.toLowerCase().contains("more") || command.toLowerCase().contains("settings")) {
            UINavigationReceiver.sendNavigationCommand(context, "4");
        } else {
            Log.d(TAG, "Unrecognized voice command: " + command);
        }
    }
    
    /**
     * Helper method to send voice commands
     */
    public static void sendVoiceCommand(Context context, String command, float confidence) {
        Intent intent = new Intent(ACTION_VOICE_COMMAND);
        intent.putExtra(EXTRA_COMMAND, command);
        intent.putExtra(EXTRA_CONFIDENCE, confidence);
        context.sendBroadcast(intent);
    }
}