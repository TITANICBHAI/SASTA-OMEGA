package com.gestureai.gameautomation.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "sessions")
public class SessionData {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "session_name")
    public String sessionName;

    @ColumnInfo(name = "game_type")
    public String gameType;

    @ColumnInfo(name = "start_time")
    public long startTime;

    @ColumnInfo(name = "end_time")
    public long endTime;

    @ColumnInfo(name = "total_actions")
    public int totalActions;

    @ColumnInfo(name = "successful_actions")
    public int successfulActions;

    @ColumnInfo(name = "average_reaction_time")
    public float averageReactionTime;

    @ColumnInfo(name = "score_achieved")
    public int scoreAchieved;

    @ColumnInfo(name = "ai_strategy")
    public String aiStrategy;

    @ColumnInfo(name = "performance_rating")
    public float performanceRating;

    @ColumnInfo(name = "notes")
    public String notes;

    // Enhanced fields from original SessionData
    @ColumnInfo(name = "automation_enabled")
    public boolean automationEnabled;

    @ColumnInfo(name = "performance_metrics")
    public String performanceMetrics; // JSON string for complex data

    @ColumnInfo(name = "learning_data")
    public String learningData; // JSON string for ML training data

    @ColumnInfo(name = "error_count")
    public int errorCount;

    public SessionData() {
        // Required empty constructor for Room
        this.startTime = System.currentTimeMillis();
        this.gameType = "UNKNOWN";
        this.scoreAchieved = 0;
        this.totalActions = 0;
        this.successfulActions = 0;
        this.performanceRating = 0.0f;
        this.automationEnabled = false;
        this.aiStrategy = "NONE";
        this.performanceMetrics = "{}";
        this.learningData = "{}";
        this.errorCount = 0;
        this.averageReactionTime = 0.0f;
    }

    public SessionData(String sessionName, String gameType, String aiStrategy) {
        this.sessionName = sessionName;
        this.gameType = gameType;
        this.aiStrategy = aiStrategy;
        this.startTime = System.currentTimeMillis();
        this.totalActions = 0;
        this.successfulActions = 0;
        this.averageReactionTime = 0f;
        this.scoreAchieved = 0;
        this.performanceRating = 0f;
        this.automationEnabled = false;
        this.performanceMetrics = "{}";
        this.learningData = "{}";
        this.errorCount = 0;
    }

    // Getter and setter methods for Room compatibility
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public int getScoreAchieved() {
        return scoreAchieved;
    }

    public void setScoreAchieved(int scoreAchieved) {
        this.scoreAchieved = scoreAchieved;
    }

    public int getTotalActions() {
        return totalActions;
    }

    public void setTotalActions(int totalActions) {
        this.totalActions = totalActions;
    }

    public int getSuccessfulActions() {
        return successfulActions;
    }

    public void setSuccessfulActions(int successfulActions) {
        this.successfulActions = successfulActions;
    }

    public float getSuccessRate() {
        return totalActions > 0 ? (float) successfulActions / totalActions : 0f;
    }

    public float getPerformanceRating() {
        return performanceRating;
    }

    public void setPerformanceRating(float performanceRating) {
        this.performanceRating = performanceRating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isAutomationEnabled() {
        return automationEnabled;
    }

    public void setAutomationEnabled(boolean automationEnabled) {
        this.automationEnabled = automationEnabled;
    }

    public String getAiStrategy() {
        return aiStrategy;
    }

    public void setAiStrategy(String aiStrategy) {
        this.aiStrategy = aiStrategy;
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

    public float getAverageReactionTime() {
        return averageReactionTime;
    }

    public void setAverageReactionTime(float averageReactionTime) {
        this.averageReactionTime = averageReactionTime;
    }

    // Utility methods
    public void incrementTotalActions() {
        this.totalActions++;
    }

    public void incrementSuccessfulActions() {
        this.successfulActions++;
    }

    public void incrementErrorCount() {
        this.errorCount++;
    }

    public long getDuration() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public void endSession() {
        this.endTime = System.currentTimeMillis();
    }
}