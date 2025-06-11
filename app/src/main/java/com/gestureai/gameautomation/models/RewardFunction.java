package com.gestureai.gameautomation.models;

import org.json.JSONObject;
import java.util.*;

/**
 * Learned reward function from Inverse Reinforcement Learning
 * Maps game features to reward weights explaining AI behavior
 */
public class RewardFunction {
    private Map<String, Float> featureWeights;
    private float totalWeight;
    private long lastUpdated;
    
    public RewardFunction() {
        this.featureWeights = new HashMap<>();
        this.totalWeight = 0.0f;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void addFeature(String featureName, float weight) {
        featureWeights.put(featureName, weight);
        recalculateTotal();
    }
    
    public void updateFeature(String featureName, float weight) {
        if (featureWeights.containsKey(featureName)) {
            featureWeights.put(featureName, weight);
            recalculateTotal();
        }
    }
    
    public float getFeatureWeight(String featureName) {
        return featureWeights.getOrDefault(featureName, 0.0f);
    }
    
    public Map<String, Float> getFeatureWeights() {
        return new HashMap<>(featureWeights);
    }
    
    public void normalize() {
        if (totalWeight > 0) {
            for (Map.Entry<String, Float> entry : featureWeights.entrySet()) {
                entry.setValue(entry.getValue() / totalWeight);
            }
            recalculateTotal();
        }
    }
    
    public void clear() {
        featureWeights.clear();
        totalWeight = 0.0f;
        lastUpdated = System.currentTimeMillis();
    }
    
    private void recalculateTotal() {
        totalWeight = 0.0f;
        for (float weight : featureWeights.values()) {
            totalWeight += Math.abs(weight);
        }
        lastUpdated = System.currentTimeMillis();
    }
    
    public float calculateReward(Map<String, Float> gameFeatures) {
        float reward = 0.0f;
        
        for (Map.Entry<String, Float> feature : gameFeatures.entrySet()) {
            float weight = featureWeights.getOrDefault(feature.getKey(), 0.0f);
            reward += weight * feature.getValue();
        }
        
        return reward;
    }
    
    public List<String> getTopFeatures(int count) {
        return featureWeights.entrySet().stream()
            .sorted((a, b) -> Float.compare(Math.abs(b.getValue()), Math.abs(a.getValue())))
            .limit(count)
            .map(entry -> entry.getKey() + " (" + String.format("%.3f", entry.getValue()) + ")")
            .collect(java.util.stream.Collectors.toList());
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        
        try {
            json.put("featureWeights", new JSONObject(featureWeights));
            json.put("totalWeight", totalWeight);
            json.put("lastUpdated", lastUpdated);
            json.put("featureCount", featureWeights.size());
            
        } catch (Exception e) {
            try {
                json.put("error", "Serialization failed");
            } catch (Exception ignored) {}
        }
        
        return json;
    }
    
    public boolean isEmpty() {
        return featureWeights.isEmpty();
    }
    
    public int getFeatureCount() {
        return featureWeights.size();
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
}