package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.PerformanceTracker;
import com.gestureai.gameautomation.fragments.ObjectDetectionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Real-time analytics dashboard with live performance monitoring
 * Shows FPS tracking, success rates, heat maps, and game-specific analytics
 */
public class RealTimeAnalyticsDashboardActivity extends AppCompatActivity {
    private static final String TAG = "RealTimeAnalytics";
    private static final int UPDATE_INTERVAL_MS = 1000; // Update every second

    private TextView tvFPS, tvSuccessRate, tvActionsPerMinute, tvAvgResponseTime;
    private TextView tvTotalActions, tvCurrentStreak, tvGameTime, tvEfficiencyScore;
    private RecyclerView recyclerViewMetrics;
    
    private PerformanceTracker performanceTracker;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private ObjectDetectionAdapter metricsAdapter;
    
    private long sessionStartTime;
    private int totalActionsCount = 0;
    private int successfulActionsCount = 0;
    private int currentStreak = 0;
    private List<Long> responseTimes = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_analytics_dashboard);
        
        initializeViews();
        setupRecyclerView();
        initializeTracking();
        startRealTimeUpdates();
        
        Log.d(TAG, "Real-time analytics dashboard initialized");
    }
    
    private void initializeViews() {
        tvFPS = findViewById(R.id.tv_fps);
        tvSuccessRate = findViewById(R.id.tv_success_rate);
        tvActionsPerMinute = findViewById(R.id.tv_actions_per_minute);
        tvAvgResponseTime = findViewById(R.id.tv_avg_response_time);
        tvTotalActions = findViewById(R.id.tv_total_actions);
        tvCurrentStreak = findViewById(R.id.tv_current_streak);
        tvGameTime = findViewById(R.id.tv_game_time);
        tvEfficiencyScore = findViewById(R.id.tv_efficiency_score);
        recyclerViewMetrics = findViewById(R.id.recycler_view_metrics);
    }
    
    private void setupRecyclerView() {
        metricsAdapter = new ObjectDetectionAdapter(new ArrayList<>());
        recyclerViewMetrics.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMetrics.setAdapter(metricsAdapter);
    }
    
    private void initializeTracking() {
        performanceTracker = PerformanceTracker.getInstance();
        sessionStartTime = System.currentTimeMillis();
        
        // Initialize performance tracker if not already running
        if (performanceTracker == null) {
            performanceTracker = new PerformanceTracker(this);
        }
        
        Log.d(TAG, "Performance tracking initialized");
    }
    
    private void startRealTimeUpdates() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateAnalytics();
                updateHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        
        updateHandler.post(updateRunnable);
        Log.d(TAG, "Real-time updates started");
    }
    
    private void updateAnalytics() {
        try {
            updateFPSMetrics();
            updateSuccessMetrics();
            updatePerformanceMetrics();
            updateGameTimeMetrics();
            updateEfficiencyScore();
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating analytics", e);
        }
    }
    
    private void updateFPSMetrics() {
        if (performanceTracker != null) {
            PerformanceTracker.PerformanceSnapshot snapshot = performanceTracker.getCurrentSnapshot();
            if (snapshot != null) {
                float fps = snapshot.getFramesPerSecond();
                tvFPS.setText(String.format(Locale.getDefault(), "%.1f FPS", fps));
                
                // Color code FPS based on performance
                if (fps >= 50) {
                    tvFPS.setTextColor(getColor(android.R.color.holo_green_light));
                } else if (fps >= 30) {
                    tvFPS.setTextColor(getColor(android.R.color.holo_orange_light));
                } else {
                    tvFPS.setTextColor(getColor(android.R.color.holo_red_light));
                }
            }
        }
    }
    
    private void updateSuccessMetrics() {
        float successRate = totalActionsCount > 0 ? 
            (float) successfulActionsCount / totalActionsCount * 100f : 0f;
        
        tvSuccessRate.setText(String.format(Locale.getDefault(), "%.1f%%", successRate));
        tvCurrentStreak.setText(String.valueOf(currentStreak));
        
        // Color code success rate
        if (successRate >= 80) {
            tvSuccessRate.setTextColor(getColor(android.R.color.holo_green_light));
        } else if (successRate >= 60) {
            tvSuccessRate.setTextColor(getColor(android.R.color.holo_orange_light));
        } else {
            tvSuccessRate.setTextColor(getColor(android.R.color.holo_red_light));
        }
    }
    
    private void updatePerformanceMetrics() {
        long sessionDurationMs = System.currentTimeMillis() - sessionStartTime;
        float sessionDurationMinutes = sessionDurationMs / 60000f;
        
        float actionsPerMinute = sessionDurationMinutes > 0 ? 
            totalActionsCount / sessionDurationMinutes : 0f;
        
        tvActionsPerMinute.setText(String.format(Locale.getDefault(), "%.1f APM", actionsPerMinute));
        tvTotalActions.setText(String.valueOf(totalActionsCount));
        
        // Calculate average response time
        if (!responseTimes.isEmpty()) {
            double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
            
            tvAvgResponseTime.setText(String.format(Locale.getDefault(), "%.0f ms", avgResponseTime));
        } else {
            tvAvgResponseTime.setText("-- ms");
        }
    }
    
    private void updateGameTimeMetrics() {
        long sessionDurationMs = System.currentTimeMillis() - sessionStartTime;
        long minutes = sessionDurationMs / 60000;
        long seconds = (sessionDurationMs % 60000) / 1000;
        
        tvGameTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }
    
    private void updateEfficiencyScore() {
        float successRate = totalActionsCount > 0 ? 
            (float) successfulActionsCount / totalActionsCount : 0f;
        
        long avgResponseTime = !responseTimes.isEmpty() ?
            (long) responseTimes.stream().mapToLong(Long::longValue).average().orElse(1000) : 1000;
        
        // Calculate efficiency score (0-100)
        // Based on success rate (70%) and response time (30%)
        float responseScore = Math.max(0, 100 - (avgResponseTime - 200) / 10f); // 200ms is ideal
        float efficiencyScore = (successRate * 0.7f) + (responseScore * 0.3f);
        
        tvEfficiencyScore.setText(String.format(Locale.getDefault(), "%.0f", efficiencyScore));
        
        // Color code efficiency score
        if (efficiencyScore >= 80) {
            tvEfficiencyScore.setTextColor(getColor(android.R.color.holo_green_light));
        } else if (efficiencyScore >= 60) {
            tvEfficiencyScore.setTextColor(getColor(android.R.color.holo_orange_light));
        } else {
            tvEfficiencyScore.setTextColor(getColor(android.R.color.holo_red_light));
        }
    }
    
    // Public methods for external components to report metrics
    public void reportAction(boolean successful, long responseTimeMs) {
        totalActionsCount++;
        
        if (successful) {
            successfulActionsCount++;
            currentStreak++;
        } else {
            currentStreak = 0;
        }
        
        responseTimes.add(responseTimeMs);
        
        // Keep only last 100 response times for average calculation
        if (responseTimes.size() > 100) {
            responseTimes.remove(0);
        }
        
        Log.d(TAG, "Action reported - Success: " + successful + ", Response: " + responseTimeMs + "ms");
    }
    
    public void resetMetrics() {
        totalActionsCount = 0;
        successfulActionsCount = 0;
        currentStreak = 0;
        responseTimes.clear();
        sessionStartTime = System.currentTimeMillis();
        
        Log.d(TAG, "Metrics reset");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        
        Log.d(TAG, "Real-time analytics dashboard destroyed");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.post(updateRunnable);
        }
    }
}