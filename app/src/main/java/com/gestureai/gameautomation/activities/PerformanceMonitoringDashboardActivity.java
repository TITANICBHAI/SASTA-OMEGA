package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;
import java.util.ArrayList;

import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.managers.PerformanceMonitoringManager;
import com.gestureai.gameautomation.managers.PerformanceMonitoringManager.PerformanceMetrics;
import com.gestureai.gameautomation.managers.PerformanceMonitoringManager.PerformanceSnapshot;
import com.gestureai.gameautomation.managers.PerformanceMonitoringManager.PerformanceAnalysis;
import com.gestureai.gameautomation.managers.PerformanceMonitoringManager.PerformanceUpdateListener;
import com.gestureai.gameautomation.managers.PerformanceMonitoringManager.VisualizationType;

public class PerformanceMonitoringDashboardActivity extends AppCompatActivity implements PerformanceUpdateListener {
    private static final String TAG = "PerformanceDashboard";
    
    private PerformanceMonitoringManager performanceManager;
    
    // UI Components
    private Button btnStartMonitoring, btnStopMonitoring, btnClearHistory, btnAnalyze;
    private TextView tvMonitoringStatus;
    
    // Real-time metrics display
    private TextView tvCPUUsage, tvMemoryUsage, tvCurrentFPS, tvTouchLatency;
    private TextView tvAIInferenceTime, tvActionExecutionTime, tvBatteryLevel;
    private ProgressBar pbCPU, pbMemory, pbFPS, pbLatency;
    
    // Trend indicators
    private TextView tvCPUTrend, tvMemoryTrend, tvFPSTrend, tvLatencyTrend;
    
    // Performance analysis
    private TextView tvPerformanceScore, tvRecommendations;
    private RecyclerView rvPerformanceHistory;
    
    // Debug visualization controls
    private Switch swTouchVisualization, swObjectDetection, swPerformanceOverlay;
    
    private PerformanceHistoryAdapter historyAdapter;
    private List<PerformanceSnapshot> performanceHistory;
    
