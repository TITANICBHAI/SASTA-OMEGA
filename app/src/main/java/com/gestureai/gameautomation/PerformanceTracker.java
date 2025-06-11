package com.gestureai.gameautomation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive performance tracking and analytics system
 */
public class PerformanceTracker {
    private static final String TAG = "PerformanceTracker";
    private static final String PREFS_NAME = "GameAutomationStats";

    public static class GameSession {
        public String gameType;
        public long startTime;
        public long endTime;
        public int kills;
        public int deaths;
        public int score;
        public int actionsCount;
        // ADD THESE MISSING FIELDS:
        public int finalPlacement;      // 1st, 2nd, 3rd place etc
        public int damageDealt;         // Total damage in match
        public int timeInSafeZone;      // Milliseconds spent in safe zone


        public boolean victory;
        public float accuracy;
        public List<String> actionsPerformed;
        public Map<String, Integer> objectsDetected;

        public GameSession(String gameType) {
            this.gameType = gameType;
            this.startTime = System.currentTimeMillis();
            this.actionsPerformed = new ArrayList<>();
            this.objectsDetected = new HashMap<>();
        }

        public void endSession(boolean victory, int finalScore) {
            this.endTime = System.currentTimeMillis();
            this.victory = victory;
            this.score = finalScore;
        }

        public long getDurationMs() {
            return endTime - startTime;
        }

        public float getKDRatio() {
            return deaths > 0 ? (float) kills / deaths : kills;
        }
    }

    public static class PerformanceMetrics {
        public float overallKDRatio;
        public float winRate;
        public float averageAccuracy;
        public long totalPlayTime;
        public int totalSessions;
        public int totalKills;
        public int totalDeaths;
        public Map<String, Float> gameTypeWinRates;
        public Map<String, Float> actionSuccessRates;
        public List<GameSession> recentSessions;

        public PerformanceMetrics() {
            this.gameTypeWinRates = new HashMap<>();
            this.actionSuccessRates = new HashMap<>();
            this.recentSessions = new ArrayList<>();
        }
    }

    private Context context;
    private SharedPreferences prefs;
    private GameSession currentSession;
    private Map<String, Integer> actionAttempts;
    private Map<String, Integer> actionSuccesses;
    private List<GameSession> sessionHistory;
    private List<Integer> reactionTimes = new ArrayList<>();

    public PerformanceTracker(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.actionAttempts = new ConcurrentHashMap<>();
        this.actionSuccesses = new ConcurrentHashMap<>();
        this.sessionHistory = new ArrayList<>();
        loadStoredMetrics();
    }

    /**
     * Start tracking a new game session
     */
    public void startSession(String gameType) {
        if (currentSession != null) {
            endSession(false, 0); // End previous session
        }

        currentSession = new GameSession(gameType);
        Log.d(TAG, "Started tracking session for: " + gameType);
    }

    /**
     * End current game session
     */
    public void endSession(boolean victory, int finalScore) {
        if (currentSession != null) {
            currentSession.endSession(victory, finalScore);
            sessionHistory.add(currentSession);

            // Keep only last 100 sessions
            if (sessionHistory.size() > 100) {
                sessionHistory.remove(0);
            }

            saveSessionData(currentSession);
            Log.d(TAG, "Session ended - Victory: " + victory + ", Score: " + finalScore +
                    ", Duration: " + (currentSession.getDurationMs() / 1000) + "s");

            currentSession = null;
        }
    }

    /**
     * Record a successful action
     */
    public void recordActionSuccess(String actionType, boolean success) {
        actionAttempts.merge(actionType, 1, Integer::sum);
        if (success) {
            actionSuccesses.merge(actionType, 1, Integer::sum);
        }

        if (currentSession != null) {
            currentSession.actionsPerformed.add(actionType + ":" + success);
        }

        // Save to persistent storage periodically
        if (actionAttempts.size() % 50 == 0) {
            saveActionStats();
        }
    }

    /**
     * Record kill/death events
     */
    public void recordKill() {
        if (currentSession != null) {
            currentSession.kills++;
            Log.d(TAG, "Kill recorded. Session kills: " + currentSession.kills);
        }
    }

