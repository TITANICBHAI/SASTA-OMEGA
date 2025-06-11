package com.gestureai.gameautomation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    private static final String TAG = "PermissionManager";
    private final Context context;

    public static final int REQUEST_CODE_PERMISSIONS = 1001;
    public static final int REQUEST_CODE_OVERLAY = 1002;
    public static final int REQUEST_CODE_ACCESSIBILITY = 1003;

    private static final String[] BASE_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE
    };

    private static final String[] MODERN_MEDIA_PERMISSIONS = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
    };

    private static final String[] LEGACY_STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public PermissionManager(Context context) {
        this.context = context;
    }

    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return concat(BASE_PERMISSIONS, MODERN_MEDIA_PERMISSIONS);
        } else {
            return concat(BASE_PERMISSIONS, LEGACY_STORAGE_PERMISSIONS);
        }
    }

    private String getAccessibilityServiceId() {
        return context.getPackageName() + "/com.gestureai.gameautomation.services.TouchAutomationService";
    }

    public boolean isAccessibilityServiceEnabled() {
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(getAccessibilityServiceId());
    }

    public boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public boolean hasAllRuntimePermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllPermissionsGranted() {
        return hasAllRuntimePermissions() && canDrawOverlays() && isAccessibilityServiceEnabled();
    }

    public void requestAllPermissions(Activity activity) {
        Log.d(TAG, "Requesting all required permissions");

        if (!hasAllRuntimePermissions()) {
            requestRuntimePermissions(activity);
        }

        if (!canDrawOverlays()) {
            requestOverlayPermission(activity);
        }

        if (!isAccessibilityServiceEnabled()) {
            requestAccessibilityPermission(activity);
        }
    }

    public void requestRuntimePermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, getRequiredPermissions(), REQUEST_CODE_PERMISSIONS);
    }

    public void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY);
        }
    }

    public void requestAccessibilityPermission(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivityForResult(intent, REQUEST_CODE_ACCESSIBILITY);
    }

    public String getPermissionStatus() {
        return "Permission Status:\n" +
                "- Accessibility Service: " + (isAccessibilityServiceEnabled() ? "✓" : "✗") + "\n" +
                "- Overlay Permission: " + (canDrawOverlays() ? "✓" : "✗") + "\n" +
                "- Runtime Permissions: " + (hasAllRuntimePermissions() ? "✓" : "✗") + "\n" +
                "- All Granted: " + (areAllPermissionsGranted() ? "✓" : "✗");
    }

    public String[] getMissingPermissions() {
        List<String> missing = new ArrayList<>();

        if (!isAccessibilityServiceEnabled()) missing.add("Accessibility Service");
        if (!canDrawOverlays()) missing.add("Overlay Permission");

        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }

        return missing.toArray(new String[0]);
    }

    // Utility method to concatenate arrays
    private static String[] concat(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    // Optional static shortcuts
    public static boolean hasOverlayPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static boolean hasAccessibilityPermission(Context context) {
        String service = context.getPackageName() + "/com.gestureai.gameautomation.services.TouchAutomationService";
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(service);
    }
}
