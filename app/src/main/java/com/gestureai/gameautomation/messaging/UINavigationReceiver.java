package com.gestureai.gameautomation.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gestureai.gameautomation.MainActivity;

/**
 * Broadcast receiver for handling UI navigation commands from fragments
 */
public class UINavigationReceiver extends BroadcastReceiver {
    private static final String TAG = "UINavigationReceiver";
    
    public static final String ACTION_UI_NAVIGATION = "com.gestureai.gameautomation.UI_NAVIGATION";
    public static final String EXTRA_FRAGMENT_POSITION = "fragment_position";
    public static final String EXTRA_COMMAND = "command";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_UI_NAVIGATION.equals(intent.getAction())) {
            String command = intent.getStringExtra(EXTRA_COMMAND);
            String position = intent.getStringExtra(EXTRA_FRAGMENT_POSITION);
            
            Log.d(TAG, "Received UI navigation command: " + command + ", position: " + position);
            
            try {
                if ("navigate".equals(command) && position != null) {
                    // Find the MainActivity instance and navigate
                    MainActivity mainActivity = MainActivity.getCurrentInstance();
                    if (mainActivity != null) {
                        mainActivity.navigateToFragment(position);
                    } else {
                        Log.w(TAG, "MainActivity instance not available for navigation");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling UI navigation command", e);
            }
        }
    }
    
    /**
     * Helper method to send navigation commands
     */
    public static void sendNavigationCommand(Context context, String fragmentPosition) {
        Intent intent = new Intent(ACTION_UI_NAVIGATION);
        intent.putExtra(EXTRA_COMMAND, "navigate");
        intent.putExtra(EXTRA_FRAGMENT_POSITION, fragmentPosition);
        context.sendBroadcast(intent);
    }
}