package com.gestureai.gameautomation.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.gestureai.gameautomation.data.GameAction;

import java.util.concurrent.CompletableFuture;

/**
 * Critical accessibility service for game automation
 * Provides system-level touch automation and screen interaction
 */
public class GameAutomationAccessibilityService extends AccessibilityService {
    private static final String TAG = "GameAutomationA11y";
    private static GameAutomationAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Game Automation Accessibility Service connected");
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
        super.onDestroy();
        instance = null;
        Log.d(TAG, "Game Automation Accessibility Service destroyed");
    }
}