package com.gestureai.gameautomation;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.Log;
import android.view.ViewConfiguration;
import java.util.List;
import java.util.ArrayList;

public class TouchController {
    private static final String TAG = "TouchController";
    
    private AccessibilityService accessibilityService;
    private Context context;
    private int screenWidth;
    private int screenHeight;
    
    public static class TouchAction {
        public enum Type { TAP, SWIPE, LONG_PRESS, PINCH, MULTI_TOUCH }
        
        public Type type;
        public PointF startPoint;
        public PointF endPoint;
        public long duration;
        public List<PointF> multiTouchPoints;
        
        public TouchAction(Type type, PointF startPoint) {
            this.type = type;
            this.startPoint = startPoint;
            this.duration = 100; // Default 100ms
        }
        
        public TouchAction(Type type, PointF startPoint, PointF endPoint, long duration) {
            this.type = type;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.duration = duration;
        }
    }
    
    public interface TouchCallback {
        void onTouchExecuted(boolean success);
        void onTouchError(String error);
    }
    
    public TouchController(Context context) {
        this.context = context;
        Log.d(TAG, "TouchController initialized");
    }
    
    public void setAccessibilityService(AccessibilityService service) {
        this.accessibilityService = service;
    }
    
    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    public void executeTap(float x, float y, TouchCallback callback) {
        TouchAction action = new TouchAction(TouchAction.Type.TAP, new PointF(x, y));
        executeTouch(action, callback);
    }
    
    public void executeSwipe(float startX, float startY, float endX, float endY, long duration, TouchCallback callback) {
        TouchAction action = new TouchAction(TouchAction.Type.SWIPE, 
            new PointF(startX, startY), new PointF(endX, endY), duration);
        executeTouch(action, callback);
    }
    
    public void executeLongPress(float x, float y, TouchCallback callback) {
        TouchAction action = new TouchAction(TouchAction.Type.LONG_PRESS, new PointF(x, y));
        action.duration = ViewConfiguration.getLongPressTimeout() + 100;
        executeTouch(action, callback);
    }
    
    public void executeTouch(TouchAction action, TouchCallback callback) {
        if (accessibilityService == null) {
            if (callback != null) callback.onTouchError("Accessibility service not available");
            return;
        }
        
        try {
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            Path path = createPathForAction(action);
            
            if (path != null) {
                GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                    path, 0, action.duration);
                gestureBuilder.addStroke(stroke);
                
                boolean success = accessibilityService.dispatchGesture(
                    gestureBuilder.build(),
                    new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            if (callback != null) callback.onTouchExecuted(true);
                            Log.d(TAG, "Gesture completed successfully");
                        }
                        
                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            if (callback != null) callback.onTouchError("Gesture cancelled");
                            Log.w(TAG, "Gesture cancelled");
                        }
                    },
                    null
                );
                
                if (!success && callback != null) {
                    callback.onTouchError("Failed to dispatch gesture");
                }
            } else {
                if (callback != null) callback.onTouchError("Invalid touch path");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing touch", e);
            if (callback != null) callback.onTouchError(e.getMessage());
        }
    }
    
    private Path createPathForAction(TouchAction action) {
        Path path = new Path();
        
        switch (action.type) {
            case TAP:
            case LONG_PRESS:
                path.moveTo(action.startPoint.x, action.startPoint.y);
                path.lineTo(action.startPoint.x, action.startPoint.y);
                break;
                
            case SWIPE:
                if (action.endPoint != null) {
                    path.moveTo(action.startPoint.x, action.startPoint.y);
                    path.lineTo(action.endPoint.x, action.endPoint.y);
                }
                break;
                
            default:
                return null;
        }
        
        return path;
    }
    
    // Game-specific touch patterns
    public void executeJump(TouchCallback callback) {
        // Common jump gesture - tap in lower center of screen
        float jumpX = screenWidth * 0.5f;
        float jumpY = screenHeight * 0.8f;
        executeTap(jumpX, jumpY, callback);
    }
    
    public void executeShoot(TouchCallback callback) {
        // Common shoot gesture - tap in upper right
        float shootX = screenWidth * 0.9f;
        float shootY = screenHeight * 0.2f;
        executeTap(shootX, shootY, callback);
    }
    
    public void executeMove(String direction, TouchCallback callback) {
        float centerX = screenWidth * 0.5f;
        float centerY = screenHeight * 0.5f;
        float distance = Math.min(screenWidth, screenHeight) * 0.3f;
        
        float endX = centerX, endY = centerY;
        
        switch (direction.toUpperCase()) {
            case "UP":
                endY = centerY - distance;
                break;
            case "DOWN":
                endY = centerY + distance;
                break;
            case "LEFT":
                endX = centerX - distance;
                break;
            case "RIGHT":
                endX = centerX + distance;
                break;
        }
        
        executeSwipe(centerX, centerY, endX, endY, 300, callback);
    }
    
    public boolean isReady() {
        return accessibilityService != null && screenWidth > 0 && screenHeight > 0;
    }
}