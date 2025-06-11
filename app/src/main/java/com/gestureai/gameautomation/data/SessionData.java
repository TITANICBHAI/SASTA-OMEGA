package com.gestureai.gameautomation.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "session_data")
public class SessionData {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "session_start_time")
    public long sessionStartTime;

    @ColumnInfo(name = "session_end_time")
    public long sessionEndTime;

    @ColumnInfo(name = "game_type")
    public String gameType;

    @ColumnInfo(name = "total_score")
    public int totalScore;

    @ColumnInfo(name = "actions_performed")
    public int actionsPerformed;

    @ColumnInfo(name = "success_rate")
    public float successRate;

    @ColumnInfo(name = "automation_enabled")
    public boolean automationEnabled;

    @ColumnInfo(name = "ai_strategy_used")
    public String aiStrategyUsed;

    @ColumnInfo(name = "performance_metrics")
    public String performanceMetrics; // JSON string for complex data

    @ColumnInfo(name = "learning_data")
    public String learningData; // JSON string for ML training data

    @ColumnInfo(name = "error_count")
    public int errorCount;

    @ColumnInfo(name = "average_response_time")
    public float averageResponseTime;

    public SessionData() {
        // Required empty constructor for Room
        this.sessionStartTime = System.currentTimeMillis();
        this.gameType = "UNKNOWN";
        this.totalScore = 0;
        this.actionsPerformed = 0;
        this.successRate = 0.0f;
        this.automationEnabled = false;
        this.aiStrategyUsed = "NONE";
        this.performanceMetrics = "{}";
        this.learningData = "{}";
        this.errorCount = 0;
        this.averageResponseTime = 0.0f;
    }

    public SessionData(String gameType, boolean automationEnabled) {
        this();
        this.gameType = gameType;
        this.automationEnabled = automationEnabled;
    }

    // Getter and setter methods for Room compatibility
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionStartTime() {
        return sessionStartTime;
    }

    public void setSessionStartTime(long sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    public long getSessionEndTime() {
        return sessionEndTime;
    }

    public void setSessionEndTime(long sessionEndTime) {
        this.sessionEndTime = sessionEndTime;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getActionsPerformed() {
        return actionsPerformed;
    }

    public void setActionsPerformed(int actionsPerformed) {
        this.actionsPerformed = actionsPerformed;
    }

    public float getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(float successRate) {
        this.successRate = successRate;
    }

    public boolean isAutomationEnabled() {
        return automationEnabled;
    }

    public void setAutomationEnabled(boolean automationEnabled) {
        this.automationEnabled = automationEnabled;
    }

    public String getAiStrategyUsed() {
        return aiStrategyUsed;
    }

    public void setAiStrategyUsed(String aiStrategyUsed) {
        this.aiStrategyUsed = aiStrategyUsed;
    }

    public String getPerformanceMetrics() {
        return performanceMetrics;
    }

    public void setPerformanceMetrics(String performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }

    public String getLearningData() {
        return learningData;
    }

    public void setLearningData(String learningData) {
        this.learningData = learningData;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public float getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(float averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    // Utility methods
    public void incrementActionsPerformed() {
        this.actionsPerformed++;
    }

    public void incrementErrorCount() {
        this.errorCount++;
    }

    public void updateSuccessRate(int successfulActions) {
        if (actionsPerformed > 0) {
            this.successRate = (float) successfulActions / actionsPerformed;
        }
    }

    public long getSessionDuration() {
        if (sessionEndTime > 0) {
            return sessionEndTime - sessionStartTime;
        }
        return System.currentTimeMillis() - sessionStartTime;
    }

    public void endSession() {
        this.sessionEndTime = System.currentTimeMillis();
    }
}