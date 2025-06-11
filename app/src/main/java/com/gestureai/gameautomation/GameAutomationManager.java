package com.gestureai.gameautomation;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.database.AppDatabase;
import com.gestureai.gameautomation.data.SessionData;
import com.gestureai.gameautomation.messaging.EventBus;
import com.gestureai.gameautomation.synchronization.AIComponentSynchronizer;
import com.gestureai.gameautomation.ai.GameStrategyAgent;

public class GameAutomationManager {
    private static final String TAG = "GameAutomationManager";
    private static GameAutomationManager instance;
    
    private Context context;
    private boolean isRunning = false;
    private GameAutomationEngine automationEngine;
    private AppDatabase database;
    private EventBus eventBus;
    private AIComponentSynchronizer synchronizer;
    private GameStrategyAgent strategyAgent;
    
    // Current session tracking
    private SessionData currentSession;
    private String currentStrategy = "Adaptive AI";
    private String currentGameType = "Auto-Detect";
    private int reactionSpeed = 50;
    private boolean autoGameDetectionEnabled = false;
    
    private GameAutomationManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.eventBus = EventBus.getInstance();
        this.synchronizer = AIComponentSynchronizer.getInstance();
        this.automationEngine = GameAutomationEngine.getInstance();
        this.strategyAgent = new GameStrategyAgent(context);
        
        Log.d(TAG, "GameAutomationManager initialized with database and event bus");
    }
    
    public static GameAutomationManager getInstance(Context context) {
        if (instance == null) {
            synchronized (GameAutomationManager.class) {
                if (instance == null) {
                    instance = new GameAutomationManager(context);
                }
            }
        }
        return instance;
    }
    
    public void startAutomation() {
        if (!isRunning && synchronizer.canAccessNeuralNetwork(TAG)) {
            isRunning = true;
            
            // Create new session
            currentSession = new SessionData(
                "Session_" + System.currentTimeMillis(),
                currentGameType,
                currentStrategy
            );
            // Execute database operations on background thread
            new Thread(() -> {
                try {
                    long sessionId = database.sessionDataDao().insertSession(currentSession);
                    currentSession.id = sessionId;
                    Log.d(TAG, "Session created with ID: " + sessionId);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create session", e);
                }
            }).start();
            
            // Start automation engine
            if (automationEngine != null) {
                automationEngine.setAutomationEnabled(true);
            }
            
            // Start strategy agent
            if (strategyAgent != null) {
                try {
                    GameStrategyAgent.GameType gameType = GameStrategyAgent.GameType.valueOf(currentGameType.toUpperCase().replace("-", "_"));
                    strategyAgent.setGameType(gameType);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown game type: " + currentGameType + ", using ARCADE as fallback");
                    strategyAgent.setGameType(GameStrategyAgent.GameType.ARCADE);
                }
            }
            
            // Post event
            eventBus.post(new EventBus.ServiceStatusChangedEvent(TAG, "GameAutomation", true, "Automation started"));
            
            Log.d(TAG, "Automation started with session ID: " + (currentSession != null ? currentSession.id : "unknown"));
        }
    }
    
    public void stopAutomation() {
        if (isRunning) {
            isRunning = false;
            
            // End current session on background thread
            if (currentSession != null) {
                currentSession.endTime = System.currentTimeMillis();
                final SessionEntity sessionToUpdate = currentSession;
                new Thread(() -> {
                    try {
                        database.sessionDataDao().updateSession(sessionToUpdate);
                        Log.d(TAG, "Session updated successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to update session", e);
                    }
                }).start();
            }
            
            // Stop automation engine
            if (automationEngine != null) {
                automationEngine.setAutomationEnabled(false);
            }
            
            // Post event
            eventBus.post(new EventBus.ServiceStatusChangedEvent(TAG, "GameAutomation", false, "Automation stopped"));
            
            Log.d(TAG, "Automation stopped");
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void setStrategy(String strategy) {
        this.currentStrategy = strategy;
        if (strategyAgent != null && isRunning) {
            // Update strategy in real-time
            strategyAgent.updateStrategy(strategy);
        }
        Log.d(TAG, "Strategy set to: " + strategy);
    }
    
    public void setReactionSpeed(int speed) {
        this.reactionSpeed = speed;
        if (automationEngine != null) {
            automationEngine.setReactionSpeed(speed);
        }
        Log.d(TAG, "Reaction speed set to: " + speed + "%");
    }
    
    public void setGameType(String gameType) {
        this.currentGameType = gameType;
        if (strategyAgent != null) {
            try {
                GameStrategyAgent.GameType type = GameStrategyAgent.GameType.valueOf(gameType.toUpperCase().replace("-", "_"));
                strategyAgent.setGameType(type);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unknown game type: " + gameType + ", using ARCADE as fallback");
                strategyAgent.setGameType(GameStrategyAgent.GameType.ARCADE);
            }
        }
        Log.d(TAG, "Game type set to: " + gameType);
    }
    
    public void enableAutoGameDetection(boolean enabled) {
        this.autoGameDetectionEnabled = enabled;
        if (automationEngine != null) {
            automationEngine.setAutoGameDetection(enabled);
        }
        Log.d(TAG, "Auto game detection: " + (enabled ? "enabled" : "disabled"));
    }
    
    public void startGameTypeDetection() {
        if (automationEngine != null && autoGameDetectionEnabled) {
            automationEngine.startGameTypeDetection();
            Log.d(TAG, "Game type detection started");
        }
    }
    
    public SessionEntity getCurrentSession() {
        return currentSession;
    }
    
    public String getCurrentStrategy() {
        return currentStrategy;
    }
    
    public String getCurrentGameType() {
        return currentGameType;
    }
    
    public int getReactionSpeed() {
        return reactionSpeed;
    }
    
    public boolean isAutoGameDetectionEnabled() {
        return autoGameDetectionEnabled;
    }
    
    public void cleanup() {
        if (isRunning) {
            stopAutomation();
        }
        
        if (automationEngine != null) {
            automationEngine.cleanup();
        }
        
        if (strategyAgent != null) {
            strategyAgent.cleanup();
        }
        
        Log.d(TAG, "GameAutomationManager cleaned up");
    }
}