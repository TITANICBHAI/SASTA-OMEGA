package com.gestureai.gameautomation.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.analytics.PerformanceTracker;
import com.gestureai.gameautomation.analytics.SessionData;
import com.gestureai.gameautomation.analytics.ResourceMonitor;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.ArrayList;
import java.util.List;

/**
 * Real-time analytics dashboard showing performance metrics,
 * success rates, and system resource usage during automation
 */
public class AnalyticsFragment extends Fragment {
    
    // UI Components
    private TextView tvSuccessRate;
    private TextView tvAverageReactionTime;
    private TextView tvTotalActions;
    private TextView tvCurrentFPS;
    private TextView tvMemoryUsage;
    private TextView tvCPUUsage;
    private LineChart chartPerformance;
    private PieChart chartActionDistribution;
    private RecyclerView rvSessionHistory;
    private Button btnExportAnalytics;
    private Button btnClearData;
    private Button btnStartMonitoring;
    private Button btnStopMonitoring;
    
    // Backend Integration
    private PerformanceTracker performanceTracker;
    private ResourceMonitor resourceMonitor;
    private SessionAnalyticsAdapter sessionsAdapter;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private volatile boolean isMonitoring = false;
    private volatile boolean isDestroyed = false;
    
    // Database query management
    private final java.util.concurrent.ExecutorService queryExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.Future<?>> activeQueries = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Real-time data
    private List<Float> fpsHistory = new ArrayList<>();
    private List<Float> reactionTimeHistory = new ArrayList<>();
    private List<SessionData> sessionHistory = new ArrayList<>();
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);
        
        initializeViews(view);
        setupCharts();
        setupListeners();
        initializeBackend();
        startRealTimeUpdates();
        
        return view;
    }
    
    private void initializeViews(View view) {
        if (view == null || isDestroyed) {
            android.util.Log.w("AnalyticsFragment", "Cannot initialize views - view is null or fragment destroyed");
            return;
        }
        
        try {
            // Performance metrics
            tvSuccessRate = view.findViewById(R.id.tv_success_rate);
            tvAverageReactionTime = view.findViewById(R.id.tv_avg_reaction_time);
            tvTotalActions = view.findViewById(R.id.tv_total_actions);
            tvCurrentFPS = view.findViewById(R.id.tv_current_fps);
            tvMemoryUsage = view.findViewById(R.id.tv_memory_usage);
            tvCPUUsage = view.findViewById(R.id.tv_cpu_usage);
            
            // Charts
            chartPerformance = view.findViewById(R.id.chart_performance);
            chartActionDistribution = view.findViewById(R.id.chart_action_distribution);
            
            // Session history
            rvSessionHistory = view.findViewById(R.id.rv_session_history);
            if (rvSessionHistory != null && getContext() != null) {
                rvSessionHistory.setLayoutManager(new LinearLayoutManager(getContext()));
            }
            
            // Control buttons
            btnExportAnalytics = view.findViewById(R.id.btn_export_analytics);
            btnClearData = view.findViewById(R.id.btn_clear_data);
            btnStartMonitoring = view.findViewById(R.id.btn_start_monitoring);
            btnStopMonitoring = view.findViewById(R.id.btn_stop_monitoring);
            
            // Validate critical views
            if (btnStartMonitoring == null || btnStopMonitoring == null) {
                throw new IllegalStateException("Critical control buttons missing from layout");
            }
            
        } catch (Exception e) {
            android.util.Log.e("AnalyticsFragment", "Error initializing views", e);
            isDestroyed = true;
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isDestroyed = true;
        stopRealTimeUpdates();
        
        // Cancel all active database queries to prevent leaks
        for (java.util.concurrent.Future<?> query : activeQueries.values()) {
            if (query != null && !query.isDone()) {
                query.cancel(true);
            }
        }
        activeQueries.clear();
        
        // Shutdown query executor
        if (queryExecutor != null && !queryExecutor.isShutdown()) {
            queryExecutor.shutdown();
            try {
                if (!queryExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    queryExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                queryExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Clean up handlers
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        
        // Clear view references
        tvSuccessRate = null;
        tvAverageReactionTime = null;
        tvTotalActions = null;
        tvCurrentFPS = null;
        tvMemoryUsage = null;
        tvCPUUsage = null;
        chartPerformance = null;
        chartActionDistribution = null;
        rvSessionHistory = null;
        btnExportAnalytics = null;
        btnClearData = null;
        btnStartMonitoring = null;
        btnStopMonitoring = null;
        
        // Clear data lists
        if (fpsHistory != null) fpsHistory.clear();
        if (reactionTimeHistory != null) reactionTimeHistory.clear();
        if (sessionHistory != null) sessionHistory.clear();
        
        // Clear backend references
        performanceTracker = null;
        resourceMonitor = null;
        sessionsAdapter = null;
        updateHandler = null;
        updateRunnable = null;
    }
    
    private void stopRealTimeUpdates() {
        isMonitoring = false;
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
    
    private void initializeBackend() {
        performanceTracker = new PerformanceTracker(getContext());
        resourceMonitor = new ResourceMonitor(getContext());
        
        // Setup session adapter
        sessionsAdapter = new SessionAnalyticsAdapter(sessionHistory);
        rvSessionHistory.setAdapter(sessionsAdapter);
        
        // Load existing session data
        loadSessionHistory();
    }
    
    private void setupCharts() {
        // Setup performance line chart
        chartPerformance.getDescription().setEnabled(false);
        chartPerformance.setTouchEnabled(true);
        chartPerformance.setDragEnabled(true);
        chartPerformance.setScaleEnabled(true);
        chartPerformance.getXAxis().setGranularity(1f);
        
        // Setup action distribution pie chart
        chartActionDistribution.getDescription().setEnabled(false);
        chartActionDistribution.setUsePercentValues(true);
        chartActionDistribution.setEntryLabelTextSize(12f);
        chartActionDistribution.setEntryLabelColor(Color.BLACK);
    }
    
    private void setupListeners() {
        btnStartMonitoring.setOnClickListener(v -> startMonitoring());
        btnStopMonitoring.setOnClickListener(v -> stopMonitoring());
        btnExportAnalytics.setOnClickListener(v -> exportAnalyticsData());
        btnClearData.setOnClickListener(v -> clearAnalyticsData());
    }
    
    private void startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true;
            performanceTracker.startTracking();
            resourceMonitor.startMonitoring();
            
            btnStartMonitoring.setEnabled(false);
            btnStopMonitoring.setEnabled(true);
            
            android.widget.Toast.makeText(getContext(), "Analytics monitoring started", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false;
            performanceTracker.stopTracking();
            resourceMonitor.stopMonitoring();
            
            btnStartMonitoring.setEnabled(true);
            btnStopMonitoring.setEnabled(false);
            
            // Save current session
            saveCurrentSession();
            
            android.widget.Toast.makeText(getContext(), "Analytics monitoring stopped", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startRealTimeUpdates() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMonitoring) {
                    updateMetrics();
                    updateCharts();
                }
                updateHandler.postDelayed(this, 1000); // Update every second
            }
        };
        updateHandler.post(updateRunnable);
    }
    
    private void updateMetrics() {
        // Get current performance data
        PerformanceTracker.PerformanceData perfData = performanceTracker.getCurrentPerformance();
        ResourceMonitor.ResourceData resourceData = resourceMonitor.getCurrentResources();
        
        // Update performance metrics
        tvSuccessRate.setText(String.format("%.1f%%", perfData.successRate * 100));
        tvAverageReactionTime.setText(String.format("%.0f ms", perfData.averageReactionTime));
        tvTotalActions.setText(String.valueOf(perfData.totalActions));
        tvCurrentFPS.setText(String.format("%.1f FPS", perfData.currentFPS));
        
        // Update resource metrics
        tvMemoryUsage.setText(String.format("%.1f MB", resourceData.memoryUsageMB));
        tvCPUUsage.setText(String.format("%.1f%%", resourceData.cpuUsagePercent));
        
        // Store data for charts
        fpsHistory.add(perfData.currentFPS);
        reactionTimeHistory.add(perfData.averageReactionTime);
        
        // Keep only last 60 data points (1 minute of data)
        if (fpsHistory.size() > 60) {
            fpsHistory.remove(0);
            reactionTimeHistory.remove(0);
        }
    }
    
    private void updateCharts() {
        updatePerformanceChart();
        updateActionDistributionChart();
    }
    
    private void updatePerformanceChart() {
        List<Entry> fpsEntries = new ArrayList<>();
        List<Entry> reactionEntries = new ArrayList<>();
        
        for (int i = 0; i < fpsHistory.size(); i++) {
            fpsEntries.add(new Entry(i, fpsHistory.get(i)));
            reactionEntries.add(new Entry(i, reactionTimeHistory.get(i) / 10f)); // Scale for visibility
        }
        
        LineDataSet fpsDataSet = new LineDataSet(fpsEntries, "FPS");
        fpsDataSet.setColor(Color.BLUE);
        fpsDataSet.setValueTextColor(Color.BLUE);
        
        LineDataSet reactionDataSet = new LineDataSet(reactionEntries, "Reaction Time (x10ms)");
        reactionDataSet.setColor(Color.RED);
        reactionDataSet.setValueTextColor(Color.RED);
        
        LineData lineData = new LineData(fpsDataSet, reactionDataSet);
        chartPerformance.setData(lineData);
        chartPerformance.invalidate(); // Refresh chart
    }
    
    private void updateActionDistributionChart() {
        PerformanceTracker.ActionStats actionStats = performanceTracker.getActionStatistics();
        
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(actionStats.tapCount, "Taps"));
        entries.add(new PieEntry(actionStats.swipeCount, "Swipes"));
        entries.add(new PieEntry(actionStats.longPressCount, "Long Press"));
        entries.add(new PieEntry(actionStats.gestureCount, "Gestures"));
        
        PieDataSet dataSet = new PieDataSet(entries, "Action Distribution");
        dataSet.setColors(new int[]{Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED});
        
        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);
        
        chartActionDistribution.setData(data);
        chartActionDistribution.invalidate();
    }
    
    private void loadSessionHistory() {
        // Load previous sessions from PerformanceTracker
        List<SessionData> sessions = performanceTracker.getSessionHistory();
        sessionHistory.clear();
        sessionHistory.addAll(sessions);
        sessionsAdapter.notifyDataSetChanged();
    }
    
    private void saveCurrentSession() {
        if (performanceTracker != null) {
            SessionData currentSession = performanceTracker.getCurrentSession();
            sessionHistory.add(0, currentSession); // Add to beginning
            sessionsAdapter.notifyItemInserted(0);
            
            // Keep only last 50 sessions
            if (sessionHistory.size() > 50) {
                sessionHistory.remove(sessionHistory.size() - 1);
                sessionsAdapter.notifyItemRemoved(sessionHistory.size());
            }
        }
    }
    
    private void exportAnalyticsData() {
        performanceTracker.exportAnalyticsData(new PerformanceTracker.ExportCallback() {
            @Override
            public void onExportComplete(String filePath) {
                getActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(getContext(), 
                        "Analytics exported to: " + filePath, 
                        android.widget.Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onExportError(String error) {
                getActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(getContext(), 
                        "Export failed: " + error, 
                        android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void clearAnalyticsData() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
            .setTitle("Clear Analytics Data")
            .setMessage("Are you sure you want to clear all analytics data? This cannot be undone.")
            .setPositiveButton("Clear", (dialog, which) -> {
                performanceTracker.clearAllData();
                sessionHistory.clear();
                fpsHistory.clear();
                reactionTimeHistory.clear();
                sessionsAdapter.notifyDataSetChanged();
                
                // Reset UI
                tvSuccessRate.setText("0.0%");
                tvAverageReactionTime.setText("0 ms");
                tvTotalActions.setText("0");
                tvCurrentFPS.setText("0.0 FPS");
                
                chartPerformance.clear();
                chartActionDistribution.clear();
                
                android.widget.Toast.makeText(getContext(), "Analytics data cleared", android.widget.Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Get real-time analytics summary for other components
     */
    public AnalyticsSummary getCurrentAnalytics() {
        if (performanceTracker == null) return new AnalyticsSummary();
        
        PerformanceTracker.PerformanceData perfData = performanceTracker.getCurrentPerformance();
        ResourceMonitor.ResourceData resourceData = resourceMonitor.getCurrentResources();
        
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.successRate = perfData.successRate;
        summary.averageReactionTime = perfData.averageReactionTime;
        summary.currentFPS = perfData.currentFPS;
        summary.totalActions = perfData.totalActions;
        summary.memoryUsage = resourceData.memoryUsageMB;
        summary.cpuUsage = resourceData.cpuUsagePercent;
        summary.isMonitoring = isMonitoring;
        
        return summary;
    }
    
    /**
     * Analytics summary data class
     */
    public static class AnalyticsSummary {
        public float successRate;
        public float averageReactionTime;
        public float currentFPS;
        public int totalActions;
        public float memoryUsage;
        public float cpuUsage;
        public boolean isMonitoring;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        if (isMonitoring) {
            stopMonitoring();
        }
    }
    
    /**
     * Adapter for session history RecyclerView
     */
    private static class SessionAnalyticsAdapter extends RecyclerView.Adapter<SessionAnalyticsAdapter.ViewHolder> {
        private List<SessionData> sessions;
        
        public SessionAnalyticsAdapter(List<SessionData> sessions) {
            this.sessions = sessions;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_analytics, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SessionData session = sessions.get(position);
            holder.bind(session);
        }
        
        @Override
        public int getItemCount() {
            return sessions.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSessionDate;
            TextView tvSessionDuration;
            TextView tvSessionActions;
            TextView tvSessionSuccessRate;
            
            public ViewHolder(View itemView) {
                super(itemView);
                tvSessionDate = itemView.findViewById(R.id.tv_session_date);
                tvSessionDuration = itemView.findViewById(R.id.tv_session_duration);
                tvSessionActions = itemView.findViewById(R.id.tv_session_actions);
                tvSessionSuccessRate = itemView.findViewById(R.id.tv_session_success_rate);
            }
            
            public void bind(SessionData session) {
                tvSessionDate.setText(session.getFormattedDate());
                tvSessionDuration.setText(session.getFormattedDuration());
                tvSessionActions.setText(String.valueOf(session.totalActions));
                tvSessionSuccessRate.setText(String.format("%.1f%%", session.successRate * 100));
            }
        }
    }
}