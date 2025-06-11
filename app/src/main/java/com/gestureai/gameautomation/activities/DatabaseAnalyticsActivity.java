package com.gestureai.gameautomation.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.database.SessionDao;
import com.gestureai.gameautomation.database.SessionData;
import com.gestureai.gameautomation.database.GestureDatabase;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Database Analytics Interface
 * Provides visualization for SessionDao analytics queries and performance trends
 */
public class DatabaseAnalyticsActivity extends Activity {
    private static final String TAG = "DatabaseAnalytics";
    
    // UI Components
    private TextView tvTotalSessions;
    private TextView tvAverageSessionDuration;
    private TextView tvTotalActions;
    private TextView tvOverallSuccessRate;
    private TextView tvMostPlayedGame;
    private TextView tvBestPerformingStrategy;
    private Button btnRefreshData;
    private Button btnExportReport;
    private Button btnFilterByGame;
    private Button btnFilterByDate;
    private RecyclerView rvSessionHistory;
    private RecyclerView rvPerformanceTrends;
    private Spinner spinnerGameFilter;
    private Spinner spinnerTimeRange;
    
    // Database Components
    private SessionDao sessionDao;
    private GestureDatabase database;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_session_analytics_dashboard);
            
            initializeComponents();
            setupDatabase();
            loadAnalyticsData();
            
            Log.d(TAG, "Database Analytics interface initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Database Analytics", e);
            finish();
        }
    }
    
    private void initializeComponents() {
        // Initialize UI components
        tvTotalSessions = findViewById(R.id.tv_total_sessions);
        tvAverageSessionDuration = findViewById(R.id.tv_average_duration);
        tvTotalActions = findViewById(R.id.tv_total_actions);
        tvOverallSuccessRate = findViewById(R.id.tv_success_rate);
        tvMostPlayedGame = findViewById(R.id.tv_most_played_game);
        tvBestPerformingStrategy = findViewById(R.id.tv_best_strategy);
        btnRefreshData = findViewById(R.id.btn_refresh_data);
        btnExportReport = findViewById(R.id.btn_export_report);
        btnFilterByGame = findViewById(R.id.btn_filter_game);
        btnFilterByDate = findViewById(R.id.btn_filter_date);
        rvSessionHistory = findViewById(R.id.rv_session_history);
        rvPerformanceTrends = findViewById(R.id.rv_performance_trends);
        spinnerGameFilter = findViewById(R.id.spinner_game_filter);
        spinnerTimeRange = findViewById(R.id.spinner_time_range);
        
        // Setup listeners
        setupListeners();
        setupFilters();
    }
    
    private void setupListeners() {
        btnRefreshData.setOnClickListener(this::refreshAnalyticsData);
        btnExportReport.setOnClickListener(this::exportAnalyticsReport);
        btnFilterByGame.setOnClickListener(this::applyGameFilter);
        btnFilterByDate.setOnClickListener(this::applyDateFilter);
    }
    
    private void setupFilters() {
        // Setup game filter spinner
        String[] gameTypes = {"All Games", "Battle Royale", "MOBA", "FPS", "RPG", "Strategy", "Arcade"};
        ArrayAdapter<String> gameAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, gameTypes);
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGameFilter.setAdapter(gameAdapter);
        
        // Setup time range filter
        String[] timeRanges = {"All Time", "Last 7 Days", "Last 30 Days", "Last 90 Days"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, timeRanges);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeRange.setAdapter(timeAdapter);
        
        // Setup RecyclerViews
        rvSessionHistory.setLayoutManager(new LinearLayoutManager(this));
        rvPerformanceTrends.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void setupDatabase() {
        try {
            database = GestureDatabase.getInstance(this);
            sessionDao = database.sessionDao();
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup database", e);
        }
    }
    
    private void loadAnalyticsData() {
        new Thread(() -> {
            try {
                // Get basic statistics
                List<SessionData> allSessions = sessionDao.getAllSessions();
                
                runOnUiThread(() -> {
                    displayBasicStatistics(allSessions);
                    displayPerformanceTrends(allSessions);
                    displayGameSpecificStats(allSessions);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading analytics data", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading analytics data", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void displayBasicStatistics(List<SessionData> sessions) {
        try {
            int totalSessions = sessions.size();
            long totalDuration = 0;
            int totalActions = 0;
            int totalSuccessfulActions = 0;
            
            for (SessionData session : sessions) {
                totalDuration += session.sessionDuration;
                totalActions += session.totalActions;
                totalSuccessfulActions += session.successfulActions;
            }
            
            long averageDuration = totalSessions > 0 ? totalDuration / totalSessions : 0;
            float overallSuccessRate = totalActions > 0 ? 
                (float) totalSuccessfulActions / totalActions * 100 : 0;
            
            // Update UI
            tvTotalSessions.setText("Total Sessions: " + totalSessions);
            tvAverageSessionDuration.setText("Avg Duration: " + formatDuration(averageDuration));
            tvTotalActions.setText("Total Actions: " + totalActions);
            tvOverallSuccessRate.setText(String.format("Success Rate: %.1f%%", overallSuccessRate));
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying basic statistics", e);
        }
    }
    
    private void displayPerformanceTrends(List<SessionData> sessions) {
        try {
            // Calculate performance trends over time
            Map<String, Float> dailyPerformance = new HashMap<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            for (SessionData session : sessions) {
                String date = dateFormat.format(new Date(session.startTime));
                float sessionSuccessRate = session.totalActions > 0 ? 
                    (float) session.successfulActions / session.totalActions : 0;
                
                dailyPerformance.put(date, 
                    dailyPerformance.getOrDefault(date, 0f) + sessionSuccessRate);
            }
            
            Log.d(TAG, "Performance trends calculated for " + dailyPerformance.size() + " days");
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating performance trends", e);
        }
    }
    
    private void displayGameSpecificStats(List<SessionData> sessions) {
        try {
            // Calculate game-specific statistics
            Map<String, Integer> gameSessionCounts = new HashMap<>();
            Map<String, Float> gameSuccessRates = new HashMap<>();
            Map<String, Long> gameDurations = new HashMap<>();
            
            for (SessionData session : sessions) {
                String gameType = session.gameType != null ? session.gameType : "Unknown";
                
                // Count sessions per game
                gameSessionCounts.put(gameType, 
                    gameSessionCounts.getOrDefault(gameType, 0) + 1);
                
                // Calculate success rates per game
                float sessionSuccessRate = session.totalActions > 0 ? 
                    (float) session.successfulActions / session.totalActions : 0;
                gameSuccessRates.put(gameType, 
                    gameSuccessRates.getOrDefault(gameType, 0f) + sessionSuccessRate);
                
                // Calculate total durations per game
                gameDurations.put(gameType, 
                    gameDurations.getOrDefault(gameType, 0L) + session.sessionDuration);
            }
            
            // Find most played game
            String mostPlayedGame = "None";
            int maxSessions = 0;
            for (Map.Entry<String, Integer> entry : gameSessionCounts.entrySet()) {
                if (entry.getValue() > maxSessions) {
                    maxSessions = entry.getValue();
                    mostPlayedGame = entry.getKey();
                }
            }
            
            // Find best performing strategy
            String bestStrategy = "None";
            float bestSuccessRate = 0;
            for (SessionData session : sessions) {
                if (session.aiStrategy != null) {
                    float sessionSuccessRate = session.totalActions > 0 ? 
                        (float) session.successfulActions / session.totalActions : 0;
                    if (sessionSuccessRate > bestSuccessRate) {
                        bestSuccessRate = sessionSuccessRate;
                        bestStrategy = session.aiStrategy;
                    }
                }
            }
            
            // Update UI
            tvMostPlayedGame.setText("Most Played: " + mostPlayedGame + " (" + maxSessions + " sessions)");
            tvBestPerformingStrategy.setText("Best Strategy: " + bestStrategy + 
                String.format(" (%.1f%% success)", bestSuccessRate * 100));
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying game-specific stats", e);
        }
    }
    
    private void refreshAnalyticsData(View view) {
        tvTotalSessions.setText("Loading...");
        tvAverageSessionDuration.setText("Loading...");
        tvTotalActions.setText("Loading...");
        tvOverallSuccessRate.setText("Loading...");
        
        loadAnalyticsData();
        Toast.makeText(this, "Analytics data refreshed", Toast.LENGTH_SHORT).show();
    }
    
    private void exportAnalyticsReport(View view) {
        new Thread(() -> {
            try {
                List<SessionData> allSessions = sessionDao.getAllSessions();
                String reportData = generateAnalyticsReport(allSessions);
                
                runOnUiThread(() -> {
                    // In a real implementation, save to file
                    Toast.makeText(this, "Analytics report generated", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Analytics report: " + reportData.length() + " characters");
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error exporting analytics report", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error exporting report", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void applyGameFilter(View view) {
        String selectedGame = (String) spinnerGameFilter.getSelectedItem();
        if (selectedGame != null && !selectedGame.equals("All Games")) {
            filterSessionsByGame(selectedGame);
        } else {
            loadAnalyticsData();
        }
    }
    
    private void applyDateFilter(View view) {
        String selectedRange = (String) spinnerTimeRange.getSelectedItem();
        if (selectedRange != null && !selectedRange.equals("All Time")) {
            filterSessionsByTimeRange(selectedRange);
        } else {
            loadAnalyticsData();
        }
    }
    
    private void filterSessionsByGame(String gameType) {
        new Thread(() -> {
            try {
                List<SessionData> filteredSessions = sessionDao.getSessionsByGameType(gameType);
                
                runOnUiThread(() -> {
                    displayBasicStatistics(filteredSessions);
                    displayPerformanceTrends(filteredSessions);
                    displayGameSpecificStats(filteredSessions);
                    Toast.makeText(this, "Filtered by game: " + gameType, Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error filtering by game", e);
            }
        }).start();
    }
    
    private void filterSessionsByTimeRange(String timeRange) {
        new Thread(() -> {
            try {
                long cutoffTime = System.currentTimeMillis();
                switch (timeRange) {
                    case "Last 7 Days":
                        cutoffTime -= 7 * 24 * 60 * 60 * 1000L;
                        break;
                    case "Last 30 Days":
                        cutoffTime -= 30 * 24 * 60 * 60 * 1000L;
                        break;
                    case "Last 90 Days":
                        cutoffTime -= 90 * 24 * 60 * 60 * 1000L;
                        break;
                }
                
                List<SessionData> filteredSessions = sessionDao.getSessionsAfterTime(cutoffTime);
                
                runOnUiThread(() -> {
                    displayBasicStatistics(filteredSessions);
                    displayPerformanceTrends(filteredSessions);
                    displayGameSpecificStats(filteredSessions);
                    Toast.makeText(this, "Filtered by time: " + timeRange, Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error filtering by time range", e);
            }
        }).start();
    }
    
    private String generateAnalyticsReport(List<SessionData> sessions) {
        StringBuilder report = new StringBuilder();
        report.append("=== GestureAI Analytics Report ===\n");
        report.append("Generated: ").append(new Date().toString()).append("\n\n");
        
        report.append("Basic Statistics:\n");
        report.append("- Total Sessions: ").append(sessions.size()).append("\n");
        
        long totalDuration = 0;
        int totalActions = 0;
        int totalSuccessfulActions = 0;
        
        for (SessionData session : sessions) {
            totalDuration += session.sessionDuration;
            totalActions += session.totalActions;
            totalSuccessfulActions += session.successfulActions;
        }
        
        report.append("- Total Duration: ").append(formatDuration(totalDuration)).append("\n");
        report.append("- Total Actions: ").append(totalActions).append("\n");
        report.append("- Success Rate: ").append(
            totalActions > 0 ? String.format("%.2f%%", (float) totalSuccessfulActions / totalActions * 100) : "0%"
        ).append("\n");
        
        return report.toString();
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Database Analytics interface destroyed");
    }
}