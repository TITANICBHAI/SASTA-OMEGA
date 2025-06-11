package com.gestureai.gameautomation.ai;

import android.util.Log;

public class GameReward {
    private static final String TAG = "GameReward";
    
    public enum RewardType {
        SCORE_INCREASE,
        LEVEL_COMPLETION,
        ENEMY_DEFEATED,
        ITEM_COLLECTED,
        OBJECTIVE_COMPLETED,
        SURVIVAL_TIME,
        ACCURACY_BONUS,
        SPEED_BONUS,
        PENALTY_AVOIDED,
        DEATH_PENALTY,
        TIME_PENALTY,
        FAILURE_PENALTY
    }
    
    private final RewardType type;
    private final float value;
    private final long timestamp;
    private final String context;
    private final boolean isPositive;
    
    public GameReward(RewardType type, float value, String context) {
        this.type = type;
        this.value = value;
        this.context = context;
        this.timestamp = System.currentTimeMillis();
        this.isPositive = value > 0;
        
        Log.d(TAG, "GameReward created: " + type + " = " + value + " (" + context + ")");
    }
    
    // Convenience constructors for common rewards
    public static GameReward scoreIncrease(float points) {
        return new GameReward(RewardType.SCORE_INCREASE, points, "score_gained");
    }
    
    public static GameReward levelComplete(float bonus) {
        return new GameReward(RewardType.LEVEL_COMPLETION, bonus, "level_completed");
    }
    
    public static GameReward enemyDefeated(float reward) {
        return new GameReward(RewardType.ENEMY_DEFEATED, reward, "enemy_eliminated");
    }
    
    public static GameReward itemCollected(float value) {
        return new GameReward(RewardType.ITEM_COLLECTED, value, "item_pickup");
    }
    
    public static GameReward objectiveCompleted(float bonus) {
        return new GameReward(RewardType.OBJECTIVE_COMPLETED, bonus, "objective_achieved");
    }
    
    public static GameReward survivalBonus(float timeAlive) {
        return new GameReward(RewardType.SURVIVAL_TIME, timeAlive * 0.1f, "survival_duration");
    }
    
    public static GameReward accuracyBonus(float accuracy) {
        return new GameReward(RewardType.ACCURACY_BONUS, accuracy * 10f, "accuracy_achievement");
    }
    
    public static GameReward speedBonus(float speed) {
        return new GameReward(RewardType.SPEED_BONUS, speed * 5f, "speed_achievement");
    }
    
    public static GameReward penaltyAvoided(float value) {
        return new GameReward(RewardType.PENALTY_AVOIDED, value, "penalty_avoided");
    }
    
    // Penalty rewards (negative values)
    public static GameReward deathPenalty() {
        return new GameReward(RewardType.DEATH_PENALTY, -100f, "player_death");
    }
    
    public static GameReward timePenalty(float overtime) {
        return new GameReward(RewardType.TIME_PENALTY, -overtime * 2f, "time_exceeded");
    }
    
    public static GameReward failurePenalty(float severity) {
        return new GameReward(RewardType.FAILURE_PENALTY, -severity, "action_failed");
    }
    
    // Calculate reward value based on game context
    public float getAdjustedValue(GameStrategyAgent.GameType gameType) {
        float multiplier = 1.0f;
        
        switch (gameType) {
            case BATTLE_ROYALE:
                // Survival and elimination rewards are more valuable
                if (type == RewardType.SURVIVAL_TIME || type == RewardType.ENEMY_DEFEATED) {
                    multiplier = 2.0f;
                }
                break;
                
            case MOBA:
                // Objective and teamwork rewards are more valuable
                if (type == RewardType.OBJECTIVE_COMPLETED || type == RewardType.ENEMY_DEFEATED) {
                    multiplier = 1.5f;
                }
                break;
                
            case FPS:
                // Accuracy and elimination rewards are more valuable
                if (type == RewardType.ACCURACY_BONUS || type == RewardType.ENEMY_DEFEATED) {
                    multiplier = 1.8f;
                }
                break;
                
            case RACING:
                // Speed and completion rewards are more valuable
                if (type == RewardType.SPEED_BONUS || type == RewardType.LEVEL_COMPLETION) {
                    multiplier = 1.6f;
                }
                break;
                
            case PUZZLE:
                // Accuracy and objective rewards are more valuable
                if (type == RewardType.ACCURACY_BONUS || type == RewardType.OBJECTIVE_COMPLETED) {
                    multiplier = 1.4f;
                }
                break;
                
            case ARCADE:
            default:
                // Standard scoring
                multiplier = 1.0f;
                break;
        }
        
        return value * multiplier;
    }
    
    // Getters
    public RewardType getType() {
        return type;
    }
    
    public float getValue() {
        return value;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getContext() {
        return context;
    }
    
    public boolean isPositive() {
        return isPositive;
    }
    
    public boolean isPenalty() {
        return !isPositive;
    }
    
    @Override
    public String toString() {
        return "GameReward{" +
                "type=" + type +
                ", value=" + value +
                ", context='" + context + '\'' +
                ", timestamp=" + timestamp +
                ", isPositive=" + isPositive +
                '}';
    }
}