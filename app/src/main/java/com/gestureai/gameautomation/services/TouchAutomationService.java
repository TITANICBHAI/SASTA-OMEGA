package com.gestureai.gameautomation.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import com.gestureai.gameautomation.GameAutomationEngine;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.AdaptiveDecisionMaker;


public class TouchAutomationService extends AccessibilityService {
    private static final String TAG = "TouchAutomationService";
    private static TouchAutomationService instance;
    private GameStrategyAgent gameStrategyAgent;
    private AdaptiveDecisionMaker adaptiveDecisionMaker;
    private boolean learningEnabled = true;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "Touch Automation Service created");
    }
    
    public static TouchAutomationService getInstance() {
        return instance;
    }
    
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        
        try {
            // Initialize AI learning components with proper context validation
            if (this != null && getApplicationContext() != null) {
                gameStrategyAgent = new GameStrategyAgent(getApplicationContext());
                adaptiveDecisionMaker = new AdaptiveDecisionMaker();
                Log.d(TAG, "AI learning components initialized");
                
                // Initialize automation engine with application context
                GameAutomationEngine.initialize(getApplicationContext());
                Log.d(TAG, "Touch Automation Service connected successfully");
            } else {
                Log.e(TAG, "Service context is null, cannot initialize components");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize service components: " + e.getMessage());
        }
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Monitor for game events
        if (event.getPackageName() != null) {
            String packageName = event.getPackageName().toString();
            
            // Detect Subway Surfers
            if (packageName.contains("kiloo.subwaysurf") || packageName.contains("subway")) {
                GameAutomationEngine engine = GameAutomationEngine.getInstance();
                if (engine != null) {
                    engine.setAutomationEnabled(true);
                    Log.d(TAG, "Game detected: " + packageName);
                }
            }
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "Touch Automation Service interrupted");
        cleanup();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
        instance = null;
    }
    
    // Removed duplicate getInstance() method - already declared at line 27
    
    /**
     * Check if service is available and properly initialized
     */
    public static boolean isServiceAvailable() {
        return instance != null;
    }
    
    /**
     * Wait for service to be available with timeout
     */
    public static TouchAutomationService waitForService(int timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (instance == null && (System.currentTimeMillis() - startTime) < timeoutMs) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return instance;
    }
    
    private void cleanup() {
        try {
            if (gameStrategyAgent != null) {
                gameStrategyAgent.cleanup();
            }
            if (adaptiveDecisionMaker != null) {
                adaptiveDecisionMaker.cleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during service cleanup", e);
        }
    }

    public boolean performTap(int x, int y) {
        boolean result = executeGesture(x, y, "TAP");
        if (learningEnabled) {
            learnFromTouchResult("TAP", x, y, result);
        }
        return result;
    }

    public boolean performSwipe(int startX, int startY, int endX, int endY, int duration) {
        boolean result = executeSwipeGesture(startX, startY, endX, endY, duration);
        if (learningEnabled) {
            learnFromTouchResult("SWIPE", startX, startY, result);
        }
        return result;
    }

    private void learnFromTouchResult(String actionType, int x, int y, boolean success) {
        try {
            float reward = success ? 1.0f : -0.5f;
            Log.d(TAG, "Touch learning - Action: " + actionType + ", Success: " + success);
        } catch (Exception e) {
            Log.w(TAG, "Error in touch learning", e);
        }
    }
    
    // Missing methods that are called from TouchExecutionManager
    public boolean executeTap(float x, float y) {
        return performTap((int) x, (int) y);
    }
    
    public boolean executeLongPress(float x, float y, long duration) {
        if (instance == null) {
            Log.e(TAG, "Service not available");
            return false;
        }
        
        Path longPressPath = new Path();
        longPressPath.moveTo(x, y);
        
        GestureDescription.StrokeDescription longPressStroke = 
            new GestureDescription.StrokeDescription(longPressPath, 0, duration);
        
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(longPressStroke);
        
        return dispatchGesture(gestureBuilder.build(), null, null);
    }
    
    public boolean executeSwipe(float startX, float startY, float endX, float endY, long duration) {
        return performSwipe((int) startX, (int) startY, (int) endX, (int) endY, (int) duration);
    }
    
    public boolean executeDoubleTap(float x, float y) {
        // Execute first tap
        boolean firstTap = performTap((int) x, (int) y);
        
        try {
            Thread.sleep(100); // Short delay between taps
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Execute second tap
        boolean secondTap = performTap((int) x, (int) y);
        
        return firstTap && secondTap;
    }
    
    // Core gesture execution methods
    private boolean executeGesture(int x, int y, String gestureType) {
        if (instance == null) {
            Log.e(TAG, "Service not available for " + gestureType);
            return false;
        }
        
        try {
            Path tapPath = new Path();
            tapPath.moveTo(x, y);
            
            long duration = gestureType.equals("TAP") ? 50L : 200L;
            GestureDescription.StrokeDescription tapStroke = 
                new GestureDescription.StrokeDescription(tapPath, 0, duration);
            
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(tapStroke);
            
            return dispatchGesture(gestureBuilder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error executing " + gestureType + " gesture", e);
            return false;
        }
    }
    
    private boolean executeSwipeGesture(int startX, int startY, int endX, int endY, int duration) {
        if (instance == null) {
            Log.e(TAG, "Service not available for swipe");
            return false;
        }
        
        try {
            Path swipePath = new Path();
            swipePath.moveTo(startX, startY);
            swipePath.lineTo(endX, endY);
            
            GestureDescription.StrokeDescription swipeStroke = 
                new GestureDescription.StrokeDescription(swipePath, 0, duration);
            
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(swipeStroke);
            
            return dispatchGesture(gestureBuilder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error executing swipe gesture", e);
            return false;
        }
    }
    
    public boolean executePinch(android.graphics.PointF center, float startDistance, float endDistance, long duration) {
        if (instance == null) {
            Log.e(TAG, "Service not available for pinch");
            return false;
        }
        
        try {
            // First finger path
            Path path1 = new Path();
            float startX1 = center.x - startDistance / 2;
            float endX1 = center.x - endDistance / 2;
            path1.moveTo(startX1, center.y);
            path1.lineTo(endX1, center.y);
            
            // Second finger path  
            Path path2 = new Path();
            float startX2 = center.x + startDistance / 2;
            float endX2 = center.x + endDistance / 2;
            path2.moveTo(startX2, center.y);
            path2.lineTo(endX2, center.y);
            
            GestureDescription.StrokeDescription stroke1 = 
                new GestureDescription.StrokeDescription(path1, 0, duration);
            GestureDescription.StrokeDescription stroke2 = 
                new GestureDescription.StrokeDescription(path2, 0, duration);
            
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(stroke1);
            gestureBuilder.addStroke(stroke2);
            
            return dispatchGesture(gestureBuilder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error executing pinch gesture", e);
            return false;
        }
    }
}