    // Critical: Memory leak prevention
    private android.os.Handler uiUpdateHandler;
    private Runnable uiUpdateRunnable;
    private final java.util.concurrent.atomic.AtomicBoolean isDestroyed = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_monitoring_dashboard);
        
        initializeViews();
        initializePerformanceManager();
        setupEventListeners();
        initializeUIUpdates();
        updateUI();
        
        Log.d(TAG, "Performance Monitoring Dashboard initialized");
    }
    
    private void initializeViews() {
        // Control buttons
        btnStartMonitoring = findViewById(R.id.btn_start_monitoring);
        btnStopMonitoring = findViewById(R.id.btn_stop_monitoring);
        btnClearHistory = findViewById(R.id.btn_clear_history);
        btnAnalyze = findViewById(R.id.btn_analyze);
        tvMonitoringStatus = findViewById(R.id.tv_monitoring_status);
        
        // Real-time metrics
        tvCPUUsage = findViewById(R.id.tv_cpu_usage);
        tvMemoryUsage = findViewById(R.id.tv_memory_usage);
        tvCurrentFPS = findViewById(R.id.tv_current_fps);
        tvTouchLatency = findViewById(R.id.tv_touch_latency);
        tvAIInferenceTime = findViewById(R.id.tv_ai_inference_time);
        tvActionExecutionTime = findViewById(R.id.tv_action_execution_time);
        tvBatteryLevel = findViewById(R.id.tv_battery_level);
        
        // Progress bars
        pbCPU = findViewById(R.id.pb_cpu);
        pbMemory = findViewById(R.id.pb_memory);
        pbFPS = findViewById(R.id.pb_fps);
        pbLatency = findViewById(R.id.pb_latency);
        
        // Trend indicators
        tvCPUTrend = findViewById(R.id.tv_cpu_trend);
        tvMemoryTrend = findViewById(R.id.tv_memory_trend);
        tvFPSTrend = findViewById(R.id.tv_fps_trend);
        tvLatencyTrend = findViewById(R.id.tv_latency_trend);
        
        // Analysis
        tvPerformanceScore = findViewById(R.id.tv_performance_score);
        tvRecommendations = findViewById(R.id.tv_recommendations);
        
        // Debug visualization switches
        swTouchVisualization = findViewById(R.id.sw_touch_visualization);
        swObjectDetection = findViewById(R.id.sw_object_detection);
        swPerformanceOverlay = findViewById(R.id.sw_performance_overlay);
        
        // Performance history
        rvPerformanceHistory = findViewById(R.id.rv_performance_history);
        performanceHistory = new ArrayList<>();
        historyAdapter = new PerformanceHistoryAdapter(performanceHistory);
        rvPerformanceHistory.setLayoutManager(new LinearLayoutManager(this));
        rvPerformanceHistory.setAdapter(historyAdapter);
    }
    
    private void initializePerformanceManager() {
        performanceManager = PerformanceMonitoringManager.getInstance(this);
        performanceManager.addPerformanceUpdateListener(this);
    }
    
    private void setupEventListeners() {
        btnStartMonitoring.setOnClickListener(v -> {
            performanceManager.startMonitoring();
            updateUI();
        });
        
        btnStopMonitoring.setOnClickListener(v -> {
            performanceManager.stopMonitoring();
            updateUI();
        });
        
        btnClearHistory.setOnClickListener(v -> {
            performanceManager.clearHistory();
            performanceHistory.clear();
            historyAdapter.notifyDataSetChanged();
            updateUI();
        });
        
        btnAnalyze.setOnClickListener(v -> performAnalysis());
        
        // Debug visualization switches
        swTouchVisualization.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                performanceManager.enableVisualization(VisualizationType.TOUCH_POINTS);
            } else {
                performanceManager.disableVisualization(VisualizationType.TOUCH_POINTS);
            }
        });
        
        swObjectDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                performanceManager.enableVisualization(VisualizationType.OBJECT_DETECTION);
            } else {
                performanceManager.disableVisualization(VisualizationType.OBJECT_DETECTION);
            }
        });
        
        swPerformanceOverlay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                performanceManager.enableVisualization(VisualizationType.PERFORMANCE_OVERLAY);
            } else {
                performanceManager.disableVisualization(VisualizationType.PERFORMANCE_OVERLAY);
            }
        });
    }
    
    private void updateUI() {
        runOnUiThread(() -> {
            boolean isMonitoring = performanceManager.isMonitoring();
            
            // Update button states
            btnStartMonitoring.setEnabled(!isMonitoring);
            btnStopMonitoring.setEnabled(isMonitoring);
            
            // Update monitoring status
            tvMonitoringStatus.setText(isMonitoring ? "MONITORING ACTIVE" : "MONITORING STOPPED");
            tvMonitoringStatus.setTextColor(getResources().getColor(
                isMonitoring ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            
            // Update metrics if monitoring
            if (isMonitoring) {
                updateMetricsDisplay();
            }
            
            // Update visualization switch states
            updateVisualizationSwitches();
        });
    }
    
    private void updateMetricsDisplay() {
        PerformanceMetrics metrics = performanceManager.getCurrentMetrics();
        
        if (metrics != null) {
            // CPU
            tvCPUUsage.setText(String.format("%.1f%%", metrics.cpuUsage));
            pbCPU.setProgress((int) metrics.cpuUsage);
            updateTrendIndicator(tvCPUTrend, metrics.cpuTrend);
            
            // Memory
            tvMemoryUsage.setText(String.format("%.1f%%", metrics.memoryUsage));
            pbMemory.setProgress((int) metrics.memoryUsage);
            updateTrendIndicator(tvMemoryTrend, metrics.memoryTrend);
            
            // FPS
            tvCurrentFPS.setText(String.format("%.1f FPS", metrics.currentFPS));
            pbFPS.setProgress((int) Math.min(100, (metrics.currentFPS / 60f) * 100));
            updateTrendIndicator(tvFPSTrend, metrics.fpsTrend);
            
            // Touch Latency
            tvTouchLatency.setText(String.format("%d ms", metrics.touchLatency));
            pbLatency.setProgress((int) Math.min(100, (metrics.touchLatency / 200f) * 100));
            updateTrendIndicator(tvLatencyTrend, metrics.latencyTrend);
            
            // AI Performance
            tvAIInferenceTime.setText(String.format("%d ms", metrics.aiInferenceTime));
            tvActionExecutionTime.setText(String.format("%d ms", metrics.actionExecutionTime));
            
            // Battery
            tvBatteryLevel.setText(String.format("%d%%", metrics.batteryLevel));
        }
    }
    
    private void updateTrendIndicator(TextView trendView, float trend) {
        if (Math.abs(trend) < 0.1f) {
            trendView.setText("→");
            trendView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else if (trend > 0) {
            trendView.setText("↑");
            trendView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            trendView.setText("↓");
            trendView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }
    
    private void updateVisualizationSwitches() {
        swTouchVisualization.setChecked(
            performanceManager.isVisualizationEnabled(VisualizationType.TOUCH_POINTS));
        swObjectDetection.setChecked(
            performanceManager.isVisualizationEnabled(VisualizationType.OBJECT_DETECTION));
        swPerformanceOverlay.setChecked(
            performanceManager.isVisualizationEnabled(VisualizationType.PERFORMANCE_OVERLAY));
    }
    
    private void performAnalysis() {
        try {
            PerformanceAnalysis analysis = performanceManager.analyzePerformance();
            
            // Update performance score
            tvPerformanceScore.setText(String.format("Performance Score: %.1f/100", analysis.performanceScore));
            
            // Color code the score
            int scoreColor;
            if (analysis.performanceScore >= 80f) {
                scoreColor = android.R.color.holo_green_dark;
            } else if (analysis.performanceScore >= 60f) {
                scoreColor = android.R.color.holo_orange_dark;
            } else {
                scoreColor = android.R.color.holo_red_dark;
            }
            tvPerformanceScore.setTextColor(getResources().getColor(scoreColor));
            
            // Update recommendations
            StringBuilder recommendations = new StringBuilder();
            recommendations.append("Recommendations:\n");
            for (String recommendation : analysis.recommendations) {
                recommendations.append("• ").append(recommendation).append("\n");
            }
            tvRecommendations.setText(recommendations.toString());
            
            Log.d(TAG, "Performance analysis completed. Score: " + analysis.performanceScore);
            
        } catch (Exception e) {
            Log.e(TAG, "Error performing analysis", e);
            Toast.makeText(this, "Error analyzing performance", Toast.LENGTH_SHORT).show();
        }
    }
    
    // PerformanceUpdateListener implementation
    @Override
    public void onPerformanceUpdate(PerformanceSnapshot snapshot) {
        runOnUiThread(() -> {
            // Add to history
            performanceHistory.add(snapshot);
            
            // Maintain history size (keep last 100 entries)
            while (performanceHistory.size() > 100) {
                performanceHistory.remove(0);
            }
            
            // Update adapter
            historyAdapter.notifyDataSetChanged();
            
            // Update real-time display
            updateMetricsDisplay();
        });
    }
    
    @Override
    public void onMonitoringStateChanged(boolean isActive) {
        runOnUiThread(() -> {
            updateUI();
            String message = isActive ? "Performance monitoring started" : "Performance monitoring stopped";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void initializeUIUpdates() {
        uiUpdateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isDestroyed.get() && performanceManager != null && performanceManager.isMonitoring()) {
                    updateMetricsDisplay();
                    uiUpdateHandler.postDelayed(this, 1000); // Update every second
                }
            }
        };
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Critical: Prevent memory leaks during activity destruction
        isDestroyed.set(true);
        
        // Clean up UI update handler
        if (uiUpdateHandler != null) {
            uiUpdateHandler.removeCallbacksAndMessages(null);
            if (uiUpdateRunnable != null) {
                uiUpdateHandler.removeCallbacks(uiUpdateRunnable);
                uiUpdateRunnable = null;
            }
            uiUpdateHandler = null;
        }
        
        // Clean up performance manager listener
        if (performanceManager != null) {
            performanceManager.removePerformanceUpdateListener(this);
            performanceManager = null;
        }
        
        // Clear adapter reference
        historyAdapter = null;
        performanceHistory = null;
        
        Log.d(TAG, "PerformanceMonitoringDashboardActivity destroyed - memory cleaned up");
    }
    
    // Performance History Adapter
    private static class PerformanceHistoryAdapter extends RecyclerView.Adapter<PerformanceHistoryAdapter.ViewHolder> {
        private final List<PerformanceSnapshot> snapshots;
        
        public PerformanceHistoryAdapter(List<PerformanceSnapshot> snapshots) {
            this.snapshots = snapshots;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_performance_snapshot, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PerformanceSnapshot snapshot = snapshots.get(position);
            holder.bind(snapshot);
        }
        
        @Override
        public int getItemCount() {
            return snapshots.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvTimestamp;
            private final TextView tvCPU;
            private final TextView tvMemory;
            private final TextView tvFPS;
            private final TextView tvLatency;
            
            public ViewHolder(View itemView) {
                super(itemView);
                tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
                tvCPU = itemView.findViewById(R.id.tv_cpu);
                tvMemory = itemView.findViewById(R.id.tv_memory);
                tvFPS = itemView.findViewById(R.id.tv_fps);
                tvLatency = itemView.findViewById(R.id.tv_latency);
            }
            
            public void bind(PerformanceSnapshot snapshot) {
                tvTimestamp.setText(formatTimestamp(snapshot.timestamp));
                tvCPU.setText(String.format("%.1f%%", snapshot.cpuUsage));
                tvMemory.setText(String.format("%.1f%%", snapshot.memoryUsage));
                tvFPS.setText(String.format("%.1f", snapshot.fps));
                tvLatency.setText(String.format("%dms", snapshot.touchLatency));
            }
            
            private String formatTimestamp(long timestamp) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
                return sdf.format(new java.util.Date(timestamp));
            }
        }
    }
}