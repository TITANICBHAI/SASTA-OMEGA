package com.gestureai.gameautomation.workflow;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.workflow.actions.*;
import com.gestureai.gameautomation.workflow.conditions.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;

/**
 * Core Workflow Engine - Executes custom automation sequences
 * Supports conditional logic, loops, timing, and visual recognition
 */
public class WorkflowEngine {
    private static final String TAG = "WorkflowEngine";
    private static volatile WorkflowEngine instance;
    
    private Context context;
    private ExecutorService executorService;
    private Map<String, WorkflowDefinition> workflows;
    private Map<String, Object> globalVariables;
    private boolean isRunning = false;
    
    public static WorkflowEngine getInstance(Context context) {
        if (instance == null) {
            synchronized (WorkflowEngine.class) {
                if (instance == null) {
                    instance = new WorkflowEngine(context);
                }
            }
        }
        return instance;
    }
    
    private WorkflowEngine(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(2); // Prevent single point of failure
        this.workflows = new ConcurrentHashMap<>();
        this.globalVariables = new ConcurrentHashMap<>();
        
        Log.d(TAG, "WorkflowEngine initialized");
    }
    
    /**
     * Execute workflow from JSON configuration
     */
    public void executeWorkflow(String workflowJson) {
        try {
            JSONObject config = new JSONObject(workflowJson);
            WorkflowDefinition workflow = parseWorkflowFromJson(config);
            executeWorkflow(workflow);
        } catch (Exception e) {
            Log.e(TAG, "Error executing workflow from JSON", e);
        }
    }
    
    /**
     * Execute workflow definition
     */
    public void executeWorkflow(WorkflowDefinition workflow) {
        if (isRunning) {
            Log.w(TAG, "Workflow already running, stopping previous execution");
            stopWorkflow();
        }
        
        isRunning = true;
        executorService.submit(() -> {
            try {
                Log.d(TAG, "Starting workflow: " + workflow.getName());
                executeSteps(workflow.getSteps());
                Log.d(TAG, "Workflow completed: " + workflow.getName());
            } catch (Exception e) {
                Log.e(TAG, "Workflow execution failed", e);
            } finally {
                isRunning = false;
            }
        });
    }
    
    private void executeSteps(List<WorkflowStep> steps) throws Exception {
        for (WorkflowStep step : steps) {
            if (!isRunning) break; // Check for stop signal
            
            Log.d(TAG, "Executing step: " + step.getName());
            
            // Check condition if present
            if (step.getCondition() != null && !step.getCondition().evaluate(context, globalVariables)) {
                Log.d(TAG, "Step condition not met, skipping: " + step.getName());
                continue;
            }
            
            // Execute action
            if (step.getAction() != null) {
                step.getAction().execute(context, globalVariables);
            }
            
            // Handle loops
            if (step.isLoop()) {
                executeLoop(step);
            }
            
            // Wait if specified
            if (step.getWaitTime() > 0) {
                Thread.sleep(step.getWaitTime());
            }
        }
    }
    
    private void executeLoop(WorkflowStep step) throws Exception {
        int maxIterations = step.getMaxIterations();
        int currentIteration = 0;
        
        while (isRunning && currentIteration < maxIterations) {
            // Check loop condition
            if (step.getLoopCondition() != null && 
                !step.getLoopCondition().evaluate(context, globalVariables)) {
                Log.d(TAG, "Loop condition not met, breaking loop");
                break;
            }
            
            // Execute loop steps
            executeSteps(step.getLoopSteps());
            currentIteration++;
            
            // Wait between iterations
            if (step.getLoopDelay() > 0) {
                Thread.sleep(step.getLoopDelay());
            }
        }
    }
    
    private WorkflowDefinition parseWorkflowFromJson(JSONObject config) throws Exception {
        String name = config.getString("name");
        String description = config.optString("description", "");
        
        WorkflowDefinition workflow = new WorkflowDefinition(name, description);
        
        JSONArray stepsArray = config.getJSONArray("steps");
        for (int i = 0; i < stepsArray.length(); i++) {
            JSONObject stepJson = stepsArray.getJSONObject(i);
            WorkflowStep step = parseStepFromJson(stepJson);
            workflow.addStep(step);
        }
        
        return workflow;
    }
    
