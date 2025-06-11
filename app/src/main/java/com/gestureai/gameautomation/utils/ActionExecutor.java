package com.gestureai.gameautomation.utils;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.TouchController;
import com.gestureai.gameautomation.services.TouchAutomationService;

/**
 * Utility class for executing game actions through touch automation
 * This is NOT a service - it's a utility class that coordinates with TouchAutomationService
 */
public class ActionExecutor {
    private static final String TAG = "ActionExecutor";
    
    private Context context;
    private TouchController touchController;
    private TouchAutomationService automationService;
    
    public ActionExecutor(Context context) {
        this.context = context;
        this.touchController = new TouchController(context);
    }
    
    public void setAutomationService(TouchAutomationService service) {
        this.automationService = service;
    }
    
    public boolean executeAction(GameAction action) {
        if (action == null) {
            Log.w(TAG, "Cannot execute null action");
            return false;
        }
        
        try {
            switch (action.getActionType()) {
                case "TOUCH":
                    return executeTouchAction(action);
                case "SWIPE":
                    return executeSwipeAction(action);
                case "HOLD":
                    return executeHoldAction(action);
                case "MULTI_TOUCH":
                    return executeMultiTouchAction(action);
                default:
                    Log.w(TAG, "Unknown action type: " + action.getActionType());
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing action: " + action.getActionType(), e);
            return false;
        }
    }
    
    private boolean executeTouchAction(GameAction action) {
        if (automationService != null) {
            return automationService.performTouch(action.getX(), action.getY());
        } else if (touchController != null) {
            return touchController.performTouch(action.getX(), action.getY());
        }
        return false;
    }
    
    private boolean executeSwipeAction(GameAction action) {
        if (automationService != null) {
            return automationService.performSwipe(
                action.getX(), action.getY(),
                action.getEndX(), action.getEndY(),
                action.getDuration()
            );
        } else if (touchController != null) {
            return touchController.performSwipe(
                action.getX(), action.getY(),
                action.getEndX(), action.getEndY(),
                action.getDuration()
            );
        }
        return false;
    }
    
    private boolean executeHoldAction(GameAction action) {
        if (automationService != null) {
            return automationService.performLongPress(action.getX(), action.getY(), action.getDuration());
        } else if (touchController != null) {
            return touchController.performLongPress(action.getX(), action.getY(), action.getDuration());
        }
        return false;
    }
    
    private boolean executeMultiTouchAction(GameAction action) {
        if (automationService != null) {
            return automationService.performMultiTouch(action.getTouchPoints());
        } else if (touchController != null) {
            return touchController.performMultiTouch(action.getTouchPoints());
        }
        return false;
    }
    
    public boolean isReady() {
        return (automationService != null && automationService.isConnected()) || 
               (touchController != null);
    }
    
    public void cleanup() {
        if (touchController != null) {
            touchController.cleanup();
        }
        automationService = null;
    }
}