package com.gestureai.gameautomation.workflow.visual;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Visual node representation for the workflow builder
 */
public class VisualWorkflowNode {
    private String id;
    private String displayName;
    private String description;
    private float x, y;
    private WorkflowBuilder.NodeType nodeType;
    private Map<String, Object> properties;
    private boolean hasErrors;
    private boolean isSelected;
    
    public VisualWorkflowNode(String id, String displayName, float x, float y) {
        this.id = id;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.properties = new HashMap<>();
        this.hasErrors = false;
        this.isSelected = false;
        this.nodeType = WorkflowBuilder.NodeType.ACTION;
    }
    
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public Object getProperty(String key, Object defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VisualWorkflowNode)) return false;
        VisualWorkflowNode that = (VisualWorkflowNode) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public float getX() { return x; }
    public float getY() { return y; }
    
    public WorkflowBuilder.NodeType getNodeType() { return nodeType; }
    public void setNodeType(WorkflowBuilder.NodeType nodeType) { this.nodeType = nodeType; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    public boolean hasErrors() { return hasErrors; }
    public void setHasErrors(boolean hasErrors) { this.hasErrors = hasErrors; }
    
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { this.isSelected = selected; }
}