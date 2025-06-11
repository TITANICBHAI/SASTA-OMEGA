package com.gestureai.gameautomation;

/**
 * Universal game state representation
 * Captures essential game metrics for AI decision making
 */
public class GameState {
    public float health = 100.0f;
    public int ammo = 30;
    public int score = 0;
    public float playerX = 0.0f;
    public float playerY = 0.0f;
    public float confidence = 0.0f;
    public String currentWeapon = "default";
    public boolean inCover = false;
    public boolean hasShield = false;
    public int lives = 3;
    public float zoneDistance = 0.0f;
    public long gameTime = 0L;
    
    // Team-based game features
    public int teamScore = 0;
    public int enemyTeamScore = 0;
    public boolean isTeamGame = false;
    
    // Resource management
    public int coins = 0;
    public int powerups = 0;
    public float energy = 100.0f;
    
    public GameState() {
        this.gameTime = System.currentTimeMillis();
    }
    
    public GameState(float health, int ammo, int score) {
        this();
        this.health = health;
        this.ammo = ammo;
        this.score = score;
    }
    
    public float getHealthPercentage() {
        return Math.max(0, Math.min(100, health)) / 100.0f;
    }
    
    public float getAmmoPercentage() {
        return Math.max(0, Math.min(100, ammo)) / 100.0f;
    }
    
    public boolean isLowHealth() {
        return health < 30.0f;
    }
    
    public boolean isLowAmmo() {
        return ammo < 10;
    }
    
    public boolean isCriticalState() {
        return isLowHealth() || (isLowAmmo() && !inCover);
    }
}