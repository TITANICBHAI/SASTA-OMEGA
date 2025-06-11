package com.gestureai.gameautomation.managers;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

/**
 * Accessibility Permission Manager
 * Handles accessibility service permission flow with proper error handling and recovery
 */
public class AccessibilityPermissionManager {
    private static final String TAG = "AccessibilityPermManager";
    private static final String ACCESSIBILITY_SERVICE_ID = "com.gestureai.gameautomation/.services.TouchAutomationService";
    
    private Context context;
    private AccessibilityManager accessibilityManager;
    private PermissionStateListener listener;
    
    public AccessibilityPermissionManager(Context context) {
        this.context = context;
        this.accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    }
    
    /**
     * Check if accessibility service is enabled
     */
    public boolean isAccessibilityServiceEnabled() {
        try {
            if (accessibilityManager == null) {
                return false;
            }
            
            List<AccessibilityServiceInfo> enabledServices = 
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            
            for (AccessibilityServiceInfo service : enabledServices) {
                if (service.getId().equals(ACCESSIBILITY_SERVICE_ID)) {
                    Log.d(TAG, "Accessibility service is enabled");
                    return true;
                }
            }
            
            Log.d(TAG, "Accessibility service is not enabled");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility service status", e);
            return false;
        }
    }
    
    /**
     * Request accessibility permission with proper flow
     */
    public void requestAccessibilityPermission() {
        try {
            if (isAccessibilityServiceEnabled()) {
                if (listener != null) {
                    listener.onPermissionGranted();
                }
                return;
            }
            
            Log.d(TAG, "Requesting accessibility permission");
            
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            // Start monitoring for permission changes
            startPermissionMonitoring();
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting accessibility permission", e);
            if (listener != null) {
                listener.onPermissionError("Failed to open accessibility settings: " + e.getMessage());
            }
        }
    }
    
    /**
     * Monitor accessibility permission changes
     */
    private void startPermissionMonitoring() {
        new Thread(() -> {
            int maxAttempts = 60; // 60 seconds timeout
            int attempts = 0;
            
            while (attempts < maxAttempts && !isAccessibilityServiceEnabled()) {
                try {
                    Thread.sleep(1000);
                    attempts++;
                    
                    if (isAccessibilityServiceEnabled()) {
                        Log.d(TAG, "Accessibility permission granted");
                        if (listener != null) {
                            listener.onPermissionGranted();
                        }
                        return;
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Permission monitoring interrupted");
                    return;
                }
            }
            
            // Timeout reached
            if (!isAccessibilityServiceEnabled()) {
                Log.w(TAG, "Accessibility permission timeout");
                if (listener != null) {
                    listener.onPermissionTimeout();
                }
            }
        }).start();
    }
    
    /**
     * Handle accessibility service binding with recovery
     */
    public boolean bindAccessibilityService() {
        try {
            if (!isAccessibilityServiceEnabled()) {
                Log.w(TAG, "Cannot bind - accessibility service not enabled");
                return false;
            }
            
            // Service binding logic would go here
            // In Android's accessibility framework, binding happens automatically
            // when the service is enabled in settings
            
            Log.d(TAG, "Accessibility service binding initiated");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error binding accessibility service", e);
            return false;
        }
    }
    
    /**
     * Implement service recovery mechanism
     */
    public void recoverAccessibilityService() {
        try {
            Log.d(TAG, "Attempting accessibility service recovery");
            
            if (isAccessibilityServiceEnabled()) {
                // Service is enabled but may be unresponsive
                // Trigger service restart through intent
                Intent restartIntent = new Intent("com.gestureai.gameautomation.RESTART_ACCESSIBILITY");
                context.sendBroadcast(restartIntent);
                
                Log.d(TAG, "Sent accessibility service restart signal");
            } else {
                // Service is disabled, request permission again
                requestAccessibilityPermission();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error recovering accessibility service", e);
        }
    }
    
    /**
     * Get accessibility service status details
     */
    public AccessibilityStatus getAccessibilityStatus() {
        try {
            if (accessibilityManager == null) {
                return new AccessibilityStatus(false, "AccessibilityManager not available");
            }
            
            if (!accessibilityManager.isEnabled()) {
                return new AccessibilityStatus(false, "Accessibility services are disabled system-wide");
            }
            
            if (isAccessibilityServiceEnabled()) {
                return new AccessibilityStatus(true, "TouchAutomationService is enabled and active");
            } else {
                return new AccessibilityStatus(false, "TouchAutomationService is not enabled");
            }
            
        } catch (Exception e) {
            return new AccessibilityStatus(false, "Error checking status: " + e.getMessage());
        }
    }
    
    /**
     * Set permission state listener
     */
    public void setPermissionStateListener(PermissionStateListener listener) {
        this.listener = listener;
    }
    
    /**
     * Permission state listener interface
     */
    public interface PermissionStateListener {
        void onPermissionGranted();
        void onPermissionDenied();
        void onPermissionTimeout();
        void onPermissionError(String error);
    }
    
    /**
     * Accessibility status class
     */
    public static class AccessibilityStatus {
        private boolean enabled;
        private String message;
        
        public AccessibilityStatus(boolean enabled, String message) {
            this.enabled = enabled;
            this.message = message;
        }
        
        public boolean isEnabled() { return enabled; }
        public String getMessage() { return message; }
    }
}