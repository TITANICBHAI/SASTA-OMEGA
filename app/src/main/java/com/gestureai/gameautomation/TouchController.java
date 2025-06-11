package com.gestureai.gameautomation;

import android.util.Log;

public class TouchController {
    private static final String TAG = "TouchController";
    
    public void executeTap(int x, int y) {
        Log.d(TAG, "Executing tap at: " + x + ", " + y);
    }
    
    public void executeSwipe(int startX, int startY, int endX, int endY) {
        Log.d(TAG, "Executing swipe from " + startX + "," + startY + " to " + endX + "," + endY);
    }
    
    public void executeLongPress(int x, int y, long duration) {
        Log.d(TAG, "Executing long press at: " + x + ", " + y + " for " + duration + "ms");
    }
}