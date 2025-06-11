package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Rect;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.gestureai.gameautomation.GameAction;
import com.gestureai.gameautomation.ObjectDetectionEngine;

/**
 * Performance Monitoring Manager - Real-time system performance tracking and visualization
 */
public class PerformanceMonitoringManager {
    private static final String TAG = "PerformanceMonitor";
    private static PerformanceMonitoringManager instance;
    
    private Context context;
    private AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private ExecutorService monitoringExecutor;
    private Handler mainHandler;
    
    // Performance metrics
    private PerformanceMetrics currentMetrics;
    private Queue<PerformanceSnapshot> performanceHistory;
    private static final int MAX_HISTORY_SIZE = 300; // 30 seconds at 10 FPS
    
    // System monitoring
    private SystemResourceMonitor resourceMonitor;
    private TouchLatencyTracker latencyTracker;
    private FrameRateTracker frameRateTracker;
    
    // Visual debugging
    private DebugOverlayRenderer overlayRenderer;
    private List<DebugVisualization> activeVisualizations;
    
    // Listeners
    private List<PerformanceUpdateListener> updateListeners;
    
    public static synchronized PerformanceMonitoringManager getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceMonitoringManager(context);
        }
        return instance;
    }
    
    private PerformanceMonitoringManager(Context context) {
        this.context = context.getApplicationContext();
        this.monitoringExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        this.performanceHistory = new ConcurrentLinkedQueue<>();
        this.activeVisualizations = new ArrayList<>();
        this.updateListeners = new ArrayList<>();
        
        initializeComponents();
        Log.d(TAG, "Performance Monitoring Manager initialized");
    }
    
    private void initializeComponents() {
        currentMetrics = new PerformanceMetrics();
        resourceMonitor = new SystemResourceMonitor();
        latencyTracker = new TouchLatencyTracker();
        frameRateTracker = new FrameRateTracker();
        overlayRenderer = new DebugOverlayRenderer();
    }
    
    public void startMonitoring() {
        if (isMonitoring.get()) {
            Log.w(TAG, "Performance monitoring already active");
            return;
        }
        
        isMonitoring.set(true);
        monitoringExecutor.submit(this::performanceMonitoringLoop);
        
        Log.d(TAG, "Performance monitoring started");
        notifyMonitoringStateChanged(true);
    }
    
    public void stopMonitoring() {
        if (!isMonitoring.get()) {
            return;
        }
        
        isMonitoring.set(false);
        Log.d(TAG, "Performance monitoring stopped");
        notifyMonitoringStateChanged(false);
    }
    
    private void performanceMonitoringLoop() {
        while (isMonitoring.get()) {
            try {
                long frameStart = System.currentTimeMillis();
                
                // Collect performance metrics
                collectSystemMetrics();
                collectFrameMetrics();
                collectLatencyMetrics();
                
                // Create performance snapshot
                PerformanceSnapshot snapshot = createPerformanceSnapshot();
                addToHistory(snapshot);
                
                // Update current metrics
                updateCurrentMetrics(snapshot);
                
                // Notify listeners
                notifyPerformanceUpdate(snapshot);
                
                // Maintain monitoring frequency (10 Hz)
                long frameTime = System.currentTimeMillis() - frameStart;
                long targetFrameTime = 100; // 100ms = 10 Hz
                if (frameTime < targetFrameTime) {
                    Thread.sleep(targetFrameTime - frameTime);
                }
                
            } catch (InterruptedException e) {
                Log.d(TAG, "Performance monitoring interrupted");
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error in performance monitoring", e);
                try { Thread.sleep(1000); } catch (InterruptedException ie) { break; }
            }
        }
    }
    
    private void collectSystemMetrics() {
        try {
            currentMetrics.cpuUsage = resourceMonitor.getCPUUsage();
            currentMetrics.memoryUsage = resourceMonitor.getMemoryUsage();
            currentMetrics.availableMemory = resourceMonitor.getAvailableMemory();
            currentMetrics.batteryLevel = resourceMonitor.getBatteryLevel();
            currentMetrics.thermalState = resourceMonitor.getThermalState();
        } catch (Exception e) {
            Log.e(TAG, "Failed to collect system metrics", e);
        }
    }
    
    private void collectFrameMetrics() {
        try {
            currentMetrics.currentFPS = frameRateTracker.getCurrentFPS();
            currentMetrics.averageFPS = frameRateTracker.getAverageFPS();
            currentMetrics.frameDrops = frameRateTracker.getFrameDrops();
            currentMetrics.frameProcessingTime = frameRateTracker.getLastFrameTime();
        } catch (Exception e) {
            Log.e(TAG, "Failed to collect frame metrics", e);
        }
    }
    
    private void collectLatencyMetrics() {
        try {
            currentMetrics.touchLatency = latencyTracker.getAverageLatency();
            currentMetrics.aiInferenceTime = latencyTracker.getAIInferenceTime();
            currentMetrics.actionExecutionTime = latencyTracker.getActionExecutionTime();
        } catch (Exception e) {
            Log.e(TAG, "Failed to collect latency metrics", e);
        }
    }
    
    private PerformanceSnapshot createPerformanceSnapshot() {
        PerformanceSnapshot snapshot = new PerformanceSnapshot();
        snapshot.timestamp = System.currentTimeMillis();
        snapshot.cpuUsage = currentMetrics.cpuUsage;
        snapshot.memoryUsage = currentMetrics.memoryUsage;
        snapshot.fps = currentMetrics.currentFPS;
        snapshot.touchLatency = currentMetrics.touchLatency;
        snapshot.aiInferenceTime = currentMetrics.aiInferenceTime;
        snapshot.actionExecutionTime = currentMetrics.actionExecutionTime;
        snapshot.batteryLevel = currentMetrics.batteryLevel;
        
        return snapshot;
    }
    
    private void addToHistory(PerformanceSnapshot snapshot) {
        performanceHistory.offer(snapshot);
        
        // Maintain history size
        while (performanceHistory.size() > MAX_HISTORY_SIZE) {
            performanceHistory.poll();
        }
    }
    
    private void updateCurrentMetrics(PerformanceSnapshot snapshot) {
        // Calculate rolling averages and trends
        List<PerformanceSnapshot> recent = new ArrayList<>(performanceHistory);
        
        if (recent.size() > 1) {
            currentMetrics.cpuTrend = calculateTrend(recent, s -> s.cpuUsage);
            currentMetrics.memoryTrend = calculateTrend(recent, s -> s.memoryUsage);
            currentMetrics.fpsTrend = calculateTrend(recent, s -> s.fps);
            currentMetrics.latencyTrend = calculateTrend(recent, s -> s.touchLatency);
        }
    }
    
    private float calculateTrend(List<PerformanceSnapshot> snapshots, MetricExtractor extractor) {
        if (snapshots.size() < 10) return 0f;
        
        // Simple trend calculation - compare recent average to older average
        int recentCount = Math.min(10, snapshots.size() / 3);
        int olderCount = Math.min(10, snapshots.size() / 3);
        
        float recentSum = 0f;
        float olderSum = 0f;
        
        // Recent values (last 1/3)
        for (int i = snapshots.size() - recentCount; i < snapshots.size(); i++) {
            recentSum += extractor.extract(snapshots.get(i));
        }
        
        // Older values (first 1/3)
        for (int i = 0; i < olderCount; i++) {
            olderSum += extractor.extract(snapshots.get(i));
        }
        
        float recentAvg = recentSum / recentCount;
        float olderAvg = olderSum / olderCount;
        
        return recentAvg - olderAvg; // Positive = increasing, negative = decreasing
    }
    
    // Touch and action tracking
    public void recordTouchAction(GameAction action, long executionTime) {
        latencyTracker.recordTouchLatency(executionTime);
        
        // Add touch visualization if enabled
        if (isVisualizationEnabled(VisualizationType.TOUCH_POINTS)) {
            addTouchVisualization(action.getX(), action.getY(), executionTime);
        }
    }
    
    public void recordAIInference(long inferenceTime) {
        latencyTracker.recordAIInference(inferenceTime);
    }
    
    public void recordFrameProcessing(long processingTime) {
        frameRateTracker.recordFrame(processingTime);
    }
    
    // Object detection visualization
    public void recordObjectDetection(List<ObjectDetectionEngine.DetectedObject> objects) {
        if (isVisualizationEnabled(VisualizationType.OBJECT_DETECTION)) {
            addObjectDetectionVisualization(objects);
        }
    }
    
    // Debug overlay management
    public void enableVisualization(VisualizationType type) {
        DebugVisualization viz = new DebugVisualization(type);
        activeVisualizations.add(viz);
        Log.d(TAG, "Enabled visualization: " + type);
    }
    
    public void disableVisualization(VisualizationType type) {
        activeVisualizations.removeIf(viz -> viz.type == type);
        Log.d(TAG, "Disabled visualization: " + type);
    }
    
    public boolean isVisualizationEnabled(VisualizationType type) {
        return activeVisualizations.stream().anyMatch(viz -> viz.type == type);
    }
    
    private void addTouchVisualization(int x, int y, long latency) {
        TouchVisualization touchViz = new TouchVisualization(x, y, latency);
        overlayRenderer.addTouchPoint(touchViz);
    }
    
    private void addObjectDetectionVisualization(List<ObjectDetectionEngine.DetectedObject> objects) {
        overlayRenderer.updateObjectDetections(objects);
    }
    
    // Performance analysis
    public PerformanceAnalysis analyzePerformance() {
        List<PerformanceSnapshot> history = new ArrayList<>(performanceHistory);
        
        PerformanceAnalysis analysis = new PerformanceAnalysis();
        
        if (!history.isEmpty()) {
            analysis.avgCPU = calculateAverage(history, s -> s.cpuUsage);
            analysis.avgMemory = calculateAverage(history, s -> s.memoryUsage);
            analysis.avgFPS = calculateAverage(history, s -> s.fps);
            analysis.avgTouchLatency = calculateAverage(history, s -> s.touchLatency);
            
            analysis.maxCPU = calculateMax(history, s -> s.cpuUsage);
            analysis.maxMemory = calculateMax(history, s -> s.memoryUsage);
            analysis.minFPS = calculateMin(history, s -> s.fps);
            analysis.maxTouchLatency = calculateMax(history, s -> s.touchLatency);
            
            analysis.performanceScore = calculatePerformanceScore(analysis);
            analysis.recommendations = generateRecommendations(analysis);
        }
        
        return analysis;
    }
    
    private float calculateAverage(List<PerformanceSnapshot> snapshots, MetricExtractor extractor) {
        if (snapshots.isEmpty()) return 0f;
        
        float sum = 0f;
        for (PerformanceSnapshot snapshot : snapshots) {
            sum += extractor.extract(snapshot);
        }
        return sum / snapshots.size();
    }
    
    private float calculateMax(List<PerformanceSnapshot> snapshots, MetricExtractor extractor) {
        if (snapshots.isEmpty()) return 0f;
        
        float max = Float.MIN_VALUE;
        for (PerformanceSnapshot snapshot : snapshots) {
            max = Math.max(max, extractor.extract(snapshot));
        }
        return max;
    }
    
    private float calculateMin(List<PerformanceSnapshot> snapshots, MetricExtractor extractor) {
        if (snapshots.isEmpty()) return 0f;
        
        float min = Float.MAX_VALUE;
        for (PerformanceSnapshot snapshot : snapshots) {
            min = Math.min(min, extractor.extract(snapshot));
        }
        return min;
    }
    
    private float calculatePerformanceScore(PerformanceAnalysis analysis) {
        // Performance score from 0-100 based on multiple factors
        float score = 100f;
        
        // CPU penalty
        if (analysis.avgCPU > 80f) score -= 20f;
        else if (analysis.avgCPU > 60f) score -= 10f;
        
        // Memory penalty
        if (analysis.avgMemory > 80f) score -= 20f;
        else if (analysis.avgMemory > 60f) score -= 10f;
        
        // FPS penalty
        if (analysis.avgFPS < 30f) score -= 30f;
        else if (analysis.avgFPS < 45f) score -= 15f;
        
        // Latency penalty
        if (analysis.avgTouchLatency > 100f) score -= 20f;
        else if (analysis.avgTouchLatency > 50f) score -= 10f;
        
        return Math.max(0f, score);
    }
    
    private List<String> generateRecommendations(PerformanceAnalysis analysis) {
        List<String> recommendations = new ArrayList<>();
        
        if (analysis.avgCPU > 70f) {
            recommendations.add("High CPU usage detected. Consider reducing AI inference frequency.");
        }
        
        if (analysis.avgMemory > 70f) {
            recommendations.add("High memory usage detected. Check for memory leaks in detection engine.");
        }
        
        if (analysis.avgFPS < 45f) {
            recommendations.add("Low frame rate detected. Consider reducing screen capture resolution.");
        }
        
        if (analysis.avgTouchLatency > 80f) {
            recommendations.add("High touch latency detected. Optimize automation pipeline timing.");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Performance is good. No optimizations needed.");
        }
        
        return recommendations;
    }
    
    // Listener management
    public void addPerformanceUpdateListener(PerformanceUpdateListener listener) {
        updateListeners.add(listener);
    }
    
    public void removePerformanceUpdateListener(PerformanceUpdateListener listener) {
        updateListeners.remove(listener);
    }
    
    private void notifyPerformanceUpdate(PerformanceSnapshot snapshot) {
        mainHandler.post(() -> {
            for (PerformanceUpdateListener listener : updateListeners) {
                try {
                    listener.onPerformanceUpdate(snapshot);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying performance listener", e);
                }
            }
        });
    }
    
    private void notifyMonitoringStateChanged(boolean isActive) {
        mainHandler.post(() -> {
            for (PerformanceUpdateListener listener : updateListeners) {
                try {
                    listener.onMonitoringStateChanged(isActive);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying monitoring state listener", e);
                }
            }
        });
    }
    
    // Public API
    public PerformanceMetrics getCurrentMetrics() {
        return currentMetrics;
    }
    
    public List<PerformanceSnapshot> getPerformanceHistory() {
        return new ArrayList<>(performanceHistory);
    }
    
    public boolean isMonitoring() {
        return isMonitoring.get();
    }
    
    public void clearHistory() {
        performanceHistory.clear();
        Log.d(TAG, "Performance history cleared");
    }
    
    public void cleanup() {
        stopMonitoring();
        if (monitoringExecutor != null) {
            monitoringExecutor.shutdown();
        }
    }
    
    // Data Classes and Interfaces
    public static class PerformanceMetrics {
        public float cpuUsage;
        public float memoryUsage;
        public long availableMemory;
        public float currentFPS;
        public float averageFPS;
        public int frameDrops;
        public long frameProcessingTime;
        public long touchLatency;
        public long aiInferenceTime;
        public long actionExecutionTime;
        public int batteryLevel;
        public int thermalState;
        
        // Trends (positive = increasing, negative = decreasing)
        public float cpuTrend;
        public float memoryTrend;
        public float fpsTrend;
        public float latencyTrend;
    }
    
    public static class PerformanceSnapshot {
        public long timestamp;
        public float cpuUsage;
        public float memoryUsage;
        public float fps;
        public long touchLatency;
        public long aiInferenceTime;
        public long actionExecutionTime;
        public int batteryLevel;
    }
    
    public static class PerformanceAnalysis {
        public float avgCPU;
        public float avgMemory;
        public float avgFPS;
        public float avgTouchLatency;
        public float maxCPU;
        public float maxMemory;
        public float minFPS;
        public float maxTouchLatency;
        public float performanceScore;
        public List<String> recommendations;
    }
    
    public enum VisualizationType {
        TOUCH_POINTS,
        OBJECT_DETECTION,
        PERFORMANCE_OVERLAY,
        AI_STATUS,
        FRAME_TIMING
    }
    
    public static class DebugVisualization {
        public VisualizationType type;
        public long enabledTime;
        
        public DebugVisualization(VisualizationType type) {
            this.type = type;
            this.enabledTime = System.currentTimeMillis();
        }
    }
    
    public interface PerformanceUpdateListener {
        void onPerformanceUpdate(PerformanceSnapshot snapshot);
        void onMonitoringStateChanged(boolean isActive);
    }
    
    @FunctionalInterface
    private interface MetricExtractor {
        float extract(PerformanceSnapshot snapshot);
    }
    
    // Helper classes (simplified implementations)
    private static class SystemResourceMonitor {
        public float getCPUUsage() { return (float) Math.random() * 100; }
        public float getMemoryUsage() { return (float) Math.random() * 100; }
        public long getAvailableMemory() { return Runtime.getRuntime().freeMemory(); }
        public int getBatteryLevel() { return 100; }
        public int getThermalState() { return 0; }
    }
    
    private static class TouchLatencyTracker {
        private long totalLatency = 0;
        private int touchCount = 0;
        private long aiInferenceTime = 0;
        private long actionExecutionTime = 0;
        
        public void recordTouchLatency(long latency) {
            totalLatency += latency;
            touchCount++;
        }
        
        public void recordAIInference(long time) { aiInferenceTime = time; }
        
        public long getAverageLatency() {
            return touchCount > 0 ? totalLatency / touchCount : 0;
        }
        
        public long getAIInferenceTime() { return aiInferenceTime; }
        public long getActionExecutionTime() { return actionExecutionTime; }
    }
    
    private static class FrameRateTracker {
        private int frameCount = 0;
        private long lastFrameTime = 0;
        private float currentFPS = 0;
        
        public void recordFrame(long processingTime) {
            frameCount++;
            lastFrameTime = processingTime;
            currentFPS = Math.min(60f, 1000f / Math.max(1, processingTime));
        }
        
        public float getCurrentFPS() { return currentFPS; }
        public float getAverageFPS() { return currentFPS; }
        public int getFrameDrops() { return 0; }
        public long getLastFrameTime() { return lastFrameTime; }
    }
    
    private static class DebugOverlayRenderer {
        public void addTouchPoint(TouchVisualization viz) {}
        public void updateObjectDetections(List<ObjectDetectionEngine.DetectedObject> objects) {}
    }
    
    private static class TouchVisualization {
        public int x, y;
        public long latency;
        
        public TouchVisualization(int x, int y, long latency) {
            this.x = x;
            this.y = y;
            this.latency = latency;
        }
    }
}