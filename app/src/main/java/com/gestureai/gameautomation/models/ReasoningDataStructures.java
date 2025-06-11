package com.gestureai.gameautomation.models;

import java.util.*;

/**
 * Complete data structures for reasoning integration
 */

public class SequenceAnalysisResult {
    private Map<String, List<Float>> objectTrajectories;
    private List<String> reasoningEvolution;
    private float coherenceScore;
    
    public SequenceAnalysisResult() {
        this.objectTrajectories = new HashMap<>();
        this.reasoningEvolution = new ArrayList<>();
        this.coherenceScore = 0.0f;
    }
    
    public Map<String, List<Float>> getObjectTrajectories() { return objectTrajectories; }
    public void setObjectTrajectories(Map<String, List<Float>> trajectories) { this.objectTrajectories = trajectories; }
    public List<String> getReasoningEvolution() { return reasoningEvolution; }
    public void setReasoningEvolution(List<String> evolution) { this.reasoningEvolution = evolution; }
    public float getCoherenceScore() { return coherenceScore; }
    public void setCoherenceScore(float score) { this.coherenceScore = score; }
}

public class ExtractedReasoningInsights {
    private StrategicIntent strategicIntent;
    private TargetAnalysis targetAnalysis;
    private InteractionComplexity interactionComplexity;
    
    public StrategicIntent getStrategicIntent() { return strategicIntent; }
    public void setStrategicIntent(StrategicIntent intent) { this.strategicIntent = intent; }
    public TargetAnalysis getTargetAnalysis() { return targetAnalysis; }
    public void setTargetAnalysis(TargetAnalysis analysis) { this.targetAnalysis = analysis; }
    public InteractionComplexity getInteractionComplexity() { return interactionComplexity; }
    public void setInteractionComplexity(InteractionComplexity complexity) { this.interactionComplexity = complexity; }
}

public class StrategicIntent {
    public enum Type { OFFENSIVE, DEFENSIVE, RESOURCE_GATHERING, EXPLORATION, UTILITY }
    public enum Urgency { HIGH, MEDIUM, LOW }
    
    private Type type;
    private Urgency urgency;
    private float confidence;
    
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
}

public class TargetAnalysis {
    public enum Category { HOSTILE_ENTITY, UI_ELEMENT, COLLECTIBLE, NAVIGATION, UNKNOWN }
    
    private String targetName;
    private Category category;
    private float relevanceScore;
    
    public String getTargetName() { return targetName; }
    public void setTargetName(String name) { this.targetName = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public float getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(float score) { this.relevanceScore = score; }
}

public class InteractionComplexity {
    public enum Level { SIMPLE, MODERATE, COMPLEX, VAGUE }
    
    private Level complexityLevel;
    private String description;
    private float executionDifficulty;
    
    public Level getComplexityLevel() { return complexityLevel; }
    public void setComplexityLevel(Level level) { this.complexityLevel = level; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public float getExecutionDifficulty() { return executionDifficulty; }
    public void setExecutionDifficulty(float difficulty) { this.executionDifficulty = difficulty; }
}

public class ActionPattern {
    public enum Type { SEQUENTIAL, CONDITIONAL, TEMPORAL }
    
    private Type type;
    private String description;
    private int frequency;
    private float confidence;
    private Map<String, Object> metadata;
    
    public ActionPattern() {
        this.metadata = new HashMap<>();
    }
    
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getFrequency() { return frequency; }
    public void setFrequency(int frequency) { this.frequency = frequency; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public Map<String, Object> getMetadata() { return metadata; }
}

public class DecisionNode {
    private String nodeId;
    private String description;
    private Map<String, String> conditions;
    private Map<String, Float> actions;
    
    public DecisionNode(String nodeId, String description) {
        this.nodeId = nodeId;
        this.description = description;
        this.conditions = new HashMap<>();
        this.actions = new HashMap<>();
    }
    
    public void addCondition(String key, String value) {
        conditions.put(key, value);
    }
    
    public void addAction(String action, float score) {
        actions.put(action, score);
    }
    
    public Map<String, Float> evaluateActions(Object gameState) {
        // Simple evaluation - in practice would use complex logic
        return new HashMap<>(actions);
    }
    
    public String getNodeId() { return nodeId; }
    public String getDescription() { return description; }
    public Map<String, String> getConditions() { return conditions; }
    public Map<String, Float> getActions() { return actions; }
}

public class DecisionResult {
    private String recommendedAction;
    private float confidence;
    private String reasoning;
    
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String action) { this.recommendedAction = action; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
}

public class SemanticConcept {
    public enum Type { STRATEGIC, OBJECT, INTERACTION }
    
    private String conceptId;
    private Type type;
    private int usageCount;
    private Map<String, Float> contextFeatures;
    private Map<String, Float> objectFeatures;
    private Map<String, Float> uiFeatures;
    
    public SemanticConcept(String conceptId, Type type) {
        this.conceptId = conceptId;
        this.type = type;
        this.usageCount = 0;
        this.contextFeatures = new HashMap<>();
        this.objectFeatures = new HashMap<>();
        this.uiFeatures = new HashMap<>();
    }
    
