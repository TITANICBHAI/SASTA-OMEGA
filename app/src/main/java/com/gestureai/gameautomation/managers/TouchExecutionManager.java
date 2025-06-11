package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.Log;
import com.gestureai.gameautomation.services.TouchAutomationService;

public class TouchExecutionManager {
    private static final String TAG = "TouchExecutionManager";
    private Context context;
    private TouchAutomationService automationService;
    
    public TouchExecutionManager(Context context) {
        this.context = context;
        // Wait for automation service to be available
        initializeAutomationService();
    }
    
    private void initializeAutomationService() {
        new Thread(() -> {
            try {
                // Wait up to 5 seconds for service to become available
                TouchAutomationService service = TouchAutomationService.waitForService(5000);
                if (service != null) {
                    setAutomationService(service);
                    Log.d(TAG, "Automation service connected successfully");
                } else {
                    Log.e(TAG, "Failed to connect to automation service");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing automation service", e);
            }
        }).start();
    }
    
    public void setAutomationService(TouchAutomationService service) {
        this.automationService = service;
    }
    
    public boolean executeTap(float x, float y) {
        if (automationService != null) {
            return automationService.executeTap(x, y);
        }
        Log.w(TAG, "Automation service not available");
        return false;
    }
    
    public boolean executeLongPress(float x, float y, long duration) {
        if (automationService != null) {
            return automationService.executeLongPress(x, y, duration);
        }
        Log.w(TAG, "Automation service not available");
        return false;
    }
    
    public boolean executeSwipe(float startX, float startY, float endX, float endY, long duration) {
        if (automationService != null) {
            return automationService.executeSwipe(startX, startY, endX, endY, duration);
        }
        Log.w(TAG, "Automation service not available");
        return false;
    }
    
    public boolean executeDoubleTap(float x, float y) {
        if (automationService != null) {
            return automationService.executeDoubleTap(x, y);
        }
        Log.w(TAG, "Automation service not available");
        return false;
    }
    
    public boolean executePinch(PointF center, float startDistance, float endDistance, long duration) {
        if (automationService != null) {
            return automationService.executePinch(center, startDistance, endDistance, duration);
        }
        Log.w(TAG, "Automation service not available");
        return false;
    }
    
    public boolean executeCustomPath(Path customPath, long duration) {
        if (automationService != null) {
            return automationService.executeCustomPath(customPath, duration);
        }
        Log.w(TAG, "Automation service not available");
        return false;
    }
}