    private WorkflowStep parseStepFromJson(JSONObject stepJson) throws Exception {
        String name = stepJson.getString("name");
        WorkflowStep step = new WorkflowStep(name);
        
        // Parse action
        if (stepJson.has("action")) {
            JSONObject actionJson = stepJson.getJSONObject("action");
            step.setAction(parseActionFromJson(actionJson));
        }
        
        // Parse condition
        if (stepJson.has("condition")) {
            JSONObject conditionJson = stepJson.getJSONObject("condition");
            step.setCondition(parseConditionFromJson(conditionJson));
        }
        
        // Parse timing
        step.setWaitTime(stepJson.optLong("waitTime", 0));
        
        // Parse loop configuration
        if (stepJson.has("loop")) {
            JSONObject loopJson = stepJson.getJSONObject("loop");
            step.setLoop(true);
            step.setMaxIterations(loopJson.optInt("maxIterations", 1));
            step.setLoopDelay(loopJson.optLong("delay", 1000));
            
            if (loopJson.has("condition")) {
                step.setLoopCondition(parseConditionFromJson(loopJson.getJSONObject("condition")));
            }
            
            if (loopJson.has("steps")) {
                JSONArray loopStepsArray = loopJson.getJSONArray("steps");
                List<WorkflowStep> loopSteps = new ArrayList<>();
                for (int j = 0; j < loopStepsArray.length(); j++) {
                    loopSteps.add(parseStepFromJson(loopStepsArray.getJSONObject(j)));
                }
                step.setLoopSteps(loopSteps);
            }
        }
        
        return step;
    }
    
    private WorkflowAction parseActionFromJson(JSONObject actionJson) throws Exception {
        String type = actionJson.getString("type");
        
        switch (type) {
            case "tap":
                return new TapAction(
                    actionJson.getInt("x"),
                    actionJson.getInt("y")
                );
            case "tapElement":
                return new TapElementAction(
                    actionJson.getString("elementId"),
                    actionJson.optString("text", null)
                );
            case "swipe":
                return new SwipeAction(
                    actionJson.getInt("startX"),
                    actionJson.getInt("startY"),
                    actionJson.getInt("endX"),
                    actionJson.getInt("endY"),
                    actionJson.optLong("duration", 500)
                );
            case "type":
                return new TypeTextAction(actionJson.getString("text"));
            case "wait":
                return new WaitAction(actionJson.getLong("duration"));
            case "screenshot":
                return new ScreenshotAction(actionJson.optString("filename", null));
            case "openApp":
                return new OpenAppAction(actionJson.getString("packageName"));
            default:
                throw new IllegalArgumentException("Unknown action type: " + type);
        }
    }
    
    private WorkflowCondition parseConditionFromJson(JSONObject conditionJson) throws Exception {
        String type = conditionJson.getString("type");
        
        switch (type) {
            case "textVisible":
                return new TextVisibleCondition(conditionJson.getString("text"));
            case "elementExists":
                return new ElementExistsCondition(conditionJson.getString("elementId"));
            case "imageMatches":
                return new ImageMatchCondition(
                    conditionJson.getString("templatePath"),
                    conditionJson.optDouble("threshold", 0.8)
                );
            case "numberCompare":
                return new NumberCompareCondition(
                    conditionJson.getString("variableName"),
                    conditionJson.getString("operator"),
                    conditionJson.getDouble("value")
                );
            case "timeElapsed":
                return new TimeElapsedCondition(conditionJson.getLong("duration"));
            default:
                throw new IllegalArgumentException("Unknown condition type: " + type);
        }
    }
    
    public void stopWorkflow() {
        isRunning = false;
        Log.d(TAG, "Workflow stop requested");
    }
    
    public boolean isWorkflowRunning() {
        return isRunning;
    }
    
    public void setGlobalVariable(String name, Object value) {
        globalVariables.put(name, value);
    }
    
    public Object getGlobalVariable(String name) {
        return globalVariables.get(name);
    }
    
    public void shutdown() {
        stopWorkflow();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}