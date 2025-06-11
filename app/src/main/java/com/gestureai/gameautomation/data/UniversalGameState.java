package com.gestureai.gameautomation.data;

/**
 * Universal game state representation for AI agents
 * Contains all necessary game information for strategic decision making
 */
public class UniversalGameState {
    
    // Player position and status
    public float playerX;
    public float playerY;
    public float healthLevel;           // 0.0 to 1.0
    public float timeInGame;           // Game time in seconds
    
    // Game environment
    public float threatLevel;          // 0.0 to 1.0 (enemy proximity/danger)
    public float opportunityLevel;     // 0.0 to 1.0 (available actions/rewards)
    public int objectCount;           // Number of detected objects
    public float gameScore;           // Current score/progress
    
    // Screen and object information
    public int screenWidth;
    public int screenHeight;
    public int nearestObjectX;
    public int nearestObjectY;
    public String nearestObjectType;
    
    // Game-specific context
    public String gameType;           // "BATTLE_ROYALE", "MOBA", "FPS", etc.
    public String currentAction;      // Last executed action
    public float actionConfidence;    // Confidence in last action
    
    // Performance metrics
    public long timestamp;
    public float fps;
    public boolean isStable;
    
    // Additional fields for AI compatibility
    public boolean powerUpActive;
    public float speedLevel;
    public float powerLevel;
    public float difficultyLevel;
    public int consecutiveSuccess;
    public int nearestObstacleX;
    public int nearestObstacleY;
    
    public UniversalGameState() {
        // Initialize with default values
        this.playerX = 0f;
        this.playerY = 0f;
        this.healthLevel = 1.0f;
        this.timeInGame = 0f;
        this.threatLevel = 0f;
        this.opportunityLevel = 0f;
        this.objectCount = 0;
        this.gameScore = 0f;
        this.screenWidth = 1080;
        this.screenHeight = 1920;
        this.nearestObjectX = 0;
        this.nearestObjectY = 0;
        this.nearestObjectType = "unknown";
        this.gameType = "UNKNOWN";
        this.currentAction = "WAIT";
        this.actionConfidence = 0.5f;
        this.timestamp = System.currentTimeMillis();
        this.fps = 60f;
        this.isStable = true;
        this.powerUpActive = false;
        this.speedLevel = 1.0f;
        this.powerLevel = 1.0f;
        this.difficultyLevel = 0.5f;
        this.consecutiveSuccess = 0;
        this.nearestObstacleX = 0;
        this.nearestObstacleY = 0;
    }
    
    public UniversalGameState(float playerX, float playerY, float healthLevel, 
                            float threatLevel, float opportunityLevel, int objectCount) {
        this();
        this.playerX = playerX;
        this.playerY = playerY;
        this.healthLevel = healthLevel;
        this.threatLevel = threatLevel;
        this.opportunityLevel = opportunityLevel;
        this.objectCount = objectCount;
    }
    
    /**
     * Update state with new values
     */
    public void updateState(float playerX, float playerY, float healthLevel,
                          float threatLevel, float opportunityLevel) {
        this.playerX = playerX;
        this.playerY = playerY;
        this.healthLevel = healthLevel;
        this.threatLevel = threatLevel;
        this.opportunityLevel = opportunityLevel;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Set screen dimensions
     */
    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    /**
     * Set nearest object information
     */
    public void setNearestObject(int x, int y, String type) {
        this.nearestObjectX = x;
        this.nearestObjectY = y;
        this.nearestObjectType = type;
    }
    
    /**
     * Update performance metrics
     */
    public void updatePerformance(float fps, boolean isStable) {
        this.fps = fps;
        this.isStable = isStable;
    }
    
    /**
     * Get state as array for neural network input
     */
    public float[] toArray() {
        return new float[] {
            playerX / screenWidth,           // Normalized player X
            playerY / screenHeight,          // Normalized player Y
            healthLevel,                     // Health level (0-1)
            threatLevel,                     // Threat level (0-1)
            opportunityLevel,                // Opportunity level (0-1)
            (float)objectCount / 20f,        // Normalized object count
            gameScore / 1000f,               // Normalized game score
            timeInGame / 3600f,              // Normalized time (max 1 hour)
            nearestObjectX / (float)screenWidth,  // Normalized nearest object X
            nearestObjectY / (float)screenHeight, // Normalized nearest object Y
            actionConfidence,                // Last action confidence
            fps / 60f,                       // Normalized FPS
            isStable ? 1f : 0f,             // Stability flag
            powerUpActive ? 1f : 0f,        // Power-up status
            speedLevel,                     // Speed level
            powerLevel,                     // Power level
            difficultyLevel,                // Difficulty level
            consecutiveSuccess / 10f,       // Normalized success count
            nearestObstacleX / (float)screenWidth,  // Normalized obstacle X
            nearestObstacleY / (float)screenHeight  // Normalized obstacle Y
        };
    }
    
    /**
     * Clone the current state
     */
    public UniversalGameState clone() {
        UniversalGameState clone = new UniversalGameState();
        clone.playerX = this.playerX;
        clone.playerY = this.playerY;
        clone.healthLevel = this.healthLevel;
        clone.timeInGame = this.timeInGame;
        clone.threatLevel = this.threatLevel;
        clone.opportunityLevel = this.opportunityLevel;
        clone.objectCount = this.objectCount;
        clone.gameScore = this.gameScore;
        clone.screenWidth = this.screenWidth;
        clone.screenHeight = this.screenHeight;
        clone.nearestObjectX = this.nearestObjectX;
        clone.nearestObjectY = this.nearestObjectY;
        clone.nearestObjectType = this.nearestObjectType;
        clone.gameType = this.gameType;
        clone.currentAction = this.currentAction;
        clone.actionConfidence = this.actionConfidence;
        clone.timestamp = this.timestamp;
        clone.fps = this.fps;
        clone.isStable = this.isStable;
        clone.powerUpActive = this.powerUpActive;
        clone.speedLevel = this.speedLevel;
        clone.powerLevel = this.powerLevel;
        clone.difficultyLevel = this.difficultyLevel;
        clone.consecutiveSuccess = this.consecutiveSuccess;
        clone.nearestObstacleX = this.nearestObstacleX;
        clone.nearestObstacleY = this.nearestObstacleY;
        return clone;
    }
    
    @Override
    public String toString() {
        return String.format("GameState[pos=(%.1f,%.1f), health=%.2f, threat=%.2f, score=%.1f, objects=%d]",
                playerX, playerY, healthLevel, threatLevel, gameScore, objectCount);
    }
}