    public void addContextFeature(String key, float value) {
        contextFeatures.put(key, value);
    }
    
    public void addObjectFeature(String key, float value) {
        objectFeatures.put(key, value);
    }
    
    public void addUIFeature(String key, float value) {
        uiFeatures.put(key, value);
    }
    
    public void incrementUsageCount() {
        usageCount++;
    }
    
    public String getConceptId() { return conceptId; }
    public Type getType() { return type; }
    public int getUsageCount() { return usageCount; }
    public Map<String, Float> getContextFeatures() { return contextFeatures; }
    public Map<String, Float> getObjectFeatures() { return objectFeatures; }
    public Map<String, Float> getUIFeatures() { return uiFeatures; }
}

public class VisualContext {
    private String sceneType;
    private float complexity;
    private List<String> dominantObjects;
    private Map<String, Float> visualFeatures;
    
    public VisualContext() {
        this.dominantObjects = new ArrayList<>();
        this.visualFeatures = new HashMap<>();
    }
    
    public String getSceneType() { return sceneType; }
    public void setSceneType(String type) { this.sceneType = type; }
    public float getComplexity() { return complexity; }
    public void setComplexity(float complexity) { this.complexity = complexity; }
    public List<String> getDominantObjects() { return dominantObjects; }
    public Map<String, Float> getVisualFeatures() { return visualFeatures; }
}

public class ActionIntent {
    private String intentType;
    private float confidence;
    private Map<String, Object> parameters;
    private String reasoningBasis;
    
    public ActionIntent() {
        this.parameters = new HashMap<>();
    }
    
    public String getIntentType() { return intentType; }
    public void setIntentType(String type) { this.intentType = type; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public Map<String, Object> getParameters() { return parameters; }
    public String getReasoningBasis() { return reasoningBasis; }
    public void setReasoningBasis(String basis) { this.reasoningBasis = basis; }
}

public class DemonstrationSequence {
    private String sequenceId;
    private List<String> actionSequence;
    private Map<String, Object> contextData;
    private float effectivenessScore;
    
    public DemonstrationSequence() {
        this.actionSequence = new ArrayList<>();
        this.contextData = new HashMap<>();
    }
    
    public String getSequenceId() { return sequenceId; }
    public void setSequenceId(String id) { this.sequenceId = id; }
    public List<String> getActionSequence() { return actionSequence; }
    public Map<String, Object> getContextData() { return contextData; }
    public float getEffectivenessScore() { return effectivenessScore; }
    public void setEffectivenessScore(float score) { this.effectivenessScore = score; }
}

public class SemanticAnalysisResult {
    private float semanticCoherence;
    private List<String> extractedConcepts;
    private Map<String, Float> conceptConfidence;
    
    public SemanticAnalysisResult() {
        this.extractedConcepts = new ArrayList<>();
        this.conceptConfidence = new HashMap<>();
    }
    
    public float getSemanticCoherence() { return semanticCoherence; }
    public void setSemanticCoherence(float coherence) { this.semanticCoherence = coherence; }
    public List<String> getExtractedConcepts() { return extractedConcepts; }
    public Map<String, Float> getConceptConfidence() { return conceptConfidence; }
}

public class LearningMetrics {
    private float learningProgress;
    private int patternsDiscovered;
    private float modelAccuracy;
    private Map<String, Float> skillProgression;
    
    public LearningMetrics() {
        this.skillProgression = new HashMap<>();
    }
    
    public float getLearningProgress() { return learningProgress; }
    public void setLearningProgress(float progress) { this.learningProgress = progress; }
    public int getPatternsDiscovered() { return patternsDiscovered; }
    public void setPatternsDiscovered(int patterns) { this.patternsDiscovered = patterns; }
    public float getModelAccuracy() { return modelAccuracy; }
    public void setModelAccuracy(float accuracy) { this.modelAccuracy = accuracy; }
    public Map<String, Float> getSkillProgression() { return skillProgression; }
}

public class CompletedDemonstration {
    private String demonstrationId;
    private long completionTime;
    private List<ActionPattern> discoveredPatterns;
    private float overallQuality;
    private Map<String, Object> metadata;
    
    public CompletedDemonstration(Object session, DemonstrationSequence sequence, 
                                List<ActionPattern> patterns, Object workflows) {
        this.demonstrationId = "demo_" + System.currentTimeMillis();
        this.completionTime = System.currentTimeMillis();
        this.discoveredPatterns = new ArrayList<>(patterns);
        this.overallQuality = 0.8f;
        this.metadata = new HashMap<>();
    }
    
    public String getDemonstrationId() { return demonstrationId; }
    public long getCompletionTime() { return completionTime; }
    public List<ActionPattern> getDiscoveredPatterns() { return discoveredPatterns; }
    public float getOverallQuality() { return overallQuality; }
    public Map<String, Object> getMetadata() { return metadata; }
}