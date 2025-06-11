package com.gestureai.gameautomation;

/**
 * Basic game state representation for compatibility
 * Bridges to UniversalGameState for advanced AI features
 */
public class GameState {
    public float playerX;
    public float playerY;
    public float healthLevel;
    public float threatLevel;
    public int objectCount;
    public float gameScore;
    public long timestamp;
    
    public GameState() {
        this.playerX = 0f;
        this.playerY = 0f;
        this.healthLevel = 1.0f;
        this.threatLevel = 0f;
        this.objectCount = 0;
        this.gameScore = 0f;
        this.timestamp = System.currentTimeMillis();
    }
    
    public GameState(float playerX, float playerY, float healthLevel, float threatLevel) {
        this.playerX = playerX;
        this.playerY = playerY;
        this.healthLevel = healthLevel;
        this.threatLevel = threatLevel;
        this.objectCount = 0;
        this.gameScore = 0f;
        this.timestamp = System.currentTimeMillis();
    }
    
    public void updateState(float x, float y, float health, float threat) {
        this.playerX = x;
        this.playerY = y;
        this.healthLevel = health;
        this.threatLevel = threat;
        this.timestamp = System.currentTimeMillis();
    }
}