package com.gestureai.gameautomation.workflow.recording;

/**
 * Intelligent suggestion generated during recording based on pattern analysis and NLP
 */
public class RecordingSuggestion {
    private String title;
    private String description;
    private SuggestionType type;
    private float relevanceScore;
    private String actionId;
    private Object suggestionData;
    
    public enum SuggestionType {
        OPTIMIZATION,           // Performance or efficiency improvements
        PATTERN,               // Pattern-based workflow suggestions
        TIMING,                // Timing optimization suggestions
        WORKFLOW_CREATION,     // Suggests creating new workflows
        GAME_SPECIFIC,         // Game-specific automation suggestions
        NAVIGATION,            // UI navigation improvements
        ADAPTIVE,              // Adaptive recording suggestions
        CONSOLIDATION          // Action consolidation suggestions
    }
    
    public RecordingSuggestion(String title, String description, SuggestionType type, float relevanceScore) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.relevanceScore = relevanceScore;
    }
    
    public boolean isHighPriority() {
        return relevanceScore > 0.8f;
    }
    
    public boolean isActionable() {
        return actionId != null && !actionId.isEmpty();
    }
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public SuggestionType getType() { return type; }
    public void setType(SuggestionType type) { this.type = type; }
    
    public float getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(float relevanceScore) { this.relevanceScore = relevanceScore; }
    
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    
    public Object getSuggestionData() { return suggestionData; }
    public void setSuggestionData(Object suggestionData) { this.suggestionData = suggestionData; }
}