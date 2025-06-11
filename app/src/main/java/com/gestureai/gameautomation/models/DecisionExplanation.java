package com.gestureai.gameautomation.models;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;

/**
 * Comprehensive decision explanation model
 * Contains multimodal analysis results and reasoning chains
 */
public class DecisionExplanation {
    public long timestamp;
    public int frameIndex;
    public String decision;
    public float confidence;
    public String reasoning;
    public String textualContext;
    public String rewardAnalysis;
    
    public List<String> keyFactors = new ArrayList<>();
    public List<String> causalChain = new ArrayList<>();
    public List<String> alternativeActions = new ArrayList<>();
    
    public Map<String, Float> visualFeatures = new HashMap<>();
    public Map<String, Float> visualInfluence = new HashMap<>();
    public Map<String, Float> textualInfluence = new HashMap<>();
    public Map<String, Float> confidenceBreakdown = new HashMap<>();
    
    public DecisionExplanation() {}
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        
        try {
            json.put("timestamp", timestamp);
            json.put("frameIndex", frameIndex);
            json.put("decision", decision);
            json.put("confidence", confidence);
            json.put("reasoning", reasoning);
            json.put("textualContext", textualContext);
            json.put("rewardAnalysis", rewardAnalysis);
            
            // Convert lists to JSON arrays
            json.put("keyFactors", new JSONArray(keyFactors));
            json.put("causalChain", new JSONArray(causalChain));
            json.put("alternativeActions", new JSONArray(alternativeActions));
            
            // Convert maps to JSON objects
            json.put("visualFeatures", new JSONObject(visualFeatures));
            json.put("visualInfluence", new JSONObject(visualInfluence));
            json.put("textualInfluence", new JSONObject(textualInfluence));
            json.put("confidenceBreakdown", new JSONObject(confidenceBreakdown));
            
        } catch (Exception e) {
            // Return minimal JSON on error
            try {
                json.put("error", "JSON serialization failed");
                json.put("timestamp", timestamp);
            } catch (Exception ignored) {}
        }
        
        return json;
    }
    
    public String getSummary() {
        return String.format("Decision: %s (%.1f%% confidence) - %s", 
            decision, confidence * 100, 
            reasoning.length() > 100 ? reasoning.substring(0, 100) + "..." : reasoning);
    }
    
    public String getTopInfluencingFactor() {
        String topFactor = "Unknown";
        float maxInfluence = 0;
        
        for (Map.Entry<String, Float> entry : visualInfluence.entrySet()) {
            if (entry.getValue() > maxInfluence) {
                maxInfluence = entry.getValue();
                topFactor = "Visual: " + entry.getKey();
            }
        }
        
        for (Map.Entry<String, Float> entry : textualInfluence.entrySet()) {
            if (entry.getValue() > maxInfluence) {
                maxInfluence = entry.getValue();
                topFactor = "Text: " + entry.getKey();
            }
        }
        
        return topFactor;
    }
}