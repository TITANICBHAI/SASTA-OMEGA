package com.gestureai.gameautomation;

import android.graphics.Rect;

/**
 * Represents a bounding box for object detection and labeling
 */
public class BoundingBox {
    public int x;
    public int y;
    public int width;
    public int height;
    public String label;
    public float confidence;
    
    public BoundingBox(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confidence = 1.0f;
    }
    
    public BoundingBox(float x, float y, float width, float height) {
        this.x = (int) x;
        this.y = (int) y;
        this.width = (int) width;
        this.height = (int) height;
        this.confidence = 1.0f;
    }
    
    public BoundingBox(int x, int y, int width, int height, String label, float confidence) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.confidence = confidence;
    }
    
    public Rect toRect() {
        return new Rect(x, y, x + width, y + height);
    }
    
    public int centerX() {
        return x + width / 2;
    }
    
    public int centerY() {
        return y + height / 2;
    }
    
    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX <= x + width && 
               pointY >= y && pointY <= y + height;
    }
    
    public float area() {
        return width * height;
    }
    
    public float intersectionOverUnion(BoundingBox other) {
        int intersectionLeft = Math.max(x, other.x);
        int intersectionTop = Math.max(y, other.y);
        int intersectionRight = Math.min(x + width, other.x + other.width);
        int intersectionBottom = Math.min(y + height, other.y + other.height);
        
        if (intersectionLeft >= intersectionRight || intersectionTop >= intersectionBottom) {
            return 0.0f;
        }
        
        float intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop);
        float unionArea = area() + other.area() - intersectionArea;
        
        return intersectionArea / unionArea;
    }
}