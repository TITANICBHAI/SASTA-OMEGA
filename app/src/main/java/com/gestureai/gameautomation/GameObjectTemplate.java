package com.gestureai.gameautomation;

/**
 * Template for game object detection and action mapping
 */
public class GameObjectTemplate {
    public final String name;
    public final String defaultAction;
    public final String description;
    public final int[] hsvLower;
    public final int[] hsvUpper;
    public final int minSize;
    public final int maxSize;
    public final float confidenceThreshold;

    public GameObjectTemplate(String name, String defaultAction, String description,
                            int[] hsvLower, int[] hsvUpper, int minSize, int maxSize, 
                            float confidenceThreshold) {
        this.name = name;
        this.defaultAction = defaultAction;
        this.description = description;
        this.hsvLower = hsvLower;
        this.hsvUpper = hsvUpper;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.confidenceThreshold = confidenceThreshold;
    }
}