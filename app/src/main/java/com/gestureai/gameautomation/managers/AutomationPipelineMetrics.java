package com.gestureai.gameautomation.managers;

import android.util.Log;
import com.gestureai.gameautomation.GameAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Real-time metrics collection for automation pipeline performance
 */
public class AutomationPipelineMetrics {
    private static final String TAG = "PipelineMetrics";
    
    // Performance counters
    private final AtomicInteger totalActions = new AtomicInteger(0);
    private final AtomicInteger successfulActions = new AtomicInteger(0);
    private final AtomicInteger failedActions = new AtomicInteger(0);
    
    // Timing metrics
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicLong minLatency = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatency = new AtomicLong(0);
    
    // Action type tracking
    private final Map<String, ActionTypeMetrics> actionTypeMetrics = new ConcurrentHashMap<>();
    
    // Recent performance history (last 100 actions)
    private final List<ActionPerformanceRecord> recentActions = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_RECENT_ACTIONS = 100;
    
    // System performance tracking
    private final AtomicLong frameProcessingTime = new AtomicLong(0);
    private final AtomicLong aiInferenceTime = new AtomicLong(0);
    private final AtomicLong actionExecutionTime = new AtomicLong(0);
    
    // Session metrics
    private long sessionStartTime = System.currentTimeMillis();
    private final AtomicInteger framesProcessed = new AtomicInteger(0);
    
    public void recordActionExecution(GameAction action, boolean success, long executionTime, long totalLatency) {
        // Update counters
        totalActions.incrementAndGet();
        if (success) {
            successfulActions.incrementAndGet();
        } else {
            failedActions.incrementAndGet();
        }
        
        // Update timing
        this.totalLatency.addAndGet(totalLatency);
        updateLatencyBounds(totalLatency);
        
        // Update action type metrics
        String actionType = action.getActionType();
        actionTypeMetrics.computeIfAbsent(actionType, k -> new ActionTypeMetrics()).recordAction(success, executionTime);
        
        // Add to recent actions history
        ActionPerformanceRecord record = new ActionPerformanceRecord(
            System.currentTimeMillis(), actionType, success, executionTime, totalLatency);
        
        synchronized (recentActions) {
            recentActions.add(record);
            if (recentActions.size() > MAX_RECENT_ACTIONS) {
                recentActions.remove(0);
            }
        }
        
        Log.d(TAG, String.format("Action recorded: %s, Success: %b, Time: %dms, Total Latency: %dms", 
              actionType, success, executionTime, totalLatency));
    }
    
    public void recordActionFailure(GameAction action, String errorMessage) {
        failedActions.incrementAndGet();
        totalActions.incrementAndGet();
        
        String actionType = action.getActionType();
        actionTypeMetrics.computeIfAbsent(actionType, k -> new ActionTypeMetrics()).recordFailure(errorMessage);
        
        Log.w(TAG, String.format("Action failed: %s, Error: %s", actionType, errorMessage));
    }
    
    public void recordFrameProcessing(long processingTime) {
        frameProcessingTime.set(processingTime);
        framesProcessed.incrementAndGet();
    }
    
    public void recordAIInference(long inferenceTime) {
        aiInferenceTime.set(inferenceTime);
    }
    
    public void recordActionExecution(long executionTime) {
        actionExecutionTime.set(executionTime);
    }
    
    private void updateLatencyBounds(long latency) {
        // Update min latency
        long currentMin = minLatency.get();
        while (latency < currentMin && !minLatency.compareAndSet(currentMin, latency)) {
            currentMin = minLatency.get();
        }
        
        // Update max latency
        long currentMax = maxLatency.get();
        while (latency > currentMax && !maxLatency.compareAndSet(currentMax, latency)) {
            currentMax = maxLatency.get();
        }
    }
    
    // Getters for metrics
    public int getTotalActions() { return totalActions.get(); }
    public int getSuccessfulActions() { return successfulActions.get(); }
    public int getFailedActions() { return failedActions.get(); }
    
    public double getSuccessRate() {
        int total = totalActions.get();
        return total > 0 ? (double) successfulActions.get() / total * 100.0 : 0.0;
    }
    
    public long getAverageLatency() {
        int total = totalActions.get();
        return total > 0 ? totalLatency.get() / total : 0;
    }
    
    public long getMinLatency() { 
        long min = minLatency.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    
    public long getMaxLatency() { return maxLatency.get(); }
    
    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    public double getActionsPerSecond() {
        long duration = getSessionDuration();
        return duration > 0 ? (double) totalActions.get() / (duration / 1000.0) : 0.0;
    }
    
    public double getFramesPerSecond() {
        long duration = getSessionDuration();
        return duration > 0 ? (double) framesProcessed.get() / (duration / 1000.0) : 0.0;
    }
    
    public long getCurrentFrameProcessingTime() { return frameProcessingTime.get(); }
    public long getCurrentAIInferenceTime() { return aiInferenceTime.get(); }
    public long getCurrentActionExecutionTime() { return actionExecutionTime.get(); }
    
    public Map<String, ActionTypeMetrics> getActionTypeMetrics() {
        return new ConcurrentHashMap<>(actionTypeMetrics);
    }
    
    public List<ActionPerformanceRecord> getRecentActions() {
        synchronized (recentActions) {
            return new ArrayList<>(recentActions);
        }
    }
    
    public void reset() {
        totalActions.set(0);
        successfulActions.set(0);
        failedActions.set(0);
        totalLatency.set(0);
        minLatency.set(Long.MAX_VALUE);
        maxLatency.set(0);
        frameProcessingTime.set(0);
        aiInferenceTime.set(0);
        actionExecutionTime.set(0);
        framesProcessed.set(0);
        sessionStartTime = System.currentTimeMillis();
        actionTypeMetrics.clear();
        synchronized (recentActions) {
            recentActions.clear();
        }
        Log.d(TAG, "Metrics reset");
    }
    
    /**
     * Metrics for specific action types
     */
    public static class ActionTypeMetrics {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final List<String> recentErrors = Collections.synchronizedList(new ArrayList<>());
        
        public void recordAction(boolean success, long executionTime) {
            count.incrementAndGet();
            if (success) {
                successCount.incrementAndGet();
            }
            totalExecutionTime.addAndGet(executionTime);
        }
        
        public void recordFailure(String error) {
            count.incrementAndGet();
            synchronized (recentErrors) {
                recentErrors.add(error);
                if (recentErrors.size() > 10) {
                    recentErrors.remove(0);
                }
            }
        }
        
        public int getCount() { return count.get(); }
        public int getSuccessCount() { return successCount.get(); }
        public double getSuccessRate() {
            int total = count.get();
            return total > 0 ? (double) successCount.get() / total * 100.0 : 0.0;
        }
        public long getAverageExecutionTime() {
            int total = count.get();
            return total > 0 ? totalExecutionTime.get() / total : 0;
        }
        public List<String> getRecentErrors() {
            synchronized (recentErrors) {
                return new ArrayList<>(recentErrors);
            }
        }
    }
    
    /**
     * Individual action performance record
     */
    public static class ActionPerformanceRecord {
        public final long timestamp;
        public final String actionType;
        public final boolean success;
        public final long executionTime;
        public final long totalLatency;
        
        public ActionPerformanceRecord(long timestamp, String actionType, boolean success, 
                                     long executionTime, long totalLatency) {
            this.timestamp = timestamp;
            this.actionType = actionType;
            this.success = success;
            this.executionTime = executionTime;
            this.totalLatency = totalLatency;
        }
    }
}