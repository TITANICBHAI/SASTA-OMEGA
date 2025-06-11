package com.gestureai.gameautomation.models;

import android.graphics.Point;

public class GestureResult {
    private final String type;
    private final float confidence;
    private final Point centerPoint;
    private final long timestamp;

    public GestureResult(String type, float confidence, Point centerPoint) {
        this.type = type;
        this.confidence = confidence;
        this.centerPoint = centerPoint;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public float getConfidence() {
        return confidence;
    }

    public Point getCenterPoint() {
        return centerPoint;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public boolean isValid() {
        return type != null && !type.isEmpty() && confidence > 0.0f && centerPoint != null;
    }

    @Override
    public String toString() {
        return String.format("GestureResult{type='%s', confidence=%.2f, center=(%d,%d)}", 
                           type, confidence, centerPoint.x, centerPoint.y);
    }
}