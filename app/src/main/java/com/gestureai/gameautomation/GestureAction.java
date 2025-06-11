package com.gestureai.gameautomation;

public class GestureAction {
    private String actionType;
    public int startX, startY, endX, endY;
    private long duration;

    public GestureAction(String actionType, int startX, int startY, int endX, int endY) {
        this.actionType = actionType;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.duration = 200; // default duration
    }

    public GestureAction(String actionType, int startX, int startY, int endX, int endY, long duration) {
        this.actionType = actionType;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.duration = duration;
    }

    // Getters
    public String getActionType() { return actionType; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getEndX() { return endX; }
    public int getEndY() { return endY; }
    public long getDuration() { return duration; }

    // Setters
    public void setDuration(long duration) { this.duration = duration; }
}