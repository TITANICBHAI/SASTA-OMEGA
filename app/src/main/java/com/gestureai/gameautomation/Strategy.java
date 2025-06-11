package com.gestureai.gameautomation;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Strategy {
    private String name;
    private String description;
    private String gameType;
    private List<StrategyRule> rules;
    private Map<String, Object> parameters;
    private float successRate;
    private int priority;
    private boolean isActive;

    public Strategy(String name, String description, String gameType) {
        this.name = name;
        this.description = description;
        this.gameType = gameType;
        this.rules = new ArrayList<>();
        this.parameters = new HashMap<>();
        this.successRate = 0.0f;
        this.priority = 1;
        this.isActive = true;
    }

    public static class StrategyRule {
        public String condition;
        public String action;
        public float priority;
        public Map<String, Object> actionParams;

        public StrategyRule(String condition, String action, float priority) {
            this.condition = condition;
            this.action = action;
            this.priority = priority;
            this.actionParams = new HashMap<>();
        }
    }

    public void addRule(String condition, String action, float priority) {
        rules.add(new StrategyRule(condition, action, priority));
    }

    public GameAction evaluateRules(GameState gameState) {
        for (StrategyRule rule : rules) {
            if (matchesCondition(rule.condition, gameState)) {
                return createActionFromRule(rule, gameState);
            }
        }
        return null;
    }

    private boolean matchesCondition(String condition, GameState gameState) {
        condition = condition.toLowerCase();
        
        if (condition.contains("obstacle") && gameState.hasObstacle) {
            return true;
        }
        if (condition.contains("coin") && gameState.hasCoin) {
            return true;
        }
        if (condition.contains("powerup") && gameState.hasPowerUp) {
            return true;
        }
        if (condition.contains("game active") && gameState.isGameActive) {
            return true;
        }
        
        return false;
    }

    private GameAction createActionFromRule(StrategyRule rule, GameState gameState) {
        GameAction action = new GameAction(rule.action, 0, 0, rule.priority / 10.0f, "");

        // Set coordinates based on action type
        switch (rule.action.toUpperCase()) {
            case "JUMP":
                return GameAction.createSwipe(
                    Constants.JUMP_START_X, Constants.JUMP_START_Y,
                    Constants.JUMP_END_X, Constants.JUMP_END_Y,
                    Constants.SWIPE_DURATION
                );
            case "SLIDE":
                return GameAction.createSwipe(
                    Constants.SLIDE_START_X, Constants.SLIDE_START_Y,
                    Constants.SLIDE_END_X, Constants.SLIDE_END_Y,
                    Constants.SWIPE_DURATION
                );
            case "MOVE_LEFT":
                return GameAction.createSwipe(
                    Constants.MOVE_LEFT_START_X, Constants.MOVE_LEFT_START_Y,
                    Constants.MOVE_LEFT_END_X, Constants.MOVE_LEFT_END_Y,
                    Constants.SWIPE_DURATION
                );
            case "MOVE_RIGHT":
                return GameAction.createSwipe(
                    Constants.MOVE_RIGHT_START_X, Constants.MOVE_RIGHT_START_Y,
                    Constants.MOVE_RIGHT_END_X, Constants.MOVE_RIGHT_END_Y,
                    Constants.SWIPE_DURATION
                );
            case "TAP":
            case "COLLECT":
                return GameAction.createTap(
                    Constants.SCREEN_WIDTH_REFERENCE / 2,
                    Constants.SCREEN_HEIGHT_REFERENCE / 2
                );
        }
        
        return action;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getGameType() { return gameType; }
    public List<StrategyRule> getRules() { return rules; }
    public Map<String, Object> getParameters() { return parameters; }
    public float getSuccessRate() { return successRate; }
    public int getPriority() { return priority; }
    public boolean isActive() { return isActive; }

    public void setSuccessRate(float successRate) { this.successRate = successRate; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setActive(boolean active) { this.isActive = active; }

    // Default strategies for Subway Surfers
    public static Strategy createSubwaySurfersStrategy() {
        Strategy strategy = new Strategy("Subway Surfers Auto", "Automated gameplay for Subway Surfers", "runner");
        
        strategy.addRule("obstacle ahead", "JUMP", 10.0f);
        strategy.addRule("train coming", "SLIDE", 10.0f);
        strategy.addRule("barrier ahead", "JUMP", 9.0f);
        strategy.addRule("coin visible", "COLLECT", 5.0f);
        strategy.addRule("powerup available", "COLLECT", 7.0f);
        strategy.addRule("jetpack visible", "COLLECT", 8.0f);
        strategy.addRule("magnet visible", "COLLECT", 7.0f);
        
        return strategy;
    }

    public static Strategy createConservativeStrategy() {
        Strategy strategy = new Strategy("Conservative Play", "Focus on survival over collection", "runner");
        
        strategy.addRule("obstacle ahead", "JUMP", 10.0f);
        strategy.addRule("train coming", "SLIDE", 10.0f);
        strategy.addRule("barrier ahead", "JUMP", 10.0f);
        strategy.addRule("powerup available", "COLLECT", 3.0f);
        strategy.addRule("coin visible", "COLLECT", 2.0f);
        
        return strategy;
    }

    public static Strategy createAggressiveStrategy() {
        Strategy strategy = new Strategy("Aggressive Collection", "Maximize coin and powerup collection", "runner");
        
        strategy.addRule("obstacle ahead", "JUMP", 8.0f);
        strategy.addRule("train coming", "SLIDE", 8.0f);
        strategy.addRule("coin visible", "COLLECT", 9.0f);
        strategy.addRule("powerup available", "COLLECT", 10.0f);
        strategy.addRule("jetpack visible", "COLLECT", 10.0f);
        strategy.addRule("magnet visible", "COLLECT", 9.0f);
        
        return strategy;
    }
}