package com.gestureai.gameautomation.models;

/**
 * Data structures for IRL intention analysis with reasoning integration
 */

public class IntentionAnalysis {
    private float baseIntentionScore;
    private StrategicContext strategicContext;
    private TargetContext targetContext;
    private MethodContext methodContext;
    private float overallConfidence;
    private String errorMessage;
    
    public float getBaseIntentionScore() { return baseIntentionScore; }
    public void setBaseIntentionScore(float score) { this.baseIntentionScore = score; }
    public StrategicContext getStrategicContext() { return strategicContext; }
    public void setStrategicContext(StrategicContext context) { this.strategicContext = context; }
    public TargetContext getTargetContext() { return targetContext; }
    public void setTargetContext(TargetContext context) { this.targetContext = context; }
    public MethodContext getMethodContext() { return methodContext; }
    public void setMethodContext(MethodContext context) { this.methodContext = context; }
    public float getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(float confidence) { this.overallConfidence = confidence; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String message) { this.errorMessage = message; }
}

public class StrategicContext {
    private float clarity;
    private String strategicType;
    private float urgencyLevel;
    
    public float getClarity() { return clarity; }
    public void setClarity(float clarity) { this.clarity = clarity; }
    public String getStrategicType() { return strategicType; }
    public void setStrategicType(String type) { this.strategicType = type; }
    public float getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(float level) { this.urgencyLevel = level; }
}

public class TargetContext {
    private float specificity;
    private String targetType;
    private float relevanceScore;
    
    public float getSpecificity() { return specificity; }
    public void setSpecificity(float specificity) { this.specificity = specificity; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String type) { this.targetType = type; }
    public float getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(float score) { this.relevanceScore = score; }
}

public class MethodContext {
    private float precision;
    private String methodType;
    private float executionDifficulty;
    
    public float getPrecision() { return precision; }
    public void setPrecision(float precision) { this.precision = precision; }
    public String getMethodType() { return methodType; }
    public void setMethodType(String type) { this.methodType = type; }
    public float getExecutionDifficulty() { return executionDifficulty; }
    public void setExecutionDifficulty(float difficulty) { this.executionDifficulty = difficulty; }
}