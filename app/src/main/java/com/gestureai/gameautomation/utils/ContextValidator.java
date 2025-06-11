package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.util.Log;

public class ContextValidator {
    private static final String TAG = "ContextValidator";
    
    public static boolean isContextValid(Context context) {
        if (context == null) {
            Log.w(TAG, "Context is null");
            return false;
        }
        
        try {
            // Test if context is still valid by accessing application context
            context.getApplicationContext();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Context is invalid", e);
            return false;
        }
    }
    
    public static Context getValidContext(Context context) {
        if (isContextValid(context)) {
            return context.getApplicationContext();
        }
        return null;
    }
    
    public static boolean canAccessSystemServices(Context context) {
        if (!isContextValid(context)) {
            return false;
        }
        
        try {
            context.getSystemService(Context.ACTIVITY_SERVICE);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Cannot access system services", e);
            return false;
        }
    }
}