
package com.gestureai.gameautomation;

import android.graphics.RectF;

public class GameObject {
    private String type; // "coin", "obstacle", "powerup", "character", etc.
    private RectF bounds;
    private float confidence;
    private float x, y, width, height;
    private String label;
    
    public GameObject(String type, float x, float y, float width, float height) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new RectF(x, y, x + width, y + height);
        this.confidence = 1.0f;
    }
    
    public GameObject(String type, RectF bounds) {
        this.type = type;
        this.bounds = new RectF(bounds);
        this.x = bounds.left;
        this.y = bounds.top;
        this.width = bounds.width();
        this.height = bounds.height();
        this.confidence = 1.0f;
    }
    
    public float getCenterX() {
        return x + width / 2;
    }
    
    public float getCenterY() {
        return y + height / 2;
    }
    
    public boolean intersects(GameObject other) {
        return this.bounds.intersect(other.bounds);
    }
    
    public float distanceTo(GameObject other) {
        float dx = this.getCenterX() - other.getCenterX();
        float dy = this.getCenterY() - other.getCenterY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    // Getters
    public String getType() { return type; }
    public RectF getBounds() { return bounds; }
    public float getConfidence() { return confidence; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public String getLabel() { return label; }
    
    // Setters
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public void setLabel(String label) { this.label = label; }
    public void setType(String type) { this.type = type; }
    
    // Utility methods for game logic
    public boolean isObstacle() {
        return type.equals("obstacle") || type.equals("barrier") || type.equals("enemy");
    }
    
    public boolean isCollectible() {
        return type.equals("coin") || type.equals("power_up") || type.equals("gem");
    }
}
