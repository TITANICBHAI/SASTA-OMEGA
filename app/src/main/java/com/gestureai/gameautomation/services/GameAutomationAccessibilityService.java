package com.gestureai.gameautomation.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.gestureai.gameautomation.GameAction;

import java.util.concurrent.CompletableFuture;

/**
 * Critical accessibility service for game automation
 * Provides system-level touch automation and screen interaction
 */
public class GameAutomationAccessibilityService extends AccessibilityService {
    private static final String TAG = "GameAutomationA11y";
    private static GameAutomationAccessibilityService instance;
    
    // Permission state monitoring
    private final java.util.concurrent.atomic.AtomicBoolean isPermissionValid = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.ScheduledExecutorService permissionMonitor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    private volatile boolean isServiceActive = false;
    private final java.util.List<PermissionStateListener> permissionListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    
    public interface PermissionStateListener {
        void onPermissionRevoked();
        void onPermissionRestored();
        void onServiceDisabled();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        isServiceActive = true;
        isPermissionValid.set(true);
        
        // Start permission monitoring
        startPermissionMonitoring();
        
        Log.d(TAG, "Game Automation Accessibility Service connected");
        
        // Notify listeners of permission restoration
        for (PermissionStateListener listener : permissionListeners) {
            try {
                listener.onPermissionRestored();
            } catch (Exception e) {
                Log.e(TAG, "Error notifying permission restored", e);
            }
        }
    }
    
    @Override
    public boolean onUnbind(android.content.Intent intent) {
        isServiceActive = false;
        isPermissionValid.set(false);
        
        // Shutdown permission monitoring with proper cleanup
        if (permissionMonitor != null && !permissionMonitor.isShutdown()) {
            try {
                permissionMonitor.shutdown();
                if (!permissionMonitor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    permissionMonitor.shutdownNow();
                }
            } catch (InterruptedException e) {
                permissionMonitor.shutdownNow();
                Thread.currentThread().interrupt();
                Log.w(TAG, "Permission monitor shutdown interrupted");
            }
        }
        
        // Notify listeners of service disable and clear list to prevent memory leaks
        for (PermissionStateListener listener : permissionListeners) {
            try {
                listener.onServiceDisabled();
            } catch (Exception e) {
                Log.e(TAG, "Error notifying service disabled", e);
            }
        }
        permissionListeners.clear();
        
        instance = null;
        Log.d(TAG, "Game Automation Accessibility Service unbound");
        return super.onUnbind(intent);
    }
    
