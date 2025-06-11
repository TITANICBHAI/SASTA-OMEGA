package com.gestureai.gameautomation.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "session_data")
public class SessionData {
    @PrimaryKey
    public long sessionId;
    
    // Session timing
    public long startTime;
    public long endTime;
    public long sessionDuration;
    
    // Game information
    public String gamePackage;
    public String gameType;
    public String automationMode;
    public int gameScore;
    
    // Action metrics (matching UnifiedServiceCoordinator expectations)
    public int totalActions;
    public int successfulActions;
    public long totalLatency;
    public float averageLatency;
    
    // AI performance metrics
    public float aiConfidence;
    public int objectsDetected;
    public int correctPredictions;
    public String aiStrategy;
    
    // Legacy fields (maintaining backward compatibility)
    public long duration;
    public int score;
    public float accuracy;
    public long timestamp;
    public int totalTouches;
    
    // Game-specific metrics
    public String gamePackageName;
    public String gameVersion;
    public String difficulty;
    
    public SessionData() {
        // Default constructor required by Room
        this.sessionId = System.currentTimeMillis();
        this.startTime = System.currentTimeMillis();
        this.timestamp = this.startTime;
        this.totalActions = 0;
        this.successfulActions = 0;
        this.totalLatency = 0;
        this.averageLatency = 0.0f;
        this.aiStrategy = "Default";
        this.automationMode = "UNIFIED_PIPELINE";
    }
    
    public SessionData(String gameType, String gamePackage, String automationMode) {
        this();
        this.gameType = gameType;
        this.gamePackage = gamePackage;
        this.automationMode = automationMode;
    }
    
    // Legacy constructor for backward compatibility
    public SessionData(String sessionIdStr, String gameType, long duration, int score) {
        this();
        this.gameType = gameType;
        this.duration = duration;
        this.score = score;
        this.gameScore = score;
        this.sessionDuration = duration;
    }
    
    public float getSuccessRate() {
        return totalActions > 0 ? (float) successfulActions / totalActions : 0.0f;
    }
    
    public float getDetectionAccuracy() {
        return objectsDetected > 0 ? (float) correctPredictions / objectsDetected : 0.0f;
    }
    
    public void updateMetrics(int actions, int successful, long latency) {
        this.totalActions += actions;
        this.successfulActions += successful;
        this.totalLatency += latency;
        this.averageLatency = this.totalActions > 0 ? (float) this.totalLatency / this.totalActions : 0.0f;
    }
    
    public void endSession() {
        this.endTime = System.currentTimeMillis();
        this.sessionDuration = this.endTime - this.startTime;
        this.duration = this.sessionDuration; // Legacy field sync
    }
}