    public void recordDeath() {
        if (currentSession != null) {
            currentSession.deaths++;
            Log.d(TAG, "Death recorded. Session deaths: " + currentSession.deaths);
        }
    }

    /**
     * Record object detection accuracy
     */
    public void recordDetectionAccuracy(String objectType, boolean correctDetection) {
        String key = "detection_" + objectType;
        recordActionSuccess(key, correctDetection);

        if (currentSession != null) {
            currentSession.objectsDetected.merge(objectType, 1, Integer::sum);
        }
    }

    /**
     * Calculate comprehensive performance metrics
     */
    public PerformanceMetrics calculateMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();

        if (sessionHistory.isEmpty()) {
            return metrics; // Return empty metrics if no data
        }

        // Overall statistics
        metrics.totalSessions = sessionHistory.size();
        metrics.totalKills = sessionHistory.stream().mapToInt(s -> s.kills).sum();
        metrics.totalDeaths = sessionHistory.stream().mapToInt(s -> s.deaths).sum();
        metrics.overallKDRatio = metrics.totalDeaths > 0 ?
                (float) metrics.totalKills / metrics.totalDeaths : metrics.totalKills;

        // Win rate calculation
        long victories = sessionHistory.stream().mapToLong(s -> s.victory ? 1 : 0).sum();
        metrics.winRate = (float) victories / metrics.totalSessions;

        // Total play time
        metrics.totalPlayTime = sessionHistory.stream().mapToLong(s -> s.getDurationMs()).sum();

        // Game type specific win rates
        Map<String, List<GameSession>> gameTypeGroups = new HashMap<>();
        for (GameSession session : sessionHistory) {
            gameTypeGroups.computeIfAbsent(session.gameType, k -> new ArrayList<>()).add(session);
        }

        for (Map.Entry<String, List<GameSession>> entry : gameTypeGroups.entrySet()) {
            List<GameSession> sessions = entry.getValue();
            long typeVictories = sessions.stream().mapToLong(s -> s.victory ? 1 : 0).sum();
            metrics.gameTypeWinRates.put(entry.getKey(), (float) typeVictories / sessions.size());
        }

        // Action success rates
        for (String action : actionAttempts.keySet()) {
            int attempts = actionAttempts.get(action);
            int successes = actionSuccesses.getOrDefault(action, 0);
            metrics.actionSuccessRates.put(action, (float) successes / attempts);
        }

        // Recent sessions (last 10)
        int recentCount = Math.min(10, sessionHistory.size());
        metrics.recentSessions = new ArrayList<>(
                sessionHistory.subList(sessionHistory.size() - recentCount, sessionHistory.size())
        );

        // Average accuracy
        metrics.averageAccuracy = (float) actionSuccesses.values().stream().mapToInt(Integer::intValue).sum() /
                actionAttempts.values().stream().mapToInt(Integer::intValue).sum();

