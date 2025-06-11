package com.gestureai.gameautomation;

import android.graphics.Rect;

/**
 * Represents a detected object in the game screen
 */
public class DetectedObject {
    public final String name;
    public final Rect boundingRect;
    public final float confidence;
    public final String action;
    public final String description;


    public DetectedObject(String name, Rect boundingRect, float confidence,
                          String action, String description) {
        this.name = name;
        this.boundingRect = boundingRect;
        this.confidence = confidence;
        this.action = action;

        this.description = description;
    }

    public String getName() {
        return name;
    }

    public static class Builder {
        private String name;
        private Rect boundingRect;
        private float confidence;
        private String action;
        private String description;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setBoundingRect(Rect boundingRect) {
            this.boundingRect = boundingRect;
            return this;
        }

        public Builder setConfidence(float confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder setAction(String action) {
            this.action = action;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public DetectedObject build() {
            return new DetectedObject(name, boundingRect, confidence, action, description);
        }
    }

}