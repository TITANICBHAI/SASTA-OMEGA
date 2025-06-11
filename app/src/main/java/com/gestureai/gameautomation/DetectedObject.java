package com.gestureai.gameautomation;

import android.graphics.Rect;

/**
 * Enhanced DetectedObject with reasoning integration and semantic analysis
 */
public class DetectedObject {
    private String label;
    private float confidence;
    private Rect bounds;
    private String action;
    private String description;
    
    // Enhanced reasoning integration
    private boolean reasoningRelevance = false;
    private String reasoningContext;
    private float semanticRelevanceScore = 0.0f;
    private String strategicImportance;
    private long timestamp;

    public DetectedObject(String label, float confidence, Rect bounds) {
        this.label = label;
        this.confidence = confidence;
        this.bounds = bounds;
        this.timestamp = System.currentTimeMillis();
    }

    public DetectedObject(String label, Rect boundingRect, float confidence,
                          String action, String description) {
        this.label = label;
        this.bounds = boundingRect;
        this.confidence = confidence;
        this.action = action;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
    }

    // Basic getters and setters
    public String getLabel() { return label; }
    public String getName() { return label; } // Backward compatibility
    public float getConfidence() { return confidence; }
    public Rect getBounds() { return bounds; }
    public Rect getBoundingRect() { return bounds; } // Backward compatibility
    public String getAction() { return action; }
    public String getDescription() { return description; }
    public long getTimestamp() { return timestamp; }

    // Enhanced reasoning getters and setters
    public boolean isReasoningRelevance() { return reasoningRelevance; }
    public void setReasoningRelevance(boolean relevant) { this.reasoningRelevance = relevant; }
    public String getReasoningContext() { return reasoningContext; }
    public void setReasoningContext(String context) { this.reasoningContext = context; }
    public float getSemanticRelevanceScore() { return semanticRelevanceScore; }
    public void setSemanticRelevanceScore(float score) { this.semanticRelevanceScore = score; }
    public String getStrategicImportance() { return strategicImportance; }
    public void setStrategicImportance(String importance) { this.strategicImportance = importance; }
    
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public void setLabel(String label) { this.label = label; }
    public void setBounds(Rect bounds) { this.bounds = bounds; }
    public void setAction(String action) { this.action = action; }
    public void setDescription(String description) { this.description = description; }

    public static class Builder {
        private String label;
        private Rect boundingRect;
        private float confidence;
        private String action;
        private String description;

        public Builder setName(String name) {
            this.label = name;
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
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
            return new DetectedObject(label, boundingRect, confidence, action, description);
        }
    }

}