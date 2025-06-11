package com.gestureai.gameautomation.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.os.IBinder;
import android.os.Binder;
import android.content.Intent;
import com.gestureai.gameautomation.GameAutomationEngine;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.AdaptiveDecisionMaker;


public class TouchAutomationService extends AccessibilityService {
    private static final String TAG = "TouchAutomationService";
    private static volatile TouchAutomationService instance;
    private static final Object lock = new Object();
    private volatile GameStrategyAgent gameStrategyAgent;
    private volatile AdaptiveDecisionMaker adaptiveDecisionMaker;
    private volatile boolean learningEnabled = true;
    private volatile boolean isDestroyed = false;
    private final Object serviceLock = new Object();
    
    private volatile boolean isServiceReady = false;
    
    /**
     * Static instance access for accessibility service
     */
    public static TouchAutomationService getInstanceSafe() {
        return instance;
    }
    
    public static boolean isServiceAvailable() {
        return instance != null && !instance.isDestroyed;
    }
    
    public boolean isServiceReady() {
        return isServiceReady && !isDestroyed;
    }
    
    public boolean executeTouchAction(String actionType, float x, float y) {
        if (!isServiceReady || isDestroyed) {
            Log.w(TAG, "Service not ready for touch action: " + actionType);
            return false;
        }
        return performGestureAction(actionType, (int) x, (int) y);
    }
        
        public boolean executeComplexGesture(Path gesturePath, long duration) {
            if (!isServiceReady || isDestroyed) {
                Log.w(TAG, "Service not ready for complex gesture");
                return false;
            }
            return executeCustomPath(gesturePath, duration);
        }
        
        public void setLearningEnabled(boolean enabled) {
            learningEnabled = enabled;
            Log.d(TAG, "Learning mode set to: " + enabled);
        }
        
        public boolean isLearningEnabled() {
            return learningEnabled && !isDestroyed;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service binding requested");
        return binder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service unbinding");
        return super.onUnbind(intent);
    }
    
    // Rate limiting for accessibility events with flood prevention
    private long lastEventTime = 0;
    private static final long EVENT_RATE_LIMIT_MS = 50; // Minimum 50ms between events
    private final java.util.concurrent.atomic.AtomicInteger eventCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.ConcurrentLinkedQueue<AccessibilityEvent> eventQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final int MAX_EVENT_QUEUE_SIZE = 100;
    private volatile boolean eventProcessingActive = true;
    
    // Touch injection coordinate validation with precision
    private volatile int screenWidth = 1080;
    private volatile int screenHeight = 2340;
    private volatile float densityDpi = 420.0f;
    private static final float COORDINATE_PRECISION_THRESHOLD = 0.5f;
    private static final int MIN_TOUCH_AREA = 48; // Minimum 48dp touch target
    private static final int EDGE_MARGIN = 10; // 10px margin from screen edges
    private final java.util.concurrent.atomic.AtomicInteger coordinateValidationFailures = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.ConcurrentHashMap<String, TouchCalibrationData> touchCalibrationMap = new java.util.concurrent.ConcurrentHashMap<>();
    
    private static class TouchCalibrationData {
        final float xOffset;
        final float yOffset;
        final float accuracyScore;
        final long lastUpdated;
        
