package com.gestureai.gameautomation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.PerformanceTracker;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsActivity extends AppCompatActivity {
    private static final String TAG = "AnalyticsActivity";
    
    private TextView tvTotalSessions;
    private TextView tvTotalTime;
    private TextView tvAvgAccuracy;
    private TextView tvBestScore;
    private RecyclerView rvSessionHistory;
    private Button btnComprehensiveAnalytics;
    private Button btnRealTimeAnalytics;
    private Button btnSessionReplay;
    private PerformanceTracker performanceTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);
        
        initializeViews();
        loadAnalyticsData();
    }

    private void initializeViews() {
        tvTotalSessions = findViewById(R.id.tv_total_sessions);
        tvTotalTime = findViewById(R.id.tv_total_time);
        tvAvgAccuracy = findViewById(R.id.tv_avg_accuracy);
        tvBestScore = findViewById(R.id.tv_best_score);
        rvSessionHistory = findViewById(R.id.rv_session_history);
        btnComprehensiveAnalytics = findViewById(R.id.btn_comprehensive_analytics);
        btnRealTimeAnalytics = findViewById(R.id.btn_real_time_analytics);
        btnSessionReplay = findViewById(R.id.btn_session_replay);
        
        // Setup RecyclerView
        rvSessionHistory.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize performance tracker
        performanceTracker = new PerformanceTracker(this);
        
        setupButtonListeners();
    }
    
    private void setupButtonListeners() {
        // Connect to ComprehensiveAnalyticsActivity
        btnComprehensiveAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComprehensiveAnalyticsActivity.class);
            startActivity(intent);
        });
        
        // Connect to Real-time Analytics (create activity if needed)
        btnRealTimeAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.gestureai.gameautomation.activities.RealTimeAnalyticsActivity.class);
            startActivity(intent);
        });
        
        // Connect to Session Replay System
        btnSessionReplay.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.gestureai.gameautomation.activities.SessionReplaySystemActivity.class);
            startActivity(intent);
        });
    }

    private void loadAnalyticsData() {
        // Load analytics data from database/storage
        // Placeholder data for now
        tvTotalSessions.setText("0");
        tvTotalTime.setText("0 min");
        tvAvgAccuracy.setText("0%");
        tvBestScore.setText("0");
    }
}