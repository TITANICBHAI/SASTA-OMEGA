package com.gestureai.gameautomation.ui;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.PerformanceTracker;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Bridge for real-time performance dashboard UI integration
 * Connects analytics backend to frontend displays
 */
public class PerformanceDashboardBridge {
    private static final String TAG = "PerformanceDashboardBridge";
    
    private Context context;
    private PerformanceTracker performanceTracker;
    private boolean isStreaming = false;
    
    public interface PerformanceDataListener {
        void onCPUUsageUpdate(double cpuPercent);
        void onMemoryUsageUpdate(double memoryPercent);
        void onFPSUpdate(double fps);
        void onAIAccuracyUpdate(double accuracy);
        void onGameScoreUpdate(int score);
        void onOverallPerformanceUpdate(double overallScore);
    }
    
    public interface MetricsHistoryListener {
        void onHistoryUpdate(Map<String, List<Double>> metricsHistory);
    }
    
    private List<PerformanceDataListener> dataListeners = new ArrayList<>();
    private List<MetricsHistoryListener> historyListeners = new ArrayList<>();
    private Map<String, List<Double>> metricsHistory = new HashMap<>();
    
    public PerformanceDashboardBridge(Context context) {
        this.context = context;
        this.performanceTracker = new PerformanceTracker(context);
        initializeHistoryTracking();
        Log.d(TAG, "Performance Dashboard Bridge initialized");
    }
    
    private void initializeHistoryTracking() {
        metricsHistory.put("cpu_usage", new ArrayList<>());
        metricsHistory.put("memory_usage", new ArrayList<>());
        metricsHistory.put("fps", new ArrayList<>());
        metricsHistory.put("ai_accuracy", new ArrayList<>());
        metricsHistory.put("game_score", new ArrayList<>());
        metricsHistory.put("overall_score", new ArrayList<>());
    }
    
    public void startRealTimeStreaming() {
        if (isStreaming) return;
        
        isStreaming = true;
        performanceTracker.startTracking();
        
        new Thread(() -> {
            while (isStreaming) {
                try {
                    Thread.sleep(500); // Update every 500ms for real-time feel
                    
                    updatePerformanceMetrics();
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
        
        Log.d(TAG, "Real-time performance streaming started");
    }
    
    private void updatePerformanceMetrics() {
        // Get current system metrics
        double cpuUsage = getCurrentCPUUsage();
        double memoryUsage = getCurrentMemoryUsage();
        double fps = getCurrentFPS();
        double aiAccuracy = getCurrentAIAccuracy();
        int gameScore = getCurrentGameScore();
        
        // Record in performance tracker
        performanceTracker.recordSystemUsage(cpuUsage, memoryUsage, fps);
        performanceTracker.recordAIAccuracy(aiAccuracy);
        performanceTracker.recordGameScore(gameScore);
        
        double overallScore = performanceTracker.getCurrentOverallScore();
        
        // Update history
        updateMetricsHistory("cpu_usage", cpuUsage);
        updateMetricsHistory("memory_usage", memoryUsage);
        updateMetricsHistory("fps", fps);
        updateMetricsHistory("ai_accuracy", aiAccuracy);
        updateMetricsHistory("game_score", (double) gameScore);
        updateMetricsHistory("overall_score", overallScore);
        
        // Notify UI listeners
        for (PerformanceDataListener listener : dataListeners) {
            listener.onCPUUsageUpdate(cpuUsage);
            listener.onMemoryUsageUpdate(memoryUsage);
            listener.onFPSUpdate(fps);
            listener.onAIAccuracyUpdate(aiAccuracy);
            listener.onGameScoreUpdate(gameScore);
            listener.onOverallPerformanceUpdate(overallScore);
        }
        
        // Notify history listeners
        for (MetricsHistoryListener listener : historyListeners) {
            listener.onHistoryUpdate(new HashMap<>(metricsHistory));
        }
    }
    
    private void updateMetricsHistory(String metricName, double value) {
        List<Double> history = metricsHistory.get(metricName);
        if (history != null) {
            history.add(value);
            
            // Keep only last 100 data points
            if (history.size() > 100) {
                history.remove(0);
            }
        }
    }
    
    private double getCurrentCPUUsage() {
        // Simulate realistic CPU usage
        return Math.random() * 30 + 40; // 40-70% range
    }
    
    private double getCurrentMemoryUsage() {
        // Get actual memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return (double) usedMemory / totalMemory * 100;
    }
    
    private double getCurrentFPS() {
        // Simulate FPS monitoring
        return Math.random() * 20 + 45; // 45-65 FPS range
    }
    
    private double getCurrentAIAccuracy() {
        // Simulate AI accuracy based on performance
        return Math.random() * 0.2 + 0.75; // 75-95% accuracy
    }
    
    private int getCurrentGameScore() {
        // Simulate game score progression
        return (int) (Math.random() * 1000 + 500);
    }
    
    public void stopRealTimeStreaming() {
        isStreaming = false;
        performanceTracker.stopTracking();
        Log.d(TAG, "Real-time performance streaming stopped");
    }
    
    public Map<String, Double> getCurrentSnapshot() {
        Map<String, Double> snapshot = new HashMap<>();
        Map<String, PerformanceTracker.PerformanceMetric> metrics = performanceTracker.getAllMetrics();
        
        for (Map.Entry<String, PerformanceTracker.PerformanceMetric> entry : metrics.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().currentValue);
        }
        
        return snapshot;
    }
    
    public String getPerformanceSummary() {
        return performanceTracker.getPerformanceSummary();
    }
    
    public void addPerformanceDataListener(PerformanceDataListener listener) {
        dataListeners.add(listener);
    }
    
    public void addMetricsHistoryListener(MetricsHistoryListener listener) {
        historyListeners.add(listener);
    }
    
    public void removePerformanceDataListener(PerformanceDataListener listener) {
        dataListeners.remove(listener);
    }
    
    public void removeMetricsHistoryListener(MetricsHistoryListener listener) {
        historyListeners.remove(listener);
    }
    
    public void cleanup() {
        stopRealTimeStreaming();
        dataListeners.clear();
        historyListeners.clear();
        metricsHistory.clear();
        Log.d(TAG, "Performance Dashboard Bridge cleaned up");
    }
}