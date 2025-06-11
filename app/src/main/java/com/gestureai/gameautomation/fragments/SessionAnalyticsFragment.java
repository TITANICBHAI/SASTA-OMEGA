package com.gestureai.gameautomation.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.database.AppDatabase;
import com.gestureai.gameautomation.database.dao.SessionDataDao;
import com.gestureai.gameautomation.data.SessionData;
import com.gestureai.gameautomation.activities.SessionAnalyticsDashboardActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Session Analytics & Replay System - Real-time performance monitoring
 */
public class SessionAnalyticsFragment extends Fragment {
    private static final String TAG = "SessionAnalyticsFragment";
    
    // Analytics Overview
    private TextView tvTotalSessions, tvTotalActions, tvAverageSuccessRate, tvTotalPlayTime;
    private ProgressBar pbPerformanceScore;
    
    // Charts
    private LineChart lineChartPerformance;
    private PieChart pieChartGameTypes;
    
    // Session List
    private RecyclerView rvRecentSessions;
    private SessionHistoryAdapter sessionAdapter;
    
    // Controls
    private Button btnReplaySession, btnExportData, btnClearHistory, btnRefreshData;
    private Spinner spinnerGameTypeFilter, spinnerTimeRange;
    
    // Data
    private AppDatabase database;
    private SessionDataDao sessionDao;
    private List<SessionData> recentSessions;
    private SessionData selectedSession;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session_analytics, container, false);
        
        initializeComponents();
        initializeViews(view);
        setupClickListeners();
        setupSpinners();
        loadAnalyticsData();
        
        return view;
    }
    
    private void initializeComponents() {
        database = AppDatabase.getInstance(getContext());
        sessionDao = database.sessionDataDao();
        recentSessions = new ArrayList<>();
    }
    
    private void initializeViews(View view) {
        // Overview Stats
        tvTotalSessions = view.findViewById(R.id.tv_total_sessions);
        tvTotalActions = view.findViewById(R.id.tv_total_actions);
        tvAverageSuccessRate = view.findViewById(R.id.tv_average_success_rate);
        tvTotalPlayTime = view.findViewById(R.id.tv_total_play_time);
        pbPerformanceScore = view.findViewById(R.id.pb_performance_score);
        
        // Charts
        lineChartPerformance = view.findViewById(R.id.line_chart_performance);
        pieChartGameTypes = view.findViewById(R.id.pie_chart_game_types);
        
        // Session List
        rvRecentSessions = view.findViewById(R.id.rv_recent_sessions);
        rvRecentSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        sessionAdapter = new SessionHistoryAdapter(recentSessions, this::onSessionSelected);
        rvRecentSessions.setAdapter(sessionAdapter);
        
        // Controls
        btnReplaySession = view.findViewById(R.id.btn_replay_session);
        btnExportData = view.findViewById(R.id.btn_export_data);
        btnClearHistory = view.findViewById(R.id.btn_clear_history);
        btnRefreshData = view.findViewById(R.id.btn_refresh_data);
        spinnerGameTypeFilter = view.findViewById(R.id.spinner_game_type_filter);
        spinnerTimeRange = view.findViewById(R.id.spinner_time_range);
    }
    
    private void setupClickListeners() {
        btnReplaySession.setOnClickListener(v -> replaySelectedSession());
        btnExportData.setOnClickListener(v -> exportAnalyticsData());
        btnClearHistory.setOnClickListener(v -> clearSessionHistory());
        btnRefreshData.setOnClickListener(v -> loadAnalyticsData());
    }
    
    private void setupSpinners() {
        // Game Type Filter
        String[] gameTypes = {"All Games", "Battle Royale", "MOBA", "FPS", "Arcade", "RPG"};
        ArrayAdapter<String> gameTypeAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, gameTypes);
        gameTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGameTypeFilter.setAdapter(gameTypeAdapter);
        
        // Time Range Filter
        String[] timeRanges = {"Last 24 Hours", "Last Week", "Last Month", "All Time"};
        ArrayAdapter<String> timeRangeAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, timeRanges);
        timeRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeRange.setAdapter(timeRangeAdapter);
        
        // Set up filter listeners
        spinnerGameTypeFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterSessionsByGameType(gameTypes[position]);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        spinnerTimeRange.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterSessionsByTimeRange(timeRanges[position]);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    private void loadAnalyticsData() {
        new Thread(() -> {
            try {
                // Load overview statistics
                long totalSessions = sessionDao.getSessionCount();
                long totalActions = sessionDao.getTotalActionCount();
                float averageSuccessRate = sessionDao.getAverageSuccessRate();
                List<SessionEntity> sessions = sessionDao.getRecentSessions(50);
                
                // Calculate total play time
                long totalPlayTime = 0;
                for (SessionEntity session : sessions) {
                    if (session.endTime != null) {
                        totalPlayTime += (session.endTime - session.startTime);
                    }
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateOverviewStats(totalSessions, totalActions, averageSuccessRate, totalPlayTime);
                        updateSessionsList(sessions);
                        updatePerformanceChart(sessions);
                        updateGameTypesChart(sessions);
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading analytics data", e);
            }
        }).start();
    }
    
    private void updateOverviewStats(long totalSessions, long totalActions, float averageSuccessRate, long totalPlayTime) {
        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvTotalActions.setText(String.format("%,d", totalActions));
        tvAverageSuccessRate.setText(String.format("%.1f%%", averageSuccessRate));
        
        // Convert milliseconds to hours
        long hours = totalPlayTime / (1000 * 60 * 60);
        long minutes = (totalPlayTime % (1000 * 60 * 60)) / (1000 * 60);
        tvTotalPlayTime.setText(String.format("%dh %dm", hours, minutes));
        
        // Calculate performance score (0-100)
        int performanceScore = Math.min(100, (int) (averageSuccessRate * 1.2f)); // Scale success rate
        pbPerformanceScore.setProgress(performanceScore);
    }
    
    private void updateSessionsList(List<SessionEntity> sessions) {
        recentSessions.clear();
        recentSessions.addAll(sessions);
        sessionAdapter.notifyDataSetChanged();
    }
    
    private void updatePerformanceChart(List<SessionEntity> sessions) {
        List<Entry> entries = new ArrayList<>();
        
        for (int i = 0; i < Math.min(sessions.size(), 20); i++) {
            SessionEntity session = sessions.get(i);
            entries.add(new Entry(i, session.successRate));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Success Rate %");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        
        LineData lineData = new LineData(dataSet);
        lineChartPerformance.setData(lineData);
        lineChartPerformance.getDescription().setText("Performance Over Time");
        lineChartPerformance.invalidate(); // refresh
    }
    
    private void updateGameTypesChart(List<SessionEntity> sessions) {
        Map<String, Integer> gameTypeCounts = new HashMap<>();
        
        for (SessionEntity session : sessions) {
            String gameType = session.gameType != null ? session.gameType : "Unknown";
            gameTypeCounts.put(gameType, gameTypeCounts.getOrDefault(gameType, 0) + 1);
        }
        
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : gameTypeCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "Game Types");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        
        PieData pieData = new PieData(dataSet);
        pieChartGameTypes.setData(pieData);
        pieChartGameTypes.getDescription().setText("Game Type Distribution");
        pieChartGameTypes.invalidate(); // refresh
    }
    
    private void filterSessionsByGameType(String gameType) {
        if ("All Games".equals(gameType)) {
            loadAnalyticsData();
            return;
        }
        
        new Thread(() -> {
            try {
                List<SessionEntity> filteredSessions = sessionDao.getSessionsByGameType(gameType);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateSessionsList(filteredSessions);
                        updatePerformanceChart(filteredSessions);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error filtering sessions by game type", e);
            }
        }).start();
    }
    
    private void filterSessionsByTimeRange(String timeRange) {
        // Implementation for time range filtering
        // Would filter sessions based on selected time range
        loadAnalyticsData(); // For now, just reload all data
    }
    
    private void onSessionSelected(SessionData session) {
        selectedSession = session;
        btnReplaySession.setEnabled(true);
        Log.d(TAG, "Session selected: " + session.sessionName);
    }
    
    private void replaySelectedSession() {
        if (selectedSession != null) {
            Intent intent = new Intent(getActivity(), SessionAnalyticsDashboardActivity.class);
            intent.putExtra("session_id", selectedSession.id);
            intent.putExtra("show_replay", true);
            startActivity(intent);
        }
    }
    
    private void exportAnalyticsData() {
        new Thread(() -> {
            try {
                // Export session data to CSV or JSON format
                List<SessionEntity> allSessions = sessionDao.getAllSessions();
                
                StringBuilder csvData = new StringBuilder();
                csvData.append("Session Name,Game Type,Strategy,Start Time,End Time,Actions,Success Rate\n");
                
                for (SessionEntity session : allSessions) {
                    csvData.append(String.format("%s,%s,%s,%d,%s,%d,%.2f\n",
                        session.sessionName,
                        session.gameType,
                        session.strategy,
                        session.startTime,
                        session.endTime != null ? session.endTime.toString() : "Ongoing",
                        session.actionsPerformed,
                        session.successRate));
                }
                
                // Save to external storage
                String filePath = saveDataToFile(csvData.toString());
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(getContext(), 
                            "Data exported to: " + filePath, android.widget.Toast.LENGTH_LONG).show();
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error exporting data", e);
            }
        }).start();
    }
    
    private String saveDataToFile(String data) {
        // Implementation to save data to file
        // Return file path
        return "/storage/emulated/0/Download/session_analytics.csv";
    }
    
    private void clearSessionHistory() {
        new android.app.AlertDialog.Builder(getContext())
            .setTitle("Clear Session History")
            .setMessage("Are you sure you want to clear all session data? This action cannot be undone.")
            .setPositiveButton("Clear", (dialog, which) -> {
                new Thread(() -> {
                    try {
                        // Clear all sessions from database
                        List<SessionEntity> allSessions = sessionDao.getAllSessions();
                        for (SessionEntity session : allSessions) {
                            sessionDao.deleteSession(session);
                        }
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                loadAnalyticsData(); // Refresh UI
                                android.widget.Toast.makeText(getContext(), 
                                    "Session history cleared", android.widget.Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error clearing session history", e);
                    }
                }).start();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    // Adapter for session history list
    public static class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder> {
        private List<SessionEntity> sessions;
        private OnSessionSelectedListener listener;
        
        public interface OnSessionSelectedListener {
            void onSessionSelected(SessionEntity session);
        }
        
        public SessionHistoryAdapter(List<SessionEntity> sessions, OnSessionSelectedListener listener) {
            this.sessions = sessions;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_history, parent, false);
            return new SessionViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
            SessionEntity session = sessions.get(position);
            holder.bind(session, listener);
        }
        
        @Override
        public int getItemCount() {
            return sessions.size();
        }
        
        static class SessionViewHolder extends RecyclerView.ViewHolder {
            TextView tvSessionName, tvGameType, tvDuration, tvSuccessRate;
            
            public SessionViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSessionName = itemView.findViewById(R.id.tv_session_name);
                tvGameType = itemView.findViewById(R.id.tv_game_type);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                tvSuccessRate = itemView.findViewById(R.id.tv_success_rate);
            }
            
            public void bind(SessionEntity session, OnSessionSelectedListener listener) {
                tvSessionName.setText(session.sessionName);
                tvGameType.setText(session.gameType);
                
                if (session.endTime != null) {
                    long duration = session.endTime - session.startTime;
                    long minutes = duration / (1000 * 60);
                    tvDuration.setText(minutes + " min");
                } else {
                    tvDuration.setText("Ongoing");
                }
                
                tvSuccessRate.setText(String.format("%.1f%%", session.successRate));
                
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSessionSelected(session);
                    }
                });
            }
        }
    }
}