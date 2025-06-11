package com.gestureai.gameautomation;

import com.gestureai.gameautomation.utils.NLPProcessor;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced template for game object detection with hierarchical classification and semantic understanding
 */
public class GameObjectTemplate {
    public final String name;
    public final String category;
    public final String type;
    public final String state;
    public final String context;
    public final String defaultAction;
    public final String description;
    public final int[] hsvLower;
    public final int[] hsvUpper;
    public final int minSize;
    public final int maxSize;
    public final float confidenceThreshold;
    
    // Enhanced fields for semantic understanding
    public final String semanticDescription;
    public final Map<String, String> customAttributes;
    public final List<String> tags;
    public final float importanceScore;
    public final String objectBehavior;
    public final String interactionType;

    // Enhanced constructor with hierarchical classification and semantic understanding
    public GameObjectTemplate(String name, String category, String type, String state, 
                            String context, String defaultAction, String description,
                            int[] hsvLower, int[] hsvUpper, int minSize, int maxSize, 
                            float confidenceThreshold, String semanticDescription,
                            Map<String, String> customAttributes, List<String> tags,
                            float importanceScore, String objectBehavior, String interactionType) {
        this.name = name;
        this.category = category != null ? category : "unknown";
        this.type = type != null ? type : "default";
        this.state = state != null ? state : "normal";
        this.context = context != null ? context : "general";
        this.defaultAction = defaultAction;
        this.description = description;
        this.hsvLower = hsvLower;
        this.hsvUpper = hsvUpper;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.confidenceThreshold = confidenceThreshold;
        
        // Enhanced semantic fields
        this.semanticDescription = semanticDescription != null ? semanticDescription : description;
        this.customAttributes = customAttributes != null ? customAttributes : new HashMap<>();
        this.tags = tags != null ? tags : new ArrayList<>();
        this.importanceScore = Math.max(0.0f, Math.min(1.0f, importanceScore));
        this.objectBehavior = objectBehavior != null ? objectBehavior : "static";
        this.interactionType = interactionType != null ? interactionType : "none";
    }
    
    // Enhanced constructor with hierarchical classification (backward compatibility)
    public GameObjectTemplate(String name, String category, String type, String state, 
                            String context, String defaultAction, String description,
                            int[] hsvLower, int[] hsvUpper, int minSize, int maxSize, 
                            float confidenceThreshold) {
        this(name, category, type, state, context, defaultAction, description,
             hsvLower, hsvUpper, minSize, maxSize, confidenceThreshold,
             null, null, null, 0.5f, null, null);
    }
    
    // Backward compatibility constructor
    public GameObjectTemplate(String name, String defaultAction, String description,
                            int[] hsvLower, int[] hsvUpper, int minSize, int maxSize, 
                            float confidenceThreshold) {
        this(name, "unknown", "default", "normal", "general", defaultAction, description,
             hsvLower, hsvUpper, minSize, maxSize, confidenceThreshold);
    }
    
    // Utility methods for hierarchical classification with semantic understanding
    public String getFullClassification() {
        return category + "_" + type + "_" + name + "_" + state + "_" + context;
    }
    
    public String getSemanticClassification() {
        StringBuilder semantic = new StringBuilder();
        semantic.append(category).append("/").append(type);
        if (!tags.isEmpty()) {
            semantic.append(" [").append(String.join(", ", tags)).append("]");
        }
        semantic.append(" (").append(objectBehavior).append(")");
        return semantic.toString();
    }
    
    public boolean matchesCategory(String categoryPattern) {
        return category.toLowerCase().contains(categoryPattern.toLowerCase());
    }
    
    public boolean matchesType(String typePattern) {
        return type.toLowerCase().contains(typePattern.toLowerCase());
    }
    
    public boolean matchesState(String statePattern) {
        return state.toLowerCase().contains(statePattern.toLowerCase());
    }
    
    public boolean matchesContext(String contextPattern) {
        return context.toLowerCase().contains(contextPattern.toLowerCase());
    }
    
    public boolean matchesSemanticPattern(String pattern, NLPProcessor nlpProcessor) {
        if (nlpProcessor == null) return false;
        
        // Use NLP processor to analyze semantic similarity
        NLPProcessor.SemanticAnalysis analysis = nlpProcessor.analyzeSemanticContext(semanticDescription);
        String[] patterns = pattern.toLowerCase().split("\\s+");
        
        for (String pat : patterns) {
            if (semanticDescription.toLowerCase().contains(pat) ||
                description.toLowerCase().contains(pat) ||
                tags.stream().anyMatch(tag -> tag.toLowerCase().contains(pat))) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean hasCustomAttribute(String key) {
        return customAttributes.containsKey(key);
    }
    
    public String getCustomAttribute(String key) {
        return customAttributes.get(key);
    }
    
    public boolean hasTag(String tag) {
        return tags.stream().anyMatch(t -> t.toLowerCase().equals(tag.toLowerCase()));
    }
    
    // Getters
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getType() { return type; }
    public String getState() { return state; }
    public String getContext() { return context; }
    public String getSemanticDescription() { return semanticDescription; }
    public Map<String, String> getCustomAttributes() { return new HashMap<>(customAttributes); }
    public List<String> getTags() { return new ArrayList<>(tags); }
    public float getImportanceScore() { return importanceScore; }
    public String getObjectBehavior() { return objectBehavior; }
    public String getInteractionType() { return interactionType; }
}