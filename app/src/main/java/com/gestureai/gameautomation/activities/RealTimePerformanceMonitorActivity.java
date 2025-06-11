package com.gestureai.gameautomation.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Button;
import android.view.View;

import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.managers.PerformanceMonitoringManager;
import com.gestureai.gameautomation.ai.ResourceMonitor;
import com.gestureai.gameautomation.services.ServiceHealthMonitor;

/**
 * Real-time AI Performance Monitoring Interface
 * Provides live visualization of AI performance metrics that were previously hidden
 */
public class RealTimePerformanceMonitorActivity extends Activity {
    private static final String TAG = "RealTimePerformance";
    
    // UI Components
    private TextView tvAIConfidence;
    private TextView tvDetectionAccuracy;
    private TextView tvActionSuccessRate;
    private TextView tvAverageLatency;
    private TextView tvCPUUsage;
    private TextView tvMemoryUsage;
    private TextView tvGPUUsage;
    private ProgressBar pbAIPerformance;
    private ProgressBar pbSystemHealth;
    private Button btnExportMetrics;
    private Button btnResetCounters;
    
    // Backend Components
    private PerformanceMonitoringManager performanceManager;
    private ResourceMonitor resourceMonitor;
    private ServiceHealthMonitor serviceHealthMonitor;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isMonitoring = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_real_time_performance_dashboard);
            
            initializeComponents();
            setupUpdateLoop();
            startMonitoring();
            
            Log.d(TAG, "Real-time Performance Monitor initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Performance Monitor", e);
            finish();
        }
    }
    
    private void initializeComponents() {
        // Initialize UI components
        tvAIConfidence = findViewById(R.id.tv_ai_confidence);
        tvDetectionAccuracy = findViewById(R.id.tv_detection_accuracy);
        tvActionSuccessRate = findViewById(R.id.tv_action_success_rate);
        tvAverageLatency = findViewById(R.id.tv_average_latency);
        tvCPUUsage = findViewById(R.id.tv_cpu_usage);
        tvMemoryUsage = findViewById(R.id.tv_memory_usage);
        tvGPUUsage = findViewById(R.id.tv_gpu_usage);
        pbAIPerformance = findViewById(R.id.pb_ai_performance);
        pbSystemHealth = findViewById(R.id.pb_system_health);
        btnExportMetrics = findViewById(R.id.btn_export_metrics);
        btnResetCounters = findViewById(R.id.btn_reset_counters);
        
        // Initialize backend components
        performanceManager = new PerformanceMonitoringManager(this);
        resourceMonitor = new ResourceMonitor(this);
        serviceHealthMonitor = ServiceHealthMonitor.getInstance();
        
        // Setup button listeners
        btnExportMetrics.setOnClickListener(this::exportMetrics);
        btnResetCounters.setOnClickListener(this::resetCounters);
        
        updateHandler = new Handler(Looper.getMainLooper());
    }
    
    private void setupUpdateLoop() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMonitoring) {
                    updatePerformanceMetrics();
                    updateSystemMetrics();
                    updateServiceHealth();
                    
                    // Update every 1 second for real-time monitoring
                    updateHandler.postDelayed(this, 1000);
                }
            }
        };
    }
    
    private void startMonitoring() {
        isMonitoring = true;
        updateHandler.post(updateRunnable);
        Log.d(TAG, "Real-time monitoring started");
    }
    
    private void updatePerformanceMetrics() {
        try {
            // Get AI performance metrics from PerformanceMonitoringManager
            float aiConfidence = performanceManager.getCurrentAIConfidence();
            float detectionAccuracy = performanceManager.getDetectionAccuracy();
            float actionSuccessRate = performanceManager.getActionSuccessRate();
            long averageLatency = performanceManager.getAverageLatency();
            
            // Update UI
            tvAIConfidence.setText(String.format("AI Confidence: %.1f%%", aiConfidence * 100));
            tvDetectionAccuracy.setText(String.format("Detection Accuracy: %.1f%%", detectionAccuracy * 100));
            tvActionSuccessRate.setText(String.format("Action Success: %.1f%%", actionSuccessRate * 100));
            tvAverageLatency.setText(String.format("Avg Latency: %dms", averageLatency));
            
            // Update progress bars
            pbAIPerformance.setProgress((int)(aiConfidence * 100));
            
        } catch (Exception e) {
            Log.w(TAG, "Error updating performance metrics", e);
        }
    }
    
    private void updateSystemMetrics() {
        try {
            // Get system resource metrics from ResourceMonitor
            float cpuUsage = resourceMonitor.getCPUUsage();
            float memoryUsage = resourceMonitor.getMemoryUsage();
            float gpuUsage = resourceMonitor.getGPUUsage();
            
            // Update UI
            tvCPUUsage.setText(String.format("CPU: %.1f%%", cpuUsage));
            tvMemoryUsage.setText(String.format("Memory: %.1f%%", memoryUsage));
            tvGPUUsage.setText(String.format("GPU: %.1f%%", gpuUsage));
            
            // Update system health progress bar
            float systemHealth = (100 - cpuUsage - memoryUsage) / 2;
            pbSystemHealth.setProgress((int)Math.max(0, systemHealth));
            
        } catch (Exception e) {
            Log.w(TAG, "Error updating system metrics", e);
        }
    }
    
    private void updateServiceHealth() {
        try {
            if (serviceHealthMonitor != null) {
                boolean allServicesHealthy = serviceHealthMonitor.areAllServicesHealthy();
                int activeServices = serviceHealthMonitor.getActiveServiceCount();
                
                // Visual indicator for service health could be added here
                Log.d(TAG, "Services healthy: " + allServicesHealthy + ", Active: " + activeServices);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error updating service health", e);
        }
    }
    
    private void exportMetrics(View view) {
        try {
            String exportPath = performanceManager.exportPerformanceData();
            if (exportPath != null) {
                Log.i(TAG, "Performance metrics exported to: " + exportPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to export metrics", e);
        }
    }
    
    private void resetCounters(View view) {
        try {
            performanceManager.resetCounters();
            resourceMonitor.resetCounters();
            Log.i(TAG, "Performance counters reset");
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset counters", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isMonitoring = false;
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        Log.d(TAG, "Real-time Performance Monitor destroyed");
    }
}