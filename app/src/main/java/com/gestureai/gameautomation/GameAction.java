package com.gestureai.gameautomation;

/**
 * Represents a game action to be executed
 */
public class GameAction {
    private final String actionType;
    private final int x;
    private final int y;
    private final float confidence;
    private final String objectName;
    private final long timestamp;
    private final float priority;

    public GameAction(String actionType, int x, int y, float confidence, String objectName) {
        this.actionType = actionType;
        this.x = x;
        this.y = y;
        this.confidence = confidence;
        this.objectName = objectName;
        this.timestamp = System.currentTimeMillis();
        this.priority = confidence; // Default priority equals confidence
    }

    public GameAction(String actionType, int x, int y, float confidence, String objectName, float priority) {
        this.actionType = actionType;
        this.x = x;
        this.y = y;
        this.confidence = confidence;
        this.objectName = objectName;
        this.timestamp = System.currentTimeMillis();
        this.priority = priority;
    }

    // Getters
    public String getActionType() { return actionType; }
    public int getX() { return x; }
    public int getY() { return y; }
    public float getConfidence() { return confidence; }
    public String getObjectName() { return objectName; }
    public long getTimestamp() { return timestamp; }
    public float getPriority() { return priority; }

    @Override
    public String toString() {
        return String.format("GameAction{type='%s', pos=(%d,%d), confidence=%.2f, object='%s'}", 
                           actionType, x, y, confidence, objectName);
    }
    public static GameAction createSwipe(int startX, int startY, int endX, int endY, long duration) {
        GameAction action = new GameAction("SWIPE", startX, startY, 0.8f, "swipe_gesture");
        return action;
    }

    public static GameAction createTap(int x, int y) {
        GameAction action = new GameAction("TAP", x, y, 0.9f, "tap_gesture");
        return action;
    }
}