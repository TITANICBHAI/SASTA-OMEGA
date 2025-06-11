package com.gestureai.gameautomation.utils;

import android.os.SystemClock;
import android.util.Log;

public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    
    private long startTime;
    private long frameCount;
    private long totalProcessingTime;
    private float averageFps;
    private boolean isMonitoring;
    
    public void startMonitoring() {
        startTime = SystemClock.elapsedRealtime();
        frameCount = 0;
        totalProcessingTime = 0;
        isMonitoring = true;
        Log.d(TAG, "Performance monitoring started");
    }
    
    public void stopMonitoring() {
        isMonitoring = false;
        Log.d(TAG, "Performance monitoring stopped");
    }
    
    public void recordFrame() {
        if (!isMonitoring) return;
        
        frameCount++;
        long currentTime = SystemClock.elapsedRealtime();
        long elapsedTime = currentTime - startTime;
        
        if (elapsedTime > 0) {
            averageFps = (frameCount * 1000f) / elapsedTime;
        }
    }
    
    public void recordProcessingTime(long processingTimeMs) {
        if (!isMonitoring) return;
        totalProcessingTime += processingTimeMs;
    }
    
    public PerformanceMetrics getMetrics() {
        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        float averageProcessingTime = frameCount > 0 ? totalProcessingTime / (float) frameCount : 0;
        
        return new PerformanceMetrics(
            averageFps,
            averageProcessingTime,
            frameCount,
            elapsedTime
        );
    }
    
    public static class PerformanceMetrics {
        public final float fps;
        public final float averageProcessingTime;
        public final long totalFrames;
        public final long elapsedTime;
        
        public PerformanceMetrics(float fps, float averageProcessingTime, long totalFrames, long elapsedTime) {
            this.fps = fps;
            this.averageProcessingTime = averageProcessingTime;
            this.totalFrames = totalFrames;
            this.elapsedTime = elapsedTime;
        }
        
        @Override
        public String toString() {
            return String.format("FPS: %.1f, Avg Processing: %.1fms, Frames: %d", 
                               fps, averageProcessingTime, totalFrames);
        }
    }
}