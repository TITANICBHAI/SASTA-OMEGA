package com.gestureai.gameautomation.workflow.recording;

import android.graphics.Bitmap;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced recorded action with AI analysis and context information
 */
public class EnhancedRecordedAction extends RecordedAction {
    private String description;
    private GameContext context;
    private GestureType gestureType;
    private ActionIntent intent;
    private Bitmap screenshot;
    private Map<String, Object> metadata;
    private float confidence;
    
    public enum GestureType {
        SINGLE_TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_UP, SWIPE_DOWN, 
        SWIPE_LEFT, SWIPE_RIGHT, PINCH, ZOOM, COMPLEX_GESTURE
    }
    
    public enum ActionIntent {
        NAVIGATION, SELECTION, INPUT, INTERACTION, MENU_ACCESS,
        GAME_ACTION, COLLECTION, COMBAT, MOVEMENT, UNKNOWN
    }
    
    public EnhancedRecordedAction(RecordedAction baseAction) {
        super(baseAction.getType(), baseAction.getX(), baseAction.getY(), baseAction.getTimestamp());
        
        // Copy base properties
        setStartX(baseAction.getStartX());
        setStartY(baseAction.getStartY());
        setEndX(baseAction.getEndX());
        setEndY(baseAction.getEndY());
        setDuration(baseAction.getDuration());
        setKeyCode(baseAction.getKeyCode());
        setText(baseAction.getText());
        
        this.metadata = new HashMap<>();
        this.confidence = 1.0f;
    }
    
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    public boolean hasScreenshot() {
        return screenshot != null;
    }
    
    public boolean hasContext() {
        return context != null;
    }
    
    public boolean isHighConfidence() {
        return confidence > 0.8f;
    }
    
    // Getters and setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public GameContext getContext() { return context; }
    public void setContext(GameContext context) { this.context = context; }
    
    public GestureType getGestureType() { return gestureType; }
    public void setGestureType(GestureType gestureType) { this.gestureType = gestureType; }
    
    public ActionIntent getIntent() { return intent; }
    public void setIntent(ActionIntent intent) { this.intent = intent; }
    
    public Bitmap getScreenshot() { return screenshot; }
    public void setScreenshot(Bitmap screenshot) { this.screenshot = screenshot; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
}