    private void startPermissionMonitoring() {
        permissionMonitor.scheduleWithFixedDelay(() -> {
            try {
                checkPermissionState();
            } catch (Exception e) {
                Log.e(TAG, "Error in permission monitoring", e);
            }
        }, 5, 10, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    private void checkPermissionState() {
        boolean currentPermissionState = isAccessibilityServiceEnabled();
        boolean previousState = isPermissionValid.get();
        
        if (previousState && !currentPermissionState) {
            // Permission was revoked
            isPermissionValid.set(false);
            Log.w(TAG, "Accessibility permission revoked");
            
            for (PermissionStateListener listener : permissionListeners) {
                try {
                    listener.onPermissionRevoked();
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying permission revoked", e);
                }
            }
        } else if (!previousState && currentPermissionState) {
            // Permission was restored
            isPermissionValid.set(true);
            Log.i(TAG, "Accessibility permission restored");
            
            for (PermissionStateListener listener : permissionListeners) {
                try {
                    listener.onPermissionRestored();
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying permission restored", e);
                }
            }
        }
    }
    
    private boolean isAccessibilityServiceEnabled() {
        try {
            android.provider.Settings.Secure.getString(
                getContentResolver(),
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            
            // Check if our service is in the enabled list
            String enabledServices = android.provider.Settings.Secure.getString(
                getContentResolver(),
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            
            if (enabledServices == null) return false;
            
            String serviceName = getPackageName() + "/" + getClass().getName();
            return enabledServices.contains(serviceName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility service state", e);
            return false;
        }
    }
    
    public void addPermissionStateListener(PermissionStateListener listener) {
        if (listener != null && !permissionListeners.contains(listener)) {
            permissionListeners.add(listener);
        }
    }
    
    public void removePermissionStateListener(PermissionStateListener listener) {
        permissionListeners.remove(listener);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Monitor accessibility events for game state changes
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d(TAG, "Window content changed: " + event.getPackageName());
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    public static GameAutomationAccessibilityService getInstance() {
        return instance;
    }

    public static boolean isServiceAvailable() {
        return instance != null;
    }

    /**
     * Execute touch automation action
     */
    public CompletableFuture<Boolean> executeAction(GameAction action) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            Path path = new Path();

            switch (action.getActionType()) {
                case "TAP":
                    path.moveTo(action.getX(), action.getY());
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));
                    break;

                case "DOUBLE_TAP":
                    path.moveTo(action.getX(), action.getY());
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 100, 50));
                    break;

                case "LONG_PRESS":
                    path.moveTo(action.getX(), action.getY());
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 1000));
                    break;

                case "SWIPE_LEFT":
                    path.moveTo(action.getX() + 200, action.getY());
                    path.lineTo(action.getX() - 200, action.getY());
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
                    break;

                case "SWIPE_RIGHT":
                    path.moveTo(action.getX() - 200, action.getY());
                    path.lineTo(action.getX() + 200, action.getY());
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
                    break;

                case "SWIPE_UP":
                    path.moveTo(action.getX(), action.getY() + 200);
                    path.lineTo(action.getX(), action.getY() - 200);
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
                    break;

                case "SWIPE_DOWN":
                    path.moveTo(action.getX(), action.getY() - 200);
                    path.lineTo(action.getX(), action.getY() + 200);
                    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
                    break;

                default:
                    Log.w(TAG, "Unknown action type: " + action.getActionType());
                    future.complete(false);
                    return future;
            }

            GestureDescription gesture = gestureBuilder.build();
            
            boolean dispatched = dispatchGesture(gesture, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    future.complete(true);
                    Log.d(TAG, "Gesture completed: " + action.getActionType());
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    future.complete(false);
                    Log.w(TAG, "Gesture cancelled: " + action.getActionType());
                }
            }, null);

            if (!dispatched) {
                future.complete(false);
                Log.e(TAG, "Failed to dispatch gesture: " + action.getActionType());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error executing action: " + action.getActionType(), e);
            future.complete(false);
        }

        return future;
    }

    /**
     * Find clickable elements on screen
     */
    public AccessibilityNodeInfo findClickableElement(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return null;
        }

        return findNodeByText(rootNode, text);
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;

        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().contains(text)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findNodeByText(child, text);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Click on accessibility node
     */
    public boolean clickNode(AccessibilityNodeInfo node) {
        if (node == null) return false;

        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            // Find clickable parent
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                parent = parent.getParent();
            }
        }

        return false;
    }

    @Override
    public void onDestroy() {
        try {
            // Clear static instance to prevent memory leaks
            synchronized (instanceLock) {
                instance = null;
            }
            
            // Cancel any pending gesture operations
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
                try {
                    if (!executorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        Log.w(TAG, "Executor did not terminate gracefully");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            Log.d(TAG, "Game Automation Accessibility Service destroyed with proper cleanup");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during service cleanup", e);
        } finally {
            super.onDestroy();
        }
    }
    
    @Override
    public void onServiceDisconnected() {
        try {
            // Clear static instance when service is disconnected
            synchronized (instanceLock) {
                instance = null;
            }
            
            Log.d(TAG, "Game Automation Accessibility Service disconnected");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during service disconnection", e);
        } finally {
            super.onServiceDisconnected();
        }
    }
}