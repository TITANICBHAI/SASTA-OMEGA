package com.gestureai.gameautomation.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an action intent derived from natural language processing
 * Used by NLPProcessor to communicate game actions to the automation engine
 */
public class ActionIntent {
    private String action;
    private Map<String, Object> parameters;
    private float confidence;
    private long timestamp;

    public ActionIntent(String action, Map<String, Object> parameters, float confidence) {
        this.action = action;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.confidence = confidence;
        this.timestamp = System.currentTimeMillis();
    }

    public ActionIntent(String action, float confidence) {
        this(action, new HashMap<>(), confidence);
    }

    // Getters
    public String getAction() {
        return action;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public float getConfidence() {
        return confidence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Utility methods
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    public Object getParameter(String key) {
        return parameters.get(key);
    }

    public String getParameterAsString(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }

    public Integer getParameterAsInt(String key) {
        Object value = parameters.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Float getParameterAsFloat(String key) {
        Object value = parameters.get(key);
        if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Double) {
            return ((Double) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public void addParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public boolean isHighConfidence() {
        return confidence >= 0.8f;
    }

    public boolean isMediumConfidence() {
        return confidence >= 0.5f && confidence < 0.8f;
    }

    public boolean isLowConfidence() {
        return confidence < 0.5f;
    }

    @Override
    public String toString() {
        return "ActionIntent{" +
                "action='" + action + '\'' +
                ", parameters=" + parameters +
                ", confidence=" + confidence +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionIntent that = (ActionIntent) o;

        if (Float.compare(that.confidence, confidence) != 0) return false;
        if (!action.equals(that.action)) return false;
        return parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + parameters.hashCode();
        result = 31 * result + (confidence != +0.0f ? Float.floatToIntBits(confidence) : 0);
        return result;
    }
}