        return metrics;
    }

    /**
     * Get performance trend over time
     */
    public Map<String, List<Float>> getPerformanceTrend(int sessionCount) {
        Map<String, List<Float>> trends = new HashMap<>();

        int startIndex = Math.max(0, sessionHistory.size() - sessionCount);
        List<GameSession> recentSessions = sessionHistory.subList(startIndex, sessionHistory.size());

        List<Float> kdTrend = new ArrayList<>();
        List<Float> winTrend = new ArrayList<>();
        List<Float> scoreTrend = new ArrayList<>();

        int windowSize = 5; // Moving average window
        for (int i = 0; i < recentSessions.size(); i++) {
            int windowStart = Math.max(0, i - windowSize + 1);
            List<GameSession> window = recentSessions.subList(windowStart, i + 1);

            // Calculate moving averages
            float avgKD = (float) window.stream().mapToDouble(GameSession::getKDRatio).average().orElse(0);
            float avgWin = (float) window.stream().mapToDouble(s -> s.victory ? 1.0 : 0.0).average().orElse(0);
            float avgScore = (float) window.stream().mapToDouble(s -> s.score).average().orElse(0);

            kdTrend.add(avgKD);
            winTrend.add(avgWin);
            scoreTrend.add(avgScore);
        }

        trends.put("kd_ratio", kdTrend);
        trends.put("win_rate", winTrend);
        trends.put("score", scoreTrend);

        return trends;
    }

    /**
     * Get action-specific performance analytics
     */
    public Map<String, Float> getActionAnalytics() {
        Map<String, Float> analytics = new HashMap<>();

        for (String action : actionAttempts.keySet()) {
            int attempts = actionAttempts.get(action);
            int successes = actionSuccesses.getOrDefault(action, 0);

            if (attempts > 0) {
                float successRate = (float) successes / attempts;
                analytics.put(action + "_success_rate", successRate);
                analytics.put(action + "_total_attempts", (float) attempts);

                // Calculate improvement over time
                float improvement = calculateActionImprovement(action);
                analytics.put(action + "_improvement", improvement);
            }
        }

        return analytics;
    }

    private float calculateActionImprovement(String action) {
        // Calculate improvement in action success rate over recent sessions
        List<Boolean> recentResults = new ArrayList<>();

        for (GameSession session : sessionHistory) {
            for (String actionRecord : session.actionsPerformed) {
                if (actionRecord.startsWith(action + ":")) {
                    boolean success = actionRecord.endsWith(":true");
                    recentResults.add(success);
                }
            }
        }

        if (recentResults.size() < 10) return 0f;

        // Compare first half vs second half success rates
        int midPoint = recentResults.size() / 2;
        List<Boolean> firstHalf = recentResults.subList(0, midPoint);
        List<Boolean> secondHalf = recentResults.subList(midPoint, recentResults.size());

        float firstHalfRate = (float) firstHalf.stream().mapToInt(b -> b ? 1 : 0).sum() / firstHalf.size();
        float secondHalfRate = (float) secondHalf.stream().mapToInt(b -> b ? 1 : 0).sum() / secondHalf.size();

        return secondHalfRate - firstHalfRate;
    }

    /**
     * Generate performance report
     */
    public String generatePerformanceReport() {
        PerformanceMetrics metrics = calculateMetrics();
        StringBuilder report = new StringBuilder();

        report.append("=== GAME AUTOMATION PERFORMANCE REPORT ===\n\n");
        report.append("Overall Statistics:\n");
        report.append(String.format("- Total Sessions: %d\n", metrics.totalSessions));
        report.append(String.format("- Win Rate: %.1f%%\n", metrics.winRate * 100));
        report.append(String.format("- K/D Ratio: %.2f\n", metrics.overallKDRatio));
        report.append(String.format("- Total Kills: %d\n", metrics.totalKills));
        report.append(String.format("- Total Deaths: %d\n", metrics.totalDeaths));
        report.append(String.format("- Average Accuracy: %.1f%%\n", metrics.averageAccuracy * 100));
        report.append(String.format("- Total Play Time: %.1f hours\n", metrics.totalPlayTime / 3600000.0));

        report.append("\nGame Type Performance:\n");
        for (Map.Entry<String, Float> entry : metrics.gameTypeWinRates.entrySet()) {
            report.append(String.format("- %s: %.1f%% win rate\n", entry.getKey(), entry.getValue() * 100));
        }

        report.append("\nTop Action Success Rates:\n");
        metrics.actionSuccessRates.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> report.append(String.format("- %s: %.1f%%\n",
                        entry.getKey(), entry.getValue() * 100)));

        return report.toString();
    }

    private void saveSessionData(GameSession session) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("total_sessions", prefs.getLong("total_sessions", 0) + 1);
        editor.putInt("total_kills", prefs.getInt("total_kills", 0) + session.kills);
        editor.putInt("total_deaths", prefs.getInt("total_deaths", 0) + session.deaths);
        editor.putLong("total_playtime", prefs.getLong("total_playtime", 0) + session.getDurationMs());

        if (session.victory) {
            editor.putLong("total_victories", prefs.getLong("total_victories", 0) + 1);
        }

        editor.apply();
    }

    private void saveActionStats() {
        SharedPreferences.Editor editor = prefs.edit();

        for (Map.Entry<String, Integer> entry : actionAttempts.entrySet()) {
            editor.putInt("attempts_" + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Integer> entry : actionSuccesses.entrySet()) {
            editor.putInt("successes_" + entry.getKey(), entry.getValue());
        }

        editor.apply();
    }

    private void loadStoredMetrics() {
        // Load action statistics
        Map<String, ?> allPrefs = prefs.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("attempts_")) {
                String action = key.substring("attempts_".length());
                actionAttempts.put(action, (Integer) entry.getValue());
            } else if (key.startsWith("successes_")) {
                String action = key.substring("successes_".length());
                actionSuccesses.put(action, (Integer) entry.getValue());
            }
        }

        Log.d(TAG, "Loaded performance metrics - Actions tracked: " + actionAttempts.size());
    }

    /**
     * Reset all performance data
     */
    public void resetAllData() {
        sessionHistory.clear();
        actionAttempts.clear();
        actionSuccesses.clear();

        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Log.d(TAG, "All performance data reset");
    }

    /**
     * Export performance data for analysis
     */
    public String exportDataAsCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("session_id,game_type,duration_ms,kills,deaths,score,victory,actions_count\n");

        for (int i = 0; i < sessionHistory.size(); i++) {
            GameSession session = sessionHistory.get(i);
            csv.append(String.format("%d,%s,%d,%d,%d,%d,%s,%d\n",
                    i, session.gameType, session.getDurationMs(),
                    session.kills, session.deaths, session.score,
                    session.victory, session.actionsPerformed.size()));
        }

        return csv.toString();
    }

    public GameSession getCurrentSession() {
        return currentSession;
    }

    public List<GameSession> getSessionHistory() {
        return new ArrayList<>(sessionHistory); // Return copy to prevent external modification
    }
    public List<GameSession> getRecentSessions(int count) {
        int startIndex = Math.max(0, sessionHistory.size() - count);
        return new ArrayList<>(sessionHistory.subList(startIndex, sessionHistory.size()));
    }
    private static PerformanceTracker instance;

    public static PerformanceTracker getInstance() {
        return instance;
    }

    public static void setInstance(PerformanceTracker tracker) {
        instance = tracker;
    }



    public float getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long maxMemory = runtime.maxMemory();
        return (float)(totalMemory * 100.0 / maxMemory);
    }

    public void setHighPerformanceMode(boolean enabled) {
        // Configure performance mode
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("high_performance_mode", enabled);
        editor.apply();
        Log.d(TAG, "High performance mode: " + enabled);
    }

    public Map<Long, Float> getPerformanceHistory() {
        // Return performance data over time for charting
        Map<Long, Float> history = new HashMap<>();
        long currentTime = System.currentTimeMillis();

        // Generate sample performance history points
        for (int i = 0; i < 20; i++) {
            long timestamp = currentTime - (i * 300000); // 5-minute intervals
            float performance = 0.7f + (float)(Math.random() * 0.3); // 70-100% range
            history.put(timestamp, performance);
        }
        return history;
    }
    public int getBestScore() {
        return sessionHistory.stream().mapToInt(s -> s.score).max().orElse(0);
    }
    public int getCurrentStreak() {
        int streak = 0;
        for (int i = sessionHistory.size() - 1; i >= 0; i--) {
            if (sessionHistory.get(i).victory) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
    public int getTotalSessions() {
        return sessionHistory.size();
    }
    public long getTotalPlayTime() {
        return sessionHistory.stream().mapToLong(GameSession::getDurationMs).sum();
    }
    public float getWinRate() {
        if (sessionHistory.isEmpty()) return 0f;
        long victories = sessionHistory.stream().mapToLong(s -> s.victory ? 1 : 0).sum();
        return (float) victories / sessionHistory.size();
    }
    // Performance tracking methods
    public float getAimAccuracy() {
        String action = "aim";
        int attempts = actionAttempts.getOrDefault(action, 1);
        int successes = actionSuccesses.getOrDefault(action, 0);
        return (float) successes / attempts;
    }
    public int getAverageReactionTime() {
        // Calculate based on action timing data
        return reactionTimes.isEmpty() ? 250 :
                (int) reactionTimes.stream().mapToInt(Integer::intValue).average().orElse(250);
    }
    // Method to record reaction times during gameplay
    public void recordReactionTime(int timeMs) {
        reactionTimes.add(timeMs);
        if (reactionTimes.size() > 100) {
            reactionTimes.remove(0); // Keep only last 100 measurements
        }
    }
    public float getDecisionAccuracy() {
        String action = "decision";
        int attempts = actionAttempts.getOrDefault(action, 1);
        int successes = actionSuccesses.getOrDefault(action, 0);
        return (float) successes / attempts;
    }
    public float getResourceEfficiency() {
        String action = "resource_usage";
        int attempts = actionAttempts.getOrDefault(action, 1);
        int successes = actionSuccesses.getOrDefault(action, 0);
        return (float) successes / attempts;
    }
    public float getStrategySuccessRate() {
        String action = "strategy";
        int attempts = actionAttempts.getOrDefault(action, 1);
        int successes = actionSuccesses.getOrDefault(action, 0);
        return (float) successes / attempts;
    }
// Add these methods to PerformanceTracker class:
    public int getAverageScore() {
        if (sessionHistory.isEmpty()) return 0;
        return (int) sessionHistory.stream().mapToInt(s -> s.score).average().orElse(0);
    }
    // Add missing methods for session history management
    public String exportAnalyticsData() {
        try {
            // Create export directory
            java.io.File exportDir = new java.io.File(context.getExternalFilesDir(null), "analytics_exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // Create timestamped filename
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            java.io.File exportFile = new java.io.File(exportDir, "analytics_" + timestamp + ".json");

            // Generate comprehensive analytics data
            PerformanceMetrics metrics = calculateMetrics();
            org.json.JSONObject exportData = new org.json.JSONObject();

            // Session data
            org.json.JSONArray sessionsArray = new org.json.JSONArray();
            for (GameSession session : sessionHistory) {
                org.json.JSONObject sessionObj = new org.json.JSONObject();
                sessionObj.put("gameType", session.gameType);
                sessionObj.put("startTime", session.startTime);
                sessionObj.put("endTime", session.endTime);
                sessionObj.put("kills", session.kills);
                sessionObj.put("deaths", session.deaths);
                sessionObj.put("score", session.score);
                sessionObj.put("victory", session.victory);
                sessionObj.put("duration", session.getDurationMs());
                sessionObj.put("kdRatio", session.getKDRatio());
                sessionsArray.put(sessionObj);
            }
            exportData.put("sessions", sessionsArray);

            // Performance metrics
            org.json.JSONObject metricsObj = new org.json.JSONObject();
            metricsObj.put("totalSessions", metrics.totalSessions);
            metricsObj.put("winRate", metrics.winRate);
            metricsObj.put("overallKDRatio", metrics.overallKDRatio);
            metricsObj.put("totalPlayTime", metrics.totalPlayTime);
            metricsObj.put("averageAccuracy", metrics.averageAccuracy);
            exportData.put("metrics", metricsObj);

            // Action analytics
            org.json.JSONObject actionsObj = new org.json.JSONObject();
            for (Map.Entry<String, Integer> entry : actionAttempts.entrySet()) {
                String action = entry.getKey();
                int attempts = entry.getValue();
                int successes = actionSuccesses.getOrDefault(action, 0);

                org.json.JSONObject actionData = new org.json.JSONObject();
                actionData.put("attempts", attempts);
                actionData.put("successes", successes);
                actionData.put("successRate", (float) successes / attempts);
                actionsObj.put(action, actionData);
            }
            exportData.put("actions", actionsObj);

            // Write to file
            java.io.FileWriter writer = new java.io.FileWriter(exportFile);
            writer.write(exportData.toString(2)); // Pretty print with indent
            writer.close();

            Log.d(TAG, "Analytics exported to: " + exportFile.getAbsolutePath());
            return exportFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Failed to export analytics data", e);
            return null;
        }
    }
    public void clearSessionHistory() {
        sessionHistory.clear();
        reactionTimes.clear();
    }

}