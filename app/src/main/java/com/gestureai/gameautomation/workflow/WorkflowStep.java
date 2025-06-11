package com.gestureai.gameautomation.workflow;

import com.gestureai.gameautomation.workflow.actions.WorkflowAction;
import com.gestureai.gameautomation.workflow.conditions.WorkflowCondition;
import java.util.List;

/**
 * Workflow Step - Individual step in automation sequence
 */
public class WorkflowStep {
    private String name;
    private WorkflowAction action;
    private WorkflowCondition condition;
    private long waitTime;
    
    // Loop configuration
    private boolean isLoop;
    private int maxIterations;
    private long loopDelay;
    private WorkflowCondition loopCondition;
    private List<WorkflowStep> loopSteps;
    
    public WorkflowStep(String name) {
        this.name = name;
        this.waitTime = 0;
        this.isLoop = false;
        this.maxIterations = 1;
        this.loopDelay = 1000;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public WorkflowAction getAction() { return action; }
    public void setAction(WorkflowAction action) { this.action = action; }
    
    public WorkflowCondition getCondition() { return condition; }
    public void setCondition(WorkflowCondition condition) { this.condition = condition; }
    
    public long getWaitTime() { return waitTime; }
    public void setWaitTime(long waitTime) { this.waitTime = waitTime; }
    
    public boolean isLoop() { return isLoop; }
    public void setLoop(boolean loop) { isLoop = loop; }
    
    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }
    
    public long getLoopDelay() { return loopDelay; }
    public void setLoopDelay(long loopDelay) { this.loopDelay = loopDelay; }
    
    public WorkflowCondition getLoopCondition() { return loopCondition; }
    public void setLoopCondition(WorkflowCondition loopCondition) { this.loopCondition = loopCondition; }
    
    public List<WorkflowStep> getLoopSteps() { return loopSteps; }
    public void setLoopSteps(List<WorkflowStep> loopSteps) { this.loopSteps = loopSteps; }
    
    @Override
    public String toString() {
        return "WorkflowStep{" +
                "name='" + name + '\'' +
                ", hasAction=" + (action != null) +
                ", hasCondition=" + (condition != null) +
                ", waitTime=" + waitTime +
                ", isLoop=" + isLoop +
                '}';
    }
}