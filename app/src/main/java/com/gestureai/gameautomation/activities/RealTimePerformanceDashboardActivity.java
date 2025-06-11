package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.PerformanceTracker;
import com.gestureai.gameautomation.ai.ResourceMonitor;
import com.gestureai.gameautomation.GameAutomationEngine;
import java.util.List;
import java.util.ArrayList;

public class RealTimePerformanceDashboardActivity extends AppCompatActivity {
    private static final String TAG = "PerformanceDashboard";
    
    // Real-time metrics display
    private TextView tvCurrentFPS;
    private TextView tvTargetFPS;
    private TextView tvCPUUsage;
    private TextView tvMemoryUsage;
    private TextView tvBatteryLevel;
    private TextView tvTouchLatency;
    private TextView tvAIProcessingTime;
    private TextView tvScreenCaptureRate;
    
    // Progress indicators
    private ProgressBar pbCPUUsage;
    private ProgressBar pbMemoryUsage;
    private ProgressBar pbBatteryLevel;
    private ProgressBar pbPerformanceScore;
    
    // Performance history
    private RecyclerView rvPerformanceHistory;
    private PerformanceHistoryAdapter adapter;
    
    // Monitoring components
    private PerformanceTracker performanceTracker;
    private ResourceMonitor resourceMonitor;
    private Handler updateHandler;
    private Runnable updateRunnable;
    
    // Performance data
    private List<PerformanceSnapshot> performanceHistory;
    private boolean isMonitoring = false;
    
