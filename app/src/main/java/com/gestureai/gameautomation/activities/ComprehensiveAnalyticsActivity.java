package com.gestureai.gameautomation.activities;

import static com.gestureai.gameautomation.utils.TimeUtils.formatTime;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.gestureai.gameautomation.PerformanceTracker;
import com.gestureai.gameautomation.R;
import com.gestureai.gameautomation.ReinforcementLearner;
import com.gestureai.gameautomation.ai.*;
import com.gestureai.gameautomation.managers.MLModelManager;
import com.gestureai.gameautomation.utils.TimeUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComprehensiveAnalyticsActivity extends AppCompatActivity {
    private static final String TAG = "ComprehensiveAnalytics";

    // Main Navigation
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    // Overview Statistics
    private TextView tvTotalSessions;
    private TextView tvTotalTime;
    private TextView tvWinRate;
    private TextView tvAvgScore;
    private TextView tvBestScore;
    private TextView tvCurrentStreak;

    // Performance Metrics
    private TextView tvAimAccuracy;
    private TextView tvReactionTime;
    private TextView tvDecisionAccuracy;
    private TextView tvResourceEfficiency;
    private TextView tvStrategySuccess;
    private ProgressBar pbOverallPerformance;

    // AI Learning Progress
    private TextView tvModelAccuracy;
    private TextView tvTrainingEpisodes;
    private TextView tvLearningProgress;
    private TextView tvAdaptationRate;
    private ProgressBar pbLearningProgress;

    // Game-Specific Analytics
    private LinearLayout layoutBattleRoyaleStats;
    private LinearLayout layoutMOBAStats;
    private LinearLayout layoutFPSStats;

    // Battle Royale Specific
    private TextView tvSurvivalRate;
    private TextView tvAvgPlacement;
    private TextView tvKillDeathRatio;
    private TextView tvDamagePerMatch;
    private TextView tvZoneAwareness;

    // MOBA Specific
    private TextView tvCSPerMinute;
    private TextView tvKDA;
    private TextView tvGoldPerMinute;
    private TextView tvWardScore;
    private TextView tvObjectiveParticipation;

    // FPS Specific
    private TextView tvHeadshotPercentage;
    private TextView tvAccuracyStats;
    private TextView tvMovementEfficiency;
    private TextView tvWeaponMastery;
    private TextView tvMapKnowledge;

    // Real-time Monitoring
    private TextView tvCurrentCPU;
    private TextView tvCurrentMemory;
    private TextView tvCurrentFPS;
    private TextView tvCurrentLatency;
    private Switch switchRealTimeMonitoring;

    // Session History
    private ListView lvSessionHistory;
    private Button btnExportData;
    private Button btnClearHistory;
    private SessionHistoryAdapter sessionAdapter;

    // AI Components
    private PerformanceTracker performanceTracker;
    private MultiPlayerStrategy multiPlayerStrategy;
    private MOBAStrategy mobaStrategy;
    private FPSStrategy fpsStrategy;
    private ResourceMonitor resourceMonitor;
    private ReinforcementLearner reinforcementLearner;

    private List<SessionData> sessionHistory;
    private boolean isRealTimeMonitoring = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comprehensive_analytics);

        initializeViews();
        initializeAIComponents();
        setupTabLayout();
        loadAnalyticsData();
        setupListeners();
        startRealTimeMonitoring();
    }

    private void initializeViews() {
        // Main Navigation
        tabLayout = findViewById(R.id.tab_layout);
       // viewPager = findViewById(R.id.view_pager);

        // Overview Statistics
        tvTotalSessions = findViewById(R.id.tv_total_sessions);
        tvTotalTime = findViewById(R.id.tv_total_time);
        tvWinRate = findViewById(R.id.tv_win_rate);
        tvAvgScore = findViewById(R.id.tv_avg_score);
        tvBestScore = findViewById(R.id.tv_best_score);
        tvCurrentStreak = findViewById(R.id.tv_current_streak);

        // Performance Metrics
        tvAimAccuracy = findViewById(R.id.tv_aim_accuracy);
        tvReactionTime = findViewById(R.id.tv_reaction_time);
        tvDecisionAccuracy = findViewById(R.id.tv_decision_accuracy);
        tvResourceEfficiency = findViewById(R.id.tv_resource_efficiency);
        tvStrategySuccess = findViewById(R.id.tv_strategy_success);
        pbOverallPerformance = findViewById(R.id.pb_overall_performance);

        // AI Learning Progress
        tvModelAccuracy = findViewById(R.id.tv_model_accuracy);
        tvTrainingEpisodes = findViewById(R.id.tv_training_episodes);
        tvLearningProgress = findViewById(R.id.tv_learning_progress);
        tvAdaptationRate = findViewById(R.id.tv_adaptation_rate);
        pbLearningProgress = findViewById(R.id.pb_learning_progress);

        // Game-Specific Layouts
        layoutBattleRoyaleStats = findViewById(R.id.layout_battle_royale_stats);
        layoutMOBAStats = findViewById(R.id.layout_moba_stats);
        layoutFPSStats = findViewById(R.id.layout_fps_stats);

        // Battle Royale Specific
        tvSurvivalRate = findViewById(R.id.tv_survival_rate);
        tvAvgPlacement = findViewById(R.id.tv_avg_placement);
        tvKillDeathRatio = findViewById(R.id.tv_kill_death_ratio);
        tvDamagePerMatch = findViewById(R.id.tv_damage_per_match);
        tvZoneAwareness = findViewById(R.id.tv_zone_awareness);

        // MOBA Specific
        tvCSPerMinute = findViewById(R.id.tv_cs_per_minute);
        tvKDA = findViewById(R.id.tv_kda);
        tvGoldPerMinute = findViewById(R.id.tv_gold_per_minute);
        tvWardScore = findViewById(R.id.tv_ward_score);
        tvObjectiveParticipation = findViewById(R.id.tv_objective_participation);

        // FPS Specific
        tvHeadshotPercentage = findViewById(R.id.tv_headshot_percentage);
        tvAccuracyStats = findViewById(R.id.tv_accuracy_stats);
        tvMovementEfficiency = findViewById(R.id.tv_movement_efficiency);
        tvWeaponMastery = findViewById(R.id.tv_weapon_mastery);
        tvMapKnowledge = findViewById(R.id.tv_map_knowledge);

        // Real-time Monitoring
        tvCurrentCPU = findViewById(R.id.tv_current_cpu);
        tvCurrentMemory = findViewById(R.id.tv_current_memory);
        tvCurrentFPS = findViewById(R.id.tv_current_fps);
        tvCurrentLatency = findViewById(R.id.tv_current_latency);
        switchRealTimeMonitoring = findViewById(R.id.switch_realtime_monitoring);

        // Session History
        lvSessionHistory = findViewById(R.id.lv_session_history);
        btnExportData = findViewById(R.id.btn_export_data);
        btnClearHistory = findViewById(R.id.btn_clear_history);

        sessionHistory = new ArrayList<>();
        sessionAdapter = new SessionHistoryAdapter(this, sessionHistory);
        lvSessionHistory.setAdapter(sessionAdapter);
    }

    private void setupTabLayout() {
        String[] tabTitles = {"Overview", "Performance", "AI Learning", "Game Stats", "Monitoring"};

        // Setup ViewPager with tabs
        // This would typically use a FragmentStateAdapter
        // For now, we'll handle tab switching manually

        for (String title : tabTitles) {
            tabLayout.addTab(tabLayout.newTab().setText(title));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchToTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void switchToTab(int position) {
        // Hide all sections first
        hideAllSections();

        switch (position) {
            case 0: // Overview
                showOverviewSection();
                break;
            case 1: // Performance
                showPerformanceSection();
                break;
            case 2: // AI Learning
                showAILearningSection();
                break;
            case 3: // Game Stats
                showGameStatsSection();
                break;
            case 4: // Monitoring
                showMonitoringSection();
                break;
        }
    }

    private void hideAllSections() {
        // Hide all layout sections
        findViewById(R.id.layout_overview).setVisibility(View.GONE);
        findViewById(R.id.layout_performance).setVisibility(View.GONE);
        findViewById(R.id.layout_ai_learning).setVisibility(View.GONE);
        findViewById(R.id.layout_game_stats).setVisibility(View.GONE);
        findViewById(R.id.layout_monitoring).setVisibility(View.GONE);
    }

    private void showOverviewSection() {
        findViewById(R.id.layout_overview).setVisibility(View.VISIBLE);
        updateOverviewData();
    }

    private void showPerformanceSection() {
        findViewById(R.id.layout_performance).setVisibility(View.VISIBLE);
        updatePerformanceData();
    }

    private void showAILearningSection() {
        findViewById(R.id.layout_ai_learning).setVisibility(View.VISIBLE);
        updateAILearningData();
    }

    private void showGameStatsSection() {
        findViewById(R.id.layout_game_stats).setVisibility(View.VISIBLE);
        updateGameStatsData();
    }

    private void showMonitoringSection() {
        findViewById(R.id.layout_monitoring).setVisibility(View.VISIBLE);
        updateMonitoringData();
    }

    private void initializeAIComponents() {
        try {
            performanceTracker = new PerformanceTracker(this);
            multiPlayerStrategy = new MultiPlayerStrategy(this);
            // Share session data
            multiPlayerStrategy.gameSessionHistory = performanceTracker.getSessionHistory();
            mobaStrategy = new MOBAStrategy(this);
            fpsStrategy = new FPSStrategy(this);
            resourceMonitor = new ResourceMonitor(this);
            reinforcementLearner = new ReinforcementLearner(this);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing analytics components", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        switchRealTimeMonitoring.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            isRealTimeMonitoring = isChecked;
            if (isChecked) {
                startRealTimeMonitoring();
            } else {
                stopRealTimeMonitoring();
            }
        });

        btnExportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportAnalyticsData();
            }
        });
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSessionHistory();
            }
        });
    }

    private void loadAnalyticsData() {
        updateOverviewData();
        updatePerformanceData();
        updateAILearningData();
        updateGameStatsData();
        updateMonitoringData();
        loadSessionHistory();
    }

    private void updateOverviewData() {
        if (performanceTracker != null) {
            int totalSessions = performanceTracker.getTotalSessions();
            long totalTime = performanceTracker.getTotalPlayTime();
            float winRate = performanceTracker.getWinRate();
            int avgScore = performanceTracker.getAverageScore();
            int bestScore = performanceTracker.getBestScore();
            int currentStreak = performanceTracker.getCurrentStreak();

            tvTotalSessions.setText(String.valueOf(totalSessions));
            tvTotalTime.setText(formatTime(totalTime));
            tvWinRate.setText(String.format("%.1f%%", winRate * 100));
            tvAvgScore.setText(String.valueOf(avgScore));
            tvBestScore.setText(String.valueOf(bestScore));
            tvCurrentStreak.setText(String.valueOf(currentStreak));
        }
    }

    private void updatePerformanceData() {
        if (performanceTracker != null) {
            float aimAccuracy = performanceTracker.getAimAccuracy();
            int reactionTime = performanceTracker.getAverageReactionTime();
            float decisionAccuracy = performanceTracker.getDecisionAccuracy();
            float resourceEff = performanceTracker.getResourceEfficiency();
            float strategySuccess = performanceTracker.getStrategySuccessRate();

            tvAimAccuracy.setText(String.format("%.1f%%", aimAccuracy * 100));
            tvReactionTime.setText(reactionTime + "ms");
            tvDecisionAccuracy.setText(String.format("%.1f%%", decisionAccuracy * 100));
            tvResourceEfficiency.setText(String.format("%.1f%%", resourceEff * 100));
            tvStrategySuccess.setText(String.format("%.1f%%", strategySuccess * 100));

            // Overall performance score
            float overallScore = (aimAccuracy + decisionAccuracy + resourceEff + strategySuccess) / 4.0f;
            pbOverallPerformance.setProgress((int)(overallScore * 100));
        }
    }

    private void updateAILearningData() {
        if (reinforcementLearner != null) {
            float modelAccuracy = reinforcementLearner.getModelAccuracy();
            int episodes = reinforcementLearner.getEpisodeCount();
            int learningProgress = reinforcementLearner.getLearningProgress();
            float adaptationRate = reinforcementLearner.getAdaptationRate();

            tvModelAccuracy.setText(String.format("%.2f%%", modelAccuracy * 100));
            tvTrainingEpisodes.setText(String.valueOf(episodes));
            tvLearningProgress.setText(String.format("%.1f%%", learningProgress * 100));
            tvAdaptationRate.setText(String.format("%.3f", adaptationRate));

            pbLearningProgress.setProgress((int)(learningProgress * 100));
        }
    }

    private void updateGameStatsData() {
        // Show relevant stats based on current game type
        // This would be determined by game detection
        showBattleRoyaleStats();

        if (multiPlayerStrategy != null) {
            float survivalRate = multiPlayerStrategy.getSurvivalRate();
            int avgPlacement = multiPlayerStrategy.getAveragePlacement();
            float kdr = multiPlayerStrategy.getKillDeathRatio();
            int damage = multiPlayerStrategy.getAverageDamage();
            float zoneAwareness = multiPlayerStrategy.getZoneAwarenessScore();

            tvSurvivalRate.setText(String.format("%.1f%%", survivalRate * 100));
            tvAvgPlacement.setText(String.valueOf(avgPlacement));
            tvKillDeathRatio.setText(String.format("%.2f", kdr));
            tvDamagePerMatch.setText(String.valueOf(damage));
            tvZoneAwareness.setText(String.format("%.1f%%", zoneAwareness * 100));
        }
    }

    private void updateMonitoringData() {
        if (resourceMonitor != null) {
            float cpuUsage = resourceMonitor.getCPUUsage();
            float memoryUsage = resourceMonitor.getMemoryUsage();
            int fps = resourceMonitor.getCurrentFPS();
            int latency = resourceMonitor.getNetworkLatency();

            tvCurrentCPU.setText(String.format("%.1f%%", cpuUsage));
            tvCurrentMemory.setText(String.format("%.1f%%", memoryUsage));
            tvCurrentFPS.setText(String.valueOf(fps));
            tvCurrentLatency.setText(latency + "ms");

            // Color code based on performance
            updateMonitoringColors(cpuUsage, memoryUsage, fps, latency);
        }
    }

    private void updateMonitoringColors(float cpu, float memory, int fps, int latency) {
        // CPU coloring
        if (cpu > 80) tvCurrentCPU.setTextColor(Color.RED);
        else if (cpu > 60) tvCurrentCPU.setTextColor(Color.YELLOW);
        else tvCurrentCPU.setTextColor(Color.GREEN);

        // Memory coloring
        if (memory > 85) tvCurrentMemory.setTextColor(Color.RED);
        else if (memory > 70) tvCurrentMemory.setTextColor(Color.YELLOW);
        else tvCurrentMemory.setTextColor(Color.GREEN);

        // FPS coloring
        if (fps < 30) tvCurrentFPS.setTextColor(Color.RED);
        else if (fps < 50) tvCurrentFPS.setTextColor(Color.YELLOW);
        else tvCurrentFPS.setTextColor(Color.GREEN);

        // Latency coloring
        if (latency > 100) tvCurrentLatency.setTextColor(Color.RED);
        else if (latency > 50) tvCurrentLatency.setTextColor(Color.YELLOW);
        else tvCurrentLatency.setTextColor(Color.GREEN);
    }

    private void showBattleRoyaleStats() {
        layoutBattleRoyaleStats.setVisibility(View.VISIBLE);
        layoutMOBAStats.setVisibility(View.GONE);
        layoutFPSStats.setVisibility(View.GONE);
    }

    private void loadSessionHistory() {
        sessionHistory.clear();

        if (performanceTracker != null) {
            List<PerformanceTracker.GameSession> sessions = performanceTracker.getRecentSessions(20);
            for (PerformanceTracker.GameSession session : sessions) {
                SessionData data = new SessionData();
                data.timestamp = session.startTime;
                data.duration = session.getDurationMs();
                data.score = session.score;
                data.accuracy = session.accuracy;
                data.gameType = session.gameType;
                sessionHistory.add(data);
            }
        }

        sessionAdapter.notifyDataSetChanged();
    }

    private void startRealTimeMonitoring() {
        if (!isRealTimeMonitoring) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
            while (isRealTimeMonitoring && !isFinishing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateMonitoringData();
                    }
                });
                try {
                    Thread.sleep(1000); // Update every second
                } catch (InterruptedException e) {
                    break;
                }
            }
            }
        }).start();
    }

    private void stopRealTimeMonitoring() {
        isRealTimeMonitoring = false;
    }

    private void exportAnalyticsData() {
        if (performanceTracker != null) {
            String exportPath = performanceTracker.exportAnalyticsData();
            if (exportPath != null) {
                Toast.makeText(this, "Data exported to: " + exportPath, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clearSessionHistory() {
        sessionHistory.clear();
        sessionAdapter.notifyDataSetChanged();
        if (performanceTracker != null) {
            performanceTracker.clearSessionHistory();
        }
        Toast.makeText(this, "Session history cleared", Toast.LENGTH_SHORT).show();
    }

//    formatTime(milliseconds)

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRealTimeMonitoring();
    }

    // Data classes and adapters
    public static class SessionData {
        public long timestamp;
        public long duration;
        public int score;
        public float accuracy;
        public String gameType;
    }

    private static class SessionHistoryAdapter extends ArrayAdapter<SessionData> {
        public SessionHistoryAdapter(ComprehensiveAnalyticsActivity context, List<SessionData> sessions) {
            super(context, android.R.layout.simple_list_item_2, sessions);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            SessionData session = getItem(position);
            if (session != null) {
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                text1.setText(session.gameType + " - Score: " + session.score);
                text2.setText("Duration: " + formatDuration(session.duration) +
                        " | Accuracy: " + String.format("%.1f%%", session.accuracy * 100));
            }

            return view;
        }

        private String formatDuration(long milliseconds) {
            long minutes = milliseconds / (1000 * 60);
            long seconds = (milliseconds / 1000) % 60;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

}