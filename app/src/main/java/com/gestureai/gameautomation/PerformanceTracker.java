package com.gestureai.gameautomation;

import android.content.Context;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceTracker {
    private static final String TAG = "PerformanceTracker";
    private static volatile PerformanceTracker instance;
    private static final Object instanceLock = new Object();
    
    public static PerformanceTracker getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new PerformanceTracker();
                    Log.d(TAG, "PerformanceTracker instance created");
                }
            }
        }
        return instance;
    }
    
    public static PerformanceTracker getInstance(Context context) {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new PerformanceTracker(context);
                    Log.d(TAG, "PerformanceTracker instance created with context");
                }
            }
        }
        return instance;
    }
    
    private Context context;
    private final Map<String, PerformanceMetric> metrics;
    private final List<PerformanceSnapshot> snapshots;
    private long sessionStartTime;
    private volatile boolean isTracking = false;
    private final Object metricsLock = new Object();
    
    public static class PerformanceMetric {
        public String name;
        public double currentValue;
        public double averageValue;
        public double minValue;
        public double maxValue;
        public int sampleCount;
        public long lastUpdated;
        
        public PerformanceMetric(String name) {
            this.name = name;
            this.currentValue = 0.0;
            this.averageValue = 0.0;
            this.minValue = Double.MAX_VALUE;
            this.maxValue = Double.MIN_VALUE;
            this.sampleCount = 0;
            this.lastUpdated = System.currentTimeMillis();
        }
        
        public void updateValue(double value) {
            this.currentValue = value;
            this.sampleCount++;
            this.averageValue = ((this.averageValue * (sampleCount - 1)) + value) / sampleCount;
            this.minValue = Math.min(this.minValue, value);
            this.maxValue = Math.max(this.maxValue, value);
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    public static class PerformanceSnapshot {
        public long timestamp;
        public Map<String, Double> values;
        public double overallScore;
        
        public PerformanceSnapshot() {
            this.timestamp = System.currentTimeMillis();
            this.values = new HashMap<>();
            this.overallScore = 0.0;
        }
    }
    
    public PerformanceTracker() {
        this.metrics = new ConcurrentHashMap<>();
        this.snapshots = new ArrayList<>();
        this.sessionStartTime = System.currentTimeMillis();
        
        initializeMetrics();
        Log.d(TAG, "PerformanceTracker initialized without context");
    }
    
    public PerformanceTracker(Context context) {
        this.context = context;
        this.metrics = new ConcurrentHashMap<>();
        this.snapshots = new ArrayList<>();
        this.sessionStartTime = System.currentTimeMillis();
        
        initializeMetrics();
        Log.d(TAG, "PerformanceTracker initialized");
    }
    
    private void initializeMetrics() {
        // AI Performance Metrics
        addMetric("ai_decision_time");
        addMetric("ai_accuracy");
        addMetric("ai_confidence");
        addMetric("learning_rate");
        
        // Game Performance Metrics
        addMetric("game_score");
        addMetric("survival_time");
        addMetric("actions_per_minute");
        addMetric("success_rate");
        
        // System Performance Metrics
        addMetric("cpu_usage");
        addMetric("memory_usage");
        addMetric("fps");
        addMetric("latency");
        
        // Strategy Metrics
        addMetric("strategy_effectiveness");
        addMetric("pattern_recognition");
        addMetric("adaptation_speed");
        addMetric("error_recovery");
    }
    
    private void addMetric(String name) {
        metrics.put(name, new PerformanceMetric(name));
    }
    
    public void startTracking() {
        isTracking = true;
        sessionStartTime = System.currentTimeMillis();
        Log.d(TAG, "Performance tracking started");
    }
    
    public void stopTracking() {
        isTracking = false;
        Log.d(TAG, "Performance tracking stopped");
    }
    
    public void recordMetric(String metricName, double value) {
        if (!isTracking) return;
        
        PerformanceMetric metric = metrics.get(metricName);
        if (metric != null) {
            metric.updateValue(value);
            Log.d(TAG, "Recorded " + metricName + ": " + value);
        }
    }
    
    public void recordAIDecisionTime(long milliseconds) {
        recordMetric("ai_decision_time", milliseconds);
    }
    
    public void recordAIAccuracy(double accuracy) {
        recordMetric("ai_accuracy", accuracy);
    }
    
    public void recordGameScore(int score) {
        recordMetric("game_score", score);
    }
    
    public void recordSystemUsage(double cpuPercent, double memoryPercent, double fps) {
        recordMetric("cpu_usage", cpuPercent);
        recordMetric("memory_usage", memoryPercent);
        recordMetric("fps", fps);
    }
    
    public PerformanceSnapshot takeSnapshot() {
        PerformanceSnapshot snapshot = new PerformanceSnapshot();
        double totalScore = 0.0;
        int metricCount = 0;
        
        for (PerformanceMetric metric : metrics.values()) {
            snapshot.values.put(metric.name, metric.currentValue);
            
            // Calculate weighted overall score
            double normalizedValue = normalizeMetricValue(metric);
            totalScore += normalizedValue;
            metricCount++;
        }
        
        snapshot.overallScore = metricCount > 0 ? totalScore / metricCount : 0.0;
        snapshots.add(snapshot);
        
        // Keep only last 100 snapshots
        if (snapshots.size() > 100) {
            snapshots.remove(0);
        }
        
        return snapshot;
    }
    
    private double normalizeMetricValue(PerformanceMetric metric) {
        // Normalize different metrics to 0-1 scale for overall score calculation
        switch (metric.name) {
            case "ai_accuracy":
            case "success_rate":
            case "strategy_effectiveness":
                return metric.currentValue; // Already 0-1
                
            case "ai_decision_time":
            case "latency":
                return Math.max(0, 1.0 - (metric.currentValue / 1000.0)); // Lower is better
                
            case "cpu_usage":
            case "memory_usage":
                return Math.max(0, 1.0 - (metric.currentValue / 100.0)); // Lower is better
                
            case "fps":
                return Math.min(1.0, metric.currentValue / 60.0); // Higher is better, cap at 60
                
            default:
                return metric.currentValue / (metric.maxValue > 0 ? metric.maxValue : 1.0);
        }
    }
    
    public PerformanceMetric getMetric(String name) {
        return metrics.get(name);
    }
    
    public Map<String, PerformanceMetric> getAllMetrics() {
        return new HashMap<>(metrics);
    }
    
    public List<PerformanceSnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }
    
    public double getCurrentOverallScore() {
        PerformanceSnapshot current = takeSnapshot();
        return current.overallScore;
    }
    
    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    public String getPerformanceSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Performance Summary:\n");
        summary.append("Session Duration: ").append(getSessionDuration() / 1000).append("s\n");
        summary.append("Overall Score: ").append(String.format("%.2f", getCurrentOverallScore())).append("\n\n");
        
        for (PerformanceMetric metric : metrics.values()) {
            if (metric.sampleCount > 0) {
                summary.append(metric.name).append(": ")
                    .append(String.format("%.2f", metric.currentValue))
                    .append(" (avg: ").append(String.format("%.2f", metric.averageValue))
                    .append(")\n");
            }
        }
        
        return summary.toString();
    }
    
    public void reset() {
        for (PerformanceMetric metric : metrics.values()) {
            metric.currentValue = 0.0;
            metric.averageValue = 0.0;
            metric.minValue = Double.MAX_VALUE;
            metric.maxValue = Double.MIN_VALUE;
            metric.sampleCount = 0;
        }
        snapshots.clear();
        sessionStartTime = System.currentTimeMillis();
        Log.d(TAG, "Performance metrics reset");
    }
}