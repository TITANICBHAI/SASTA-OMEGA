package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.GameAction;

public class StrategyProcessor {
    private static final String TAG = "StrategyProcessor";
    
    private Context context;
    private GameStrategyAgent.GameType currentGameType;
    private String aiMode = "ADAPTIVE";
    private float aggressionLevel = 0.5f;
    
    public StrategyProcessor(Context context) {
        this.context = context;
        this.currentGameType = GameStrategyAgent.GameType.ARCADE;
        Log.d(TAG, "StrategyProcessor initialized");
    }
    
    public void setAIMode(String mode) {
        this.aiMode = mode;
        Log.d(TAG, "AI Mode set to: " + mode);
    }
    
    public void setGameType(GameStrategyAgent.GameType gameType) {
        this.currentGameType = gameType;
        Log.d(TAG, "Game type set to: " + gameType);
    }
    
    public void setAggressionLevel(float level) {
        this.aggressionLevel = Math.max(0f, Math.min(1f, level));
        Log.d(TAG, "Aggression level set to: " + aggressionLevel);
    }
    
    public GameAction processStrategy(GameStrategyAgent.UniversalGameState gameState) {
        try {
            switch (aiMode) {
                case "AGGRESSIVE":
                    return processAggressiveStrategy(gameState);
                case "DEFENSIVE":
                    return processDefensiveStrategy(gameState);
                case "LEARNING":
                    return processLearningStrategy(gameState);
                case "ADAPTIVE":
                default:
                    return processAdaptiveStrategy(gameState);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing strategy", e);
            return createFallbackAction(gameState);
        }
    }
    
    private GameAction processAggressiveStrategy(GameStrategyAgent.UniversalGameState gameState) {
        // Prioritize high-reward, high-risk actions
        if (gameState.opportunityLevel > 0.6f) {
            return new GameAction("TAP", gameState.nearestObjectX, gameState.nearestObjectY, 
                                0.9f, "aggressive_strategy");
        }
        return new GameAction("SWIPE_UP", gameState.playerX, gameState.playerY - 100, 
                            0.7f, "aggressive_movement");
    }
    
    private GameAction processDefensiveStrategy(GameStrategyAgent.UniversalGameState gameState) {
        // Prioritize safety and survival
        if (gameState.threatLevel > 0.5f) {
            return new GameAction("SWIPE_DOWN", gameState.playerX, gameState.playerY + 100, 
                                0.8f, "defensive_avoid");
        }
        return new GameAction("WAIT", gameState.playerX, gameState.playerY, 
                            0.6f, "defensive_wait");
    }
    
    private GameAction processLearningStrategy(GameStrategyAgent.UniversalGameState gameState) {
        // Explore different actions for learning
        String[] learningActions = {"TAP", "SWIPE_LEFT", "SWIPE_RIGHT", "LONG_PRESS"};
        String action = learningActions[(int)(Math.random() * learningActions.length)];
        
        return new GameAction(action, gameState.playerX + (int)(Math.random() * 200 - 100), 
                            gameState.playerY + (int)(Math.random() * 200 - 100), 
                            0.5f, "learning_exploration");
    }
    
    private GameAction processAdaptiveStrategy(GameStrategyAgent.UniversalGameState gameState) {
        // Balance between aggressive and defensive based on game state
        float adaptiveWeight = (gameState.opportunityLevel - gameState.threatLevel + 1f) / 2f;
        
        if (adaptiveWeight > 0.6f) {
            return processAggressiveStrategy(gameState);
        } else if (adaptiveWeight < 0.4f) {
            return processDefensiveStrategy(gameState);
        } else {
            // Balanced approach
            return new GameAction("TAP", gameState.nearestObjectX, gameState.nearestObjectY, 
                                0.7f, "adaptive_balanced");
        }
    }
    
    private GameAction createFallbackAction(GameStrategyAgent.UniversalGameState gameState) {
        return new GameAction("WAIT", gameState.playerX, gameState.playerY, 
                            0.5f, "fallback_action");
    }
    
    public String getCurrentAIMode() {
        return aiMode;
    }
    
    public GameStrategyAgent.GameType getCurrentGameType() {
        return currentGameType;
    }
    
    public float getAggressionLevel() {
        return aggressionLevel;
    }
}