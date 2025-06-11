package com.gestureai.gameautomation.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow Definition - Represents a complete automation sequence
 */
public class WorkflowDefinition {
    private String name;
    private String description;
    private List<WorkflowStep> steps;
    private long createdTime;
    private String category;
    
    public WorkflowDefinition(String name, String description) {
        this.name = name;
        this.description = description;
        this.steps = new ArrayList<>();
        this.createdTime = System.currentTimeMillis();
        this.category = "General";
    }
    
    public void addStep(WorkflowStep step) {
        steps.add(step);
    }
    
    public void removeStep(int index) {
        if (index >= 0 && index < steps.size()) {
            steps.remove(index);
        }
    }
    
    public void insertStep(int index, WorkflowStep step) {
        if (index >= 0 && index <= steps.size()) {
            steps.add(index, step);
        }
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<WorkflowStep> getSteps() { return steps; }
    public void setSteps(List<WorkflowStep> steps) { this.steps = steps; }
    
    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public int getStepCount() { return steps.size(); }
    
    @Override
    public String toString() {
        return "WorkflowDefinition{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", stepCount=" + steps.size() +
                ", category='" + category + '\'' +
                '}';
    }
}