        TouchCalibrationData(float xOffset, float yOffset, float accuracyScore) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.accuracyScore = accuracyScore;
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    private static class ValidatedCoordinates {
        final int x;
        final int y;
        final float accuracy;
        final boolean isCalibrated;
        
        ValidatedCoordinates(int x, int y, float accuracy, boolean isCalibrated) {
            this.x = x;
            this.y = y;
            this.accuracy = accuracy;
            this.isCalibrated = isCalibrated;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (lock) {
            instance = this;
        }
        Log.d(TAG, "Touch Automation Service created");
    }
    
    public static TouchAutomationService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    Log.w(TAG, "TouchAutomationService not yet initialized");
                    return null;
                }
            }
        }
        return instance;
    }
    
    public static TouchAutomationService getInstanceSafe() {
        if (instance == null) {
            Log.w(TAG, "TouchAutomationService instance not yet created");
            return null;
        }
        return instance;
    }
    
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        
        synchronized (lock) {
            instance = this;
            isServiceConnected = true;
        }
        
        try {
            // Initialize AI learning components with proper context validation
            if (getApplicationContext() != null) {
                gameStrategyAgent = new GameStrategyAgent(getApplicationContext());
                adaptiveDecisionMaker = new AdaptiveDecisionMaker();
                Log.d(TAG, "AI learning components initialized");
                
                // Initialize screen parameters for coordinate validation
                initializeScreenParameters();
                
                // Mark service as ready after successful initialization
                isServiceReady = true;
                
                Log.i(TAG, "TouchAutomationService fully initialized and ready");
                
                // Initialize automation engine with application context
                GameAutomationEngine.getInstance().initialize(getApplicationContext());
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
        if (isDestroyed || !eventProcessingActive) return;
        
        // Event flood prevention with queue management
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEventTime < EVENT_RATE_LIMIT_MS) {
            return; // Skip event to prevent flooding
        }
        lastEventTime = currentTime;
        
        // Check event queue size to prevent memory accumulation
        if (eventQueue.size() >= MAX_EVENT_QUEUE_SIZE) {
            // Remove oldest events to prevent memory explosion
            AccessibilityEvent oldEvent = eventQueue.poll();
            if (oldEvent != null) {
                oldEvent.recycle();
            }
        }
        
        // Increment event counter for monitoring
        eventCount.incrementAndGet();
        
        // Monitor for game events with proper synchronization
        synchronized (serviceLock) {
            if (event != null && event.getPackageName() != null) {
                try {
                    String packageName = event.getPackageName().toString();
                    
                    // Detect supported games with validation
                    if (packageName.contains("kiloo.subwaysurf") || packageName.contains("subway") ||
                        packageName.contains("pubg") || packageName.contains("fortnite") ||
                        packageName.contains("lol") || packageName.contains("dota")) {
                        
                        GameAutomationEngine engine = GameAutomationEngine.getInstance();
                        if (engine != null && !isDestroyed) {
                            engine.setAutomationEnabled(true);
                            Log.d(TAG, "Game detected: " + packageName);
                            
                            // Update AI components with game context
                            updateAIComponentsForGame(packageName);
                        }
                    }
                } catch (SecurityException e) {
                    Log.w(TAG, "Permission denied for accessibility event", e);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing accessibility event", e);
                }
            }
        }
    }
    
    private void updateAIComponentsForGame(String packageName) {
        try {
            if (gameStrategyAgent != null && !isDestroyed) {
                gameStrategyAgent.updateGameContext(packageName);
            }
            if (adaptiveDecisionMaker != null && !isDestroyed) {
                adaptiveDecisionMaker.adaptToGameType(packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating AI components for game", e);
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "Touch Automation Service interrupted");
        cleanup();
    }
    
    @Override
    public void onDestroy() {
        isDestroyed = true;
        
        synchronized (serviceLock) {
            cleanup();
            instance = null;
        }
        
        super.onDestroy();
        Log.d(TAG, "TouchAutomationService destroyed and instance cleared");
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
                Log.w(TAG, "Interrupted while waiting for service");
                return null;
            }
        }
        
        synchronized (lock) {
            TouchAutomationService service = instance;
            if (service != null && !service.isDestroyed) {
                return service;
            } else {
                Log.e(TAG, "Service not available or destroyed after " + timeoutMs + "ms timeout");
                return null;
            }
        }
    }
    
    private synchronized void cleanup() {
        try {
            // Stop event processing first
            eventProcessingActive = false;
            
            // Clear event queue to prevent memory leaks
            if (eventQueue != null) {
                eventQueue.clear();
            }
            
            // Cleanup AI components
            if (gameStrategyAgent != null) {
                gameStrategyAgent.cleanup();
                gameStrategyAgent = null;
            }
            if (adaptiveDecisionMaker != null) {
                adaptiveDecisionMaker.cleanup();
                adaptiveDecisionMaker = null;
            }
            
            // Clear calibration data
            if (touchCalibrationMap != null) {
                touchCalibrationMap.clear();
            }
            
            // Reset counters
            eventCount.set(0);
            coordinateValidationFailures.set(0);
            
            Log.d(TAG, "Service cleanup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during service cleanup", e);
        }
    }

    private void initializeScreenParameters() {
        try {
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
            densityDpi = metrics.densityDpi;
            
            Log.d(TAG, "Screen parameters initialized: " + screenWidth + "x" + screenHeight + 
                      " @ " + densityDpi + " dpi");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing screen parameters", e);
            // Use default values as fallback
        }
    }
    
    private ValidatedCoordinates validateAndCalibrateCoordinates(int x, int y, String actionType) {
        // Basic bounds validation
        boolean wasOutOfBounds = false;
        if (x < EDGE_MARGIN || x > screenWidth - EDGE_MARGIN ||
            y < EDGE_MARGIN || y > screenHeight - EDGE_MARGIN) {
            coordinateValidationFailures.incrementAndGet();
            Log.w(TAG, "Coordinates out of safe bounds: (" + x + "," + y + ")");
            wasOutOfBounds = true;
            
            // Clamp to safe bounds
            x = Math.max(EDGE_MARGIN, Math.min(x, screenWidth - EDGE_MARGIN));
            y = Math.max(EDGE_MARGIN, Math.min(y, screenHeight - EDGE_MARGIN));
        }
        
        // Apply calibration if available
        TouchCalibrationData calibration = touchCalibrationMap.get(actionType);
        boolean isCalibrated = false;
        float accuracy = wasOutOfBounds ? 0.5f : 1.0f;
        
        if (calibration != null && 
            (System.currentTimeMillis() - calibration.lastUpdated) < 300000) { // 5 minutes validity
            
            float calibratedX = x + calibration.xOffset;
            float calibratedY = y + calibration.yOffset;
            
            // Ensure calibrated coordinates are still within bounds
            if (calibratedX >= EDGE_MARGIN && calibratedX <= screenWidth - EDGE_MARGIN &&
                calibratedY >= EDGE_MARGIN && calibratedY <= screenHeight - EDGE_MARGIN) {
                x = Math.round(calibratedX);
                y = Math.round(calibratedY);
                isCalibrated = true;
                accuracy = Math.min(1.0f, accuracy + calibration.accuracyScore);
                
                Log.d(TAG, "Applied calibration for " + actionType + ": offset(" + 
                      calibration.xOffset + "," + calibration.yOffset + ")");
            } else {
                Log.w(TAG, "Calibration would place coordinates out of bounds, skipping");
            }
        }
        
        return new ValidatedCoordinates(x, y, accuracy, isCalibrated);
    } 
                    true,
                    calibration.accuracyScore
                );
            }
        }
        
        // Pixel-perfect alignment for high precision
        float alignedX = alignToPixelGrid(x);
        float alignedY = alignToPixelGrid(y);
        
        return new ValidatedCoordinates(
            Math.round(alignedX), 
            Math.round(alignedY), 
            true,
            0.8f // Default accuracy
        );
    }
    
    private float alignToPixelGrid(float coordinate) {
        // Align to exact pixel boundaries for precision
        return Math.round(coordinate * densityDpi / 160.0f) * 160.0f / densityDpi;
    }
    
    private static class ValidatedCoordinates {
        final int x;
        final int y;
        final boolean isValid;
        final float accuracyScore;
        
        ValidatedCoordinates(int x, int y, boolean isValid, float accuracyScore) {
            this.x = x;
            this.y = y;
            this.isValid = isValid;
            this.accuracyScore = accuracyScore;
        }
    }
    
    private void updateTouchCalibration(String actionType, int intendedX, int intendedY, 
                                       int actualX, int actualY, boolean success) {
        if (success) {
            float xOffset = actualX - intendedX;
            float yOffset = actualY - intendedY;
            float accuracy = 1.0f - (Math.abs(xOffset) + Math.abs(yOffset)) / 100.0f;
            accuracy = Math.max(0.0f, Math.min(1.0f, accuracy));
            
            TouchCalibrationData newCalibration = new TouchCalibrationData(xOffset, yOffset, accuracy);
            touchCalibrationMap.put(actionType, newCalibration);
            
            Log.d(TAG, "Updated calibration for " + actionType + 
                      ": offset(" + xOffset + "," + yOffset + ") accuracy=" + accuracy);
        }
    }

    public boolean performTap(int x, int y) {
        ValidatedCoordinates coords = validateAndCalibrateCoordinates(x, y, "TAP");
        if (!coords.isValid) {
            Log.e(TAG, "Invalid coordinates for tap: (" + x + "," + y + ")");
            return false;
        }
        
        boolean result = executeGesture(coords.x, coords.y, "TAP");
        if (learningEnabled) {
            learnFromTouchResult("TAP", coords.x, coords.y, result);
            updateTouchCalibration("TAP", x, y, coords.x, coords.y, result);
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
    
    private boolean executeSwipeGesture(int startX, int startY, int endX, int endY, int duration) {
        try {
            Path swipePath = new Path();
            swipePath.moveTo(startX, startY);
            swipePath.lineTo(endX, endY);
            
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, duration));
            
            return dispatchGesture(gestureBuilder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute swipe gesture", e);
            return false;
        }
    }
    
    private void learnFromTouchResult(String actionType, int x, int y, boolean success) {
        try {
            if (gameStrategyAgent != null) {
                float reward = success ? 1.0f : -0.5f;
                float[] state = createStateFromTouch(x, y);
                int action = mapActionToIndex(actionType);
                gameStrategyAgent.trainFromCustomData(state, action, reward);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error learning from touch result", e);
        }
    }
    
    private float[] createStateFromTouch(int x, int y) {
        return new float[]{
            x / 1080f, y / 2340f, // Normalized coordinates
            System.currentTimeMillis() / 1000f, // Timestamp
            1.0f, 0.5f, 0.8f, 0.0f, 0.0f, 0.0f, // Padding to match state size
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
        };
    }
    
    private int mapActionToIndex(String actionType) {
        switch (actionType) {
            case "TAP": return 0;
            case "SWIPE": return 1;
            case "LONG_PRESS": return 2;
            case "DOUBLE_TAP": return 3;
            default: return 0;
        }
    }
    
    /**
     * Enhanced gesture execution with proper error handling and retry mechanism
     */
    private boolean executeGesture(int x, int y, String gestureType) {
        if (!isServiceReady || isDestroyed) {
            Log.w(TAG, "Service not ready or destroyed, cannot execute gesture: " + gestureType);
            return false;
        }
        
        // Validate coordinates before execution
        ValidatedCoordinates coords = validateAndCalibrateCoordinates(x, y, gestureType);
        if (coords == null) {
            Log.e(TAG, "Invalid coordinates for gesture: " + gestureType);
            return false;
        }
        
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Path gesturePath = new Path();
                gesturePath.moveTo(coords.x, coords.y);
                
                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                
                int duration = calculateGestureDuration(gestureType);
                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(gesturePath, 0, duration));
                
                // Use callback to track gesture completion
                GestureResultCallback callback = new GestureResultCallback(gestureType, coords.x, coords.y);
                boolean result = dispatchGesture(gestureBuilder.build(), callback, null);
                
                if (result) {
                    Log.d(TAG, "Executed " + gestureType + " at (" + coords.x + "," + coords.y + ") - Attempt: " + attempt);
                    
                    // Learn from successful execution
                    if (learningEnabled) {
                        learnFromTouchResult(gestureType, coords.x, coords.y, true);
                    }
                    
                    return true;
                } else if (attempt < maxRetries) {
                    Log.w(TAG, "Gesture execution failed, retrying... Attempt: " + (attempt + 1));
                    Thread.sleep(100); // Brief delay before retry
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Exception during gesture execution attempt " + attempt + ": " + gestureType, e);
                if (attempt == maxRetries) {
                    // Learn from failed execution
                    if (learningEnabled) {
                        learnFromTouchResult(gestureType, coords.x, coords.y, false);
                    }
                    return false;
                }
                try {
                    Thread.sleep(150); // Longer delay after exception
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        
        Log.e(TAG, "All attempts failed for gesture: " + gestureType);
        return false;
    }
    
    private int calculateGestureDuration(String gestureType) {
        switch (gestureType) {
            case "LONG_PRESS":
                return 1000;
            case "TAP":
            case "CLICK":
                return 50;
            case "DOUBLE_TAP":
                return 75;
            default:
                return 100;
        }
    }
    
    /**
     * Callback to track gesture execution results
     */
    private class GestureResultCallback extends GestureDescription.GestureResultCallback {
        private final String gestureType;
        private final int x, y;
        
        GestureResultCallback(String gestureType, int x, int y) {
            this.gestureType = gestureType;
            this.x = x;
            this.y = y;
        }
        
        @Override
        public void onCompleted(GestureDescription gestureDescription) {
            Log.d(TAG, "Gesture completed successfully: " + gestureType + " at (" + x + "," + y + ")");
        }
        
        @Override
        public void onCancelled(GestureDescription gestureDescription) {
            Log.w(TAG, "Gesture cancelled: " + gestureType + " at (" + x + "," + y + ")");
            if (learningEnabled) {
                learnFromTouchResult(gestureType, x, y, false);
            }
        }
    }
    
    // Removed duplicate method - keeping the implementation from earlier in the file
    
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
    
    public boolean executePinch(android.graphics.PointF center, float startDistance, float endDistance, long duration) {
        if (instance == null) {
            Log.e(TAG, "Service not available for pinch");
            return false;
        }
        
        try {
            // Calculate pinch gesture coordinates
            float startRadius = startDistance / 2;
            float endRadius = endDistance / 2;
            
            // Create two-finger pinch paths
            Path path1 = new Path();
            Path path2 = new Path();
            
            // First finger path (left side)
            path1.moveTo(center.x - startRadius, center.y);
            path1.lineTo(center.x - endRadius, center.y);
            
            // Second finger path (right side)
            path2.moveTo(center.x + startRadius, center.y);
            path2.lineTo(center.x + endRadius, center.y);
            
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
    
    public boolean executeCustomPath(android.graphics.Path customPath, long duration) {
        if (instance == null) {
            Log.e(TAG, "Service not available for custom path");
            return false;
        }
        
        try {
            GestureDescription.StrokeDescription customStroke = 
                new GestureDescription.StrokeDescription(customPath, 0, duration);
            
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(customStroke);
            
            return dispatchGesture(gestureBuilder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error executing custom path gesture", e);
            return false;
        }
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
    
    // CRITICAL: Missing AccessibilityService required methods
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        
        try {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            
            // Auto-detect game launch
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (packageName.contains("subway") || packageName.contains("game")) {
                    Log.d(TAG, "Game detected: " + packageName);
                    if (gameStrategyAgent != null) {
                        gameStrategyAgent.onGameDetected(packageName);
                    }
                }
            }
            
            // Process window content changes for real-time analysis
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                if (learningEnabled && gameStrategyAgent != null) {
                    gameStrategyAgent.processScreenUpdate();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing accessibility event", e);
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "TouchAutomationService interrupted");
        cleanup();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
        instance = null;
        Log.d(TAG, "TouchAutomationService destroyed");
    }
    
    /**
     * Execute game action - called by ServiceCommunicationProtocol
     */
    public boolean executeGameAction(com.gestureai.gameautomation.GameAction action) {
        try {
            if (action == null) {
                Log.w(TAG, "Cannot execute null action");
                return false;
            }
            
            // Convert GameAction to accessibility action
            return performGestureAction(action.getActionType(), action.getX(), action.getY());
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute game action", e);
            return false;
        }
    }
    
    /**
     * Perform gesture action using accessibility service
     */
    private boolean performGestureAction(String actionType, int x, int y) {
        try {
            android.graphics.Path gesturePath = new android.graphics.Path();
            
            switch (actionType) {
                case "TAP":
                case "CLICK":
                    gesturePath.moveTo(x, y);
                    break;
                    
                case "SWIPE_UP":
                    gesturePath.moveTo(x, y);
                    gesturePath.lineTo(x, y - 200);
                    break;
                    
                case "SWIPE_DOWN":
                    gesturePath.moveTo(x, y);
                    gesturePath.lineTo(x, y + 200);
                    break;
                    
                case "SWIPE_LEFT":
                    gesturePath.moveTo(x, y);
                    gesturePath.lineTo(x - 200, y);
                    break;
                    
                case "SWIPE_RIGHT":
                    gesturePath.moveTo(x, y);
                    gesturePath.lineTo(x + 200, y);
                    break;
                    
                default:
                    gesturePath.moveTo(x, y);
                    break;
            }
            
            android.accessibilityservice.GestureDescription.StrokeDescription stroke = 
                new android.accessibilityservice.GestureDescription.StrokeDescription(gesturePath, 0, 100);
            
            android.accessibilityservice.GestureDescription.Builder gestureBuilder = 
                new android.accessibilityservice.GestureDescription.Builder();
            gestureBuilder.addStroke(stroke);
            
            return dispatchGesture(gestureBuilder.build(), null, null);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform gesture action: " + actionType, e);
            return false;
        }
    }
}