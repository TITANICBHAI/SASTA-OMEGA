package com.gestureai.gameautomation.data;

/**
 * Game context class for strategic decision making
 * Provides context information for AI agents
 */
public class GameContext {
    
    public UniversalGameState gameState;
    public String gameType;
    public float difficulty;
    public long sessionTime;
    public boolean isActive;
    
    public GameContext() {
        this.gameState = new UniversalGameState();
        this.gameType = "UNKNOWN";
        this.difficulty = 0.5f;
        this.sessionTime = 0L;
        this.isActive = false;
    }
    
    public GameContext(UniversalGameState gameState, String gameType) {
        this.gameState = gameState;
        this.gameType = gameType;
        this.difficulty = 0.5f;
        this.sessionTime = System.currentTimeMillis();
        this.isActive = true;
    }
    
    public void updateContext(UniversalGameState newState) {
        this.gameState = newState;
        this.sessionTime = System.currentTimeMillis();
    }
    
    public boolean isEngagementSafe() {
        if (gameState == null) return false;
        return gameState.healthLevel > 0.3f && gameState.threatLevel < 0.7f;
    }
    
    public float getEngagementConfidence() {
        if (gameState == null) return 0.0f;
        return Math.max(0.0f, gameState.healthLevel - gameState.threatLevel);
    }
}