package com.gestureai.gameautomation.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.gestureai.gameautomation.GameAction;

import java.util.List;
import java.util.ArrayList;

public class AccessibilityManager {
    private static final String TAG = "AccessibilityManager";
    
    private AccessibilityService accessibilityService;
    private boolean isEnabled = false;
    private List<AccessibilityEventListener> listeners;
    
    public interface AccessibilityEventListener {
        void onAccessibilityEvent(AccessibilityEvent event);
        void onGameStateChange(String gameState);
    }
    
    public AccessibilityManager(AccessibilityService service) {
        this.accessibilityService = service;
        this.listeners = new ArrayList<>();
        Log.d(TAG, "AccessibilityManager initialized");
    }
    
    public void enable() {
        if (accessibilityService != null) {
            isEnabled = true;
            Log.d(TAG, "AccessibilityManager enabled");
        } else {
            Log.e(TAG, "Cannot enable - AccessibilityService is null");
        }
    }
    
    public void disable() {
        isEnabled = false;
        Log.d(TAG, "AccessibilityManager disabled");
    }
    
    public boolean isEnabled() {
        return isEnabled && accessibilityService != null;
    }
    
    public void addListener(AccessibilityEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "AccessibilityEventListener added");
        }
    }
    
    public void removeListener(AccessibilityEventListener listener) {
        listeners.remove(listener);
        Log.d(TAG, "AccessibilityEventListener removed");
    }
    
    public boolean performGesture(GameAction action) {
        if (!isEnabled || accessibilityService == null) {
            Log.w(TAG, "Cannot perform gesture - service not enabled");
            return false;
        }
        
        try {
            GestureDescription gesture = createGestureFromAction(action);
            if (gesture != null) {
                return accessibilityService.dispatchGesture(gesture, null, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing gesture", e);
        }
        
        return false;
    }
    
    private GestureDescription createGestureFromAction(GameAction action) {
        try {
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            Path path = new Path();
            
            switch (action.getType()) {
                case "TAP":
                    path.moveTo(action.getX(), action.getY());
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
                    break;
                    
                case "LONG_PRESS":
                    path.moveTo(action.getX(), action.getY());
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 1000));
                    break;
                    
                case "SWIPE_UP":
                    path.moveTo(action.getX(), action.getY());
                    path.lineTo(action.getX(), action.getY() - 200);
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
                    break;
                    
                case "SWIPE_DOWN":
                    path.moveTo(action.getX(), action.getY());
                    path.lineTo(action.getX(), action.getY() + 200);
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
                    break;
                    
                default:
                    Log.w(TAG, "Unknown gesture type: " + action.getType());
                    return null;
            }
            
            return gestureBuilder.build();
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating gesture description", e);
            return null;
        }
    }
    
    public void cleanup() {
        listeners.clear();
        isEnabled = false;
        Log.d(TAG, "AccessibilityManager cleaned up");
    }
}