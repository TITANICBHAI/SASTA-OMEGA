package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

public class PermissionHelper {
    private static final String TAG = "PermissionHelper";
    private Context context;

    public PermissionHelper(Context context) {
        this.context = context;
    }

    public boolean isAccessibilityServiceEnabled() {
        String settingValue = Settings.Secure.getString(
            context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        
        if (settingValue != null) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                String service = splitter.next();
                if (service.contains("TouchAutomationService") || service.contains("GestureAccessibilityService")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(context);
    }
}