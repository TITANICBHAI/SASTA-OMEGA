package com.gestureai.gameautomation.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

/**
 * Comprehensive permission management for all required app permissions
 * Handles accessibility, overlay, and other critical permissions
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";
    
    // Request codes
    public static final int REQUEST_ACCESSIBILITY_PERMISSION = 1001;
    public static final int REQUEST_OVERLAY_PERMISSION = 1002;
    public static final int REQUEST_WRITE_SETTINGS_PERMISSION = 1003;
    
    /**
     * Check if accessibility service is enabled
     */
    public static boolean isAccessibilityServiceEnabled(Context context) {
        try {
            String serviceName = context.getPackageName() + "/" + 
                "com.gestureai.gameautomation.services.TouchAutomationService";
            
            String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            
            if (enabledServices != null) {
                boolean isEnabled = enabledServices.contains(serviceName);
                Log.d(TAG, "Accessibility service enabled: " + isEnabled);
                return isEnabled;
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility service status", e);
            return false;
        }
    }
    
    /**
     * Request accessibility permission
     */
    public static void requestAccessibilityPermission(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivityForResult(intent, REQUEST_ACCESSIBILITY_PERMISSION);
            Log.d(TAG, "Requested accessibility permission");
        } catch (Exception e) {
            Log.e(TAG, "Error requesting accessibility permission", e);
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean canDraw = Settings.canDrawOverlays(context);
            Log.d(TAG, "Can draw overlays: " + canDraw);
            return canDraw;
        }
        return true; // Permission not required for older versions
    }
    
    /**
     * Request overlay permission
     */
    public static void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                Log.d(TAG, "Requested overlay permission");
            } catch (Exception e) {
                Log.e(TAG, "Error requesting overlay permission", e);
            }
        }
    }
    
    /**
     * Check if write settings permission is granted
     */
    public static boolean canWriteSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean canWrite = Settings.System.canWrite(context);
            Log.d(TAG, "Can write settings: " + canWrite);
            return canWrite;
        }
        return true;
    }
    
    /**
     * Request write settings permission
     */
    public static void requestWriteSettingsPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, REQUEST_WRITE_SETTINGS_PERMISSION);
                Log.d(TAG, "Requested write settings permission");
            } catch (Exception e) {
                Log.e(TAG, "Error requesting write settings permission", e);
            }
        }
    }
    
    /**
     * Check all critical permissions
     */
    public static PermissionStatus checkAllPermissions(Context context) {
        PermissionStatus status = new PermissionStatus();
        
        status.accessibilityEnabled = isAccessibilityServiceEnabled(context);
        status.overlayEnabled = canDrawOverlays(context);
        status.writeSettingsEnabled = canWriteSettings(context);
        
        status.allGranted = status.accessibilityEnabled && status.overlayEnabled && status.writeSettingsEnabled;
        
        Log.d(TAG, "Permission status: " + status.toString());
        return status;
    }
    
    /**
     * Request missing permissions
     */
    public static void requestMissingPermissions(Activity activity) {
        PermissionStatus status = checkAllPermissions(activity);
        
        if (!status.accessibilityEnabled) {
            requestAccessibilityPermission(activity);
            return;
        }
        
        if (!status.overlayEnabled) {
            requestOverlayPermission(activity);
            return;
        }
        
        if (!status.writeSettingsEnabled) {
            requestWriteSettingsPermission(activity);
            return;
        }
        
        Log.d(TAG, "All permissions granted");
    }
    
    /**
     * Show permission explanation dialog
     */
    public static void showPermissionExplanation(Activity activity, String permissionType) {
        // Implementation for showing permission explanation
        Log.d(TAG, "Should show explanation for: " + permissionType);
    }
    
    /**
     * Permission status container
     */
    public static class PermissionStatus {
        public boolean accessibilityEnabled = false;
        public boolean overlayEnabled = false;
        public boolean writeSettingsEnabled = false;
        public boolean allGranted = false;
        
        @Override
        public String toString() {
            return String.format("PermissionStatus{accessibility=%s, overlay=%s, writeSettings=%s, allGranted=%s}",
                    accessibilityEnabled, overlayEnabled, writeSettingsEnabled, allGranted);
        }
    }
}