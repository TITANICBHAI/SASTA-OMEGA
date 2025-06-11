package com.gestureai.gameautomation.utils;

import org.opencv.core.Rect;

/**
 * Template matching result data structure
 * Referenced in OpenCVHelper but was missing implementation
 */
public class TemplateMatch {
    
    public String objectName;
    public Rect boundingBox;
    public double confidence;
    public long timestamp;
    
    public TemplateMatch(String objectName, Rect boundingBox, double confidence) {
        this.objectName = objectName;
        this.boundingBox = boundingBox;
        this.confidence = confidence;
        this.timestamp = System.currentTimeMillis();
    }
    
    public int getCenterX() {
        return boundingBox.x + boundingBox.width / 2;
    }
    
    public int getCenterY() {
        return boundingBox.y + boundingBox.height / 2;
    }
    
    public int getArea() {
        return boundingBox.width * boundingBox.height;
    }
    
    @Override
    public String toString() {
        return String.format("TemplateMatch{object='%s', confidence=%.2f, center=(%d,%d)}", 
            objectName, confidence, getCenterX(), getCenterY());
    }
}