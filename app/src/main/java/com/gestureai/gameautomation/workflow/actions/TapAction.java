package com.gestureai.gameautomation.workflow.actions;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.services.TouchAutomationService;
import java.util.Map;

/**
 * Tap Action - Performs touch tap at specified coordinates
 */
public class TapAction implements WorkflowAction {
    private static final String TAG = "TapAction";
    
    private int x;
    private int y;
    
    public TapAction(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public void execute(Context context, Map<String, Object> variables) throws Exception {
        Log.d(TAG, "Executing tap at (" + x + ", " + y + ")");
        
        TouchAutomationService touchService = TouchAutomationService.getInstance();
        if (touchService != null) {
            touchService.performTap(x, y);
        } else {
            throw new Exception("TouchAutomationService not available");
        }
    }
    
    @Override
    public String getDescription() {
        return "Tap at coordinates (" + x + ", " + y + ")";
    }
    
    @Override
    public boolean isValid() {
        return x >= 0 && y >= 0;
    }
    
    // Getters and setters
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
}