    // Critical: Memory leak prevention
    private final java.util.concurrent.atomic.AtomicBoolean isDestroyed = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_performance_dashboard);
        
        initializeViews();
        initializeMonitoring();
        startRealTimeUpdates();
    }
    
    private void initializeViews() {
        // Real-time metrics
        tvCurrentFPS = findViewById(R.id.tv_current_fps);
        tvTargetFPS = findViewById(R.id.tv_target_fps);
        tvCPUUsage = findViewById(R.id.tv_cpu_usage);
        tvMemoryUsage = findViewById(R.id.tv_memory_usage);
        tvBatteryLevel = findViewById(R.id.tv_battery_level);
        tvTouchLatency = findViewById(R.id.tv_touch_latency);
        tvAIProcessingTime = findViewById(R.id.tv_ai_processing_time);
        tvScreenCaptureRate = findViewById(R.id.tv_screen_capture_rate);
        
        // Progress bars
        pbCPUUsage = findViewById(R.id.pb_cpu_usage);
        pbMemoryUsage = findViewById(R.id.pb_memory_usage);
        pbBatteryLevel = findViewById(R.id.pb_battery_level);
        pbPerformanceScore = findViewById(R.id.pb_performance_score);
        
        // Performance history
        rvPerformanceHistory = findViewById(R.id.rv_performance_history);
        rvPerformanceHistory.setLayoutManager(new LinearLayoutManager(this));
        
        performanceHistory = new ArrayList<>();
        adapter = new PerformanceHistoryAdapter(performanceHistory);
        rvPerformanceHistory.setAdapter(adapter);
    }
    
    private void initializeMonitoring() {
        performanceTracker = new PerformanceTracker(this);
        resourceMonitor = new ResourceMonitor(this);
        
        updateHandler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updatePerformanceMetrics();
                updateHandler.postDelayed(this, 1000); // Update every second
            }
        };
    }
    
    private void startRealTimeUpdates() {
        isMonitoring = true;
        performanceTracker.startMonitoring();
        resourceMonitor.startMonitoring();
        
        // Critical: Use weak reference to prevent activity retention
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isDestroyed.get() && isMonitoring) {
                    updatePerformanceMetrics();
                    updateHandler.postDelayed(this, 1000); // Update every second
                }
            }
        };
        updateHandler.post(updateRunnable);
    }
    
    private void updatePerformanceMetrics() {
        if (!isMonitoring) return;
        
        // Get current performance data
        PerformanceTracker.PerformanceData perfData = performanceTracker.getCurrentPerformance();
        ResourceMonitor.SystemResources sysRes = resourceMonitor.getCurrentResources();
        
        // Update FPS metrics
        tvCurrentFPS.setText(String.format("%.1f FPS", perfData.currentFPS));
        tvTargetFPS.setText(String.format("Target: %.0f FPS", perfData.targetFPS));
        
        // Update system resources
        tvCPUUsage.setText(String.format("%.1f%%", sysRes.cpuUsage));
        tvMemoryUsage.setText(String.format("%.1f MB", sysRes.memoryUsage));
        tvBatteryLevel.setText(String.format("%.0f%%", sysRes.batteryLevel));
        
        // Update automation-specific metrics
        tvTouchLatency.setText(String.format("%.0f ms", perfData.touchLatency));
        tvAIProcessingTime.setText(String.format("%.0f ms", perfData.aiProcessingTime));
        tvScreenCaptureRate.setText(String.format("%.1f Hz", perfData.screenCaptureRate));
        
        // Update progress bars
        pbCPUUsage.setProgress((int) sysRes.cpuUsage);
        pbMemoryUsage.setProgress((int) (sysRes.memoryUsage / 10)); // Scale for display
        pbBatteryLevel.setProgress((int) sysRes.batteryLevel);
        
        // Calculate overall performance score
        float performanceScore = calculatePerformanceScore(perfData, sysRes);
        pbPerformanceScore.setProgress((int) (performanceScore * 100));
        
        // Add to history
        PerformanceSnapshot snapshot = new PerformanceSnapshot(
            System.currentTimeMillis(),
            perfData.currentFPS,
            sysRes.cpuUsage,
            sysRes.memoryUsage,
            perfData.touchLatency,
            performanceScore
        );
        
        performanceHistory.add(0, snapshot);
        if (performanceHistory.size() > 100) {
            performanceHistory.remove(performanceHistory.size() - 1);
        }
        
        adapter.notifyDataSetChanged();
    }
    
    private float calculatePerformanceScore(PerformanceTracker.PerformanceData perfData, 
                                          ResourceMonitor.SystemResources sysRes) {
        float fpsScore = Math.min(1.0f, perfData.currentFPS / perfData.targetFPS);
        float cpuScore = Math.max(0.0f, 1.0f - (sysRes.cpuUsage / 100.0f));
        float memoryScore = Math.max(0.0f, 1.0f - (sysRes.memoryUsage / 1000.0f));
        float latencyScore = Math.max(0.0f, 1.0f - (perfData.touchLatency / 100.0f));
        
        return (fpsScore + cpuScore + memoryScore + latencyScore) / 4.0f;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Critical: Prevent memory leaks during activity destruction
        isDestroyed.set(true);
        isMonitoring = false;
        
        // Clean up handler and runnables
        if (updateHandler != null) {
            updateHandler.removeCallbacksAndMessages(null);
            if (updateRunnable != null) {
                updateHandler.removeCallbacks(updateRunnable);
                updateRunnable = null;
            }
            updateHandler = null;
        }
        
        // Stop monitoring components
        if (performanceTracker != null) {
            performanceTracker.stopMonitoring();
            performanceTracker = null;
        }
        if (resourceMonitor != null) {
            resourceMonitor.stopMonitoring();
            resourceMonitor = null;
        }
        
        // Clear adapter and data references
        adapter = null;
        performanceHistory = null;
        
        android.util.Log.d(TAG, "RealTimePerformanceDashboardActivity destroyed - memory cleaned up");
    }
    
    // Performance snapshot data class
    public static class PerformanceSnapshot {
        public long timestamp;
        public float fps;
        public float cpuUsage;
        public float memoryUsage;
        public float touchLatency;
        public float performanceScore;
        
        public PerformanceSnapshot(long timestamp, float fps, float cpuUsage, 
                                 float memoryUsage, float touchLatency, float performanceScore) {
            this.timestamp = timestamp;
            this.fps = fps;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.touchLatency = touchLatency;
            this.performanceScore = performanceScore;
        }
    }
    
    // RecyclerView adapter for performance history
    private class PerformanceHistoryAdapter extends RecyclerView.Adapter<PerformanceHistoryAdapter.ViewHolder> {
        private List<PerformanceSnapshot> data;
        
        public PerformanceHistoryAdapter(List<PerformanceSnapshot> data) {
            this.data = data;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_performance_snapshot, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PerformanceSnapshot snapshot = data.get(position);
            
            holder.tvTimestamp.setText(formatTimestamp(snapshot.timestamp));
            holder.tvFPS.setText(String.format("%.1f", snapshot.fps));
            holder.tvCPU.setText(String.format("%.1f%%", snapshot.cpuUsage));
            holder.tvMemory.setText(String.format("%.0f MB", snapshot.memoryUsage));
            holder.tvLatency.setText(String.format("%.0f ms", snapshot.touchLatency));
            holder.tvScore.setText(String.format("%.0f%%", snapshot.performanceScore * 100));
        }
        
        @Override
        public int getItemCount() {
            return data.size();
        }
        
        private String formatTimestamp(long timestamp) {
            long secondsAgo = (System.currentTimeMillis() - timestamp) / 1000;
            return secondsAgo + "s ago";
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTimestamp, tvFPS, tvCPU, tvMemory, tvLatency, tvScore;
            
            public ViewHolder(android.view.View itemView) {
                super(itemView);
                tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
                tvFPS = itemView.findViewById(R.id.tv_fps);
                tvCPU = itemView.findViewById(R.id.tv_cpu);
                tvMemory = itemView.findViewById(R.id.tv_memory);
                tvLatency = itemView.findViewById(R.id.tv_latency);
                tvScore = itemView.findViewById(R.id.tv_score);
            }
        }
    }
}