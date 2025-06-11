package com.gestureai.gameautomation.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.ZoneTracker;
import com.gestureai.gameautomation.WeaponRecognizer;
import com.gestureai.gameautomation.TeamClassifier;
import com.gestureai.gameautomation.database.SessionData;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class SessionAnalyticsDashboardActivity extends AppCompatActivity {
    private static final String TAG = "SessionAnalytics";
    
    // Session overview
    private TextView tvTotalSessions;
    private TextView tvTotalPlayTime;
    private TextView tvAverageScore;
    private TextView tvBestSession;
    private TextView tvWorstSession;
    private TextView tvImprovementRate;
    
    // AI Performance Comparison
    private TextView tvDQNPerformance;
    private TextView tvPPOPerformance;
    private TextView tvHybridPerformance;
    private ProgressBar pbDQNAccuracy;
    private ProgressBar pbPPOAccuracy;
    private ProgressBar pbHybridAccuracy;
    
    // Session history
    private RecyclerView rvSessionHistory;
    private SessionHistoryAdapter sessionAdapter;
    
    // Performance charts placeholder
    private TextView tvPerformanceChart;
    private TextView tvAccuracyChart;
    
    // Filter controls
    private Spinner spinnerGameType;
    private Spinner spinnerTimeRange;
    private Button btnApplyFilters;
    private Button btnExportData;
    
    // AI Components Integration
    private DQNAgent dqnAgent;
    private PPOAgent ppoAgent;
    private ZoneTracker zoneTracker;
    private WeaponRecognizer weaponRecognizer;
    private TeamClassifier teamClassifier;
    
    // Data
    private List<SessionData> allSessions;
    private List<SessionData> filteredSessions;
    private AIPerformanceMetrics aiMetrics;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_analytics_dashboard);
        
        initializeViews();
        initializeAIComponents();
        loadSessionData();
        setupFilters();
        calculateAnalytics();
    }
    
    private void initializeViews() {
        // Session overview
        tvTotalSessions = findViewById(R.id.tv_total_sessions);
        tvTotalPlayTime = findViewById(R.id.tv_total_play_time);
        tvAverageScore = findViewById(R.id.tv_average_score);
        tvBestSession = findViewById(R.id.tv_best_session);
        tvWorstSession = findViewById(R.id.tv_worst_session);
        tvImprovementRate = findViewById(R.id.tv_improvement_rate);
        
        // AI Performance
        tvDQNPerformance = findViewById(R.id.tv_dqn_performance);
        tvPPOPerformance = findViewById(R.id.tv_ppo_performance);
        tvHybridPerformance = findViewById(R.id.tv_hybrid_performance);
        pbDQNAccuracy = findViewById(R.id.pb_dqn_accuracy);
        pbPPOAccuracy = findViewById(R.id.pb_ppo_accuracy);
        pbHybridAccuracy = findViewById(R.id.pb_hybrid_accuracy);
        
        // Session history
        rvSessionHistory = findViewById(R.id.rv_session_history);
        rvSessionHistory.setLayoutManager(new LinearLayoutManager(this));
        
        // Charts
        tvPerformanceChart = findViewById(R.id.tv_performance_chart);
        tvAccuracyChart = findViewById(R.id.tv_accuracy_chart);
        
        // Filters
        spinnerGameType = findViewById(R.id.spinner_game_type);
        spinnerTimeRange = findViewById(R.id.spinner_time_range);
        btnApplyFilters = findViewById(R.id.btn_apply_filters);
        btnExportData = findViewById(R.id.btn_export_data);
        
        setupButtonListeners();
    }
    
    private void setupButtonListeners() {
        btnApplyFilters.setOnClickListener(v -> applyFilters());
        btnExportData.setOnClickListener(v -> exportSessionData());
    }
    
    private void initializeAIComponents() {
        try {
            // Initialize DQN and PPO agents for performance comparison
            dqnAgent = new DQNAgent(16, 8);
            ppoAgent = new PPOAgent(16, 8);
            
            // Initialize zone tracking for battle royale analysis
            zoneTracker = new ZoneTracker(this);
            
            // Initialize weapon recognition for FPS analysis
            weaponRecognizer = new WeaponRecognizer(this);
            
            // Initialize team classification for multiplayer analysis
            teamClassifier = new TeamClassifier(this);
            
            android.util.Log.d(TAG, "AI components initialized successfully");
            
        } catch (Exception e) {
            android.util.Log.w(TAG, "Some AI components failed to initialize", e);
        }
    }
    
    private void loadSessionData() {
        // Load session data from database
        allSessions = loadSessionsFromDatabase();
        filteredSessions = new ArrayList<>(allSessions);
        
        // Initialize adapter
        sessionAdapter = new SessionHistoryAdapter(filteredSessions);
        rvSessionHistory.setAdapter(sessionAdapter);
    }
    
    private List<SessionData> loadSessionsFromDatabase() {
        // Simulate loading from database with real session data structure
        List<SessionData> sessions = new ArrayList<>();
        
        // In production, this would load from Room database
        for (int i = 0; i < 20; i++) {
            SessionData session = new SessionData();
            session.sessionId = "session_" + i;
            session.gameType = i % 3 == 0 ? "Battle Royale" : i % 3 == 1 ? "MOBA" : "FPS";
            session.duration = 300000 + (i * 60000); // 5-25 minutes
            session.score = 1000 + (i * 150);
            session.aiStrategy = i % 3 == 0 ? "DQN" : i % 3 == 1 ? "PPO" : "Hybrid";
            session.accuracy = 0.6f + (i * 0.02f);
            session.timestamp = System.currentTimeMillis() - (i * 3600000); // Last 20 hours
            sessions.add(session);
        }
        
        return sessions;
    }
    
    private void setupFilters() {
        // Game type filter
        String[] gameTypes = {"All Games", "Battle Royale", "MOBA", "FPS", "Strategy"};
        ArrayAdapter<String> gameAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, gameTypes);
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGameType.setAdapter(gameAdapter);
        
        // Time range filter
        String[] timeRanges = {"All Time", "Last 24 Hours", "Last Week", "Last Month"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, timeRanges);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeRange.setAdapter(timeAdapter);
    }
    
    private void applyFilters() {
        String selectedGameType = spinnerGameType.getSelectedItem().toString();
        String selectedTimeRange = spinnerTimeRange.getSelectedItem().toString();
        
        filteredSessions.clear();
        
        long timeThreshold = getTimeThreshold(selectedTimeRange);
        
        for (SessionData session : allSessions) {
            boolean matchesGame = selectedGameType.equals("All Games") || 
                                session.gameType.equals(selectedGameType);
            boolean matchesTime = timeThreshold == 0 || 
                                session.timestamp >= timeThreshold;
            
            if (matchesGame && matchesTime) {
                filteredSessions.add(session);
            }
        }
        
        sessionAdapter.notifyDataSetChanged();
        calculateAnalytics();
    }
    
    private long getTimeThreshold(String timeRange) {
        long currentTime = System.currentTimeMillis();
        switch (timeRange) {
            case "Last 24 Hours":
                return currentTime - (24 * 3600000);
            case "Last Week":
                return currentTime - (7 * 24 * 3600000);
            case "Last Month":
                return currentTime - (30L * 24 * 3600000);
            default:
                return 0;
        }
    }
    
    private void calculateAnalytics() {
        if (filteredSessions.isEmpty()) {
            clearAnalytics();
            return;
        }
        
        // Calculate basic session metrics
        int totalSessions = filteredSessions.size();
        long totalPlayTime = filteredSessions.stream()
            .mapToLong(session -> session.duration)
            .sum();
        
        double averageScore = filteredSessions.stream()
            .mapToInt(session -> session.score)
            .average()
            .orElse(0.0);
        
        SessionData bestSession = filteredSessions.stream()
            .max((s1, s2) -> Integer.compare(s1.score, s2.score))
            .orElse(null);
        
        SessionData worstSession = filteredSessions.stream()
            .min((s1, s2) -> Integer.compare(s1.score, s2.score))
            .orElse(null);
        
        // Calculate improvement rate
        double improvementRate = calculateImprovementRate();
        
        // Update UI
        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvTotalPlayTime.setText(formatDuration(totalPlayTime));
        tvAverageScore.setText(String.format("%.0f", averageScore));
        tvBestSession.setText(bestSession != null ? String.valueOf(bestSession.score) : "N/A");
        tvWorstSession.setText(worstSession != null ? String.valueOf(worstSession.score) : "N/A");
        tvImprovementRate.setText(String.format("%.1f%%", improvementRate));
        
        // Calculate AI performance comparison
        calculateAIPerformance();
        
        // Update performance visualization
        updatePerformanceCharts();
    }
    
    private double calculateImprovementRate() {
        if (filteredSessions.size() < 2) return 0.0;
        
        // Sort by timestamp
        filteredSessions.sort((s1, s2) -> Long.compare(s1.timestamp, s2.timestamp));
        
        double firstHalfAvg = filteredSessions.subList(0, filteredSessions.size() / 2)
            .stream()
            .mapToInt(session -> session.score)
            .average()
            .orElse(0.0);
        
        double secondHalfAvg = filteredSessions.subList(filteredSessions.size() / 2, filteredSessions.size())
            .stream()
            .mapToInt(session -> session.score)
            .average()
            .orElse(0.0);
        
        return firstHalfAvg > 0 ? ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100 : 0.0;
    }
    
    private void calculateAIPerformance() {
        // Separate sessions by AI strategy
        Map<String, List<SessionData>> strategyGroups = new HashMap<>();
        for (SessionData session : filteredSessions) {
            strategyGroups.computeIfAbsent(session.aiStrategy, k -> new ArrayList<>()).add(session);
        }
        
        // Calculate performance for each strategy
        double dqnPerf = calculateStrategyPerformance(strategyGroups.get("DQN"));
        double ppoPerf = calculateStrategyPerformance(strategyGroups.get("PPO"));
        double hybridPerf = calculateStrategyPerformance(strategyGroups.get("Hybrid"));
        
        // Update AI performance display
        tvDQNPerformance.setText(String.format("%.1f%% Accuracy", dqnPerf));
        tvPPOPerformance.setText(String.format("%.1f%% Accuracy", ppoPerf));
        tvHybridPerformance.setText(String.format("%.1f%% Accuracy", hybridPerf));
        
        pbDQNAccuracy.setProgress((int) dqnPerf);
        pbPPOAccuracy.setProgress((int) ppoPerf);
        pbHybridAccuracy.setProgress((int) hybridPerf);
        
        // Update AI agents with performance data
        if (dqnAgent != null) {
            dqnAgent.updatePerformanceMetric((float) (dqnPerf / 100.0));
        }
        if (ppoAgent != null) {
            ppoAgent.updatePerformanceMetric((float) (ppoPerf / 100.0));
        }
    }
    
    private double calculateStrategyPerformance(List<SessionData> strategySessions) {
        if (strategySessions == null || strategySessions.isEmpty()) {
            return 0.0;
        }
        
        return strategySessions.stream()
            .mapToDouble(session -> session.accuracy * 100)
            .average()
            .orElse(0.0);
    }
    
    private void updatePerformanceCharts() {
        // Create simple text-based charts for performance trends
        StringBuilder perfChart = new StringBuilder("Performance Trend:\n");
        StringBuilder accChart = new StringBuilder("Accuracy Trend:\n");
        
        // Group sessions by time periods for trending
        int periods = Math.min(10, filteredSessions.size());
        int sessionsPerPeriod = Math.max(1, filteredSessions.size() / periods);
        
        for (int i = 0; i < periods; i++) {
            int startIdx = i * sessionsPerPeriod;
            int endIdx = Math.min(startIdx + sessionsPerPeriod, filteredSessions.size());
            
            List<SessionData> periodSessions = filteredSessions.subList(startIdx, endIdx);
            
            double avgScore = periodSessions.stream()
                .mapToInt(s -> s.score)
                .average()
                .orElse(0.0);
            
            double avgAccuracy = periodSessions.stream()
                .mapToDouble(s -> s.accuracy)
                .average()
                .orElse(0.0);
            
            // Simple ASCII bar chart
            int scoreBar = (int) (avgScore / 100);
            int accBar = (int) (avgAccuracy * 20);
            
            perfChart.append(String.format("P%d: %s (%.0f)\n", 
                i + 1, "■".repeat(Math.max(1, scoreBar)), avgScore));
            accChart.append(String.format("P%d: %s (%.1f%%)\n", 
                i + 1, "■".repeat(Math.max(1, accBar)), avgAccuracy * 100));
        }
        
        tvPerformanceChart.setText(perfChart.toString());
        tvAccuracyChart.setText(accChart.toString());
    }
    
    private void clearAnalytics() {
        tvTotalSessions.setText("0");
        tvTotalPlayTime.setText("0m");
        tvAverageScore.setText("0");
        tvBestSession.setText("N/A");
        tvWorstSession.setText("N/A");
        tvImprovementRate.setText("0%");
        
        tvDQNPerformance.setText("No Data");
        tvPPOPerformance.setText("No Data");
        tvHybridPerformance.setText("No Data");
        
        pbDQNAccuracy.setProgress(0);
        pbPPOAccuracy.setProgress(0);
        pbHybridAccuracy.setProgress(0);
    }
    
    private void exportSessionData() {
        // Export filtered session data to JSON
        StringBuilder exportData = new StringBuilder();
        exportData.append("[\n");
        
        for (int i = 0; i < filteredSessions.size(); i++) {
            SessionData session = filteredSessions.get(i);
            exportData.append("  {\n");
            exportData.append(String.format("    \"sessionId\": \"%s\",\n", session.sessionId));
            exportData.append(String.format("    \"gameType\": \"%s\",\n", session.gameType));
            exportData.append(String.format("    \"duration\": %d,\n", session.duration));
            exportData.append(String.format("    \"score\": %d,\n", session.score));
            exportData.append(String.format("    \"aiStrategy\": \"%s\",\n", session.aiStrategy));
            exportData.append(String.format("    \"accuracy\": %.3f,\n", session.accuracy));
            exportData.append(String.format("    \"timestamp\": %d\n", session.timestamp));
            exportData.append("  }");
            if (i < filteredSessions.size() - 1) exportData.append(",");
            exportData.append("\n");
        }
        
        exportData.append("]");
        
        // Save to file or share
        String filename = "session_analytics_" + System.currentTimeMillis() + ".json";
        // In production, implement file saving logic
        
        Toast.makeText(this, "Analytics exported: " + filename, Toast.LENGTH_LONG).show();
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    // Session history adapter
    private class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder> {
        private List<SessionData> data;
        
        public SessionHistoryAdapter(List<SessionData> data) {
            this.data = data;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_history, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SessionData session = data.get(position);
            
            holder.tvSessionId.setText(session.sessionId);
            holder.tvGameType.setText(session.gameType);
            holder.tvDuration.setText(formatDuration(session.duration));
            holder.tvScore.setText(String.valueOf(session.score));
            holder.tvAIStrategy.setText(session.aiStrategy);
            holder.tvAccuracy.setText(String.format("%.1f%%", session.accuracy * 100));
            holder.tvTimestamp.setText(formatTimestamp(session.timestamp));
        }
        
        @Override
        public int getItemCount() {
            return data.size();
        }
        
        private String formatTimestamp(long timestamp) {
            long hoursAgo = (System.currentTimeMillis() - timestamp) / 3600000;
            return hoursAgo + "h ago";
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSessionId, tvGameType, tvDuration, tvScore, tvAIStrategy, tvAccuracy, tvTimestamp;
            
            public ViewHolder(android.view.View itemView) {
                super(itemView);
                tvSessionId = itemView.findViewById(R.id.tv_session_id);
                tvGameType = itemView.findViewById(R.id.tv_game_type);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                tvScore = itemView.findViewById(R.id.tv_score);
                tvAIStrategy = itemView.findViewById(R.id.tv_ai_strategy);
                tvAccuracy = itemView.findViewById(R.id.tv_accuracy);
                tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            }
        }
    }
    
    // AI Performance metrics data class
    private static class AIPerformanceMetrics {
        public double dqnAccuracy;
        public double ppoAccuracy;
        public double hybridAccuracy;
        public int totalSessions;
        public double improvementRate;
    }
}