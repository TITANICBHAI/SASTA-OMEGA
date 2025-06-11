package com.gestureai.gameautomation.activities;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.gestureai.gameautomation.messaging.EventBus;

/**
 * Centralized permission status manager with real service checks
 * Fixes the permission flow to use actual service verification
 */
public class PermissionStatusManager {
    
    private static final String TAG = "PermissionStatusManager";
    private Context context;
    private EventBus eventBus;
    
    public PermissionStatusManager(Context context) {
        this.context = context.getApplicationContext();
        this.eventBus = EventBus.getInstance();
    }
    
    /**
     * Check if TouchAutomationService is actually enabled in accessibility settings
     */
    public boolean isTouchAutomationServiceEnabled() {
        String targetService = context.getPackageName() + "/com.gestureai.gameautomation.services.TouchAutomationService";
        String enabledServices = Settings.Secure.getString(
            context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        
        boolean isEnabled = enabledServices != null && enabledServices.contains(targetService);
        
        // Post permission status event
        eventBus.post(new EventBus.PermissionStatusChangedEvent(
            TAG, "TouchAutomationService", isEnabled
        ));
        
        Log.d(TAG, "TouchAutomationService enabled: " + isEnabled);
        return isEnabled;
    }
    
    /**
     * Check if GameAutomationAccessibilityService is enabled
     */
    public boolean isGameAutomationAccessibilityServiceEnabled() {
        String targetService = context.getPackageName() + "/com.gestureai.gameautomation.services.GameAutomationAccessibilityService";
        String enabledServices = Settings.Secure.getString(
            context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        
        boolean isEnabled = enabledServices != null && enabledServices.contains(targetService);
        
        eventBus.post(new EventBus.PermissionStatusChangedEvent(
            TAG, "GameAutomationAccessibilityService", isEnabled
        ));
        
        Log.d(TAG, "GameAutomationAccessibilityService enabled: " + isEnabled);
        return isEnabled;
    }
    
    /**
     * Check overlay permission with proper API level handling
     */
    public boolean hasOverlayPermission() {
        boolean hasPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasPermission = Settings.canDrawOverlays(context);
        } else {
            hasPermission = true; // Not required on older versions
        }
        
        eventBus.post(new EventBus.PermissionStatusChangedEvent(
            TAG, "SYSTEM_ALERT_WINDOW", hasPermission
        ));
        
        Log.d(TAG, "Overlay permission granted: " + hasPermission);
        return hasPermission;
    }
    
    /**
     * Check camera permission
     */
    public boolean hasCameraPermission() {
        boolean hasPermission = ContextCompat.checkSelfPermission(context, 
            android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        
        eventBus.post(new EventBus.PermissionStatusChangedEvent(
            TAG, "CAMERA", hasPermission
        ));
        
        return hasPermission;
    }
    
    /**
     * Check microphone permission
     */
    public boolean hasMicrophonePermission() {
        boolean hasPermission = ContextCompat.checkSelfPermission(context,
            android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        
        eventBus.post(new EventBus.PermissionStatusChangedEvent(
            TAG, "RECORD_AUDIO", hasPermission
        ));
        
        return hasPermission;
    }
    
    /**
     * Check storage permissions (Android version aware)
     */
    public boolean hasStoragePermissions() {
        boolean hasPermission;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            hasPermission = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 transition period
            hasPermission = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 10 and below
            hasPermission = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        
        eventBus.post(new EventBus.PermissionStatusChangedEvent(
            TAG, "STORAGE", hasPermission
        ));
        
        return hasPermission;
    }
    
    /**
     * Check notification permission (Android 13+)
     */
    public boolean hasNotificationPermission() {
        boolean hasPermission;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            hasPermission = true; // Not required on older versions
        }
        
        eventBus.post(new EventBus.PermissionStatusChangedEvent(
            TAG, "POST_NOTIFICATIONS", hasPermission
        ));
        
        return hasPermission;
    }
    
    /**
     * Check if all critical permissions are granted
     */
    public boolean hasAllCriticalPermissions() {
        return hasCameraPermission() && 
               hasMicrophonePermission() && 
               hasStoragePermissions() && 
               hasOverlayPermission() && 
               hasNotificationPermission();
    }
    
    /**
     * Check if all critical services are enabled
     */
    public boolean areAllCriticalServicesEnabled() {
        return isTouchAutomationServiceEnabled() && 
               isGameAutomationAccessibilityServiceEnabled();
    }
    
    /**
     * Check if app is ready for full operation
     */
    public boolean isAppReadyForOperation() {
        boolean permissionsReady = hasAllCriticalPermissions();
        boolean servicesReady = areAllCriticalServicesEnabled();
        
        Log.d(TAG, "App readiness check - Permissions: " + permissionsReady + ", Services: " + servicesReady);
        
        return permissionsReady && servicesReady;
    }
    
    /**
     * Get detailed permission status report
     */
    public PermissionStatusReport getDetailedPermissionStatus() {
        PermissionStatusReport report = new PermissionStatusReport();
        
        report.cameraPermission = hasCameraPermission();
        report.microphonePermission = hasMicrophonePermission();
        report.storagePermissions = hasStoragePermissions();
        report.overlayPermission = hasOverlayPermission();
        report.notificationPermission = hasNotificationPermission();
        report.touchAutomationService = isTouchAutomationServiceEnabled();
        report.gameAutomationService = isGameAutomationAccessibilityServiceEnabled();
        
        report.allPermissionsGranted = hasAllCriticalPermissions();
        report.allServicesEnabled = areAllCriticalServicesEnabled();
        report.readyForOperation = isAppReadyForOperation();
        
        return report;
    }
    
    public static class PermissionStatusReport {
        public boolean cameraPermission;
        public boolean microphonePermission;
        public boolean storagePermissions;
        public boolean overlayPermission;
        public boolean notificationPermission;
        public boolean touchAutomationService;
        public boolean gameAutomationService;
        
        public boolean allPermissionsGranted;
        public boolean allServicesEnabled;
        public boolean readyForOperation;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Permission Status Report:\n");
            sb.append("Camera: ").append(cameraPermission ? "✓" : "✗").append("\n");
            sb.append("Microphone: ").append(microphonePermission ? "✓" : "✗").append("\n");
            sb.append("Storage: ").append(storagePermissions ? "✓" : "✗").append("\n");
            sb.append("Overlay: ").append(overlayPermission ? "✓" : "✗").append("\n");
            sb.append("Notifications: ").append(notificationPermission ? "✓" : "✗").append("\n");
            sb.append("Touch Automation Service: ").append(touchAutomationService ? "✓" : "✗").append("\n");
            sb.append("Game Automation Service: ").append(gameAutomationService ? "✓" : "✗").append("\n");
            sb.append("Ready for Operation: ").append(readyForOperation ? "✓" : "✗").append("\n");
            return sb.toString();
        }
    }
}