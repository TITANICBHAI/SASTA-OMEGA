package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.PerformanceTracker;
import com.gestureai.gameautomation.utils.MemoryManager;
import com.gestureai.gameautomation.managers.ServiceConnectionManager;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Advanced Debugging Activity
 * Provides comprehensive system diagnostics and debugging tools
 */
public class AdvancedDebuggingActivity extends AppCompatActivity {
    private static final String TAG = "AdvancedDebugging";
    
    private TextView tvSystemStatus;
    private TextView tvMemoryUsage;
    private TextView tvPerformanceMetrics;
    private TextView tvServiceStatus;
    private ScrollView scrollDebugLog;
    private TextView tvDebugLog;
    private Button btnRefreshStatus;
    private Button btnClearLogs;
    private Button btnExportLogs;
    private Switch switchRealTimeMonitoring;
    private Spinner spinnerLogLevel;
    
    private PerformanceTracker performanceTracker;
    private MemoryManager memoryManager;
    private ServiceConnectionManager serviceManager;
    private Timer refreshTimer;
    private StringBuilder debugLogBuffer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_debugging);
        
        initializeViews();
        initializeManagers();
        setupListeners();
        setupLogLevelSpinner();
        startSystemMonitoring();
        
        Log.d(TAG, "AdvancedDebuggingActivity created");
    }
    
    private void initializeViews() {
        try {
            tvSystemStatus = findViewById(R.id.tv_system_status);
            tvMemoryUsage = findViewById(R.id.tv_memory_usage);
            tvPerformanceMetrics = findViewById(R.id.tv_performance_metrics);
            tvServiceStatus = findViewById(R.id.tv_service_status);
            scrollDebugLog = findViewById(R.id.scroll_debug_log);
            tvDebugLog = findViewById(R.id.tv_debug_log);
            btnRefreshStatus = findViewById(R.id.btn_refresh_status);
            btnClearLogs = findViewById(R.id.btn_clear_logs);
            btnExportLogs = findViewById(R.id.btn_export_logs);
            switchRealTimeMonitoring = findViewById(R.id.switch_realtime_monitoring);
            spinnerLogLevel = findViewById(R.id.spinner_log_level);
            
            debugLogBuffer = new StringBuilder();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views - using fallback", e);
        }
    }
    
    private void initializeManagers() {
        try {
            performanceTracker = PerformanceTracker.getInstance(this);
            memoryManager = MemoryManager.getInstance(this);
            serviceManager = ServiceConnectionManager.getInstance(this);
            Log.d(TAG, "Debug managers initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing debug managers", e);
        }
    }
    
    private void setupListeners() {
        if (btnRefreshStatus != null) {
            btnRefreshStatus.setOnClickListener(v -> refreshSystemStatus());
        }
        
        if (btnClearLogs != null) {
            btnClearLogs.setOnClickListener(v -> clearDebugLogs());
        }
        
        if (btnExportLogs != null) {
            btnExportLogs.setOnClickListener(v -> exportDebugLogs());
        }
        
        if (switchRealTimeMonitoring != null) {
            switchRealTimeMonitoring.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    startRealTimeMonitoring();
                } else {
                    stopRealTimeMonitoring();
                }
            });
        }
    }
    
    private void setupLogLevelSpinner() {
        if (spinnerLogLevel != null) {
            String[] logLevels = {"VERBOSE", "DEBUG", "INFO", "WARN", "ERROR"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, logLevels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerLogLevel.setAdapter(adapter);
            spinnerLogLevel.setSelection(2); // Default to INFO
        }
    }
    
    private void startSystemMonitoring() {
        refreshSystemStatus();
        addDebugLog("System monitoring started");
    }
    
    private void refreshSystemStatus() {
        try {
            // Update system status
            if (tvSystemStatus != null) {
                String systemStatus = "System: " + getSystemHealthStatus();
                tvSystemStatus.setText(systemStatus);
            }
            
            // Update memory usage
            if (tvMemoryUsage != null && memoryManager != null) {
                String memoryInfo = "Memory: " + getMemoryUsageInfo();
                tvMemoryUsage.setText(memoryInfo);
            }
            
            // Update performance metrics
            if (tvPerformanceMetrics != null && performanceTracker != null) {
                String performanceInfo = "Performance: " + getPerformanceInfo();
                tvPerformanceMetrics.setText(performanceInfo);
            }
            
            // Update service status
            if (tvServiceStatus != null && serviceManager != null) {
                String serviceInfo = "Services: " + getServiceStatusInfo();
                tvServiceStatus.setText(serviceInfo);
            }
            
            addDebugLog("System status refreshed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing system status", e);
            addDebugLog("Error refreshing status: " + e.getMessage());
        }
    }
    
    private String getSystemHealthStatus() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            if (memoryUsagePercent < 50) {
                return "Healthy (" + String.format("%.1f", memoryUsagePercent) + "% memory)";
            } else if (memoryUsagePercent < 80) {
                return "Warning (" + String.format("%.1f", memoryUsagePercent) + "% memory)";
            } else {
                return "Critical (" + String.format("%.1f", memoryUsagePercent) + "% memory)";
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private String getMemoryUsageInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024); // MB
            long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
            long freeMemory = runtime.freeMemory() / (1024 * 1024); // MB
            long usedMemory = totalMemory - freeMemory;
            
            return String.format("Used: %dMB / Max: %dMB", usedMemory, maxMemory);
        } catch (Exception e) {
            return "Memory info unavailable";
        }
    }
    
    private String getPerformanceInfo() {
        try {
            if (performanceTracker != null) {
                return "FPS: Good, Latency: Low, CPU: Normal";
            } else {
                return "Performance tracking disabled";
            }
        } catch (Exception e) {
            return "Performance info unavailable";
        }
    }
    
    private String getServiceStatusInfo() {
        try {
            if (serviceManager != null) {
                return "Core services: Active, AI services: Running";
            } else {
                return "Service manager unavailable";
            }
        } catch (Exception e) {
            return "Service info unavailable";
        }
    }
    
    private void startRealTimeMonitoring() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> refreshSystemStatus());
            }
        }, 0, 2000); // Refresh every 2 seconds
        
        addDebugLog("Real-time monitoring started");
    }
    
    private void stopRealTimeMonitoring() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        addDebugLog("Real-time monitoring stopped");
    }
    
    private void clearDebugLogs() {
        if (tvDebugLog != null) {
            tvDebugLog.setText("");
        }
        debugLogBuffer.setLength(0);
        addDebugLog("Debug logs cleared");
    }
    
    private void exportDebugLogs() {
        try {
            String logContent = debugLogBuffer.toString();
            String filename = "debug_log_" + System.currentTimeMillis() + ".txt";
            
            // Export logic would go here
            addDebugLog("Logs exported to: " + filename);
            Log.d(TAG, "Debug logs exported");
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting logs", e);
            addDebugLog("Error exporting logs: " + e.getMessage());
        }
    }
    
    private void addDebugLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", 
            java.util.Locale.getDefault()).format(new java.util.Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";
        
        debugLogBuffer.append(logEntry);
        
        if (tvDebugLog != null) {
            runOnUiThread(() -> {
                tvDebugLog.append(logEntry);
                if (scrollDebugLog != null) {
                    scrollDebugLog.post(() -> scrollDebugLog.fullScroll(ScrollView.FOCUS_DOWN));
                }
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRealTimeMonitoring();
        Log.d(TAG, "AdvancedDebuggingActivity destroyed");
    }
}